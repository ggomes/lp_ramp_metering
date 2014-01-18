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
    private double eta = 1.0;

    private Linear J = new Linear();
    private List<Linear> lhs_MLcons = new ArrayList<Linear>();
    private List<Linear> lhs_ORcons = new ArrayList<Linear>();
    private List<Linear> lhs_MLffw = new ArrayList<Linear>();
    private List<Linear> lhs_MLcng = new ArrayList<Linear>();
    private List<Linear> lhs_ORdem = new ArrayList<Linear>();

    public LPBuilder(Network network,FundamentalDiagramSet fds,ActuatorSet actuators,int K){

        this.K = K;
        fwy = new FwyNetwork(network,fds,actuators);

        double betabar = 0.4;
        double vf=0.9;
        double w = 0.3;
        double gamma = 1d;
        int i,k;

        // objective function
        for(i=0;i<fwy.getSegments().size();i++){
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
        for(i=0;i<fwy.getSegments().size();i++){
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
                if(betabar!=0d)
                    C.add(+1/betabar,getVar("f",i,k));
                lhs_MLcons.add(C);
            }
        }

        // onramp conservation
        for(i=0;i<fwy.getSegments().size();i++){
            FwySegment seg = fwy.getSegments().get(i);
            if(seg.is_metered){
                for(k=0;k<K;k++){
                    Linear C = new Linear();
                    C.add(+1d,getVar("l",i,k+1));
                    if(k>0)
                        C.add(-1d,getVar("l",i,k));
                    C.add(+1d,getVar("r",i,k));
                    lhs_ORcons.add(C);
                }
            }
        }

        // mainline flows - freeflow
        for(i=0;i<fwy.getSegments().size();i++){
            FwySegment seg = fwy.getSegments().get(i);
            for(k=0;k<K;k++){
                Linear C = new Linear();
                C.add(+1d,getVar("f",i,k));
                if(betabar>0){
                    if(k>0)
                        C.add(-betabar*vf,getVar("n",i,k));
                    if(seg.is_metered)
                        C.add(-betabar*vf*gamma,getVar("r",i,k));
                }
                lhs_MLffw.add(C);
            }
        }

        // mainline flows - congestion
        for(i=0;i<fwy.getSegments().size()-1;i++){
            FwySegment seg = fwy.getSegments().get(i);
            FwySegment next_seg = fwy.getSegments().get(i+1);
            for(k=0;k<K;k++){
                Linear C = new Linear();
                C.add(+1d,getVar("f",i,k));
                if(k>0)
                    C.add(w,getVar("n",i+1,k));
                if(next_seg.is_metered)
                    C.add(w*gamma,getVar("r",i+1,k));
                lhs_MLcng.add(C);
            }
        }

        // onramp flow demand
        for(i=0;i<fwy.getSegments().size();i++){
            FwySegment seg = fwy.getSegments().get(i);
            if(seg.is_metered){
                for(k=0;k<K;k++){
                    Linear C = new Linear();
                    C.add(+1d,getVar("r",i,k));
                    C.add(-1d,getVar("l",i,k));
                    lhs_ORdem.add(C);
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
        for(Linear X : lhs_MLcons)
            L.add(X,"<=",0);
        for(Linear X : lhs_ORcons)
            L.add(X,"<=",0);
        for(Linear X : lhs_MLffw)
            L.add(X,"<=",0);
        for(Linear X : lhs_MLcng)
            L.add(X,"<=",0);
        for(Linear X : lhs_ORdem)
            L.add(X,"<=",0);

        // bounds
        for(i=0;i<fwy.getSegments().size()-1;i++){
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
