import edu.berkeley.path.beats.jaxb.Actuator;
import edu.berkeley.path.beats.jaxb.FundamentalDiagram;
import edu.berkeley.path.beats.jaxb.Link;
import edu.berkeley.path.beats.simulator.Parameters;
import net.sf.javailp.Linear;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 1/16/14.
 */
public class FwySegment {

    // link references
    protected Link ml_link;
    protected Link or_link;
    protected Link fr_link;

    // fundamental diagram
    protected double vf;
    protected double w;
    protected double F;
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
//    private double [] demand_profile;
//    private double [] split_ratio_profile;

    ///////////////////////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////////////////////

    public FwySegment(Link ml_link,Link or_link,Link fr_link,FundamentalDiagram fd,Actuator actuator){

        // link references
        this.ml_link = ml_link;
        this.or_link = or_link;
        this.fr_link = fr_link;

        // fundamental diagram
        F = fd.getCapacity();
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
        return ml_link!=null;
    }

    public boolean hasOR(){
        return or_link!=null;
    }

    public boolean hasFR(){
        return fr_link!=null;
    }

    public Long getMLid(){
        return hasML() ?  ml_link.getId() : null;
    }

    public Long getORid(){
        return hasOR() ?  or_link.getId() : null;
    }

    public Long getFRid(){
        return hasFR() ?  fr_link.getId() : null;
    }

    public double d(int k){
        return Double.NaN;
    }

    public double betabar(int k){
        return Double.NaN;
    }

    ///////////////////////////////////////////////////////////////////
    // Override
    ///////////////////////////////////////////////////////////////////


    @Override
    public String toString() {
        return String.format("%s\t%s\t%s",ml_link==null?"-":ml_link.getId(),
                                          or_link==null?"-":or_link.getId(),
                                          fr_link==null?"-":fr_link.getId());

    }


}
