package com.storytron.enginecommon;

import com.storytron.uber.Script;

/** Thrown when a storyworld has errors. */
public final class BadStoryworldException extends Exception {
	private static final long serialVersionUID = 0L;
	
	public BadStoryworldException(Iterable<Triplet<Script.Type,String[],String>> errors){
		this.errors = errors;
	}
	public Iterable<Triplet<Script.Type,String[],String>> errors; 
}
