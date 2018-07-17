package com.storytron.uber;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.storytron.enginecommon.BgItemData;
import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.ScaledImage;
import com.storytron.enginecommon.SerializedImage;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.IterableFilter;
import com.storytron.uber.Actor.ExtraTrait;
import com.storytron.uber.Actor.PTraitType;
import com.storytron.uber.Role.Option;
import com.storytron.uber.deiktotrans.DeiktoLoader;
import com.storytron.uber.deiktotrans.DeiktoLoader.BadVersionException;
import com.storytron.uber.operator.CustomOperator;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;
import com.storytron.uber.operator.OperatorFactory;
import com.storytron.uber.operator.ParameterOperator;


/** 
  This class is the root of the world model.
  <p>
 A world model is a set of verbs, actors, stages, things and traits 
that describe the universe where stories evolve using Storyteller. 
The world model is implemented by the set of classes in the package 
{@link com.storytron.uber}.
<p>
 The world model also contains its own dictionary of operators. This
 dictionary contains many operators common to all the storyworlds, plus some
 specific operators that only apply to it (most likely comming from custom
 traits).
<p>
 This class enforces global limits on the model to prevent arbitrarily
 huge storyworlds. For that sake, most of the model parts that are 
 in low levels of the hierarchy, like {@link Role}s and {@link Option}s,
 are added and removed through methods in this class. The only exception
 when enforcing limits are the script nodes, which global counts must
 be updated by the editors that manipulate the scripts.
<p>
Deikto provides also some auxiliary functionality:
<ul>
    <li>loading the model from a stream
         <ul><li>
           {@link #Deikto(InputStream, File)} 
          </li></ul>
         A mechanism is provided to load old storyworld formats
         through {@link DeiktoLoader}.
    </li><li>saving the model to a stream
    	<ul><li>
           {@link #writeXML(OutputStream)}
         </li></ul> 
    </li><li>providing the path to resource images
    	<ul><li>
           {@link #getImageFile(String)}
        </li></ul> 
    </li><li>saving images associated to the model in a directory on disk
    	<ul><li>
           {@link #writeResources(File)}
        </li></ul> 
    </li><li>collects and provides background information about various parts of the model
          <ul><li>
                    {@link #getPeople(boolean)}
          </li><li> {@link #getPlaces(boolean)}
          </li><li> {@link #getThings(boolean)}
          </li></ul> 
    </li><li>traversals over {@link Script}s
          <ul><li>
           {@link #traverseScripts(com.storytron.uber.Deikto.ScriptTraverser)}
           </li><li> {@link #traverseScripts(com.storytron.uber.Script.NodeTraverser)}
          </li></ul> 
    </li><li>cloning the non-fixed parts of the model
          <ul><li>
           {@link #cloneWorldShareLanguage()})
          </li></ul> 
</li></ul>

<h4>First level of world models</h4>

Each model has some fixed sets:

<ul><li>
     a set of verbs ({@link #verbs})
    </li><li> a set of actor traits ({@link #getActorTraits()})
    </li><li> a set of stage traits ({@link #getStageTraits()})
    </li><li> a set of prop traits ({@link #getPropTraits()}) 
    </li><li> a set of actor text traits ({@link #getTextTraits(TraitType)})
    </li><li> a set of stage text traits ({@link #getTextTraits(TraitType)})
    </li><li> a set of prop text traits ({@link #getTextTraits(TraitType)}) 
</li></ul>

We say that these sets are fixed because they are not modified during execution of a story, 
neither any of their elements.
<p>
And each model has some non-fixed sets:

<ul><li>
              a set of {@link Actor}s ({@link #actors})
    </li><li> a set of {@link Stage}s ({@link #stages})
    </li><li> a set of {@link Prop}s ({@link #props}) 
</li></ul>
Elements are not added or deleted from these sets, but certain values of them can be modified 
during execution of a story.
<p>
All the elements on these sets have associated indexes, the indexes can be used for fast lookups. 
It is possible to insert elements at specific indexes or change the indexes if needed. The 
operations that involve removing or adding elements are implemented in Deikto.
<p>
Here are some of the methods you can expect to find in Deikto: 
<ul><li>
     {@link #addVerb(Verb)}
    </li><li> {@link #addVerb(Verb)}
    </li><li> {@link #removeVerb(Verb)}
    </li><li> {@link #removeAllVerbs()}
    </li><li> {@link #getIndexOf(Verb)}
    </li><li> {@link #getVerb(int)}
    </li><li> {@link #getVerbs()}
    </li><li> {@link #findVerb(String)} 
    </li><li> {@link #addRole(Verb, Role)}
    </li><li> {@link #addOption(Role, Option)}
    </li><li> {@link #deleteRole(Verb, int)}
    </li><li> {@link #deleteOption(Role, int)}
</li></ul>

<p>
And here are some trait operations:
<ul><li>
     {@link #addTrait(TraitType, FloatTrait)}
    </li><li> {@link #addTrait(TraitType, int, FloatTrait)}
    </li><li> {@link #removeTrait(TraitType, String)}
    </li><li> {@link #removeTrait(TraitType, FloatTrait)}
    </li><li> {@link #renameTrait(TraitType, FloatTrait, String)}
    </li><li> {@link #getTrait(TraitType, String)} 
	</li><li> {@link #addTextTrait(TraitType, TextTrait)}
    </li><li> {@link #addTextTrait(TraitType, int, TextTrait)}
    </li><li> {@link #removeTextTrait(TraitType, String)}
    </li><li> {@link #removeTextTrait(TraitType, TextTrait)}
    </li><li> {@link #renameTrait(TraitType, TextTrait, String)}
    </li><li> {@link #getTextTrait(TraitType, String)}
</li></ul>
Traits also have a rename operation because renaming a trait requires renaming some other 
elements associated to it.
*/
public final class Deikto implements Cloneable {
	/** Maximum size allowed for an array of bytes in a saved state. */
	private static final int STATE_BYTE_ARRAY_MAXIMUM_SIZE = 100000;

	public static final int MAXIMUM_FIELD_LENGTH = 40;
	public static final int MAXIMUM_TEXT_TRAIT_LENGTH = 100;
	public static final int MAXIMUM_INACTIVITY_TIMEOUT = 100;
	public static final int MINIMUM_INACTIVITY_TIMEOUT = 10;
	public static final int MAXIMUM_AMOUNT_OF_PARAMETERS = 15;
	public Limits limits = new Limits();
	private ArrayList<Actor> actors = new ArrayList<Actor>();
	private ArrayList<Stage> stages = new ArrayList<Stage>();
	private ArrayList<Prop> props = new ArrayList<Prop>();
	private ArrayList<Verb> verbs = new ArrayList<Verb>();
	private OperatorDictionary operators;
	private int inactivityTimeout=MINIMUM_INACTIVITY_TIMEOUT;
	private String copyright = "";
	/**
	 * The following three fields are used for editing the model
	 * and are expected to be null when the storyworld is loaded
	 * in the server.
	 * */
	public OperatorUsageGraph usageGraph;
	public HashMap<Role,ArrayList<Verb>> roleVerbs;
	public HashMap<Option,ArrayList<Role>> optionRoles;

	/** A type that helps organize trait types in collections. */
	public static enum TraitType {
		Actor,
		Prop,
		Stage
	}

	private EnumMap<TraitType,ArrayList<FloatTrait>> traits = new EnumMap<TraitType,ArrayList<FloatTrait>>(TraitType.class);
	private EnumMap<TraitType,ArrayList<TextTrait>> textTraits = new EnumMap<TraitType,ArrayList<TextTrait>>(TraitType.class);
	private ArrayList<Boolean> visibleRelationships = new ArrayList<Boolean>();
	private int propTraitCount = 0;
	private int stageTraitCount = 0;
	private int actorTraitCount = 0;
	private int propTextTraitCount = 0;
	private int stageTextTraitCount = 0;
	private int actorTextTraitCount = 0;
	public int roleCount=0;
	public int optionCount=0;
	private int scriptNodeCount=0;
	public ArrayList<Quantifier> quantifiers = new ArrayList<Quantifier>();
	public Category categories  = new Category(this);
	private Verb startingVerb;
	private Role.Link startingRole;
	private Role.Option startingOption;
	private File file; // needed for loading images
	/** 
	 * This version number helps differentiate different copies of a storyworld.
	 * <p>
	 * Whenever changes are made to a storyworld, the version number is incremented
	 * once (and only once) until the storyworld is saved. If after saving a storyworld,
	 * new changes are done, then the version number is incremented again.
	 * <p>
	 * The Swat.UndoManager class is responsible for updating the version number.
	 * This is convenient because the UndoManager keeps track of all the possible
	 * changes that can be done to a storyworld, and has knowledge to determine
	 * if modifications are changes being undone or new changes.  
	 * */
	public int version=0;
	private boolean relationshipsVisible = true;

	/**
	 * Builds a Deikto instance from a storyworld data saved somewhere else.
	 * The data to read can be in any version of the format, provided
	 * that {@link DeiktoLoader} knows how to transform it to the most
	 * up to date format.  
	 * @param isXML input stream to read storyworld data from.
	 * @param f file path used to locate images associated to the storyworld.
	 *          It is intended to hold the name of the storyworld file on disk, the 
	 *          images are expected to be in a directory next to it.
	 *          The name of the file is used by storyteller to identify if
	 *          saved stories belong to this storyworld.
	 * */
	public Deikto(InputStream isXML,File f,boolean permissive) throws BadVersionException, 
								ReadingException, LimitException, SAXException, IOException {
		this(f);

		readXML(isXML,permissive);
	}

	/** 
	 * Builds a Deikto instance. 
	 * @param f file path used to locate images associated to the storyworld.
	 *          It is intended to hold the name of the storyworld file on disk, the 
	 *          images are expected to be in a directory next to it.
	 *          The name of the file is used by storyteller to identify if
	 *          saved stories belong to this storyworld.
	 * */
	public Deikto(File f) throws LimitException {
		file = f;
		init();
	}

	/** Fills data into the storyworld from a given XML data stream. */
	public void readXML(InputStream isXML,boolean permissive) throws BadVersionException, 
										ReadingException, LimitException, SAXException, IOException {
		Document doc=DeiktoLoader.loadStoryworldXML(isXML);
		readDOMFirstPass(doc);
		readDOMSecondPass(doc,permissive);
	}
	
	/**
	 * Makes a copy of the world model, and shares the language
	 * dictionaries.  
	 * */	
	public Deikto cloneWorldShareLanguage(){
		try {
			Deikto copy = (Deikto)clone();
			copy.actors=new ArrayList<Actor>(actors);			
			for(int i=0;i<copy.actors.size();i++) {
				Actor origActor = actors.get(i);
				Actor actorClone = origActor.clone(this);
				copy.actors.set(i, actorClone);
			}

			copy.stages=new ArrayList<Stage>(stages);
			for(int i=0;i<copy.stages.size();i++) {
				Stage stageClone = stages.get(i).clone();
				copy.stages.set(i, stageClone);
			}

			copy.props=new ArrayList<Prop>(props);
			for(int i=0;i<copy.props.size();i++) {
				Prop propClone = props.get(i).clone();
				copy.props.set(i, propClone);
			}
			
			return copy;
		} catch (CloneNotSupportedException e){
			throw new Error(e.getMessage(),e.getCause());
		}		
	}
	/**
	 * Fixates the value of the perceived traits, so that they
	 * do not change when the real trait values change.
	 * This operation needs to be applied on the storyworld
	 * before running it on the engine.  
	 * */
	public void fixatePTraitValues(){
		for(Actor a:getActors()){
			for(FloatTrait t:getActorTraits())
				for(Actor b:getActors()){
					a.setP(t,b,a.getP(t, b));
					a.setC(t,b,a.getC(t, b));
				}
			for(FloatTrait t:getStageTraits())
				for(Stage s:getStages()){
					a.setP(t,s,a.getP(t, s));
					a.setC(t,s,a.getC(t, s));
				}
			for(FloatTrait t:getPropTraits())
				for(Prop p:getProps()){
					a.setP(t,p,a.getP(t, p));
					a.setC(t,p,a.getC(t, p));
				}
		}
	}
	
	public static final String[] predefinedActorTraits = {
		"Quiet_Chatty",
		"Cool_Volatile",
		};
	public static int ACTOR_COOL_VOLATIL = 1;

	private static final String[] predefinedActorTraitDescriptions = {
		"This is a BNumber which represents the Actor's propensity to" +
		" converse with others. A value of 1 indicates a chatterbox who" +
		" tells everybody everything, and a value of -1 represents the" +
		" ultimate in taciturnity. 0 is the average, and denotes an Actor" +
		" who keeps their mouths shut when they don't have anything" +
		" interesting to say, but not when they do. This Attribute is most" +
		" often used in Gossip.",
		
		"Cool_Volatile ranges from -1 to 1. Its only effect is to determine" +
		" the ease with which an Actor's Moods get excited. An Actor with" +
		" maximum Cool_Volatile will be consumed with anger at the slightest" +
		" provocation, or fearfully run for cover when an alarm clock goes" +
		" off. If he or she finds loose change in a vending machine, he or" +
		" she will proclaim it the most joyful day of his or her life, and" +
		" if he or she sees a dead squirrel on the roadside, he or she will" +
		" be sad enough to break down in tears. This Actor can get sexually" +
		" aroused by looking at a relatively attractive pair of bricks, while" +
		" a spec of dust in his or her partner's hair would prove a" +
		" disgusting turn-off. An Actor with -1 Cool_Volatile never has mood" +
		" swings: this Actor is a stoic whose Moods always remain locked at" +
		" zero. An Actor with 0 for this Attribute is just your average" +
		" guy/gal - laughs in parties, cries in funerals, and carries on" +
		" with his or her life. An Actor's Cool_Volatile is used as a" +
		" Modulator (see BNumber Arithmetic) for that Actor's Moods."
		};

	/**
	 * Initializes word collections with the predefined words.
	 * Loads predefined traits.
	 * */
	public boolean init()  throws LimitException {
		operators = new OperatorDictionary();
		
		for(TraitType tt:TraitType.values()) {
			traits.put(tt,new ArrayList<FloatTrait>());
			textTraits.put(tt,new ArrayList<TextTrait>());
		}
		for(int i=0;i<predefinedActorTraits.length;i++)
			createTrait(TraitType.Actor,predefinedActorTraits[i],false,predefinedActorTraitDescriptions[i]);
		int c=0;
		for(String q:Quantifier.predefinedQuantifierLabels)
			quantifiers.add(new Quantifier(q,c++));
		quantifiers.add(new Quantifier("how much?",-1));
		
		addProp(new Prop("nothing"));
		addStage(new Stage("nowhere"));
		return true;
	}
	
	public OperatorDictionary getOperatorDictionary(){
		return operators;
	}
	
	/** Creates a custom operator type. */
	public CustomOperator createCustomOperator(Operator.Type t,String name) {
		CustomOperator op = new CustomOperator(name,0);
		op.setDataType(t);
		op.setBody(new Script(Script.Type.OperatorBody,-1,op,OperatorFactory.getReturnedValueOperator(t,name),true));

		operators.addOperator(op);
		return op;
	}
	
