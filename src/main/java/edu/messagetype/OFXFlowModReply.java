package edu.messagetype;

import java.nio.ByteBuffer;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.util.U16;

import edu.type.ActionPointer;

public class OFXFlowModReply extends OFMessage {
	/*
	 * format: 8-byte type 4-byte ActionPointer 2-byte status 2-byte padding
	 */
	public static int MINIMUM_LENGTH = 16;

//	public enum Status {
//		// TODO to be discussed upon
//		Success((byte) 0), Error((byte) 1);
//
//		protected byte value;
//
//		private Status() {
//			this.value = (byte) this.ordinal();
//		}
//
//		private Status(byte value) {
//			this.value = value;
//		}
//
//		public byte getValue() {
//			return value;
//		}
//	}

	protected ActionPointer AP;
	protected byte status;

	public OFXFlowModReply() {
		super();
		this.type = OFType.OFX_FLOW_MOD_REPLY;
		this.length = U16.t(MINIMUM_LENGTH);
		this.AP = new ActionPointer();
	}

	/**
	 * Get ActionPointerValue
	 * 
	 * @return
	 */
	public short getActionPointerValue() {
		return this.AP.GetPointerValue();
	}

	/**
	 * Set ActionPointerValue
	 * 
	 * @param ActionPointerValue
	 */
	public OFXFlowModReply setActionPointerValue(short ap) {
		this.AP.SetPointerValue(ap);
		return this;
	}

	/**
	 * @return the status
	 */
	public byte getStatus() {
		return status;
	}

	/**
	 * Set Status
	 * 
	 * @param status
	 */
	public OFXFlowModReply setStatus(byte status) {
		this.status = status;
		return this;
	}

//	public OFXFlowModReply setStatus(Status status) {
//		this.status = status.getValue();
//		return this;
//	}

	@Override
	public void readFrom(ByteBuffer data) {
		super.readFrom(data);
		this.AP.SetPointerValue(data.getShort());
		this.status = data.get();
		// padding
		data.getInt();
		data.get();
	}

	@Override
	public void writeTo(ByteBuffer data) {
		super.writeTo(data);
		data.putShort(this.AP.GetPointerValue());
		data.put(this.status);
		// padding
		data.putInt( 0);
		data.put((byte)0);
	}

	@Override
	public int hashCode() {
		final int prime = 271;
		int result = super.hashCode();
		result = prime * result + this.AP.GetPointerValue();
		result = prime * result + this.status;
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
		if (!(obj instanceof OFXFlowModReply)) {
			return false;
		}
		OFXFlowModReply other = (OFXFlowModReply) obj;
		if (!this.AP.equals(other.AP))
			return false;

		if (status != other.status) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "OFXFlowModReply [ActionPointerValue="
				+ this.AP.GetPointerValue() + ", Status=" + status + "]";
	}

}
