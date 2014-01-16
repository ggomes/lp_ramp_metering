import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.BeatsException;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import net.sf.javailp.*;

/**
 * Created by gomes on 1/15/14.
 */
public class LPBuilder {

    public static void main(String [] args){

        Scenario scenario = null;


        try {
            String configname = "C:\\Users\\gomes\\code\\research\\lp_ramp_metering\\data\\_smalltest_MPC.xml";
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


    public LPBuilder(){

        Problem problem = new Problem();

        Linear linear = new Linear();
        linear.add(143, "x");
        linear.add(60, "y");
        problem.setObjective(linear, OptType.MAX);

    }

    public static void problemA(){
        SolverFactory factory = new SolverFactoryLpSolve(); // use lp_solve
        factory.setParameter(Solver.VERBOSE, 0);
        factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds

        /**
         * Constructing a Problem:
         * Maximize: 143x+60y
         * Subject to:
         * 120x+210y <= 15000
         * 110x+30y <= 4000
         * x+y <= 75
         *
         * With x,y being integers
         *
         */
        Problem problem = new Problem();

        Linear linear = new Linear();
        linear.add(143, "x");
        linear.add(60, "y");

        problem.setObjective(linear, OptType.MAX);

        linear = new Linear();
        linear.add(120, "x");
        linear.add(210, "y");

        problem.add(linear, "<=", 15000);

        linear = new Linear();
        linear.add(110, "x");
        linear.add(30, "y");

        problem.add(linear, "<=", 4000);

        linear = new Linear();
        linear.add(1, "x");
        linear.add(1, "y");

        problem.add(linear, "<=", 75);

        problem.setVarType("x", Double.class);
        problem.setVarType("y", Double.class);

        Solver solver = factory.get(); // you should use this solver only once for one problem
        Result result = solver.solve(problem);

        System.out.println(result);

        /**
         * Extend the problem with x <= 16 and solve it again
         */
        problem.setVarUpperBound("x", 16);

        solver = factory.get();
        result = solver.solve(problem);

        System.out.println(result);
    }
}