	/** Deletes a custom operator. */
	public void deleteCustomOperator(final CustomOperator op){
		operators.removeOperator(op.getLabel());
		if (usageGraph!=null)
			usageGraph.remove(op);
		traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, com.storytron.uber.Script.Node n) {
				if (n.getOperator()==op)
					s.deleteNode(Deikto.this, n);
				return true;
			}
		});
	}

	/**
	 * <p>Creates a trait of type <code>tt</code> with name <code>traitLabel</code> 
	 * and inserts it at index <code>i</code> (the index is given wrt the trait list 
	 * containing the traits of the given type).
	 * Make sure that there does not exist a trait with the same name, and that
	 * it name is distinct from the name of an existing operator.</p>
	 * <p>This method creates the trait and the operators (see 
	 * {@link #addTrait(com.storytron.uber.Deikto.TraitType, int, FloatTrait)} for details)
	 * for getting and setting the trait.</p> 
	 * @param visible tells if the trait is visible or not.
	 * */
	public FloatTrait createTrait(TraitType tt,int i,String traitLabel,boolean visible,String description) throws LimitException {
		FloatTrait t = new FloatTrait(traitLabel,getIndexCount(tt),visible,description);
		addTrait(tt,i,t);
		return t;
	}
	/**
	 * Add a {@link FloatTrait} to the world. 
	 * This method creates the {@link Operator}s for getting and setting 
	 * the trait. All of the generated operators are created by 
	 * {@link OperatorFactory} class. 
	 * Make sure that there does not exist a trait with the same 
	 * name, even if it is of different type.</p>
	 * <p>
	 *  The following operators are common to all traits:
	 *  <ul>
	 *  <li> TraitName: returns the trait value for a given element.
	 *  </li><li> SetTraitName: Sets the value of a trait for a given element.
	 *  </li><li> PTraitName: returns the perceived trait value of one actor
	 *                        for another.
	 *  </li><li> CTraitName: returns the confidence in the perceived trait value 
	 *                        of one actor for another.
	 *  </li><li> SetPTraitName: Sets the perceived trait value of one actor
	 *                        for another.
	 *  </li><li> SetCTraitName: Sets the confidence in the perceived trait value 
	 *                        of one actor for another.
	 *  </li><li> TraitNameWeight: returns the trait weight value for a given actor.
	 *  </li><li> PTraitNameWeight: returns the perceived trait weight value 
	 *            from one actor towards another.
	 *  </li>
	 * </ul>  
	 * <p> 
	 * @param i the index where to insert the Trait in the collection for 
	 *          traits of type tt. 
	 * */
	public void addTrait(TraitType tt,int i,FloatTrait t) throws LimitException {
		if (limits.maximumTraitCount<=traits.get(tt).size())
			throw new LimitException(LimitException.Type.Traits,t.getLabel(),limits.maximumTraitCount);
			
		traits.get(tt).add(i,t);

		if (tt==TraitType.Actor){
			operators.addOperator(OperatorFactory.createActorTraitOperator(Actor.TraitType.Normal,t));
			operators.addOperator(OperatorFactory.createActorTraitOperator(Actor.TraitType.Weight,t));
			operators.addOperator(OperatorFactory.createActorGetPWeightTraitOperator(t));
		} else 
			operators.addOperator(OperatorFactory.createGetTraitOperator(tt,t));
		
		operators.addOperator(OperatorFactory.createSetTraitOperator(tt,t));
		operators.addOperator(OperatorFactory.createGetPTraitOperator(Actor.PTraitType.Perception,tt,t));
		operators.addOperator(OperatorFactory.createGetPTraitOperator(Actor.PTraitType.Confidence,tt,t));
		operators.addOperator(OperatorFactory.createSetPTraitOperator(Actor.PTraitType.Perception,tt,t));
		operators.addOperator(OperatorFactory.createSetPTraitOperator(Actor.PTraitType.Confidence,tt,t));
	}

	/**
	 * Convenient method for creating a text trait at the end of the trait set
	 * for the given type (see {@link #createTextTrait(com.storytron.uber.TraitType, int, String)}).
	 * */
	public TextTrait createTextTrait(TraitType tt,String traitLabel,String description) throws LimitException {
		return createTextTrait(tt,getTextTraitCount(tt),traitLabel,description);
	}

	/**
	 * Creates a text trait of type <code>tt</code> with name <code>traitLabel</code> 
	 * and inserts it at index <code>i</code> (the index is given wrt the trait list 
	 * containing the traits of the given type).
	 * Make sure that there does not exist a trait with the same name, and that
	 * it name is distinct from the name of an existing operator.</p>
	 * <p>
	 * This method creates the trait and the operator for getting its value (see 
	 * {@link #addTextTrait(com.storytron.uber.Deikto.TraitType, int, TextTrait)} for details).
	 * */
	public TextTrait createTextTrait(TraitType tt,int i,String traitLabel,String description) throws LimitException {
		TextTrait t = new TextTrait(traitLabel,getTextIndexCount(tt),description);
		addTextTrait(tt,i,t);
		return t;
	}

	/**
	 * Add a {@link TextTrait} to the world. 
	 * This method creates the {@link Operator} for getting the trait value. 
	 */	
	public void addTextTrait(TraitType tt,int i,TextTrait t) throws LimitException {
		if (limits.maximumTextTraitCount<=textTraits.get(tt).size())
			throw new LimitException(LimitException.Type.Traits,t.getLabel(),limits.maximumTextTraitCount);
			
		textTraits.get(tt).add(i,t);
		operators.addOperator(OperatorFactory.createGetTextTraitOperator(tt,t));
	}

	private static void operatorNames(TraitType tt,FloatTrait t,LinkedList<String> names){
		operatorNames(tt, t, t.getLabel(),names);
	}
	private static void operatorNames(TraitType tt,FloatTrait t,String name, LinkedList<String> names){
		operatorNamesGet(tt,t,name,names);
		operatorNamesSet(tt,t,name,names);
	}
	/** Returns GET operator names for the given trait. */
	public static void operatorNamesGet(TraitType tt,FloatTrait t, LinkedList<String> names){
		operatorNamesGet(tt,t,t.getLabel(),names);
	}
	private static void operatorNamesGet(TraitType tt,FloatTrait t,String name, LinkedList<String> names){
		names.add(name);
		names.add("P"+name);
		names.add("C"+name);
		if (tt==TraitType.Actor && !t.isVisible()){
			names.add(name+"Weight");
			names.add("P"+name+"Weight");
		}
	}
	public static void operatorNamesSet(TraitType tt,FloatTrait t, LinkedList<String> names){
		operatorNamesSet(tt,t,t.getLabel(),names);
	}
	private static void operatorNamesSet(TraitType tt,FloatTrait t,String name, LinkedList<String> names){
		names.add("Set"+name);
		names.add("SetP"+name);
		names.add("SetC"+name);
	}
	/**
	 * <p>Rename an existing trait. 
	 * Takes care of renaming associated operators too.</p> 
	 * */
	public void renameTrait(TraitType tt,FloatTrait t,String newName) {		
		LinkedList<String> oldNames = new LinkedList<String>(); 
		operatorNames(tt, t, oldNames);
		LinkedList<String> newNames = new LinkedList<String>();
		operatorNames(tt, t, newName, newNames);
		Iterator<String> newNameIt = newNames.iterator();
		for(String oldName:oldNames)
			operators.renameOperator(oldName,newNameIt.next());		
		t.setLabel(newName);
	}
	/** Changes visibility of a given trait. */
	public void changeTraitVisibility(final FloatTrait t){
		t.setVisible(!t.isVisible());
	}
	
	/**
	 * <p>Rename an existing text trait. 
	 * Takes care of renaming the associated operators too.</p> 
	 * */
	public void renameTextTrait(TraitType tt,TextTrait t,String newName) {		
		operators.renameOperator(t.getLabel(),newName);		
		t.setLabel(newName);
	}

	/**
	 * <p>
	 * Convenient method for creating a trait at the end of the trait set
	 * for the given type (see {@link #createTrait(com.storytron.uber.TraitType, int, String)}).
	 * </p> 
	 * @param visible tells if the trait is visible or not.
	 * */
	public FloatTrait createTrait(TraitType tt,String traitLabel,boolean visible,String description) throws LimitException {
		return createTrait(tt,getTraitCount(tt),traitLabel,visible,description);
	}
	/**
	 * <p>
	 * Convenient method for adding a trait at the end of the trait set
	 * for the given type (see {@link #addTrait(com.storytron.uber.TraitType, int, FloatTrait)}).
	 * </p> 
	 * */
	public void addTrait(TraitType tt,FloatTrait t) throws LimitException {
		addTrait(tt,getTraitCount(tt),t);
	}
	/**
	 * Convenient method for generating positions for referencing
	 * trait values. This count is different from the size of
	 * trait collections when you start deleting and later adding traits.
	 * */
	private int getIndexCount(TraitType tt){
			switch(tt){
			case Prop: return propTraitCount++;
			case Stage: return stageTraitCount++;
			case Actor: return actorTraitCount++;
			default: return -1;
			}
	}

	/**
	 * Convenient method for generating positions for referencing
	 * text trait values. This count is different from the size of
	 * trait collections when you start deleting and later adding traits.
	 * */
	private int getTextIndexCount(TraitType tt){
			switch(tt){
			case Prop: return propTextTraitCount++;
			case Stage: return stageTextTraitCount++;
			case Actor: return actorTextTraitCount++;
			default: return -1;
			}
	}

	/**
	 * A method for enable reordering of traits. Is not very efficient, but it
	 * is called sparsely. 
	 * */
	public void moveTrait(TraitType tt,int from,int to) {
		traits.get(tt).add(to,traits.get(tt).remove(from));
		{
			OperatorDictionary.Menu m = OperatorDictionary.Menu.valueOf("Set"+tt.name());
			int setFrom=operators.getOperatorMenuIndex(m, "Set"+traits.get(tt).get(from).getLabel());
			int setTo=operators.getOperatorMenuIndex(m, "Set"+traits.get(tt).get(to).getLabel());
			operators.moveOperatorMenu(m,setFrom,setTo);
		}
		if (tt==TraitType.Actor){
			{
				OperatorDictionary.Menu m = OperatorDictionary.Menu.valueOf("Set"+tt.name()+"P");
				int setFrom=operators.getOperatorMenuIndex(m, "Set"+traits.get(tt).get(from).getLabel());
				int setTo=operators.getOperatorMenuIndex(m, "Set"+traits.get(tt).get(to).getLabel());
				operators.moveOperatorMenu(m,setFrom,setTo);
			}
			{
				OperatorDictionary.Menu m = OperatorDictionary.Menu.valueOf("Set"+tt.name()+"C");
				int setFrom=operators.getOperatorMenuIndex(m, "Set"+traits.get(tt).get(from).getLabel());
				int setTo=operators.getOperatorMenuIndex(m, "Set"+traits.get(tt).get(to).getLabel());
				operators.moveOperatorMenu(m,setFrom,setTo);
			}
		}

		OperatorDictionary.Menu m = OperatorDictionary.Menu.valueOf(tt.name());
		int getFrom=operators.getOperatorMenuIndex(m, traits.get(tt).get(from).getLabel());
		int getTo=operators.getOperatorMenuIndex(m, traits.get(tt).get(to).getLabel());
		operators.moveOperatorMenu(m,getFrom,getTo);
	}
	/**
	 * <p>Removes a trait of type <code>tt</code> with the given <code>label</code>.
	 * This method also removes the word and the operators
	 * associated to the trait.</p>
	 * */
	public void removeTrait(TraitType tt,String label){
		removeTrait(tt,getTrait(tt,label));
	}
	/**
	 * <p>Removes a trait <code>t</code> of type <code>tt</code>.
	 * This method also removes the word and the operators
	 * associated to the trait.</p>
	 * */
	public void removeTrait(TraitType tt,final FloatTrait t) {
		final LinkedList<String> setnames = new LinkedList<String>();
		operatorNamesSet(tt, t, setnames);
		for(Verb v:verbs){
			for(String name:setnames)
				deleteConsequence(v,name);
		}
		final LinkedList<String> names = new LinkedList<String>();
		operatorNamesGet(tt, t, names);
		traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Script.Node n) {
				if (names.contains(n.getOperator().getLabel())	||
					 n.getConstant()==t
				 	)
					s.deleteNode(Deikto.this,n);
				return true;
			}
		});
		names.addAll(setnames);
		for(String n:names)
			operators.removeOperator(n);

		traits.get(tt).remove(t);
	}
	/**
	 * <p>Removes a text trait of type <code>tt</code> with the given <code>label</code>.
	 * This method also removes the operator associated to the trait.</p>
	 * */
	public void removeTextTrait(TraitType tt,String label){
		removeTextTrait(tt,getTextTrait(tt,label));
	}
	/**
	 * <p>Removes a text trait <code>t</code> of type <code>tt</code>.
	 * This method also removes the operator associated to the trait.</p>
	 * */
	public void removeTextTrait(TraitType tt,final TextTrait t) {
		traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Script.Node n) {
				if (t.getLabel().contains(n.getOperator().getLabel()))
					s.deleteNode(Deikto.this,n);
				return true;
			}
		});
		operators.removeOperator(t.getLabel());

		textTraits.get(tt).remove(t);
	}
	
	/**
	 * Checks if a trait, actor, stage, prop, quantifier or operator exists with a given name.
	 * */
	public String nameExists(String name){
		name = name.trim();

		if (getActorIndex(name)!=-1)
			return "An Actor "+name+" is already defined.";

		if (getStageIndex(name)!=-1)
			return "A Stage "+name+" is already defined.";

		if (getPropIndex(name)!=-1)
			return "A Prop "+name+" is already defined.";

		if (findQuantifier(name)!=-1)
			return "An actor "+name+" is already defined.";


		for(Deikto.TraitType tt:Deikto.TraitType.values()) {
			for(FloatTrait t:getTraits(tt)) {
				if (t.getLabel().equals(name)) 
					return "A Trait \""+name+"\" is already defined as "+Utils.a_an(tt.name())+" "+tt.name()+" Trait.";
			}
			for(TextTrait t:getTextTraits(tt)) {
				if (t.getLabel().equals(name)) 
					return "A Text Trait \""+name+"\" is already defined as "+Utils.a_an(tt.name())+" "+tt.name()+" Text Trait.";
			}
		}
		// Check all the operators
		for(Operator op:getOperatorDictionary())
			if (op.getLabel().equals(name)) {
				// Build the operator description.
				String opStr=null;
				if (op.getCArguments()>0) {
					opStr=name+"("+op.getArgumentDataType(0);
					for(int i=1;i<op.getCArguments();i++) opStr+=","+op.getArgumentDataType(i);
					opStr+=")";
				}
				
				return "An operator with name "+name+" already exists."
						+(opStr==null?" It has no arguments":
							" It looks like "+opStr)
						+(op.getToolTipText().length()==0?".":
							" and its description is: \""+op.getToolTipText()+"\"")
				       +" So it's not possible to use this name.";
			}

		return null;
	}

	/** @return true iff relationships are visible in storyteller. */
	public boolean areRelationshipsVisible() {
		return relationshipsVisible;
	}

	/** Tells if relationships are visible in storyteller or not. */
	public void setRelationshipsVisible(boolean areVisible) {
		relationshipsVisible = areVisible;
	}

	/** 
	 * Determines if a trait must be visible in the relationship browser. 
	 * Only makes sense to call for actor traits. 
	 * */
	public void setRelationshipVisible(FloatTrait t,boolean visible){
		if (visible) {
			if (t.getValuePosition()<visibleRelationships.size())
				visibleRelationships.set(t.getValuePosition(),true);
		} else {
			for(int i=visibleRelationships.size()-1;i<t.getValuePosition();i++)
				visibleRelationships.add(true);
			visibleRelationships.set(t.getValuePosition(),false);	
		}
	}

	/** 
	 * Tells if a trait is visible in the relationship browser. 
	 * Only makes sense to call for actor traits. 
	 * */
	public boolean isRelationshipVisible(FloatTrait t){
		return t.getValuePosition()>=visibleRelationships.size() || visibleRelationships.get(t.getValuePosition()); 
	}
	
	/** Specifies the copyright text. */
	public void setCopyright(String text){
		copyright = text;
	}
	
	/** @return the copyright text. */
	public String getCopyright(){
		return copyright;
	}
//**********************************************************************
	public Verb getStartingVerb() {
		return startingVerb;
	}
//**********************************************************************
	public void setStartingVerb(Verb newStartingVerb) {
		startingVerb = newStartingVerb;
	}
//**********************************************************************
	public Role.Link getStartingRole() {
		return startingRole;
	}
//**********************************************************************
	public void setStartingRole(Role.Link newStartingRole) {
		startingRole = newStartingRole;
	}
//**********************************************************************
	public Role.Option getStartingOption() {
		return startingOption;
	}
//**********************************************************************
	public void setStartingOption(Role.Option newStartingOption) {
		startingOption = newStartingOption;
	}
	
	/** 
	 * Methods for manipulating the inactivity time out for this storyworld.
	 * <p>
	 * The inactivity timeout specifies how much should the engine wait before 
	 * terminating the story when nothing interesting is happening.
	 * */
	public int getInactivityTimeout(){ return inactivityTimeout; }
	public void setInactivityTimeout(int timeout) throws LimitException {
		if (timeout>MAXIMUM_INACTIVITY_TIMEOUT || timeout<MINIMUM_INACTIVITY_TIMEOUT)
			throw new LimitException(LimitException.Type.InactivityTimeout,String.valueOf(timeout),100);
		inactivityTimeout=timeout; 
	}
	
	private String checkNameLength(String name) throws LimitException {
		if (name.length()>MAXIMUM_FIELD_LENGTH)
			throw new LimitException(LimitException.Type.Name,name,MAXIMUM_FIELD_LENGTH);
		return name;
	}

	private String checkTextTraitLength(String name) throws LimitException {
		if (name.length()>MAXIMUM_TEXT_TRAIT_LENGTH)
			throw new LimitException(LimitException.Type.Name,name,MAXIMUM_TEXT_TRAIT_LENGTH);
		return name;
	}


	/** 
	 * @return the first child with a given tag name, or null if
	 * no such child exists.
	 * */
	private static Node getChildByTagName(Node parent,String tagName){
		NodeList nl = parent.getChildNodes();
		for (int i=0;i<nl.getLength();i++) {
			Node c = nl.item(i);
			if (c.getNodeName().equals(tagName))
				return c;
		}
		return null;
	}
	
	private void readDOMFirstPass(Document doc) throws LimitException {
		String tag;
		int zcWords = 0;
	
		Node dic = getChildByTagName(doc,"dictionary");
		if (dic.getAttributes().getNamedItem("version")!=null)
			version = Integer.parseInt(dic.getAttributes().getNamedItem("version").getNodeValue());
		if (dic.getAttributes().getNamedItem("inactivityTimeout")!=null)
			setInactivityTimeout(Integer.parseInt(dic.getAttributes().getNamedItem("inactivityTimeout").getNodeValue()));
		
		// read copyright
		Node cr = getChildByTagName(dic,"copyright");
		if (cr!=null)
			setCopyright(getValue(cr));

		// reading prop traits
		NodeList propTraitsList = getChildByTagName(dic,"propTraits").getChildNodes();
		for(int i=0;i<propTraitsList.getLength();i++) {
			Node traitNode = propTraitsList.item(i);
			if (traitNode.getAttributes()!=null)
				createTrait(TraitType.Prop,checkNameLength(traitNode.getAttributes().getNamedItem("Label").getNodeValue()),true,traitNode.getTextContent());
		}

		// reading prop text traits
		Node propTextTraits = getChildByTagName(dic,"propTextTraits");
		if (propTextTraits!=null){
			NodeList propTextTraitsList = propTextTraits.getChildNodes();
			for(int i=0;i<propTextTraitsList.getLength();i++) {
				Node traitNode = propTextTraitsList.item(i);
				if (traitNode.getAttributes()!=null)
					createTextTrait(TraitType.Prop,checkTextTraitLength(traitNode.getAttributes().getNamedItem("Label").getNodeValue()),traitNode.getTextContent());
			}
		}

		// reading stage traits
		// Do not attempt to read non-existent stage trait items.
		// This fixes an error that occurs on the server.
		Node stageTraitsNode = getChildByTagName(dic,"stageTraits");
		if (stageTraitsNode!=null) {
			NodeList stageTraitsList = stageTraitsNode.getChildNodes();
			for(int i=0;i<stageTraitsList.getLength();i++) {
				Node traitNode = stageTraitsList.item(i);
				if (traitNode.getAttributes()!=null)
					createTrait(TraitType.Stage,checkNameLength(traitNode.getAttributes().getNamedItem("Label").getNodeValue()),true,traitNode.getTextContent());
			}
		}
		
		// reading stage text traits
		Node stageTextTraits = getChildByTagName(dic,"stageTextTraits");
		if (stageTextTraits!=null){
			NodeList stageTextTraitsList = stageTextTraits.getChildNodes();
			for(int i=0;i<stageTextTraitsList.getLength();i++) {
				Node traitNode = stageTextTraitsList.item(i);
				if (traitNode.getAttributes()!=null)
					createTextTrait(TraitType.Stage,checkTextTraitLength(traitNode.getAttributes().getNamedItem("Label").getNodeValue()),traitNode.getTextContent());
			}
		}
		
		// reading outer traits
		Node outerTraits = getChildByTagName(dic,"outerTraits");
		if (outerTraits.getAttributes().getNamedItem("rvisible")!=null &&
				outerTraits.getAttributes().getNamedItem("rvisible").getNodeValue().equals("false"))
			setRelationshipsVisible(false);
		NodeList outerTraitsList = outerTraits.getChildNodes();
		for(int i=0;i<outerTraitsList.getLength();i++) {
			Node traitNode = outerTraitsList.item(i);
			if (traitNode.getAttributes()!=null) {
				String label = outerTraitsList.item(i).getAttributes().getNamedItem("Label").getNodeValue();
				FloatTrait t;
				if (!Utils.contains(predefinedActorTraits,label))
					t = createTrait(TraitType.Actor,checkNameLength(label),
							outerTraitsList.item(i).getAttributes().getNamedItem("visible")==null ||
							outerTraitsList.item(i).getAttributes().getNamedItem("visible").getNodeValue().equals("true"),traitNode.getTextContent());
				else
					t = getTrait(TraitType.Actor,label);
				if (outerTraitsList.item(i).getAttributes().getNamedItem("rvisible")!=null &&
					outerTraitsList.item(i).getAttributes().getNamedItem("rvisible").getNodeValue().equals("false")
					)
					setRelationshipVisible(t, false);
			}
		}

		// reading actor text traits
		Node actorTextTraits = getChildByTagName(dic,"actorTextTraits");
		if (actorTextTraits!=null){
			NodeList actorTextTraitsList = actorTextTraits.getChildNodes();
			for(int i=0;i<actorTextTraitsList.getLength();i++) {
				Node traitNode = actorTextTraitsList.item(i);
				if (traitNode.getAttributes()!=null)
					createTextTrait(TraitType.Actor,checkTextTraitLength(traitNode.getAttributes().getNamedItem("Label").getNodeValue()),traitNode.getTextContent());
			}
		}

		// Generate actor objects
		NodeList actorList = getChildByTagName(dic,"actorSet").getChildNodes();
		zcWords = 0;
		for (int i=0; i<actorList.getLength(); i++) {
			final Node current = actorList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {
				if ("actor".equals(tag)) {
					++zcWords;
					addActor(new Actor(checkNameLength(getLabel(current, "Label"))));
//					System.out.println(label);
				}
			}
		}
		System.out.println(zcWords + " Actors read");
		
		// Generate prop objects
		NodeList propList = getChildByTagName(dic,"propSet").getChildNodes();
		zcWords = 0;
		for (int i=0; i<propList.getLength(); i++) {
			final Node current = propList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {
				if ("prop".equals(tag)) {
					++zcWords;
					addProp(new Prop(checkNameLength(getLabel(current, "Label"))));
				}
			}
		}
		System.out.println(zcWords + " props read");
		
		// Generate stage objects
		NodeList stageList = getChildByTagName(dic,"stageSet").getChildNodes();
		zcWords = 0;
		for (int i=0; i<stageList.getLength(); i++) {
			final Node current = stageList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {
				if ("stage".equals(tag)) {
					++zcWords;
					addStage(new Stage(checkNameLength(getLabel(current, "Label"))));

//					System.out.println("stage "+ label + " read");
				}
			}
		}
		System.out.println(zcWords + " stages read");
		
		
		// *** Begin reading custom operator set ***
		Node customOperatorSetNode = getChildByTagName(dic,"customOperatorSet");
		if (customOperatorSetNode!=null){
			NodeList operatorList = customOperatorSetNode.getChildNodes();
			for (int i=0; i<operatorList.getLength(); i++) {
				final Node current = operatorList.item(i);
				if (current.getNodeType() == Element.ELEMENT_NODE && "customOperator".equals(current.getNodeName())) {
					final Operator.Type t = Operator.Type.valueOf(getLabel(current, "Type"));
					final CustomOperator op = createCustomOperator(t, checkNameLength(getLabel(current, "Label")));

					final NodeList opList = current.getChildNodes();
					for(int j=0;j<opList.getLength();j++) {
						Node operatorChild = opList.item(j);
						if (operatorChild.getNodeType() == Element.ELEMENT_NODE) {
							if ("parameter".equals(operatorChild.getNodeName())) {
								String argName = checkNameLength(getLabel(operatorChild,"Label"));
								op.addArgument(Operator.Type.valueOf(getLabel(operatorChild, "Type")),argName,"");
								if (op.getCArguments()>Deikto.MAXIMUM_AMOUNT_OF_PARAMETERS)
									throw new LimitException(LimitException.Type.CustomOperator,"Too many parameters for operator "+op.getLabel(),Deikto.MAXIMUM_AMOUNT_OF_PARAMETERS);
							} else if ("description".equals(operatorChild.getNodeName())) {
								op.setToolTipText(operatorChild.getTextContent());
							}
						}
					}
				}
			}
		}
		// *** End reading custom operator set ***

		
		Node categorySetNode = getChildByTagName(dic,"categorySet"); 
		if (categorySetNode!=null) {
			NodeList categoryList = categorySetNode.getChildNodes();
			generateVerbsInCategories(categoryList);
			
		} else {		
			// Generate verb objects
			
			NodeList verbList = getChildByTagName(dic,"verbSet").getChildNodes();
			generateVerbs(verbList);

			System.out.println(zcWords + " verbs read");
		}
	}

	public static int nextElement(NodeList attributeList,int k){
		if (k<attributeList.getLength()) {
			k++;
			while (k<attributeList.getLength() && attributeList.item(k).getNodeType()!=Node.ELEMENT_NODE) k++;
		}
		return k;
	}
	public static int nextElement(String name,NodeList attributeList,int k){
		while (k<attributeList.getLength()) {
			k++;
			while (k<attributeList.getLength() && attributeList.item(k).getNodeType()!=Node.ELEMENT_NODE) k++;
					
			if (k<attributeList.getLength()) {
				String label=getLabel(attributeList.item(k), "Name");
				if (!label.equals(name))
					System.out.println("WARNING: found attribute "+label+
							" when searching for attribute "+name);
				else return k;
			}
		}
		return k;
	}
	public static int nextElementTag(String name,NodeList p2List,int k){
		while (k<p2List.getLength()) {
			k++;
			while (k<p2List.getLength() && p2List.item(k).getNodeType()!=Node.ELEMENT_NODE) k++;
					
			if (k<p2List.getLength()) {
				String label=p2List.item(k).getNodeName();
				if (!label.equals(name))
					System.out.println("WARNING: found attribute "+label+
							" when searching for attribute "+name);
				else return k;
			}
		}
		return k;
	}

