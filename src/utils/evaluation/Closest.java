package utils.evaluation;

import agents.LiftAgent;
import utils.Task;

public class Closest extends CallEvaluation {

    public Closest(LiftAgent agent) {
        super(agent);
    }

    @Override
    public int evaluate(Task task) {
        return 0;
    }
}
