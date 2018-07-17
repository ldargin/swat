package com.storytron.uber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import com.storytron.enginecommon.ScaledImage;
import com.storytron.enginecommon.Utils;

/**
  Class for representing storyworld characters. 
  An Actor object contains the state needed to represent the situation of
  a character in a storyworld. This state is described by traits,
  and certain traits can be added or removed by the author.  
  <p>
  Basically, the Actor class implements a convenient interface for setting
  and getting the values that define the state, under the assumption that
  some of this traits can be removed or added from the model.
  <p>
  Traits which can not be removed are implemented as fields of the class
  accessed through methods that wrap them. Traits that can be removed or added,
  are grouped by data type (Text or BNumber) and organized into resizable
  arrays that hold the trait values. Text trait values are indexed with the type
  {@link TextTrait}, and BNnumber traits are indexed with the type {@link FloatTrait}. 
  <p>
  For each Actor {@link FloatTrait} that exists in the model ({@link Deikto}) 
  there are certain related values in the Actor state. 
  <ul>
  <li>A value for the trait. Manipulated with {@link #set(FloatTrait, float)} and
  		{@link #get(FloatTrait)} </li>
  <li>A value indicating how the Actor perceives the trait on a another Actor.
      Manipulated with {@link #getP(FloatTrait, Actor)} and 
      {@link #setP(FloatTrait, Actor, Float)}. </li>
  <li>A value indicating how sure the Actor is of the value he perceives 
      for the trait on a another Actor.
      Manipulated with {@link #setC(FloatTrait, Actor, Float)} and 
      {@link #getC(FloatTrait, Actor)}. </li>
  <li>A value indicating how the Actor tends to perceive the value of the Trait 
      in other Actors. Represents how optimistic/pessimistic or humble/conceited 
      the Actor is with respect to it. Manipulated with {@link #getAccord(FloatTrait)}
      and {@link #setAccord(FloatTrait, float)}</li>
  <li>A value indicating how important the trait is to the Actor. Would he 
  		discriminate people based on the Trait? Manipulated with
  		{@link #getWeight(FloatTrait)} and {@link #setWeight(FloatTrait, float)}</li>
  </ul>
  The Text traits are retrieved through the methods {@link #getText(TextTrait)} and
  {@link #setText(TextTrait, String)}.
  <p>
  For each {@link Stage} {@link FloatTrait} that exists in the model ({@link Deikto}) 
  there are certain related values in the Actor state.
  <ul>
  <li>A value indicating how the Actor perceives the trait on a given {@link Stage}.
      Manipulated with {@link #getP(FloatTrait, Stage)} and {@link #setP(FloatTrait, Stage, Float)}. </li>
  <li>A value indicating how sure the Actor is of the value he perceives 
      for the trait on a {@link Stage}.
      Manipulated with {@link #setC(FloatTrait, Stage, Float)} and 
      {@link #getC(FloatTrait, Stage)}. </li>
  </ul>

  For each {@link Prop} {@link FloatTrait} that exists in the model ({@link Deikto}) 
  there are certain related values in the Actor state.
  <ul>
  <li>A value indicating how the Actor perceives the trait on a given {@link Prop}.
      Manipulated with {@link #getP(FloatTrait, Prop)} and {@link #setP(FloatTrait, Prop, Float)}. </li>
  <li>A value indicating how sure the Actor is of the value he perceives 
      for the trait on a {@link Prop}.
      Manipulated with {@link #setC(FloatTrait, Prop, Float)} and 
      {@link #getC(FloatTrait, Prop)}. </li>
  </ul>

  The fixed traits are:
  <ul>
  <li>active, female, dontMoveMe, unconscious, location, spyingOn,
		targetStage, occupiedUntil, cBored, howLongToSpy, description.
		All of them have get an set methods.</li>
  <li>image: holds in memory an image associated to the actor.
      Manipulated through the methods {@link #getImage(Deikto)} and
      {@link #setImage(ScaledImage)}. It is also possible to retrieve the
      image as a sequence of png bytes. This is useful for sending the
      image through the cable. If the image is null the png bytes are
      obtained from the file on disk.  
  </li>
  <li> imageFilename: holds the path to an image file associated to the actor.
      It is manipulated through the methods {@link #getImageName()} and
      {@link #setImageName(String)}, but is also set to null when a
      null image is set using {@link #setImage(ScaledImage)} (setImage(null)). 
  </li>
  <li>The elements enumerated in {@link ExtraTrait}.
	  Manipulated with {@link #get(com.storytron.uber.Actor.ExtraTrait, Actor)}
	  and {@link #set(com.storytron.uber.Actor.ExtraTrait, Actor, float)}</li>
  <li>The elements enumerated in {@link MoodTrait}. Manipulated with
      {@link #get(com.storytron.uber.Actor.MoodTrait)} and
      {@link #set(com.storytron.uber.Actor.MoodTrait, float)}.</li>
  </ul>
  
  Traits can be iterated like shown in the following code snippets.
  <pre>
  // All non-fixed traits
  for(Trait t:dk.getActorTraits()) {
  	zActor.get(t);zActor.set(t,floatValue);
  	zActor.getP(t,ofWhom);zActor.setP(t,ofWhom,floatValue);
  	zActor.getC(t,ofWhom);zActor.setC(t,ofWhom,floatValue);
  	if (t.isVisible()) {
  		zActor.getAccord(t);zActor.setAccord(t,floatValue);
  		zActor.getWeight(t);zActor.setWeight(t,floatValue);
  	}
  }
  for(Trait t:dk.getStageTraits()) {
  	zActor.getP(t,ofWhom);zActor.setP(t,ofWhom,floatValue);
  	zActor.getC(t,ofWhom);zActor.setC(t,ofWhom,floatValue);
  }
  for(Trait t:dk.getPropTraits()) {
  	zActor.getP(t,ofWhom);zActor.setP(t,ofWhom,floatValue);
  	zActor.getC(t,ofWhom);zActor.setC(t,ofWhom,floatValue);
  }
  // All moods
  for (MoodTrait t:Actor.MoodTraits) {zActor.get(t);zActor.set(t,floatValue);}
  // All extra traits
  for (ExtraTrait t:Actor.ExtraTraits) {zActor.get(t,ofWhom);zActor.set(t,ofWhom,floatValue);}
 </pre>

 */
