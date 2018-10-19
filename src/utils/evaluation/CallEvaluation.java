package utils.evaluation;

import agents.LiftAgent;
import utils.Task;

public abstract class CallEvaluation {
    protected LiftAgent agent;

    public CallEvaluation(LiftAgent agent) {
        this.agent = agent;
    }

    public abstract int evaluate(Task task);
}
