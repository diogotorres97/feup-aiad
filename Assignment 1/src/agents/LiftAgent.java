package agents;

import jade.core.AID;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import sajas.core.Agent;
import sajas.domain.DFService;
import sajas.proto.AchieveREInitiator;
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
import java.util.Vector;

public class LiftAgent extends Agent implements Drawable {
    private static int NANO_TO_S = 1000000000;
    private final int speed;
    private final int stop_time;
    private double maxCallTime;
    private double minCallTime;
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
    private AID building = null;
    private long totalTaskTime;

    public LiftAgent(int x, int y, int speed, int stop_time, int strategy, int max_capacity, Object2DGrid space) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.stop_time = stop_time;
        this.space = space;
        this.max_capacity = max_capacity;
        this.usageTime = 0;
        this.noUsageTime = 0;
        this.minCallTime = 1000;
        this.maxCallTime = -1000;
        this.totalTaskTime = 0;
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
        return this.usageTime + this.noUsageTime == 0 ? 0 : this.usageTime * 1.0 / (this.usageTime + this.noUsageTime);
    }

    public double getOccupationRatio() {
        if (currentTask != null && !goingToOrigin)
            return currentTask.getNumAllPeople() * 1.0 / max_capacity;
        return 0;
    }

    public double getMaxWaitingTime() {
        return maxCallTime;
    }

    public double getMinWaitingTime() {
        return minCallTime;
    }

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
        if (currentTask != null && getCurrentFloor() == currentTask.getOriginFloor())
            simGraphics.drawRect(Color.BLACK);
        else
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

    //Function responsible for checking current task progress and trigger start/end task mechanisms accordingly (pick up/let off)
    public void executeTasks() {
        Task futureTask = null;
        if (tasks.isEmpty()) { //If no more tasks then stop
            ++this.noUsageTime;
            state = Direction.STOPPED;
            goingToOrigin = false;
            currentTask = null;
            return;
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
                futureTask = startTask();
            }
        } else if (currentTask.getDestinationFloor() == getCurrentFloor()) {
            endTask();
        }

        if (futureTask != null) {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            try {
                msg.setContentObject(futureTask);
            } catch (IOException e) {
                e.printStackTrace();
            }
            addBehaviour(new CallReRequester(this, msg));
        }
    }

    private void setEndAndUpdateMinMax() {
        currentTask.setEndTime(System.nanoTime());
        double taskWaitingTime = currentTask.getWaitingTime() / NANO_TO_S;
        System.out.println("TASK TIME:" + taskWaitingTime);
        this.totalTaskTime += taskWaitingTime;
        maxCallTime = Math.max(maxCallTime, taskWaitingTime);
        minCallTime = Math.min(minCallTime, taskWaitingTime);
    }

    public long getTotalTaskTime() {
        return totalTaskTime;
    }

    //When lift gets to current task origin, this function tries to pick up all the people and deals with overcapacity accordingly
    //(picks random people until full and sends remaining task to building)
    private Task startTask() {
        Task futureTask;

        if (currentTask.getNumAllPeople() > max_capacity) {
            System.out.println("Insufficient capacity for " + currentTask + ", making new request");
            futureTask = currentTask.getClone();

            int numberOfPeople = 0;
            Random seed = new Random(System.currentTimeMillis());
            int nTries = 0;

            do {
                int randomFloor = futureTask.getDestinations().get(seed.nextInt(futureTask.getDestFloorPeopleSize())); //Pick a random floor
                TreeMap<Integer, Integer> currentTaskDestMap = currentTask.getDestFloorPeople();
                TreeMap<Integer, Integer> futureTaskDestMap = futureTask.getDestFloorPeople();

                int randomPeople = seed.nextInt(currentTaskDestMap.get(randomFloor)) + 1; //Pick a random number of people of that floor
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

                if (futureTaskDestMap.isEmpty()) //If no more more destination floors, no need to resend task to building
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

    public int getStopTime() {
        return stop_time;
    }

    public int getSpeed() {
        return speed;
    }


    //Private class responsible for answering task allocation protocol
    class CallAnswerer extends ContractNetResponder {

        CallAnswerer(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) {
            LiftAgent a = (LiftAgent) myAgent;
            if (a.building == null) a.building = cfp.getSender();
            Task task = null;
            try {
                task = (Task) cfp.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }

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

            ACLMessage result = accept.createReply();
            result.setPerformative(ACLMessage.INFORM);
            result.setContent("Will be done");

            return result;
        }

    }


    //Private class responsible for sending tasks back to building to be reallocated
    class CallReRequester extends AchieveREInitiator {

        CallReRequester(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        protected Vector<ACLMessage> prepareRequests(ACLMessage msg) {
            Vector<ACLMessage> v = new Vector<>();
            v.add(msg);
            return v;
        }

        protected void handleAgree(ACLMessage agree) {
            System.out.println(getLocalName() + ": Building agreed to make call");
        }

        protected void handleRefuse(ACLMessage refuse) {
            //Should never happen
        }

        protected void handleInform(ACLMessage inform) {
            System.out.println(getLocalName() + ": Building will make call");
        }

        protected void handleFailure(ACLMessage failure) {
            //Should never happen
        }

    }

}
