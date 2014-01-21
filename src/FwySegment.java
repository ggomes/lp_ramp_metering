import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.Parameters;
import net.sf.javailp.Linear;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 1/16/14.
 */
public class FwySegment {

    // link references
    protected Long ml_link_id;
    protected Long or_link_id;
    protected Long fr_link_id;
    protected Long fr_node_id;

    // fundamental diagram
    protected double vf;
    protected double w;
    protected double f_max;
    protected double n_max;

    // initial condition
    protected double no;
    protected double lo;

    // metering
    protected boolean is_metered;
    protected double l_max;
    protected double r_max;

    // constraints
    protected List<Linear> lhs_MLcons = new ArrayList<Linear>();
    protected List<Linear> lhs_ORcons = new ArrayList<Linear>();
    protected List<Linear> lhs_MLffw = new ArrayList<Linear>();
    protected List<Linear> lhs_MLcng = new ArrayList<Linear>();
    protected List<Linear> lhs_ORdem = new ArrayList<Linear>();

    // data profiles
    protected ArrayList<Double> demand_profile;
    protected ArrayList<Double> split_ratio_profile;

    ///////////////////////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////////////////////

    public FwySegment(Link ml_link,Link or_link,Link fr_link,FundamentalDiagram fd,Actuator actuator){

        // link references
        this.ml_link_id = ml_link==null?null:ml_link.getId();
        this.or_link_id = or_link==null?null:or_link.getId();
        this.fr_link_id = fr_link==null?null:fr_link.getId();
        this.fr_node_id = fr_link==null?null: fr_link.getBegin().getNodeId();

        // fundamental diagram
        f_max = fd.getCapacity();
        vf= fd.getFreeFlowSpeed();
        w = fd.getCongestionSpeed();
        n_max = fd.getJamDensity();

        // metering
        is_metered = actuator!=null;
        if(is_metered){
            Parameters P = (Parameters) actuator.getParameters();
            r_max = Double.parseDouble(P.get("max_rate_in_vphpl"));
            l_max = Double.parseDouble(P.get("max_queue_length_in_veh"));
        }
        else{
            r_max = Double.POSITIVE_INFINITY;
            l_max = Double.POSITIVE_INFINITY;
        }

        // constraints=
        lhs_MLcons = new ArrayList<Linear>();
        lhs_ORcons = new ArrayList<Linear>();
        lhs_MLffw = new ArrayList<Linear>();
        lhs_MLcng = new ArrayList<Linear>();
        lhs_ORdem = new ArrayList<Linear>();

    }

    ///////////////////////////////////////////////////////////////////
    // get
    ///////////////////////////////////////////////////////////////////

    public boolean hasML(){
        return ml_link_id!=null;
    }

    public boolean hasOR(){
        return or_link_id!=null;
    }

    public boolean hasFR(){
        return fr_link_id!=null;
    }

    public double d(int k){
        return Double.NaN;
    }

    public double betabar(int k){
        return Double.NaN;
    }

    ///////////////////////////////////////////////////////////////////
    // set
    ///////////////////////////////////////////////////////////////////

    protected void reset_state(){
        this.no = 0d;
        this.lo = 0d;
    }

    protected void reset_demands(){
        demand_profile = new ArrayList<Double>();
    }

    protected void reset_split_ratios(){
        split_ratio_profile = new ArrayList<Double>();
    }

    protected void add_demands(String str,double dt){


    }

    ///////////////////////////////////////////////////////////////////
    // Override
    ///////////////////////////////////////////////////////////////////

    @Override
    public String toString() {

        return String.format(  "ml_link_id=%s\n" +
                               "or_link_id=%s\n" +
                               "fr_link_id=%s\n" +
                               "fr_node_id=%s\n" +
                               "vf=%.1f\n" +
                               "w=%.1f\n" +
                               "f_max=%.1f\n" +
                               "n_max=%.1f\n" +
                               "no=%.1f\n" +
                               "lo=%.1f\n" +
                               "is_metered=%s\n"+
                               "l_max=%.1f\n"+
                               "r_max=%.1f\n"+
                               "dem=%s\n"+
                               "beta=%s\n",
                                ml_link_id,
                                or_link_id,
                                fr_link_id,
                                fr_node_id,
                                vf,w,f_max,n_max,no,lo,
                                is_metered,l_max,r_max,
                                demand_profile==null?"[]":demand_profile.toString(),
                                split_ratio_profile==null?"[]":split_ratio_profile.toString()
                );
    }

}