//**********************************************************************	
	private void readDOMSecondPass(Document doc,boolean permissive) throws ReadingException, LimitException {
		String tag, label;
		int zcWords = 0;
		Node dic = getChildByTagName(doc,"dictionary");
		
		// *** Begin reading actorSet ***
		NodeList actorList = getChildByTagName(dic,"actorSet").getChildNodes();
		NodeList actorChildren = null;
		NodeList attributeList = null;
		// [change-ld] Replaced attributeNode with "current" 
		
		zcWords = 0;
		for (int i=0; i<actorList.getLength(); i++) {
			final Node current = actorList.item(i);
			tag = current.getNodeName();
			
			// Skip text nodes
			if (current.getNodeType() == Element.ELEMENT_NODE) {
				if ("actor".equals(tag)) {
					

					++zcWords;
					label = getLabel(current, "Label");
					Actor actor = (Actor)actors.get(findActor(label));
					
					// ** read in the attributes **
					
					// Get the list of child elements under "actor".  
					// Only "attributes" is currently under "actor"
					actorChildren = current.getChildNodes();
					
					for ( int j=0; j < actorChildren.getLength(); ++j) {
						if (actorChildren.item(j).getNodeName() == "attributes") {
							final Node currentAttributes = actorChildren.item(j);
					
							// TODO: raise an exception if there is no attribute
							
							// Get the list of elements under "attributes"
							// Only the "attribute" elements are currently under "attributes" 
							attributeList = currentAttributes.getChildNodes();
							int k=-1;						
							actor.setDescription(Utils.emptyIfNull(getValue(attributeList.item(k=nextElement("description",attributeList, k)))));
							Node child=attributeList.item(k=nextElement(attributeList, k));
							if ("image".equals(getLabel(child, "Name"))){
								actor.setImageName(getValue(child));
								child = attributeList.item(k=nextElement("active",attributeList, k));
							}							
							actor.setActive(getBool(getValue(child)));
							actor.setFemale(getBool(getValue(attributeList.item(k=nextElement("female",attributeList, k)))));
							actor.setDontMoveMe(getBool(getValue(attributeList.item(k=nextElement("dontMoveMe",attributeList, k)))));
							actor.setUnconscious(getBool(getValue(attributeList.item(k=nextElement("unconscious",attributeList, k)))));
							actor.setLocation(getStage(findStage(getValue(attributeList.item(k=nextElement("location",attributeList, k))))));
							actor.setSpyingOn(getActor(findActor(getValue(attributeList.item(k=nextElement("spyingOn",attributeList, k))))));
							actor.setTargetStage(getStage(findStage(getValue(attributeList.item(k=nextElement("targetStage",attributeList, k))))));
							actor.setOccupiedUntil(getInt(getValue(attributeList.item(k=nextElement("occupiedUntil",attributeList, k)))));
							actor.setCBored(getInt(getValue(attributeList.item(k=nextElement("cBored",attributeList, k)))));
							actor.setHowLongToSpy(getInt(getValue(attributeList.item(k=nextElement("howLongToSpy",attributeList, k)))));

							child=attributeList.item(k=nextElement(attributeList, k));
							for(Actor.MoodTrait t:Actor.MoodTraits){
								if (child==null) break;
								if (t.name().equals(getLabel(child, "Name"))) {
									actor.set(t,getFloat(getValue(child), getLabel(child, "Name")));
									child = attributeList.item(k=nextElement(attributeList, k));
								}
							}
							for(Actor.TraitType tt:Actor.TraitType.values()){
								for(FloatTrait t:getActorTraits()){
									if (child==null) break;
									if (Actor.traitName(tt, t).equals(getLabel(child, "Name"))) {
										actor.set(tt,t,getFloat(getValue(child), getLabel(child, "Name")));
										child = attributeList.item(k=nextElement(attributeList, k));
									}
								}
							}
							for(TextTrait t:getTextTraits(TraitType.Actor)){
								if (child==null) break;
								if (t.getLabel().equals(getLabel(child, "Name"))) {
									actor.setText(t,getValue(child));
									child = attributeList.item(k=nextElement(attributeList, k));
								}
							}
							if (k<attributeList.getLength())
								throw new ReadingException(ReadingException.Type.WordDescriptionTraitDoesNotExist,attributeList.item(k).getNodeName(),actor.getLabel(),child==null?null:getLabel(child, "Name"));
						} else if (actorChildren.item(j).getNodeName() == "knowsMe") {
							NodeList propGrandChildren = actorChildren.item(j).getChildNodes();
							int iActor=-1;
							int k=0;
							for (;k<propGrandChildren.getLength() && iActor<getActorCount(); ++k) {
								Node c = propGrandChildren.item(k);
								if (c.getNodeType() == Element.ELEMENT_NODE &&
										c.getNodeName().equals("ofWhom")) {						
									label = getLabel(c, "Label");
									do {iActor++;}
									while(iActor<getActorCount() && !getActor(iActor).getLabel().equals(label));
									if (iActor>=getActorCount()) break;

									actor.setKnowsMe(getActor(iActor), getBool(getValue(c)));
								}
							}
							if (k<propGrandChildren.getLength())
								System.out.println("WARNING: reading actor "+actor.getLabel()+" knowsMe: do not know how to read child "+propGrandChildren.item(k).getNodeName());
						}
					}
				}
			}
		}
		// *** End reading actorSet ***
		
		
		// *** Begin reading pValueSet ***
		// [change-ld] Added Kin, Up2XXX and KnowsXXX values to the pValueSet 
		NodeList pValueList = getChildByTagName(dic,"pValueSet").getChildNodes();
		NodeList aboutWhomList = null;
		NodeList p2List = null;
				
		int iWho=-1;
		for (int i=0; i<pValueList.getLength(); i++) {
			final Node current = pValueList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {
				if ("pValues".equals(tag)) {
					iWho++;
					label = getLabel(current, "OfWhom");
					Actor who = getActor(iWho);
					if (!label.equals(who.getLabel())){
						System.out.println("WARNING: in pValueSet: found actor "+label+" when expecting actor "+who.getLabel());
						continue;
					}
					aboutWhomList = current.getChildNodes();
					int iAboutWhom=-1;
					int iAboutWhat=-1;
					int iAboutWhere=-1;
					
					int j=0;
					for (;j<aboutWhomList.getLength() && iAboutWhom<getActorCount(); ++j) {
						// [change-ld] Replaced "pValueList" with "aboutWhomList" in the next line, for bugfix
						Node currentAboutWhom = aboutWhomList.item(j);
						tag = currentAboutWhom.getNodeName();
						
						if (currentAboutWhom.getNodeType() == Element.ELEMENT_NODE
							&& "AboutWhom".equals(tag)) {
							label = getLabel(currentAboutWhom, "Label");
							do { iAboutWhom++; }
							while(iWho==iAboutWhom || iAboutWhom<getActorCount() && !label.equals(getActor(iAboutWhom).getLabel()));
							if (iAboutWhom>=getActorCount()) break;
							
							Actor secondPerson = getActor(iAboutWhom);
							p2List = currentAboutWhom.getChildNodes();
							
							int k=-1;
							Node child = p2List.item(k=nextElement(p2List, k));
							for(PTraitType tt:Actor.PTraitType.values()) {
								if (child==null) break;
								for(FloatTrait t:getActorTraits()){
									if (child==null) break;
									if (Actor.traitName(tt, t).equals(getLabel(child, "Name"))) {
										who.set(tt,t,secondPerson, getFloat(getValue(child), Actor.traitName(tt, t)));
										child = p2List.item(k=nextElement(p2List, k));
									}
								}
							}
							for(ExtraTrait t:Actor.ExtraTraits)	{
								if (child==null) break;
								if (t.name().equals(getLabel(child, "Name"))) {
									who.set(t,secondPerson, getFloat(getValue(child), t.name()));
									child = p2List.item(k=nextElement(p2List, k));
								}
							}
							if (k<p2List.getLength())
								throw new ReadingException(ReadingException.Type.WordDescriptionPTraitDoesNotExist,child.getNodeName()+" "+getLabel(child, "Name"),who.getLabel(),secondPerson.getLabel());
						} else if (currentAboutWhom.getNodeType() == Element.ELEMENT_NODE
								&& "AboutWhat".equals(tag)) {
							
							label = getLabel(currentAboutWhom, "Label");
							do { iAboutWhat++; }
							while(iAboutWhat<getPropCount() && !label.equals(getProp(iAboutWhat).getLabel()));
							if (iAboutWhat>=getPropCount()) break;
							
							Prop secondProp = getProp(iAboutWhat);
							p2List = currentAboutWhom.getChildNodes();
							
							int k=-1;
							Node child = p2List.item(k=nextElement(p2List, k));
							for(PTraitType tt:Actor.PTraitType.values()) {
								if (child==null) break;
								for(FloatTrait t:getPropTraits()){
									if (child==null) break;
									if (Actor.traitName(tt, t).equals(getLabel(child, "Name"))) {
										who.set(tt,t,secondProp, getFloat(getValue(child), Actor.traitName(tt, t)));
										child = p2List.item(k=nextElement(p2List, k));
									}
								}
							}
							if (k<p2List.getLength())
								System.out.println("WARNING: reading actor pPropTrait "+who.getLabel()+" about "+secondProp.getLabel()+": do not know how to read child "+child.getNodeName());
						} else if (currentAboutWhom.getNodeType() == Element.ELEMENT_NODE
								&& "AboutWhere".equals(tag)) {
							
							label = getLabel(currentAboutWhom, "Label");
							do { iAboutWhere++; }
							while(iAboutWhere<getStageCount() && !label.equals(getStage(iAboutWhere).getLabel()));
							if (iAboutWhere>=getStageCount()) break;
							
							Stage secondStage = getStage(iAboutWhere);
							p2List = currentAboutWhom.getChildNodes();
							
							int k=-1;
							Node child = p2List.item(k=nextElement(p2List, k));
							for(PTraitType tt:Actor.PTraitType.values()) {
								if (child==null) break;
								for(FloatTrait t:getStageTraits()){
									if (child==null) break;
									if (Actor.traitName(tt, t).equals(getLabel(child, "Name"))) {
										who.set(tt,t,secondStage, getFloat(getValue(child), Actor.traitName(tt, t)));
										child = p2List.item(k=nextElement(p2List, k));
									}
								}
							}
							if (k<p2List.getLength())
								System.out.println("WARNING: reading actor pStageTrait "+who.getLabel()+" about "+secondStage.getLabel()+": do not know how to read child "+child.getNodeName());
						}
					}
					if (j<aboutWhomList.getLength())
						System.out.println("WARNING: reading actor p2trait "+who.getLabel()+": do not know how to read child "+aboutWhomList.item(j).getNodeName());
				}
			}
		}
		// *** End reading pValueSet
		
		// *** Begin reading propSet
		
		NodeList propList = getChildByTagName(dic,"propSet").getChildNodes();
		
		int iProp=0; // skip prop nothing 
		for (int i=0; i<propList.getLength(); i++) {
			final Node current = propList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {
				if ("prop".equals(tag)) {
					label = getLabel(current, "Label");
					
					Prop prop = getProp(++iProp);
					if (!label.equals(prop.getLabel())){
						System.out.println("WARNING: in propSet: found prop "+label+" when expecting prop "+prop.getLabel());
						continue;
					}
					
					NodeList propChildren = current.getChildNodes();
					int k=-1;							
					prop.setDescription(Utils.emptyIfNull(getValue(propChildren.item(k=nextElementTag("description",propChildren, k)))));
					Node child=propChildren.item(k=nextElement(propChildren, k));
					if ("image".equals(child.getNodeName())){
						prop.setImageName(getValue(child));
						child = propChildren.item(k=nextElementTag("carried",propChildren, k));
					}							
					prop.setCarried(getBool(getValue(child)));
					prop.setVisible(getBool(getValue(propChildren.item(k=nextElementTag("visible",propChildren, k)))));
					prop.setInPlay(getBool(getValue(propChildren.item(k=nextElementTag("inPlay",propChildren, k)))));
					prop.setOwner(getActor(findActor(getValue(propChildren.item(k=nextElementTag("owner",propChildren, k))))));
					prop.setLocation(getStage(findStage(getValue(propChildren.item(k=nextElementTag("location",propChildren, k))))));

					child = propChildren.item(k=nextElement(propChildren, k));
					if (child!=null && "knowsMe".equals(child.getNodeName())){
						NodeList propGrandChildren = child.getChildNodes();
						int iActor=-1;
						int j=0;
						for (;j<propGrandChildren.getLength() && iActor<getActorCount(); ++j) {
							Node c = propGrandChildren.item(j);
							if (c.getNodeType() == Element.ELEMENT_NODE &&
								c.getNodeName().equals("ofWhom")) {						
								label = getLabel(c, "Label");
								do {iActor++;}
								while(iActor<getActorCount() && !getActor(iActor).getLabel().equals(label));
								if (iActor>=getActorCount()) break;
									
								Actor a=getActor(iActor);
								prop.setKnowsMe(a, getBool(getValue(c)));
							}
						}
						if (j<propGrandChildren.getLength())
							System.out.println("WARNING: reading prop "+prop.getLabel()+" knowsMe: do not know how to read child "+propGrandChildren.item(j).getNodeName());
						child = propChildren.item(k=nextElement(propChildren, k));
					}
					while(k<propChildren.getLength()){
						Node c = propChildren.item(k);
						if (c.getNodeType() == Element.ELEMENT_NODE) {
							String name=getLabel(c, "Name");
							FloatTrait t=getTrait(TraitType.Prop,name,false);
							if (t!=null) prop.setTrait(t,getFloat(getValue(c), name));
							else break;
						}
						k++;
					}
					while(k<propChildren.getLength()){
						Node c = propChildren.item(k);
						if (c.getNodeType() == Element.ELEMENT_NODE) {
							String name=getLabel(c, "Name");
							TextTrait t=getTextTrait(TraitType.Prop,name,false);
							if (t!=null) prop.setText(t,getValue(c));
							else System.out.println("WARNING: reading prop "+prop.getLabel()+" custom trait: do not know how to read child "+name);
						}
						k++;
					}
				}
			}
		}		
		// *** End reading propSet

		// *** Begin reading stageSet ***
		NodeList stageList = getChildByTagName(dic,"stageSet").getChildNodes();

		int iStage=0;
		for (int i=0; i<stageList.getLength(); i++) {
			final Node current = stageList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {
				if ("stage".equals(tag)) {
					label = getLabel(current, "Label");
					Stage stage = getStage(++iStage);
					if (!label.equals(stage.getLabel())){
						System.out.println("WARNING: in stageSet: found stage "+label+" when expecting stage "+stage.getLabel());
						continue;
					}
					
					NodeList stageChildren = current.getChildNodes();
					int k=-1;							
					stage.setDescription(Utils.emptyIfNull(getValue(stageChildren.item(k=nextElementTag("description",stageChildren, k)))));
					Node child=stageChildren.item(k=nextElement(stageChildren, k));
					if ("image".equals(child.getNodeName())){
						stage.setImageName(getValue(child));
						child = stageChildren.item(k=nextElementTag("doorOpen",stageChildren, k));
					}							
					stage.setDoorOpen(getBool(getValue(child)));
					stage.setPopulation(getInt(getValue(stageChildren.item(k=nextElementTag("population",stageChildren, k)))));
					stage.setOwner(getActor(findActor(getValue(stageChildren.item(k=nextElementTag("owner",stageChildren, k))))));
					stage.setXCoord(getFloat(getValue(stageChildren.item(k=nextElementTag("xCoord",stageChildren, k))),"xCoord"));
					stage.setYCoord(getFloat(getValue(stageChildren.item(k=nextElementTag("yCoord",stageChildren, k))),"yCoord"));

					child = stageChildren.item(k=nextElement(stageChildren, k));
					if (child!=null && "unwelcoming_Homey".equals(child.getNodeName())){
						NodeList stageGrandChildren = child.getChildNodes();
						int iActor=-1;
						int j=0;
						for (;j<stageGrandChildren.getLength() && iActor<getActorCount(); ++j) {
							Node c = stageGrandChildren.item(j);
							if (c.getNodeType() == Element.ELEMENT_NODE &&
								c.getNodeName().equals("ofWhom")) {						
								label = getLabel(c, "Label");
								do {iActor++;}
								while(iActor<getActorCount() && !getActor(iActor).getLabel().equals(label));
								if (iActor>=getActorCount()) break;
								
								Actor a=getActor(iActor);
								stage.setUnwelcoming_Homey(a, getFloat(getValue(c),a.getLabel()));
							}
						}
						if (j<stageGrandChildren.getLength())
							System.out.println("WARNING: reading stage "+stage.getLabel()+" unwelcoming_Homey: do not know how to read child "+stageGrandChildren.item(j).getNodeName());
						child = stageChildren.item(k=nextElement(stageChildren, k));
					}
					if (child!=null && "knowsMe".equals(child.getNodeName())){
						NodeList stageGrandChildren = child.getChildNodes();
						int iActor=-1;
						int j=0;
						for (; j<stageGrandChildren.getLength() && iActor<getActorCount(); ++j) {
							Node c = stageGrandChildren.item(j);
							if (c.getNodeType() == Element.ELEMENT_NODE &&
								c.getNodeName().equals("ofWhom")) {						
								label = getLabel(c, "Label");
								do {iActor++;}
								while(iActor<getActorCount() && !getActor(iActor).getLabel().equals(label));
								if (iActor>=getActorCount()) break;
								
								Actor a=getActor(iActor);
								stage.setKnowsMe(a, getBool(getValue(c)));
							}
						}
						if (j<stageGrandChildren.getLength())
							System.out.println("WARNING: reading stage "+stage.getLabel()+" knowsMe: do not know how to read child "+stageGrandChildren.item(j).getNodeName());
						child = stageChildren.item(k=nextElement(stageChildren, k));
					}
					while(k<stageChildren.getLength()){
						Node c = stageChildren.item(k);
						if (c.getNodeType() == Element.ELEMENT_NODE) {
							String name=getLabel(c, "Name");
							FloatTrait t=getTrait(TraitType.Stage,name,false);
							if (t!=null) stage.setTrait(t,getFloat(getValue(c), name));
							else break;
						}
						k++;
					}
					while(k<stageChildren.getLength()){
						Node c = stageChildren.item(k);
						if (c.getNodeType() == Element.ELEMENT_NODE) {
							String name=getLabel(c, "Name");
							TextTrait t=getTextTrait(TraitType.Stage,name,false);
							if (t!=null) stage.setText(t,getValue(c));
							else System.out.println("WARNING: reading stage "+stage.getLabel()+" custom trait: do not know how to read child "+name);
						}
						k++;
					}
				}
			}
		}		
		// *** End reading stageset ***

		// *** Begin reading custom operator set ***
		Node customOperatorSetNode = getChildByTagName(dic,"customOperatorSet");
		if (customOperatorSetNode!=null){
			NodeList operatorList = customOperatorSetNode.getChildNodes();
			for (int i=0; i<operatorList.getLength(); i++) {
				final Node current = operatorList.item(i);
				if (current.getNodeType() == Element.ELEMENT_NODE && "customOperator".equals(current.getNodeName())) {
					final CustomOperator op = (CustomOperator)operators.getOperator(getLabel(current, "Label"));

					final NodeList opList = current.getChildNodes();
					for(int j=0;j<opList.getLength();j++) {
						Node operatorChild = opList.item(j);
						if (operatorChild.getNodeType() == Element.ELEMENT_NODE) {
							if ("body".equals(operatorChild.getNodeName()))
								op.setBody(buildScriptDOM(Script.Type.OperatorBody,operatorChild, op,-1,null,-1,null,permissive));
						}
					}
				}
			}
		}
		// *** End reading custom operator set ***
		
		ArrayList<Option> options = readOptions(getChildByTagName(dic,"optionSet"),permissive);
		ArrayList<Role> roles = readRoles(getChildByTagName(dic,"roleSet"),options,permissive);

		// *** Begin reading categorySet or verbSet***
		Node categorySetNode = getChildByTagName(dic,"categorySet"); 
		if (categorySetNode != null) {
			NodeList categoryList = categorySetNode.getChildNodes();
			readCategories(categories, categoryList, roles, permissive);
		} else {

			readVerbs(getChildByTagName(dic,"verbSet").getChildNodes(), null,roles,permissive);
		}
		// *** End reading categorySet or verbset ***

		System.gc();
		// Set starting things.
		
		startingVerb = null;
		startingRole = null;
		startingOption = null;
		
		Node thisNode = getChildByTagName(dic,"startingVerb");
		if (thisNode!=null){
			int iVerb = findVerb(thisNode.getAttributes().getNamedItem("Label").getNodeValue());
			startingVerb = getVerb(iVerb==-1?0:iVerb);
		
			thisNode = getChildByTagName(dic,"startingRole");
			if (thisNode!=null){
				startingRole = startingVerb.getRole(thisNode.getAttributes().getNamedItem("Label").getNodeValue());
			
				thisNode = getChildByTagName(dic,"startingOption");
				if (startingRole!=null && thisNode!=null){
					startingOption = startingRole.getRole().getOption(thisNode.getAttributes().getNamedItem("Label").getNodeValue());
				}
			}
		}
		
	}
	
	private ArrayList<Option> readOptions(Node n,boolean permissive) throws ReadingException, LimitException {
		ArrayList<Option> options = new ArrayList<Option>();
		NodeList children = n.getChildNodes();
		for(int i=0;i<children.getLength();i++) {
			Node optionElem = children.item(i);
			if ("option".equals(optionElem.getNodeName())) {
				Option option = new Option(getVerb(findVerb(getLabel(optionElem,"Label"))),false);
				NodeList optionChildren = optionElem.getChildNodes();
				for (int l=0; l<optionChildren.getLength(); ++l) {
					Node current = optionChildren.item(l);
					String name = current.getNodeName();												
					if (current.getNodeType() == Element.ELEMENT_NODE) {

						// this line could be Inclination, or socketSpecs
						if ("acceptable".equals(name))
							setAcceptableScript(option,buildScriptDOM(Script.Type.OptionAcceptable,current, Sentence.MaxWordSockets, null, -1, option,permissive));
						else if ("desirable".equals(name))
							setDesirableScript(option,buildScriptDOM(Script.Type.OptionDesirable,current, Sentence.MaxWordSockets, null, -1, option,permissive)); 
						else if ("socketSpecs".equals(name)) { // this had better be true
							int partIndex = getInt(getLabel(current,"Index"));
							option.initWordSocket(partIndex,false);
							setAcceptableScript(option.getWordSocket(partIndex),buildScriptDOM(Script.Type.Acceptable,current.getChildNodes().item(1), partIndex, null, -1, option,permissive));
							setDesirableScript(option.getWordSocket(partIndex),buildScriptDOM(Script.Type.Desirable,current.getChildNodes().item(3), partIndex, null, -1, option,permissive));
						}
					}
				}
				options.add(option);
			}
		}
		return options;
	}
	
	private ArrayList<Role> readRoles(Node n,ArrayList<Option> options,boolean permissive) throws ReadingException, LimitException {
		ArrayList<Role> roles = new ArrayList<Role>();
		NodeList children = n.getChildNodes();
		for(int i=0;i<children.getLength();i++) {
			Node roleElem = children.item(i);
			if ("role".equals(roleElem.getNodeName())) {
				Role role = new Role(false);
				NodeList roleChildren = roleElem.getChildNodes();
				for (int k=0; k<roleChildren.getLength(); ++k) {
					Node current = roleChildren.item(k);
					if (current.getNodeType() == Element.ELEMENT_NODE) {

						String name = current.getNodeName();
						if ("script".equals(name)) { 
							Script script = buildScriptDOM(Script.Type.Emotion,current, -1,null,i,null,permissive);
							addEmotion(role,script);
						} else if ("assumeRoleIf".equals(name)) {
							Script script = buildScriptDOM(Script.Type.AssumeRoleIf,current, -1,null,i,null,permissive);
							setAssumeRoleIf(role,script);
						}
						if ("optionlink".equals(name)) {
							int iOption = getInt(getLabel(current,"Index"));
							addOption(role,options.get(iOption));
						}
					}
				}
				roles.add(role);
			}
		}
		return roles;
	}

	/** 
	 * Performs checks on the storyworld scripts and returns the list of found errors.
	 * <p>
	 * @param rejectUndefinedNodes tells is undefined operators should be rejected. 
	 * @param usageGraph uses usage graph to perform checks. If it is null, the graph
	 *                   in the public field usageGraph is used instead, and if that
	 *                   field is null then a new graph is derived. 
	 * @param keepUsageGraph if true keeps the usageGraph in the public
	 *                       field usageGraph.
	 * */
	public LinkedList<LogIssue> checkScripts(OperatorUsageGraph usageGraph,boolean keepUsageGraph){
		if (usageGraph==null) {
			if (this.usageGraph==null) {
				usageGraph = new OperatorUsageGraph();
				for(Operator op:operators.getOperators(OperatorDictionary.Menu.Custom))
					usageGraph.add((CustomOperator)op);
			} else
				usageGraph=this.usageGraph;
		}
		if (keepUsageGraph)
			this.usageGraph=usageGraph;

		final LinkedList<LogIssue> dictionaryIssueList = new LinkedList<LogIssue>();
		
		List<CustomOperator> cycle = usageGraph.getCycle();
		ListIterator<CustomOperator> ops = cycle.listIterator(cycle.size());
		if (ops.hasPrevious()) {
			CustomOperator op = ops.previous();
			StringBuilder msg = new StringBuilder();
			// if an operator calls itself it will be detected with normal sniffing.
			if (ops.hasPrevious()) {
				CustomOperator firstCall = ops.previous();
				if (ops.hasPrevious())
					msg.append("Custom operator " + op.getLabel() 
								+ " executes itself through this sequence of calls: "
								+ firstCall.getLabel());
				else
					msg.append("Custom operator " + op.getLabel() 
								+ " executes itself through "+firstCall.getLabel());
				
				while(ops.hasPrevious()) {
					msg.append(", ");
					msg.append(ops.previous());
				}
				dictionaryIssueList.add(new LogIssue(new ScriptPath(null,null,null),op.getBody(),msg.toString()));
			}
		}
		
		traverseScripts(new ScriptTraverser(){
				public void traversing(Verb verb, Role.Link role, Option option, Script s) {
					ScriptPath sp = new ScriptPath(verb,role,option);
					String result = sp.sniff(s);
					if (result!=null)
						dictionaryIssueList.add(new LogIssue(sp,s,result));
					else if (ScriptPath.containsUndefinedOperators(s))
						dictionaryIssueList.add(new LogIssue(sp,s,"Contains undefined nodes."));
				}
		});
		return dictionaryIssueList;
	}
	
	private void readCategories(Category parent, NodeList categoryList, ArrayList<Role> roles, boolean permissive)
													throws ReadingException, LimitException {
		Node current;		
		String tag, label;
		Category currentCategory;
		for (int i=0; i<categoryList.getLength(); i++) {
			current = categoryList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {	
				if ("category".equals(tag)) {
					// create the category
					label = getLabel(current, "Label");
					currentCategory = new Category(label, parent);
					parent.addChild(currentCategory);
					readCategories(currentCategory, current.getChildNodes(), roles, permissive);
				} else if ("verb".equals(tag)) {
					// There are verb elements, so read them all at once here
					readVerbs(categoryList, parent.name,roles,permissive);
					break;
				}
			}
		}
	}
	
	private void generateVerbsInCategories(NodeList categoryList) throws LimitException {
		Node current;		
		String tag;

		for (int i=0; i<categoryList.getLength(); i++) {
			current = categoryList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {	
				if ("category".equals(tag)) {
					// process the child categories
					generateVerbsInCategories(current.getChildNodes());
				} else if ("verb".equals(tag)) {
					// There are verb elements, so read them all at once here
					generateVerbs(categoryList);
					break;
				}
			}
		}
		
	}
	
	// Generate verb objects.
	// This should be called from firstReadDOM()
	private void generateVerbs(NodeList verbList) throws LimitException {
		// Generate verb objects
		Node current;
		int zcWords;
		String tag, label;
		zcWords = 0;
		for (int i=0; i<verbList.getLength(); i++) {
			current = verbList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {
				if ("verb".equals(tag)) {
					++zcWords;
					label = checkNameLength(getLabel(current, "Label"));
					addVerb(new Verb(label));
	
//					System.out.println("verb "+ label + " read");
				}				
			}
		}
		System.out.println(zcWords + " verbs read");
	}
	
	private void readVerbs(NodeList verbList,String categoryName,ArrayList<Role> roles,boolean permissive) 
					throws ReadingException, LimitException {
		NodeList verbChildren = null;
		Node current;
		String tag, name;

		for (int i=0; i<verbList.getLength(); i++) {
			current = verbList.item(i);
			tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE) {
				if ("verb".equals(tag)) {
					final Verb verb = getVerb(findVerb(getLabel(current, "Label")));
					final String verbWitnesses = getLabel(current, "Witnesses");
					if (verbWitnesses.length()>0)
						verb.setWitnesses(Verb.Witnesses.valueOf(verbWitnesses));
					
					verbChildren = current.getChildNodes();

					for (int j=0; j<verbChildren.getLength(); ++j) {
						current = verbChildren.item(j);
						name = current.getNodeName();
						if (current.getNodeType() == Element.ELEMENT_NODE) {
	
							if ("description".equals(name))
								verb.setDescription(Utils.emptyIfNull(getValue(current)));
							else if ("hijackable".equals(name))
								verb.setHijackable(getBool(getValue(current)));
							else if ("occupiesDirObject".equals(name))
								verb.setOccupiesDirObject(getBool(getValue(current)));
							else if ("timeToPrepare".equals(name))
								verb.setTimeToPrepare(getInt(getValue(current)));
							else if ("timeToExecute".equals(name))
								verb.setTimeToExecute(getInt(getValue(current)));
							else if ("category".equals(name)) {
								// Use the "category" field in Dictionary.xml if reading from verbSet
								if (categoryName == null) {
									verb.setCategory(getValue(current));
									
									// add the category if it doesn't yet exist
									if (categories.findChild(getValue(current)) == null)
										categories.addChild(new Category(getValue(current), categories ));
								} else {
									// Verb is embedded in categorySet
									verb.setCategory(categoryName);
								}
							}
							else if ("expression".equals(name))
								verb.setExpression(getValue(current));
							else if ("trivial_Momentous".equals(name))
								verb.setTrivial_Momentous(getFloat(getValue(current), name));							
							else if ("socket".equals(name)) {
								int index = getInt(getLabel(current, "Index"));
								String type = getLabel(current, "Type");
								final Verb.WSData wsData = new Verb.WSData(Sentence.getTypeFromLabel(type));
								String visible = getLabel(current, "Visible");
								if (visible.length()>0)
									wsData.visible = getBool(visible);
								String witness = getLabel(current, "Witness");
								if (witness.length()>0)
									wsData.witness = getBool(witness);
								
								String suffix = getLabel(current, "Suffix");
								if (suffix.length()!=0)
									wsData.suffix = verb.defaultSuffixScript(index,suffix);
								wsData.note = Utils.nullifyIfEmpty(getLabel(current, "Notes"));
								String presence = getLabel(current, "Presence");
								if (presence.length()!=0)
									wsData.presence = Verb.Presence.valueOf(presence);
								
								NodeList socketChildren = current.getChildNodes();
								for (int k=0; k<socketChildren.getLength(); ++k) {
									Node currentSocketChild = socketChildren.item(k); 
									if (currentSocketChild.getNodeType() == Element.ELEMENT_NODE) {
										 if ("script".equals(currentSocketChild.getNodeName())) {
											 wsData.text = buildScriptDOM(Script.Type.WordsocketLabel,currentSocketChild, index, verb, -1, null,permissive);
											 break;
										 } else if ("suffix".equals(currentSocketChild.getNodeName()))
											 wsData.suffix = buildScriptDOM(Script.Type.WordsocketSuffix,currentSocketChild, index, verb, -1, null,permissive);
									}
								}
								if (wsData.text==null)
									wsData.text = verb.defaultWordsocketTextScript(index);
								if (wsData.suffix==null)
									wsData.suffix = verb.defaultSuffixScript(index,"");
								
								verb.setWSData(index, wsData);
							}
							else if ("abort".equals(name)) // is there a consequence script?
								setAbortScript(verb,buildScriptDOM(Script.Type.AbortIf,current, -1, verb, -1, null,permissive)); 
							else if ("script".equals(name)) // is there a consequence script?
								addConsequence(verb,buildScriptDOM(Script.Type.Consequence,current, -1, verb, -1, null,permissive)); 
							else if ("rolelink".equals(name)) {
								int iRole = getInt(getLabel(current,"Index"));
								addRole(verb,new Role.Link(roles.get(iRole),getLabel(current,"Label")));
							}
						}
					}
				}
			}
		}			
	}
	
	// Get the attribute specfied by "Name" from the node "current"
	private static String getLabel(Node current, String Name) {
		// CC 3/6/07 added this if-statement to deal with a dictionary change.
		// This if-statement can be stripped out after 4/1/07.
		if (current.getAttributes().getNamedItem(Name)!=null)
			return current.getAttributes().getNamedItem(Name).getNodeValue();
		else
			return "";
	}
	
	// Get the enclosed from the node "current", as a string (i.e. <E>value</E>)
	private String getValue(Node current) {
		if (current.hasChildNodes())
			return current.getChildNodes().item(0).getNodeValue();
		else
			return null;
	}
	
	// Get the boolean value of a string (i.e. "true" returns true)
	private Boolean getBool(String value) {
		if (value.toLowerCase().trim().equals("true"))
			return true;
		else
			return false;
	}
	
	// Get the float value of a string
	private Float getFloat(String value, String source) throws LimitException {
		if ((value.equals("NaN")) | (value.contains("Infinity"))) {
			throw new LimitException(LimitException.Type.BNumber,"Value "+value+" of "+source+" is not between -1.0 and 1.0.",0);
		}
		float x = new Float(value);
		if ((x<=-1.0) | (x>=1.0))
			throw new LimitException(LimitException.Type.BNumber,"Value "+value+" of "+source+" is not between -1.0 and 1.0.",0);
		return x;
	}
	
	// Get the integer value of a string
	private Integer getInt(String value) {
		return new Integer(value);
	}
	