public final class Actor extends Word {
	private static final long serialVersionUID = 1L;
	
	/** KnowsMe values are initialized to this value. */
	public static final boolean DEFAULT_KNOWSME = true;
	
	// Start of protected fields
	private boolean active; // am I alive and active?
	private boolean female; // am I a female?
	private boolean dontMoveMe; // am I locked in my current stage by the storybuilder?
	private boolean unconscious;// am I unconscious?
	private boolean human; // am I played by a human player?
	private Reference location; // stage which I currently occupy
	private Reference spyingOn;
	private Reference targetStage; // destination assigned by storybuilder
	private int occupiedUntil; // busy with event execution
	private int cBored; // how long I've been doing nothing
	private int howLongToSpy; // continue spying until this minute arrives
	private int onThePhoneWith; // index of Actor with whom I'm talking on the phone
	private Set<Actor> dontKnowsMe = new TreeSet<Actor>(); // actors who don't know this actor
	private ArrayList<String> texts = new ArrayList<String>(); // text values
	private String description = " ";
	private String imageFilename = null;
	private ScaledImage image = null;
	private int imageChangeCount = 0;

	/**
	 * Gets the value a float trait.
	 * @param t trait to get.
	 * @return value of the trait.
	 */
	public float get(FloatTrait t){	return get(TraitType.Normal,t);	}

	/**
	 * Gets the value of a float trait.
	 * @param tt type of value.
	 * @param t trait to get.
	 * @return value of the trait.
	 */
	public float get(TraitType tt,FloatTrait t){	
		// Return 0.0f if the trait has not been initialized. 
		// This is for reducing initialization code in Actor constructors.
		Float f = pm.get(tt,t);
		if (f==null) return DEFAULT_FLOATTRAITVALUE;
		else return f;	
	}
	/**
	 * Sets a new value for a float trait, overriding the previous one.
	 * @param t trait to set.
	 * @param f value to set.  
	 * */
	public void set(FloatTrait t,float f){ 
		set(TraitType.Normal,t,f);
	}

	/**
	 * Sets a new value for a float trait, overriding the previous one.
	 * @param tt type of value.
	 * @param t trait to set.
	 * @param f value to set.  
	 * */
	public void set(TraitType tt,FloatTrait t,float f){
		// If the new value is 0 we do not need to store the value.
		if (f==DEFAULT_FLOATTRAITVALUE) 
			pm.remove(tt,t);
		else 
			pm.set(tt,t,f);	
	}

