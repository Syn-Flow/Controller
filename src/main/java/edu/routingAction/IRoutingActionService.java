
package edu.routingAction;

import java.util.List;

import net.floodlightcontroller.core.module.IFloodlightService;
import edu.type.SwitchActionTuple;


public interface IRoutingActionService extends IFloodlightService {

	/** Check if a route exists between src and dst, including tunnel links
     *  in the path.
     */
    public boolean routeExists(long src, long dst);

    /** Check if a route exists between src and dst, with option to have
     *  or not have tunnels as part of the path.
     */
    public boolean routeExists(long src, long dst, boolean tunnelEnabled);
    
    //remaining to modified
    
    public List<SwitchActionTuple> getRoute(long src, long dst, long cookie);
    
    //switchDPID to action list
    public List<SwitchActionTuple> getRoute(long srcId, short srcPort,
            long dstId, short dstPort, long cookie);
}
