package com.storytron.uber;
import java.io.Serializable;
import java.util.ArrayList;

import com.storytron.uber.Role.Option;
import com.storytron.uber.Role.Option.OptionWordSocket;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;

/** 
  This class represents a verb.
  A verb expresses all the variations in which a certain action can occur.
  It is the root of a hierarchy of conditions and behaviors that 
  comprehends {@link Role}s, {@link Option}s and {@link OptionWordSocket}s.
  {@link Script}s are intensively used to tell those conditions and behaviors.  
  <p>
  The verb manages a collections of {@link Role}s and a collection of consequence
  {@link Script}s. 
  <p>
  Roles are manipulated through the methods
  {@link #addRole(Deikto, Role)}, {@link #addRole(Deikto, int, Role)},
  {@link #addRole(Deikto, String, boolean)}, {@link #deleteRole(Deikto, int)},
  {@link #getRoleCount()}, {@link #getRoleIndex(Role)}, {@link #getRole(int)},
  {@link #getRoleIndex(String)}, {@link #getRoles()}.
  <p>
  Consequences are scripts executed when an event of the verb happens.
  They can be manipulated through the methods
  {@link #addConsequence(Operator)}, {@link #addConsequence(Script)},
  {@link #addConsequence(int, Script)}, {@link #getConsequence(int)},
  {@link #getConsequence(String)}, {@link #getConsequenceCount()},
  {@link #getConsequenceIndex(String)}, {@link #getConsequences()}.
  <p>
  The following attributes can be set through getters and setters:
    hijackable, occupiesDirObject, timeToPrepare, timeToExecute, audience,
	category, expression, trivial_Momentous, description, abortScript.
  <p>
	For each {@link Sentence wordsocket}, the Verb tells if the wordsocket is
	used with the method {@link #isWordSocketActive(int)} and
	controls activation of the wordsocket with the method 
	{@link #setWordSocketIsActive(int, boolean)}.
 * */
