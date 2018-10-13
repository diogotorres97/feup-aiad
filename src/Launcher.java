import agents.BuildingAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.engine.SimInit;

public class Launcher extends Repast3Launcher {
    private int LIFT_MAX_CAPACITY = 6;
    private int LIFT_SPEED = 20;
    private int NUM_FLOORS = 10;
    private int NUM_LIFTS = 4;

    private ContainerController mainContainer;

    public static void main(String[] args) {
        SimInit init = new SimInit();
        init.setNumRuns(1);   // works only in batch mode
        init.loadModel(new Launcher(), null, false);
    }

    @Override
    public String[] getInitParam() {
        return new String[]{"LIFT_MAX_CAPACITY", "LIFT_SPEED", "NUM_FLOORS", "NUM_LIFTS"};
    }

    @Override
    public String getName() {
        return "AIAD Group 50 - Lift Management";
    }

    @Override
    protected void launchJADE() {

        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        mainContainer = rt.createMainContainer(p);

        launchAgents();
    }

    private void launchAgents() {
        System.out.println(LIFT_MAX_CAPACITY + "|" + LIFT_SPEED + "|" + NUM_FLOORS + "|" + NUM_LIFTS);
        try {
            mainContainer.acceptNewAgent("spy", new BuildingAgent(NUM_FLOORS)).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

    }

    /*
     * Getters/setters necessary for repast GUI parametrization
     */

    public int getLIFT_MAX_CAPACITY() {
        return LIFT_MAX_CAPACITY;
    }

    public void setLIFT_MAX_CAPACITY(int LIFT_MAX_CAPACITY) {
        this.LIFT_MAX_CAPACITY = LIFT_MAX_CAPACITY;
    }

    public int getLIFT_SPEED() {
        return LIFT_SPEED;
    }

    public void setLIFT_SPEED(int LIFT_SPEED) {
        this.LIFT_SPEED = LIFT_SPEED;
    }

    public int getNUM_FLOORS() {
        return NUM_FLOORS;
    }

    public void setNUM_FLOORS(int NUM_FLOORS) {
        this.NUM_FLOORS = NUM_FLOORS;
    }

    public int getNUM_LIFTS() {
        return NUM_LIFTS;
    }

    public void setNUM_LIFTS(int NUM_LIFTS) {
        this.NUM_LIFTS = NUM_LIFTS;
    }
}