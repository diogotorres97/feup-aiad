import agents.BuildingAgent;
import agents.LiftAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.reflector.ListPropertyDescriptor;
import uchicago.src.sim.analysis.DataRecorder;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.stream.Collectors;

public class Launcher extends Repast3Launcher {
    private static boolean BATCH_MODE = false;

    private int LIFT_MAX_CAPACITY = 6;
    private int CALL_FREQUENCY = 100;
    private int NUM_FLOORS = 5;
    private int NUM_LIFTS = 4;
    private int LIFT_SPEED = 15;
    private int LIFT_STRATEGY = 0;
    private int CALL_STRATEGY = 0;
    private int LIFT_STOP_TIME = 1;

    private ContainerController mainContainer;

    private DisplaySurface dsurf;
    private Object2DGrid space;
    private ArrayList<LiftAgent> agentList;

    private BuildingAgent building;

    private DataRecorder recorder;

    public static void main(String[] args) {
        SimInit init = new SimInit();
        init.setNumRuns(1);   // works only in batch mode
        init.loadModel(new Launcher(), null, Launcher.BATCH_MODE);
    }

    @Override
    public String[] getInitParam() {
        return new String[]{"CALL_FREQUENCY", "CALL_STRATEGY", "LIFT_MAX_CAPACITY", "LIFT_SPEED", "LIFT_STRATEGY", "NUM_FLOORS", "NUM_LIFTS"};
    }

    @Override
    public String getName() {
        return "AIAD Group 50 - Lift Management";
    }

    @Override
    public void setup() {
        super.setup();

        // Setup combobox parameter for lift behavior strategy
        Hashtable<Integer, String> h1 = new Hashtable<>();
        h1.put(0, "Traditional (closest)");
        h1.put(1, "Smallest time (up/down)");
        h1.put(2, "Smallest time (numpad)");
        ListPropertyDescriptor pd1 = new ListPropertyDescriptor("LIFT_STRATEGY", h1);

        // Setup combobox parameter for calling behavior strategy
        Hashtable<Integer, String> h2 = new Hashtable<>();
        h2.put(0, "Morning traffic");
        h2.put(1, "Mid-day traffic");
        ListPropertyDescriptor pd2 = new ListPropertyDescriptor("CALL_STRATEGY", h2);

        descriptors.put("LIFT_STRATEGY", pd1);
        descriptors.put("CALL_STRATEGY", pd2);

        if (dsurf != null) dsurf.dispose();
        dsurf = new DisplaySurface(this, "Lift Management");
        registerDisplaySurface("Lift Management", dsurf);
    }

    public void begin() {
        super.begin();
        buildModel();
        buildDisplay();
        buildSchedule();
    }

    private void buildModel() {
        agentList = new ArrayList<>();
        space = new Object2DGrid(NUM_LIFTS + 1, NUM_FLOORS);

        launchAgents();

        recorder = new DataRecorder("./data.txt", this);
        for (LiftAgent a : agentList) {
            //3nd parameter - Record all digits pre decimal separator
            //4nd parameter - Round to 3 digits post decimal separator
            recorder.addNumericDataSource(a.getLocalName() + "_occupation", a::getOccupationRatio, -1, 3);
            recorder.addNumericDataSource(a.getLocalName() + "_usage_rate", a::getUsageRate, -1, 3);
            recorder.addNumericDataSource(a.getLocalName() + "_min_call_time", a::getMinWaitingTime, -1, 3);
            recorder.addNumericDataSource(a.getLocalName() + "_max_call_time", a::getMaxWaitingTime, -1, 3);
        }

        recorder.addNumericDataSource("global_avg_wait_time", () -> agentList.stream().mapToLong(LiftAgent::getTotalTaskTime).sum() * 1.0 / building.getTotalCalls());
        recorder.addNumericDataSource("global_min_wait_time", () -> Collections.min(agentList.stream().map(LiftAgent::getMinWaitingTime).collect(Collectors.toList())), -1, 3);
        recorder.addNumericDataSource("global_max_wait_time", () -> Collections.max(agentList.stream().map(LiftAgent::getMaxWaitingTime).collect(Collectors.toList())), -1, 3);
    }


    private void buildDisplay() {
        Object2DDisplay agentDisplay = new Object2DDisplay(space);

        dsurf.addDisplayable(agentDisplay, "agents");
        dsurf.setBackground(Color.WHITE);
        addSimEventListener(dsurf);
        dsurf.display();
    }

    private void buildSchedule() {
        getSchedule().scheduleActionAtInterval(1, dsurf, "updateDisplay", Schedule.LAST);
        getSchedule().scheduleActionAtInterval(CALL_FREQUENCY, new BasicAction() {
            @Override
            public void execute() {
                building.newCall();
            }
        });
        getSchedule().scheduleActionAtInterval(LIFT_SPEED, new BasicAction() {
            @Override
            public void execute() {
                for (LiftAgent agent : agentList) {
                    agent.updatePosition();
                    agent.executeTasks();
                }
            }
        });
        getSchedule().scheduleActionAtInterval(LIFT_SPEED, new BasicAction() {
            @Override
            public void execute() {
                recorder.record();
            }
        });
        getSchedule().scheduleActionAtEnd(recorder, "writeToFile");
    }

    @Override
    protected void launchJADE() {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        mainContainer = rt.createMainContainer(p);
    }

    private void launchAgents() {
        for (int i = 0; i < NUM_LIFTS; ++i) {
            LiftAgent agent = new LiftAgent(i + 1, NUM_FLOORS - 1,
                    LIFT_SPEED, LIFT_STOP_TIME, LIFT_STRATEGY, LIFT_MAX_CAPACITY,
                    space);
            space.putObjectAt(agent.getX(), agent.getY(), agent);
            try {
                mainContainer.acceptNewAgent("lift" + i, agent).start();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
            agentList.add(agent);
        }

        building = new BuildingAgent(NUM_FLOORS, CALL_STRATEGY, LIFT_SPEED, LIFT_MAX_CAPACITY, space);
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

    public int getLIFT_SPEED() {
        return LIFT_SPEED;
    }

    public void setLIFT_SPEED(int LIFT_SPEED) {
        this.LIFT_SPEED = LIFT_SPEED;
    }
}
