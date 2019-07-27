package utils.evaluation;

import agents.LiftAgent;
import utils.Direction;
import utils.Task;

public abstract class SmallestTime extends CallEvaluation {

    boolean numpad;

    SmallestTime(LiftAgent agent) {
        super(agent);
    }

    private int estimateDestFloor(Task t) {
        int destFloor = 0;
        int numFloors = agent.getTotalFloors();

        if (t.getOriginFloor() == 0)
            destFloor = ((numFloors + 1) / 2);
        else if (t.getDirection() == Direction.UP)
            destFloor = ((numFloors + 1) - t.getOriginFloor()) / 2 + t.getOriginFloor();

        return destFloor;
    }

    @Override
    public int evaluate(Task task) {
        int score = 0;

        int currentFloor = agent.getCurrentFloor();
        int destFloor = 0;
        boolean addToTask = false;

        int STOP_TIME = agent.getStopTime();
        int LIFT_SPEED = agent.getSpeed();

        for (Task t : agent.getTasks()) {
            if (t == agent.getCurrentTask() && !agent.isGoingToOrigin()) {
                //If t is the current task and the lift isnt going to origin then sum only the time until finish the current task
                int size = t.getDestFloorPeopleSize() - 1;
                destFloor = t.getDestinations().get(size);
                score += Math.abs(currentFloor - destFloor) * LIFT_SPEED + size * STOP_TIME;
            } else if (t == agent.getCurrentTask() && agent.isGoingToOrigin()) {
                //If t is the current task and the lift is going to origin then sum the time to arrive to origin plus the time until finish the current task
                int size = t.getDestFloorPeopleSize() - 1;
                destFloor = t.getDestinations().get(size);
                score += Math.abs(currentFloor - t.getOriginFloor()) * LIFT_SPEED + STOP_TIME;
                score += Math.abs(t.getOriginFloor() - destFloor) * LIFT_SPEED + size * STOP_TIME;
            } else if (t != agent.getCurrentTask() && task.similarTo(t)) {
                //If t isn't the current task and i can merge the task then sum the time to arrive to origin
                addToTask = true;
                score += Math.abs(destFloor - t.getOriginFloor()) * LIFT_SPEED + STOP_TIME;
                break;
            } else if (t != agent.getCurrentTask() && !task.similarTo(t)) {
                //If t isn't the current task and i cannot merge the task then sum the time to arrive to origin plus the time until finish the current task
                score += Math.abs(destFloor - t.getOriginFloor()) * LIFT_SPEED + STOP_TIME;
                int size = t.getDestFloorPeopleSize() - 1;
                if (numpad) {
                    destFloor = t.getDestinations().get(size);
                } else {
                    destFloor = estimateDestFloor(t);
                }
                score += Math.abs(t.getOriginFloor() - destFloor) * LIFT_SPEED + size * STOP_TIME;
            }
        }

        if (!addToTask) {
            score += Math.abs(destFloor - task.getOriginFloor()) * LIFT_SPEED + STOP_TIME;
        }

        if (agent.getTasks().isEmpty()) {
            score += Math.abs(currentFloor - task.getOriginFloor()) * LIFT_SPEED + STOP_TIME;
        }

        System.out.println("LIFT: " + agent.getName() + "SCORE: " + score);
        return score;
    }
}
