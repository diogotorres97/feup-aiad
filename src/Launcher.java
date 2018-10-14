import agents.BuildingAgent;
import agents.LiftAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import sajas.core.Agent;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;

import java.util.ArrayList;

public class Launcher extends Repast3Launcher {
    public static boolean BATCH_MODE = false;

    private int LIFT_MAX_CAPACITY = 6;
    private int LIFT_SPEED = 20;
    private int NUM_FLOORS = 5;
    private int NUM_LIFTS = 4;

    private ContainerController mainContainer;

    private DisplaySurface dsurf;
    private Object2DGrid space;
    private ArrayList<Agent> agentList;

    public static void main(String[] args) {
        SimInit init = new SimInit();
        init.setNumRuns(1);   // works only in batch mode
        init.loadModel(new Launcher(), null, Launcher.BATCH_MODE);
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
    public void setup() {
        super.setup();

        dsurf = new DisplaySurface(this,"Lift Management");
        registerDisplaySurface("Lift Management", dsurf);
    }

    public void begin() {
        super.begin();
        buildModel();
        buildDisplay();
        buildSchedule();
    }

    private void buildModel() {
        // create and store agents
        // create space, data recorders
        agentList = new ArrayList<>();
        space = new Object2DGrid(NUM_LIFTS, NUM_FLOORS);
        LiftAgent agent = new LiftAgent(1, 3, space);
        space.putObjectAt(agent.getX(), agent.getY(), agent);
        agentList.add(agent);
    }

    private void buildDisplay() {
        // create displays, charts
        Object2DDisplay agentDisplay = new Object2DDisplay(space);
        agentDisplay.setObjectList(agentList);

        dsurf.addDisplayableProbeable(agentDisplay, "agents");
        addSimEventListener(dsurf);
        dsurf.display();
    }

    private void buildSchedule() {
        // build the schedule
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