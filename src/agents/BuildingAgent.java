package agents;

import jade.core.AID;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import sajas.core.Agent;
import sajas.domain.DFService;
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
    private int numFloors;
    private CallStrategy callStrategy;
    private Vector<AID> lifts;
    private Vector<Floor> floors;
    private Object2DGrid space;

    public BuildingAgent(int numFloors, int callStrategy, Object2DGrid space) {
        this.numFloors = numFloors;
        this.lifts = new Vector<>();
        this.space = space;
        this.floors = new Vector<>(numFloors);
        for (int i = 0; i < numFloors; ++i) {
            Floor floor = new Floor(0, numFloors - 1 - i);
            this.floors.add(floor);
            space.putObjectAt(floor.getX(), floor.getY(), floor);
        }

        switch (callStrategy) {
            case 0:
                this.callStrategy = new MorningCallStrategy(numFloors);
                break;
            case 1:
            default:
                this.callStrategy = new MidCallStrategy(numFloors);
                break;
        }
    }

    @Override
    protected void setup() {
        initialLiftAgentSearch();
        liftAgentSubscription();
        System.out.println("Setting up building");
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
        System.out.println("Taking down");
    }

    public int getNumFloors() {
        return numFloors;
    }

    public void setNumFloors(int numFloors) {
        this.numFloors = numFloors;
    }

    private Vector<AID> getLifts() {
        return lifts;
    }

    public void newCall() {
        newCall(callStrategy.generateTask());
    }

    public void newCall(Task task) {
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        try {
            msg.setContentObject(task);
        } catch (IOException e) {
            e.printStackTrace();
        }
        floors.get(task.getOriginFloor()).activate();
        addBehaviour(new CallGenerator(this, msg));
    }

    private class CallGenerator extends ContractNetInitiator {

        public CallGenerator(Agent a, ACLMessage msg) {
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

            //System.out.println("got " + responses.size() + " responses!");

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
}
