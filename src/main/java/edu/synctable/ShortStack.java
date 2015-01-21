package edu.synctable;

public class ShortStack {

	short[] data;

	int maxSize;
	int top;

	public ShortStack(int maxSize) {
		this.maxSize = maxSize;
		data = new short[maxSize];
		top = -1;
	}

	public int getSize() {
		return maxSize;
	}

	public int getElementCount() {
		return top;
	}

	public boolean push(short data) throws StackException {
		if (top + 1 == maxSize) {
			throw new StackException("Stack is full!");
		}
		this.data[++top] = data;
		return true;
	}

	public short pop() throws StackException {
		if (top == -1) {
			throw new StackException("Stack is empty!");
		}
		return this.data[top--];
	}

	public short peek() {
		return this.data[getElementCount()];
	}
}