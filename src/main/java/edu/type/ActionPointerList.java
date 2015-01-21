package edu.type;

import java.util.LinkedList;

public class ActionPointerList {
    protected LinkedList<ActionPointer> apList;
    protected short length;

    public LinkedList<ActionPointer> getApList() {
	return apList;
    }

    public void setApList(LinkedList<ActionPointer> apList) {
	this.apList = apList;
    }

    public ActionPointerList() {
	this.apList = new LinkedList<ActionPointer>();
    }

    public ActionPointerList(LinkedList<ActionPointer> apList) {
	this.apList = apList;
	this.length = (short) apList.size();
    }

    public short length() {
	return length;
    }

    public short add(ActionPointer ap) {
	this.apList.add(ap);
	this.length++;
	return ap.GetPointerValue();
    }

    public short add(short apValue) {
	this.apList.add(new ActionPointer(apValue));
	this.length++;
	return apValue;
    }

    public short addFirst(short apValue) {
	this.apList.addFirst(new ActionPointer(apValue));
	this.length++;
	return apValue;
    }

    public ActionPointer get(int i) {
	return this.apList.get(i);
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	for (ActionPointer ap : apList) {
	    sb.append(ap.GetPointerValue() + "\n");
	}
	return sb.toString();
    }
}
