import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.BeatsException;
import edu.berkeley.path.beats.simulator.Scenario;
import edu.berkeley.path.beats.simulator.ObjectFactory;
import net.sf.javailp.*;

import java.util.List;

/**
 * Created by gomes on 1/15/14.
 */
public class LPBuilder {

    private FwyNetwork fwy;
    private double eta = 1.0;
    private Problem constant_data = new Problem();


    public LPBuilder(Network network,FundamentalDiagramSet fds,ActuatorSet actuators){

        fwy = new FwyNetwork(network,fds,actuators);

        System.out.println(fwy.toString());

        // objective function
//        Linear J = new Linear();
//        for(edu.berkeley.path.beats.jaxb.Link jlink : network.getLinkList().getLink()){
//            Link link = (Link) jlink;
//            J.add(1d,"n_"+link.getId());
//            J.add(-eta,"f_"+link.getId());
//            if(link.isOnramp()){
//                J.add(1d,"l_"+link.getId());
//                J.add(-eta,"r_"+link.getId());
//            }
//        }
//        constant_data.setObjective(J, OptType.MIN);

        // mainline conservation


        // mainline flows - freeflow

        // mainline flows - congestion

        // mainline flows - capacity (bound)

        // onramp flows - capacity (bound)

        // onramp flows - positivity (bound)

        // queue length limit (bound)


//        C1 = new Linear();
//        C1.add(120, "x");
//        C1.add(210, "y");
//        problem.add(C1, "<=", 15000);
//
//        C2 = new Linear();
//        C2.add(110, "x");
//        C2.add(30, "y");
//        problem.add(C2, "<=", 4000);
//
//        problem.setVarType("x", Double.class);
//        problem.setVarType("y", Double.class);

    }

    public void problemA(InitialDensitySet ic,DemandSet demand_set,SplitRatioSet splitratios){

        // copy constant_data => P
//        Problem P = new Problem();
//        P.setObjective(constant_data.getObjective());
//        P.setOptimizationType(constant_data.getOptType());
//        for(Constraint C : constant_data.getConstraints())
//            P.add(C);

        // add constraints

        // onramp conservation

        // unmetered onramps

        // onramp flows => freeflow

        // initial conditions


        // solve
//        SolverFactory factory = new SolverFactoryLpSolve(); // use lp_solve
//        factory.setParameter(Solver.VERBOSE, 0);
//        factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds
//        Solver solver = factory.get(); // you should use this solver only once for one problem
//        Result result = solver.solve(P);
//
//        System.out.println(result);
    }


}
