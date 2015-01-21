package edu.synctable;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface ActionTableService extends IFloodlightService {

	public void push(long switchID, int apValue) throws Exception;

	public int pop(long switchID) throws Exception;
}
