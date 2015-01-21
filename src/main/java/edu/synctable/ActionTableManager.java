package edu.synctable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.ImmutablePort;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.action.OFXActionJump;
import edu.action.OFXActionPopShim;
import edu.executor.ExecutorService;
import edu.util.ActionMapper;

public class ActionTableManager implements IOFSwitchListener,
	IFloodlightModule, ActionTableService {

    IFloodlightProviderService floodlightProvider;
    Logger logger;

    ExecutorService executor;

    private static final int debug_size = 30;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	Collection<Class<? extends IFloodlightService>> services = new ArrayList<Class<? extends IFloodlightService>>(
		1);
	services.add(ActionTableService.class);
	return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
	Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	// We are the class that implements the service
	m.put(ActionTableService.class, this);
	return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	l.add(IFloodlightProviderService.class);
	l.add(ExecutorService.class);
	return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	floodlightProvider = context
		.getServiceImpl(IFloodlightProviderService.class);
	logger = LoggerFactory.getLogger(this.getClass());
	executor = context.getServiceImpl(ExecutorService.class);
	apAllocator.init();
    }

    @Override
    public void startUp(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	// TODO to be removed
	floodlightProvider.addOFSwitchListener(this);
	logger.info(this.getName() + " is up");
    }

    // @Override
    public String getName() {
	return "ActionTableManager";
    }

    // TODO new actions to be added
    private void installStaticActionEntry(IOFSwitch sw) throws Exception {
	LinkedList<OFAction> staticAction2 = new LinkedList<OFAction>();
	staticAction2.add(new OFXActionPopShim());
	staticAction2.add(new OFActionOutput(OFPort.OFPP_CONTROLLER,
		(short) 0xffff));

	// executor.SendOutOFXFlowMod(sw, (LinkedList<OFAction>) staticAction1);
	if (apAllocator.popAP((int) sw.getId(), (short) 0)) {
	    ActionMapper.add(sw.getId(),
		    new LinkedList<OFAction>(staticAction2), (short)0, -1);
	    staticAction2.addFirst(new OFXActionJump((short)0));
	    executor.SendOutOFXFlowMod(sw, staticAction2);
	} else {
	    throw new Exception("static entry can't be installed on: "
		    + sw.getId());
	}
    }

    @Override
    public void push(long switchID, int apValue) throws Exception {
	apAllocator.pushAP((int) switchID, apValue);

    }

    @Override
    public int pop(long switchID) throws Exception {
	return apAllocator.popAP((int) switchID);
    }

    @Override
    public void switchAdded(long switchId) {

	IOFSwitch sw = floodlightProvider.getSwitch(switchId);
	try {
	    installStaticActionEntry(sw);
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	// TODO to be deleted
	for (int i = 0; i < 5; i++) {
	    LinkedList<OFAction> staticActions = new LinkedList<OFAction>();
	    staticActions.add(new OFActionOutput(i + 1, (short) 0xffff));
	    try {
		if (apAllocator.popAP((int) switchId,
			(short) (i + 1 + (switchId - 1) * debug_size))) {
		    ActionMapper.add(switchId, new LinkedList<OFAction>(
			    staticActions), (short) (i + 1 + (switchId - 1)
			    * debug_size), -1);
		    staticActions.addFirst(new OFXActionJump(
			    (short) (i + 1 + (switchId - 1) * debug_size)));
		    executor.SendOutOFXFlowMod(sw, staticActions);
		} else {
		    throw new Exception(
			    "output static entries can't be installed on: "
				    + switchId);
		}
	    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	}
	for (int i = 0; i < debug_size * (switchId - 1); i++) {
	    apAllocator.popAP((int) switchId);
	}
    }

    @Override
    public void switchRemoved(long switchId) {
	ActionMapper.remove(switchId);
	apAllocator.initSw(switchId);
    }

    @Override
    public void switchActivated(long switchId) {
	// TODO Auto-generated method stub

    }

    @Override
    public void switchPortChanged(long switchId, ImmutablePort port,
	    PortChangeType type) {
	// TODO Auto-generated method stub

    }

    @Override
    public void switchChanged(long switchId) {
	// TODO Auto-generated method stub

    }
}
