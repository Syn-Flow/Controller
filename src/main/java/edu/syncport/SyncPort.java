package edu.syncport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.ImmutablePort;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;

import org.openflow.protocol.OFPhysicalPort.OFPortConfig;
import org.openflow.protocol.OFPortMod;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.linkdiscovery.F3LinkDiscoveryService;

public class SyncPort implements IFloodlightModule, ILinkDiscoveryListener,
	IDeviceListener {

    IFloodlightProviderService floodlightProvider;
    protected IDeviceService device;
    protected F3LinkDiscoveryService linkDiscovery;
    Logger logger;

    protected Map<Long, Map<Integer, Boolean>> portsyncmap;

    public void SendPortMod(long dpid, int portNumber, int PortConfig) {
	IOFSwitch sw = floodlightProvider.getSwitch(dpid);
	ImmutablePort port = sw.getPort(portNumber);
	OFPortMod portmod = new OFPortMod();
	portmod.setConfig(PortConfig);
	portmod.setMask((OFPortConfig.OFXPPC_ATTR).getValue()
		+ (OFPortConfig.OFXPPC_STATUS).getValue());
	portmod.setPortNumber(port.getPortNumber());
	portmod.setHardwareAddress(port.getHardwareAddress());
	try {
	    sw.write(portmod, null);
	} catch (IOException e) {

	    e.printStackTrace();
	}
    }

    public void SendPortMod(IOFSwitch sw, ImmutablePort port, int PortConfig) {
	OFPortMod portmod = new OFPortMod();
	portmod.setConfig(PortConfig);
	portmod.setMask((OFPortConfig.OFXPPC_ATTR).getValue()
		+ (OFPortConfig.OFXPPC_STATUS).getValue());
	portmod.setPortNumber(port.getPortNumber());
	portmod.setHardwareAddress(port.getHardwareAddress());
	try {
	    sw.write(portmod, null);
	} catch (IOException e) {

	    e.printStackTrace();
	}
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
	return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
	return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
	Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	l.add(IFloodlightProviderService.class);
	l.add(F3LinkDiscoveryService.class);
	l.add(IDeviceService.class);
	return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	floodlightProvider = context
		.getServiceImpl(IFloodlightProviderService.class);
	linkDiscovery = context.getServiceImpl(F3LinkDiscoveryService.class);
	device = context.getServiceImpl(IDeviceService.class);
	portsyncmap = new HashMap<Long, Map<Integer, Boolean>>();
	logger = LoggerFactory.getLogger(SyncPort.class);

    }

    @Override
    public void startUp(FloodlightModuleContext context)
	    throws FloodlightModuleException {
	linkDiscovery.addListener(this);
	device.addListener(this);
	// TODO to be removed
	logger.info(this.getName() + " is up");

    }

    public void handleSingleUpdate(LDUpdate update) {
	long src = update.getSrc();// get srcSwitch
	int srcPort = update.getSrcPort();
	long dst = update.getDst();// get dstSwitch
	int dstPort = update.getDstPort();
	System.out.println(update);

	switch (update.getOperation()) {
	case LINK_REMOVED:
	    portsyncmap.get(src).put(srcPort, false);
	    SendPortMod(src, srcPort, 0);
	    portsyncmap.get(dst).put(dstPort, false);
	    SendPortMod(dst, dstPort, 0);
	    break;
	case LINK_UPDATED:
	    if (!portsyncmap.containsKey(src)) {
		portsyncmap.put(src, new HashMap<Integer, Boolean>());
	    }
	    if (portsyncmap.get(src) != null
		    && portsyncmap.get(src).get(srcPort) == null) {
		portsyncmap.get(src).put(srcPort, true);
		SendPortMod(src, srcPort, OFPortConfig.OFXPPC_STATUS.getValue());
	    }
	    if (!portsyncmap.containsKey(dst)) {
		portsyncmap.put(src, new HashMap<Integer, Boolean>());
	    }
	    if (portsyncmap.get(dst) != null
		    && portsyncmap.get(dst).get(dstPort) == null) {
		portsyncmap.get(dst).put(dstPort, true);
		SendPortMod(dst, dstPort, OFPortConfig.OFXPPC_STATUS.getValue());
	    }
	    break;
	case PORT_DOWN:
	    SendPortMod(src, srcPort, 0);
	    break;
	case PORT_UP:
	    break;
	case SWITCH_REMOVED:
	    portsyncmap.remove(src);
	    break;
	case SWITCH_UPDATED:
	    break;
	case TUNNEL_PORT_ADDED:
	case TUNNEL_PORT_REMOVED:
	default:
	    break;
	}
    }

    @Override
    public void linkDiscoveryUpdate(LDUpdate update) {
	handleSingleUpdate(update);
    }

    @Override
    public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
	for (LDUpdate update : updateList) {
	    handleSingleUpdate(update);
	}

    }

    @Override
    public String getName() {
	return "SyncPort";
    }

    @Override
    public boolean isCallbackOrderingPrereq(String type, String name) {
	return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(String type, String name) {
	return true;
    }

    @Override
    public void deviceAdded(IDevice device) {
	System.out.print(HexString.toHexString(device.getMACAddress())
		+ "Device added" + ", attached to ");
	SwitchPort switchPort = device.getAttachmentPoints()[0];
	IOFSwitch sw = floodlightProvider.getSwitch(switchPort.getSwitchDPID());
	ImmutablePort port = sw.getPort(switchPort.getPort());
	if (port.getPortNumber() > 0) {
	    SendPortMod(sw, port, (OFPortConfig.OFXPPC_ATTR).getValue()
		    + (OFPortConfig.OFXPPC_STATUS).getValue());
	}
	System.out.println(switchPort);
    }

    @Override
    public void deviceRemoved(IDevice device) {
	System.out.println("Device removed"
		+ HexString.toHexString(device.getMACAddress()));
	SwitchPort switchPort = device.getAttachmentPoints()[0];
	IOFSwitch sw = floodlightProvider.getSwitch(switchPort.getSwitchDPID());
	ImmutablePort port = sw.getPort(switchPort.getPort());
	if (port.getPortNumber() > 0) {
	    SendPortMod(sw, port, 0);
	}
    }

    @Override
    public void deviceMoved(IDevice device) {

    }

    @Override
    public void deviceIPV4AddrChanged(IDevice device) {
	System.out.println("Device IPv4 addr changed: "
		+ HexString.toHexString(device.getMACAddress()));
    }

    @Override
    public void deviceVlanChanged(IDevice device) {

    }

}
