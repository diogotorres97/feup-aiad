package agents;

import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;

import java.util.Random;

public class BuildingAgent extends Agent {
    private int numFloors;

    public BuildingAgent(int numFloors) {
        this.numFloors = numFloors;
    }
    @Override
    protected void setup() {
        System.out.println("Setup done");
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

    private class genericBehaviour extends Behaviour {
        private int x;
        private Random rand = new Random();

        @Override
        public void action() {
            x = rand.nextInt(numFloors);
            System.out.println("Generated " + x);
        }

        @Override
        public boolean done() {
            System.out.println("Oi x " + x);
            return x > 8;
        }
    }
}
