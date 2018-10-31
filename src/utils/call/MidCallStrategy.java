package utils.call;

import java.util.Random;

public class MidCallStrategy extends CallStrategy {

    public MidCallStrategy(int numFloors, int max_capacity) {
        this.numFloors = numFloors;
        this.max_capacity = max_capacity;
        PROB_BOTTOM_FLOOR_ORIGIN = 0.15;
        PROB_OTHER_FLOORS_ORIGIN = (1 - PROB_BOTTOM_FLOOR_ORIGIN) / (numFloors - 1);
        PROB_BOTTOM_FLOOR_DEST = 0.4;
        PROB_OTHER_FLOORS_DEST = (1 - PROB_BOTTOM_FLOOR_DEST) / (numFloors - 1);
        rng = new Random(System.currentTimeMillis());
    }
}