//***********************************************************************
	/** A class for storing log messages. */
	public static class LogIssue {	
		public String result;
		public ScriptPath sp;
		public Script s;
		public LogIssue(ScriptPath sp,Script s,String result) { 
			this.result=result;
			this.s = s;
			this.sp = sp;
		}
		public LogIssue(String result) { 
			this.result=result;
		}

		@Override
		public String toString(){ 
			if (s!=null)
				return " Problem in Script {"+sp.getPath(s)+"}.  "+result; 
			else
				return result; 
		}
	}
	
//***********************************************************************
	private Script buildScriptDOM(Script.Type st,Node scriptNode, int tiWordSocket, Verb verb, int iRole, Role.Option option,boolean permissive)
		throws LimitException, ReadingException
	{
		return buildScriptDOM(st,scriptNode, null, tiWordSocket, verb, iRole, option, permissive);
	}
	private Script buildScriptDOM(Script.Type st,Node scriptNode, CustomOperator op, int tiWordSocket, Verb verb, int iRole, Role.Option option,boolean permissive)
		throws ReadingException, LimitException
	{
		Node current;
		NodeList children = scriptNode.getChildNodes();
		int i=0;
		while(i<children.getLength() && children.item(i).getNodeType() != Element.ELEMENT_NODE)
			i++;
		
		// CC 01/22/07: added this next line to insure that all Script owners are properly initialized
		Operator rootOp;
		if (op!=null) {
			rootOp = op.getBody().getRoot().getOperator();
		} else {
			rootOp = operators.getOperator(getLabel(children.item(i), "Label"));
			if (rootOp==null) {
				String path = (verb!=null?verb.getCategory()+": "+verb.getLabel()+": ":"")+(iRole>=0?"role "+iRole+": ":"")+(option!=null?option.getLabel()+": ":"")+getLabel(children.item(i), "Label");
				throw new ReadingException(ReadingException.Type.OperatorDoesNotExist,getLabel(children.item(i), "Label"),path);
			}
		}
		
		Script script = new Script(st,tiWordSocket,op,rootOp, false);
		
		// Add the first-order tokens to the script, using recursion to add the embedded tokens
		while(i<children.getLength()) {
			current = children.item(i);
			if (current.getNodeType() == Element.ELEMENT_NODE)
				addTokenToScript(script,current, true, permissive);
			i++;
		}
		// This is a compatibility test made on July 12, 2007, remove it after a while.
		// add the note for the script
		Script.Node t=(Script.Node)script.getRoot().getFirstChild();
		if (t.getDescription().length()==0){
			String notes=getLabel(scriptNode, "Notes");
			if (notes!=null) t.setDescription(notes.trim());
		}
		
		return (script);
	}	
	
	
	/** 
	 * Use recursion to add tokens, and the sub-tokens that they contain, to the script.
	 * @return the number of added nodes
	 * */
	private void addTokenToScript(Script script, Node tokenNode, Boolean isRoot, boolean permissive) 
					throws ReadingException, LimitException {

		if (!permissive && limits.maximumScriptNodeCount<=scriptNodeCount)
			throw new LimitException(LimitException.Type.Nodes,"",limits.maximumScriptNodeCount);

		NodeList children;
		Node child;
		String label, value;
		value = getLabel(tokenNode, "Arg");
		Operator op;
		if (getLabel(tokenNode, "Parameter").equals("true")) {
			int i=Integer.parseInt(value);
			label = script.getCustomOperator().getArgumentLabel(i);
			op = script.getCustomOperator().getParameterOperator(i);
		} else {
			label = getLabel(tokenNode, "Label");
			if ("Returned value".equals(label)) {
				op = script.getCustomOperator().getBody().getRoot().getOperator();
			} else
				op = operators.getOperator(label);
		}

		// Get description 
		children = tokenNode.getChildNodes();
		String desc="";
		for (int i=0; i<children.getLength(); ++i) 
			if (children.item(i).getNodeName().equals("Description")){
				desc=children.item(i).getTextContent();
				break;
			}
		
		Object constantObject=null;
		if (op==null) {
			System.out.println("Could not find operator: "+label);
			throw new ReadingException(ReadingException.Type.OperatorDoesNotExist,label,"");
		}
		if (op.getOperatorType()==Operator.OpType.Constant) {
			switch(op.getDataType()){
			case Verb:
				constantObject = getVerb(findVerb(value));
				break;
			case Actor:
				constantObject = getActor(value);
				break;
			case Prop:
				constantObject = getProp(value);
				break;
			case Stage:
				constantObject = getStage(value);
				break;
			case Quantifier:
				constantObject = getQuantifier(value);
				break;
			case ActorTrait:
				constantObject = getOuterTrait(value);
				break;
			case MoodTrait:
				constantObject = Actor.MoodTrait.valueOf(value);
				break;
			case PropTrait:
				constantObject = getPropTrait(value);
				break;
			case StageTrait:
				constantObject = getStageTrait(value);
				break;
			case Text:
				constantObject = value;
				break;
			case BNumber:
			case Number:
				constantObject = new Float(value);
				break;
			}
		}
		
		scriptNodeCount++;
		if (tokenNode.hasChildNodes()) {
			script.addNode(isRoot, op, desc, constantObject, 1);
			for (int i=0; i<children.getLength(); ++i) {
				child = children.item(i);
				if (child.getNodeType() == Element.ELEMENT_NODE
						&& child.getNodeName().equals("token"))  
					addTokenToScript(script, child, false, permissive);
			}
			script.addNode(false, (Operator)null, desc, 0.0f, -1);
		} else
			script.addNode(isRoot, op, desc, constantObject, 0);
	}
	
	/** 
	 * Write resources for this storyworld into directory fdir.
	 * The behavior is undefined if fdir is not a directory. 
	 * */
	public void writeResources(File fdir)  throws WritingException, IOException {
		
		for(Actor a:getActors()){			
			String name=writeImage(fdir,a);
			if (name!=null)
				a.setImageName(name);
		}
		for(Prop p:getProps()){
			String name=writeImage(fdir,p);
			if (name!=null)
				p.setImageName(name);
		}
		for(Stage s:getStages()){
			String name=writeImage(fdir,s);
			if (name!=null)
				s.setImageName(name);
		}
	}

	private String writeImage(File fdir,Word w) throws IOException {
		final String imageName = w.getImageName();
		final ScaledImage image = w.isImageModified()?w.getImage(this):null;
		if (image==null && (imageName==null || w.isImageModified())) // image does not exist 
			return null;
		
		boolean sameFile = imageName==null?false:Utils.getResourceDir(getFile()).getPath().equals(fdir.getPath());
		if (sameFile && !w.isImageModified()) // image was untouched since loaded
			return imageName;
		else {
			File f;
			// get the destination file
			if (imageName!=null) { // there is a source file
				f = new File(fdir.getPath(),imageName);
			} else if (w.isImageModified()) // no source file -> there's a new image to save
				f = File.createTempFile("image", ".jpg", fdir);
			else // nothing to save
				return null;

			if (w.isImageModified()) { // save the loaded image
				javax.imageio.ImageIO.write(image.getBufferedImage(), "JPG", f);
			} else { // copy the source file to the destination 
				final File sourceFile = new File(Utils.getResourceDir(getFile()),imageName);
				final byte[] bytes = Utils.getBytesFromFile(sourceFile);
				FileOutputStream fos = new FileOutputStream(f);
				try {
					fos.write(bytes);
				} finally {
					fos.close();
				}
			}
			w.resetImageChangeCount();

			return f.getName();
		}
	};

	/** @return the names of the files being references by this storyworld */
	public LinkedList<String> getResourceNames() throws WritingException {
		LinkedList<String> files = new LinkedList<String>();
		for(Actor a:getActors())
			if (a.getImageName()!=null)
				files.add(a.getImageName());
		for(Prop p:getProps())
			if (p.getImageName()!=null)
				files.add(p.getImageName());
		for(Stage s:getStages())
			if (s.getImageName()!=null)
				files.add(s.getImageName());
		return files;
	}
	
	//	**********************************************************************	
	public void writeXML(OutputStream outputStream) {
		DocumentBuilder builder = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Element dictionary, actorSet, pValueSet, 
						stageSet, propSet;
		Actor actor;

		try {
			builder = factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document doc = builder.newDocument();

		// write to doc here.
		dictionary = doc.createElement("dictionary");
		doc.appendChild(dictionary);
		
		dictionary.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		dictionary.setAttribute("xsi:noNamespaceSchemaLocation", "Dictionary.xsd");
		dictionary.setAttribute("version", String.valueOf(version));
		dictionary.setAttribute("inactivityTimeout", String.valueOf(inactivityTimeout));
		
		// CC added version control
		Element versionID = doc.createElement("dictionaryVersion");
		versionID.setAttribute("Label", DeiktoLoader.dictionaryCurrentVersion);
		dictionary.appendChild(versionID);

		// write copyright
		Element cr = doc.createElement("copyright");
		cr.setTextContent(getCopyright());
		dictionary.appendChild(cr);

		if (startingVerb!=null) {
			Element sVerb = doc.createElement("startingVerb");
			sVerb.setAttribute("Label", startingVerb.getLabel());
			dictionary.appendChild(sVerb);
		}
		
		if (startingRole!=null) {
			Element sRole = doc.createElement("startingRole");
			sRole.setAttribute("Label", startingRole.getLabel());
			dictionary.appendChild(sRole);
		}
		
		if (startingOption!=null) {
			Element sOption = doc.createElement("startingOption");
			sOption.setAttribute("Label", startingOption.getLabel());
			dictionary.appendChild(sOption);
		}

		// writing prop traits 
		Element propTraitsElem = doc.createElement("propTraits");
		dictionary.appendChild(propTraitsElem);
		for (FloatTrait t:getPropTraits()){
			Element traitElem = doc.createElement("attribute");
			traitElem.setAttribute("Label", t.getLabel());
			traitElem.setTextContent(t.getDescription());
			propTraitsElem.appendChild(traitElem);
		}

//		 writing prop text traits 
		Element propTextTraitsElem = doc.createElement("propTextTraits");
		dictionary.appendChild(propTextTraitsElem);
		for (TextTrait t:getTextTraits(TraitType.Prop)){
			Element traitElem = doc.createElement("attribute");
			traitElem.setAttribute("Label", t.getLabel());
			traitElem.setTextContent(t.getDescription());
			propTextTraitsElem.appendChild(traitElem);
		}

//		 writing stage traits 
		Element stageTraitsElem = doc.createElement("stageTraits");
		dictionary.appendChild(stageTraitsElem);
		for (FloatTrait t:getStageTraits()){
			Element traitElem = doc.createElement("attribute");
			traitElem.setAttribute("Label", t.getLabel());
			traitElem.setTextContent(t.getDescription());
			stageTraitsElem.appendChild(traitElem);
		}

//		 writing stage text traits 
		Element stageTextTraitsElem = doc.createElement("stageTextTraits");
		dictionary.appendChild(stageTextTraitsElem);
		for (TextTrait t:getTextTraits(TraitType.Stage)){
			Element traitElem = doc.createElement("attribute");
			traitElem.setAttribute("Label", t.getLabel());
			traitElem.setTextContent(t.getDescription());
			stageTextTraitsElem.appendChild(traitElem);
		}

//		 writing outer traits 
		Element outerTraitsElem = doc.createElement("outerTraits");
		if (!areRelationshipsVisible())
			outerTraitsElem.setAttribute("rvisible","false");
		dictionary.appendChild(outerTraitsElem);
		for (FloatTrait t:getActorTraits()) {
			// we want to write the predefined traits if the author has edited
			// the default value for isRelationshipVisible.
			boolean predefined = Utils.contains(predefinedActorTraits,t.getLabel());
			if (!isRelationshipVisible(t) || !predefined){
				Element traitElem = doc.createElement("attribute");
				traitElem.setAttribute("Label", t.getLabel());
				if (!t.isVisible())
					traitElem.setAttribute("visible","false");
				if (!isRelationshipVisible(t))
					traitElem.setAttribute("rvisible","false");
				if (!predefined)
					traitElem.setTextContent(t.getDescription());
				outerTraitsElem.appendChild(traitElem);
			}
		}

		// writing actor text traits 
		Element actorTextTraitsElem = doc.createElement("actorTextTraits");
		dictionary.appendChild(actorTextTraitsElem);
		for (TextTrait t:getTextTraits(TraitType.Actor)){
			Element traitElem = doc.createElement("attribute");
			traitElem.setAttribute("Label", t.getLabel());
			traitElem.setTextContent(t.getDescription());
			actorTextTraitsElem.appendChild(traitElem);
		}

		//  *** Begin writing actorSet ***
		actorSet = doc.createElement("actorSet");	
		dictionary.appendChild(actorSet);

		for(int i = 0; (i < actors.size()); ++i) {
			actor = (Actor)actors.get(i);
			Element actorElement = doc.createElement("actor");
			actorElement.setAttribute("Label", getActor(i).getLabel());
			actorSet.appendChild(actorElement);

			Element newElement = doc.createElement("attributes");
			actorElement.appendChild(newElement);
			
			addTrait(newElement, "description", actor.getDescription());
			if (actor.getImageName()!=null)
				addTrait(newElement, "image", actor.getImageName());
			addTrait(newElement, "active", actor.getActive());
			addTrait(newElement, "female", actor.getFemale());
			addTrait(newElement, "dontMoveMe", actor.getDontMoveMe());
			addTrait(newElement, "unconscious", actor.getUnconscious());
			addTrait(newElement, "location", ((Stage)stages.get(actor.getLocation())).getLabel());
			addTrait(newElement, "spyingOn", ((Actor)actors.get(actor.getSpyingOn())).getLabel());
			addTrait(newElement, "targetStage", ((Stage)stages.get(actor.getTargetStage())).getLabel());
			addTrait(newElement, "occupiedUntil", actor.getOccupiedUntil());
			addTrait(newElement, "cBored", actor.getCBored());
			addTrait(newElement, "howLongToSpy", actor.getHowLongToSpy());

			for(Actor.MoodTrait t:Actor.MoodTraits)
				if (actor.get(t)!=Actor.DEFAULT_FLOATTRAITVALUE) 
					addTrait(newElement, t.name(), actor.get(t));
			for(Actor.TraitType tt:Actor.TraitType.values())
				for(FloatTrait t:getActorTraits())
					if (actor.get(tt,t)!=Actor.DEFAULT_FLOATTRAITVALUE) 
						addTrait(newElement, Actor.traitName(tt, t), actor.get(tt,t));
			for(TextTrait t:getTextTraits(TraitType.Actor)) {
				if (actor.getText(t)!=null) 
					addTrait(newElement, t.getLabel(), actor.getText(t));
			}
			
			Element knowsMeElement = doc.createElement("knowsMe");				
			for(int j = 1; (j < actors.size()); ++j) { 
				if (actor.getKnowsMe(getActor(j))!=Actor.DEFAULT_KNOWSME) {
					Element ofWhomElement = doc.createElement("ofWhom");
					ofWhomElement.setAttribute("Label", getActor(j).getLabel());
					ofWhomElement.setTextContent(String.valueOf(actor.getKnowsMe(getActor(j))));
						knowsMeElement.appendChild(ofWhomElement);
				}
			}
			if (knowsMeElement.getChildNodes().getLength()>0)
				actorElement.appendChild(knowsMeElement);
		}
		//  *** End writing actorSet ***

		//  *** Begin writing pValueSet ***
		//	[change-ld] Added Kin, Up2XXX and KnowsXXX values to the pValueSet 
		pValueSet = doc.createElement("pValueSet");
		dictionary.appendChild(pValueSet);
		
		Element pValues;
		for(int i = 0; (i < actors.size()); ++i) {
			actor = (Actor)actors.get(i);
			pValues = doc.createElement("pValues");
			pValues.setAttribute("OfWhom", getActor(i).getLabel());
			pValueSet.appendChild(pValues);
			for(int j = 0; (j < actors.size()); ++j) {
				if (j != i) {
					Actor actor2 = getActor(j);
					Element AboutWhom = doc.createElement("AboutWhom");
					AboutWhom.setAttribute("Label", actor2.getLabel());
					for(Actor.PTraitType tt:Actor.PTraitType.values())
						for(FloatTrait t:getActorTraits())
							if (actor.isOverrided(t,actor2)) 
								addTrait(AboutWhom, Actor.traitName(tt, t), actor.get(tt,t,actor2));
					for(ExtraTrait t:Actor.ExtraTraits)
						if (actor.get(t,actor2)!=Actor.DEFAULT_P2FLOATTRAITVALUE) 
							addTrait(AboutWhom, t.name(), actor.get(t,actor2));						
					if (AboutWhom.getChildNodes().getLength()>0)
						pValues.appendChild(AboutWhom);
				}
			}

			for(Prop prop2:props) {
				Element AboutWhom = doc.createElement("AboutWhat");
				AboutWhom.setAttribute("Label", prop2.getLabel());
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getPropTraits())
						if (actor.isOverrided(t,prop2)) 
							addTrait(AboutWhom, Actor.traitName(tt, t), actor.get(tt,t,prop2));
				if (AboutWhom.getChildNodes().getLength()>0)
					pValues.appendChild(AboutWhom);
			}

			for(Stage stage2:stages) {
				Element AboutWhom = doc.createElement("AboutWhere");
				AboutWhom.setAttribute("Label", stage2.getLabel());
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getStageTraits())
						if (actor.isOverrided(t,stage2)) 
							addTrait(AboutWhom, Actor.traitName(tt, t), actor.get(tt,t,stage2));
				if (AboutWhom.getChildNodes().getLength()>0)
					pValues.appendChild(AboutWhom);
			}
		}
		//  *** End writing pValueSet ***
		

		//  *** Begin writing propSet ***			
		propSet = doc.createElement("propSet");
		dictionary.appendChild(propSet);
		
		for (int i = 1; (i < props.size()); ++i) {
			Prop prop = props.get(i);
			Element propElement = doc.createElement("prop");
			propElement.setAttribute("Label", getPropLabel(i));
			propSet.appendChild(propElement);
			addVariable(propElement, "description", prop.getDescription());
			if (prop.getImageName()!=null)
				addVariable(propElement, "image", prop.getImageName());
			addVariable(propElement, "carried", prop.getCarried());
			addVariable(propElement, "visible", prop.getVisible());
			addVariable(propElement, "inPlay", prop.getInPlay());
			addVariable(propElement, "owner", ((Actor)actors.get(prop.getOwner())).getLabel());
			addVariable(propElement, "location", ((Stage)stages.get(prop.getLocation())).getLabel());

			Element knowsMeElement = doc.createElement("knowsMe");				
			for(int j = 1; (j < actors.size()); ++j) 
				if (prop.getKnowsMe(getActor(j))!=Prop.DEFAULT_KNOWSME) {
					Element ofWhomElement = doc.createElement("ofWhom");
					ofWhomElement.setAttribute("Label", getActor(j).getLabel());
					ofWhomElement.setTextContent(String.valueOf(prop.getKnowsMe(getActor(j))));
						knowsMeElement.appendChild(ofWhomElement);
				}
			if (knowsMeElement.getChildNodes().getLength()>0)
				propElement.appendChild(knowsMeElement);
			for(FloatTrait t:traits.get(TraitType.Prop))
				if (prop.getTrait(t)!=Prop.DEFAULT_PROPTRAITVALUE)
					addTrait(propElement, t.getLabel(), prop.getTrait(t));
			for(TextTrait t:textTraits.get(TraitType.Prop))
				if (prop.getText(t)!=null)
					addTrait(propElement, t.getLabel(), prop.getText(t));
		}
		//  *** End writing propSet ***			
		
		//  *** Begin writing stageSet ***			
		stageSet = doc.createElement("stageSet");
		dictionary.appendChild(stageSet);
		
		Element stageElement, unwelcomingHomeyElement;
		for(int i = 1; (i < stages.size()); ++i) {
			Stage stage = stages.get(i);
			stageElement = doc.createElement("stage");
			stageElement.setAttribute("Label", getStageLabel(i));
			stageSet.appendChild(stageElement);
			addVariable(stageElement, "description", stage.getDescription());
			if (stage.getImageName()!=null)
				addVariable(stageElement, "image", stage.getImageName());
			addVariable(stageElement, "doorOpen", stage.getDoorOpen());
			addVariable(stageElement, "population", stage.getPopulation());
			addVariable(stageElement, "owner", ((Actor)actors.get(stage.getOwner())).getLabel());
			addVariable(stageElement, "xCoord", stage.getXCoord());
			addVariable(stageElement, "yCoord", stage.getYCoord());
			
			// Now we write out the unwelcoming_Homey
			unwelcomingHomeyElement = doc.createElement("unwelcoming_Homey");
			for(int j = 1; (j < actors.size()); ++j) 
				if (stage.getUnwelcoming_Homey(getActor(j))!=Stage.DEFAULT_P2UNWELCOMINGHOMEY) {
					Element ofWhomElement = doc.createElement("ofWhom");
					ofWhomElement.setAttribute("Label", getActor(j).getLabel());
					ofWhomElement.setTextContent(String.valueOf(stage.getUnwelcoming_Homey(getActor(j))));
					unwelcomingHomeyElement.appendChild(ofWhomElement);
				}
			if (unwelcomingHomeyElement.getChildNodes().getLength()>0)
				stageElement.appendChild(unwelcomingHomeyElement);
			Element knowsMeElement = doc.createElement("knowsMe");				
			for(int j = 1; (j < actors.size()); ++j) 
				if (stage.getKnowsMe(getActor(j))!=Stage.DEFAULT_KNOWSME) {
					Element ofWhomElement = doc.createElement("ofWhom");
					ofWhomElement.setAttribute("Label", getActor(j).getLabel());
					ofWhomElement.setTextContent(String.valueOf(stage.getKnowsMe(getActor(j))));
					knowsMeElement.appendChild(ofWhomElement);
				}
			if (knowsMeElement.getChildNodes().getLength()>0)
				stageElement.appendChild(knowsMeElement);
			for (FloatTrait t:traits.get(TraitType.Stage)) {
				if (stage.getTrait(t)!=Stage.DEFAULT_STAGETRAITVALUE)
					addTrait(stageElement, t.getLabel(), stage.getTrait(t));
			}
			for(TextTrait t:textTraits.get(TraitType.Stage)) {
				if (stage.getText(t)!=null)
					addTrait(stageElement, t.getLabel(), stage.getText(t));
			}
		}
		//  *** End writing stageSet ***			

		
		//  *** Begin writing custom operators ***	
		Element customOperatorSet = doc.createElement("customOperatorSet");
		dictionary.appendChild(customOperatorSet);
		for(Operator op:operators.getOperators(OperatorDictionary.Menu.Custom)) {
			Element customOperator = doc.createElement("customOperator");
			customOperator.setAttribute("Label", op.getLabel());
			customOperator.setAttribute("Type", op.getDataType().name());
			customOperatorSet.appendChild(customOperator);

			// write description
			if (op.getToolTipText()!=null && op.getToolTipText().trim().length()>0) {
				Element description = doc.createElement("description");
				description.setTextContent(op.getToolTipText());
				customOperator.appendChild(description);
			}
			
			// write parameters
			for(int i=0;i<op.getCArguments();i++) {
				Element parameter = doc.createElement("parameter");
				customOperator.appendChild(parameter);
				parameter.setAttribute("Label",op.getArgumentLabel(i));
				parameter.setAttribute("Type", op.getArgumentDataType(i).name());
			}

			addScript(customOperator,((CustomOperator)op).getBody(),"body");
		}
		//  *** End writing custom operators ***			
		
		// write options
		Element optionSet = doc.createElement("optionSet");
		dictionary.appendChild(optionSet);
		Map<Option,Integer> options = writeOptions(doc,optionSet);
		
		// write roles
		Element roleSet = doc.createElement("roleSet");
		dictionary.appendChild(roleSet);
		Map<Role,Integer> roles = writeRoles(doc,roleSet,options);
		
		// *** Begin writing categorySet ***
		//	[change-ld] writing verbs in categories now
		Element categorySet = doc.createElement("categorySet");
		
		writeCategory(doc, categorySet, categories, roles);
		dictionary.appendChild(categorySet);
		

		//  *** Transform the DOM model to a text string ***
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setAttribute("indent-number", new Integer(4));  //
			
			Transformer transformer = tf.newTransformer();
			
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			
	
			// initialize StreamResult with File object to save to file
			
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(outputStream);
			transformer.transform(source,result);
		} 
		catch (TransformerConfigurationException e) {
			System.out.println("can't tranform the DOM model");
		}
		catch (TransformerException e) {
			System.out.println("can't transform the DOM model");
		}
	}

	/** @return the file used to locate storyworld resources. */
	public File getFile() {
		return file;
	}

	/** @param file the file to locate storyworld resources */
	public void setFile(File file) {
		this.file = file;
	}

	/** @return the file for an image with the given filename. */
	public File getImageFile(String filename){
		if (filename==null)
			return null;
		
		return new File(Utils.getResourceDir(file),filename);
	}
	
	// Write categories and their embedded verbs
	private void writeCategory(Document doc, Element parent, Category cat, Map<Role,Integer> roles) {
		Element categoryElement;
		ArrayList<String> verbList;
		for (Category child: cat.getChildren()) {
			categoryElement = doc.createElement("category");
			categoryElement.setAttribute("Label", child.name);
			if (child.children != null) {
				writeCategory(doc, categoryElement, child, roles);
			}
			parent.appendChild(categoryElement);
			
			// write verbs, if there are any here
			if (child.hasNumVerbs(this) > 0) {
				verbList = child.getVerbNames(this);
				for (String verbName: verbList) {
					writeVerb(doc, categoryElement, findVerb(verbName), roles);
				}
				
			}
		}		
	}

	private Map<Option,Integer> writeOptions(Document doc, Element parent) {
		Map<Option,Integer> options = new HashMap<Option, Integer>();
		
		for (Verb verb:getVerbs()) {
			for (Role.Link roleLink:verb.getRoles()) {
				Role role = roleLink.getRole();
				for (Option option: role.getOptions()) {
					if (options.containsKey(option))
						continue;
					
					options.put(option, options.size());
					Element optionElement = doc.createElement("option");
					optionElement.setAttribute("Label", option.getLabel());
					parent.appendChild(optionElement);

					addScript(optionElement, option.getAcceptableScript(),"acceptable");
					addScript(optionElement, option.getDesirableScript(),"desirable");
					for (int k = 0; k<Sentence.MaxWordSockets; ++k) {
						if (option.isWordSocketActive(k)) {
							Element wordSocketSpecsElement = doc.createElement("socketSpecs");
							wordSocketSpecsElement.setAttribute("Index", ((Integer)k).toString());
							optionElement.appendChild(wordSocketSpecsElement);

							addScript(wordSocketSpecsElement, option.getWordSocket(k).getAcceptableScript());
							addScript(wordSocketSpecsElement, option.getWordSocket(k).getDesirableScript());
						}
					}
				}
			}
		}
		
		return options;
	} 

	private Map<Role,Integer> writeRoles(Document doc, Element parent, Map<Option,Integer> options) {
		Map<Role,Integer> roles = new HashMap<Role, Integer>();

		for (Verb verb:getVerbs()) {
			for (Role.Link roleLink:verb.getRoles()) {
				Role role = roleLink.getRole();
				if (roles.containsKey(role))
					continue;
				
				roles.put(role, roles.size());
				Element roleElement = doc.createElement("role");
				parent.appendChild(roleElement);

				addScript(roleElement, role.getAssumeRoleIfScript(), "assumeRoleIf");

				for (int j = 0; (j < role.getEmotionCount()); ++j)
					addScript(roleElement, role.getEmotion(j));
				for (Option option:role.getOptions()) {
					Element optionElement = doc.createElement("optionlink");
					optionElement.setAttribute("Index", options.get(option).toString());
					roleElement.appendChild(optionElement);
				}
			}
		}
		
		return roles;
	} 
	
	// Write the verb pointed to by the verbIndex.  Works whether or not the verb is to
	// be embedded in a category.
	private void writeVerb(Document doc, Element parent, Integer verbIndex, Map<Role,Integer> roles) {
		Element verbElement;
		Element roleElement;
		Script script;
		Verb verb;
		
		verb = getVerb(verbIndex);
		
		verbElement = doc.createElement("verb");
		verbElement.setAttribute("Label", verb.getLabel());
		verbElement.setAttribute("Witnesses", verb.getWitnesses().name());
		
		if (verb.getDescription()!=null && verb.getDescription().length()>0)
			addVariable(verbElement, "description", verb.getDescription());
		addVariable(verbElement, "hijackable", verb.getHijackable());
		if (!verb.getOccupiesDirObject())
			addVariable(verbElement, "occupiesDirObject", verb.getOccupiesDirObject());
		addVariable(verbElement, "timeToPrepare", verb.getTimeToPrepare());
		addVariable(verbElement, "timeToExecute", verb.getTimeToExecute());
		addVariable(verbElement, "category", verb.getCategory());
		addVariable(verbElement, "expression", verb.getExpression());
		addVariable(verbElement, "trivial_Momentous", verb.getTrivial_Momentous());

		// write out any wordSocketIsActive strings
		for (int j=0; j<Sentence.MaxWordSockets; ++j) {
			if (verb.isWordSocketActive(j)) {
				Element socketElement = doc.createElement("socket");
				socketElement.setAttribute("Index", ((Integer)j).toString());
				socketElement.setAttribute("Type", verb.getWordSocketBaseLabel(j));
				socketElement.setAttribute("Visible", verb.isVisible(j)?"true":"false");
				socketElement.setAttribute("Witness", verb.isWitness(j)?"true":"false");
				socketElement.setAttribute("Presence", verb.getPresence(j).name());
				socketElement.setAttribute("Notes", verb.getNote(j));
				if (verb.getSuffix(j)!=null)
					addScript(socketElement,verb.getSuffix(j),"suffix");
				if (verb.getWordsocketTextScript(j)!=null)
					addScript(socketElement,verb.getWordsocketTextScript(j));
				verbElement.appendChild(socketElement);
			}
		}
		
		if (verb.getAbortScript()!=null)
			addScript(verbElement, verb.getAbortScript(),"abort");
		
		for (int j=0; j<verb.getConsequenceCount(); ++j) {
			script = (Script)verb.getConsequence(j);
			addScript(verbElement, script);
		}
		
		for (Role.Link roleLink:verb.getRoles()) {
			roleElement = doc.createElement("rolelink");
			roleElement.setAttribute("Label", roleLink.getLabel());
			roleElement.setAttribute("Index", roles.get(roleLink.getRole()).toString());
			verbElement.appendChild(roleElement);
		}
		parent.appendChild(verbElement);
	}
	
	// addTrait replaces writeTrait()
	private void addTrait(Element current, String name, String value) {
		Element newElement = current.getOwnerDocument().createElement("attribute"); 
		
		newElement.setAttribute("Name", name);
		current.appendChild(newElement);
		newElement.setTextContent(value);
	}
	private void addTrait(Element current, String name, Boolean value) {
		addTrait(current, name, String.valueOf(value));
	}
	
	private void addTrait(Element current, String name, Integer value) {
		addTrait(current, name, String.valueOf(value));
	}
	
	private void addTrait(Element current, String name, Float value) {
		addTrait(current, name, String.valueOf(value));
	}

	// addVariable replaces writeVariable()
	private void addVariable(Element current, String name, String value) {
		Element newElement = current.getOwnerDocument().createElement(name); 

		current.appendChild(newElement);
		newElement.setTextContent(value);
		
	}
	private void addVariable(Element current, String name, Boolean value) {
		addVariable(current, name, String.valueOf(value));
	}
	
	private void addVariable(Element current, String name, Integer value) {
		addVariable(current, name, String.valueOf(value));
	}
	
	private void addVariable(Element current, String name, Float value) {
		addVariable(current, name, String.valueOf(value));
	}

	
	//**********************************************************************
	// addScript() replaces writeScript()
	private void addScript(Element current, Script tScript) {
		addScript(current,tScript,"script");
	}
	private void addScript(Element current, Script tScript, String label) {
		Element scriptElement = current.getOwnerDocument().createElement(label);
		
		addNode(scriptElement, tScript.getRoot());
		current.appendChild(scriptElement);
	}
	
	// **********************************************************************
	// addNode replaces writeNode()
	private void addNode(Element current, Script.Node tNode) {
		Element tokenElement = current.getOwnerDocument().createElement("token");
		current.appendChild(tokenElement);
		
		Operator zOperator = tNode.getOperator();
		
		String constantValue;
		if (zOperator.getLabel().endsWith("Constant"))
			constantValue = tNode.toString();
		else
			constantValue = "";
		
		if (tNode.getDescription().length()!=0){
			Element descriptionElement = current.getOwnerDocument().createElement("Description");
			descriptionElement.setTextContent(tNode.getDescription());
			tokenElement.appendChild(descriptionElement);
		}
		
		if (zOperator instanceof ParameterOperator) {
			tokenElement.setAttribute("Parameter", "true");
			tokenElement.setAttribute("Arg", String.valueOf(((ParameterOperator)zOperator).getParameterIndex()));
		} else if (tNode.isLeaf())	{
			tokenElement.setAttribute("Label", zOperator.getLabel());
			tokenElement.setAttribute("Arg", constantValue);
		} else {
			tokenElement.setAttribute("Label", zOperator.getLabel());
			tokenElement.setAttribute("Arg", constantValue);
			
			for (int i = 0; (i < tNode.getChildCount()); ++i)
				addNode(tokenElement,(Script.Node)tNode.getChildAt(i));
		}		
	}
	
