package edu.util;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import edu.action.OFXActionPopShim;

public class HeaderWrapper {

    // TODO the ttl might need to be changed
    // static final ShimHeader header = new ShimHeader((byte) 10, (byte) 2);
    // static final ActionPointerList apList = new ActionPointerList(
    // (LinkedList<ActionPointer>) Arrays.asList(new ActionPointer(0)));

    public static boolean wrap(long swID, ByteBuffer data, int port) {
	data.put((byte) 10);
	data.put((byte) 2);
	// for (int i = 0; i < header.getNumPT(); i++) {
	// data.putInt(apList.get(i).GetPointerValue());
	// }
	LinkedList<OFAction> tempActions = new LinkedList<OFAction>();
	short ap1, ap2;
	try {
	    tempActions.add(new OFActionOutput(port, (short) 0xffff));
	    ap1 = ActionMapper.map(swID, new LinkedList<OFAction>(tempActions),
		    -1);
	    tempActions.clear();
	    tempActions.add(new OFXActionPopShim());
	    tempActions.add(new OFActionOutput(OFPort.OFPP_CONTROLLER,
		    (short) 0xffff));
	    ap2 = ActionMapper.map(swID, new LinkedList<OFAction>(tempActions),
		    -1);
	    if (ap1 >= 0 && ap2 >= 0) {
		data.putShort(ap1);
		data.putShort(ap2);
		return true;
	    }
	    throw new Exception("ap1: " + ap1 + ", ap2: " + ap2);
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}

    }

}
