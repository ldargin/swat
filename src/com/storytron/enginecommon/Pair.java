package com.storytron.enginecommon;

import java.io.Serializable;

/** Utility class to store pairs. */
public final class Pair <T1,T2> implements Serializable, Comparable<Pair<T1,T2>> {
	public static final long serialVersionUID = 0L;
	public T1 first;
	public T2 second;
	public Pair(T1 first,T2 second){
		this.first = first;
		this.second = second;
	}

	@SuppressWarnings("unchecked")
	public int compareTo(Pair<T1,T2> o) {
		int f = ((Comparable)first).compareTo(o.first);
		if (f==0)
			return ((Comparable)second).compareTo(o.second);
		else
			return f;
	}
}
