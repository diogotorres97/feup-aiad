package utils.call;

import java.util.Random;

public abstract class CallStrategy {
    protected int numFloors;
    protected double PROB_BOTTOM_FLOOR_ORIGIN;
    protected double PROB_OTHER_FLOORS_ORIGIN;
    protected double PROB_BOTTOM_FLOOR_DEST;
    protected double PROB_OTHER_FLOORS_DEST;
    Random rng;

    public int generateOriginFloor() {
        double random = rng.nextDouble();

        if (random < PROB_BOTTOM_FLOOR_ORIGIN)
            return 0;
        else
            return (int)Math.ceil((random - PROB_BOTTOM_FLOOR_ORIGIN)/PROB_OTHER_FLOORS_ORIGIN);
    }


    public int generateDestinationFloor() {
        double random = rng.nextDouble();

        if (random < PROB_BOTTOM_FLOOR_DEST)
            return 0;
        else
            return (int)Math.ceil((random - PROB_BOTTOM_FLOOR_DEST)/PROB_OTHER_FLOORS_DEST);
    }
}
