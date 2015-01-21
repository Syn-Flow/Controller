package edu.routingAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
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
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.routing.RouteId;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.topology.NodePortTuple;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.type.SwitchActionTuple;

//use for convert routepath to <switch id, <actionList>>
public class RoutingActionManager implements IRoutingActionService,
	IFloodlightModule, IOFMessageListener {

    protected static Logger log;

    public static String MODULE_NAME = "RoutingActionManager";

    // Service
    protected IFloodlightProviderService floodlightProvider;
    protected IDeviceService deviceManager;
    protected IRoutingService routingEngine;
    protected ITopologyService topology;

    // REST api
    protected IRestApiService restApi;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	// TODO Auto-generated method stub

	Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();

	l.add(IRoutingActionService.class);

	return l;
	// return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
	// TODO Auto-generated method stub
	Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();

	m.put(IRoutingActionService.class, this);

	return m;
	// return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	// TODO Auto-generated method stub
	Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();

	l.add(IFloodlightProviderService.class);
	l.add(IRoutingService.class);
	l.add(IDeviceService.class);
	l.add(ITopologyService.class);

	// REST Api:
	l.add(IRestApiService.class);

	return l;
	// return null;
    }

    @Override
    public void init(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	// TODO Auto-generated method stub
	floodlightProvider = context
		.getServiceImpl(IFloodlightProviderService.class);

	deviceManager = context.getServiceImpl(IDeviceService.class);
	routingEngine = context.getServiceImpl(IRoutingService.class);
	topology = context.getServiceImpl(ITopologyService.class);

	// REST API
	restApi = context.getServiceImpl(IRestApiService.class);

	log = LoggerFactory.getLogger(RoutingActionManager.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	// TODO Auto-generated method stub
	floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);

	// REST Api
	restApi.addRestletRoutable(new RoutingActionWebRoutable());
    }

    // override to output
    public String toString() {

	StringBuilder builder = new StringBuilder();

	builder.append("Module Name is: " + MODULE_NAME);

	return builder.toString();
    }

    @Override
    public String getName() {
	// TODO Auto-generated method stub
	return MODULE_NAME;
	// return null;
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
	// TODO Auto-generated method stub

	// remainint to deal with some packet or drop it ..

	return Command.CONTINUE;
	// return null;
    }

    @Override
    public boolean routeExists(long src, long dst) {
	// TODO Auto-generated method stub

	return routingEngine.routeExists(src, dst);
    }

    @Override
    public boolean routeExists(long src, long dst, boolean tunnelEnabled) {
	// TODO Auto-generated method stub
	return routingEngine.routeExists(src, dst, tunnelEnabled);
    }

    @Override
    public List<SwitchActionTuple> getRoute(long src, long dst, long cookie) {
	// TODO Auto-generated method stub

	Route r = routingEngine.getRoute(src, dst, cookie);
	return routeToAction(r);
    }

    @Override
    public List<SwitchActionTuple> getRoute(long srcId, short srcPort,
	    long dstId, short dstPort, long cookie) {
	// TODO Auto-generated method stub
	Route r = routingEngine
		.getRoute(srcId, srcPort, dstId, dstPort, cookie);
	return routeToAction(r);
    }

    private List<SwitchActionTuple> routeToAction(Route r) {

	// Map<Long, List<OFAction>> buffer = new LinkedHashMap<Long,
	// List<OFAction>>();
	List<SwitchActionTuple> buffer = new ArrayList<SwitchActionTuple>();

	RouteId id = r.getId();
	List<NodePortTuple> switchPorts = r.getPath();

	int routeCount = r.getRouteCount();
	// System.out.println("routeCount: " + routeCount);
	System.out.println("id: " + id);
	System.out.println("switchPorts: " + switchPorts);

	for (int i = 0; i < switchPorts.size(); i++) {
	    if (i % 2 == 0)// in port, unuseful right row
		continue;
	    long switchDPID = switchPorts.get(i).getNodeId();
	    int outputPort = switchPorts.get(i).getPortId();
	    // System.out.println(switchDPID + " " + outputPort);
	    LinkedList<OFAction> actionList = new LinkedList<OFAction>();
	    actionList.add(new OFActionOutput(outputPort, (short) 0xffff));
	    // actionList.add(new OFXActionDecTTL());

	    SwitchActionTuple tmp = new SwitchActionTuple(switchDPID,
		    actionList);

	    buffer.add(tmp);
	}
	System.out.println(buffer.toString());
	return buffer;
    }
}
