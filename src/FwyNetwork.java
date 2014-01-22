import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by gomes on 1/16/14.
 */
public final class FwyNetwork {

    protected ArrayList<FwySegment> segments;
    protected ArrayList<Long> ml_link_id;
    protected ArrayList<Long> fr_link_id;
    protected ArrayList<Long> or_link_id;
    protected ArrayList<Long> or_source_id;
    protected ArrayList<Long> fr_node_id;

    ///////////////////////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////////////////////

    public FwyNetwork(Network network,FundamentalDiagramSet fds,ActuatorSet actuatorset,double sim_dt_in_seconds) throws Exception{

        segments = new ArrayList<FwySegment>();
        ml_link_id = new ArrayList<Long>();
        fr_link_id = new ArrayList<Long>();
        or_link_id = new ArrayList<Long>();
        or_source_id = new ArrayList<Long>();
        fr_node_id = new ArrayList<Long>();

        // find first mainline link, then iterate downstream until you reach the end
        Link link = find_first_fwy_link(network);
        while(link!=null){
            Link onramp = start_node_onramp_or_source(link);
            Link offramp = end_node_offramp(link);
            FundamentalDiagram fd = get_fd_for_link(link,fds);
            Actuator actuator = get_onramp_actuator(onramp,actuatorset);
            segments.add(new FwySegment(link,onramp,offramp,fd,actuator,sim_dt_in_seconds));
            ml_link_id.add(link.getId());
            or_link_id.add(onramp==null?null:onramp.getId());
            Link onramp_source = get_onramp_source(onramp);
            or_source_id.add(onramp_source==null?null:onramp_source.getId());
            fr_link_id.add(offramp==null?null:offramp.getId());
            fr_node_id.add(offramp==null?null:offramp.getBegin_node().getId());
            link = next_freeway_link(link);
        }

    }

    ///////////////////////////////////////////////////////////////////
    // get
    ///////////////////////////////////////////////////////////////////

    protected List<FwySegment> getSegments() {
        return segments;
    }

    ///////////////////////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////////////////////

    private Link find_first_fwy_link(Network network) throws Exception{

        if(!all_are_fwy_or_fr_src_snk(network))
            throw new Exception("Not all links are freeway, onramp, offramps, sources, or sinks");

        // gather links that are freeway sources, or sources that end in simple nodes
        List<Link> first_fwy_list = new ArrayList<Link>();
        for(edu.berkeley.path.beats.jaxb.Link jlink : network.getLinkList().getLink()){
            Link link = (Link) jlink;
            Node end_node = link.getEnd_node();
            boolean end_node_is_simple = end_node.getnIn()==1 && end_node.getnOut()==1;
            boolean supplies_onramp = end_node.getnOut()>0 ? isOnrampType(end_node.getOutput_link()[0]) : false;
            if( link.isSource() &&  end_node_is_simple && !supplies_onramp)
                    first_fwy_list.add(link);
        }

        // there must be exactly one of these
        if(first_fwy_list.isEmpty())
            throw new Exception("NO FIRST FWY LINK");

        if(first_fwy_list.size()>1)
            throw new Exception("MULTIPLE FIRST FWY LINKS");

        Link first_fwy = first_fwy_list.get(0);

        // if it is a source link, use next
        if(isSource(first_fwy))
            first_fwy = first_fwy.getEnd_node().getOutput_link()[0];

        return first_fwy;
    }

    private boolean all_are_fwy_or_fr_src_snk(Network network){
        for(edu.berkeley.path.beats.jaxb.Link jlink : network.getLinkList().getLink()){
            Link link = (Link) jlink;
            if(!isFreewayType(link) && !isOnrampType(link) && !isOfframpType(link) && !isSource(link) && !isSinkType(link))
                return false;
        }
        return true;
    }

    private Link end_node_offramp(Link link){
        for(Link olink : link.getEnd_node().getOutput_link()){
            if(isOfframpType(olink))
                return olink;
        }
        return null;
    }

    private boolean isSource(Link link){
        return link.getBegin_node().getInput_link().length==0;
    }

    private boolean isFreewayType(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("freeway")==0;
    }

    private boolean isOfframpType(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("off-ramp")==0;
    }

    private boolean isOnrampType(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("on-ramp")==0;
    }

    private boolean isSinkType(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("sink")==0;
    }

    private Link next_freeway_link(Link link){
        for(Link olink : link.getEnd_node().getOutput_link()){
            if(isFreewayType(olink))
                return olink;
        }
        return null;
    }

    private Link start_node_onramp_or_source(Link link){
        for(Link ilink : link.getBegin_node().getInput_link()){
            if(isOnrampType(ilink) || isSource(ilink))
                return ilink;
        }
        return null;
    }

    private FundamentalDiagram get_fd_for_link(Link link,FundamentalDiagramSet fds){
        if(fds==null)
            return null;
        for(FundamentalDiagramProfile fdp : fds.getFundamentalDiagramProfile())
            if(link.getId()==fdp.getLinkId())
                if(fdp.getFundamentalDiagram()!=null && !fdp.getFundamentalDiagram().isEmpty())
                    return fdp.getFundamentalDiagram().get(0);
        return null;
    }

