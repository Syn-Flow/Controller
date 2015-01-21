package edu.messagetype;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.openflow.util.U16;

public class OFXTableFeature extends OFMessage {
	public static int MINIMUM_LENGTH = 16;
	protected byte tableCount;
	protected int[] entryCount;

	public byte getTableCount() {
		return tableCount;
	}

	public void setTableCount(byte tableCount) {
		this.tableCount = tableCount;
	}

	public int[] getEntryCount() {
		return entryCount;
	}

	public void setEntryCount(int[] entryCount) {
		this.entryCount = entryCount;
	}

	public OFXTableFeature() {
		super();
		this.type = OFType.OFX_TABLE_FEATURE;
		this.length = U16.t(MINIMUM_LENGTH);
		this.tableCount = 0;
		this.entryCount = null;
	}

	public OFXTableFeature(byte tableCount, int[] entryCount) {
		super();
		this.type = OFType.OFX_TABLE_FEATURE;
		this.length = U16.t(8 + 1 + tableCount + tableCount % 2 == 0 ? 7 : 3);
		this.tableCount = tableCount;
		this.entryCount = entryCount;
	}

	@Override
	public void readFrom(ByteBuffer data) {
		super.readFrom(data);
		this.tableCount = data.get();
		for (byte i = 0; i < tableCount; i++) {
			entryCount[i] = data.getInt();
		}
		data.position(data.position() + tableCount % 2 == 0 ? 7 : 3);
	}

	@Override
	public void writeTo(ByteBuffer data) {
		super.writeTo(data);
		data.put(tableCount);
		for (byte i = 0; i < tableCount; i++) {
			data.putInt(entryCount[i]);
		}
		// padding
		data.put((byte) 0);
		data.putShort((short) 0);
		if (tableCount % 2 == 0)
			data.putInt(0);
	}

	@Override
	public int hashCode() {
		final int prime = "OFXTableFeature".hashCode();
		int result = prime * tableCount;
		result = prime * result + entryCount.hashCode();
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
		if (!(obj instanceof OFXTableFeature)) {
			return false;
		}
		OFXTableFeature other = (OFXTableFeature) obj;
		if (tableCount != other.getTableCount())
			return false;

		return Arrays.equals(entryCount, other.getEntryCount());
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("OFXTableFeature [tableCount=" + tableCount + "]\n");
		for (byte i = 0; i < tableCount; i++) {
			sb.append(entryCount[i] + "\n");
		}
		return sb.toString();
	}
}
