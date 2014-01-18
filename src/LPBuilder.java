import edu.berkeley.path.beats.jaxb.*;
import net.sf.javailp.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 1/15/14.
 */
public class LPBuilder {

    private FwyNetwork fwy;
    private int K;
    private int I;
    private double eta = 1.0;
    private double gamma = 1d;
    private Linear J = new Linear();

    public LPBuilder(Network network,FundamentalDiagramSet fds,ActuatorSet actuators,int K){

        this.K = K;
        this.I = fwy.getSegments().size();
        fwy = new FwyNetwork(network,fds,actuators);

        int i,k;

        // objective function
        for(i=0;i<I;i++){
            FwySegment seg = fwy.getSegments().get(i);
            for(k=0;k<K;k++){
                J.add( 1.0,getVar("n",i,k+1));
                J.add(-eta,getVar("f",i,k  ));
            }
            if(seg.hasOR()){
                for(k=0;k<K;k++){
                    J.add( 1.0,getVar("l",i,k+1));
                    J.add(-eta,getVar("r",i,k  ));
                }
            }
        }

        // mainline conservation
        for(i=0;i<I;i++){
            FwySegment seg = fwy.getSegments().get(i);
            for(k=0;k<K;k++){
                Linear C = new Linear();
                C.add(+1d,getVar("n",i,k+1));
                if(k>0)
                    C.add(-1d,getVar("n",i,k));
                if(i>0)
                    C.add(-1d,getVar("f",i-1,k));
                if(seg.is_metered)
                    C.add(-1d,getVar("r",i,k));
                if(seg.betabar(k)!=0d)
                    C.add(+1/seg.betabar(k),getVar("f",i,k));
                seg.lhs_MLcons.add(C);
            }
        }

        // onramp conservation
        for(i=0;i<I;i++){
            FwySegment seg = fwy.getSegments().get(i);
            if(seg.is_metered){
                for(k=0;k<K;k++){
                    Linear C = new Linear();
                    C.add(+1d,getVar("l",i,k+1));
                    if(k>0)
                        C.add(-1d,getVar("l",i,k));
                    C.add(+1d,getVar("r",i,k));
                    seg.lhs_ORcons.add(C);
                }
            }
        }

        // mainline flows - freeflow
        for(i=0;i<I;i++){
            FwySegment seg = fwy.getSegments().get(i);
            for(k=0;k<K;k++){
                Linear C = new Linear();
                C.add(+1d,getVar("f",i,k));
                if(seg.betabar(k)>0){
                    if(k>0)
                        C.add(-seg.betabar(k)*seg.vf,getVar("n",i,k));
                    if(seg.is_metered)
                        C.add(-seg.betabar(k)*seg.vf*gamma,getVar("r",i,k));
                }
                seg.lhs_MLffw.add(C);
            }
        }

        // mainline flows - congestion
        for(i=0;i<I-1;i++){
            FwySegment seg = fwy.getSegments().get(i);
            FwySegment next_seg = fwy.getSegments().get(i+1);
            for(k=0;k<K;k++){
                Linear C = new Linear();
                C.add(+1d,getVar("f",i,k));
                if(k>0)
                    C.add(seg.w,getVar("n",i+1,k));
                if(next_seg.is_metered)
                    C.add(seg.w*gamma,getVar("r",i+1,k));
                seg.lhs_MLcng.add(C);
            }
        }

        // onramp flow demand
        for(i=0;i<I;i++){
            FwySegment seg = fwy.getSegments().get(i);
            if(seg.is_metered){
                for(k=0;k<K;k++){
                    Linear C = new Linear();
                    C.add(+1d,getVar("r",i,k));
                    C.add(-1d,getVar("l",i,k));
                    seg.lhs_ORdem.add(C);
                }
            }
        }
    }

    public void problemA(InitialDensitySet ic,DemandSet demand_set,SplitRatioSet splitratios){

        int i,k;
        double fmax = 100;
        double lmax = 100;
        double rmax = 100;

        Problem L = new Problem();
        L.setObjective(J, OptType.MIN);

        // RHS
        for(i=0;i<I;i++){
            FwySegment seg = fwy.getSegments().get(i);

            // initial conditions ................................

            // ml conservation
            L.add(seg.lhs_MLcons.get(0),"<=",seg.no);

            // or conservation
            if(seg.is_metered)
                L.add(seg.lhs_ORcons.get(0),"<=",seg.lo + seg.d(0));

            // ml flow freeflow
            L.add(seg.lhs_MLffw.get(0),"<=",(gamma*(seg.is_metered?0:seg.d(0)) + seg.no)*seg.vf*seg.betabar(0));

            // ml flow congestion
            if(i<I-1){
                FwySegment next_seg = fwy.getSegments().get(i+1);
                L.add(seg.lhs_MLcng.get(0),"<=", next_seg.w*(next_seg.n_max - next_seg.no - gamma*(next_seg.is_metered?0:next_seg.d(0))) );
            }

            // or flow demand
            if(seg.is_metered)
                L.add(seg.lhs_ORdem.get(0),"<=",seg.d(0));

            // future times ...........................................
            for(k=1;k<K;k++){

                // ml conservation
                L.add(seg.lhs_MLcons.get(k),"<=",0);

                // or conservation
                if(seg.is_metered)
                    L.add(seg.lhs_ORcons.get(k),"<=",seg.d(k));

                // ml flow freeflow
                L.add(seg.lhs_MLffw.get(k),"<=",gamma*(seg.is_metered?0:seg.d(k))*seg.vf*seg.betabar(k));

                // ml flow congestion
                if(i<I-1){
                    FwySegment next_seg = fwy.getSegments().get(i+1);
                    L.add(seg.lhs_MLcng.get(k),"<=", next_seg.w*(next_seg.n_max - gamma*(next_seg.is_metered?0:next_seg.d(k))) );
                }

                // or flow demand
                if(seg.is_metered)
                    L.add(seg.lhs_ORdem.get(k),"<=",seg.d(k));

            }
        }

        // bounds
        for(i=0;i<I-1;i++){
            FwySegment seg = fwy.getSegments().get(i);

            for(k=0;k<K;k++){

                // mainline capacity
                L.setVarUpperBound(getVar("f",i,k),fmax);

                if(seg.is_metered){

                    // queue length limit
                    L.setVarUpperBound(getVar("l",i,k+1),lmax);

                    // onramp flow positivity
                    L.setVarLowerBound(getVar("r",i,k),0);

                    // maximum metering rate
                    L.setVarUpperBound(getVar("r", i, k), rmax);
                }
            }
        }

        // solve
        SolverFactory factory = new SolverFactoryLpSolve(); // use lp_solve
        factory.setParameter(Solver.VERBOSE, 0);
        factory.setParameter(Solver.TIMEOUT, 100); // set timeout to 100 seconds
        Solver solver = factory.get(); // you should use this solver only once for one problem
        Result result = solver.solve(L);

        System.out.println(result);
    }

    private String getVar(String name,int index,int timestep){
        return name+"_"+index+"_"+timestep;
    }
}
