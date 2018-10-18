package agents;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import sajas.core.Agent;
import sajas.domain.DFService;
import sajas.proto.ContractNetInitiator;
import utils.Task;
import utils.call.CallStrategy;
import utils.call.MidCallStrategy;
import utils.call.MorningCallStrategy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.Vector;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Integer.MAX_VALUE;

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
    protected void setup() {}

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

    private class FIPAContractNetInit extends ContractNetInitiator {

        public FIPAContractNetInit(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        protected Vector prepareCfps(ACLMessage cfp) {
            Vector<ACLMessage> v = new Vector<ACLMessage>();

            //Could do this only once at setup, but this way more lifts can be incorporated at runtime
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("lift");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
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

            //Get min
            int min = MAX_VALUE;
            for(int i = 0; i < responses.size(); ++i) {
                int curr = MAX_VALUE;
                try {
                    curr = (Integer)((ACLMessage)responses.get(i)).getContentObject();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }

                if(curr < min) min = curr;
            }

            //Choose first with min value
            boolean chosen = false;
            for(int i=0; i<responses.size(); i++) {
                ACLMessage current = (ACLMessage)responses.get(i);
                try {
                    ACLMessage msg = current.createReply();
                    if(!chosen && (Integer)current.getContentObject() == min) {
                        msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        chosen = true;
                    }
                    else
                        msg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    acceptances.add(msg);
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void handleAllResultNotifications(Vector resultNotifications) {
            System.out.println("got " + resultNotifications.size() + " result notifs!");
        }

    }

    public void newCall() {
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        try {
            msg.setContentObject(new Task(callStrategy.generateOriginFloor(), callStrategy.generateDestinationFloor()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        addBehaviour(new FIPAContractNetInit(this, msg));
    }
}
