package utils;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.*;

public class Floor implements Drawable {
    private int x;
    private int y;
    private int counter = 0;

    public Floor(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(SimGraphics simGraphics) {
        if(counter > 0) {
            simGraphics.drawRect(Color.GREEN);
            counter = (counter + 1) % 15;
        }
        else
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

    public void activate() {
        counter++;
    }
}
