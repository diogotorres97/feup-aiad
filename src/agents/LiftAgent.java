package agents;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.domain.DFService;
import sajas.proto.ContractNetResponder;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Multi2DGrid;
import uchicago.src.sim.space.Object2DGrid;
import utils.Task;

import java.io.IOException;
import java.util.Random;

import static java.awt.Color.BLUE;

public class LiftAgent extends Agent implements Drawable {
    private int speed;
    private Multi2DGrid space;
    public int x;
    public int y;

    public LiftAgent(int x, int y, int speed, Multi2DGrid space) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.space = space;
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
        } catch(FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Lift registered!");
        addBehaviour(new FIPAContractNetResp(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
    }

    class FIPAContractNetResp extends ContractNetResponder {

        public FIPAContractNetResp(Agent a, MessageTemplate mt) {
            super(a, mt);
        }


        protected ACLMessage handleCfp(ACLMessage cfp) {
            Task task = null;
            try {
                task = (Task)cfp.getContentObject();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
            System.out.println(getLocalName() + " got request for task: " + task.getOriginFloor() + "|" + task.getDestinationFloor());
            ACLMessage reply = cfp.createReply();
            reply.setPerformative(ACLMessage.PROPOSE);
            Random r = new Random();
            try {
                reply.setContentObject(r.nextInt(10));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return reply;
        }

        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
            System.out.println(myAgent.getLocalName() + " got a reject...");
        }

        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
            System.out.println(myAgent.getLocalName() + " got an accept!");
            ACLMessage result = accept.createReply();
            result.setPerformative(ACLMessage.INFORM);
            result.setContent("this is the result");

            return result;
        }

    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch(FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void draw(SimGraphics simGraphics) {
        simGraphics.drawFastCircle(BLUE);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}
