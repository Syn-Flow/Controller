package edu.messagetype;

import java.nio.ByteBuffer;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.util.U16;

public class OFXPortModReply extends OFMessage {
	/*
	 * format: 8-byte type 8-byte DPID 4-byte PortNum 4-byte Config 1-byte error
	 * 7 byte padding
	 */
	public static int MINIMUM_LENGTH = 32;

	protected long dpid;
	protected int port;
	protected int config;
	protected byte error;

	public OFXPortModReply() {
		super();
		this.type = OFType.OFX_PORT_MOD_REPLY;
		this.length = U16.t(MINIMUM_LENGTH);
		this.dpid = -1;
		this.port = -1;
		this.config = 0;
		this.error = 0;
	}

	public OFXPortModReply(long dpid, int port, byte config, byte error) {
		super();
		this.type = OFType.OFX_PORT_MOD_REPLY;
		this.length = U16.t(MINIMUM_LENGTH);
		this.dpid = dpid;
		this.port = port;
		this.config = config;
		this.error = error;
	}

	public byte getError() {
		return error;
	}

	public void setError(byte error) {
		this.error = error;
	}

	public long getDpid() {
		return dpid;
	}

	public void setDpid(long dpid) {
		this.dpid = dpid;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getConfig() {
		return config;
	}

	public void setConfig(int config) {
		this.config = config;
	}

	@Override
	public void readFrom(ByteBuffer data) {
		super.readFrom(data);
		this.dpid = data.getLong();
		this.port = data.getInt();
		this.config = data.getInt();
		this.error = data.get();
		// padding for 7 byte
		data.position(data.position() + 7);
	}

	@Override
	public void writeTo(ByteBuffer data) {
		super.writeTo(data);
		data.putLong(dpid);
		data.putInt(port);
		data.putInt(config);
		data.put(error);
		// padding
		data.put((byte) 0);
		data.putShort((short) 0);
		data.putInt(0);
	}

	@Override
	public int hashCode() {
		final int prime = "OFXPortModReply".hashCode();
		int result = (int) (dpid ^ (dpid >>> 32));
		result = prime * result + this.port;
		result = prime * result + this.config;
		result = prime * result + this.error;
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
		if (!(obj instanceof OFXPortModReply)) {
			return false;
		}
		OFXPortModReply other = (OFXPortModReply) obj;
		if (dpid != other.dpid)
			return false;

		if (port != other.port) {
			return false;
		}
		if (config != other.config)
			return false;
		if (error != other.error) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "OFXPortModReply [dpid=" + dpid + ", port=" + port + ", config="
				+ Integer.toBinaryString(config) + ", error=" + error + "]";
	}

}