//**********************************************************************	
	public int findActor(String tWordLabel) {
		int i = actors.size();
		do { --i; } while ((i >= 0) && !actors.get(i).getLabel().equals(tWordLabel));
		if (i < 0)
			System.out.println("Error in Deikto.findActor: not found: " + tWordLabel);
		return (i);
	}
//**********************************************************************	
	public int findStage(String tWordLabel) {
		int i = stages.size();
		do { --i; } while ((i >= 0) && !stages.get(i).getLabel().equals(tWordLabel));
		if (i < 0)
			System.out.println("Error in Deikto.findStage: not found: " + tWordLabel);
		return (i);
	}
//**********************************************************************	
	public int findProp(String tWordLabel) {
		int i = props.size();
		do { --i; } while ((i >= 0) && !props.get(i).getLabel().equals(tWordLabel));
		if (i < 0)
			System.out.println("Error in Deikto.findProp: not found: " + tWordLabel);
		return (i);
	}
//**********************************************************************	
	public int findVerb(String tWordLabel) {
		int i = getVerbCount();
		do { --i; } while ((i >= 0) && !getVerb(i).getLabel().equals(tWordLabel));
		if (i < 0)
			System.out.println("Error in Deikto.findVerb: not found: " + tWordLabel);
		return (i);
	}
