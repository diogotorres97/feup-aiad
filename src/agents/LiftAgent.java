package agents;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import sajas.core.Agent;
import sajas.domain.DFService;
import sajas.proto.ContractNetResponder;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;
import utils.Direction;
import utils.Task;
import utils.evaluation.CallEvaluation;
import utils.evaluation.Closest;
import utils.evaluation.SmallestTimeEstimate;
import utils.evaluation.SmallestTimeNumpad;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class LiftAgent extends Agent implements Drawable {
    private double maxCallTime;
    private double minCallTime;
    private final int speed;
    private final int stop_time;
    private int x;
    private int y;
    private Object2DGrid space;
    private int max_capacity;
    private CallEvaluation evaluator;
    private ArrayList<Task> tasks = new ArrayList<>();
    private Task currentTask = null;
    private boolean goingToOrigin = false;
    private Direction state = Direction.STOPPED;
    private int usageTime;
    private int noUsageTime;

    public LiftAgent(int x, int y, int speed, int stop_time, int strategy, int max_capacity, Object2DGrid space) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.stop_time = stop_time;
        this.space = space;
        this.max_capacity = max_capacity;
        this.usageTime = 0;
        this.noUsageTime = 0;
        this.minCallTime = Double.MAX_VALUE;
        this.maxCallTime = Double.MIN_VALUE;
        switch (strategy) {
            case 0:
                this.evaluator = new Closest(this);
                break;
            case 1:
                this.evaluator = new SmallestTimeEstimate(this);
                break;
            case 2:
            default:
                this.evaluator = new SmallestTimeNumpad(this);
        }
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public Task getCurrentTask() {
        return currentTask;
    }

    public boolean isGoingToOrigin() {
        return goingToOrigin;
    }

    public Direction getState() {
        return state;
    }

    @Override
    protected void setup() {
        //Register as a lift so building can later find out about it at runtime
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("lift");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        //System.out.println("Lift registered!");
        addBehaviour(new CallAnswerer(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
    }

    public void updatePosition() {
        space.putObjectAt(getX(), getY(), null);

        if (state == Direction.UP && y > 0)
            y--;
        else if (state == Direction.DOWN && y < space.getSizeY() - 1)
            y++;

        space.putObjectAt(getX(), getY(), this);
    }

    public int getCurrentFloor() {
        return space.getSizeY() - y - 1;
    }

    public double getUsageRate() {
        return this.usageTime + this.noUsageTime == 0 ? 0 : this.usageTime*1.0/(this.usageTime+this.noUsageTime);
    }

    public double getMaxWaitingTime() {
        return maxCallTime/1000000000;
    }

    public double getMinWaitingTime() {
        return minCallTime/1000000000;
      
    public int getTotalFloors() {
        return space.getSizeY() - 1;
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void draw(SimGraphics simGraphics) {
        simGraphics.drawRect(Color.BLUE);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    private void addTask(Task task) {
        long startTime = System.nanoTime();
        //If possible, join task with other tasks with same origin and same destination direction
        for (Task t : tasks) {
            if (!task.similarTo(t)) continue;
            if (t != currentTask && goingToOrigin) {
                System.out.println(getLocalName() + " joined tasks: " + t + " and " + task);
                t.setStartTime(startTime);
                if (t.getDestinationFloor() != task.getDestinationFloor()) {
                    t.addDestinationFloor(task.getDestinationFloor(), task.getNumPeople());
                } else {
                    t.setNumPeople(t.getNumPeople() + task.getNumPeople());
                }
                return;
            }
        }

        task.setStartTime(startTime);
        tasks.add(task);
    }

    public Task executeTasks() {
        if (tasks.isEmpty()) { //If no more tasks then stop
            ++this.noUsageTime;
            state = Direction.STOPPED;
            goingToOrigin = false;
            currentTask = null;
            return null;
        }

        ++this.usageTime;

        if (currentTask == null) { //if I finish my task then get the next one
            currentTask = tasks.get(0);
            goingToOrigin = true;
        }

        if (goingToOrigin) {
            findState(currentTask);
            if (currentTask.getOriginFloor() == getCurrentFloor()) { // Check if the lift got to the origin floor
                setEndAndUpdateMinMax();
                return startTask(1);
            }
        } else if (currentTask.getDestinationFloor() == getCurrentFloor()) {
            endTask();
        }

        return null;
    }

    private void setEndAndUpdateMinMax() {
        currentTask.setEndTime(System.nanoTime());
        double taskWaitingTime = currentTask.getWaitingTime();
        maxCallTime = Math.max(maxCallTime, taskWaitingTime);
        minCallTime= Math.min(minCallTime, taskWaitingTime);
    }

    private Task startTask(int algorithm) {
        Task futureTask;
        switch (algorithm) {
            case 0:
                futureTask = pickFromFirstDestination();
                break;
            default:
                futureTask = pickRandomly();
                break;

        }
        return futureTask;
    }

    private Task pickFromFirstDestination() {
        Task futureTask;

        if (currentTask.getNumAllPeople() > max_capacity) {
            System.out.println("Insufficient capacity for " + currentTask + ", making new request");
            futureTask = currentTask.getClone();

            if (currentTask.getNumPeople() > max_capacity) { //first we transport the people for the first destination
                currentTask.setNumPeople(max_capacity); //Number of people that i can transport
                currentTask.removeTail(); //remove the rest of destinations of current task
                futureTask.setNumPeople(currentTask.getNumPeople() - max_capacity); //number of people left outside
            } else {
                // everyone from the first destination got on the lift, fill the lift and make new call for the rest
                int numberOfPeople = currentTask.getNumPeople();
                futureTask.removeDestinationFloor();
                currentTask.removeTail();
                TreeMap<Integer, Integer> futureTaskDestMap = futureTask.getDestFloorPeople();

                for (int key : futureTaskDestMap.keySet()) {
                    if (futureTaskDestMap.get(key) + numberOfPeople > max_capacity) {
                        int leftPeople = (futureTaskDestMap.get(key) + numberOfPeople) - max_capacity;
                        currentTask.addDestinationFloor(key, futureTaskDestMap.get(key) - leftPeople);

                        if (leftPeople > 0) {
                            futureTaskDestMap.put(key, leftPeople);
                        } else {
                            futureTaskDestMap.remove(key);
                        }
                        break;
                    } else {
                        numberOfPeople += futureTaskDestMap.get(key);
                        currentTask.addDestinationFloor(key, futureTaskDestMap.get(key));
                        futureTaskDestMap.remove(key);
                    }
                }

                if (futureTask.getDestinations().isEmpty())
                    return null;
            }

            return futureTask; // Send new request if not all people got in
        }

        return null;
    }

    private Task pickRandomly() {
        Task futureTask;

        if (currentTask.getNumAllPeople() > max_capacity) {
            System.out.println("Insufficient capacity for " + currentTask + ", making new request");
            futureTask = currentTask.getClone();

            int numberOfPeople = 0;
            Random seed = new Random(System.currentTimeMillis());
            int nTries = 0;

            do {
                int randomFloor = currentTask.getDestinations().get(seed.nextInt(currentTask.getDestFloorPeopleSize())); //Pick a random floor
                TreeMap<Integer, Integer> currentTaskDestMap = currentTask.getDestFloorPeople();
                TreeMap<Integer, Integer> futureTaskDestMap = futureTask.getDestFloorPeople();

                int randomPeople = seed.nextInt(currentTaskDestMap.get(randomFloor)); //Pick a random number of people of that floor
                int leftTotalPeopleDestination;

                //Fill the lift
                if (randomPeople + numberOfPeople > max_capacity) {
                    int leftPeople = (randomPeople + numberOfPeople) - max_capacity;
                    int peopleToPick = randomPeople - leftPeople;
                    currentTaskDestMap.put(randomFloor, peopleToPick);
                    numberOfPeople += peopleToPick;

                    leftTotalPeopleDestination = (futureTaskDestMap.get(randomFloor) - randomPeople) + leftPeople;
                } else {
                    numberOfPeople += randomPeople;
                    currentTaskDestMap.put(randomFloor, randomPeople);

                    leftTotalPeopleDestination = futureTaskDestMap.get(randomFloor) - randomPeople;
                }

                //Handle left people
                if (leftTotalPeopleDestination > 0) {
                    futureTaskDestMap.put(randomFloor, leftTotalPeopleDestination);
                } else {
                    futureTaskDestMap.remove(randomFloor);
                }

                if (futureTaskDestMap.isEmpty()) //If doesnt exist more destination floors dont send new task to building
                    return null;

            } while (numberOfPeople < max_capacity && ++nTries < 5);

            return futureTask; // Send new request if not all people got in
        }

        return null;
    }

    private void endTask() {
        System.out.println(getLocalName() + " answered " + currentTask);

        if (currentTask.getDestinations().size() > 1) { //If I have more destinations to go
            currentTask.removeDestinationFloor();
        } else { //finish the task, it's time to stop or move to the next one
            tasks.remove(0); // remove currentTask
            if (tasks.isEmpty()) {
                state = Direction.STOPPED;
                goingToOrigin = false;
                currentTask = null;
            } else {
                currentTask = tasks.get(0);
                goingToOrigin = true;
                findState(currentTask);
            }
        }
    }

    private void findState(Task task) {
        if (getCurrentFloor() < task.getOriginFloor())
            state = Direction.UP;
        else if (getCurrentFloor() > task.getOriginFloor())
            state = Direction.DOWN;
        else {
            state = task.getDirection();
            goingToOrigin = false;
        }
    }
      
    public Task getCurrentTask() {
        return currentTask;
    }

    public boolean isGoingToOrigin() {
        return goingToOrigin;
    }

    public ArrayList<Task> getTasks() {
        return tasks;
      
    public int getStopTime() {
        return stop_time;
    }

    public int getSpeed() {
        return speed;
    }

    class CallAnswerer extends ContractNetResponder {

        CallAnswerer(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) {
            Task task = null;
            try {
                task = (Task) cfp.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            //System.out.println(getLocalName() + " got request for task: " + task.getOriginFloor() + "|" + task.getDestinationFloor());
            ACLMessage reply = cfp.createReply();
            reply.setPerformative(ACLMessage.PROPOSE);
            try {
                reply.setContentObject(evaluator.evaluate(task));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return reply;
        }

        @Override
        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
            //System.out.println(myAgent.getLocalName() + " got a reject...");
        }

        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
            System.out.println(myAgent.getLocalName() + " got an accept!");
            try {
                Task task = (Task) cfp.getContentObject();
                addTask(task);
            } catch (UnreadableException e) {
                e.printStackTrace();
            }

            //TODO: Inform now?? Can't afford (?) to wait for task to end
            ACLMessage result = accept.createReply();
            result.setPerformative(ACLMessage.INFORM);
            result.setContent("this is the result");

            return result;
        }

    }
}
