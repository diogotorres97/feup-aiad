package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private int originFloor;

    private TreeMap<Integer, Integer> destFloorPeople; //Key = destination floor, Value = nr. of people to drop off
    private int numCalls;

    public Task(int originFloor, int destinationFloor) {
        this.originFloor = originFloor;
        if (originFloor < destinationFloor)
            this.destFloorPeople = new TreeMap<>();
        else
            this.destFloorPeople = new TreeMap<>(Collections.reverseOrder());
        this.destFloorPeople.put(destinationFloor, 1);
        this.numCalls = 1;
    }

    public Task getClone() {
        try {
            return (Task) this.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String toString() {
        return originFloor + " " + destFloorPeople.keySet() + " " + destFloorPeople.values() + " " + getDirection();
    }

    public TreeMap<Integer, Integer> getDestFloorPeople() {
        return destFloorPeople;
    }

    public Direction getDirection() {
        return originFloor < getDestinationFloor() ? Direction.UP : Direction.DOWN;
    }

    public int getOriginFloor() {
        return originFloor;
    }

    public int getDestinationFloor() {
        return destFloorPeople.firstKey();
    }

    public void removeDestinationFloor() {
        destFloorPeople.remove(destFloorPeople.firstKey());
    }

    public ArrayList<Integer> getDestinations() {
        return new ArrayList<>(destFloorPeople.keySet());
    }

    public int getDestFloorPeopleSize() {
        return destFloorPeople.size();
    }

    public void addDestinationFloor(int destinationFloor, int numPeople) {
        destFloorPeople.put(destinationFloor, numPeople);
    }

    public int getNumPeople() {
        return destFloorPeople.get(destFloorPeople.firstKey());
    }

    public void setNumPeople(int numPeople) {
        destFloorPeople.put(destFloorPeople.firstKey(), numPeople);
    }

    public int getNumAllPeople() {
        return destFloorPeople.values().stream().mapToInt(aNumPeople -> aNumPeople).sum();
    }

    public void incrementNumCalls() {
        numCalls++;
    }

    public int getNumCalls() {
        return numCalls;
    }

    public void removeTail() {
        Map.Entry<Integer, Integer> temp = destFloorPeople.firstEntry();
        destFloorPeople.clear();
        destFloorPeople.put(temp.getKey(),temp.getValue());
    }
    public boolean similarTo(Task t) {
        return t.getOriginFloor() == getOriginFloor() && getDirection() == getDirection();
    }
}