	/**
	 * Gets the value of a mood trait.
	 * @param t trait to get.
	 * @return value of the trait.
	 */
	public float get(MoodTrait t){	
		// Return 0.0f if the trait has not been initialized. 
		// This is for reducing initialization code in Actor constructors. 
		Float f = pm.get(t);	
		if (f==null) return DEFAULT_FLOATTRAITVALUE;
		else return f;	
	}
	/**
	 * Sets a new value for a mood trait, overriding the previous one.
	 * @param t trait to set.
	 * @param f value to set.  
	 * */
	public void set(MoodTrait t,float f){
		// If the new value is 0 we do not need to store the value.
		if (f==DEFAULT_FLOATTRAITVALUE) pm.remove(t);
		else pm.set(t,f);	
	}


	/**
	 * Gets the value a float trait.
	 * @param t trait to get.
	 * @return value of the trait.
	 */
	public float getAccord(FloatTrait t){ return get(TraitType.Accord,t); }
	/**
	 * Sets a new value for a float trait, overriding the previous one.
	 * @param t trait to set.
	 * @param f value to set.  
	 * */
	public void setAccord(FloatTrait t,float f){ set(TraitType.Accord,t,f);	}

	/**
	 * Gets the value a float trait.
	 * @param t trait to get.
	 * @return value of the trait.
	 */
	public float getWeight(FloatTrait t){	return get(TraitType.Weight,t); }
	/**
	 * Sets a new value for a float trait, overriding the previous one.
	 * @param t trait to set.
	 * @param f value to set.  
	 * */
	public void setWeight(FloatTrait t,float f){ set(TraitType.Accord,t,f); }


	/**
	 * Gets the perceived values of a trait of another actor.
	 * @param tt trait type (Perception or Confidence).
	 * @param t trait to get.
	 * @param a actor having the perceived trait.
	 * @return value of the trait.
	 */
	public float get(PTraitType tt,FloatTrait t,Actor a){
		Map<Actor,Float> m = pm.get(tt,t); 
		Float f = null;
		if (m!=null)
			f = m.get(a);
		if (f==null)
			switch(tt){
			case Perception: return systemAssignedPValue(t,a);
			default: return systemAssignedCValue(t,a);
			}
		else return f;	
	}
	public float getP(FloatTrait t,Actor a){ return get(PTraitType.Perception,t,a); }
	public float getC(FloatTrait t,Actor a){ return get(PTraitType.Confidence,t,a); }	

	/**
	 * Gets the perceived value of a prop trait.
	 * @param t trait to get.
	 * @param p prop having the perceived trait.
	 * @return value of the trait.
	 */
	public float getP(FloatTrait t,Prop p){ return get(PTraitType.Perception,t,p); }
	public float getC(FloatTrait t,Prop p){ return get(PTraitType.Confidence,t,p); }
	public float get(PTraitType tt,FloatTrait t,Prop p){
		ArrayList<Float> m = propPTraits.get(tt).get(p);
		Float f = m==null || t.getValuePosition()>=m.size() ? null: m.get(t.getValuePosition());
		if (f==null)
			if (p.getKnowsMe(this) && t.isVisible())
				switch(tt){
				case Perception: return p.getTrait(t);
				default: return 0.99f;
				}
			else
				switch(tt){
				case Perception: return DEFAULT_P2FLOATTRAITVALUE;
				default: return -0.99f;
				}
		else return f;	
	}
	public void set(PTraitType tt,FloatTrait t,Prop p,Float f){
		ArrayList<Float> ts = propPTraits.get(tt).get(p);
		if (f==null) {
			if (ts!=null && t.getValuePosition()<ts.size()) 
				ts.set(t.getValuePosition(),null);
			return;
		}
		if (ts==null) {
			ts=new ArrayList<Float>();
			propPTraits.get(tt).put(p,ts);
		}

		// pad with null the undefined positions before the trait value. 
		for(int i=ts.size();i<=t.getValuePosition();i++) 
			ts.add(null);
		ts.set(t.getValuePosition(),f);
	}
	public void setP(FloatTrait t,Prop p,Float f){ set(PTraitType.Perception,t,p,f); }
	public void setC(FloatTrait t,Prop p,Float f){ set(PTraitType.Confidence,t,p,f); }


