package agents;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.domain.DFService;
import utils.call.CallStrategy;
import utils.call.MidCallStrategy;
import utils.call.MorningCallStrategy;

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
        addBehaviour(new genericBehaviour());
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
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("lift");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(getSelf(), template);
                System.out.println(result.length);
                for(int i=0; i<result.length; ++i) {
                    System.out.println("Found " + result[i].getName());
                }
            } catch(FIPAException fe) {
                fe.printStackTrace();
            }
        }

        @Override
        public boolean done() {
            return true;
        }
    }
}
