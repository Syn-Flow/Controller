package edu.routingAction;

import net.floodlightcontroller.linkdiscovery.web.DirectedLinksResource;
import net.floodlightcontroller.linkdiscovery.web.ExternalLinksResource;
import net.floodlightcontroller.linkdiscovery.web.LinksResource;
import net.floodlightcontroller.restserver.RestletRoutable;
import net.floodlightcontroller.topology.web.BlockedPortsResource;
import net.floodlightcontroller.topology.web.BroadcastDomainPortsResource;
import net.floodlightcontroller.topology.web.EnabledPortsResource;
import net.floodlightcontroller.topology.web.RouteResource;
import net.floodlightcontroller.topology.web.SwitchClustersResource;
import net.floodlightcontroller.topology.web.TunnelLinksResource;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class RoutingActionWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		// TODO Auto-generated method stub
		Router router = new Router(context);
        
		
		router.attach("/links/json", LinksResource.class);
        router.attach("/directed-links/json", DirectedLinksResource.class);
        router.attach("/external-links/json", ExternalLinksResource.class);
        router.attach("/tunnellinks/json", TunnelLinksResource.class);
        router.attach("/switchclusters/json", SwitchClustersResource.class);
        router.attach("/broadcastdomainports/json", BroadcastDomainPortsResource.class);
        router.attach("/enabledports/json", EnabledPortsResource.class);
        router.attach("/blockedports/json", BlockedPortsResource.class);
        router.attach("/route/{src-dpid}/{src-port}/{dst-dpid}/{dst-port}/json", RouteResource.class);
        
        //route action api
        router.attach("/ra/{src-dpid}/{src-port}/{dst-dpid}/{dst-port}/json", RoutingActionResource.class);
		return router;
    }

	@Override
	public String basePath() {
		// TODO Auto-generated method stub
		return "/dai/ra";//ra for routingAction
	}

}
