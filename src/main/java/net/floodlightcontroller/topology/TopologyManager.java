/**
 *    Copyright 2013, Big Switch Networks, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.topology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.HAListenerTypeMarker;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IHAListener;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.annotations.LogMessageCategory;
import net.floodlightcontroller.core.annotations.LogMessageDoc;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.counter.ICounterStoreService;
import net.floodlightcontroller.debugcounter.IDebugCounter;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.debugcounter.IDebugCounterService.CounterException;
import net.floodlightcontroller.debugcounter.IDebugCounterService.CounterType;
import net.floodlightcontroller.debugcounter.NullDebugCounter;
import net.floodlightcontroller.debugevent.IDebugEventService;
import net.floodlightcontroller.debugevent.IDebugEventService.EventColumn;
import net.floodlightcontroller.debugevent.IDebugEventService.EventFieldType;
import net.floodlightcontroller.debugevent.IDebugEventService.EventType;
import net.floodlightcontroller.debugevent.IDebugEventService.MaxEventsRegistered;
import net.floodlightcontroller.debugevent.IEventUpdater;
import net.floodlightcontroller.debugevent.NullDebugEvent;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
//import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.BSN;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.LLDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.topology.web.TopologyWebRoutable;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Yikai

import edu.linkdiscovery.F3LinkDiscoveryService;

/**
 * Topology manager is responsible for maintaining the controller's notion
 * of the network graph, as well as implementing tools for finding routes
 * through the topology.
 */
