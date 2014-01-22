import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import edu.berkeley.path.beats.simulator.Scenario;
import net.sf.javailp.Result;

import java.io.PrintWriter;

/**
 * Created by gomes on 1/16/14.
 */
public class Runner {

    public static void main(String [] args){

        int num_time = 6;
        int num_time_cooldown = 3;
        double sim_dt_in_seconds = 5d;

        try {

            // load the scenario
            String config_file = "data\\bla_us.xml";
            //String config_file = "data\\15S_20131002_db.xml";

            Scenario scenario = ObjectFactory.createAndLoadScenario(config_file);
            scenario.initialize(sim_dt_in_seconds,0d,num_time*sim_dt_in_seconds,1);

            // construct solver
            LP_ramp_metering lp_ramp_metering = new LP_ramp_metering(scenario,num_time,num_time_cooldown,sim_dt_in_seconds);

            // validate
            if(!lp_ramp_metering.is_valid)
                throw new Exception(lp_ramp_metering.validation_message);

            // dynamic information
            DemandSet demand_set = scenario.getDemandSet();
            SplitRatioSet split_ratios = scenario.getSplitRatioSet();
            InitialDensitySet ic = scenario.getInitialDensitySet();

            // solve
            LP_solution opt = lp_ramp_metering.solve(ic, demand_set, split_ratios);

            // ge
            PrintWriter outX = new PrintWriter("matlab\\xopt.m");
            outX.print("function [n,l,f,r]=xopt()\n");
            outX.print(opt.print(true));
            outX.close();

        } catch (Exception e) {
            System.err.print(e.getMessage());
            return;
        }
    }
}
