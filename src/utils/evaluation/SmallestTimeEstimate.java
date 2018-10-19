package utils.evaluation;

import agents.LiftAgent;
import utils.Task;

public class SmallestTimeEstimate extends CallEvaluation {

    public SmallestTimeEstimate(LiftAgent agent) {
        super(agent);
    }

    @Override
    public int evaluate(Task task) {
        return 0;
    }
}