public final class Verb extends Word implements Cloneable, Serializable {
	private static final long serialVersionUID = 1l;
	/** Specifies who is acknowledged of the verb when it is performed. */
	public static enum Witnesses {
		ANYBODY_ON_STAGE,
		EVERYBODY_EVERYWHERE,
		SUBJECT_ONLY,
		NOBODY_AT_ALL,
		CUSTOM_SPEC
	}
	/** Specifies who must be present when the verb is performed. */
	public static enum Presence {
		REQUIRED,
		NOT_REQUIRED,
		ABSENT
	}
	/** Class which groups all the data related to an active wordsocket. */
	public static class WSData implements Cloneable {
		public boolean visible = true;
		public boolean witness = true;
		public Presence presence = Presence.REQUIRED;
		public Operator.Type type;
		public Script text;
		public Script suffix;
		public String note;
		public WSData(Operator.Type type) {
			this.type = type;
		}
		public WSData clone(){
			try {
				WSData newData = (WSData)super.clone();
				if (text!=null)
					newData.text = text.clone();
				if (suffix!=null)
					newData.suffix = suffix.clone();
				return newData;
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	private boolean hijackable, occupiesDirObject;
	private int timeToPrepare;
	private int timeToExecute;
	private Witnesses witnesses;
	private String category;
	private String expression;
	private WSData[] socketData;
	private float trivial_Momentous;
	private ArrayList<Role.Link> roles = new ArrayList<Role.Link>();
	private ArrayList<Script> consequences = new ArrayList<Script>();
	private String description;
	private Script abortScript;
	//**********************************************************************
	public Verb(String tLabel) {	
	  super(tLabel);
	  socketData = new WSData[Sentence.MaxWordSockets];
	  socketData[0] = new WSData(Operator.Type.Actor); // Subject is always active
	  socketData[1] = new WSData(Operator.Type.Verb); // Verb is always active
	  hijackable = false;
	  occupiesDirObject = true;
	  timeToPrepare = 1;
	  timeToExecute = 1;
	  witnesses = Witnesses.ANYBODY_ON_STAGE;
	  category = "Other";
	  expression = "null";
	  trivial_Momentous = 0.0f;
	}
	
	/** Clones the verb. */
	public Verb clone(boolean cloneRoles){
		Verb v = (Verb)super.clone();
		v.socketData = v.socketData.clone();
		v.roles = new ArrayList<Role.Link>(v.roles);
		if (cloneRoles) {
			for(int i=0;i<v.roles.size();i++)
				v.roles.set(i,v.roles.get(i).clone(true));
		} else {
			for(int i=0;i<v.roles.size();i++)
				v.roles.set(i,new Role.Link(v.roles.get(i)));
		}
		v.consequences = new ArrayList<Script>(v.consequences);
		for(int i=0;i<v.consequences.size();i++) 
			v.consequences.set(i,v.consequences.get(i).clone());
		
		if (abortScript!=null)
			v.abortScript = abortScript.clone();
		v.socketData = socketData.clone();
		for(int i=0;i<socketData.length;i++) {
			if (socketData[i]!=null)
				v.socketData[i] = socketData[i].clone();
		}
		
		return v;
	}
	
//	**********************************************************************	
	void addRole(int pos, Role.Link r) {
		roles.add(pos,r);
	}
//	**********************************************************************	
	Role.Link deleteRole(int iRole) {
		return roles.remove(iRole);
	}
	public int getRoleCount() {
		return roles.size();
	}
	
	public int countRoleOccurrences(Role r){
		int counter=0;
		for(Role.Link rl:roles) {
			if (rl.getRole()==r)
				counter++;
		} 
		return counter;
	}
	
//**********************************************************************		
	public Role.Link getLastRole() {
	  return roles.get(roles.size()-1);
	}
//**********************************************************************		
	public Role.Link getRole(int roleIndex) {
		  return roles.get(roleIndex);
	}
	public Role.Link getRole(String label) {
		int i = getRoleIndex(label);
		if (i>=0)
		  return roles.get(i);
		else
			return null;
	}
	public int getRoleIndex(Role.Link r) {
		return roles.indexOf(r);
	}
	public int getRoleIndex(String label) {
		int i=0;
		for(Role.Link r:roles)
			if (r.getLabel().equals(label)) return i;
			else i++;
		return -1;
	}
	public boolean containsRole(Role.Link r){ return roles.contains(r);	}
	public Iterable<Role.Link> getRoles(){ return roles; }
	public int getOptionCount(){
		int count=0;
		for(Role.Link r:getRoles())
			count+=r.getRole().getOptions().size();
		return count;
	}
	
	void addConsequence(int i,Script s) {
		consequences.add(i,s);
	}
//	**********************************************************************
	Script addConsequence(Operator tOperator) {
		Script zScript = new Script(Script.Type.Consequence,-1,null,tOperator, true);		
		consequences.add(zScript);
		return(zScript);
	}
//**********************************************************************		
	void deleteConsequence(int iConsequence) {
		consequences.remove(iConsequence);
	}
//**********************************************************************	
	public Script getConsequence(int iConsequence) {
		  return((Script)consequences.get(iConsequence));
		}
//	**********************************************************************	
	public Script getConsequence(String label) {
		for(Script s:consequences)
			if (s.getLabel().equals(label))
				return s;
		return null;
	}
//**********************************************************************	
	public int getConsequenceIndex(String label) {
		int i=0;
		for(Script s:consequences){
			if (s.getLabel().equals(label))
				return i;
			i++;
		}
		return -1;
	}
//**********************************************************************	
	public boolean isWordSocketActive(int tiPart) {
		return socketData[tiPart]!=null;
	}
//**********************************************************************	
	public void setWSData(int tiTerm, WSData newValue) {
		socketData[tiTerm] = newValue;
	}
//**********************************************************************
	public WSData getWSData(int iSocket) {
		return socketData[iSocket];
	}
	public boolean[] getVisible() {
		boolean[] visible = new boolean[Sentence.MaxWordSockets];
		for(int i=0;i<Sentence.MaxWordSockets;i++){
			if (socketData[i]!=null)
				visible[i] = socketData[i].visible;
		}
		return visible;
	}
//**********************************************************************	
	public boolean isVisible(int tiPart) {
		return socketData[tiPart].visible;
	}
	public boolean isWitness(int tiPart) {
		return socketData[tiPart].witness;
	}
	public Presence getPresence(int tiPart) {
		return socketData[tiPart].presence;
	}
//**********************************************************************	
	public Operator.Type getWordSocketType(int tiSocket) {
		return socketData[tiSocket].type;
	}
//**********************************************************************
	public String getWordSocketBaseLabel(int tiSocket) {
		if (tiSocket==0) return "Subject";
		else if (tiSocket==1) return "Verb";
		else if ((tiSocket==2) & (socketData[tiSocket].type==Operator.Type.Actor)) return "DirObject";
		else return socketData[tiSocket].type.name();
	}
	//**********************************************************************
	public String getWordSocketFullLabel(int tiSocket) {
		return getWordSocketFullLabel(socketData[tiSocket].type,tiSocket);
	}
	public static int getWordSocketIndex(String label){
		if ("Subject".equals(label))
			return Sentence.Subject;
		else if ("Verb".equals(label))
			return Sentence.Verb;
		else if ("DirObject".equals(label))
			return 2;
		// Otherwise parse the index from the label 
		int end = 0;
		while (Character.isDigit(label.charAt(end))) 
			end++;
		return Integer.parseInt(label.substring(0,end))-1;
	}
	public static String getWordSocketFullLabel(Operator.Type t,int tiSocket) {
		if (tiSocket==0) return "Subject";
		else if (tiSocket==1) return "Verb";
		else if ((tiSocket==2) & (t==Operator.Type.Actor)) return "DirObject";
		else if (t==Operator.Type.UnType)
			return String.valueOf(tiSocket+1);
		else return String.valueOf(tiSocket+1)+t.name();
	}
//**********************************************************************	
	public String getCategory() {
	  return(category);
	}
//**********************************************************************	
	public void setCategory(String tCategory) {
		category = tCategory;
	}
//**********************************************************************	
	public String getExpression() {
	  return(expression);
	}
//**********************************************************************	
	public void setExpression(String tExpression) {
		expression = tExpression;
	}
	
	public Witnesses getWitnesses() {
		  return witnesses;
	}
	public void setWitnesses(Witnesses w) {
		witnesses = w;
	}

//**********************************************************************	
	public int getTimeToExecute() {
	  return(timeToExecute);
	}
//**********************************************************************	
	public void setTimeToExecute(int tTime) {
		timeToExecute = tTime;
	}
//**********************************************************************	
	public int getTimeToPrepare() {
	  return(timeToPrepare);
	}
//**********************************************************************	
	public void setTimeToPrepare(int tTime) {
		timeToPrepare = tTime;
	}
//**********************************************************************	
	public float getTrivial_Momentous() {
	  return(trivial_Momentous);
	}
//**********************************************************************	
	public void setTrivial_Momentous(float iImportance) {
		trivial_Momentous = iImportance;
	}
//**********************************************************************	
	public boolean getHijackable() {
		  return(hijackable);
	}
	//**********************************************************************	
	public void setHijackable(boolean tHijackable) {
		hijackable = tHijackable;
	}
	//**********************************************************************	
	public boolean getOccupiesDirObject() {
		  return occupiesDirObject;
	}
	//**********************************************************************	
	public void setOccupiesDirObject(boolean tOccupiesDirObject) {
		this.occupiesDirObject = tOccupiesDirObject;
	}
	//**********************************************************************	
	public ArrayList<Script> getConsequences() {
	  return(consequences);
	}
//**********************************************************************	
	public int getConsequenceCount() {
	  return(consequences.size());
	}
//**********************************************************************	
	public String getDescription() {
		return description;
	}
//**********************************************************************
	public void setDescription(String newString) {
		description = newString;
	}
//**********************************************************************
	public Script getSuffix(int i) {
		return socketData[i].suffix;
	}
//**********************************************************************
	public String getNote(int i) {
		return socketData[i].note;
	}
//**********************************************************************
	public Script getWordsocketTextScript(int i){ return socketData[i].text; } 
	
/**********************************************************************	
	public void setDescription(String description) {
		this.description = description;
	}
	/** @return the abort script for this verb */
	public Script getAbortScript() {
		return abortScript;
	}
	/** Sets the script to use to prevent execution of this verb. */
	void setAbortScript(Script abortScript) {
		this.abortScript = abortScript;
	}
	/** Creates a default script to use as abort script. */
	public Script defaultAbortScript() {
		Script script = new Script(Script.Type.AbortIf,-1,null,OperatorDictionary.getAbortIfOperator(),false);
		script.getRoot().add(Script.createNode(OperatorDictionary.getFalseOperator(),0.0f));
		return script;
	}
	
	/** Creates a default script to use as a wordsocket script. */
	public Script defaultWordsocketTextScript(int iWordsocket) {
		Script script = new Script(Script.Type.WordsocketLabel,iWordsocket,null,OperatorDictionary.getWordsocketTextOperator(),false);
		if (iWordsocket==Sentence.Subject && getWordSocketType(iWordsocket)==Operator.Type.Actor) {
			script.addNode(false, OperatorDictionary.getNominativePronounOperator(), "", 0f,1);
			script.addNode(false, OperatorDictionary.getThisSubjectOperator(), "", 0f,1);
		} else
			script.addNode(false, OperatorDictionary.getTheNameOperator(), "", 0f,1);
		return script;
	}
	
	/** Creates a default script to use as a wordsocket suffix. */
	public Script defaultSuffixScript(int iWordsocket,String text) {
		Script script = new Script(Script.Type.WordsocketSuffix,iWordsocket,null,OperatorDictionary.getWordsocketTextOperator(),false);
		script.addNode(false, OperatorDictionary.getTextConstantOperator(), "", text,1);
		return script;
	}

}
