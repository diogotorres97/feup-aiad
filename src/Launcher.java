import agents.BuildingAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import sajas.core.Runtime;
import sajas.sim.repast3.Repast3Launcher;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.engine.SimInit;

public class Launcher extends Repast3Launcher {


    private ContainerController mainContainer;
    private ContainerController agentContainer;

    /**
     * Launching Repast3
     *
     * @param args
     */
    public static void main(String[] args) {
        SimInit init = new SimInit();
        init.setNumRuns(1);   // works only in batch mode
        init.loadModel(new Launcher(), null, true);
    }

    @Override
    public String[] getInitParam() {
        return new String[0];
    }

    @Override
    public String getName() {
        return "Service Consumer/Provider -- SAJaS Repast3 Test";
    }

    @Override
    protected void launchJADE() {

        Runtime rt = Runtime.instance();
        Profile p1 = new ProfileImpl();
        mainContainer = rt.createMainContainer(p1);

        if (true) {
            Profile p2 = new ProfileImpl();
            agentContainer = rt.createAgentContainer(p2);
        } else {
            agentContainer = mainContainer;
        }

        launchAgents();
    }

    private void launchAgents() {
        try {
            agentContainer.acceptNewAgent("spy", new BuildingAgent()).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

    }

}