	/**
	 * Gets the perceived value of a stage trait.
	 * @param t trait to get.
	 * @param s stage having the perceived trait.
	 * @return value of the trait.
	 */
	public float getP(FloatTrait t,Stage p){ return get(PTraitType.Perception,t,p); }
	public float getC(FloatTrait t,Stage p){ return get(PTraitType.Confidence,t,p); }
	public float get(PTraitType tt,FloatTrait t,Stage s){
		ArrayList<Float> m = stagePTraits.get(tt).get(s);
		Float f = m==null  || t.getValuePosition()>=m.size() ? null: m.get(t.getValuePosition());
		if (f==null)
			if (s.getKnowsMe(this) && t.isVisible())
				switch(tt){
				case Perception: return s.getTrait(t);
				default: return 0.99f;
				}
			else
				switch(tt){
				case Perception: return DEFAULT_P2FLOATTRAITVALUE;
				default: return -0.99f;
				}
		else return f;
	}
	public void set(PTraitType tt,FloatTrait t,Stage s,Float f){
		ArrayList<Float> ts = stagePTraits.get(tt).get(s);
		if (f==null) {
			if (ts!=null && t.getValuePosition()<ts.size()) 
				ts.set(t.getValuePosition(),null);
			return;
		}
		if (ts==null) {
			ts=new ArrayList<Float>();
			stagePTraits.get(tt).put(s,ts);
		}

		// pad with null the undefined positions before the trait value. 
		for(int i=ts.size();i<=t.getValuePosition();i++) 
			ts.add(null);
		ts.set(t.getValuePosition(),f);
	}
	public void setP(FloatTrait t,Stage s,Float f){ set(PTraitType.Perception,t,s,f); }
	public void setC(FloatTrait t,Stage s,Float f){ set(PTraitType.Confidence,t,s,f); }


	/**
	 * Gets the value of an extra trait for another actor.
	 * @param t trait to get.
	 * @param a target actor.
	 * @return value of the trait.
	 * @see #get(com.storytron.uber.Actor.InnerTrait)
	 */
	public float get(ExtraTrait t,Actor a){	
		Float f = pm.get(t).get(a);
		if (f==null)
			return DEFAULT_P2FLOATTRAITVALUE;
		else 
			return f;	
	}


	/**
	 * Tells if the perceived value of a trait of another actor is specified by the user
	 * or calculated by the system.
	 * You can override a perceived trait by specifying a value for it with 
	 * {@link #setP(com.storytron.uber.Actor.InnerTrait, Actor, Float)}.
	 * @param t trait to get.
	 * @param a actor having the perceived trait.
	 * @return true iff the value is specified by the user.
	 * @see #get(com.storytron.uber.Actor.InnerTrait)
	 */
	public boolean isOverrided(FloatTrait t,Actor a){
		Map<Actor,Float> m = pm.get(PTraitType.Perception,t);
		return  m!=null && null != m.get(a);
	}

	/**
	 * Tells if the perceived value of a prop trait is specified by the user
	 * or calculated by the system.
	 * You can override a perceived trait by specifying a value for it with 
	 * {@link #setP(FloatTrait, Prop, Float)}.
	 * @param t trait to get.
	 * @param p prop having the perceived trait.
	 * @return true iff the value is specified by the user.
	 */
	public boolean isOverrided(FloatTrait t,Prop p){
		ArrayList<Float> m = propPTraits.get(PTraitType.Perception).get(p);
		return  m!=null && t.getValuePosition()<m.size() && null != m.get(t.getValuePosition());
	}

	/**
	 * Tells if the perceived value of a stage trait is specified by the user
	 * or calculated by the system.
	 * You can override a perceived trait by specifying a value for it with 
	 * {@link #setP(FloatTrait, Stage, Float)}.
	 * @param t trait to get.
	 * @param s stage having the perceived trait.
	 * @return true iff the value is specified by the user.
	 */
	public boolean isOverrided(FloatTrait t,Stage s){
		ArrayList<Float> m = stagePTraits.get(PTraitType.Perception).get(s);
		return  m!=null && t.getValuePosition()<m.size() && null != m.get(t.getValuePosition());
	}

	/**
	 * Sets the value of a trait of another actor, overriding the previous value.
	 * If the specified value is <code>null</code> the perceived value will be calculated by
	 * the system. 
	 * @param tt type of value.
	 * @param t trait to set.
	 * @param a actor having the perceived trait.
	 * @param f value to set.  
	 * */
	public void set(PTraitType tt,FloatTrait t,Actor a,Float f){
		Map<Actor,Float> m = pm.get(tt,t);
		if (f==null) {
			if (m!=null) 
				m.remove(a);
		} else {
			if (m==null){
				m = new TreeMap<Actor,Float>();
				pm.set(tt,t,m);
			}
			m.put(a,f);
		}
	}
	public void setP(FloatTrait t,Actor a,Float f){ set(PTraitType.Perception,t,a,f); }
	public void setC(FloatTrait t,Actor a,Float f){ set(PTraitType.Confidence,t,a,f); }

