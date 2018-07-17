package com.storytron.enginecommon;

import java.awt.image.BufferedImage;

/** 
 * Class for manipulating scaled images.
 * <p>
 * Scaling operations are not applied directly on the images, but the 
 * operation is recorded so it is conveniently applied when invoking the 
 * {@link #getBufferedImage()} method.
 * <p>
 * The method {@link #applyScaling()} actually writes a new image.
 * This is useful to avoid using memory for a big image that will be shown
 * shrunk. 
 * */
public class ScaledImage implements Cloneable {
	private BufferedImage image;
	private int w, h;
	
	/** Creates a scaled image from an image. */
	public ScaledImage(BufferedImage image){
		if (image==null)
			throw new NullPointerException();
		
		this.image = image;
		w = image.getWidth();
		h = image.getHeight();
	}
	
	/** Returns the width of the scaled image. */
	public int getWidth() { return w; }
	/** Returns the height of the scaled image. */
	public int getHeight() { return h; }
	
	/** Scales the image to have the given width and height. */
	public void scaleTo(int w,int h) { 
		this.w = w;
		this.h = h;
	}
	
	/** Resizes the image to match the current dimensions ({@link #getWidth()},{@link #getHeight()}). */
	public void applyScaling(){
		image = getBufferedImage();
		w = image.getWidth();
		h = image.getHeight();
	}

	/** Returns the underlying image resized to the current dimensions. */
	public BufferedImage getBufferedImage(){
		if (getWidth()!=image.getWidth() || getHeight()!=image.getHeight())
			return Utils.getResizedImage(image, getWidth(), getHeight());
		else 
			return image;
	}
	
	/** 
	 * Creates a superficial copy of this scaled image (i.e. the underlying
	 * image is not copied). 
	 * */
	@Override
	public ScaledImage clone() {
		try {
			return (ScaledImage)super.clone();
		}catch(CloneNotSupportedException e) {
			return null;
		}
	}
}
