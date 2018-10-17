package agents;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.domain.DFService;
import sajas.proto.ContractNetInitiator;
import utils.call.CallStrategy;
import utils.call.MidCallStrategy;
import utils.call.MorningCallStrategy;

import java.util.Vector;

public class BuildingAgent extends Agent {
    private int numFloors;
    private CallStrategy callStrategy;

    public BuildingAgent(int numFloors, int callStrategy) {
        this.numFloors = numFloors;
        switch(callStrategy) {
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
        addBehaviour(new FIPAContractNetInit(this, new ACLMessage(ACLMessage.CFP)));
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

    //TODO: delete, just for experimenting
    public BuildingAgent getSelf() {
        return this;
    }

    private class genericBehaviour extends Behaviour {
        private int x;

        @Override
        public void action() {
            x = callStrategy.generateOriginFloor();
        }

        @Override
        public boolean done() {
            return true;
        }
    }

    private class FIPAContractNetInit extends ContractNetInitiator {

        public FIPAContractNetInit(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        protected Vector prepareCfps(ACLMessage cfp) {
            Vector v = new Vector();
            
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("lift");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(getSelf(), template);
                System.out.println(result.length);
                for(int i=0; i<result.length; ++i)
                    cfp.addReceiver(result[i].getName());
            } catch(FIPAException fe) {
                fe.printStackTrace();
            }

            v.add(cfp);

            return v;
        }

        protected void handleAllResponses(Vector responses, Vector acceptances) {

            System.out.println("got " + responses.size() + " responses!");

            for(int i=0; i<responses.size(); i++) {
                ACLMessage msg = ((ACLMessage) responses.get(i)).createReply();
                msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL); // OR NOT!
                acceptances.add(msg);
            }
        }

        protected void handleAllResultNotifications(Vector resultNotifications) {
            System.out.println("got " + resultNotifications.size() + " result notifs!");
        }

    }
}
