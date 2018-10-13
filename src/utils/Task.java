package utils;

import java.io.Serializable;

public class Task implements Serializable {
    private int originFloor;
    private int destinationFloor;

    public Task(int originFloor, int destinationFloor) {
        this.originFloor = originFloor;
        this.destinationFloor = destinationFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public void setDestinationFloor(int destinationFloor) {
        this.destinationFloor = destinationFloor;
    }

    public int getOriginFloor() {
        return originFloor;
    }

    public void setOriginFloor(int originFloor) {
        this.originFloor = originFloor;
    }

    @Override
    public String toString() {
        return String.format("Origin: %d, Destination: %d", this.originFloor, this.destinationFloor);
    }
}
