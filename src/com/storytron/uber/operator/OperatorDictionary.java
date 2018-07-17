package com.storytron.uber.operator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import Engine.enginePackage.Interpreter;

import com.storytron.enginecommon.Utils;
import com.storytron.uber.operator.Operator.OpType;


/**
 * Class for handling operators and groups of them.
 * Operators are grouped into menus.
 * <p>
 * All dictionaries share certain common operators which are defined in the file
 * OperatorList.xml. The method {@link #loadOperators()} must be called to load
 * the common operators before creating {@link OperatorDictionary} instances,
 * otherwise the instance will be initially empty.  
 */
public final class OperatorDictionary implements Iterable<Operator> {
	
	/** Lists all the menus of operators. */
	public static enum Menu {
		Text,
		Arithmetic,
		Logical,
		Global,
		ThisEvent,
		PastEvent,
		Chosen,
		Actor,
		Mood,
		Weight,
		Perception,
		Confidence,
		PWeight,
		Prop,
		Stage,
		Verb,
		History,
		Picking,
		Word,
		Socket,
		HypotheticalEvent,
		Boxes,
		Custom,

		SetGlobal,
		SetActor,
		SetActorP,
		SetActorC,
		SetProp,
		SetStage,
		Alarms,
		FillBoxes,

		EmotionalReaction,
		Parameter,

		NONE
	}

	/** Creates an operator dictionary containing the common operators. */
	public OperatorDictionary(){
		for(Menu mt:Menu.values()) {
			LinkedList<Operator> ops = getGlobalOperators(mt);
			menuGroups.put(mt, new LinkedList<Operator>(ops));
			for(Operator op:ops) {
				operators.put(op.getLabel(),op);
			}
		}
	}

	/** Added this ranges to distinguish classes of operator menus. */
	public final static EnumSet<Menu> ScriptMenus = EnumSet.range(Menu.Text, Menu.Custom);
	public final static EnumSet<Menu> ConsequenceMenus = EnumSet.range(Menu.SetGlobal, Menu.FillBoxes);
	public final static EnumSet<Menu> EmotionMenus = EnumSet.range(Menu.EmotionalReaction, Menu.EmotionalReaction);

	private static final Map<Menu,LinkedList<Operator>> globalMenuGroups = new EnumMap<Menu,LinkedList<Operator>>(Menu.class);
	private static Map<String,Operator> globalOperators = new HashMap<String,Operator>(900);
	
	private static final Map<String,Operator> globalUndefinedOperators = new HashMap<String,Operator>(20);
	
	private final Map<String,Operator> operators = new HashMap<String,Operator>(900);
	private final Map<Menu,List<Operator>> menuGroups = new EnumMap<Menu,List<Operator>>(Menu.class);

	/**
	 * Loads operators from a default file "OperatorList.xml".
	 * If this method is called more than once, operators
	 * are loaded only the first time. 
	 * */
	public static void loadOperators() throws IOException, 
									ParserConfigurationException, SAXException { 
		if (!loaded) {
			loadOperators("OperatorList.xml");
			loaded = true;
		}
	}
	private static boolean loaded = false; 
	
	private static Operator desirableOperator=null;
	private static Operator optionAcceptableOperator=null;
	private static Operator abortIfOperator=null;
	private static Operator wordsocketTextOperator=null;
	private static Operator assumeRoleIfOperator=null;
	private static Operator undefinedBNumberOperator=null;
	private static Operator undefinedBooleanOperator=null;
	private static Operator bnumberConstantOperator=null;
	private static Operator numberConstantOperator=null;
	private static Operator textConstantOperator=null;
	private static Operator trueOperator=null;
	private static Operator falseOperator=null;
	private static Operator undefinedActor=null;
	private static Operator undefinedProp=null;
	private static Operator undefinedStage=null;
	private static Operator undefinedVerb=null;
	private static Operator actorConstantOperator=null;
	private static Operator propConstantOperator=null;
	private static Operator stageConstantOperator=null;
	private static Operator verbConstantOperator=null;
	private static Operator sameAsThisOneOperator=null;
	private static Operator theNameOperator=null;
	private static Operator thisSubjectOperator=null;
	private static Operator nominativePronounOperator=null;
	private static Operator genitivePronounOperator=null;
	private static Operator accusativePronounOperator=null;
	private static Operator reflexivePronounOperator=null;
	private static EnumMap<Operator.Type,Operator> allWordsWhich = new EnumMap<Operator.Type,Operator>(Operator.Type.class);
	private static EnumMap<Operator.Type,Operator> acceptableOps = new EnumMap<Operator.Type,Operator>(Operator.Type.class);
	
