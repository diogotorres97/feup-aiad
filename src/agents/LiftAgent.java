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

public class LiftAgent extends Agent implements Drawable {
    private int x;
    private int y;
    private Object2DGrid space;
    private int max_capacity;
    private CallEvaluation evaluator;
    private ArrayList<Task> tasks = new ArrayList<>();
    private Task currentTask = null;
    private boolean goingToOrigin = false;
    private Direction state = Direction.STOPPED;

    public LiftAgent(int x, int y, int strategy, int max_capacity, Object2DGrid space) {
        this.x = x;
        this.y = y;
        this.space = space;
        this.max_capacity = max_capacity;
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

        if (state == Direction.UP && y >= 0)
            y--;
        else if (state == Direction.DOWN && y < space.getSizeY())
            y++;

        space.putObjectAt(getX(), getY(), this);
    }

    public int getCurrentFloor() {
        return space.getSizeY() - y - 1;
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

    public void addTask(Task task) {
        //If possible, join task with other tasks with same origin and same destination direction
        for (Task t : tasks) {
            if (!task.similarTo(t)) continue;
            if (t != currentTask && goingToOrigin) {
                System.out.println(getLocalName() + " joined tasks: " + t + " and " + task);
                if (t.getDestinationFloor() != task.getDestinationFloor()) {
                    t.addNumPeople(task.getNumPeople());
                    t.addDestinationFloor(task.getDestinationFloor(), task.getNumPeople());
                    t.incrementNumCalls();
                } else
                    t.setNumPeople(t.getNumPeople() + task.getNumPeople());
                return;
            }
        }

        tasks.add(task);
    }

    public Task executeTasks() {
        if (tasks.isEmpty()) {
            state = Direction.STOPPED;
            goingToOrigin = false;
            currentTask = null;
            return null;
        }

        Task doneTask = null;
        if (currentTask == null) {
            currentTask = tasks.get(0);
            goingToOrigin = true;
        }

        if (goingToOrigin) {
            findState(currentTask);
            // Check if the lift got to the origin floor
            if (currentTask.getOriginFloor() == getCurrentFloor()) {
                // Send new request if not all people got in
                if (currentTask.getNumAllPeople() > max_capacity) {
                    System.out.println("Insufficient capacity for " + currentTask + ", making new request");
                    if (currentTask.getNumPeople() > max_capacity)
                        currentTask.setNumPeople(currentTask.getNumPeople() - max_capacity);
                    else {
                        // everyone from the first call got on the lift, make new call for the rest
                        doneTask = currentTask;
                        doneTask.removeNumPeople();
                        doneTask.removeDestinationFloor();
                    }
                } else
                    currentTask.setNumPeople(0);
                if (doneTask == null)
                    doneTask = currentTask;
            }
        } else if (currentTask.getDestinationFloor() == getCurrentFloor()) {
            System.out.println(getLocalName() + " answered " + currentTask);
            if (currentTask.getNumPeople() == 0 && currentTask.getDestinations().size() > 1) {
                currentTask.removeDestinationFloor();
                if (currentTask.getNumPeopleSize() > 1)
                    currentTask.removeNumPeople();
            } else {
                tasks.remove(0);
                if (tasks.size() > 0) {
                    currentTask = tasks.get(0);
                    goingToOrigin = true;
                    findState(currentTask);
                } else {
                    state = Direction.STOPPED;
                    goingToOrigin = false;
                    currentTask = null;
                }
            }
        }
        return doneTask;
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
