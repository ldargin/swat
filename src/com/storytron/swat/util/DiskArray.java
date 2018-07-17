package com.storytron.swat.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;

/** Holds a list of gzipped objects on disk. */
public class DiskArray <T> extends AbstractList<T> {

	private File file;
	private ArrayList<Integer> offsets = new ArrayList<Integer>();
	private RandomAccessFile raf;
	private ReadWriter<T> writer;

	/** Created the disk array, allocating a file on disk for that purpose. */
	public DiskArray() throws IOException {
		this(null);
		writer = new ReadWriter<T>(){
			@SuppressWarnings("unchecked")
			public T read(RandomAccessFile raf,int bytes) throws IOException {
				try {
					byte[] bs = new byte [bytes];
					raf.readFully(bs);
					ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bs));
					T o = (T)ois.readObject();
					ois.close();
					return o;
				} catch(ClassNotFoundException e){
						e.printStackTrace();
				}
				return null;
			}
			public int write(T e, RandomAccessFile raf,int available) throws IOException {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(e);
				oos.close();
				byte[] bs = bos.toByteArray();
				if (bs.length<=available)
					raf.write(bs);
				return bs.length;
			}
		};
	}
	/** Passes the given ReadWriter to read and write array elements. */
	public DiskArray(ReadWriter<T> writer) throws IOException {
		this.writer = writer;
		file = File.createTempFile("swat_",".tmp");
    	//System.out.println("log temp file: "+file.getCanonicalPath());
		raf = new RandomAccessFile(file,"rw");
		offsets.add(0);
	}
	
	/** 
	 * Interface for customizing the way reading and writing 
	 * is done to the array. 
	 * */
	public interface ReadWriter<T> {
		/**
		 * @param the available space to write. This method should not
		 *        write anything if there is not enough available space.
		 * @return the amount of bytes written. If there is not available space
		 *         returns a value greater than available (whichever). 
		 * */
		public int write(T e,RandomAccessFile raf,int available) throws IOException;
		/** @param bytes the amount of bytes to read. */
		public T read(RandomAccessFile raf,int bytes) throws IOException;
	}
	
	public int size(){ return offsets.size()-1; }
	
	/** Returns object at position index, or null if such position does not exist. */
	public T get(int index){
		try {
			raf.seek(offsets.get(index));
			return writer.read(raf,offsets.get(index+1)-offsets.get(index));
		} catch(IOException e) {
			throw new RuntimeException(e); 
		}
	};
	/** Adds an object to the end of the list. */
	@Override
	public boolean add(T elem){
		try {
			raf.seek(raf.length());
			writer.write(elem, raf, Integer.MAX_VALUE);
			offsets.add((int)raf.length());
		} catch(IOException e){
			throw new RuntimeException(e); 
		}
		return true;
	};

	/**
	 * @throws IllegalArgumentException if the element is greater than the 
	 *         available space on the file for the slot at position index.
	 * */
	@Override
	public T set(int index,T elem){
		try {
			raf.seek(offsets.get(index));
			int available = offsets.get(index+1)-offsets.get(index);
			int size = writer.write(elem, raf, available);
			if (size>available)
				throw new IllegalArgumentException("The element being written is greater than the available space.");
		} catch(IOException e){
			e.printStackTrace();
		}
		return elem;
	} 
	
	/** Removes the last element in the disk array. */
	public void removeLast(){
		if (offsets.size()<=1)
			return;
		try{
			raf.setLength(offsets.get(offsets.size()-2));
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			offsets.remove(offsets.get(offsets.size()-1));
		}
	}

	/** Empties the temporal storage on disk. */
	@Override
	public void clear() {
		try{
			raf.setLength(0);
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			offsets.clear();
			offsets.add(0);
		}
	}

	/** Deletes the temporal storage on disk. */
	public void dispose(){
		try {
			raf.close();
		} catch(IOException e) {
		} finally {
			file.delete();
			file = null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (file!=null)
			dispose();
		super.finalize();
	}

	/** A test program with a sample usage for this class. */
	public static void main(String[] args){
		try {
			DiskArray<int[]> da = new DiskArray<int[]>();
			int[] ia0 = new int[]{ 0,0,0,0 };
			da.add(ia0);
			int[] ia1 = new int[]{ 0,1,0,0 };
			da.add(ia1);
			int[] ia2 = new int[]{ 0,0,0,1 };
			da.add(ia2);
			
			System.out.println(Arrays.equals(da.get(0),ia0));
			System.out.println(Arrays.equals(da.get(1),ia1));
			System.out.println(Arrays.equals(da.get(2),ia2));

			int[] ia3 = new int[]{ 0,1,1,0 };
			da.set(1,ia3);
			System.out.println(Arrays.equals(da.get(1),ia3));
			
			da.dispose();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
}
