package com.storytron.swat.util;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * This class provides an array which groups its elements in blocks
 * that are written to disk. The block are cached in memory as they are used.  
 * */
public class BufferedArray<T> extends AbstractList<T> {
	private int blockSize;
	private List<Object[]> blockArray;
	private Object[] lasts;
	private int top;
	
	/** 
	 * Creates a buffered array. 
	 * @param blockSize is the amount of elements that are grouped in each block.
	 * Useful when writing to an array is expensive (like writing to disk using
	 * DiskArray).  
	 * */
	public BufferedArray(int blockSize,List<Object[]> array) {
		blockArray = array;
		this.blockSize = blockSize;
		lasts = new Object[blockSize];
		top=0;
	}
	
	/** Yields the element at the given index. */
	@SuppressWarnings("unchecked")
	public T get(int index) {
		int b = index/blockSize;
		if (b<blockArray.size())
			return (T)blockArray.get(b)[index-b*blockSize];
		else
			return (T)lasts[index-b*blockSize];
	}

	/** Adds an element at the end of the array. */
	@Override
	public boolean add(T e) {
		lasts[top++]=e;
		if (top==blockSize) { 
			// Send the last array to the underlying disk array. 
			blockArray.add(lasts);
			top=0;
			lasts=new Object[blockSize];
		}
		return true;
	}
	
	/** Sets the element at a given index. */
	@SuppressWarnings("unchecked")
	@Override
	public T set(int index,T e){
		int b=index/blockSize;
		int i = index-b*blockSize;
		if (blockArray.size()>b) {
			Object[] os=blockArray.get(b);
			T old = (T)os[i];
			os[i]=e;
			blockArray.set(b, os);
			return old;
		} else {
			T old = (T)lasts[i];
			lasts[i] = e;
			return old;
		}
	}
	
	/** Removes all elements from the array. */
	public void clear(){
		blockArray.clear();
		clearBuffer();
	}
	
	/** @return the size of the array. */
	public int size(){
		return blockArray.size()*blockSize+top;
	}

	/** 
	 * @return the unwritten elements (elements in the internal 
	 *         buffer that have not been written yet.)
	 *         All the elements added to the buffered array are
	 *         either in the internal buffer or written in
	 *         the underlying array. 
	 * */
	public Object[] getBufferedElements(){
		Object[] arr = new Object[top];
		System.arraycopy(lasts, 0, arr, 0, top);
		return arr;
	}

	/** 
	 * Clears the elements in the buffer, without inserting them in 
	 * the underlying array. 
	 * */
	public void clearBuffer(){
		top=0;
		Arrays.fill(lasts,null);
	} 

}
