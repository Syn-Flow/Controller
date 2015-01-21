package edu.type;

public class ActionPointer {
	@Override
	public String toString() {
		return pointerValue + "";
	}

	protected short pointerValue;

	public ActionPointer() {
		this((short) -1);
	}

	public ActionPointer(short p) {
		this.pointerValue = p;
	}

	public ActionPointer SetPointerValue(short v) {
		pointerValue = v;
		return this;
	}

	public short GetPointerValue() {
		return pointerValue;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ActionPointer))
			return false;
		else if (this == other)
			return true;
		else if (this.GetPointerValue() == ((ActionPointer) other)
				.GetPointerValue())
			return true;
		return false;
	}
}