import edu.berkeley.path.beats.jaxb.Actuator;
import edu.berkeley.path.beats.jaxb.FundamentalDiagram;
import edu.berkeley.path.beats.jaxb.Link;
import edu.berkeley.path.beats.simulator.Parameters;

/**
 * Created by gomes on 1/16/14.
 */
public class FwySegment {

    // link references
    private Link ml_link;
    private Link or_link;
    private Link fr_link;

    // fundamental diagram
    private double vf;
    private double w;
    private double F;
    private double n_max;

    // metering
    private boolean is_metered;
    private double l_max;
    private double r_max;

    // data profiles
//    private double [] demand_profile;
//    private double [] split_ratio_profile;


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

    }


    @Override
    public String toString() {
        return String.format("%s\t%s\t%s",ml_link==null?"-":ml_link.getId(),
                                          or_link==null?"-":or_link.getId(),
                                          fr_link==null?"-":fr_link.getId());

    }


}
