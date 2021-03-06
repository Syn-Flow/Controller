/**
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
package org.openflow.protocol.action;

import java.nio.ByteBuffer;


/**
 * Represents an ofp_action_pop_vlan
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 11, 2010
 */
public class OFActionPopVLAN extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    public OFActionPopVLAN() {
        super();
        super.setType(OFActionType.POP_VLAN);
        super.setLength((short) MINIMUM_LENGTH);
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        data.getInt(); //pad
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putInt(0); //pad
    }

    @Override
    public String toString() {
        return "OFActionPopVLAN []";
    }
}
