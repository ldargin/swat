package com.storytron.enginecommon;

/**
 * Class for signaling errors when exceeding model limits.
 * */
public final class LimitException extends Exception {
	private static final long serialVersionUID = 1L;
	public static enum Type {
		Actors,
		Stages,
		Props,
		Verbs,
		Roles,
		Options,
		Nodes,
		Name,
		Description,
		Traits,
		InactivityTimeout,
		BNumber,
		CustomOperator
	} 
	public LimitException.Type t;
	public String s;
	public int limit;
	public LimitException(LimitException.Type t,String s,int limit){
		super(t.name()+": "+s);
		this.t = t;
		this.s = s;
		this.limit = limit;
	};
}