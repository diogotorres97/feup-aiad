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
import utils.Task;
import utils.evaluation.CallEvaluation;
import utils.evaluation.Closest;
import utils.evaluation.SmallestTimeEstimate;
import utils.evaluation.SmallestTimeNumpad;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class LiftAgent extends Agent implements Drawable {
    private Object2DGrid space;
    private int max_capacity;
    private CallEvaluation evaluator;
    public int x;
    public int y;

    public LiftAgent(int x, int y, int strategy, int max_capacity, Object2DGrid space) {
        this.x = x;
        this.y = y;
        this.space = space;
        this.max_capacity = max_capacity;
        switch(strategy) {
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
        } catch(FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Lift registered!");
        addBehaviour(new CallAnswerer(this, MessageTemplate.MatchPerformative(ACLMessage.CFP)));
    }

    public void updatePosition() {
    }

    class CallAnswerer extends ContractNetResponder {

        public CallAnswerer(Agent a, MessageTemplate mt) {
            super(a, mt);
        }


        @Override
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
            try {
                reply.setContentObject(evaluator.evaluate(task));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return reply;
        }

        @Override
        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
            System.out.println(myAgent.getLocalName() + " got a reject...");
        }

        @Override
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
}
