package utils.call;

import utils.Task;

import java.util.Random;

public abstract class CallStrategy {
    protected int numFloors;
    protected int max_capacity;
    protected double PROB_BOTTOM_FLOOR_ORIGIN;
    protected double PROB_OTHER_FLOORS_ORIGIN;
    protected double PROB_BOTTOM_FLOOR_DEST;
    protected double PROB_OTHER_FLOORS_DEST;
    Random rng;

    private int generateOriginFloor() {
        double random = rng.nextDouble();

        if (random < PROB_BOTTOM_FLOOR_ORIGIN)
            return 0;
        else
            return (int) Math.ceil((random - PROB_BOTTOM_FLOOR_ORIGIN) / PROB_OTHER_FLOORS_ORIGIN);
    }


    private int generateDestinationFloor() {
        double random = rng.nextDouble();

        if (random < PROB_BOTTOM_FLOOR_DEST)
            return 0;
        else
            return (int) Math.ceil((random - PROB_BOTTOM_FLOOR_DEST) / PROB_OTHER_FLOORS_DEST);
    }

    public Task generateTask() {
        int origin = generateOriginFloor();
        int destination;
        do {
            destination = generateDestinationFloor();
        } while (destination == origin);

        int nr_people = rng.nextInt(max_capacity) + 1;

        return new Task(origin, destination, nr_people);
    }
}
