package agents;

import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import sajas.core.Agent;
import sajas.domain.DFService;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Multi2DGrid;
import uchicago.src.sim.space.Object2DGrid;

import static java.awt.Color.BLUE;

public class LiftAgent extends Agent implements Drawable {
    private Multi2DGrid space;
    private int x;
    private int y;

    public LiftAgent(int x, int y, Multi2DGrid space) {
        this.x = x;
        this.y = y;
        this.space = space;
    }

    @Override
    protected void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("lift");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            System.out.println("Registering "+getLocalName());
            DFService.register(this, dfd);
            System.out.println(getName()+"registered successfully!");
        } catch(FIPAException fe) {
            fe.printStackTrace();
        }
        //TODO: Behaviors?
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
