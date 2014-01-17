import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.BeatsException;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Scenario;

/**
 * Created by gomes on 1/16/14.
 */
public class Runner {

    public static void main(String [] args){

        Scenario scenario = null;
        try {
            String configname = "C:\\Users\\gomes\\code\\research\\lp_ramp_metering\\data\\15S_20131002_db.xml";
            scenario = ObjectFactory.createAndLoadScenario(configname);
            scenario.initialize(5d,0d,86400d,1);
        } catch (BeatsException e) {
            e.printStackTrace();
        }


        // constant information
        Network network = scenario.getNetworkSet().getNetwork().get(0);
        ActuatorSet actuators = scenario.getActuatorSet();
        FundamentalDiagramSet fds = scenario.getFundamentalDiagramSet();

        // construct solver
        LPBuilder lpbuilder = new LPBuilder(network,fds,actuators);

        // dynamic information
        DemandSet demand_set = scenario.getDemandSet();
        SplitRatioSet splitratios = scenario.getSplitRatioSet();
        InitialDensitySet ic = scenario.getInitialDensitySet();

        // solve
        lpbuilder.problemA(ic,demand_set,splitratios);


    }
}