//**********************************************************************	
	public int findActorTraitWord(String tWordLabel) {
		int i = getActorTraits().size();
		do { --i; } while ((i >= 0) && !getActorTraits().get(i).getLabel().equals(tWordLabel));
		if (i < 0)
			System.out.println("Error in Deikto.findActorTraitWord: not found: " + tWordLabel);
		return (i);
	}
//**********************************************************************
	public int findPropTraitWord(FloatTrait t){
		return findPropTraitWord(t.getLabel());
	}
	public int findPropTraitWord(String tWordLabel) {
		int i = getPropTraits().size();
		do { --i; } while ((i >= 0) && !getPropTraits().get(i).getLabel().equals(tWordLabel));
		if (i < 0)
			System.out.println("Error in Deikto.findPropTraitWord: not found: " + tWordLabel);
		return (i);
	}
//**********************************************************************
	public FloatTrait getPropTrait(String tLabel) {
		return getTrait(TraitType.Prop,tLabel);
	}
	public FloatTrait getTrait(TraitType tt,String tLabel) {
		return getTrait(tt,tLabel,true);
	}
	public TextTrait getTextTrait(TraitType tt,String tLabel) {
		return getTextTrait(tt,tLabel,true);
	}
	public FloatTrait getTrait(TraitType tt,String tLabel,boolean printError) {
		for (FloatTrait t:traits.get(tt))
			if (t.getLabel().equals(tLabel)) 
				return t;
		if (printError) 
			System.out.println("Error in Deikto.getTrait: not found: " + tLabel);
		return null;
	}
	public TextTrait getTextTrait(TraitType tt,String tLabel,boolean printError) {
		for (TextTrait t:textTraits.get(tt))
			if (t.getLabel().equals(tLabel)) 
				return t;
		if (printError) 
			System.out.println("Error in Deikto.getTextTrait: not found: " + tLabel);
		return null;
	}
	public int getTraitIndex(TraitType tt,String tLabel) {
		int i=0;
		for (FloatTrait t:traits.get(tt))
			if (t.getLabel().equals(tLabel)) return i;
			else i++;
		return -1;
	}
	public int getTextTraitIndex(TraitType tt,String tLabel) {
		int i=0;
		for (TextTrait t:textTraits.get(tt))
			if (t.getLabel().equals(tLabel)) return i;
			else i++;
		return -1;
	}
	public FloatTrait getStageTrait(String tLabel) {
		return getTrait(TraitType.Stage,tLabel);
	}
	public FloatTrait getOuterTrait(String tLabel) {
		return getTrait(TraitType.Actor,tLabel);
	}
	public FloatTrait getCool_VolatilTrait() {
		return traits.get(TraitType.Actor).get(ACTOR_COOL_VOLATIL);
	}
	
	public Iterable<FloatTrait> getVisibleTraits(TraitType tt){ 
		return new IterableFilter<FloatTrait>(traits.get(tt)){
					@Override
					protected boolean evaluatePredicate(FloatTrait t){ return t.isVisible(); }
				}; 
	}

//**********************************************************************	
	public ArrayList<FloatTrait> getPropTraits(){ return traits.get(TraitType.Prop); }
	public ArrayList<FloatTrait> getStageTraits(){ return traits.get(TraitType.Stage); }
	public ArrayList<FloatTrait> getActorTraits(){ return traits.get(TraitType.Actor); }
	public ArrayList<FloatTrait> getTraits(TraitType tt){ return traits.get(tt); }
	public ArrayList<TextTrait> getTextTraits(TraitType tt){ return textTraits.get(tt); }
//**********************************************************************	
	public int getTextTraitCount(TraitType tt){ return textTraits.get(tt).size(); }
	public int getTraitCount(TraitType tt){ return traits.get(tt).size(); }
	public int getVisibleTraitCount(TraitType tt){ 
		int i=0;
		for(FloatTrait t:traits.get(tt))
			if (t.isVisible())
				i++;
		return i;
	}
//**********************************************************************	
	public int findStageTrait(String tWordLabel) {
		int i = getStageTraits().size();
		do { --i; } while ((i >= 0) && !getStageTraits().get(i).getLabel().equals(tWordLabel));
		if (i < 0)
			System.out.println("Error in Deikto.findStageTrait: not found: " + tWordLabel);
		return (i);
	}
//**********************************************************************	
	public int findQuantifier(String tWordLabel) {
		int i = quantifiers.size();
		do { --i; } while ((i >= 0) && !quantifiers.get(i).getLabel().equals(tWordLabel));
		if (i < 0)
			System.out.println("Error in Deikto.findQuantifier: not found: " + tWordLabel);
		return (i);
	}
//**********************************************************************	
	public int findWordByWordType(Operator.Type tType, String wordLabel) {
		switch (tType) {
		case Actor: { return(findActor(wordLabel)); }
		case Prop: { return(findProp(wordLabel)); }
		case Stage: { return(findStage(wordLabel)); }
		case Verb: { return(findVerb(wordLabel)); }
		case ActorTrait: { return(findActorTraitWord(wordLabel)); }
		case PropTrait: { return(findPropTraitWord(wordLabel)); }
		case StageTrait: { return(findStageTrait(wordLabel)); }
		case MoodTrait: { return Actor.MoodTrait.valueOf(wordLabel).ordinal(); }
		case Quantifier: { return(findQuantifier(wordLabel)); }
		default: return(0);
		}
	}
//**********************************************************************	
	public String getPropLabel(int tIndex) {
		return (props.get(tIndex).getLabel());
	}
//**********************************************************************	
	public String getStageLabel(int tIndex) {
		return (stages.get(tIndex).getLabel());
	}
//**********************************************************************	
	public String getQuantifierLabel(int tIndex) {
		return (quantifiers.get(tIndex).getLabel());
	}
	public List<Quantifier> getQuantifiers(){ return quantifiers; }
	public Quantifier getQuantifier(String label) {
		for(Quantifier q:quantifiers) {
			if (q.getLabel().equals(label))
				return q;
		}
		return null;
	}
//**********************************************************************	
	private <W extends Word> void removeWord(java.util.List<W> ws,W w) {
		ws.remove(w);
		for(int i=w.getReference().getIndex();i<ws.size();i++)
			ws.get(i).getReference().setIndex(i);
		w.getReference().setIndex(0);
	}
	private <W extends Word> void addWord(java.util.List<W> ws,int pos,W w) {
		ws.add(pos,w);
		for(int i=pos;i<ws.size();i++)
			((Word)ws.get(i)).getReference().setIndex(i);	
	}
	private <W> void moveWord(java.util.List<W> ws,int from,int to) {
		ws.add(to,ws.remove(from));
		int max=Math.max(from,to);
		for(int i=Math.min(from,to);i<=max;i++)
			((Word)ws.get(i)).getReference().setIndex(i);		
	}
//**********************************************************************	
	public void removeActor(final Actor actor) {
		traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Script.Node n) {
				if (n.getConstant() instanceof Actor 
					&& ((Actor)n.getConstant()) == actor)
					n.setOperatorValue(OperatorDictionary.getUndefinedActorOperator(), null);
				return true;
			}
		});
		removeWord(actors,actor);
	}
//**********************************************************************
	public int getActorIndex(String label) { return getWordIndex(actors,label); }
//	**********************************************************************
	public List<Actor> getActors(){	return actors;	}
	public int getActorCount(){	return actors.size();	}
	public Actor getActor(String label){
		int i=getActorIndex(label);
		if (i==-1)
			return null;
		else
			return getActor(i);	
	}
	public Actor getActor(int i){ return actors.get(i);	}
	public int getIndexOf(Actor a){ return actors.indexOf(a);	}
//	**********************************************************************
	public void moveActor(int from,int to) { moveWord(actors,from,to); }
	public void addActor(Actor a) throws LimitException {	addActor(actors.size(),a);	}
	public void addActor(int tiActor,Actor a) throws LimitException {
		if (limits.maximumActorCount<=getActorCount())
			throw new LimitException(LimitException.Type.Actors,a.getLabel(),limits.maximumActorCount);
		addWord(actors,tiActor,a);
	}
//	**********************************************************************
	public List<Stage> getStages(){	return stages;	}
	public int getStageCount(){	return stages.size();	}
	public Stage getStage(int i){ return stages.get(i);	}
	public Stage getStage(String value){ return getStage(getStageIndex(value));	}
	public int getIndexOf(Stage a){ return stages.indexOf(a);	}
	public void removeAllStages(){	stages.clear();	}
//	**********************************************************************
	public void moveStage(int from,int to) { moveWord(stages,from,to); }
	public void addStage(Stage s) throws LimitException {	addStage(stages.size(),s);	}
	public int getStageIndex(String label) { return getWordIndex(stages,label); }
	public void addStage(int tiStage,Stage s) throws LimitException {
		if (limits.maximumStageCount<=getStageCount())
			throw new LimitException(LimitException.Type.Stages,s.getLabel(),limits.maximumStageCount);
		addWord(stages,tiStage,s);
	}
	public void removeStage(final Stage stage) {
		traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Script.Node n) {
				if (n.getConstant() instanceof Stage 
					&& ((Stage)n.getConstant()) == stage)
					n.setOperatorValue(OperatorDictionary.getUndefinedStageOperator(), null);
				return true;
			}
		});
		removeWord(stages,stage);
	}
//	**********************************************************************
	public List<Prop> getProps(){	return props;	}
	public int getPropCount(){	return props.size();	}
	public Prop getProp(int i){ return props.get(i);	}
	public Prop getProp(String label){ return getProp(getPropIndex(label));	}
	public int getPropIndex(String label) { return getWordIndex(props,label); }
	public int getIndexOf(Prop a){ return props.indexOf(a);	}
	public void removeAllProps(){	props.clear();	}
//	**********************************************************************
	public static int getWordIndex(Iterable<? extends Word> ws,String label){
		int i=0;
		for(Word w:ws){
			if (w.getLabel().equals(label)) return i;
			i++;
		}
		return -1;
	}
//	**********************************************************************
	public void moveProp(int from,int to) { moveWord(props,from,to); }
	public void addProp(Prop p) throws LimitException {addProp(props.size(),p);}
	public void addProp(int tiProp,Prop p) throws LimitException {
		if (limits.maximumPropCount<=getPropCount())
			throw new LimitException(LimitException.Type.Props,p.getLabel(),limits.maximumPropCount);
		addWord(props,tiProp,p);
	} 
	public void removeProp(final Prop prop) { 
		traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Script.Node n) {
				if (n.getConstant() instanceof Prop 
					&& ((Prop)n.getConstant()) == prop)
					n.setOperatorValue(OperatorDictionary.getUndefinedPropOperator(), null);
				return true;
			}
		});
		removeWord(props,prop);
	}
//	**********************************************************************
	public List<Verb> getVerbs(){	return verbs;	}
	public boolean containsVerb(Verb v){ return verbs.contains(v); }
	public int getVerbCount(){	return verbs.size();	}
	public Verb getVerb(int i){ 
		return verbs.get(i);	
	}
	public Verb getVerb(String label){ 
		int i=findVerb(label);
		if (i!=-1)
			return getVerb(i);
		else
			return null;
	}
	public int getIndexOf(Verb a){ return verbs.indexOf(a);	}
	public void removeAllVerbs(){	verbs.clear();	}
