package edu.routingAction;

import java.util.List;

import org.openflow.util.HexString;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.type.SwitchActionTuple;

public class RoutingActionResource extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(RoutingActionResource.class);

	@Get("json")
	public List<SwitchActionTuple> retrieve() {
		IRoutingActionService routingAction = (IRoutingActionService) getContext()
				.getAttributes().get(IRoutingActionService.class.getCanonicalName());

		String srcDpid = (String) getRequestAttributes().get("src-dpid");
		String srcPort = (String) getRequestAttributes().get("src-port");
		String dstDpid = (String) getRequestAttributes().get("dst-dpid");
		String dstPort = (String) getRequestAttributes().get("dst-port");

		log.debug(srcDpid + "--" + srcPort + "--" + dstDpid + "--" + dstPort);

		long longSrcDpid = HexString.toLong(srcDpid);
		short shortSrcPort = Short.parseShort(srcPort);
		long longDstDpid = HexString.toLong(dstDpid);
		short shortDstPort = Short.parseShort(dstPort);

		List<SwitchActionTuple> result = routingAction.getRoute(longSrcDpid, shortSrcPort, longDstDpid,
				shortDstPort, 0);

		if (result != null) {
			return result;
		} else {
			log.debug("ERROR! no route found");
			return null;
		}
	}
}
