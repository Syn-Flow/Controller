package edu.action;

import java.nio.ByteBuffer;

import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;

import edu.type.ActionPointer;

public class OFXActionJump extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected ActionPointer AP;

    public OFXActionJump() {
	super.setType(OFActionType.JUMP);
	super.setLength((short) MINIMUM_LENGTH);
	this.AP = new ActionPointer();
    }

    public OFXActionJump(short ap) {
	super.setType(OFActionType.JUMP);
	super.setLength((short) MINIMUM_LENGTH);
	this.AP = new ActionPointer(ap);
    }

    public OFXActionJump(ActionPointer ap) {
	super.setType(OFActionType.JUMP);
	super.setLength((short) MINIMUM_LENGTH);
	this.AP = ap;
    }

    public short getActionPointerValue() {
	return this.AP.GetPointerValue();
    }

    public OFXActionJump setActionPointerValue(short ap) {
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
	data.putShort(this.AP.GetPointerValue());
	//padding
	data.putShort((short)0);
    }

    @Override
    public int hashCode() {
	return ("OFXActionJump" + this.AP.GetPointerValue()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (!super.equals(obj)) {
	    return false;
	}
	if (!(obj instanceof OFXActionJump)) {
	    return false;
	}
	OFXActionJump other = (OFXActionJump) obj;
	if (!this.AP.equals(other.AP)) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "OFXActionJump [AP=" + AP + "]";
    }
}
