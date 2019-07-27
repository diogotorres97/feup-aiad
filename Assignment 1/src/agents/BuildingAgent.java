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
import sajas.proto.AchieveREResponder;
import sajas.proto.ContractNetInitiator;
import sajas.proto.SubscriptionInitiator;
import uchicago.src.sim.space.Object2DGrid;
import utils.Floor;
import utils.Task;
import utils.call.CallStrategy;
import utils.call.MidCallStrategy;
import utils.call.MorningCallStrategy;

import java.io.IOException;
import java.util.Vector;

import static java.lang.Integer.MAX_VALUE;

public class BuildingAgent extends Agent {
    private CallStrategy callStrategy;
    private Vector<AID> lifts;
    private Vector<Floor> floors;
    private Object2DGrid space;
    private Vector<Task> newTasks;
    private int totalCalls;

    public BuildingAgent(int numFloors, int callStrategy, int lift_speed, int max_capacity, Object2DGrid space) {
        this.lifts = new Vector<>();
        this.space = space;
        this.floors = new Vector<>(numFloors);
        this.newTasks = new Vector<>();
        this.totalCalls = 0;
        for (int i = 0; i < numFloors; ++i) {
            Floor floor = new Floor(0, numFloors - 1 - i, lift_speed);
            this.floors.add(floor);
            space.putObjectAt(floor.getX(), floor.getY(), floor);
        }

        switch (callStrategy) {
            case 0:
                this.callStrategy = new MorningCallStrategy(numFloors, max_capacity);
                break;
            case 1:
            default:
                this.callStrategy = new MidCallStrategy(numFloors, max_capacity);
                break;
        }
    }

    @Override
    protected void setup() {
        initialLiftAgentSearch();
        liftAgentSubscription();
        recaller();
    }

    //Initiates task reallocation behavior
    private void recaller() {
        addBehaviour(new Recaller(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
    }

    private DFAgentDescription getDFAgentDescriptionTemplate() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("lift");
        template.addServices(sd);
        return template;
    }

    private void initialLiftAgentSearch() {
        DFAgentDescription template = getDFAgentDescriptionTemplate();
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            System.out.println(result.length);
            for (DFAgentDescription aResult : result)
                System.out.println(this.lifts.add(aResult.getName()));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void liftAgentSubscription() {
        DFAgentDescription template = getDFAgentDescriptionTemplate();
        addBehaviour(new LiftAgentSubscription(this, template));
    }

    @Override
    protected void takeDown() {
        System.out.println("Taking down building");
    }

    private Vector<AID> getLifts() {
        return lifts;
    }

    //New task initiators: responsible for running the allocation protocol
    public void newCall() {
        ++totalCalls;
        if (newTasks.isEmpty())
            newCall(callStrategy.generateTask());
        else
            newCall(newTasks.remove(0));
    }

    private void newCall(Task task) {
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        try {
            msg.setContentObject(task);
        } catch (IOException e) {
            e.printStackTrace();
        }
        floors.get(task.getOriginFloor()).activate(task.getDirection());
        addBehaviour(new CallGenerator(this, msg));
    }

    public int getTotalCalls() {
        return totalCalls;
    }

    //Private class responsible for task allocation
    private class CallGenerator extends ContractNetInitiator {

        CallGenerator(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        @Override
        protected Vector prepareCfps(ACLMessage cfp) {
            Vector<ACLMessage> v = new Vector<>();

            for (AID aid : ((BuildingAgent) myAgent).getLifts())
                cfp.addReceiver(aid);

            v.add(cfp);

            return v;
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            //Get min
            int min = MAX_VALUE;
            for (Object response : responses) {
                int curr = MAX_VALUE;
                try {
                    curr = (Integer) ((ACLMessage) response).getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }

                if (curr < min) min = curr;
            }

            //Choose first with min value
            boolean chosen = false;
            for (Object response : responses) {
                ACLMessage current = (ACLMessage) response;
                try {
                    ACLMessage msg = current.createReply();
                    if (!chosen && (Integer) current.getContentObject() == min) {
                        msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        chosen = true;
                    } else {
                        msg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    }
                    acceptances.add(msg);
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void handleAllResultNotifications(Vector resultNotifications) {
            //System.out.println("got " + resultNotifications.size() + " result notifs!");
        }
    }


    //Private class responsible for being alert to late new agents
    private class LiftAgentSubscription extends SubscriptionInitiator {

        LiftAgentSubscription(Agent agent, DFAgentDescription dfad) {
            super(agent, DFService.createSubscriptionMessage(agent, getDefaultDF(), dfad, null));
        }

        @Override
        protected void handleInform(ACLMessage inform) {
            try {
                DFAgentDescription[] dfds = DFService.decodeNotification(inform.getContent());
                BuildingAgent myBuilding = (BuildingAgent) myAgent;
                for (DFAgentDescription dfd : dfds) {
                    AID agent = dfd.getName();
                    if (!myBuilding.getLifts().contains(agent)) {
                        myBuilding.getLifts().add(agent);
                        System.out.println("New agent in town: " + agent.getLocalName() + ", now have " + myBuilding.getLifts().size());
                    }
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }


    //Private class responsible for reallocating tasks
    class Recaller extends AchieveREResponder {

        Recaller(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        protected ACLMessage handleRequest(ACLMessage request) {
            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.AGREE);

            try {
                Task task = (Task) request.getContentObject();
                ((BuildingAgent) myAgent).newTasks.add(task);
            } catch (UnreadableException e) {
                e.printStackTrace();
            }

            return reply;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {
            ACLMessage result = request.createReply();
            result.setContent("It is done");
            return result;
        }

    }
}