//	**********************************************************************
	public void moveVerb(int from,int to) { moveWord(verbs,from,to); }
	public void addVerb(Verb v) throws LimitException {	addVerb(verbs.size(),v);	}
	public void removeVerb(final Verb verb) {
		if (verb==getStartingVerb())
			setStartingVerb(null);

		unregisterVerbRoleLink(verb);

		// delete options pointing to this verb
		ArrayList<Integer> optionsToDelete = new ArrayList<Integer>();
		for (Verb zVerb: getVerbs()) {
			for (Role.Link zRole: zVerb.getRoles()) {
				int i=0;
				for (Option zOption:zRole.getRole().getOptions()){ 
					if (zOption.getPointedVerb()==verb) 
						optionsToDelete.add(i);
					i++;
				}
				for (int iOption:optionsToDelete) 
					deleteOption(zRole.getRole(),iOption);
				optionsToDelete.clear();
			}
		}

		// delete references to the verb in scripts
		final Verb v = verb;
		traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Script.Node n) {
				if (n.getConstant() instanceof Verb 
					&& ((Verb)n.getConstant()) == v)
					n.setOperatorValue(OperatorDictionary.getUndefinedVerbOperator(), null);
				return true;
			}
		});

		
		removeWord(verbs,verb);

		roleCount -= verb.getRoleCount();
		optionCount -= verb.getOptionCount();
	}
	public void addVerb(int tiVerb,Verb v) throws LimitException {
		if (limits.maximumVerbCount<=getVerbCount())
			throw new LimitException(LimitException.Type.Verbs,v.getLabel(),limits.maximumVerbCount);

		final int rCount = roleCount + v.getRoleCount();
		final int oCount = optionCount + v.getOptionCount();

		if (limits.maximumRoleCount<=rCount)
			throw new LimitException(LimitException.Type.Roles,v.getLabel(),limits.maximumRoleCount);
		if (limits.maximumOptionCount<=oCount)
			throw new LimitException(LimitException.Type.Options,v.getLabel(),limits.maximumOptionCount);

		addWord(verbs,tiVerb,v);
		for(Role.Link r:v.getRoles())
			registerRoleLink(v,r.getRole());
		
		roleCount = rCount;
		optionCount = oCount;
	}
	/** Deletes a role from a verb. */
	public Role.Link deleteRole(Verb v,int iRole) {
		final Role.Link r = v.getRole(iRole);
		if (r==getStartingRole())
			setStartingRole(null);

		unregisterRoleLink(v,r);
		v.deleteRole(iRole);

		roleCount--;
		optionCount -= r.getRole().getOptions().size();
		return r;
	}

	/** Adds a role into a verb. */
	public Role.Link addRole(Verb v, Role.Link r) throws LimitException {
		return addRole(v,v.getRoleCount(),r);
	}

	/** 
	 * Adds a role into a verb. 
	 * @param addDefaultScripts tells if default script should be added to the role.
	 * */
	public Role.Link addRole(Verb v,String tLabel, boolean addDefaultScripts) throws LimitException {
		return addRole(v,new Role.Link(new Role(addDefaultScripts),tLabel));
	}

	/** Adds a role into a verb. */
	public Role.Link addRole(Verb v,int pos, Role.Link r) throws LimitException {
		if (roleCount>=limits.maximumRoleCount)
			throw new LimitException(LimitException.Type.Roles,v.getLabel(),limits.maximumRoleCount);
		final int oCount = optionCount + r.getRole().getOptions().size();
		if (limits.maximumOptionCount<=oCount)
			throw new LimitException(LimitException.Type.Options,v.getLabel(),limits.maximumOptionCount);

		v.addRole(pos,r);
		
		registerRoleLink(v,r.getRole());
		roleCount++;
		optionCount = oCount;
		return r;
	}

	private void registerRoleLink(Verb v,Role r){
		if (roleVerbs!=null) {
			ArrayList<Verb> verbs = roleVerbs.get(r);
			if (verbs==null) {
				verbs = new ArrayList<Verb>();
				roleVerbs.put(r,verbs);
			}
			if (!verbs.contains(v)) {
				verbs.add(v);
				for(Option o:r.getOptions())
					registerOptionLink(r,o);
			}
		}
	}

	private void unregisterVerbRoleLink(Verb v){
		if (roleVerbs!=null) {
			for(Role.Link r:v.getRoles()) {
				ArrayList<Verb> verbs = roleVerbs.get(r.getRole());
				if (verbs!=null) {
					verbs.remove(v);
					if (verbs.isEmpty()) {
						// That was the last reference to the role
						roleVerbs.remove(verbs);
						for(Option o:r.getRole().getOptions())
							unregisterOptionLink(r.getRole(),o);
					}
				}
			}
		}
	}
	
	private void unregisterRoleLink(Verb v,Role.Link r){
		if (roleVerbs!=null) {
			ArrayList<Verb> verbs = roleVerbs.get(r.getRole());
			if (verbs!=null && v.countRoleOccurrences(r.getRole())==1) {
				// This is the last reference to the role in the verb.
				verbs.remove(v);
				if (verbs.isEmpty()) {
					// That was the last reference to the role
					roleVerbs.remove(verbs);
					for(Option o:r.getRole().getOptions())
						unregisterOptionLink(r.getRole(),o);
				}
			}
		}
	}

	/** Adds an option in the given role. */
	public Role.Option addOption(Role r,Verb verb) throws LimitException {
		return addOption(r,new Role.Option(verb,true));
	}

	/** Adds an option in the given role. */
	public Role.Option addOption(Role r,Role.Option option) throws LimitException {
		return addOption(r,r.getOptions().size(),option);
	}

	/** Adds an option in the given role. */
	public Role.Option addOption(Role r,int pos,Role.Option option) throws LimitException {
		if (optionCount>=limits.maximumOptionCount)
			throw new LimitException(LimitException.Type.Options,"",limits.maximumOptionCount);

		final Role.Option o = r.addOption(pos,option);

		registerOptionLink(r,o);
		optionCount++;
		return o;
	}

	private void registerOptionLink(Role r,Option o){
		if (optionRoles!=null) {
			ArrayList<Role> roles = optionRoles.get(o);
			if (roles==null) {
				roles = new ArrayList<Role>();
				optionRoles.put(o,roles);
			}
			if (!roles.contains(r))
				roles.add(r);
		}
	}

	private void unregisterOptionLink(Role r,Option o){
		if (optionRoles!=null) {
			ArrayList<Role> roles = optionRoles.get(o);
			if (roles!=null) {
				roles.remove(r);
				if (roles.isEmpty())
					optionRoles.remove(roles);
			}
		}
	}
	
	/** Deletes an option from a role. */
	public void deleteOption(Role r,Role.Option option){
		int i=r.getOptions().indexOf(option);
		if (i>=0)
			deleteOption(r,i);
	}
	
	/** Deletes an option from a role. */
	public void deleteOption(Role r,int iOption){
		final Role.Option option=r.getOptions().get(iOption);
		if (option==getStartingOption())
			setStartingOption(null);
		unregisterOptionLink(r,option);
		r.deleteOption(iOption);
		
		optionCount--;
	}

	/** Adds an emotional reaction to a given role. */
	public void addEmotion(Role r,Script s) {
		addEmotion(r,r.getEmotionCount(),s);
	}

	/** Adds an emotional reaction to a given role at the given position. */
	public void addEmotion(Role r,int i,Script s) {
		r.addEmotion(i,s);
	}

	/** Adds an emotional reaction to a given role. */
	public Script addEmotion(Role r,Operator tOperator) {
		Script zScript = new Script(Script.Type.Emotion,-1,null,tOperator, true);		
		addEmotion(r,zScript);
		return zScript;
	}

	/** Deletes an emotion from a given role. */
	public void deleteEmotion(Role r,String label) {
		int i=r.getEmotionIndex(label);
		if (i>=0)
			deleteEmotion(r,i);
	}
	
	/** Deletes an emotion from a given role. */
	public void deleteEmotion(Role r,int iEmotion) {
		r.deleteEmotion(iEmotion);
	}

	/** Sets the assume role if script for a given role. */
	public void setAssumeRoleIf(Role r,Script s) {
		r.setAssumeRoleIfScript(s);
	}

	/** Sets the acceptable script for a given option. */
	public void setAcceptableScript(Role.Option o,Script s) {
		o.setAcceptableScript(s);
	}
	/** Sets the desirable script for a given option. */
	public void setDesirableScript(Role.Option o,Script s) {
		o.setDesirableScript(s);
	}

	/** Sets the acceptable script for a given wordsocket. */
	public void setAcceptableScript(Role.Option.OptionWordSocket ws,Script s) {
		ws.setAcceptableScript(s);
	}

	/** Sets the desirable script for a given wordsocket. */
	public void setDesirableScript(Role.Option.OptionWordSocket ws,Script s) {
		ws.setDesirableScript(s);
	}

	/** Sets the abortIf  script for a given verb. */
	public void setAbortScript(Verb v,Script s) throws LimitException {
		v.setAbortScript(s);
	}

	/** Adds a consequence to a given verb. */
	public void addConsequence(Verb v,Script s) {
		addConsequence(v,v.getConsequenceCount(),s);
	}

	/** Adds a consequence to a given verb at the given position. */
	public void addConsequence(Verb v,int i,Script s) {
		v.addConsequence(i,s);
	}

	/** Adds a consequence to a given verb. */
	public Script addConsequence(Verb v,Operator tOperator) {
		Script zScript = new Script(Script.Type.Consequence,-1,null,tOperator, true);		
		addConsequence(v,zScript);
		return zScript;
	}

	/** Deletes a consequence from a given verb. */
	public void deleteConsequence(Verb v,int iConsequence) {
		v.deleteConsequence(iConsequence);
	}
	
	/** Deletes a consequence from a given verb. */
	public void deleteConsequence(Verb v,String label) {
		int i = v.getConsequenceIndex(label);
		if (i>=0)
			deleteConsequence(v, i);
	}

//**********************************************************************	
	public String getLabelByDataType(Operator.Type tType, int wordIndex) {
		switch (tType) {
		case Actor: { return(getActor(wordIndex).getLabel()); }
		case Prop: { return(getProp(wordIndex).getLabel()); }
		case Stage: { return(getStage(wordIndex).getLabel()); }
		case Verb: { return(getVerb(wordIndex).getLabel()); }
		case ActorTrait: { return(getActorTraits().get(wordIndex).getLabel()); }
		case PropTrait: { return(getPropTraits().get(wordIndex).getLabel()); }
		case StageTrait: { return(getStageTraits().get(wordIndex).getLabel()); }
		case MoodTrait: { return Actor.MoodTrait.values()[wordIndex].toString(); }
		case Quantifier: { return(quantifiers.get(wordIndex).getLabel()); }
		default: return("Error_Error");
		}
	}
