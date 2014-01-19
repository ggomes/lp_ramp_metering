import edu.berkeley.path.beats.jaxb.*;
import net.sf.javailp.*;

/**
 * Ramp metering with linear programming
 */
public class LPBuilder {

    private FwyNetwork fwy;
    private int K;                      // number of time steps
    private int I;                      // number of segments
    private double eta = 1.0;           // J = TVH - eta*TVM
    private double gamma = 1d;          // merge coefficient
    private Linear J = new Linear();

    ///////////////////////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////////////////////

    public LPBuilder(Network network,FundamentalDiagramSet fds,ActuatorSet actuators,int K){

        int i,k;

        this.K = K;
        this.I = fwy.getSegments().size();

        // Make the freeway structure
        fwy = new FwyNetwork(network,fds,actuators);

        /* objective function:
           sum_[I][K] n[i][k] + sum_[I][K] l[i][k] - eta sum_[I][K] f[i][k] - eta sum[I][K] r[i][k]
         */
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

        /* mainline conservation (LHS)
           for each i in 0...I-1, k in 0...K-1
            {always}    {k>0}     {i>0}   {metered}       {betabar[i][k]>0}
           n[i][k+1] -n[i][k] -f[i-1][k]  -r[i][k] +inv(betabar[i][k])*f[i][k]
         */
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

        /* onramp conservation
           for each metered i in 0...I-1, k in 0...K-1
            {always}    {k>0}  {always}
           l[i][k+1]  -l[i][k] +r[i][k]
         */
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

        /* mainline flows - freeflow
           for each i in 0...I-1, k in 0...K-1
           {always}           {k>0}                       {metered}
           f[i][k] -betabar[i][k]*v[i]*n[i][k] - betabar[i][k]*v[i]*gamma*r[i][k]
         */
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

        /* mainline flows - congestion
           for each i in 0...I-2, k in 0...K-1
           {always}       {k>0}                {metered}
           f[i][k] + w[i+1]*n[i+1][k] + w[i+1]*gamma*r[i+1][k]
         */
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

        /* onramp flow demand
           for each metered i in 0...I-1, k in 0...K-1
           {always}   {k>0}
           r[i][k] - l[i][k]
         */
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

    ///////////////////////////////////////////////////////////////////
    // solve problem
    ///////////////////////////////////////////////////////////////////

    public void compute_optimal_metering(InitialDensitySet ic, DemandSet demand_set, SplitRatioSet splitratios){

        int i,k;
        double rhs;

        Problem L = new Problem();
        L.setObjective(J, OptType.MIN);

        // RHS
        for(i=0;i<I;i++){
            FwySegment seg = fwy.getSegments().get(i);

            /* ml conservation
               for each i in 0...I-1, k in 0...K-1
                    {k>0}  {k==0}
               LHS =  0  + n[i][0]
            */
            for(k=0;k<K;k++){
                rhs = k==0 ? seg.no : 0;
                L.add(seg.lhs_MLcons.get(k),"=",rhs);
            }

            /* or conservation
               for each metered i in 0...I-1, k in 0...K-1
                     {always}  {k==0}
               LHS = d[i][k] + l[i][0]
             */
            if(seg.is_metered){
                for(k=0;k<K;k++){
                    rhs = seg.d(k);
                    rhs += k==0 ? seg.lo : 0;
                    L.add(seg.lhs_ORcons.get(k),"=",rhs);
                }
            }

            /* ml flow freeflow
               for each i in 0...I-1, k in 0...K-1
                                {k==0}                      {!metered}
               LHS <= betabar[i][0]*v[i]*n[i][0] + betabar[i][k]*v[i]*gamma*d[i][k]
             */
            for(k=0;k<K;k++){
                rhs = k==0 ? seg.betabar(0)*seg.vf*seg.no : 0;
                rhs += !seg.is_metered ? seg.betabar(k)*seg.vf*seg.d(k) : 0;
                L.add(seg.lhs_MLffw.get(0),"<=",rhs);
            }

            /* ml flow congestion
               for each i in 0...I-2, k in 0...K-1
                         {always}              {k=0}             {!metered}
               LHS <= w[i+1]*njam[i+1] - w[i+1]*n[i+1][0] - w[i+1]*gamma*d[i+1][k]
             */
            if(i<I-1){
                FwySegment next_seg = fwy.getSegments().get(i+1);
                for(k=0;k<K;k++){
                    rhs = next_seg.w*next_seg.n_max;
                    rhs += k==0 ? -next_seg.w*next_seg.no : 0;
                    rhs += !next_seg.is_metered ? -gamma*next_seg.w*next_seg.d(0) : 0;
                    L.add(seg.lhs_MLcng.get(0),"<=",rhs);
                }
            }

            /* or flow demand
               for each metered i in 0...I-1, k in 0...K-1
                      {always}
               LHS <= d[i][k]
             */
            if(seg.is_metered)
                for(k=0;k<K;k++)
                    L.add(seg.lhs_ORdem.get(k),"<=",seg.d(k));

        }

        // bounds
        for(i=0;i<I;i++){
            FwySegment seg = fwy.getSegments().get(i);

            for(k=0;k<K;k++){

                /* mainline capacity
                   for each i in 0...I-1, k in 0...K-1
                   f[i][k] <= f_max[i]
                */
                L.setVarUpperBound(getVar("f",i,k),seg.f_max);

                if(seg.is_metered){

                    /* queue length limit
                       for each metered i in 0...I-1, k in 0...K-1
                       l[i][k] <= lmax[i]
                     */
                    L.setVarUpperBound(getVar("l",i,k+1),seg.l_max);

                    /* onramp flow positivity
                       for each metered i in 0...I-1, k in 0...K-1
                       r[i][k] >= 0
                     */

                    L.setVarLowerBound(getVar("r",i,k),0);

                    /* maximum metering rate
                       for each metered i in 0...I-1, k in 0...K-1
                       r[i][k] <= rmax[i]
                     */
                    L.setVarUpperBound(getVar("r", i, k), seg.r_max);
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

    ///////////////////////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////////////////////

    private String getVar(String name,int index,int timestep){
        return name+"_"+index+"_"+timestep;
    }
}
