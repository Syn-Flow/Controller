package edu.type;

public class ShimHeader {
	@Override
	public String toString() {
		return "F3ShimHeader [TTL=" + TTL + ", numPT=" + numPT + "]";
	}

	protected byte TTL;
	protected byte numPT = 0;

	public ShimHeader() {
		this((byte) 0);
	}

	public ShimHeader(byte T) {
		this.TTL = T;
	}

	public ShimHeader(byte T, byte numPT) {
		this.TTL = T;
		this.numPT = numPT;
	}

	public ShimHeader setTTL(byte ttl) {
		this.TTL = ttl;
		return this;
	}

	public ShimHeader setNumPT(byte numPT) {
		this.numPT = numPT;
		return this;
	}

	public ShimHeader increasePLnum(byte num) {
		this.numPT += num;
		return this;
	}

	public byte getNumPT() {
		return numPT;
	}

	public byte getTTL() {
		return TTL;
	}

}
