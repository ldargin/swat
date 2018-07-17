package com.storytron.enginecommon;

/** 
 * Thrown by login methods when the server and the client interfaces
 * are incompatible. 
 * */
public final class IncompatibleVersionException extends Exception {
	private static final long serialVersionUID = 0L;
	public int serverVersion;
	public IncompatibleVersionException(int serverVersion){
		this.serverVersion=serverVersion;
	}
}
