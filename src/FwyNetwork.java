import edu.berkeley.path.beats.jaxb.*;
import edu.berkeley.path.beats.simulator.Link;
import edu.berkeley.path.beats.simulator.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gomes on 1/16/14.
 */
public class FwyNetwork {

    private List<FwySegment> segments;

    ///////////////////////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////////////////////

    public FwyNetwork(Network network,FundamentalDiagramSet fds,ActuatorSet actuatorset){

        segments = new ArrayList<FwySegment>();

        // find first mainline link, iterate downstream until you reach the end
        Link link = find_first_fwy_link(network);
        while(link!=null){
            Link onramp = start_node_onramp(link);
            Link offramp = end_node_offramp(link);
            FundamentalDiagram fd = get_fd_for_link(link,fds);
            Actuator actuator = get_onramp_actuator(onramp,actuatorset);
            segments.add(new FwySegment(link,onramp,offramp,fd,actuator));
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

        // gather links that are freeway sources, or sources the end in simple nodes
        List<Link> first_fwy_list = new ArrayList<Link>();
        for(edu.berkeley.path.beats.jaxb.Link jlink : network.getLinkList().getLink()){
            Link link = (Link) jlink;
            Node end_node = link.getEnd_node();
            boolean end_node_is_simple = end_node.getnIn()<=1 && end_node.getnOut()<=1;
            boolean is_or_source = end_node_is_simple
                    && end_node.getOutput_link().length>0
                    && isOnramp(end_node.getOutput_link()[0]);
            if(link.isSource())
                if( isFreeway(link) || !is_or_source)
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

        return first_fwy_list.get(0);
    }

    private boolean all_are_fwy_or_fr_src_snk(Network network){
        for(edu.berkeley.path.beats.jaxb.Link jlink : network.getLinkList().getLink()){
            Link link = (Link) jlink;
            if(!isFreeway(link) && !isOnramp(link) && !isOfframp(link) && !isSource(link) && !isSink(link))
                return false;
        }
        return true;
    }

    private Link end_node_offramp(Link link){
        for(Link olink : link.getEnd_node().getOutput_link()){
            if(isOfframp(olink))
                return olink;
        }
        return null;
    }

    private boolean isFreeway(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("freeway")==0;
    }

    private boolean isOnramp(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("on-ramp")==0;
    }

    private boolean isOfframp(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("off-ramp")==0;
    }

    private boolean isSource(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("source")==0;
    }

    private boolean isSink(Link link){
        return link.getLinkType().getName().compareToIgnoreCase("sink")==0;
    }

    private Link next_freeway_link(Link link){
        for(Link olink : link.getEnd_node().getOutput_link()){
            if(isFreeway(olink))
                return olink;
        }
        return null;
    }

    private Link start_node_onramp(Link link){
        for(Link ilink : link.getBegin_node().getInput_link()){
            if(isOnramp(ilink))
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
