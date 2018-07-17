package com.storytron.enginecommon;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

import com.storytron.enginecommon.BgItemData.ImageGetter;

public final class SerializedImage implements Serializable, ImageGetter {

	public static final long serialVersionUID = 0L;
	private byte[] image;
	public SerializedImage(byte[] image){
		this.image = image;
	}
	public BufferedImage getImage() {
		try {
			if (image == null)
				return null;  // the image does not exist
			else 
				return ImageIO.read(new ByteArrayInputStream(image));

		} catch (IOException e){
			e.printStackTrace();
			return null;
		}				
	}	

}
