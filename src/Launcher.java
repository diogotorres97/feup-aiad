import agents.BuildingAgent;
import agents.LiftAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import sajas.core.Agent;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.reflector.ListPropertyDescriptor;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Multi2DGrid;
import uchicago.src.sim.space.Object2DGrid;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

public class Launcher extends Repast3Launcher {
    public static boolean BATCH_MODE = false;

    private int LIFT_MAX_CAPACITY = 6;
    private int CALL_FREQUENCY = 20;
    private int NUM_FLOORS = 5;
    private int NUM_LIFTS = 4;
    private int LIFT_STRATEGY = 0;
    private int CALL_STRATEGY = 0;

    private ContainerController mainContainer;

    private DisplaySurface dsurf;
    private Multi2DGrid space;
    private ArrayList<Agent> agentList;

    private BuildingAgent building;

    public static void main(String[] args) {
        SimInit init = new SimInit();
        init.setNumRuns(1);   // works only in batch mode
        init.loadModel(new Launcher(), null, Launcher.BATCH_MODE);
    }

    @Override
    public String[] getInitParam() {
        return new String[]{"CALL_STRATEGY", "LIFT_MAX_CAPACITY", "CALL_FREQUENCY", "LIFT_STRATEGY", "NUM_FLOORS", "NUM_LIFTS"};
    }

    @Override
    public String getName() {
        return "AIAD Group 50 - Lift Management";
    }

    @Override
    public void setup() {
        super.setup();

        // Setup combobox parameter for lift behavior strategy
        Hashtable h1 = new Hashtable();
        h1.put(new Integer(0), "Traditional");
        h1.put(new Integer(1), "Closest w/ estimate");
        h1.put(new Integer(2), "Closest w/o estimate");
        ListPropertyDescriptor pd1 = new ListPropertyDescriptor("LIFT_STRATEGY", h1);

        // Setup combobox parameter for calling behavior strategy
        Hashtable h2 = new Hashtable();
        h2.put(new Integer(0), "Morning traffic");
        h2.put(new Integer(1), "Mid-day traffic");
        ListPropertyDescriptor pd2 = new ListPropertyDescriptor("CALL_STRATEGY", h2);

        descriptors.put("LIFT_STRATEGY", pd1);
        descriptors.put("CALL_STRATEGY", pd2);

        if(dsurf != null) dsurf.dispose();
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
        space = new Multi2DGrid(NUM_LIFTS, NUM_FLOORS, false);

        launchAgents();
    }

    private void buildDisplay() {
        // create displays, charts
        Object2DDisplay agentDisplay = new Object2DDisplay(space);
        agentDisplay.setObjectList(agentList);

        dsurf.addDisplayable(agentDisplay, "agents");
        addSimEventListener(dsurf);
        dsurf.display();
    }

    private void buildSchedule() {
        // build the schedule
        getSchedule().scheduleActionAtInterval(1, dsurf, "updateDisplay", Schedule.LAST);
        getSchedule().scheduleActionAtInterval(CALL_FREQUENCY, new BasicAction() {
            @Override
            public void execute() {
                building.newCall();
            }
        });
        //TODO: Just experimenting, delete later
        getSchedule().scheduleActionAtInterval(25, new BasicAction() {
            @Override
            public void execute() {
                LiftAgent agent = (LiftAgent)agentList.get(0);
                space.putObjectAt(agent.getX(), agent.getY(), null);
                Random rng = new Random(System.currentTimeMillis());
                agent.y = rng.nextInt(space.getSizeY());
                space.putObjectAt(agent.getX(), agent.getY(), agent);
            }
        });
    }

    @Override
    protected void launchJADE() {
        //NOTE TO SELF: THIS IS CALLED BEFORE SETUP()
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        mainContainer = rt.createMainContainer(p);
    }

    private void launchAgents() {
        for(int i = 0; i < NUM_LIFTS; ++i) {
            LiftAgent agent = new LiftAgent(i, NUM_FLOORS-1, space);
            space.putObjectAt(agent.getX(), agent.getY(), agent);
            try {
                mainContainer.acceptNewAgent("lift"+i, agent).start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
            agentList.add(agent);
        }

        building = new BuildingAgent(NUM_FLOORS, CALL_STRATEGY);
        try {
            mainContainer.acceptNewAgent("spy", building).start();
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

    public int getCALL_FREQUENCY() {
        return CALL_FREQUENCY;
    }

    public void setCALL_FREQUENCY(int CALL_FREQUENCY) {
        this.CALL_FREQUENCY = CALL_FREQUENCY;
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

    public int getLIFT_STRATEGY() {
        return LIFT_STRATEGY;
    }

    public void setLIFT_STRATEGY(int LIFT_STRATEGY) {
        this.LIFT_STRATEGY = LIFT_STRATEGY;
    }

    public int getCALL_STRATEGY() {
        return CALL_STRATEGY;
    }

    public void setCALL_STRATEGY(int CALL_STRATEGY) {
        this.CALL_STRATEGY = CALL_STRATEGY;
    }
}