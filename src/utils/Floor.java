package utils;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import javax.swing.text.Style;
import java.awt.*;

public class Floor implements Drawable {
    private final int lift_speed;
    private int x;
    private int y;
    private int counter = 0;
    private Direction direction;

    public Floor(int x, int y, int lift_speed) {
        this.x = x;
        this.y = y;
        this.lift_speed = lift_speed;
        this.direction = null;
    }

    @Override
    public void draw(SimGraphics simGraphics) {
        simGraphics.setFont(new Font("Arial", Font.BOLD, 40));
        if (counter > 0) {
            //simGraphics.drawRect(Color.GREEN);
            simGraphics.drawStringInRect(Color.GREEN,Color.BLUE,this.direction == Direction.DOWN ? "v" : "^");
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

    public void activate(Direction direction) {
        this.direction = direction;
        ++counter;
    }
}