	/**
	 * Loads operators from a file on disk.
	 * @param filename name of the file to load
	 * */
	private static void loadOperators(String filename) 
			throws IOException, ParserConfigurationException, SAXException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;

		builder = factory.newDocumentBuilder();
		
		Document opListDoc = null;

		InputStream is = new BufferedInputStream(Utils.getDataURL(filename).openStream());
		opListDoc = builder.parse(is);
		is.close();
		
		// version control
		for(Menu m:Menu.values())
			globalMenuGroups.put(m,new LinkedList<Operator>());
		
		NodeList opList = opListDoc.getElementsByTagName("Operator");
		for (int i = 0; (i < opList.getLength()); ++i) {
			Node opNode = opList.item(i);
			NamedNodeMap attMap = opNode.getAttributes();
	 		Node junk1 = attMap.getNamedItem("Label");
	 		int cArgs = Integer.parseInt(attMap.getNamedItem("arg_count").getNodeValue());
	 		Node junk2 = attMap.getNamedItem("iterator");
	 		Operator.Type iteratorType = junk2==null
	 									? null
	 									: junk2.getNodeValue().equals("Undefined")
	 									? Operator.Type.UnType
	 									: Operator.Type.valueOf(junk2.getNodeValue());
	 		String codeLabel="", dataType=null, tooltipText=null;
	 		Operator.OpType operatorType=null;
	 		Menu menu=Menu.NONE;
	 		NodeList childNodes = opNode.getChildNodes();
	 		LinkedList<NamedNodeMap> nodeMaps = new LinkedList<NamedNodeMap>(); 
	 		for (int j = 0; (j < childNodes.getLength()); ++j) {
 				if ((childNodes.item(j).getNodeName()).equals("CodeLabel"))
 					codeLabel=childNodes.item(j).getTextContent();
 				else if ((childNodes.item(j).getNodeName()).equals("OperatorType"))
 					operatorType=OpType.valueOf(childNodes.item(j).getTextContent());
 				else if ((childNodes.item(j).getNodeName()).equals("DataType"))
 					dataType=childNodes.item(j).getTextContent();
 				else if ((childNodes.item(j).getNodeName()).equals("Argument"))
 					nodeMaps.add(childNodes.item(j).getAttributes());
 				else if ((childNodes.item(j).getNodeName()).equals("Menu"))
					menu=Menu.valueOf(childNodes.item(j).getTextContent());
 				else if ((childNodes.item(j).getNodeName()).equals("ToolTipText"))
 					tooltipText=childNodes.item(j).getTextContent();
	 		}
	 		
	 		Operator operator = OperatorFactory.createOperator(junk1.getNodeValue(), cArgs,Interpreter.getMethod(operatorType,codeLabel));
	 		operator.setIteratorType(iteratorType);
 			if (operatorType!=null)
 				operator.setOperatorType(operatorType);
 			if (dataType!=null)
 				operator.setDataType(Operator.Type.valueOf(dataType));
	 		int ziArg = 0;
 			for(NamedNodeMap zNamedNodeMap:nodeMaps) {
				Node zNode = zNamedNodeMap.getNamedItem("Default");
				operator.addArgument(Operator.Type.valueOf(zNamedNodeMap.getNamedItem("DataType").getNodeValue()),
											zNamedNodeMap.getNamedItem("Label").getNodeValue(),
											zNode==null?"":zNode.getNodeValue());
	   			++ziArg;
 			}
 			operator.setMenu(menu);
 			if (tooltipText!=null)
 				operator.setMyToolTipText(tooltipText);

	 		if (operator.getLabel().startsWith("?"))
	 			globalUndefinedOperators.put(operator.getLabel(),operator);
	 			
	 		globalOperators.put(operator.getLabel(),operator);
	 		globalMenuGroups.get(menu).add(operator);
		}
		
