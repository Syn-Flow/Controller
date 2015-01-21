package edu.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import org.openflow.protocol.action.OFAction;


public class ActionMapper {
    static class ActionRecorder {
	class ap_treeset_tuple {
	    short ap;
	    TreeSet<Integer> ts;

	    public ap_treeset_tuple(short ap, int appID) {
		this.ap = ap;
		ts = new TreeSet<Integer>();
		ts.add(appID);
	    }

	    public short getAp(int appID) {
		ts.add(appID);
		return ap;
	    }

	    public void AddappID(int appID) {
		ts.add(appID);
	    }

	}

	HashMap<Short, LinkedList<OFAction>> ap2entryMap;
	HashMap<LinkedList<OFAction>, ap_treeset_tuple> entry2tupleMap;

	public ActionRecorder(short ap, LinkedList<OFAction> entry, int appID) {
	    ap2entryMap = new HashMap<Short, LinkedList<OFAction>>();
	    ap2entryMap.put(ap, entry);
	    entry2tupleMap = new HashMap<LinkedList<OFAction>, ap_treeset_tuple>();
	    entry2tupleMap.put(entry, new ap_treeset_tuple(ap, appID));
	}

	public HashMap<Short, LinkedList<OFAction>> getAp2entryMap() {
	    return ap2entryMap;
	}

	public HashMap<LinkedList<OFAction>, ap_treeset_tuple> getEntry2tupleMap() {
	    return entry2tupleMap;
	}

	public void add(short ap, LinkedList<OFAction> action, int appID) {
	    ap2entryMap.put(ap, action);
	    if (!entry2tupleMap.containsKey(action)) {
		entry2tupleMap.put(action, new ap_treeset_tuple(ap, appID));
	    } else {
		entry2tupleMap.get(action).AddappID(appID);
	    }
	}

	public void timeout(int ap) {
	    LinkedList<OFAction> action = ap2entryMap.get(ap);
	    ap2entryMap.remove(ap);
	    entry2tupleMap.remove(action);
	    // TODO
	}

	public void remove() {
	    // TODO
	}
    }

    static HashMap<Long, ActionRecorder> arList = new HashMap<Long, ActionRecorder>();

    /**
     * 
     * @param swID
     * @param action
     * @param appID
     * @return return the ap if action found, -1 otherwise
     * @throws Exception
     */
    public static short map(long swID, LinkedList<OFAction> action, int appID)
	    throws Exception {
	try {
	    return arList.get(swID).getEntry2tupleMap().get(action)
		    .getAp(appID);
	} catch (Exception e) {
	    // e.printStackTrace();
	    System.out.println("---------------action not found! " + action
		    + ", swID: " + swID + "-------------");
	    return -1;
	}
    }

    /**
     * 
     * @param swID
     * @param action
     * @param ap
     * @param appID
     */
    public static void add(long swID, LinkedList<OFAction> action, short ap,
	    int appID) {
	if (arList.get(swID) == null) {
	    arList.put(swID, new ActionRecorder(ap, action, appID));
	}
	if (arList.get(swID).getAp2entryMap().get(ap) == null) {
	    arList.get(swID).add(ap, action, appID);
	}
    }

    public static void remove(long swID) {
	arList.remove(swID);
    }

    public static void main(String[] args) {
    }
}
