import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Scenario;

/**
 * Created by gomes on 1/16/14.
 */
public class Runner {

    public static void main(String [] args){

        double dt = 5d;
        int num_time = 3;

        try {

            // load the scenario
            Scenario scenario = ObjectFactory.createAndLoadScenario("data\\15S_20131002_db.xml");
            scenario.initialize(dt,0d,num_time*dt,1);

            // constant information
            Network network = scenario.getNetworkSet().getNetwork().get(0);
            ActuatorSet actuators = scenario.getActuatorSet();
            FundamentalDiagramSet fds = scenario.getFundamentalDiagramSet();

            // construct solver
            LP_ramp_metering lpbuilder = new LP_ramp_metering(network,fds,actuators,num_time,dt);

            // dynamic information
            DemandSet demand_set = scenario.getDemandSet();
            SplitRatioSet split_ratios = scenario.getSplitRatioSet();
            InitialDensitySet ic = scenario.getInitialDensitySet();

            // solve
            lpbuilder.solve(ic, demand_set, split_ratios);

        } catch (Exception e) {
            System.err.print(e.getMessage());
            return;
        }
    }
}
