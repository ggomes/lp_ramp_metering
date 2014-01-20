import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by gomes on 1/16/14.
 */
public class FwyNetwork {

    protected ArrayList<FwySegment> segments;
    protected ArrayList<Long> ml_link_id;
    protected ArrayList<Long> or_source_id;
    protected ArrayList<Long> fr_node_id;

    ///////////////////////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////////////////////

    public FwyNetwork(Network network,FundamentalDiagramSet fds,ActuatorSet actuatorset){

        segments = new ArrayList<FwySegment>();
        ml_link_id = new ArrayList<Long>();
        or_source_id = new ArrayList<Long>();
        fr_node_id = new ArrayList<Long>();

        // find first mainline link, iterate downstream until you reach the end
        Link link = find_first_fwy_link(network);
        while(link!=null){
            Link onramp = start_node_onramp_or_source(link);
            Link offramp = end_node_offramp(link);
            FundamentalDiagram fd = get_fd_for_link(link,fds);
            Actuator actuator = get_onramp_actuator(onramp,actuatorset);
            segments.add(new FwySegment(link,onramp,offramp,fd,actuator));
            ml_link_id.add(link.getId());
            Link onramp_source = get_onramp_source(onramp);
            or_source_id.add(onramp_source==null?null:onramp_source.getId());
            fr_node_id.add(offramp==null?null:offramp.getBegin_node().getId());
            link = next_freeway_link(link);
        }
    }

    ///////////////////////////////////////////////////////////////////
    // get
    ///////////////////////////////////////////////////////////////////

    public List<FwySegment> getSegments() {
        return segments;
    }

    ///////////////////////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////////////////////

    private Link find_first_fwy_link(Network network){

        if(!all_are_fwy_or_fr_src_snk(network)){
            System.out.println("Not all links are freeway, onramp, offramps, sources, or sinks");
            return null;
        }

        // gather links that are freeway sources, or sources that end in simple nodes
        List<Link> first_fwy_list = new ArrayList<Link>();
        for(edu.berkeley.path.beats.jaxb.Link jlink : network.getLinkList().getLink()){
            Link link = (Link) jlink;
            Node end_node = link.getEnd_node();
            boolean supplies_onramp = end_node.getnIn()==1
                               && end_node.getnOut()==1
                               && isOnrampType(end_node.getOutput_link()[0]);
            if(link.isSource())
                if( isFreewayType(link) || !supplies_onramp)
                    first_fwy_list.add(link);
        }

        // there must be exactly one of these
        if(first_fwy_list.isEmpty()){
            System.out.println("NO FIRST FWY LINK");
            return null;
        }

        if(first_fwy_list.size()>1){
            System.out.println("MULTIPLE FIRST FWY LINKS");
            return null;
        }

        Link first_fwy = first_fwy_list.get(0);

        // if it is a source type link, use next
        if(isSourceType(first_fwy))
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

    private boolean isSourceType(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("source")==0;
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

    ///////////////////////////////////////////////////////////////////
    // set
    ///////////////////////////////////////////////////////////////////

    protected void set_ic(InitialDensitySet ic){

        int index;

        // reset everything to zero
        for(FwySegment seg : segments)
            seg.reset_state();

        if(ic==null)
            return;

        // distribute initial condition to segments
        for(Density D : ic.getDensity()){
            index = ml_link_id.indexOf(D.getLinkId());
            if(index>=0)
                segments.get(index).no += Double.parseDouble(D.getContent());
        }
    }

    protected void set_demands(DemandSet demand_set){

        int index;

        // reset everything to zero
        for(FwySegment seg : segments)
            seg.reset_demands();

        for(DemandProfile dp : demand_set.getDemandProfile()){
            index = or_source_id.indexOf(dp.getLinkIdOrg());
            if(index>=0)
                for(Demand d:dp.getDemand())
                    for(String str : Arrays.asList(d.getContent().split(",")))
                        segments.get(index).demand_profile.add(Double.parseDouble(str));
        }

    }
    protected void set_split_ratios(SplitRatioSet srs){
        //split_ratios.getSplitRatioProfile()
        for(SplitRatioProfile srp : srs.getSplitRatioProfile()){
//            srp.getNodeId()
//            fr_node_id
        }

    }

    ///////////////////////////////////////////////////////////////////
    // Override
    ///////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        String str = "";
        for(FwySegment s : this.segments){
            str = str.concat(String.format("%s\n",s.toString()));
        }
        return str;
    }
}