		for(Operator.Type wordsocketType:Operator.WORDSOCKET_TYPES) {
			Operator gOp = OperatorFactory.getAllWordsWhichOperator(wordsocketType);
			addGlobalOperator(gOp);
	 		allWordsWhich.put(wordsocketType,gOp);
	 		
	 		Operator aOp = OperatorFactory.getAcceptableOperator(wordsocketType);
	 		addGlobalOperator(aOp);
 			acceptableOps.put(wordsocketType,aOp);
 			
	 		addGlobalOperator(OperatorFactory.getGroupNOperator(wordsocketType,2));
	 		addGlobalOperator(OperatorFactory.getGroupNOperator(wordsocketType,3));
	 		addGlobalOperator(OperatorFactory.getWord2GroupOperator(wordsocketType));
	 		addGlobalOperator(OperatorFactory.getWordNameOperator(wordsocketType));
		}

 		addGlobalOperator(OperatorFactory.getGroupNElemsOperator(Operator.Type.Quantifier,3));
 		addGlobalOperator(OperatorFactory.getGroupNElemsOperator(Operator.Type.Quantifier,5));
 		addGlobalOperator(OperatorFactory.getGroupNElemsOperator(Operator.Type.Quantifier,7));
 		addGlobalOperator(OperatorFactory.getGroupNElemsOperator(Operator.Type.Quantifier,9));