@LogMessageCategory("Network Topology")
public class TopologyManager implements
        IFloodlightModule, ITopologyService,
        IRoutingService, ILinkDiscoveryListener,
        IOFMessageListener {

    protected static Logger log = LoggerFactory.getLogger(TopologyManager.class);

    public static final String MODULE_NAME = "topology";

    public static final String CONTEXT_TUNNEL_ENABLED =
            "com.bigswitch.floodlight.topologymanager.tunnelEnabled";

    /**
     * Role of the controller.
     */
    private Role role;

    /**
     * Set of ports for each switch
     */
    protected Map<Long, Set<Integer>> switchPorts;

    /**
     * Set of links organized by node port tuple
     */
    protected Map<NodePortTuple, Set<Link>> switchPortLinks;

    /**
     * Set of direct links
     */
    protected Map<NodePortTuple, Set<Link>> directLinks;

    /**
     * set of links that are broadcast domain links.
     */
    protected Map<NodePortTuple, Set<Link>> portBroadcastDomainLinks;

    /**
     * set of tunnel links
     */
    protected Set<NodePortTuple> tunnelPorts;
    //Yikai
    protected F3LinkDiscoveryService linkDiscovery;
    protected IThreadPoolService threadPool;
    protected IFloodlightProviderService floodlightProvider;
    protected IRestApiService restApi;
    protected IDebugCounterService debugCounters;

    // Modules that listen to our updates
    protected ArrayList<ITopologyListener> topologyAware;

    protected BlockingQueue<LDUpdate> ldUpdates;

    // These must be accessed using getCurrentInstance(), not directly
    protected TopologyInstance currentInstance;
    protected TopologyInstance currentInstanceWithoutTunnels;

    protected SingletonTask newInstanceTask;
    private Date lastUpdateTime;

    /**
     * Flag that indicates if links (direct/tunnel/multihop links) were
     * updated as part of LDUpdate.
     */
    protected boolean linksUpdated;
    /**
     * Flag that indicates if direct or tunnel links were updated as
     * part of LDUpdate.
     */
    protected boolean dtLinksUpdated;

    /** Flag that indicates if tunnel ports were updated or not
     */
    protected boolean tunnelPortsUpdated;

    protected int TOPOLOGY_COMPUTE_INTERVAL_MS = 500;

    private IHAListener haListener;

    /**
     *  Debug Counters
     */
    protected static final String PACKAGE = TopologyManager.class.getPackage().getName();
    protected IDebugCounter ctrIncoming;

    /**
     * Debug Events
     */
    protected IDebugEventService debugEvents;

    /*
     * Topology Event Updater
     */
    protected IEventUpdater<TopologyEvent> evTopology;

    /**
     * Topology Information exposed for a Topology related event - used inside
     * the BigTopologyEvent class
     */
    protected class TopologyEventInfo {
        private final int numOpenflowClustersWithTunnels;
        private final int numOpenflowClustersWithoutTunnels;
        private final Map<Long, List<NodePortTuple>> externalPortsMap;
        private final int numTunnelPorts;
        public TopologyEventInfo(int numOpenflowClustersWithTunnels,
                                 int numOpenflowClustersWithoutTunnels,
                                 Map<Long, List<NodePortTuple>> externalPortsMap,
                                 int numTunnelPorts) {
            super();
            this.numOpenflowClustersWithTunnels = numOpenflowClustersWithTunnels;
            this.numOpenflowClustersWithoutTunnels = numOpenflowClustersWithoutTunnels;
            this.externalPortsMap = externalPortsMap;
            this.numTunnelPorts = numTunnelPorts;
        }
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("# Openflow Clusters:");
            builder.append(" { With Tunnels: ");
            builder.append(numOpenflowClustersWithTunnels);
            builder.append(" Without Tunnels: ");
            builder.append(numOpenflowClustersWithoutTunnels);
            builder.append(" }");
            builder.append(", # External Clusters: ");
            int numExternalClusters = externalPortsMap.size();
            builder.append(numExternalClusters);
            if (numExternalClusters > 0) {
                builder.append(" { ");
                int count = 0;
                for (Long extCluster : externalPortsMap.keySet()) {
                    builder.append("#" + extCluster + ":Ext Ports: ");
                    builder.append(externalPortsMap.get(extCluster).size());
                    if (++count < numExternalClusters) {
                        builder.append(", ");
                    } else {
                        builder.append(" ");
                    }
                }
                builder.append("}");
            }
            builder.append(", # Tunnel Ports: ");
            builder.append(numTunnelPorts);
            return builder.toString();
        }
    }

    /**
     * Topology Event class to track topology related events
     */
    protected class TopologyEvent {
        @EventColumn(name = "Reason", description = EventFieldType.STRING)
        private final String reason;
        @EventColumn(name = "Topology Summary")
        private final TopologyEventInfo topologyInfo;
        public TopologyEvent(String reason,
                TopologyEventInfo topologyInfo) {
            super();
            this.reason = reason;
            this.topologyInfo = topologyInfo;
        }
    }

   //  Getter/Setter methods
    /**
     * Get the time interval for the period topology updates, if any.
     * The time returned is in milliseconds.
     * @return
     */
    public int getTopologyComputeInterval() {
        return TOPOLOGY_COMPUTE_INTERVAL_MS;
    }

    /**
     * Set the time interval for the period topology updates, if any.
     * The time is in milliseconds.
     * @return
     */
    public void setTopologyComputeInterval(int time_ms) {
        TOPOLOGY_COMPUTE_INTERVAL_MS = time_ms;
    }

    /**
     * Thread for recomputing topology.  The thread is always running,
     * however the function applyUpdates() has a blocking call.
     */
    @LogMessageDoc(level="ERROR",
            message="Error in topology instance task thread",
            explanation="An unknown error occured in the topology " +
            		"discovery module.",
            recommendation=LogMessageDoc.CHECK_CONTROLLER)
    protected class UpdateTopologyWorker implements Runnable {
        @Override
        public void run() {
            try {
                if (ldUpdates.peek() != null)
                    updateTopology();
                handleMiscellaneousPeriodicEvents();
            }
            catch (Exception e) {
                log.error("Error in topology instance task thread", e);
            } finally {
                if (floodlightProvider.getRole() != Role.SLAVE)
                    newInstanceTask.reschedule(TOPOLOGY_COMPUTE_INTERVAL_MS,
                                           TimeUnit.MILLISECONDS);
            }
        }
    }

    // To be used for adding any periodic events that's required by topology.
    protected void handleMiscellaneousPeriodicEvents() {
        return;
    }

    public boolean updateTopology() {
        boolean newInstanceFlag;
        linksUpdated = false;
        dtLinksUpdated = false;
        tunnelPortsUpdated = false;
        List<LDUpdate> appliedUpdates = applyUpdates();
        newInstanceFlag = createNewInstance("link-discovery-updates");
        lastUpdateTime = new Date();
        informListeners(appliedUpdates);
        return newInstanceFlag;
    }

    // **********************
    // ILinkDiscoveryListener
    // **********************
    @Override
    public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
        if (log.isTraceEnabled()) {
            log.trace("Queuing update: {}", updateList);
        }
        ldUpdates.addAll(updateList);
    }

    @Override
    public void linkDiscoveryUpdate(LDUpdate update) {
        if (log.isTraceEnabled()) {
            log.trace("Queuing update: {}", update);
        }
        ldUpdates.add(update);
    }

    // ****************
    // ITopologyService
    // ****************

    //
    // ITopologyService interface methods
    //
    @Override
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public void addListener(ITopologyListener listener) {
        topologyAware.add(listener);
    }

    @Override
    public boolean isAttachmentPointPort(long switchid, int port) {
        return isAttachmentPointPort(switchid, port, true);
    }

    @Override
    public boolean isAttachmentPointPort(long switchid, int port,
                                         boolean tunnelEnabled) {

        // If the switch port is 'tun-bsn' port, it is not
        // an attachment point port, irrespective of whether
        // a link is found through it or not.
        if (linkDiscovery.isTunnelPort(switchid, port))
            return false;

        TopologyInstance ti = getCurrentInstance(tunnelEnabled);

        // if the port is not attachment point port according to
        // topology instance, then return false
        if (ti.isAttachmentPointPort(switchid, port) == false)
                return false;

        // Check whether the port is a physical port. We should not learn
        // attachment points on "special" ports.
        if ((port & 0xff00) == 0xff00 && port != (short)0xfffe) return false;

        // Make sure that the port is enabled.
        IOFSwitch sw = floodlightProvider.getSwitch(switchid);
        if (sw == null) return false;
        return (sw.portEnabled(port));
    }

    @Override
    public long getOpenflowDomainId(long switchId) {
        return getOpenflowDomainId(switchId, true);
    }

    @Override
    public long getOpenflowDomainId(long switchId, boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getOpenflowDomainId(switchId);
    }

    @Override
    public long getL2DomainId(long switchId) {
        return getL2DomainId(switchId, true);
    }

    @Override
    public long getL2DomainId(long switchId, boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getL2DomainId(switchId);
    }

    @Override
    public boolean inSameOpenflowDomain(long switch1, long switch2) {
        return inSameOpenflowDomain(switch1, switch2, true);
    }

    @Override
    public boolean inSameOpenflowDomain(long switch1, long switch2,
                                        boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.inSameOpenflowDomain(switch1, switch2);
    }

    @Override
    public boolean isAllowed(long sw, int portId) {
        return isAllowed(sw, portId, true);
    }

    @Override
    public boolean isAllowed(long sw, int portId, boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.isAllowed(sw, portId);
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    @Override
    public boolean isIncomingBroadcastAllowed(long sw, int portId) {
        return isIncomingBroadcastAllowed(sw, portId, true);
    }

    @Override
    public boolean isIncomingBroadcastAllowed(long sw, int portId,
                                              boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.isIncomingBroadcastAllowedOnSwitchPort(sw, portId);
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /** Get all the ports connected to the switch */
    @Override
    public Set<Integer> getPortsWithLinks(long sw) {
        return getPortsWithLinks(sw, true);
    }

    /** Get all the ports connected to the switch */
    @Override
    public Set<Integer> getPortsWithLinks(long sw, boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getPortsWithLinks(sw);
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /** Get all the ports on the target switch (targetSw) on which a
     * broadcast packet must be sent from a host whose attachment point
     * is on switch port (src, srcPort).
     */
    @Override
    public Set<Integer> getBroadcastPorts(long targetSw,
                                        long src, int srcPort) {
        return getBroadcastPorts(targetSw, src, srcPort, true);
    }

    /** Get all the ports on the target switch (targetSw) on which a
     * broadcast packet must be sent from a host whose attachment point
     * is on switch port (src, srcPort).
     */
    @Override
    public Set<Integer> getBroadcastPorts(long targetSw,
                                        long src, int srcPort,
                                        boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getBroadcastPorts(targetSw, src, srcPort);
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    @Override
    public NodePortTuple getOutgoingSwitchPort(long src, int srcPort,
                                               long dst, int dstPort) {
        // Use this function to redirect traffic if needed.
        return getOutgoingSwitchPort(src, srcPort, dst, dstPort, true);
    }

    @Override
    public NodePortTuple getOutgoingSwitchPort(long src, int srcPort,
                                               long dst, int dstPort,
                                               boolean tunnelEnabled) {
        // Use this function to redirect traffic if needed.
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getOutgoingSwitchPort(src, srcPort,
                                                     dst, dstPort);
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    @Override
    public NodePortTuple getIncomingSwitchPort(long src, int srcPort,
                                               long dst, int dstPort) {
        return getIncomingSwitchPort(src, srcPort, dst, dstPort, true);
    }

    @Override
    public NodePortTuple getIncomingSwitchPort(long src, int srcPort,
                                               long dst, int dstPort,
                                               boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getIncomingSwitchPort(src, srcPort,
                                                     dst, dstPort);
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * Checks if the two switchports belong to the same broadcast domain.
     */
    @Override
    public boolean isInSameBroadcastDomain(long s1, int p1, long s2,
                                           int p2) {
        return isInSameBroadcastDomain(s1, p1, s2, p2, true);

    }

    @Override
    public boolean isInSameBroadcastDomain(long s1, int p1,
                                           long s2, int p2,
                                           boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.inSameBroadcastDomain(s1, p1, s2, p2);

    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * Checks if the switchport is a broadcast domain port or not.
     */
    @Override
    public boolean isBroadcastDomainPort(long sw, int port) {
        return isBroadcastDomainPort(sw, port, true);
    }

    @Override
    public boolean isBroadcastDomainPort(long sw, int port,
                                         boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.isBroadcastDomainPort(new NodePortTuple(sw, port));
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * Checks if the new attachment point port is consistent with the
     * old attachment point port.
     */
    @Override
    public boolean isConsistent(long oldSw, int oldPort,
                                long newSw, int newPort) {
        return isConsistent(oldSw, oldPort,
                                            newSw, newPort, true);
    }

    @Override
    public boolean isConsistent(long oldSw, int oldPort,
                                long newSw, int newPort,
                                boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.isConsistent(oldSw, oldPort, newSw, newPort);
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * Checks if the two switches are in the same Layer 2 domain.
     */
    @Override
    public boolean inSameL2Domain(long switch1, long switch2) {
        return inSameL2Domain(switch1, switch2, true);
    }

    @Override
    public boolean inSameL2Domain(long switch1, long switch2,
                                  boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.inSameL2Domain(switch1, switch2);
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    @Override
    public NodePortTuple getAllowedOutgoingBroadcastPort(long src,
                                                         int srcPort,
                                                         long dst,
                                                         int dstPort) {
        return getAllowedOutgoingBroadcastPort(src, srcPort,
                                               dst, dstPort, true);
    }

    @Override
    public NodePortTuple getAllowedOutgoingBroadcastPort(long src,
                                                         int srcPort,
                                                         long dst,
                                                         int dstPort,
                                                         boolean tunnelEnabled){
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getAllowedOutgoingBroadcastPort(src, srcPort,
                                                  dst, dstPort);
    }
    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    @Override
    public NodePortTuple
    getAllowedIncomingBroadcastPort(long src, int srcPort) {
        return getAllowedIncomingBroadcastPort(src,srcPort, true);
    }

    @Override
    public NodePortTuple
    getAllowedIncomingBroadcastPort(long src, int srcPort,
                                    boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getAllowedIncomingBroadcastPort(src,srcPort);
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    @Override
    public Set<Long> getSwitchesInOpenflowDomain(long switchDPID) {
        return getSwitchesInOpenflowDomain(switchDPID, true);
    }

    @Override
    public Set<Long> getSwitchesInOpenflowDomain(long switchDPID,
                                                 boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getSwitchesInOpenflowDomain(switchDPID);
    }
    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////

    @Override
    public Set<NodePortTuple> getBroadcastDomainPorts() {
        return portBroadcastDomainLinks.keySet();
    }

    @Override
    public Set<NodePortTuple> getTunnelPorts() {
        return tunnelPorts;
    }

    @Override
    public Set<NodePortTuple> getBlockedPorts() {
        Set<NodePortTuple> bp;
        Set<NodePortTuple> blockedPorts =
                new HashSet<NodePortTuple>();

        // As we might have two topologies, simply get the union of
        // both of them and send it.
        bp = getCurrentInstance(true).getBlockedPorts();
        if (bp != null)
            blockedPorts.addAll(bp);

        bp = getCurrentInstance(false).getBlockedPorts();
        if (bp != null)
            blockedPorts.addAll(bp);

        return blockedPorts;
    }
    ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////

    // ***************
    // IRoutingService
    // ***************

    @Override
    public Route getRoute(long src, long dst, long cookie) {
        return getRoute(src, dst, cookie, true);
    }

    @Override
    public Route getRoute(long src, long dst, long cookie, boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getRoute(src, dst, cookie);
    }

    @Override
    public Route getRoute(long src, int srcPort, long dst, int dstPort, long cookie) {
        return getRoute(src, srcPort, dst, dstPort, cookie, true);
    }

    @Override
    public Route getRoute(long src, int srcPort, long dst, int dstPort, long cookie,
                          boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.getRoute(null, src, srcPort, dst, dstPort, cookie);
    }

    @Override
    public boolean routeExists(long src, long dst) {
        return routeExists(src, dst, true);
    }

    @Override
    public boolean routeExists(long src, long dst, boolean tunnelEnabled) {
        TopologyInstance ti = getCurrentInstance(tunnelEnabled);
        return ti.routeExists(src, dst);
    }

    @Override
    public ArrayList<Route> getRoutes(long srcDpid, long dstDpid,
                                      boolean tunnelEnabled) {
        // Floodlight supports single path routing now

        // return single path now
        ArrayList<Route> result=new ArrayList<Route>();
        result.add(getRoute(srcDpid, dstDpid, 0, tunnelEnabled));
        return result;
    }

    // ******************
    // IOFMessageListener
    // ******************

    @Override
    public String getName() {
        return MODULE_NAME;
    }
    //Yikai
    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return "f3linkdiscovery".equals(name);
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg,
                           FloodlightContext cntx) {
        switch (msg.getType()) {
            case PACKET_IN:
                ctrIncoming.updateCounterNoFlush();
                return this.processPacketInMessage(sw,
                                                   (OFPacketIn) msg, cntx);
            default:
                break;
        }

        return Command.CONTINUE;
    }

    // ***************
    // IHAListener
    // ***************

    private class HAListenerDelegate implements IHAListener {
        @Override
        public void transitionToMaster() {
            role = Role.MASTER;
            log.debug("Re-computing topology due " +
                    "to HA change from SLAVE->MASTER");
            newInstanceTask.reschedule(TOPOLOGY_COMPUTE_INTERVAL_MS,
                                       TimeUnit.MILLISECONDS);
        }

        @Override
        public void controllerNodeIPsChanged(
                                             Map<String, String> curControllerNodeIPs,
                                             Map<String, String> addedControllerNodeIPs,
                                             Map<String, String> removedControllerNodeIPs) {
            // no-op
        }

        @Override
        public String getName() {
            return TopologyManager.this.getName();
        }
        //Yikai
        @Override
        public boolean isCallbackOrderingPrereq(HAListenerTypeMarker type,
                                                String name) {
            return "f3linkdiscovery".equals(name) ||
                    "tunnelmanager".equals(name);
        }

        @Override
        public boolean isCallbackOrderingPostreq(HAListenerTypeMarker type,
                                                 String name) {
            // TODO Auto-generated method stub
            return false;
        }
    }

    // *****************
    // IFloodlightModule
    // *****************

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(ITopologyService.class);
        l.add(IRoutingService.class);
        return l;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
            getServiceImpls() {
        Map<Class<? extends IFloodlightService>,
        IFloodlightService> m =
            new HashMap<Class<? extends IFloodlightService>,
                IFloodlightService>();
        // We are the class that implements the service
        m.put(ITopologyService.class, this);
        m.put(IRoutingService.class, this);
        return m;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>>
            getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        //Yikai
        l.add(F3LinkDiscoveryService.class);
        l.add(IThreadPoolService.class);
        l.add(IFloodlightProviderService.class);
        l.add(ICounterStoreService.class);
        l.add(IRestApiService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
    	//Yikai
        linkDiscovery = context.getServiceImpl(F3LinkDiscoveryService.class);
        threadPool = context.getServiceImpl(IThreadPoolService.class);
        floodlightProvider =
                context.getServiceImpl(IFloodlightProviderService.class);
        restApi = context.getServiceImpl(IRestApiService.class);
        debugCounters = context.getServiceImpl(IDebugCounterService.class);
        debugEvents = context.getServiceImpl(IDebugEventService.class);

        switchPorts = new HashMap<Long,Set<Integer>>();
        switchPortLinks = new HashMap<NodePortTuple, Set<Link>>();
        directLinks = new HashMap<NodePortTuple, Set<Link>>();
        portBroadcastDomainLinks = new HashMap<NodePortTuple, Set<Link>>();
        tunnelPorts = new HashSet<NodePortTuple>();
        topologyAware = new ArrayList<ITopologyListener>();
        ldUpdates = new LinkedBlockingQueue<LDUpdate>();
        haListener = new HAListenerDelegate();
        registerTopologyDebugCounters();
        registerTopologyDebugEvents();
    }

    protected void registerTopologyDebugEvents() throws FloodlightModuleException {
        if (debugEvents == null) {
            debugEvents = new NullDebugEvent();
        }
        try {
            evTopology =
                debugEvents.registerEvent(PACKAGE, "topologyevent",
                                          "Topology Computation",
                                          EventType.ALWAYS_LOG,
                                          TopologyEvent.class, 100);
        } catch (MaxEventsRegistered e) {
            throw new FloodlightModuleException("Max events registered", e);
        }
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        clearCurrentTopology();
        // Initialize role to floodlight provider role.
        this.role = floodlightProvider.getRole();

        ScheduledExecutorService ses = threadPool.getScheduledExecutor();
        newInstanceTask = new SingletonTask(ses, new UpdateTopologyWorker());

        if (role != Role.SLAVE)
            newInstanceTask.reschedule(TOPOLOGY_COMPUTE_INTERVAL_MS,
                                   TimeUnit.MILLISECONDS);

        linkDiscovery.addListener(this);
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        floodlightProvider.addHAListener(this.haListener);
        addRestletRoutable();
    }

    private void registerTopologyDebugCounters() throws FloodlightModuleException {
        if (debugCounters == null) {
            log.error("Debug Counter Service not found.");
            debugCounters = new NullDebugCounter();
        }
        try {
            ctrIncoming = debugCounters.registerCounter(PACKAGE, "incoming",
                "All incoming packets seen by this module",
                CounterType.ALWAYS_COUNT);
        } catch (CounterException e) {
            throw new FloodlightModuleException(e.getMessage());
        }
    }

    protected void addRestletRoutable() {
        restApi.addRestletRoutable(new TopologyWebRoutable());
    }

    // ****************
    // Internal methods
    // ****************
    /**
     * If the packet-in switch port is disabled for all data traffic, then
     * the packet will be dropped.  Otherwise, the packet will follow the
     * normal processing chain.
     * @param sw
     * @param pi
     * @param cntx
     * @return
     */
    protected Command dropFilter(long sw, OFPacketIn pi,
                                             FloodlightContext cntx) {
        Command result = Command.CONTINUE;
        int port = pi.getInPort();

        // If the input port is not allowed for data traffic, drop everything.
        // BDDP packets will not reach this stage.
        if (isAllowed(sw, port) == false) {
            if (log.isTraceEnabled()) {
                log.trace("Ignoring packet because of topology " +
                        "restriction on switch={}, port={}", sw, port);
                result = Command.STOP;
            }
        }
        return result;
    }

    /**
     * TODO This method must be moved to a layer below forwarding
     * so that anyone can use it.
     * @param packetData
     * @param sw
     * @param ports
     * @param cntx
     */
    @LogMessageDoc(level="ERROR",
            message="Failed to clear all flows on switch {switch}",
            explanation="An I/O error occured while trying send " +
            		"topology discovery packet",
            recommendation=LogMessageDoc.CHECK_SWITCH)
    public void doMultiActionPacketOut(byte[] packetData, IOFSwitch sw,
                                       Set<Integer> ports,
                                       FloodlightContext cntx) {

        if (ports == null) return;
        if (packetData == null || packetData.length <= 0) return;

        OFPacketOut po =
                (OFPacketOut) floodlightProvider.getOFMessageFactory().
                getMessage(OFType.PACKET_OUT);

        List<OFAction> actions = new ArrayList<OFAction>();
        for(int p: ports) {
            actions.add(new OFActionOutput(p, (short) 0));
        }

        // set actions
        po.setActions(actions);
        // set action length
        po.setActionsLength((short) (OFActionOutput.MINIMUM_LENGTH *
                ports.size()));
        // set buffer-id to BUFFER_ID_NONE
        po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        // set in-port to OFPP_ANY
        po.setInPort(OFPort.OFPP_ANY.getValue());

        // set packet data
        po.setPacketData(packetData);

        // compute and set packet length.
        short poLength = (short)(OFPacketOut.MINIMUM_LENGTH +
                po.getActionsLength() +
                packetData.length);

        po.setLength(poLength);

        try {
            //counterStore.updatePktOutFMCounterStore(sw, po);
            if (log.isTraceEnabled()) {
                log.trace("write broadcast packet on switch-id={} " +
                        "interaces={} packet-data={} packet-out={}",
                        new Object[] {sw.getId(), ports, packetData, po});
            }
            sw.write(po, cntx);

        } catch (IOException e) {
            log.error("Failure writing packet out", e);
        }
    }

    /**
     * Get the set of ports to eliminate for sending out BDDP.  The method
     * returns all the ports that are suppressed for link discovery on the
     * switch.
     * packets.
     * @param sid
     * @return
     */
    protected Set<Integer> getPortsToEliminateForBDDP(long sid) {
        Set<NodePortTuple> suppressedNptList = linkDiscovery.getSuppressLLDPsInfo();
        if (suppressedNptList == null) return null;

        Set<Integer> resultPorts = new HashSet<Integer>();
        for(NodePortTuple npt: suppressedNptList) {
            if (npt.getNodeId() == sid) {
                resultPorts.add(npt.getPortId());
            }
        }

        return resultPorts;
    }

    /**
     * The BDDP packets are forwarded out of all the ports out of an
     * openflowdomain.  Get all the switches in the same openflow
     * domain as the sw (disabling tunnels).  Then get all the
     * external switch ports and send these packets out.
     * @param sw
     * @param pi
     * @param cntx
     */
    protected void doFloodBDDP(long pinSwitch, OFPacketIn pi,
                               FloodlightContext cntx) {

        TopologyInstance ti = getCurrentInstance(false);

        Set<Long> switches = ti.getSwitchesInOpenflowDomain(pinSwitch);

        if (switches == null)
        {
            // indicates no links are connected to the switches
            switches = new HashSet<Long>();
            switches.add(pinSwitch);
        }

        for(long sid: switches) {
            IOFSwitch sw = floodlightProvider.getSwitch(sid);
            if (sw == null) continue;
            Collection<Integer> enabledPorts = sw.getEnabledPortNumbers();
            if (enabledPorts == null)
                continue;
            Set<Integer> ports = new HashSet<Integer>();
            ports.addAll(enabledPorts);

            // all the ports known to topology // without tunnels.
            // out of these, we need to choose only those that are
            // broadcast port, otherwise, we should eliminate.
            Set<Integer> portsKnownToTopo = ti.getPortsWithLinks(sid);

            if (portsKnownToTopo != null) {
                for(int p: portsKnownToTopo) {
                    NodePortTuple npt =
                            new NodePortTuple(sid, p);
                    if (ti.isBroadcastDomainPort(npt) == false) {
                        ports.remove(p);
                    }
                }
            }

            Set<Integer> portsToEliminate = getPortsToEliminateForBDDP(sid);
            if (portsToEliminate != null) {
                ports.removeAll(portsToEliminate);
            }

            // remove the incoming switch port
            if (pinSwitch == sid) {
                ports.remove(pi.getInPort());
            }

            // we have all the switch ports to which we need to broadcast.
            doMultiActionPacketOut(pi.getPacketData(), sw, ports, cntx);
        }

    }

    protected Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi,
                                             FloodlightContext cntx) {

        // get the packet-in switch.
        Ethernet eth =
                IFloodlightProviderService.bcStore.
                get(cntx,IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

        if (eth.getPayload() instanceof BSN) {
            BSN bsn = (BSN) eth.getPayload();
            if (bsn == null) return Command.STOP;
            if (bsn.getPayload() == null) return Command.STOP;

            // It could be a packet other than BSN LLDP, therefore
            // continue with the regular processing.
            if (bsn.getPayload() instanceof LLDP == false)
                return Command.CONTINUE;

            doFloodBDDP(sw.getId(), pi, cntx);
            return Command.STOP;
        } else {
            return dropFilter(sw.getId(), pi, cntx);
        }
    }

    /**
     * Updates concerning switch disconnect and port down are not processed.
     * LinkDiscoveryManager is expected to process those messages and send
     * multiple link removed messages.  However, all the updates from
     * LinkDiscoveryManager would be propagated to the listeners of topology.
     */
    @LogMessageDoc(level="ERROR",
            message="Error reading link discovery update.",
            explanation="Unable to process link discovery update",
            recommendation=LogMessageDoc.REPORT_CONTROLLER_BUG)
    public List<LDUpdate> applyUpdates() {
        List<LDUpdate> appliedUpdates = new ArrayList<LDUpdate>();
        LDUpdate update = null;
        while (ldUpdates.peek() != null) {
            try {
                update = ldUpdates.take();
            } catch (Exception e) {
                log.error("Error reading link discovery update.", e);
            }
            if (log.isTraceEnabled()) {
                log.trace("Applying update: {}", update);
            }
            System.out.println("in topology manager:  "+update);
            switch (update.getOperation()) {
            case LINK_UPDATED:
                addOrUpdateLink(update.getSrc(), update.getSrcPort(),
                        update.getDst(), update.getDstPort(),
                        update.getType());
                break;
            case LINK_REMOVED:
                removeLink(update.getSrc(), update.getSrcPort(),
                        update.getDst(), update.getDstPort());
                break;
            case SWITCH_UPDATED:
                addOrUpdateSwitch(update.getSrc());
                break;
            case SWITCH_REMOVED:
                removeSwitch(update.getSrc());
                break;
            case TUNNEL_PORT_ADDED:
                addTunnelPort(update.getSrc(), update.getSrcPort());
                break;
            case TUNNEL_PORT_REMOVED:
                removeTunnelPort(update.getSrc(), update.getSrcPort());
                break;
            case PORT_UP: case PORT_DOWN:
                break;
            }
            // Add to the list of applied updates.
            appliedUpdates.add(update);
        }
        //Yikai Lin (hilarious0401@gmail.com)
        Map<NodePortTuple, Set<Link>> switchPortLinks = this.getSwitchPortLinks();
        for(NodePortTuple tuple:switchPortLinks.keySet()) {
            System.out.println(tuple+" -> "+switchPortLinks.get(tuple));
        }
        return (Collections.unmodifiableList(appliedUpdates));
    }

    protected void addOrUpdateSwitch(long sw) {
        // nothing to do here for the time being.
        return;
    }

    public void addTunnelPort(long sw, int port) {
        NodePortTuple npt = new NodePortTuple(sw, port);
        tunnelPorts.add(npt);
        tunnelPortsUpdated = true;
    }

    public void removeTunnelPort(long sw, int port) {
        NodePortTuple npt = new NodePortTuple(sw, port);
        tunnelPorts.remove(npt);
        tunnelPortsUpdated = true;
    }

    public boolean createNewInstance() {
        return createNewInstance("internal");
    }

    /**
     * This function computes a new topology instance.
     * It ignores links connected to all broadcast domain ports
     * and tunnel ports. The method returns if a new instance of
     * topology was created or not.
     */
    protected boolean createNewInstance(String reason) {
        Set<NodePortTuple> blockedPorts = new HashSet<NodePortTuple>();

        if (!linksUpdated) return false;

        Map<NodePortTuple, Set<Link>> openflowLinks;
        openflowLinks =
                new HashMap<NodePortTuple, Set<Link>>();
        Set<NodePortTuple> nptList = switchPortLinks.keySet();

        if (nptList != null) {
            for(NodePortTuple npt: nptList) {
                Set<Link> linkSet = switchPortLinks.get(npt);
                if (linkSet == null) continue;
                openflowLinks.put(npt, new HashSet<Link>(linkSet));
            }
        }

        // Identify all broadcast domain ports.
        // Mark any port that has inconsistent set of links
        // as broadcast domain ports as well.
        Set<NodePortTuple> broadcastDomainPorts =
                identifyBroadcastDomainPorts();

        // Remove all links incident on broadcast domain ports.
        for(NodePortTuple npt: broadcastDomainPorts) {
            if (switchPortLinks.get(npt) == null) continue;
            for(Link link: switchPortLinks.get(npt)) {
                removeLinkFromStructure(openflowLinks, link);
            }
        }

        // Remove all tunnel links.
        for(NodePortTuple npt: tunnelPorts) {
            if (switchPortLinks.get(npt) == null) continue;
            for(Link link: switchPortLinks.get(npt)) {
                removeLinkFromStructure(openflowLinks, link);
            }
        }

        TopologyInstance nt = new TopologyInstance(switchPorts,
                                                   blockedPorts,
                                                   openflowLinks,
                                                   broadcastDomainPorts,
                                                   tunnelPorts);
        nt.compute();
        // We set the instances with and without tunnels to be identical.
        // If needed, we may compute them differently.
        currentInstance = nt;
        currentInstanceWithoutTunnels = nt;

        TopologyEventInfo topologyInfo =
                new TopologyEventInfo(0, nt.getClusters().size(),
                                      new HashMap<Long, List<NodePortTuple>>(),
                                      0);
        evTopology.updateEventWithFlush(new TopologyEvent(reason,
                                                          topologyInfo));
        return true;
    }

    /**
     *  We expect every switch port to have at most two links.  Both these
     *  links must be unidirectional links connecting to the same switch port.
     *  If not, we will mark this as a broadcast domain port.
     */
    protected Set<NodePortTuple> identifyBroadcastDomainPorts() {

        Set<NodePortTuple> broadcastDomainPorts =
                new HashSet<NodePortTuple>();
        broadcastDomainPorts.addAll(this.portBroadcastDomainLinks.keySet());

        Set<NodePortTuple> additionalNpt =
                new HashSet<NodePortTuple>();

        // Copy switchPortLinks
        Map<NodePortTuple, Set<Link>> spLinks =
                new HashMap<NodePortTuple, Set<Link>>();
        for(NodePortTuple npt: switchPortLinks.keySet()) {
            spLinks.put(npt, new HashSet<Link>(switchPortLinks.get(npt)));
        }

        for(NodePortTuple npt: spLinks.keySet()) {
            Set<Link> links = spLinks.get(npt);
            boolean bdPort = false;
            ArrayList<Link> linkArray = new ArrayList<Link>();
            if (links.size() > 2) {
                bdPort = true;
            } else if (links.size() == 2) {
                for(Link l: links) {
                    linkArray.add(l);
                }
                // now, there should be two links in [0] and [1].
                Link l1 = linkArray.get(0);
                Link l2 = linkArray.get(1);

                // check if these two are symmetric.
                if (l1.getSrc() != l2.getDst() ||
                        l1.getSrcPort() != l2.getDstPort() ||
                        l1.getDst() != l2.getSrc() ||
                        l1.getDstPort() != l2.getSrcPort()) {
                    bdPort = true;
                }
            }

            if (bdPort && (broadcastDomainPorts.contains(npt) == false)) {
                additionalNpt.add(npt);
            }
        }

        if (additionalNpt.size() > 0) {
            log.warn("The following switch ports have multiple " +
                    "links incident on them, so these ports will be treated " +
                    " as braodcast domain ports. {}", additionalNpt);

            broadcastDomainPorts.addAll(additionalNpt);
        }
        return broadcastDomainPorts;
    }



    public void informListeners(List<LDUpdate> linkUpdates) {

        if (role != null && role != Role.MASTER)
            return;

        for(int i=0; i<topologyAware.size(); ++i) {
            ITopologyListener listener = topologyAware.get(i);
            listener.topologyChanged(linkUpdates);
        }
    }

    public void addSwitch(long sid) {
        if (switchPorts.containsKey(sid) == false) {
            switchPorts.put(sid, new HashSet<Integer>());
        }
    }

    private void addPortToSwitch(long s, int p) {
        addSwitch(s);
        switchPorts.get(s).add(p);
    }

    public void removeSwitch(long sid) {
        // Delete all the links in the switch, switch and all
        // associated data should be deleted.
        if (switchPorts.containsKey(sid) == false) return;

        // Check if any tunnel ports need to be removed.
        for(NodePortTuple npt: tunnelPorts) {
            if (npt.getNodeId() == sid) {
                removeTunnelPort(npt.getNodeId(), npt.getPortId());
            }
        }

        Set<Link> linksToRemove = new HashSet<Link>();
        for(Integer p: switchPorts.get(sid)) {
            NodePortTuple n1 = new NodePortTuple(sid, p);
            linksToRemove.addAll(switchPortLinks.get(n1));
        }

        if (linksToRemove.isEmpty()) return;

        for(Link link: linksToRemove) {
            removeLink(link);
        }
    }

    /**
     * Add the given link to the data structure.  Returns true if a link was
     * added.
     * @param s
     * @param l
     * @return
     */
    private boolean addLinkToStructure(Map<NodePortTuple,
                                       Set<Link>> s, Link l) {
        boolean result1 = false, result2 = false;

        NodePortTuple n1 = new NodePortTuple(l.getSrc(), l.getSrcPort());
        NodePortTuple n2 = new NodePortTuple(l.getDst(), l.getDstPort());

        if (s.get(n1) == null) {
            s.put(n1, new HashSet<Link>());
        }
        if (s.get(n2) == null) {
            s.put(n2, new HashSet<Link>());
        }
        result1 = s.get(n1).add(l);
        result2 = s.get(n2).add(l);

        return (result1 || result2);
    }

    /**
     * Delete the given link from the data strucure.  Returns true if the
     * link was deleted.
     * @param s
     * @param l
     * @return
     */
    private boolean removeLinkFromStructure(Map<NodePortTuple,
                                            Set<Link>> s, Link l) {

        boolean result1 = false, result2 = false;
        NodePortTuple n1 = new NodePortTuple(l.getSrc(), l.getSrcPort());
        NodePortTuple n2 = new NodePortTuple(l.getDst(), l.getDstPort());

        if (s.get(n1) != null) {
            result1 = s.get(n1).remove(l);
            if (s.get(n1).isEmpty()) s.remove(n1);
        }
        if (s.get(n2) != null) {
            result2 = s.get(n2).remove(l);
            if (s.get(n2).isEmpty()) s.remove(n2);
        }
        return result1 || result2;
    }

    protected void addOrUpdateTunnelLink(long srcId, int srcPort, long dstId,
                                    int dstPort) {
        // If you need to handle tunnel links, this is a placeholder.
    }

    public void addOrUpdateLink(long srcId, int srcPort, long dstId,
                                int dstPort, LinkType type) {
        Link link = new Link(srcId, srcPort, dstId, dstPort);

        if (type.equals(LinkType.MULTIHOP_LINK)) {
            addPortToSwitch(srcId, srcPort);
            addPortToSwitch(dstId, dstPort);
            addLinkToStructure(switchPortLinks, link);

            addLinkToStructure(portBroadcastDomainLinks, link);
            dtLinksUpdated = removeLinkFromStructure(directLinks, link);
            linksUpdated = true;
        } else if (type.equals(LinkType.DIRECT_LINK)) {
            addPortToSwitch(srcId, srcPort);
            addPortToSwitch(dstId, dstPort);
            addLinkToStructure(switchPortLinks, link);

            addLinkToStructure(directLinks, link);
            removeLinkFromStructure(portBroadcastDomainLinks, link);
            dtLinksUpdated = true;
            linksUpdated = true;
        } else if (type.equals(LinkType.TUNNEL)) {
            addOrUpdateTunnelLink(srcId, srcPort, dstId, dstPort);
        }
    }

    public void removeLink(Link link)  {
        linksUpdated = true;
        dtLinksUpdated = removeLinkFromStructure(directLinks, link);
        removeLinkFromStructure(portBroadcastDomainLinks, link);
        removeLinkFromStructure(switchPortLinks, link);

        NodePortTuple srcNpt =
                new NodePortTuple(link.getSrc(), link.getSrcPort());
        NodePortTuple dstNpt =
                new NodePortTuple(link.getDst(), link.getDstPort());

        // Remove switch ports if there are no links through those switch ports
        if (switchPortLinks.get(srcNpt) == null) {
            if (switchPorts.get(srcNpt.getNodeId()) != null)
                switchPorts.get(srcNpt.getNodeId()).remove(srcNpt.getPortId());
        }
        if (switchPortLinks.get(dstNpt) == null) {
            if (switchPorts.get(dstNpt.getNodeId()) != null)
                switchPorts.get(dstNpt.getNodeId()).remove(dstNpt.getPortId());
        }

        // Remove the node if no ports are present
        if (switchPorts.get(srcNpt.getNodeId())!=null &&
                switchPorts.get(srcNpt.getNodeId()).isEmpty()) {
            switchPorts.remove(srcNpt.getNodeId());
        }
        if (switchPorts.get(dstNpt.getNodeId())!=null &&
                switchPorts.get(dstNpt.getNodeId()).isEmpty()) {
            switchPorts.remove(dstNpt.getNodeId());
        }
    }

    public void removeLink(long srcId, int srcPort,
                           long dstId, int dstPort) {
        Link link = new Link(srcId, srcPort, dstId, dstPort);
        removeLink(link);
    }

    public void clear() {
        switchPorts.clear();
        tunnelPorts.clear();
        switchPortLinks.clear();
        portBroadcastDomainLinks.clear();
        directLinks.clear();
    }

    /**
    * Clears the current topology. Note that this does NOT
    * send out updates.
    */
    public void clearCurrentTopology() {
        this.clear();
        linksUpdated = true;
        dtLinksUpdated = true;
        tunnelPortsUpdated = true;
        createNewInstance("startup");
        lastUpdateTime = new Date();
    }

    /**
     * Getters.  No Setters.
     */
    public Map<Long, Set<Integer>> getSwitchPorts() {
        return switchPorts;
    }

    public Map<NodePortTuple, Set<Link>> getSwitchPortLinks() {
        return switchPortLinks;
    }

    public Map<NodePortTuple, Set<Link>> getPortBroadcastDomainLinks() {
        return portBroadcastDomainLinks;
    }

    public TopologyInstance getCurrentInstance(boolean tunnelEnabled) {
        if (tunnelEnabled)
            return currentInstance;
        else return this.currentInstanceWithoutTunnels;
    }

    public TopologyInstance getCurrentInstance() {
        return this.getCurrentInstance(true);
    }

    /**
     *  Switch methods
     */
    @Override
    public Set<Integer> getPorts(long sw) {
        IOFSwitch iofSwitch = floodlightProvider.getSwitch(sw);
        if (iofSwitch == null) return Collections.emptySet();

        Collection<Integer> ofpList = iofSwitch.getEnabledPortNumbers();
        if (ofpList == null) return Collections.emptySet();

        Set<Integer> ports = new HashSet<Integer>(ofpList);
        Set<Integer> qPorts = linkDiscovery.getQuarantinedPorts(sw);
        if (qPorts != null)
            ports.removeAll(qPorts);

        return ports;
    }
}
