package com.storytron.swat.util;

import java.util.Iterator;

/** <p>Implements a wrapper for filtering elements in an {@link Iterable} object.</p>
 * Sample usage that prints only visible traits:
 * <pre>
 * Iterable<Trair> it = new IterableFilter<Trait>(getActorTraits()){
 *		@Override
 *		protected boolean evaluatePredicate(Trait t){ return t.isVisible(); }
 *	};
 *	for(Trait t:it)
 *	  System.out.println(t.getLabel());
 * </pre>
 * */
public abstract class IterableFilter <T> implements Iterable<T> {
	
	/** 
	 * Override to specify the predicate. Elements that does not
	 * satisfy the predicate are discarded when traversing the collection.
	 * */
	protected abstract boolean evaluatePredicate(T t);  

	private Iterable<T> col;
	
	/** Constructs an iterable filter from another iterable object. */
	public IterableFilter(Iterable<T> col){
		this.col = col; 
	}

	private final class FilterIterator implements Iterator<T> {

		private Iterator<T> it;
		private T next;
		
		public FilterIterator(Iterator<T> it){
			this.it = it;
			next = getNext();
		}
		
		public boolean hasNext() {
			return next!=null;
		}
		
		public T next() {
			T oldNext = next;
			next = getNext();
			return oldNext;
		}

		public void remove() { it.remove();	}
		
		private T getNext(){
			T next;
			while(it.hasNext()){
				next = it.next();
				if (evaluatePredicate(next))
					return next;
			}
			return null;
		}
	}

	public Iterator<T> iterator() {
		// TODO Auto-generated method stub
		return new FilterIterator(col.iterator());
	}
}