    private Actuator get_onramp_actuator(Link onramp,ActuatorSet actuatorset){
        if(actuatorset==null)
            return null;
        if(onramp==null)
            return null;
        for(Actuator actuator : actuatorset.getActuator()){
            if( actuator.getActuatorType().getName().compareTo("ramp_meter")==0 &&
                actuator.getScenarioElement().getType().compareTo("link")==0 &&
                actuator.getScenarioElement().getId()==onramp.getId() )
                return actuator;
        }
        return null;
    }

    private Link get_onramp_source(Link link){
        Link rlink = link;
        while(rlink!=null && !isSource(rlink)){
            Node node = rlink.getBegin_node();
            rlink = node.getnIn()==1 ? node.getInput_link()[0] : null;
        }
        return rlink;
    }

    private static ArrayList<Double> sample(ArrayList<Double> in,double in_dt,double out_dt,int K_out,int K_out_cool,boolean holdlast){

        int k_in,k_out;
        ArrayList<Double> out = new ArrayList<Double>();

        // edge cases
        if(in==null)
            return null;
        if(in.isEmpty()){
            for(k_out=0;k_out<K_out;k_out++)
                out.add(0d);
            return out;
        }

        // normal case
        double last = 0d;
        for(k_out=0;k_out<K_out;k_out++){
            k_in = (int) Math.floor(((double)k_out)*out_dt/in_dt);
            k_in = Math.min(k_in,in.size()-1);
            if(k_out<K_out-K_out_cool){
                last = in.get(k_in);
                out.add(last);
            }
            else{
                if(holdlast)
                    out.add(last);
                else
                    out.add(0d);
            }
        }
        return out;
    }

    ///////////////////////////////////////////////////////////////////
    // set
    ///////////////////////////////////////////////////////////////////

    protected void set_ic(InitialDensitySet ic){

        // reset everything to zero
        for(FwySegment seg : segments)
            seg.reset_state();

        if(ic==null)
            return;

        // distribute initial condition to segments
        for(Density D : ic.getDensity()){
            int index = ml_link_id.indexOf(D.getLinkId());
            if(index>=0)
                segments.get(index).add_to_no_in_vpm(Double.parseDouble(D.getContent()));
            index = or_link_id.indexOf(D.getLinkId());
            if(index>=0)
                segments.get(index).add_to_lo_in_vpm(Double.parseDouble(D.getContent()));
        }
    }

    protected void set_demands(DemandSet demand_set,double sim_dt_in_seconds,int K,int Kcool){

        // reset everything to zero
        for(FwySegment seg : segments)
            seg.reset_demands();

        for(DemandProfile dp : demand_set.getDemandProfile()){
            int index = or_source_id.indexOf(dp.getLinkIdOrg());
            if(index>=0){
                FwySegment seg = segments.get(index);
                ArrayList<Double> demand = new ArrayList<Double>();
                if(dp.getDemand()!=null){
                    for(Demand d:dp.getDemand()){
                        List<String> strlist = Arrays.asList(d.getContent().split(","));
                        if(demand.isEmpty())
                            for(int i=0;i<strlist.size();i++)
                                demand.add(0d);
                        for(int i=0;i<strlist.size();i++){
                            double val = demand.get(i);
                            val += Double.parseDouble(strlist.get(i))*sim_dt_in_seconds*seg.or_lanes;
                            demand.set(i,val);
                        }
                    }
                }
                seg.demand_profile = sample(demand,dp.getDt(),sim_dt_in_seconds,K,Kcool,false);
            }
        }
    }

    protected void set_split_ratios(SplitRatioSet srs,double sim_dt_in_seconds,int K,int Kcool) throws Exception{
        int index;

        for(SplitRatioProfile srp : srs.getSplitRatioProfile()){
            index = fr_node_id.indexOf(srp.getNodeId());
            if(index>=0){
                ArrayList<Double> ml_split = new ArrayList<Double>();
                ArrayList<Double> fr_split = new ArrayList<Double>();
                for(Splitratio sr : srp.getSplitratio()){
                    if(sr.getLinkIn()==ml_link_id.get(index)){
                        if(sr.getLinkOut()==fr_link_id.get(index)){
                            for(String str : Arrays.asList(sr.getContent().split(",")))
                                fr_split.add(Double.parseDouble(str));
                        } else if(index<ml_link_id.size()-1 && sr.getLinkOut()==ml_link_id.get(index+1)){
                            for(String str : Arrays.asList(sr.getContent().split(",")))
                                ml_split.add(Double.parseDouble(str));
                        } else
                            throw new Exception("ERROR!");
                    }
                    else
                        throw new Exception("ERROR!");
                }
                if(fr_split.isEmpty() && !ml_split.isEmpty())
                    for(Double d : ml_split)
                        fr_split.add(1-d);
                segments.get(index).split_ratio_profile = sample(fr_split,srp.getDt(),sim_dt_in_seconds,K,Kcool,true);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // print
    ///////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        String str = "";
        for(int i=0;i<segments.size();i++)
            str = str.concat(String.format("%d) ----------------------------\n%s",i,segments.get(i).toString()));
        return str;
    }
}
