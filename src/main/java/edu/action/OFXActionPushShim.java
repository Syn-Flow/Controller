package edu.action;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;

import edu.type.ActionPointer;
import edu.type.ActionPointerList;
import edu.type.ShimHeader;

public class OFXActionPushShim extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected ShimHeader header;
    protected ActionPointerList apList;

    private int padding;

    public OFXActionPushShim() {
	super.setType(OFActionType.PUSH_SHIM);
	super.setLength((short) MINIMUM_LENGTH);
	this.header = new ShimHeader();
	this.apList = new ActionPointerList();
    }

    public OFXActionPushShim(byte ttl) {
	super.setType(OFActionType.PUSH_SHIM);
	super.setLength((short) MINIMUM_LENGTH);
	this.header = new ShimHeader(ttl);
	this.apList = new ActionPointerList();
    }

    public OFXActionPushShim(byte ttl, ActionPointerList apList) {
	// System.out.println("ttl: " + ttl + ", ap#: " + apList.length());
	super.setType(OFActionType.PUSH_SHIM);
	this.header = new ShimHeader(ttl);
	this.header.setNumPT((byte) apList.length());
	this.apList = apList;
	padding = apList.length()%4==0?0:(4-apList.length()%4);
	super.setLength((short) (8 + padding*2 + 2 * apList.length()));
    }

    public byte getTTL() {
	return this.header.getTTL();
    }

    public byte getPNum() {
	return this.header.getNumPT();
    }

    public OFXActionPushShim setTTL(byte ttl) {
	this.header.setTTL(ttl);
	return this;
    }

    public LinkedList<ActionPointer> getApList() {
	return apList.getApList();
    }

    public void setApList(LinkedList<ActionPointer> apList) {
	this.apList.setApList(apList);
    }

    @Override
    public void readFrom(ByteBuffer data) {
	super.readFrom(data);
	this.header.setTTL(data.get());
	this.header.setNumPT(data.get());
	data.getShort();
	for (int i = 0; i < this.header.getNumPT(); i++) {
	    this.apList.add(new ActionPointer(data.getShort()));
	}
	data.position(data.position() + padding*2);
    }

    @Override
    public void writeTo(ByteBuffer data) {
	super.writeTo(data);
	data.put(this.header.getTTL());
	data.put(this.header.getNumPT());
	data.putShort((short) 0);
	for (int i = 0; i < this.header.getNumPT(); i++) {
	    data.putShort(apList.get(i).GetPointerValue());
	}
	for(int i =0;i<padding;i++)
	    data.putShort((short)0);
    }

    @Override
    public int hashCode() {
	final int prime = "OFXActionPushShim".hashCode();
	int result = prime * this.header.getTTL();
	result = prime * result + this.header.getNumPT();
	result = prime * result + this.apList.hashCode();
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (!super.equals(obj)) {
	    return false;
	}
	if (!(obj instanceof OFXActionPushShim)) {
	    return false;
	}
	OFXActionPushShim other = (OFXActionPushShim) obj;
	if (this.header.getTTL() != other.header.getNumPT()) {
	    return false;
	}
	if (this.header.getNumPT() != other.header.getNumPT()) {
	    return false;
	}
	for (int i = 0; i < this.header.getNumPT(); i++) {
	    if (!(this.getApList().get(i).equals(other.getApList().get(i))))
		return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "OFXActionPushShim [header=" + header + ", apList=" + apList
		+ "]";
    }

}
