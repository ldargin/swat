package com.storytron.enginecommon;

import java.io.Serializable;

/** Utility class to store triplets. */
public final class Triplet <T1,T2,T3> implements Serializable, Comparable<Triplet<T1,T2,T3>> {
	public static final long serialVersionUID = 0L;
	public T1 first;
	public T2 second;
	public T3 third;
	public Triplet(T1 first,T2 second,T3 third){
		this.first = first;
		this.second = second;
		this.third = third;
	}

	@SuppressWarnings("unchecked")
	public int compareTo(Triplet<T1,T2,T3> o) {
		int f = ((Comparable)first).compareTo(o.first);
		if (f==0) {
			f = ((Comparable)second).compareTo(o.second);
			if (f==0)
				return ((Comparable)third).compareTo(o.third);
			else
				return f;
		} else
			return f;
	}
}