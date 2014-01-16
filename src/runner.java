import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.BeatsException;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Scenario;

/**
 * Created by gomes on 1/15/14.
 */
public class runner {

    public static void main(String [] args){

        Scenario scenario = null;


        try {
            String configname = "C:\\Users\\gomes\\code\\L0\\lp_ramp_metering\\data\\_smalltest_MPC.xml";
            scenario = ObjectFactory.createAndLoadScenario(configname);
        } catch (BeatsException e) {
            e.printStackTrace();
        }

        //VehicleTypeSet vt_set = scenario.getVehicleTypeSet();
        Network network = scenario.getNetworkSet().getNetwork().get(0);
        DemandSet demand_set = scenario.getDemandSet();
        //ActuatorSet actuators = scenario.getActuatorSet();
        FundamentalDiagramSet fds = scenario.getFundamentalDiagramSet();
        SplitRatioSet splitratios = scenario.getSplitRatioSet();



    }

}
