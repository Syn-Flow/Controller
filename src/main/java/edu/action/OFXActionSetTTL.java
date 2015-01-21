package edu.action;

import java.nio.ByteBuffer;

import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;
import org.openflow.util.U8;

public class OFXActionSetTTL extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected byte TTL;

    public OFXActionSetTTL() {
	super.setType(OFActionType.SET_F3_TTL);
	super.setLength((short) MINIMUM_LENGTH);
    }

    public OFXActionSetTTL(byte ttl) {
	super.setType(OFActionType.SET_F3_TTL);
	super.setLength((short) MINIMUM_LENGTH);
	this.TTL = ttl;
    }

    public byte getTTL() {
	return TTL;
    }

    public OFXActionSetTTL setTTL(byte ttl) {
	this.TTL = ttl;
	return this;
    }

    @Override
    public void readFrom(ByteBuffer data) {
	super.readFrom(data);
	this.TTL = data.get();
	data.getShort(); // pad
	data.get(); // pad
    }

    @Override
    public void writeTo(ByteBuffer data) {
	super.writeTo(data);
	data.put(this.TTL);
	data.putShort((short) 0); // pad
	data.put((byte) 0); // pad
    }

    @Override
    public int hashCode() {
	return ("OFXActionSetTTL" + TTL).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (!super.equals(obj)) {
	    return false;
	}
	if (!(obj instanceof OFXActionSetTTL)) {
	    return false;
	}
	OFXActionSetTTL other = (OFXActionSetTTL) obj;
	if (TTL != other.TTL) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "OFXActionSetTTL [TTL=" + U8.f(TTL) + "]";
    }
}
