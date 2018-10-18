package utils;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.*;

public class Floor implements Drawable {
    private int x;
    private int y;

    public Floor(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(SimGraphics simGraphics) {
        simGraphics.drawRect(Color.RED);
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
