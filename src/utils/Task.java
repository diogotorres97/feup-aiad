package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private int originFloor;
    private ArrayList<Integer> destinationFloors;
    private ArrayList<Integer> numPeople;
    private int numCalls;

    public Task(int originFloor, int destinationFloor) {
        this.originFloor = originFloor;
        this.destinationFloors = new ArrayList<>();
        this.destinationFloors.add(destinationFloor);
        this.numPeople = new ArrayList<>();
        this.numPeople.add(1);
        this.numCalls = 1;
    }

    public String toString() {
        return originFloor + " " + destinationFloors + " " + numPeople +  " " + getDirection();
    }

    public Direction getDirection() {
        return originFloor < getDestinationFloor() ? Direction.UP : Direction.DOWN;
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
        if (getDirection() == Direction.DOWN)
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
        for (Integer aNumPeople : numPeople) sum += aNumPeople;
        return sum;
    }

    public void incrementNumCalls() {
        numCalls++;
    }

    public int getNumCalls() {
        return numCalls;
    }

    public boolean similarTo(Task t) {
        return t.getOriginFloor() == getOriginFloor() && getDirection() == getDirection();
    }
}
