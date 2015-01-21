package edu.synctable;


public class apAllocator {

    private static tableSimulator[] tableArray;

    public static void init() {
	tableArray = new tableSimulator[1000];
    }

    public static void init(int swGroupSize) {
	tableArray = new tableSimulator[swGroupSize];
    }

    public static void initSw(long swID) {
	if (tableArray[(int) swID] != null)
	    tableArray[(int) swID].init();
    }

    public static short popAP(int swID) {
	if (tableArray[swID] == null)
	    tableArray[swID] = new tableSimulator();
	tableSimulator tS = tableArray[swID];
	try {
	    if (tS.pt == 0)
		return -1;
	    int a = Integer.numberOfTrailingZeros(tS.pt);
	    int b = Integer.numberOfTrailingZeros(tS.ptArray[a]);
	    int c = a * 32 + b;
	    int d = Long.numberOfTrailingZeros(tS.dataArray[c]);
	    tableArray[swID].dataArray[c] ^= (1L << d);
	    if (tableArray[swID].dataArray[c] == 0)
		tableArray[swID].ptArray[a] ^= (1 << b);
	    if (tableArray[swID].ptArray[a] == 0)
		tableArray[swID].pt ^= (1 << a);
	    return (short) (c * 64 + d);
	} catch (ArrayIndexOutOfBoundsException e) {
	    e.printStackTrace();
	    return -1;
	}
    }

    public static boolean popAP(int swID, short ap) throws Exception {
	if (tableArray[swID] == null)
	    tableArray[swID] = new tableSimulator();
	tableSimulator tS = tableArray[swID];
	try {
	    int a = ap / 64, b = ap % 64, c = a / 32, d = a % 32;
	    if (((tS.pt & (1 << c)) == 0) || ((tS.ptArray[c] & (1 << d)) == 0)
		    || (tS.dataArray[a] & (1L << b)) == 0)
		return false;
	    else {
		tableArray[swID].dataArray[a] ^= (1L << b);
		if (tableArray[swID].dataArray[a] == 0)
		    tableArray[swID].ptArray[c] ^= (1 << d);
		if (tableArray[swID].ptArray[c] == 0)
		    tableArray[swID].pt ^= (1 << c);
		return true;
	    }
	} catch (ArrayIndexOutOfBoundsException e) {
	    e.printStackTrace();
	    return false;
	}
    }

    public static void pushAP(int swID, int ap) throws Exception {
	if (tableArray[swID] == null)
	    throw new Exception("pushing AP, but swID is null, error!");
	try {
	    int a = ap / 64, b = ap % 64, c = a / 32, d = a % 32;
	    tableArray[swID].dataArray[a] |= (1L << b);
	    tableArray[swID].ptArray[c] |= (1 << d);
	    tableArray[swID].pt |= (1 << c);
	} catch (ArrayIndexOutOfBoundsException e) {
	    e.printStackTrace();
	}
    }

    public static void main(String[] args) throws Exception {
	apAllocator.init();
	for (int i = 0; i < 65536; i++) {
	    if (!apAllocator.popAP(0, (short) i))
		System.out.println(i);
	}
	for (int i = 0; i < 10000; i++) {
	    int ap = (int) (Math.random() * 65536);
	    apAllocator.pushAP(0, ap);
	    if (ap != apAllocator.popAP(0))
		System.out.println(ap);
	}

    }

}
