package edu.type;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class ActionTable {

	protected List<Stack<ActionPointer>> table;
	public static final int reserved_size = 2;
	protected int[] tableSize;

	public ActionTable(int tableCount, int[] tableSizeList) {
		table = new ArrayList<Stack<ActionPointer>>(tableCount);
		tableSize = tableSizeList;
		int count = reserved_size;
		tableSizeList[0] -= reserved_size;// reserved space for flood and sento
											// controller
		for (int i = 0; i < tableCount; i++) {
			table.add(new Stack<ActionPointer>());
			for (int j = count; j < tableSize[i] + count; j++) {
				(table.get(i)).add(new ActionPointer((short) j));
			}
			count += tableSize[i];
		}
	}

	public int pop(int table_no) throws EmptyStackException {
		return table.get(table_no).pop().GetPointerValue();
	}

	public void push(short apValue) {
		int count = 0;
		for (int i = 0; i < tableSize.length; i++) {
			count += tableSize[i];
			if (count > apValue) {
				table.get(i).push(new ActionPointer(apValue));
				return;
			}
		}
	}

	public void print(long dpid) throws Exception {
		File file = Paths
				.get("./testOutput/ActionTable/" + dpid + "/" + new Date()
						+ "/.txt").toFile();
		FileOutputStream fos = new FileOutputStream(file);
		for (int i = 0; i < table.size(); i++) {
			Iterator<ActionPointer> iterator = table.get(i).iterator();
			fos.write(("Table" + (i + 1) + "\n").getBytes());
			while (iterator.hasNext()) {
				fos.write((iterator.next().GetPointerValue() + "\n").getBytes());
			}
		}
		fos.close();
	}

	public void print(long dpid, int i) throws Exception {
		File file = Paths.get(
				"./testOutput/ActionTable/" + dpid + "/" + i + "/" + new Date()
						+ "/.txt").toFile();
		FileOutputStream fos = new FileOutputStream(file);
		Iterator<ActionPointer> iterator = table.get(i).iterator();
		while (iterator.hasNext()) {
			fos.write((iterator.next().GetPointerValue() + "\n").getBytes());
		}
		fos.close();
	}
}
