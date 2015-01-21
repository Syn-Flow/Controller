package edu.action;

import java.nio.ByteBuffer;

import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;

import edu.type.ActionPointer;

public class OFXActionPushActionPointer extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected ActionPointer AP;

    public OFXActionPushActionPointer() {
	super.setType(OFActionType.PUSH_AP);
	super.setLength((short) MINIMUM_LENGTH);
	this.AP = new ActionPointer();
    }

    public OFXActionPushActionPointer(short ap) {
	super.setType(OFActionType.PUSH_AP);
	super.setLength((short) MINIMUM_LENGTH);
	this.AP = new ActionPointer(ap);
    }

    public short getActionPointerValue() {
	return this.AP.GetPointerValue();
    }

    public OFXActionPushActionPointer setActionPointerValue(short ap) {
	this.AP.SetPointerValue(ap);
	return this;
    }

    @Override
    public void readFrom(ByteBuffer data) {
	super.readFrom(data);
	this.AP.SetPointerValue(data.getShort());
	//padding
	data.getShort();
    }

    @Override
    public void writeTo(ByteBuffer data) {
	super.writeTo(data);
	data.putShort(AP.GetPointerValue());
	//padding
	data.putShort((short)0);
    }

    @Override
    public int hashCode() {
	return ("OFXActionPushActionPointer" + AP.GetPointerValue()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (!super.equals(obj)) {
	    return false;
	}
	if (!(obj instanceof OFXActionPushActionPointer)) {
	    return false;
	}
	OFXActionPushActionPointer other = (OFXActionPushActionPointer) obj;
	if (this.AP.GetPointerValue() != other.AP.GetPointerValue()) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "OFXActionPushActionPointer [AP=" + AP + "]";
    }
}
