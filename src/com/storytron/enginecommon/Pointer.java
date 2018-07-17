package com.storytron.enginecommon;

/** A helper class for sharing data in nested classes. */
public final class Pointer<T> {
	private T t;
	public Pointer(){};
	public Pointer(T t){ this.t=t; }
	public void set(T t){ this.t = t; }
	public T get(){ return this.t; }
}