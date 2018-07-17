package com.storytron.swat.util;

import java.util.AbstractList;
import java.util.List;

/** Class that stores array elements compressed. */
public class CompressedArray<T> extends AbstractList<T> {

	private List<Compressed<T>> array;
	
	public CompressedArray(List<Compressed<T>> array){
		this.array = array;
	}
	
	@Override
	public T get(int index) {
		return array.get(index).getObject();
	}

	@Override
	public boolean add(T e) {
		return array.add(new Compressed<T>(e));
	}
	
	@Override
	public int size() {
		return array.size();
	}
	
	@Override
	public void clear(){
		array.clear();
	}

}