		desirableOperator=globalOperators.get("Desirable");
		optionAcceptableOperator=globalOperators.get("OptionAcceptable");
		abortIfOperator=globalOperators.get("AbortIf");
		wordsocketTextOperator=globalOperators.get("WordsocketText");
		assumeRoleIfOperator=globalOperators.get("AssumeRoleIf");
		undefinedBNumberOperator=globalOperators.get("?BNumber?");
		undefinedBooleanOperator=globalOperators.get("?Boolean?");
		bnumberConstantOperator=globalOperators.get("BNumberConstant");
		numberConstantOperator=globalOperators.get("NumberConstant");
		textConstantOperator=globalOperators.get("TextConstant");
		trueOperator=globalOperators.get("true");
		falseOperator=globalOperators.get("false");
		undefinedActor=globalOperators.get("?Actor?");
		undefinedProp=globalOperators.get("?Prop?");
		undefinedStage=globalOperators.get("?Stage?");
		undefinedVerb=globalOperators.get("?Verb?");
		actorConstantOperator=globalOperators.get("ActorConstant");
		propConstantOperator=globalOperators.get("PropConstant");
		stageConstantOperator=globalOperators.get("StageConstant");
		verbConstantOperator=globalOperators.get("VerbConstant");
		sameAsThisOneOperator=globalOperators.get("SameAsThisOne");
		theNameOperator=globalOperators.get("TheName");
		thisSubjectOperator=globalOperators.get("ThisSubject");
		nominativePronounOperator=globalOperators.get("NominativePronoun");
		genitivePronounOperator=globalOperators.get("GenitivePronoun");
		accusativePronounOperator=globalOperators.get("AccusativePronoun");
		reflexivePronounOperator=globalOperators.get("ReflexivePronoun");
	}
	
	private static void addGlobalOperator(Operator op){
 		globalOperators.put(op.getLabel(),op);
 		globalMenuGroups.get(op.getMenu()).add(op);
	}
	
	
	/** @return the operators shared by all the storyworlds. */
	private static LinkedList<Operator> getGlobalOperators(Menu m){ 
		return globalMenuGroups.get(m); 
	}
	
	/**
	 * Adds an operator to the dictionary.
	 * Make sure that there is not already an operator
	 * with the same name. 
	 */
	public void addOperator(Operator o){
 		operators.put(o.getLabel(),o);
 		menuGroups.get(o.getMenu()).add(o); 
 	};

 	/**
	 * Removes an operator from the dictionary.
	 * Does nothing if there is not an operator with the given name. 
	 */
	public void removeOperator(String label){
		Operator o=operators.remove(label);
		menuGroups.get(o.getMenu()).remove(o);
	};

	/** Updates the label key which is used to search for an operator. */
	public void renameOperator(String label,String newLabel){
		Operator op=operators.remove(label);
		if (op==null)
			throw new NullPointerException("No operator with label: "+label);
		operators.put(newLabel,op);
	};

	/** Returns the operators in a given menu group. */
	public List<Operator> getOperators(Menu m){ return menuGroups.get(m); };

	/**
	 * <p>Returns the index of the operator with name <code>tLabel</code>
	 * in the menu <code>m</code>. This index would be usually different
	 * from the one in the overall operator collection.</p>
	 * <p>This method is used to retrieve the indexes of operators
	 * in a menu so that they can be reordered based on the indexes 
	 * (see {@link #moveOperatorMenu(com.storytron.uber.operator.OperatorDictionary.Menu, int, int)}).</p>
	 * <p>The index of an operator might change anytime an operator is
	 * reordered or deleted.</p> 
	 * @return the index of the operator in the menu <code>m</code>, or
	 * -1 in case that the operator is not found in that menu.
	 * */
	public int getOperatorMenuIndex(Menu m,String tLabel){
		int i=0;
		for(Operator o:menuGroups.get(m))
			if (o.getLabel().equals(tLabel)) return i;
			else i++;
		return -1;
	};

	/**
	 * <p>Change position of a given operator from index <code>from</code>
	 * to index <code>to</code>. Get the index of an operator in a given 
	 * menu using {@link #getOperatorMenuIndex(com.storytron.uber.operator.OperatorDictionary.Menu, String)}</p>
	 * */
	public void moveOperatorMenu(Menu m,int from,int to){
		menuGroups.get(m).add(to,menuGroups.get(m).remove(from));
	}
	
	/** Implementation for interface Iterable. */
	public Iterator<Operator> iterator() {
		return operators.values().iterator();
	}

	/** Find an operator using its name. */
	public Operator getOperator(String name){
		return operators.get(name);
	}

	/** Find an operator using its name of the form "?label?". */
	public static Operator getUndefinedOperator(String name){
		return globalUndefinedOperators.get(name);
	}
	
	/** 
	 * Find a global operator using its name.
	 * <p>
	 * This is provided to narrow the lookups when the caller knows that
	 * the searched operator is a global one. Also allows to search for
	 * operators when an operator instance is not at hand.  
	 * */
	public static Operator getGlobalOperator(String name){
		return globalOperators.get(name);
	}

	/** Methods to search fast for common operators. These are used to 
	 * improve loading time of storyworlds. */
	public static Operator getDesirableOperator(){ return desirableOperator; }
	public static Operator getOptionAcceptableOperator(){ return optionAcceptableOperator; }
	public static Operator getAbortIfOperator(){ return abortIfOperator; }
	public static Operator getWordsocketTextOperator(){ return wordsocketTextOperator; }
	public static Operator getAssumeRoleIfOperator(){ return assumeRoleIfOperator; }
	public static Operator getUndefinedBooleanOperator(){ return undefinedBooleanOperator; }
	public static Operator getUndefinedBNumberOperator(){ return undefinedBNumberOperator; }
	public static Operator getBNumberConstantOperator(){ return bnumberConstantOperator; }
	public static Operator getNumberConstantOperator(){ return numberConstantOperator; }
	public static Operator getTextConstantOperator(){ return textConstantOperator; }
	public static Operator getTrueOperator(){ return trueOperator; }
	public static Operator getFalseOperator(){ return falseOperator; }
	public static Operator getUndefinedActorOperator(){ return undefinedActor; }
	public static Operator getUndefinedPropOperator(){ return undefinedProp; }
	public static Operator getUndefinedStageOperator(){ return undefinedStage; }
	public static Operator getUndefinedVerbOperator(){ return undefinedVerb; }
	public static Operator getActorConstantOperator(){ return actorConstantOperator; }
	public static Operator getPropConstantOperator(){ return propConstantOperator; }
	public static Operator getStageConstantOperator(){ return stageConstantOperator; }
	public static Operator getVerbConstantOperator(){ return verbConstantOperator; }
	public static Operator getSameAsThisOneOperator(){ return sameAsThisOneOperator; }
	public static Operator getTheNameOperator(){ return theNameOperator; }
	public static Operator getThisSubjectOperator(){ return thisSubjectOperator; }
	public static Operator getNominativePronounOperator(){ return nominativePronounOperator; }
	public static Operator getGenitivePronounOperator(){ return genitivePronounOperator; }
	public static Operator getAccusativePronounOperator(){ return accusativePronounOperator; }
	public static Operator getReflexivePronounOperator(){ return reflexivePronounOperator; }
	public static Operator getAllWordsWhichOperator(Operator.Type t){ return allWordsWhich.get(t); }
	public static Operator getAcceptableOperator(Operator.Type t){ return acceptableOps.get(t); }

}
