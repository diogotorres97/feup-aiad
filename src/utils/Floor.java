package utils;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Floor implements Drawable {
    private static BufferedImage up = null;
    private static BufferedImage down = null;
    private int lift_speed;
    private int x;
    private int y;
    private int counter = 0;
    private Direction direction;

    static{
        try {
            up = ImageIO.read(new File("assets/up.png"));
            down = ImageIO.read(new File("assets/down.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            simGraphics.drawImageToFit(direction == Direction.DOWN ? Floor.down : Floor.up);
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
