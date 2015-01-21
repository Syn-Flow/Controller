package edu.executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.floodlightcontroller.counter.ICounterStoreService;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketIn.OFPacketInReason;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.instruction.OFInstruction;
import org.openflow.protocol.instruction.OFInstructionApplyActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.action.OFXActionJump;
import edu.action.OFXActionPopShim;
import edu.action.OFXActionPushShim;
import edu.messagetype.OFXFlowMod;
import edu.messagetype.OFXFlowModReply;
import edu.messagetype.OFXPacketOut;
import edu.synctable.apAllocator;
import edu.type.ActionPointerList;
import edu.type.SwitchActionTuple;
import edu.util.ActionMapper;

public class Executor implements IFloodlightModule, ExecutorService,
	IOFMessageListener {

    protected IFloodlightProviderService floodlightProvider;
    protected ICounterStoreService counterStore;
    protected Logger logger;

    @Override
    public boolean Execute(ArrayList<SwitchActionTuple> switchActionTupleList,
	    OFMatch match, OFPacketIn pi, int appID) throws Exception {

	// installing from tail to head

	// ingress switch
	IOFSwitch inSw = floodlightProvider.getSwitch(switchActionTupleList
		.get(0).getSwitchID());
	LinkedList<OFAction> inActions = switchActionTupleList.get(0)
		.getActions();

	// excluding one switch case
	if (switchActionTupleList.size() == 1) {
	    // do nothing
	} else {
	    // the apList to be inserted at the first switch
	    ActionPointerList apList = new ActionPointerList();
	    ArrayList<SwitchActionTuple> coreActions = new ArrayList<SwitchActionTuple>(
		    switchActionTupleList.size() - 1);
	    // Core switches
	    // from tail to head
	    for (int i = switchActionTupleList.size() - 1; i > 0; i--) {
		LinkedList<OFAction> actions = switchActionTupleList.get(i)
			.getActions();
		long swID = switchActionTupleList.get(i).getSwitchID();
		if (i == switchActionTupleList.size() - 1)
		    actions.addFirst(new OFXActionPopShim());
		short ap = ActionMapper.map(swID, new LinkedList<OFAction>(
			actions), 0);
		if (ap == -1) {
		    ap = apAllocator.popAP((int) swID);
		    if (ap == -1)
			return false;
		    apList.addFirst(ap);
		    actions.addFirst(new OFXActionJump(ap));
		    coreActions.add(new SwitchActionTuple(swID, actions));
		} else {
		    apList.addFirst(ap);
		    coreActions.add(new SwitchActionTuple(swID, null));
		}
	    }
	    // from tail to head
	    for (int i = 0; i < coreActions.size(); i++) {
		if (coreActions.get(i).getActions() != null)
		    SendOutOFXFlowMod(floodlightProvider.getSwitch(coreActions
			    .get(i).getSwitchID()), coreActions.get(i)
			    .getActions());
	    }
	    inActions.addFirst(new OFXActionPushShim(
		    (byte) switchActionTupleList.size(), apList));
	}
	SendOutFlowMod(inSw, match, inActions);
	SendOutPktIn(inSw.getId(), pi, inActions);
	return true;
    }

    @Override
    public void SendOutOFXFlowMod(IOFSwitch sw, List<OFAction> actions) {
	OFXFlowMod flowMod = (OFXFlowMod) floodlightProvider
		.getOFMessageFactory().getMessage(OFType.OFX_FLOW_MOD);
	// TODO something to be done with the cookie
	flowMod.setCommand(OFXFlowMod.OFPFC_ADD);
	flowMod.setIdleTimeout((short) 0);
	flowMod.setHardTimeout((short) 0);
	flowMod.setFlags(OFXFlowMod.OFPFF_SEND_FLOW_REM);

	List<OFInstruction> instructions = Arrays
		.asList((OFInstruction) new OFInstructionApplyActions()
			.setActions(actions));
	flowMod.setInstructions(instructions);

	counterStore.updatePktOutFMCounterStoreLocal(sw, flowMod);

	// and write it out
	try {
	    sw.write(flowMod, null);
	    logger.info("OFXFlowMod sent for switch: " + sw.getId()
		    + ", apValue: "
		    + ((OFXActionJump) actions.get(0)).getActionPointerValue()
		    + ", ins:" + flowMod.getInstructions());
	    sw.flush();
	    // TODO add apValue here to debug
	} catch (IOException e) {
	    logger.error("Failed to write {} to switch {}", new Object[] {
		    flowMod, sw }, e);
	}

    }

    @Override
    public void SendOutFlowMod(IOFSwitch sw, OFMatch match,
	    List<OFAction> actions) {
	OFFlowMod flowMod = (OFFlowMod) floodlightProvider
		.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
	// TODO something to be done with the cookie
	flowMod.setCookie(0x2333);
	flowMod.setCommand(OFFlowMod.OFPFC_ADD);
	// TODO and timeout
	flowMod.setIdleTimeout((short) 0);
	flowMod.setHardTimeout((short) 0);
	flowMod.setPriority((short) 100);
	flowMod.setBufferId(-1);
	flowMod.setOutPort(OFPort.OFPP_ANY.getValue());
	flowMod.setFlags(OFFlowMod.OFPFF_SEND_FLOW_REM);
	flowMod.setMatch(match);
	List<OFInstruction> instructions = Arrays
		.asList((OFInstruction) new OFInstructionApplyActions()
			.setActions(actions));
	flowMod.setInstructions(instructions);

	counterStore.updatePktOutFMCounterStoreLocal(sw, flowMod);
	try {
	    sw.write(flowMod, null);
	    logger.info("OFFlowMod sent for switch: " + sw.getId() + ", ins:"
		    + flowMod.getInstructions());

	} catch (IOException e) {
	    logger.error("Failed to write {} to switch {}", new Object[] {
		    flowMod, sw }, e);
	}

    }

    @Override
    public void SendOutPktIn(long switchID, OFPacketIn pktIn,
	    List<OFAction> actions) {
	IOFSwitch sw = floodlightProvider.getSwitch(switchID);
	OFPacketOut pktOut = (OFPacketOut) floodlightProvider
		.getOFMessageFactory().getMessage(OFType.PACKET_OUT);
	pktOut.setActions(actions);

	if (sw.getBuffers() == 0) {
	    // We set the PI buffer id here so we don't have to check again
	    // below
	    pktIn.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	    pktOut.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	} else {
	    pktOut.setBufferId(pktIn.getBufferId());
	}

	pktOut.setInPort(pktIn.getInPort());

	if (pktIn.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
	    byte[] packetData = pktIn.getPacketData();
	    pktOut.setPacketData(packetData);
	}

	try {
	    counterStore.updatePktOutFMCounterStoreLocal(sw, pktOut);
	    sw.write(pktOut, null);
	    sw.flush();
	} catch (IOException e) {
	    logger.error("Failure writing packet out", e);
	}

    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	Collection<Class<? extends IFloodlightService>> services = new ArrayList<Class<? extends IFloodlightService>>(
		1);
	services.add(ExecutorService.class);
	return services;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
	Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
	m.put(ExecutorService.class, this);
	return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	l.add(IFloodlightProviderService.class);
	l.add(ICounterStoreService.class);
	return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	floodlightProvider = context
		.getServiceImpl(IFloodlightProviderService.class);
	counterStore = context.getServiceImpl(ICounterStoreService.class);

	apAllocator.init();
	logger = LoggerFactory.getLogger(this.getClass());

    }

    public String getName() {
	return "F3Executor";
    }

    @Override
    public void startUp(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	floodlightProvider
		.addOFMessageListener(OFType.OFX_FLOW_MOD_REPLY, this);
	floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	// TODO to be removed
	logger.info(this.getName() + " is up");

    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
	return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
	return type.equals(OFType.OFX_FLOW_MOD_REPLY)
		|| type.equals(OFType.PACKET_IN);
    }

    @Override
    public net.floodlightcontroller.core.IListener.Command receive(
	    IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
	try {
	    if (msg.getType().equals(OFType.PACKET_IN)) {
		OFPacketIn pki = (OFPacketIn) msg;
		if (!(pki.getReason().equals(OFPacketInReason.OFX_AP_MISS)))
		    return Command.CONTINUE;
		OFXPacketOut po = (OFXPacketOut) floodlightProvider
			.getOFMessageFactory()
			.getMessage(OFType.OFX_PACKET_OUT);
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(new OFActionOutput(-1, (short) 0xffff));

		po.setActions(actions);
		po.setBufferId(OFXPacketOut.BUFFER_ID_NONE);
		po.setInPort(pki.getInPort());
		po.setPacketData(pki.getPacketData());
		sw.write(po, null);
		sw.flush();
		System.out
			.println("---------------sending out f3 packetout!!!---------------");
		return Command.STOP;

	    }
	    return ProcessOFXFlowModReply(sw, (OFXFlowModReply) msg);
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return Command.STOP;
    }

    protected net.floodlightcontroller.core.IListener.Command ProcessOFXFlowModReply(
	    IOFSwitch sw, OFXFlowModReply msg) throws Exception {
	if (msg.getStatus() > 0) {
	    // TODO
	    System.out.println("OFXFlowModError: " + sw.getId() + ", " + msg);
	}
	return Command.STOP;
    }
}
