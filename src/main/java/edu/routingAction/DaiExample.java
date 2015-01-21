package edu.routingAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.topology.ITopologyService;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFOXMFieldType;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.executor.ExecutorService;
import edu.type.SwitchActionTuple;

public class DaiExample implements IFloodlightModule, IOFMessageListener {

    private static final int routingID = 0;
    protected IFloodlightProviderService floodlightProvider;
    protected IRoutingActionService routingActionEngine;
    protected ITopologyService topology;
    protected ExecutorService executor;

    protected Logger log = LoggerFactory.getLogger(DaiExample.class);

    @Override
    public String getName() {
	// TODO Auto-generated method stub
	return DaiExample.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
	return (type.equals(OFType.PACKET_IN) && (name.equals("topology") || name
		.equals("devicemanager")));
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
	return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	// TODO Auto-generated method stub
	// the example provide no service
	return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
	// TODO Auto-generated method stub
	// this example provide no service
	return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	// TODO Auto-generated method stub
	Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	l.add(IFloodlightProviderService.class);
	l.add(IRoutingActionService.class);
	l.add(ExecutorService.class);

	return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	// TODO Auto-generated method stub
	floodlightProvider = context
		.getServiceImpl(IFloodlightProviderService.class);
	routingActionEngine = context
		.getServiceImpl(IRoutingActionService.class);
	executor = context.getServiceImpl(ExecutorService.class);
	topology = context.getServiceImpl(ITopologyService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	// TODO Auto-generated method stub
	floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }

    @Override
    public net.floodlightcontroller.core.IListener.Command receive(
	    IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
	// TODO Auto-generated method stub

	// log.info("received a PACKET_IN");
	Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
		IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

	if (msg.getType() == OFType.PACKET_IN) {
	    OFPacketIn pi = (OFPacketIn) msg;

	    // log.info(match.toString());
	    if (eth.isBroadcast() || eth.isMulticast()) {
		// For now we treat multicast as broadcast
		// System.out.println("doFlood");
		// doFlood(sw, pi, cntx);
	    } else {
		// System.out.println("forwardFlow");
		doForwardFlow(sw, pi, cntx, false);
	    }

	}
	// Route r = routingActionEngine.getRoute();
	return Command.CONTINUE;
    }

    protected void doForwardFlow(IOFSwitch sw, OFPacketIn pi,
	    FloodlightContext cntx, boolean requestFlowRemovedNotifn) {
	OFMatch match = new OFMatch();
	match.loadFromPacket(pi.getPacketData(), pi.getInPort());
	match.setNonWildcards(EnumSet.of(OFOXMFieldType.ETH_TYPE,
		OFOXMFieldType.ETH_SRC, OFOXMFieldType.ETH_DST,
		OFOXMFieldType.IPV4_SRC, OFOXMFieldType.IPV4_DST));
	System.out.println("swID: " + sw.getId() + ", match: ");
	System.out.println(HexString.toHexString(match.getDataLayerSource()));
	System.out.println(HexString.toHexString(match
		.getDataLayerDestination()));
	System.out.println(Integer.toHexString(match.getNetworkSource()));
	System.out.println(Integer.toHexString(match.getNetworkDestination()));
	// get destination device informations.
	IDevice dstDevice = IDeviceService.fcStore.get(cntx,
		IDeviceService.CONTEXT_DST_DEVICE);

	// get the host device's attachmentPoints
	// SwitchPort[] ap = dstDevice.getAttachmentPoints();
	// System.out.println(ap.length);
	// System.out.println(ap[i].toString());

	if (dstDevice != null) {
	    IDevice srcDevice = IDeviceService.fcStore.get(cntx,
		    IDeviceService.CONTEXT_SRC_DEVICE);

	    Long srcIsland = topology.getL2DomainId(sw.getId());
	    // it's a kind of cluster! remain to read.

	    if (srcDevice == null) {
		log.debug("No device entry found for source device");
		return;
	    }
	    if (srcIsland == null) {
		log.debug("No openflow island found for source {}/{}",
			sw.getStringId(), pi.getInPort());
		return;
	    }

	    // Validate that we have a destination known on the same island
	    // Validate that the source and destination are not on the same
	    // switchport
	    boolean on_same_island = false;
	    boolean on_same_if = false;
	    for (SwitchPort dstDap : dstDevice.getAttachmentPoints()) {
		long dstSwDpid = dstDap.getSwitchDPID();
		Long dstIsland = topology.getL2DomainId(dstSwDpid);
		if ((dstIsland != null) && dstIsland.equals(srcIsland)) {
		    on_same_island = true;
		    if ((sw.getId() == dstSwDpid)
			    && (pi.getInPort() == dstDap.getPort())) {
			on_same_if = true;
		    }
		    break;
		}
	    }

	    if (!on_same_island) {
		// we dont know dst device, need flood.
		// doFlood();
	    }

	    if (on_same_if) {
		// the source and dst are attach to same switch, same port,
		return;
	    }

	    SwitchPort[] srcDaps = srcDevice.getAttachmentPoints();
	    Arrays.sort(srcDaps, clusterIdComparator);
	    SwitchPort[] dstDaps = dstDevice.getAttachmentPoints();
	    Arrays.sort(dstDaps, clusterIdComparator);

	    // src, dst attachment, sorted by island.
	    int iSrcDaps = 0, iDstDaps = 0;
	    // start iterate for every possible route.

	    while ((iSrcDaps < srcDaps.length) && (iDstDaps < dstDaps.length)) {
		SwitchPort srcDap = srcDaps[iSrcDaps];
		SwitchPort dstDap = dstDaps[iDstDaps];

		// srcCluster and dstCluster here cannot be null as
		// every switch will be at least in its own L2 domain.
		Long srcCluster = topology
			.getL2DomainId(srcDap.getSwitchDPID());
		Long dstCluster = topology
			.getL2DomainId(dstDap.getSwitchDPID());

		int srcVsDest = srcCluster.compareTo(dstCluster);

		if (srcVsDest == 0) {
		    if (!srcDap.equals(dstDap)) {
			List<SwitchActionTuple> swichActions = routingActionEngine
				.getRoute(srcDap.getSwitchDPID(),
					(short) srcDap.getPort(),
					dstDap.getSwitchDPID(),
					(short) dstDap.getPort(), 0);
			try {
			    executor.Execute(
				    (ArrayList<SwitchActionTuple>) swichActions,
				    match, pi, routingID);
			} catch (Exception e) {
			    e.printStackTrace();
			}

			// System.out.println("route found!: " +
			// route.toString());
		    }
		    iSrcDaps++;
		    iDstDaps++;
		} else if (srcVsDest < 0) {
		    iSrcDaps++;
		} else {
		    iDstDaps++;
		}
	    }

	} else {
	    // we dont know dst device
	    // doflood();
	}
    }

    // Comparator for sorting by SwitchCluster
    public Comparator<SwitchPort> clusterIdComparator = new Comparator<SwitchPort>() {
	@Override
	public int compare(SwitchPort d1, SwitchPort d2) {
	    Long d1ClusterId = topology.getL2DomainId(d1.getSwitchDPID());
	    Long d2ClusterId = topology.getL2DomainId(d2.getSwitchDPID());
	    return d1ClusterId.compareTo(d2ClusterId);
	}
    };

}
