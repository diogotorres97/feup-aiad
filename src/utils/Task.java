package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private int originFloor;
    private ArrayList<Integer> destinationFloors;
    private ArrayList<Integer> numPeople;
    private Direction direction;
    private int numCalls;

    public Task(int originFloor, int destinationFloor, ArrayList<Integer> numPeople) {
        this.originFloor = originFloor;
        this.destinationFloors = new ArrayList<>();
        this.destinationFloors.add(destinationFloor);
        this.numPeople = numPeople;
        this.numCalls = 1;
        this.direction = (originFloor < destinationFloor ? Direction.UP : Direction.DOWN);
    }

    public String toString() {
        return originFloor + " " + destinationFloors + " " + numPeople +  " " + direction;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getOriginFloor() {
        return originFloor;
    }

    public int getDestinationFloor() {
        return destinationFloors.get(0);
    }

    public void removeDestinationFloor() {
        destinationFloors.remove(0);
    }

    public ArrayList<Integer> getDestinations() {
        return destinationFloors;
    }

    public int getNumPeopleSize() {
        return numPeople.size();
    }

    public void addDestinationFloor(int destinationFloor) {
        this.destinationFloors.add(destinationFloor);
        Collections.sort(this.destinationFloors);
        if (direction == Direction.DOWN)
            Collections.reverse(this.destinationFloors);
    }

    public void removeNumPeople() {
        numPeople.remove(0);
    }

    public void setNumPeople(int numPeople) {
        this.numPeople.set(0, numPeople);
    }

    public void addNumPeople(int numPeople) {
        this.numPeople.add(numPeople);
    }
    public int getNumPeople() {
        return numPeople.get(0);
    }

    public int getNumAllPeople() {
        int sum = 0;
        for (int i = 0; i < numPeople.size(); i++)
            sum += numPeople.get(i);
        return sum;
    }

    public void incrementNumCalls() {
        numCalls++;
    }

    public int getNumCalls() {
        return numCalls;
    }
}