	/**
	 * Sets the value of an extra trait for another actor, overriding the previous value.
	 * @param t trait to set.
	 * @param a target actor.
	 * @param f value to set.  
	 * */
	public void set(ExtraTrait t,Actor a,float f){ 
		if (f==DEFAULT_P2FLOATTRAITVALUE) 
			pm.get(t).remove(a);
		else 
			pm.get(t).put(a,f);
	}

	/**
	 * Returns which would be the system assigned value for a given
	 * perceived trait.  
	 * @param t The given perceived trait. It must hold <code>P2Traits.contains(t)</code>.
	 * @param a The actor having the perceived trait.
	 * @return The value that would be assigned.
	 */
	private float systemAssignedPValue(FloatTrait t, Actor a) {
		if (a.getKnowsMe(this) && t.isVisible())
			return a.get(t);
		else {
			float weightingFactor = 1.0f-((1.0f-get(Actor.ExtraTrait.stranger_Kin,a))/2.0f);
			return a.get(t)*weightingFactor + getAccord(t)*(1.0f-weightingFactor);
		}
	}
	
	/**
	 * Returns which would be the system assigned value for the
	 * uncertainty of a given perceived trait.  
	 * @param t The given perceived trait. It must hold <code>P2Traits.contains(t)</code>.
	 * @param a The actor having the perceived trait.
	 * @return The value that would be assigned.
	 */
	private float systemAssignedCValue(FloatTrait t, Actor a) {
		if (a.getKnowsMe(this) && t.isVisible())
			return Utils.MAXI_VALUE;
		else {
			if (t.isVisible())
				return Utils.MINI_VALUE;
			else
				return get(Actor.ExtraTrait.stranger_Kin,a);
		}
	}

	/** Returns the text corresponding to a given trait for this actor. */
	public String getText(TextTrait t){
		if (t.getValuePosition()<texts.size())
			return texts.get(t.getValuePosition());
		else
			return null; 
	}
	
	/** Sets the text corresponding to a given trait for this actor. */
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
	
	/**
	 * This is a convenience class for handling maps of things parallel
	 * to the actor properties.  
	 * In ActorEditor it is used for String with descriptions and JComponents.
	 * 
	 * <p>
	 * Basically this is a class that wraps three maps:
	 * <ul>
	 * <li>one <code>EnumMap<FloatTrait,ValueT></code></li> 
	 * <li>one <code>EnumMap<P2FloatTrait,P2ValueT></code></li> 
	 * <li>one <code>EnumMap<KnowsTrait,KnowsValueT></code> </li>
	 * </ul>
	 * where ValueT, P2ValueT, and KnowsValueT are type parameters
	 * that you instantiate whatever you like. </p>
	 * 
	 * <p>The get and set methods are overloaded so
	 * <ul> 
	 * <li><code>get(FloatTrait t)</code> returns things of the first map</li>
	 * <li><code>get(P2FloatTrait t)</code> returns things of the second map</li>
	 * <li><code>get(KnowsTrait t)</code> returns things of the third map</li>
	 * </ul>
	 * And analogously for the set methods.
	 * So when you have an expresion like
	 *     <blockquote><code>pm.get(trait)</code></blockquote>
	 * it automatically returns the value from the correct map 
	 * basing its decision on the type of "trait".</p>
	 * 
	 * <p>If you have that ValueT = Box, P2ValueT = Box and KnowsValueT = JCheckBox
	 * you have two maps of Box values and one of JCheckBox values.<p>
	 * 
	 *    <blockquote><code>PropertyMap<Box,Box,JCheckBox> pm;</code></blockquote>
	 * 
	 * <p>Now you can access any of the maps with
	 * <ul> 
	 *    <li><code>pm.get(someFloatTrait)</code></li>
	 *    <li><code>pm.get(someP2FloatTrait)</code></li>
	 *    <li><code>pm.get(someKnowsTrait)</code></li>
	 * </ul>   
	 * and let the overloading mechanism decide which you want.
	 * This resulted more convenient than declaring explicitly the three maps:
	 * <ul>
	 * <li><code>EnumMap<FloatTrait,Box> m1;</code> </li>
	 * <li><code>EnumMap<P2FloatTrait,Box> m2;</code> </li>
	 * <li><code>EnumMap<KnowsTrait,JCheckBox> m3;</code> </li>
	 * </ul>
	 *  and then calling:
	 *   <ul>
	 *    <li><code>m1.get(someFloatTrait)</code></li>
	 *    <li><code>m2.get(someP2FloatTrait)</code></li>
	 *    <li><code>m3.get(someKnowsTrait)</code></li>
	 *  </ul>
	 * Why is this more inconvenient? Because whenever I write this I have
	 * to remember: ohh m1 matches someFloatTrait, m2 matches someP2FloatTrait,
	 * and m3 matches someKnowsTrait
	 * </p>
	 * <p>
	 * With  PropertyMap<Box,Box,JCheckBox> pm; I just write
	 *   <blockquote>  <code>pm.get(anyTrait)</code> </blockquote>
	 * and can forget about the matchings. The compiler will
	 * remember them for me.
	 * </p>
	 * 
	 * */
	public static final class PropertyMap<ValueT,P2ValueT,KnowsValueT> {
		public ValueT get(TraitType tt,FloatTrait t){
			final ArrayList<ValueT> ts = ft.get(tt);
			if (t.getValuePosition()<ts.size())
				return ts.get(t.getValuePosition());
			else 
				return null;
		}
		public void set(TraitType tt,FloatTrait t,ValueT f){
			final ArrayList<ValueT> ts = ft.get(tt);
			// pad with zeros the undefined positions before the trait value. 
			for(int i=ts.size();i<=t.getValuePosition();i++) 
				ts.add(null);
			ts.set(t.getValuePosition(),f);
		}
		public void remove(TraitType tt,FloatTrait t){ 
			final ArrayList<ValueT> ts = ft.get(tt);
			if (t.getValuePosition()<ts.size())
				ts.set(t.getValuePosition(),null);	
		}

