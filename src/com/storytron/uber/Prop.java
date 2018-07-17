package com.storytron.uber;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.storytron.enginecommon.ScaledImage;
import com.storytron.enginecommon.Utils;

/**
This class represents storyworld things.
The Prop state contains some attributes and most of the implementation
just sets or gets them.
<p>
A Prop has the traits 
<ul>
<li>carried, visible, inPlay, owner, location, description.
    Which can be manipulated with getters and setters.
</li><li> For each Prop{@link FloatTrait} in the model ({@link Deikto})
    it holds the value of the trait. The values can be manipulated through
    the methods {@link #getTrait(FloatTrait)} and {@link #setTrait(FloatTrait, float)}.
</li><li> For each Prop {@link TextTrait} in the model ({@link Deikto})
      it holds the value of the trait. The values can be manipulated through
      the methods {@link #getText(TextTrait)} and {@link #setText(TextTrait, float)}.
</li><li> For each {@link Actor}, it has a knowsMe value that tells if the actor 
     knows the Prop. Manipulable through {@link #getKnowsMe(Actor)} and
     {@link #setKnowsMe(Actor, boolean)} 
</li><li>image: holds in memory an image associated to the Prop.
    Manipulated through the methods {@link #getImage(Deikto)} and
    {@link #setImage(ScaledImage)}. It is also possible to retrieve the
    image as a sequence of png bytes ({@link #getImageJPGBytes(Deikto)}). 
    This is useful for sending the image through the cable. If the image 
    is null the png bytes are obtained from the file on disk.  
</li><li> imageFilename: holds the path to an image file associated to the Stage.
    It is manipulated through the methods {@link #getImageName()} and
    {@link #setImageName(String)}, but is also set to null when a
    null image is set using {@link #setImage(ScaledImage)} (setImage(null)). 
</li>
</ul>
* */
public final class Prop extends Word {
	private static final long serialVersionUID = 1L;
	private boolean carried;
	private boolean visible;
	private boolean inPlay; // set to false if prop is destroyed
	private Reference owner;
	private Reference location;
	private String description = " ";
	private ArrayList<Float> traits = new ArrayList<Float>();
	private ArrayList<String> texts = new ArrayList<String>(); 
	private Set<Actor> dontKnowsMe = new TreeSet<Actor>();
	
	/** Stage trait values are initialized to this value. */
	public static final float DEFAULT_PROPTRAITVALUE= 0.0f;
	/** KnowsMe values are initialized to this value. */
	public static final boolean DEFAULT_KNOWSME = true;
	
//**********************************************************************	
	public Prop(String label) {
		super(label);
		carried = false;
		visible = false;
		inPlay = true;
		location = Word.zeroReference;
		owner = Word.zeroReference;
	}
//**********************************************************************	
	/**
	 * Returns the value for a given trait.
	 */
	public float getTrait(FloatTrait t) { 
		if (t.getValuePosition()<traits.size())
			return traits.get(t.getValuePosition());
		else return DEFAULT_PROPTRAITVALUE;
	}
	/**
	 * Sets the value for a given trait.
	 */
	public void setTrait(FloatTrait t,float f) {
		// pad with zeros the undefined positions before the trait value. 
		for(int i=traits.size();i<=t.getValuePosition();i++) 
			traits.add(DEFAULT_PROPTRAITVALUE);
		traits.set(t.getValuePosition(),f);
	}
	
	/** Returns the text corresponding to a given trait for this prop. */
	public String getText(TextTrait t){
		if (t.getValuePosition()<texts.size())
			return texts.get(t.getValuePosition());
		else
			return null; 
	}
	
	/** Sets the text corresponding to a given trait for this prop. */
	public void setText(TextTrait t,String text){
		String zText = Utils.nullifyIfEmpty(text);
		if (t.getValuePosition()<texts.size())
			texts.set(t.getValuePosition(),text);
		else if (zText!=null) {
			for(int i=texts.size();i<t.getValuePosition();i++)
				texts.add(null);
			texts.add(zText);
		}
	}

//**********************************************************************	
	public Prop clone() {
		Prop zProp = (Prop)super.clone();
		zProp.dontKnowsMe = new TreeSet<Actor>(dontKnowsMe);
		zProp.traits = new ArrayList<Float>(traits);
		zProp.texts = new ArrayList<String>(texts);
		return zProp; 
	}
//**********************************************************************	
	public boolean getCarried() { return (carried); }
//**********************************************************************	
	public void setCarried(boolean tNewCarried) { carried = tNewCarried; }
//**********************************************************************	
	public boolean getVisible() { return (visible); }
//**********************************************************************	
	public void setVisible(boolean tNewVisible) { visible = tNewVisible; }
//**********************************************************************	
	public boolean getInPlay() { return (inPlay); }
//**********************************************************************	
	public void setInPlay(boolean tNewInPlay) { inPlay = tNewInPlay;	}
//**********************************************************************	
	public int getOwner() { return owner.getIndex();	}
	public Reference getOwnerRef() { return owner;	}
//**********************************************************************	
	public void setOwner(Actor tNewOwner) {  owner = tNewOwner.getReference();	}
	public void setOwnerRef(Reference r) {  owner = r;	}
//**********************************************************************	
	public int getLocation() {return location.getIndex();	}
	public Reference getLocationRef() { return location;	}
//**********************************************************************	
	public void setLocation(Stage tNewLocation) { location = tNewLocation.getReference();	}
	public void setLocationRef(Reference r) {  location = r;	}
//***************************************************************************
	public boolean getKnowsMe(Actor actor) { return !dontKnowsMe.contains(actor);	}
//***************************************************************************
	public void setKnowsMe(Actor actor, boolean newKnowsMe) {
		if (newKnowsMe)	dontKnowsMe.remove(actor);
		else dontKnowsMe.add(actor);
	}
//***************************************************************************
	public String getDescription() { return description; }
//***************************************************************************
	public void setDescription(String tDescription) { description = tDescription; }
//***************************************************************************
	private String imageFilename = null;
	private ScaledImage image = null;
	private int imageChangeCount;
	/** @returns the associated image or null if there is none. */
	public ScaledImage getImage(Deikto dk){
		if (image!=null || imageFilename==null)
			return image;
		try {
			return image=new ScaledImage(ImageIO.read(dk.getImageFile(imageFilename)));
		} catch(IOException ex){
			return null;
		}
	}
	/** 
	 * Sets an image as the image associated to this prop.
	 * If image is null there won't be an associated image.  
	 * */
	public void setImage(ScaledImage image){
		this.image = image;
		if (image==null)
			setImageName(null);
	}
	/** Sets an image in a file on disk as the image associated to this prop.
	 * The image is assumed to exist on disk and have the given name. */
	public void setImageName(String filename){ 
		imageFilename = filename;
	}
	/** 
	 * @returns the name of the image file on disk associated to this prop, 
	 *           or null if there is no associated image or if it hasn't been
	 *           written yet.
	 * */
	public String getImageName(){ 
		return imageFilename;
	}
	/** Gets the bytes of the image encoded as JPG. */
	public byte[] getImageJPGBytes(Deikto dk) throws IOException {
		return Utils.getImageJPGBytes(dk.getImageFile(imageFilename),image);
	}
	/** Tells if the image was modified since the prop was created. */
	public boolean isImageModified(){
		return imageChangeCount!=0;
	}
	/** Sets the image change count to 0. */
	public void resetImageChangeCount(){ 
		this.imageChangeCount = 0; 
	}
	public void increaseImageChangeCount(){ ++imageChangeCount; }
	public void decreaseImageChangeCount(){ --imageChangeCount; }
};

