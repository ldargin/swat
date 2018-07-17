package com.storytron.enginecommon;

import java.awt.image.BufferedImage;
import java.io.Serializable;

/** 
 * Background data for a given item. It contains a label, a description,
 * and an image.
 * */
public final class BgItemData  implements Serializable {
	private static final long serialVersionUID = 1l;
	private String label;
	private String description;
	private ImageGetter imageGetter;
	
	public BgItemData(String itemLabel, String itemDescription, ImageGetter imageGetter) {
		label = itemLabel;
		description = itemDescription;
		this.imageGetter = imageGetter;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getDescription() {
		return description;
	}
	
	public BufferedImage getImage() {
		if (imageGetter==null)
			return null;
		else
			return imageGetter.getImage();
	}
	
	@Override
	public String toString(){
		return label;
	}
	
	/** 
	 * This interface hides how the images are actually obtained.
	 * Three cases are expected: from memory, from disk, and from the
	 * server.
	 * */
	public interface ImageGetter {
		public BufferedImage getImage();
	}
}