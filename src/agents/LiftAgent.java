package agents;

import sajas.core.Agent;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import static java.awt.Color.BLUE;

public class LiftAgent extends Agent implements Drawable {
    private Object2DGrid space;
    private int x;
    private int y;

    public LiftAgent(int x, int y, Object2DGrid space) {
        this.x = x;
        this.y = y;
        this.space = space;
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