		public ValueT get(MoodTrait t){	return mt.get(t);	}
		public void set(MoodTrait t,ValueT f){ mt.put(t,f);	}
		public void remove(MoodTrait t){ mt.remove(t);	}

		public P2ValueT get(ExtraTrait t){	return eft.get(t);	}
		public void set(ExtraTrait t, P2ValueT f){ eft.put(t,f);	}

		public P2ValueT get(PTraitType tt,FloatTrait t){
			final ArrayList<P2ValueT> ts = pft.get(tt);
			if (t.getValuePosition()<ts.size())
				return ts.get(t.getValuePosition());
			else 
				return null;
		}
		public void set(PTraitType tt,FloatTrait t, P2ValueT f){
			final ArrayList<P2ValueT> ts = pft.get(tt);
			if (f==null) {
				if (t.getValuePosition()<ts.size()) 
					ts.set(t.getValuePosition(),null);
				return;
			}

			// pad with null the undefined positions before the trait value. 
			for(int i=ts.size();i<=t.getValuePosition();i++) 
				ts.add(null);
			ts.set(t.getValuePosition(),f);
		}

		private EnumMap<TraitType,ArrayList<ValueT>> ft = new EnumMap<TraitType,ArrayList<ValueT>>(TraitType.class);
		private EnumMap<MoodTrait,ValueT> mt = new EnumMap<MoodTrait,ValueT>(MoodTrait.class);
		private EnumMap<ExtraTrait,P2ValueT> eft = new EnumMap<ExtraTrait,P2ValueT>(ExtraTrait.class);
		private EnumMap<PTraitType,ArrayList<P2ValueT>> pft = new EnumMap<PTraitType,ArrayList<P2ValueT>>(PTraitType.class);

		public PropertyMap(){
			for(TraitType t:TraitType.values())
				ft.put(t,new ArrayList<ValueT>());
			for(PTraitType t:PTraitType.values())
				pft.put(t,new ArrayList<P2ValueT>());
		}

		@SuppressWarnings("unchecked")
		public PropertyMap<ValueT,P2ValueT,KnowsValueT> clone() {
			PropertyMap<ValueT,P2ValueT,KnowsValueT> newPM = new PropertyMap<ValueT,P2ValueT,KnowsValueT>();

			newPM.ft = ft.clone();
			for (TraitType tt: ft.keySet())
				newPM.ft.put(tt, (ArrayList<ValueT>)ft.get(tt).clone());

			for(TraitType t:TraitType.values())
				newPM.ft.get(t).addAll(ft.get(t));

			newPM.pft = pft.clone();
			for (PTraitType pt: pft.keySet())
				newPM.pft.put(pt, (ArrayList<P2ValueT>)pft.get(pt).clone());

			newPM.mt.putAll(mt);
			return newPM;
		}
	}

	private Map<Actor,Float> get(ExtraTrait t){ return pm.get(t); }
	private void set(ExtraTrait t,Map<Actor,Float> fs){ pm.set(t,fs);	}

