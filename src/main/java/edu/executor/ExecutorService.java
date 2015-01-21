package edu.executor;

import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.action.OFAction;

import edu.type.SwitchActionTuple;

public interface ExecutorService extends IFloodlightService {
    public boolean Execute(ArrayList<SwitchActionTuple> switchActionTupleList,
	    OFMatch match, OFPacketIn pi, int appID) throws Exception;

    public void SendOutPktIn(long switchID, OFPacketIn pktIn,
	    List<OFAction> actions);

    public void SendOutOFXFlowMod(IOFSwitch sw, List<OFAction> actions);

    public void SendOutFlowMod(IOFSwitch sw, OFMatch match,
	    List<OFAction> actions);
}
