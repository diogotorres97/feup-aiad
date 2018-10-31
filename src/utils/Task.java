package utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private int originFloor;
    private double startTime = -1;
    private double endTime = -1; //Pickup time

    private TreeMap<Integer, Integer> destFloorPeople; //Key = destination floor, Value = nr. of people to drop off

    public Task(int originFloor, int destinationFloor, int nr_people) {
        this.originFloor = originFloor;
        if (originFloor < destinationFloor)
            this.destFloorPeople = new TreeMap<>();
        else
            this.destFloorPeople = new TreeMap<>(Collections.reverseOrder());
        this.destFloorPeople.put(destinationFloor, nr_people);
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

    public void setStartTime(long startTime) {
        if (this.startTime == -1 || startTime < this.startTime)
            this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public double getWaitingTime() {
        return this.endTime - this.startTime;
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

    public void removeTail() {
        Map.Entry<Integer, Integer> temp = destFloorPeople.firstEntry();
        destFloorPeople.clear();
        destFloorPeople.put(temp.getKey(), temp.getValue());
    }

    public boolean similarTo(Task t) {
        return t.getOriginFloor() == getOriginFloor() && t.getDirection() == getDirection();
    }
}