	private PropertyMap<Float,Map<Actor,Float>,Set<Actor>> pm = new PropertyMap<Float,Map<Actor,Float>,Set<Actor>>(); 
	private EnumMap<PTraitType,Map<Prop,ArrayList<Float>>> propPTraits = new EnumMap<PTraitType, Map<Prop,ArrayList<Float>>>(PTraitType.class);
	private EnumMap<PTraitType,Map<Stage,ArrayList<Float>>> stagePTraits = new EnumMap<PTraitType, Map<Stage,ArrayList<Float>>>(PTraitType.class);

	public ArrayList<Sentence> plans = new ArrayList<Sentence>();

	/** FloatTrait values are initialized to this value. */ 
	public static final float DEFAULT_FLOATTRAITVALUE= 0.0f;
	/** P2FloatTrait values are initialized to this value. */
	public static final float DEFAULT_P2FLOATTRAITVALUE= 0.0f;

	//**********************************************************************
	/**
	 * Creates an actor with all its float traits initialized to 0, and
	 * completely ignorant of the world. 
	 * @param tLabel label for the new actor. 
	 */
	public Actor(String tLabel) {
		super(tLabel);

		active = true;
		female = true;
		human = !(tLabel.equals("Fate"));
		dontMoveMe = false;
		unconscious = false;
		targetStage = Word.zeroReference;
		location = Word.zeroReference;
		spyingOn = Word.zeroReference;

		occupiedUntil = 0;
		cBored = 0;
		howLongToSpy = 0;
		onThePhoneWith=0;

		for(ExtraTrait t:ExtraTraits)
			set(t,new TreeMap<Actor,Float>());
		for(PTraitType tt:PTraitType.values()){
			propPTraits.put(tt,new TreeMap<Prop, ArrayList<Float>>());
			stagePTraits.put(tt,new TreeMap<Stage, ArrayList<Float>>());
		}
	}
	//**********************************************************************
	public Actor clone(Deikto dk) {
		Actor newActor = (Actor)super.clone();

		newActor.pm = pm.clone();
		newActor.propPTraits = new EnumMap<PTraitType, Map<Prop,ArrayList<Float>>>(PTraitType.class);
		newActor.stagePTraits = new EnumMap<PTraitType, Map<Stage,ArrayList<Float>>>(PTraitType.class);
		for(ExtraTrait t:ExtraTraits)
			newActor.set(t,new TreeMap<Actor,Float>(get(t)));
		for(PTraitType tt:PTraitType.values()){
			for(FloatTrait t:dk.getActorTraits()) {
				Map<Actor,Float> m = pm.get(tt,t);
				if (m!=null)
					newActor.pm.set(tt,t,new TreeMap<Actor,Float>(m));
			}
			newActor.propPTraits.put(tt,new TreeMap<Prop, ArrayList<Float>>(propPTraits.get(tt)));
			for(Map.Entry<Prop, ArrayList<Float>> e:propPTraits.get(tt).entrySet()) {
				if (e.getValue()!=null)
					newActor.propPTraits.get(tt).put(e.getKey(),new ArrayList<Float>(e.getValue()));
			}
			newActor.stagePTraits.put(tt,new TreeMap<Stage, ArrayList<Float>>(stagePTraits.get(tt)));
			for(Map.Entry<Stage, ArrayList<Float>> e:stagePTraits.get(tt).entrySet()) {
				if (e.getValue()!=null)
					newActor.stagePTraits.get(tt).put(e.getKey(),new ArrayList<Float>(e.getValue()));
			}
		}

		newActor.plans = new ArrayList<Sentence>();
		newActor.dontKnowsMe = new TreeSet<Actor>(dontKnowsMe);
		newActor.texts = new ArrayList<String>(texts);

		return newActor;
	}
	//**********************************************************************
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
	/** Gets the bytes of the image encoded as JPG. */
	public byte[] getImageJPGBytes(Deikto dk) throws IOException {
		return Utils.getImageJPGBytes(dk.getImageFile(imageFilename),image);
	}
	/** 
	 * Sets an image as the image associated to this actor.
	 * If image is null there won't be an associated image.  
	 * */
	public void setImage(ScaledImage image){
		this.image = image;
		if (image==null)
			setImageName(null);
	}
	/** Sets an image in a file on disk as the image associated to this actor. 
	 * The image is assumed to exist on disk and have the given name. */
	public void setImageName(String filename){ 
		imageFilename = filename;
	}
	/** Tells if the image was modified since the actor was created. */
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
	 * @returns the name of the image file on disk associated to this actor, 
	 *           or null if there is no associated image or if it hasn't been
	 *           written yet.
	 * */
	public String getImageName(){ 
		return imageFilename;
	}
	//**********************************************************************	
	public boolean getActive() { return (active); }
	//***********************************************************************
	public void setActive(boolean tNewActive) {
		active = tNewActive;
		return;
	}
	//***********************************************************************
	public boolean getFemale() { return (female); }
	//***********************************************************************
	public void setFemale(boolean tNewFemale) {
		female = tNewFemale;
		return;
	}
	//***********************************************************************
	public boolean getDontMoveMe() { return (dontMoveMe); }
	//***********************************************************************
	public void setDontMoveMe(boolean tNewDontMoveMe) {
		dontMoveMe = tNewDontMoveMe;
		return;
	}
	//***********************************************************************
	public boolean getHuman() { return (human); }
	//***********************************************************************
	public void setHuman(boolean tNewHuman) {
		human = tNewHuman;
		return;
	}
	//***********************************************************************
	public boolean getAbleToAct() { return active && !unconscious; }
	//***********************************************************************
	public boolean getUnconscious() { return (unconscious); }
	//***********************************************************************
	public void setUnconscious(boolean tNewUnconscious) {
		unconscious = tNewUnconscious;
		return;
	}
	//***********************************************************************
	public int getLocation() { return location.getIndex();	}
	public Reference getLocationRef() { return location;	}
	//***********************************************************************
	public void setLocation(Stage tNewLocation) { location = tNewLocation.getReference();}
	public void setLocationRef(Reference r) { location = r;}
	//***********************************************************************
	public int getSpyingOn() { return (spyingOn.getIndex());	}
	public Reference getSpyingOnRef() { return spyingOn;	}
	//	***********************************************************************
	public void setSpyingOn(Actor tNewSpyingOn) {	spyingOn=tNewSpyingOn.getReference();}
	public void setSpyingOnRef(Reference r) {	spyingOn=r;}
	//***********************************************************************
	public int getTargetStage() {	return (targetStage.getIndex());	}
	public Reference getTargetStageRef() {	return targetStage;	}
	//***********************************************************************
	public void setTargetStage(Stage tNewTargetStage) {	targetStage = tNewTargetStage.getReference();}
	public void setTargetStageRef(Reference r) { targetStage = r; }
	//***********************************************************************
	public int getOccupiedUntil() { return (occupiedUntil); }
	//***********************************************************************
	public void setOccupiedUntil(int tNewOccupiedUntil) { occupiedUntil=tNewOccupiedUntil; return; }
	//***********************************************************************
	public int getOnThePhoneWith() { return (onThePhoneWith); }
	//***********************************************************************
	public void setOnThePhoneWith(int tNewOnThePhoneWith) { onThePhoneWith=tNewOnThePhoneWith; return; }
	//***********************************************************************
	public int getCBored() { return (cBored); }
	//***********************************************************************
	public void setCBored(int tNewCBored) { cBored = tNewCBored; return; }
	//***********************************************************************
	public int getHowLongToSpy() { return (howLongToSpy); }
	//***********************************************************************
	public void setHowLongToSpy(int tNewHowLongToSpy) {
		howLongToSpy = tNewHowLongToSpy;
		return;
	}
	//***********************************************************************
	public String getDescription() { return description; }
	//***********************************************************************
	public void setDescription(String description) {
		this.description = description;
	}
	
