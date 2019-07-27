package utils.evaluation;

import agents.LiftAgent;

public class SmallestTimeNumpad extends SmallestTime {

    public SmallestTimeNumpad(LiftAgent agent) {
        super(agent);
        this.numpad = true;
    }

}
