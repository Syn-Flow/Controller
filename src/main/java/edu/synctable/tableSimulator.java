package edu.synctable;

import java.util.Arrays;

public class tableSimulator {
    protected int pt;
    protected int[] ptArray;
    protected long[] dataArray;

    public tableSimulator() {
	pt = 0xFFFFFFFF;
	ptArray = new int[32];
	dataArray = new long[1024];

	Arrays.fill(ptArray, 0, 32, 0xFFFFFFFF);
	Arrays.fill(dataArray, 0, 1024, 0xFFFFFFFFFFFFFFFFL);
    }

    public void init() {
	pt = 0xFFFFFFFF;
	Arrays.fill(ptArray, 0, 32, 0xFFFFFFFF);
	Arrays.fill(dataArray, 0, 1024, 0xFFFFFFFFFFFFFFFFL);
    }

    public tableSimulator(int tableSize) {
	// TODO
	pt = 0xFFFFFFFF;
	ptArray = new int[tableSize / 2048];
	dataArray = new long[tableSize / 64];

	Arrays.fill(ptArray, 0, tableSize / 2048, 0xFFFFFFFF);
	Arrays.fill(dataArray, 0, tableSize / 64, 0xFFFFFFFFFFFFFFFFL);
    }

    public static void main(String[] args) {
	long a = 0xFFFFFFFFFFFFFFFFL;
	System.out.println(Long.numberOfLeadingZeros(a
		& (long) ((long) 1 << 32)));
	System.out.println(Long.toBinaryString(a & (long) ((long) 1 << 32)));
	System.out.println(Long.numberOfTrailingZeros(a & (long) (1 << 32)));
    }
}
