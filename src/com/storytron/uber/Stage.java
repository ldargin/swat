package com.storytron.uber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import com.storytron.enginecommon.ScaledImage;
import com.storytron.enginecommon.Utils;

/**
  This class represents storyworld places.
  The stage state contains some attributes and most of the implementation
  just sets or gets them. The only exception are the methods
  {@link #getTravelingTime(Stage)} and {@link #getTravelingTime(double, double)}
  that calculate the distance to other stages.
  <p>
  A Stage has the traits 
  <ul>
  <li>doorOpen, population, owner, xCoord, yCoord, description.
  Which can be manipulated with getters and setters. 
  </li><li> For each {@link Actor}, it has an unwelcoming_Homey value that tells for each
      Actor how comfortable he feels in the Stage. Manipulable through
      methods {@link #getUnwelcoming_Homey(Actor)} and
      {@link #setUnwelcoming_Homey(Actor, float)}.
  </li><li> For each Stage {@link FloatTrait} in the model ({@link Deikto})
      it holds the value of the trait. The values can be manipulated through
      the methods {@link #getTrait(FloatTrait)} and {@link #setTrait(FloatTrait, float)}.
  </li><li> For each Stage {@link TextTrait} in the model ({@link Deikto})
      it holds the value of the trait. The values can be manipulated through
      the methods {@link #getText(TextTrait)} and {@link #setText(TextTrait, float)}.
  </li><li> For each {@link Actor} has a knowsMe value that tells if the actor knows the 
       Stage. Manipulable through {@link #getKnowsMe(Actor)} and
       {@link #setKnowsMe(Actor, boolean)}
   </li><li>image: holds in memory an image associated to the Stage.
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
public final class Stage extends Word {
	private static final long serialVersionUID = 1L;
	public static final int Nowhere = 0;
	private boolean doorOpen = true; // are actors permitted to enter and exit?
	private int population;
	private Reference owner;
	private float xCoord;
	private float yCoord;
	private ArrayList<Float> traits = new ArrayList<Float>();
	private ArrayList<String> texts = new ArrayList<String>();
	private Map<Actor,Float> unwelcoming_Homey = new TreeMap<Actor,Float>();
	private Set<Actor> dontKnowsMe = new TreeSet<Actor>();
	private String description = " ";

	/** P2Unwelcoming_Homeyt values are initialized to this value. */
	public static final float DEFAULT_P2UNWELCOMINGHOMEY= 0.0f;
	/** Stage trait values are initialized to this value. */
	public static final float DEFAULT_STAGETRAITVALUE= 0.0f;
	/** KnowsMe values are initialized to this value. */
	public static final boolean DEFAULT_KNOWSME = true;

	//***************************************************************************
	public Stage(String label) {
		super(label);
		population = 0;
		owner = Word.zeroReference;
		xCoord = 0;
		yCoord = 0;
	}
	//***************************************************************************
	public Stage clone() {
		Stage zStage = (Stage)super.clone();
		zStage.unwelcoming_Homey = new TreeMap<Actor,Float>(unwelcoming_Homey);
		zStage.dontKnowsMe = new TreeSet<Actor>(dontKnowsMe);
		zStage.traits = new ArrayList<Float>(traits);
		zStage.texts = new ArrayList<String>(texts);
		return (zStage);
	}
	//		**********************************************************************	
	/**
	 * Returns the value for a given trait.
	 */
	public float getTrait(FloatTrait t) { 
		if (t.getValuePosition()<traits.size())
			return traits.get(t.getValuePosition());
		else return DEFAULT_STAGETRAITVALUE;
	}
	/**
	 * Sets the value for a given trait.
	 */
	public void setTrait(FloatTrait t,float f) {
		// pad with zeros the undefined positions before the trait value. 
		for(int i=traits.size();i<=t.getValuePosition();i++) 
			traits.add(DEFAULT_STAGETRAITVALUE);
		traits.set(t.getValuePosition(),f);
	}
	
	/** Returns the text corresponding to a given trait for this stage. */
	public String getText(TextTrait t){
		if (t.getValuePosition()<texts.size())
			return texts.get(t.getValuePosition());
		else
			return null; 
	}
	
	/** Sets the text corresponding to a given trait for this stage. */
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

	//***************************************************************************
	public int getPopulation() {return (population);}
	//***************************************************************************
	public void setPopulation(int newPopulation) {population = newPopulation;}
	//***************************************************************************
	public int getOwner() { return owner.getIndex();	}
	//**********************************************************************	
	public void setOwner(Actor tNewOwner) {  owner = tNewOwner.getReference();	}
	//***************************************************************************
	public float getXCoord() {	return (xCoord);}
	//***************************************************************************
	public void setXCoord(float newXCoord) {xCoord = newXCoord;	}
	//***************************************************************************
	public float getYCoord() {	return (yCoord);}
	//***************************************************************************
	public void setYCoord(float newYCoord) {yCoord = newYCoord;	}
	//***************************************************************************
	public float getUnwelcoming_Homey(Actor ofWhom) { 
		Float f = unwelcoming_Homey.get(ofWhom);
		if (f==null) return DEFAULT_P2UNWELCOMINGHOMEY;
		else return f;
	}
	//***************************************************************************
	public void setUnwelcoming_Homey(Actor ofWhom, float newUnwelcoming_Homey) {
		if (newUnwelcoming_Homey==DEFAULT_P2UNWELCOMINGHOMEY) unwelcoming_Homey.remove(ofWhom); 
		else unwelcoming_Homey.put(ofWhom, newUnwelcoming_Homey);
	}
	public void removeAllUnwelcoming_Homey() { unwelcoming_Homey.clear(); }
	//	***************************************************************************
	public boolean getDoorOpen() {	return (doorOpen);	}
	//***************************************************************************
	public void setDoorOpen(boolean newDoorOpen) {doorOpen = newDoorOpen;}
	//	***************************************************************************
	public boolean getKnowsMe(Actor actor) {return !dontKnowsMe.contains(actor);}
	//***************************************************************************
	public void setKnowsMe(Actor actor, boolean newKnowsMe) {
		if (newKnowsMe) dontKnowsMe.remove(actor);
		else dontKnowsMe.add(actor);
	}
	//	***************************************************************************
	public String getDescription() {
		return description;
	}
	//***************************************************************************
	public void setDescription(String description) {
		this.description = description;
	}
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
	 * Sets an image as the image associated to this stage.
	 * If image is null there won't be an associated image.  
	 * */
	public void setImage(ScaledImage image){
		this.image = image;
		if (image==null)
			setImageName(null);
	}
	/** Sets an image in a file on disk as the image associated to this stage.
	 * The image is assumed to exist on disk and have the given name. */
	public void setImageName(String filename){ 
		imageFilename = filename;
	}
	/** 
	 * @returns the name of the image file on disk associated to this stage, 
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
	/** Tells if the image was modified since the stage was created. */
	public boolean isImageModified(){
		return imageChangeCount!=0;
	}
	/** Sets the image change count to 0. */
	public void resetImageChangeCount(){ 
		this.imageChangeCount = 0; 
	}
	public void increaseImageChangeCount(){ ++imageChangeCount; }
	public void decreaseImageChangeCount(){ --imageChangeCount; }

	/**
	 * Calculates the traveling time needed to go from this stage to another one.
	 * @param to the stage towards the time is calculated.
	 * @return traveling time needed to go from this stage to <code>to</code>
	 * */
	public double getTravelingTime(Stage to) {
		return getTravelingTime(to.getXCoord(),to.getYCoord());
	}
	/**
	 * Calculates the traveling time needed to go from this stage to another location.
	 * @param toX horizontal coordinate of the location towards the time is calculated.
	 * @param toY vertical coordinate of the location towards the time is calculated.
	 * @return traveling time needed to go from this stage to <code>to</code>
	 * */
	public double getTravelingTime(double toX,double toY) {
		return Math.hypot(toX-getXCoord(),toY-getYCoord())*100;
	} 
}
