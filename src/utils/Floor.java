package utils;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.*;

public class Floor implements Drawable {
    private int x;
    private int y;
    private final int lift_speed;
    private int counter = 0;

    public Floor(int x, int y, int lift_speed) {
        this.x = x;
        this.y = y;
        this.lift_speed = lift_speed;
    }

    @Override
    public void draw(SimGraphics simGraphics) {
        //TODO: Generalize this better
        if (counter > 0) {
            simGraphics.drawRect(Color.GREEN);
            counter = (counter + 1) % lift_speed;
        } else
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
