package com.storytron.swat.util;

import java.io.IOException;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/** 
 * Stores an array of object on disk. 
 * It holds a cache of objects to improve performance to frequently
 * accessed positions. This is useful to wrap arrays whose get 
 * operation is expensive. 
 * */
public class CachedArray <T> extends AbstractList<T> {
	
	/** Construct a cached array with the specified cache size. 
	 * @throws IllegalArgumentException if maxCacheSize<=0 
	 * */
	public CachedArray(int maxCacheSize,List<T> array) {
		if (maxCacheSize<=0)
			throw new IllegalArgumentException("Maximum cache size must be greater than 0.");
		this.maxCacheSize = maxCacheSize;
		this.array = array;
	}
	private int maxCacheSize;
	private List<T> array;
	
	@Override
	public T get(int index){
		T t = cacheGet(index);
		if (t==null) {
			t = array.get(index);
			if (t!=null){
				cache.addFirst(new Entry(index,t));
				if (cache.size()>maxCacheSize) {
					if (cache.getLast().dirty)
						array.set(cache.getLast().index, cache.getLast().t);
					cache.removeLast();
				}
			}
		}
		return t;
	}
	
	/** Adds an element at the end of the array. */
	@Override
	public boolean add(T e) {
		return array.add(e);
	}
	
	@Override
	public T set(int index,T e){
		if (cacheGet(index)!=null) {
			// set as dirty the given element
			cache.getFirst().dirty=true;
			T old = cache.getFirst().t;
			cache.getFirst().t=e;
			return old;
		} else
			return array.set(index,e);
	}
	
	@Override
	public void clear(){
		array.clear();
		cache.clear();
	}

	private LinkedList<Entry> cache = new LinkedList<Entry>();
	private class Entry {
		public int index;
		public T t;
		public boolean dirty;
		public Entry(int index,T t){
			this(index,t,false);
		}
		public Entry(int index,T t,boolean dirty){
			this.index=index;
			this.t=t;
			this.dirty=dirty;
		}
	}
	/** 
	 * Gets the element if its in the cache or returns null.
	 * If the element is found it is moved to the beginning of the list.
	 * */
	private T cacheGet(int index){
		Iterator<Entry> it = cache.iterator();
		while(it.hasNext()){
			Entry e = it.next(); 
			if (e.index==index){
				it.remove();
				cache.addFirst(e);
				return e.t;
			}
		}
		return null;
	}
	
	@Override
	public int size() {
		return array.size();
	}
	
	/** A test program with a sample usage for this class. */
	public static void main(String[] args){
		try {
			int maxCacheSize = 2;
			DiskArray<int[]> a = new DiskArray<int[]>();
			CachedArray<int[]> da = new CachedArray<int[]>(maxCacheSize,a);
			int[] ia0 = new int[]{ 0,0,0,0 };
			da.add(ia0);
			int[] ia1 = new int[]{ 0,1,0,0 };
			da.add(ia1);
			int[] ia2 = new int[]{ 0,0,0,1 };
			da.add(ia2);

			System.out.println(Arrays.equals(da.get(0),ia0));
			System.out.println(Arrays.equals(da.get(1),ia1));
			System.out.println(Arrays.equals(da.get(2),ia2));
			System.out.println(da.maxCacheSize==maxCacheSize);

			int[] ia3 = new int[]{ 0,1,1,0 };
			da.set(1,ia3);
			
			da.get(0);
			da.get(2);
			
			System.out.println(Arrays.equals(da.get(1),ia3));

			a.dispose();
		} catch(IOException e){
			e.printStackTrace();
		}
	}

}
