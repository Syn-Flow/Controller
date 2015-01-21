package edu.type;

import java.util.LinkedList;

import org.openflow.protocol.action.OFAction;

public class SwitchActionTuple {
	long switchID;
	LinkedList<OFAction> actions;

	public long getSwitchID() {
		return switchID;
	}

	public void setSwitchID(long switchID) {
		this.switchID = switchID;
	}

	public LinkedList<OFAction> getActions() {
		return actions;
	}

	public void setActions(LinkedList<OFAction> actions) {
		this.actions = actions;
	}

	public SwitchActionTuple(long switchID, LinkedList<OFAction> actions) {
		this.switchID = switchID;
		this.actions = actions;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("SwitchActionTuple [switchID="
				+ switchID + ", actions=\n");
		for (OFAction action : actions) {
			sb.append(action.toString());
		}
		sb.append("]");
		return sb.toString();
	}
}