	public boolean getKnowsMe(Actor actor) {return !dontKnowsMe.contains(actor);}

	public void setKnowsMe(Actor actor, boolean newKnowsMe) {
		if (newKnowsMe) dontKnowsMe.remove(actor);
		else dontKnowsMe.add(actor);
	}

	//***********************************************************************
	public static final EnumSet<MoodTrait> MoodTraits = EnumSet.allOf(MoodTrait.class);
	public static final EnumSet<ExtraTrait> ExtraTraits = EnumSet.allOf(ExtraTrait.class);

	public static String traitName(TraitType tt,FloatTrait t){
		switch(tt){
		case Normal: return t.getLabel(); 
		case Accord: return "accord"+t.getLabel();
		default: return t.getLabel()+"Weight";
		}
	}
	public static String traitName(PTraitType tt,FloatTrait t){
		switch(tt){
		case Confidence: return "c"+t.getLabel(); 
		default: return "p"+t.getLabel();
		}
	}

	public static enum MoodTrait {
		Disgusted_Aroused,
		Sad_Happy,
		Fearful_Angry,
		Tired_Energetic // just that
	}

	public static enum TraitType {
		Normal,
		Weight,
		Accord
	}
	public static enum PTraitType {
		Perception,
		Confidence
	}

	public static enum ExtraTrait { 
		debt_Grace,
		stranger_Kin;
	}
}
