package com.storytron.swat.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Class for compressing objects.
 * The compression method is serializing the object and
 * gzipping the result.
 * */
public class Compressed <T> implements Serializable {
	static final long serialVersionUID=0;
	
	public byte[] object;
	
	public Compressed(T o){
		setObject(o);
	}
	
	/** Compresses an object and stores the result. */
	public void setObject(T o) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ZipOutputStream zos = new ZipOutputStream(bos);
			zos.putNextEntry(new ZipEntry("entry"));
			ObjectOutputStream oos = new ObjectOutputStream(zos);
			oos.writeObject(o);
			oos.close();
			zos.close();
		} catch (IOException exc) {
			exc.printStackTrace();
		}
		object = bos.toByteArray();
	}
	
	/** 
	 * Uncompresses and returns the object. 
	 * Returns null if there is no object stored. 
	 * */
	@SuppressWarnings("unchecked")
	public T getObject(){
		if (object==null)
			return null;
		
		ByteArrayInputStream bis = new ByteArrayInputStream(object);
		T o;
		try {
			ZipInputStream zis = new ZipInputStream(bis);
			zis.getNextEntry();
			ObjectInputStream ois = new ObjectInputStream(zis);
			o = (T)ois.readObject();
			ois.close();
			return o;
		} catch (IOException exc){
			exc.printStackTrace();
		} catch (ClassNotFoundException exc){
			exc.printStackTrace();
		}
		return null;
	}
}
