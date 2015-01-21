package edu.action;

import java.nio.ByteBuffer;

import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;

public class OFXActionPopShim extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    public OFXActionPopShim() {
	super.setType(OFActionType.POP_SHIM);
	super.setLength((short) MINIMUM_LENGTH);
    }

    @Override
    public void readFrom(ByteBuffer data) {
	super.readFrom(data);
	data.getInt(); // pad
    }

    @Override
    public void writeTo(ByteBuffer data) {
	super.writeTo(data);
	data.putInt(0); // pad
    }

    @Override
    public int hashCode() {
	return "OFXActionPopShim".hashCode();
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (!super.equals(obj)) {
	    return false;
	}
	if (!(obj instanceof OFXActionPopShim)) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "OFXActionPopShim";
    }

}