//**********************************************************************	
	public String getDescriptionByType(Operator.Type tType, int wordIndex) {
		switch (tType) {
		case Actor: { return(getActor(wordIndex).getDescription()); }
		case Prop: { return(getProp(wordIndex).getDescription()); }
		case Stage: { return(getStage(wordIndex).getDescription()); }
		case Verb: { return(getVerb(wordIndex).getDescription()); }
//		case Operator.OuterTrait: { return(outerTraits.get(wordIndex).getDescription()); }
//		case Operator.InnerTrait: { return(innerTraits.get(wordIndex).getDescription()); }
//		case Operator.PropTrait: { return(propTraits.get(wordIndex).getDescription()); }
//		case Operator.StageTrait: { return(stageTraits.get(wordIndex).getDescription()); }
//		case Operator.Quantifier: { return(quantifiers.get(wordIndex).getDescription()); }
		default: return("");
		}
	}

	/**
	 * Use this interface to pass a method to the traversing routine 
	 * {@link Deikto#traverseScripts(ScriptTraverser)}.
	 * This method is to be called for each node in the script visited in preorder.
	 */
	public interface ScriptTraverser {
		/** Called for each visited script.	 */
		void traversing(Verb verb,Role.Link role,Option option,Script s);
	}
	
	/**
	 * <p>Traverses each script in the world model using the
	 * traverser <code>t</code>.
	 * Use it like this:
	 * <pre>
	dk.traverse(new Traverser(){
		public boolean traversing(Script s) {
          do something with s
          return true;
      	}
	});
	  </pre>
	 * Here "do something with s" will be executed for each 
	 * script in the world model.  
	 * </p>
	 * */
	public void traverseScripts(ScriptTraverser t){
		for (Verb zVerb: getVerbs())
			traverseScripts(zVerb,t);
		for(Operator op:operators.getOperators(OperatorDictionary.Menu.Custom))
			t.traversing(null,null,null,((CustomOperator)op).getBody());
	}

	/** 
	 * Same as {@link #traverseScripts(ScriptTraverser)} but shared scripts are
	 * traversed only once.
	 * */
	public void traverseScriptsJustOnce(ScriptTraverser t){
		for(Map.Entry<Option,ArrayList<Role>> e:optionRoles.entrySet()) {
			Option zOption = e.getKey();
			t.traversing(null,null,zOption,zOption.getAcceptableScript());
			t.traversing(null,null,zOption,zOption.getDesirableScript());
			for (Role.Option.OptionWordSocket zWordSocket: zOption.getActiveWordSockets()) {
				if (zWordSocket.getIWordSocket()<2)
					continue;

				if (zWordSocket.getAcceptableScript().getRoot() != null)		
					t.traversing(null,null,zOption,zWordSocket.getAcceptableScript());
				if (zWordSocket.getDesirableScript().getRoot() != null)
					t.traversing(null,null,zOption,(Script)zWordSocket.getDesirableScript());
			}
		}
		
		for(Map.Entry<Role,ArrayList<Verb>> e:roleVerbs.entrySet()){
			t.traversing(null,null,null,e.getKey().getAssumeRoleIfScript());
			for (Script emotionScript: e.getKey().getEmotions())
				t.traversing(null,null,null,emotionScript);
		}

		for(Verb zVerb:getVerbs()) {
			if (zVerb.getAbortScript()!=null)
				t.traversing(zVerb,null,null,zVerb.getAbortScript());
			for (Script sc: zVerb.getConsequences())
				t.traversing(zVerb,null,null,sc);
			for (int i=0;i<Sentence.MaxWordSockets;i++) {
				if (zVerb.isWordSocketActive(i)) {
					t.traversing(zVerb,null,null,zVerb.getWordsocketTextScript(i));
					t.traversing(zVerb,null,null,zVerb.getSuffix(i));
				}
			}
		}
		for(Operator op:operators.getOperators(OperatorDictionary.Menu.Custom))
			t.traversing(null,null,null,((CustomOperator)op).getBody());
	}

	/** 
	 * Same as {@link #traverseScripts(ScriptTraverser)} but searches only over a
	 * given verb.
	 * */
	public void traverseScripts(Verb zVerb,ScriptTraverser t){
		if (zVerb.getAbortScript()!=null)
			t.traversing(zVerb,null,null,zVerb.getAbortScript());
		for (Script sc: zVerb.getConsequences())
			t.traversing(zVerb,null,null,sc);
		for (Role.Link zRole: zVerb.getRoles())
			traverseScripts(zVerb,zRole,t);
		for (int i=0;i<Sentence.MaxWordSockets;i++) {
			if (zVerb.isWordSocketActive(i)) {
				t.traversing(zVerb,null,null,zVerb.getWordsocketTextScript(i));
				t.traversing(zVerb,null,null,zVerb.getSuffix(i));
			}
		}
	}

	/** 
	 * Same as {@link #traverseScripts(ScriptTraverser)} but searches only over a
	 * given role.
	 * */
	public void traverseScripts(Verb zVerb,Role.Link zRole,ScriptTraverser t){
		t.traversing(zVerb,zRole,null,zRole.getRole().getAssumeRoleIfScript());
		for (Script emotionScript: zRole.getRole().getEmotions())
			t.traversing(zVerb,zRole,null,emotionScript);
		for (Role.Option zOption: zRole.getRole().getOptions()) {
			t.traversing(zVerb,zRole,zOption,zOption.getAcceptableScript());
			t.traversing(zVerb,zRole,zOption,zOption.getDesirableScript());
			for (Role.Option.OptionWordSocket zWordSocket: zOption.getActiveWordSockets()) {
				if (zWordSocket.getIWordSocket()<2)
					continue;

				if (zWordSocket.getAcceptableScript().getRoot() != null)		
					t.traversing(zVerb,zRole,zOption,zWordSocket.getAcceptableScript());
				if (zWordSocket.getDesirableScript().getRoot() != null)
					t.traversing(zVerb,zRole,zOption,(Script)zWordSocket.getDesirableScript());
			}
		}

	}

	/**
	 * <p>Traverses each node of each script in the world model using the
	 * traverser <code>t</code>.
	 * Use it like this:
	 * <pre>
	dk.traverse(new Script.Traverser(){
		public boolean traversing(Script s, DMTN n) {
          do something with s and n
          return true;
      	}
	});
	  </pre>
	 * Here "do something with s and n" will be executed for each node in each
	 * script in the world model (see {@link Script#traverse(ScriptTraverser)}).  
	 * </p>
	 * */
	public void traverseScripts(final Script.NodeTraverser t){
		traverseScripts(new ScriptTraverser() {
			public void traversing(Verb verb,Role.Link role,Option option,Script s) {
				t.setPath(verb, role, option);
				s.traverse(t);
			}
		});
	}

	/**
	 * Same as {@link #traverseScriptsJustOnce(com.storytron.uber.Script.NodeTraverser)} but
	 * works with a NodeTraverser argument.
	 * */
	public void traverseScriptsJustOnce(final Script.NodeTraverser t){
		traverseScriptsJustOnce(new ScriptTraverser() {
			public void traversing(Verb verb,Role.Link role,Option option,Script s) {
				t.setPath(verb, role, option);
				s.traverse(t);
			}
		});
	}

	/**
	 * Same as {@link #traverseScripts(com.storytron.uber.Script.NodeTraverser)} but
	 * restricts the search over a given verb.
	 * */
	public void traverseScripts(Verb v,final Script.NodeTraverser t){
		traverseScripts(v,new ScriptTraverser() {
			public void traversing(Verb verb,Role.Link role,Option option,Script s) {
				t.setPath(verb, role, option);
				s.traverse(t);
			}
		});
	}

	/**
	 * Same as {@link #traverseScripts(com.storytron.uber.Script.NodeTraverser)} but
	 * restricts the search over a given role.
	 * */
	public void traverseScripts(Verb v,Role.Link r,final Script.NodeTraverser t){
		traverseScripts(v,r,new ScriptTraverser() {
			public void traversing(Verb verb,Role.Link role,Option option,Script s) {
				t.setPath(verb, role, option);
				s.traverse(t);
			}
		});
	}

	/** 
	 * Get the script object pointed to by the type and the string locators.
     * Note: the logic in this function is driven by {@link Script#getScriptLocators()}.
     * */
	public Pair<ScriptPath,Script> getScriptPath(Script.Type st, String[] stringLocators) {
		Verb scriptVerb;
		Role.Link scriptRole;
		Role.Option scriptOption;
		switch(st){
		case AssumeRoleIf:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			scriptRole = scriptVerb.getRole(stringLocators[1]);
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,scriptRole,null),scriptRole.getRole().getAssumeRoleIfScript()); 
		case AbortIf:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,null,null),scriptVerb.getAbortScript()); 
		case Emotion:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			scriptRole = scriptVerb.getRole(stringLocators[1]);
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,scriptRole,null),scriptRole.getRole().getEmotionScript(stringLocators[2]));
		case Consequence:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,null,null),scriptVerb.getConsequence(stringLocators[1]));
		case OptionAcceptable:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			scriptRole = scriptVerb.getRole(stringLocators[1]);
			scriptOption = scriptRole.getRole().getOption(stringLocators[2]);
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,scriptRole,scriptOption),scriptOption.getAcceptableScript());
		case OptionDesirable:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			scriptRole = scriptVerb.getRole(stringLocators[1]);
			scriptOption = scriptRole.getRole().getOption(stringLocators[2]);
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,scriptRole,scriptOption),scriptOption.getDesirableScript());			
		case Desirable:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			scriptRole = scriptVerb.getRole(stringLocators[1]);
			scriptOption = scriptRole.getRole().getOption(stringLocators[2]);
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,scriptRole,scriptOption),scriptOption.getWordSocket(stringLocators[3]).getDesirableScript());
		case Acceptable:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			scriptRole = scriptVerb.getRole(stringLocators[1]);
			scriptOption = scriptRole.getRole().getOption(stringLocators[2]);
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,scriptRole,scriptOption),scriptOption.getWordSocket(stringLocators[3]).getAcceptableScript());
		case OperatorBody:
			return new Pair<ScriptPath,Script>(new ScriptPath(null,null,null),((CustomOperator)operators.getOperator(stringLocators[0])).getBody());
		case WordsocketLabel:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,null,null),scriptVerb.getWordsocketTextScript(Verb.getWordSocketIndex(stringLocators[1])));
		case WordsocketSuffix:
			scriptVerb = getVerb(findVerb(stringLocators[0]));
			return new Pair<ScriptPath,Script>(new ScriptPath(scriptVerb,null,null),scriptVerb.getSuffix(Verb.getWordSocketIndex(stringLocators[1])));
		default:
			return null;
		}
	}

	/** Gets the trait names and descriptions from an array of traits. */
	private Pair<String[],String[]> getTraitNames(Iterable<FloatTrait> traits){
		int size=0;
		for(@SuppressWarnings("unused")	FloatTrait t:traits)
			size++;
		String[] tn = new String[size];
		String[] td = new String[size];
		int i=0;
		for(FloatTrait t:traits) {
			tn[i] = t.getLabel();
			td[i] = t.getDescription();
			i++;
		}
		return new Pair<String[],String[]>(tn,td);
	}

	/** @return the actor names without "Fate". */
	public String[] getActorNames(){
		String[] names = new String[getActorCount()-1];
		for(int i=1;i<getActorCount();i++)
			names[i-1]=getActor(i).getLabel();
		return names;
	}

	/** @return the stage names without "nowhere". */
	public String[] getStageNames(){
		String[] names = new String[getStageCount()-1];
		for(int i=1;i<getStageCount();i++)
			names[i-1]=getStage(i).getLabel();
		return names;
	}

	/** @return the prop names without "nothing". */
	public String[] getPropNames(){
		String[] names = new String[getPropCount()-1];
		for(int i=1;i<getPropCount();i++)
			names[i-1]=getProp(i).getLabel();
		return names;
	}

	/** Gets visible actor trait names and descriptions. */
	public Pair<String[],String[]> getVisibleActorTraitNames(){
		return getTraitNames(getVisibleTraits(TraitType.Actor));
	}
	/** Gets visible stage trait names and descriptions. */
	public Pair<String[],String[]> getVisibleStageTraitNames(){
		return getTraitNames(getVisibleTraits(TraitType.Stage));
	}
	/** Gets visible prop trait names and descriptions. */
	public Pair<String[],String[]> getVisiblePropTraitNames(){
		return getTraitNames(getVisibleTraits(TraitType.Prop));
	}
	
	/** Gets background information about an actor. */
	public BgItemData getActorBgData(String actorName,boolean serialized) {
		int i = getActorIndex(actorName);
		if (i<0)
			return null;
		final Actor actor = getActor(i);
		BgItemData.ImageGetter img = null;
		
		if (!serialized) {
			img = new BgItemData.ImageGetter(){
					public BufferedImage getImage() {
						if (actor.getImage(Deikto.this)!=null)
							return actor.getImage(Deikto.this).getBufferedImage();
						else 
							return null;
					}
				};
		} else {
			try {
				img = new SerializedImage(actor.getImageJPGBytes(this));
			} catch (IOException e){
				//e.printStackTrace();
			}			
		}
		return new BgItemData(actor.getLabel(),actor.getDescription(),img);
	}
	
	/** Gets background information about places. */
	public BgItemData getStageBgData(String stageName,boolean serialized) {
		int i = getStageIndex(stageName);
		if (i<0)
			return null;
		final Stage stage = getStage(i);
		BgItemData.ImageGetter img = null;
		
		if (!serialized) {
			img = new BgItemData.ImageGetter(){
					public BufferedImage getImage() {
						if (stage.getImage(Deikto.this)!=null)
							return stage.getImage(Deikto.this).getBufferedImage();
						else
							return null;
					}
				};
		} else {
			try {
				img = new SerializedImage(stage.getImageJPGBytes(this));
			} catch (IOException e){
				//e.printStackTrace();
			}			
		}
		return new BgItemData(stage.getLabel(),stage.getDescription(),img);
	}

	/** Gets background information about props. */
	public BgItemData getPropBgData(String propName,boolean serialized) {
		int i = getPropIndex(propName);
		if (i<0)
			return null;
		final Prop prop = getProp(i);
		BgItemData.ImageGetter img = null;
		
		if (!serialized) {
			img = new BgItemData.ImageGetter(){
					public BufferedImage getImage() {
						if (prop.getImage(Deikto.this)!=null)
							return prop.getImage(Deikto.this).getBufferedImage();
						else
							return null;
					}
				};
		} else {
			try {
				img = new SerializedImage(prop.getImageJPGBytes(this));
			} catch (IOException e){
				//e.printStackTrace();
			}			
		}
		return new BgItemData(prop.getLabel(),prop.getDescription(),img);
	}

	/** Returns the name and descriptions of the actor traits defined in this storyworld. */
	public Pair<String[],String[]> getRelationshipNames(){
		if (!areRelationshipsVisible())
			return null;
		
		ArrayList<FloatTrait> traits = getActorTraits();
		// count visible relationships
		int count = 0;
		for(FloatTrait t:traits)
			if (isRelationshipVisible(t))
				count++;
		String[] names = new String[count];
		String[] descriptions = new String[count];
		int i = 0;
		for(FloatTrait t:traits) {
			if (isRelationshipVisible(t)) {
				names[i] = t.getLabel();
				descriptions[i++] = t.getDescription();
			}
		}
		
		return new Pair<String[],String[]>(names,descriptions); 
	}

	/** 
	 * Returns the values for a given relationship of actors.
	 * Fate relations are not returned here. 
	 * */
	public float[][] getRelationshipValues(String relationshipName){
		FloatTrait t = getTrait(TraitType.Actor,relationshipName);
		// count known actors
		Actor protagonist = getActor(1);
		int knownActorCount = 0;
		for(int i=1;i<getActorCount();i++) {
			if (getActor(i).getKnowsMe(protagonist))
				knownActorCount++;
		}
		int actorCount = getActorCount();
		float[][] values = new float[knownActorCount][knownActorCount];
		int iAcc=0;
		for(int i=1;i<actorCount;i++) {
			if (getActor(i).getKnowsMe(protagonist)) {
				int jAcc=0;
				for(int j=1;j<actorCount;j++) {
					if (getActor(j).getKnowsMe(protagonist))
						values[iAcc][jAcc++] = getActor(i).getP(t, getActor(j));
				}
				iAcc++;
			}
		}
		return values; 
	}

	/**
	 * Class for signaling different kind of errors when writing 
	 * a storyworld to the filesystem. Use the {@link #t} to 
	 * find out the exact cause of the error.
	 * */
	public static class WritingException extends IOException {
		private static final long serialVersionUID = 1L;
		public static enum Type {
			/** Raised when there exists a file with the same name than
			 * the resource directory and can not be deleted.
			 * */
			ResourceDirFileDeletion,
			/** Raised when the resource directory could not be created.
			 * This is most likely due to a problem with permissions.
			 * */
			ResourceDirCreation,
			/** Raised when the empty resource directory could not be deleted.
			 * This is most likely due to a problem with permissions.
			 * */
			ResourceDirDeletion,
			/** Raised when a given unreferenced file in the
			 * resource directory can not be deleted.
			 * */
			ResourceDeletion
		} 
		public Type t;
		public File f;
		public WritingException(Type t,File f){
			this.t = t;
			this.f = f;
		};
	}

	/**
	 * Class for signaling different kind of errors when reading 
	 * a storyworld. Use the {@link #t} to 
	 * find out the exact cause of the error.
	 * */
	public static class ReadingException extends Exception {
		private static final long serialVersionUID = 1L;
		public static enum Type {
			/** Raised when an Operator does not exist. */
			OperatorDoesNotExist,
			/** Raised when a trait in a word description does not exist. */
			WordDescriptionTraitDoesNotExist,
			/** 
			 * Raised when a trait in a word description from one actor
			 * towards another word does not exist.
			 * */
			WordDescriptionPTraitDoesNotExist
		} 
		public Type t;
		public String s0;
		public String s1;
		public String s2;
		public ReadingException(Type t,String s0,String s1){
			this(t,s0,s1,null);
		}
		public ReadingException(Type t,String s0,String s1,String s2){
			super(t.name()+": "+s0+" | "+s1+" | "+s2);
			this.t = t;
			this.s0 = s0;
			this.s1 = s1;
			this.s2 = s2;
		};
	}

	/** Limits for Deikto model. */
	public static class Limits {
		public int maximumActorCount=100;
		public int maximumStageCount=80;
		public int maximumPropCount=150;
		public int maximumVerbCount=500;
		public int maximumRoleCount=2500;
		public int maximumOptionCount=10000;
		public int maximumTraitCount=30;
		public int maximumTextTraitCount=20;
		public int maximumScriptNodeCount=500000;
		public int maximumCustomOperatorCount=100;
	}
	
	/** Saves the world state. */
	public void saveState(ObjectOutput out) throws IOException {
		// saving actor state
		for(Actor actor:getActors()){
			out.writeBoolean(actor.getActive());
			out.writeBoolean(actor.getFemale());
			out.writeBoolean(actor.getDontMoveMe());
			out.writeBoolean(actor.getUnconscious());
			out.writeInt(actor.getLocation());
			out.writeInt(actor.getSpyingOn());
			out.writeInt(actor.getTargetStage());
			out.writeInt(actor.getOccupiedUntil());
			out.writeInt(actor.getCBored());
			out.writeInt(actor.getHowLongToSpy());
		
			for(Actor.MoodTrait t:Actor.MoodTraits)
				out.writeFloat(actor.get(t));
			for(Actor.TraitType tt:Actor.TraitType.values())
				for(FloatTrait t:getActorTraits())
					out.writeFloat(actor.get(tt,t));
			for(TextTrait t:getTextTraits(TraitType.Actor))
				out.writeUTF(Utils.emptyIfNull(actor.getText(t)));

			for(Actor actor2:getActors()) {
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getActorTraits())
						out.writeFloat(actor.get(tt,t,actor2));
				for(ExtraTrait t:Actor.ExtraTraits)
					out.writeFloat(actor.get(t,actor2));
			}

			for(Actor actor2:getActors()) 
				out.writeBoolean(actor.getKnowsMe(actor2));

			for(Prop prop:getProps())
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getPropTraits())
						out.writeFloat(actor.get(tt,t,prop));

			for(Stage stage:stages) 
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getStageTraits())
						out.writeFloat(actor.get(tt,t,stage));

			out.writeInt(actor.plans.size());
			for(Sentence plan:actor.plans) 
				out.writeObject(plan);
		}
		
		// saving prop state
		for(Prop prop:getProps()){
			out.writeBoolean(prop.getCarried());
			out.writeBoolean(prop.getVisible());
			out.writeBoolean(prop.getInPlay());
			out.writeInt(prop.getOwner());
			out.writeInt(prop.getLocation());

			for(Actor actor:getActors()) 
				out.writeBoolean(prop.getKnowsMe(actor));
			for(FloatTrait t:traits.get(TraitType.Prop))
				out.writeFloat(prop.getTrait(t));
			for(TextTrait t:textTraits.get(TraitType.Prop))
				out.writeUTF(Utils.emptyIfNull(prop.getText(t)));
		}
		
		// saving stage state
		for(Stage stage:getStages()){
			out.writeBoolean(stage.getDoorOpen());
			out.writeInt(stage.getPopulation());
			out.writeInt(stage.getOwner());
			out.writeFloat(stage.getXCoord());
			out.writeFloat(stage.getYCoord());
			
			for(Actor actor:getActors()) { 
				out.writeFloat(stage.getUnwelcoming_Homey(actor));
				out.writeBoolean(stage.getKnowsMe(actor));
			}
			for(FloatTrait t:traits.get(TraitType.Stage))
				out.writeFloat(stage.getTrait(t));
			for(TextTrait t:textTraits.get(TraitType.Stage))
				out.writeUTF(Utils.emptyIfNull(stage.getText(t)));
		}
	}
	/** Checks the size of a byte array to be below a reasonable limit. */
	public static void checkByteArraySize(long bytes) throws RuntimeException {
		if (bytes>STATE_BYTE_ARRAY_MAXIMUM_SIZE)
			throw new RuntimeException("Too big array. Length: "+bytes);
	}
	/** Loads the world state. */
	public void loadState(ObjectInput in) throws IOException {
		// load actor state
		for(Actor actor:getActors()){
			actor.setActive(in.readBoolean());
			actor.setFemale(in.readBoolean());
			actor.setDontMoveMe(in.readBoolean());
			actor.setUnconscious(in.readBoolean());
			actor.setLocation(getStage(in.readInt()));
			actor.setSpyingOn(getActor(in.readInt()));
			actor.setTargetStage(getStage(in.readInt()));
			actor.setOccupiedUntil(in.readInt());
			actor.setCBored(in.readInt());
			actor.setHowLongToSpy(in.readInt());
			
			for(Actor.MoodTrait t:Actor.MoodTraits)
				actor.set(t,in.readFloat());
			for(Actor.TraitType tt:Actor.TraitType.values())
				for(FloatTrait t:getActorTraits())
					actor.set(tt,t,in.readFloat());
			for(TextTrait t:getTextTraits(TraitType.Actor))
				actor.setText(t,in.readUTF());
			
			for(Actor actor2:getActors()) {
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getActorTraits())
						actor.set(tt,t,actor2,in.readFloat());
				for(ExtraTrait t:Actor.ExtraTraits)
					actor.set(t,actor2,in.readFloat());
			}

			for(Actor actor2:getActors()) 
				actor.setKnowsMe(actor2,in.readBoolean());

			for(Prop prop:getProps())
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getPropTraits())
						actor.set(tt,t,prop,in.readFloat());

			for(Stage stage:stages) 
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getStageTraits())
						actor.set(tt,t,stage,in.readFloat());

			int size = in.readInt();
			checkByteArraySize(size*16);
			actor.plans.clear();
			try {
				for(int i=0;i<size;i++)
					actor.plans.add((Sentence)in.readObject());
			} catch (ClassNotFoundException e){
				throw new RuntimeException(e);
			}
		}

		// loading prop state
		for(Prop prop:getProps()){
			prop.setCarried(in.readBoolean());
			prop.setVisible(in.readBoolean());
			prop.setInPlay(in.readBoolean());
			prop.setOwner(getActor(in.readInt()));
			prop.setLocation(getStage(in.readInt()));

			for(Actor actor:getActors()) 
				prop.setKnowsMe(actor,in.readBoolean());
			for(FloatTrait t:traits.get(TraitType.Prop))
				prop.setTrait(t,in.readFloat());
			for(TextTrait t:getTextTraits(TraitType.Prop))
				prop.setText(t,in.readUTF());
		}
		
		// loading stage state
		for(Stage stage:getStages()){
			stage.setDoorOpen(in.readBoolean());
			stage.setPopulation(in.readInt());
			stage.setOwner(getActor(in.readInt()));
			stage.setXCoord(in.readFloat());
			stage.setYCoord(in.readFloat());
			
			for(Actor actor:getActors()) { 
				stage.setUnwelcoming_Homey(actor,in.readFloat());
				stage.setKnowsMe(actor,in.readBoolean());
			}
			for(FloatTrait t:traits.get(TraitType.Stage))
				stage.setTrait(t,in.readFloat());
			for(TextTrait t:getTextTraits(TraitType.Stage))
				stage.setText(t,in.readUTF());
		}
	}
	
	public void assertEqualStates(Deikto dk){
		// check actor state
		int iActor2 = 0;
		for(Actor actor:getActors()){
			Actor actor2 = dk.getActor(iActor2++);
			assert actor.getLabel()==actor2.getLabel();

			assert actor.getActive()==actor2.getActive();
			assert actor.getFemale()==actor2.getFemale();
			assert actor.getDontMoveMe()==actor2.getDontMoveMe();
			assert actor.getUnconscious()==actor2.getUnconscious();
			assert actor.getLocation()==actor2.getLocation();
			assert actor.getSpyingOn()==actor2.getSpyingOn();
			assert actor.getTargetStage()==actor2.getTargetStage();
			assert actor.getOccupiedUntil()==actor2.getOccupiedUntil():"OccupiedUntil "+actor.getLabel()+" "+actor2.getLabel()+": "+actor.getOccupiedUntil()+"=/="+actor2.getOccupiedUntil();
			assert actor.getCBored()==actor2.getCBored();
			assert actor.getHowLongToSpy()==actor2.getHowLongToSpy();
			
			for(Actor.MoodTrait t:Actor.MoodTraits)
				assert actor.get(t)==actor2.get(t);
			for(Actor.TraitType tt:Actor.TraitType.values())
				for(FloatTrait t:getActorTraits())
					assert actor.get(tt,t)==actor2.get(tt,t);
			for(TextTrait t:getTextTraits(TraitType.Actor))
				assert actor.getText(t)==actor2.getText(t);
			
			int iPActor2 = 0;
			for(Actor pactor:getActors()) {
				Actor pactor2 = dk.getActor(iPActor2++);
				assert pactor.getLabel()==pactor2.getLabel();
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getActorTraits()) {
						assert t.getValuePosition()==dk.getTrait(TraitType.Actor, t.getLabel()).getValuePosition();
						assert actor.get(tt,t,pactor)==actor2.get(tt,t,pactor2):
							"Distinct actor ptraits. "+actor.getLabel()+"."+tt+t.getLabel()+"["+pactor.getLabel()+"]"+
							": "+actor.get(tt,t,pactor)+"=/="+actor2.get(tt,t,pactor2);
							;
					}
				for(ExtraTrait t:Actor.ExtraTraits)
					assert actor.get(t,pactor)==actor2.get(t,pactor2);
			}
			
			int ikActor2 = 0;
			for(Actor kactor1:getActors()) {
				Actor kactor2 = dk.getActor(ikActor2++);
				assert actor.getKnowsMe(kactor1)==actor2.getKnowsMe(kactor2);
			}


			int iProp2 = 0;
			for(Prop prop:getProps()) {
				Prop prop2 = dk.getProp(iProp2++);
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getPropTraits())
						assert actor.get(tt,t,prop)==actor2.get(tt,t,prop2);
			}

			int iStage2 = 0;
			for(Stage stage:stages) {
				Stage stage2 = dk.getStage(iStage2);
				for(Actor.PTraitType tt:Actor.PTraitType.values())
					for(FloatTrait t:getStageTraits())
						assert actor.get(tt,t,stage)==actor2.get(tt,t,stage2);
			}

			assert actor.plans.size()==actor2.plans.size():"different plans for "+actor.getLabel()+" and "+actor2.getLabel()+": "+actor.plans.size()+" =/= "+actor2.plans.size();
	
			int i=0;
			for(Sentence s:actor.plans)
				s.assertEquals(actor2.plans.get(i++));
		}
		
		// loading prop state
		int iProp2 = 0;
		for(Prop prop:getProps()){
			Prop prop2 = dk.getProp(iProp2++);
			assert prop.getCarried()==prop2.getCarried();
			assert prop.getVisible()==prop2.getVisible();
			assert prop.getInPlay()==prop2.getInPlay();
			assert prop.getOwner()==prop2.getOwner();
			assert prop.getLocation()==prop2.getLocation();

			int iActor = 0;
			for(Actor actor:getActors()) {
				Actor actor2 = dk.getActor(iActor++);
				assert prop.getKnowsMe(actor)==prop2.getKnowsMe(actor2);
			}
			for(FloatTrait t:traits.get(TraitType.Prop))
				assert prop.getTrait(t)==prop2.getTrait(t);
			for(TextTrait t:getTextTraits(TraitType.Prop))
				assert prop.getText(t)==prop2.getText(t);
		}
		
		// loading stage state
		int iStage2 = 0;
		for(Stage stage:getStages()){
			Stage stage2 =  dk.getStage(iStage2++);
			assert stage.getDoorOpen()==stage2.getDoorOpen();
			assert stage.getPopulation()==stage2.getPopulation();
			assert stage.getOwner()==stage2.getOwner();
			assert stage.getXCoord()==stage2.getXCoord();
			assert stage.getYCoord()==stage2.getYCoord();
			
			int iActor = 0;
			for(Actor actor:getActors()) {
				Actor actor2 = dk.getActor(iActor++);
				assert stage.getUnwelcoming_Homey(actor)==stage2.getUnwelcoming_Homey(actor2);
				assert stage.getKnowsMe(actor)==stage2.getKnowsMe(actor2);
			}
			for(FloatTrait t:traits.get(TraitType.Stage))
				assert stage.getTrait(t)==stage2.getTrait(t);
			for(TextTrait t:getTextTraits(TraitType.Stage))
				assert stage.getText(t)==stage2.getText(t);
		}		
	}

}

