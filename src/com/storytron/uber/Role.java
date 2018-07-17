package com.storytron.uber;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import com.storytron.enginecommon.LimitException;
import com.storytron.swat.util.IterableFilter;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;


/**
 This class represents a Role that an {@link Actor} can assume during 
 performance of an event. It determines how {@link Actor}s assuming
 a Role are affected by the event, and describes which reactions the
 {@link Actor} can have.
 <p>
 A Role has a collection of emotional reaction scripts, manipulable through
 {@link #getEmotions()}, {@link #getEmotionIndex(String)},
 {@link #getEmotionScript(String)}, {@link #addEmotion(int, Script)},
 {@link #addEmotion(Verb, Operator)}, {@link #deleteEmotion(String)}.
 <p>
 A Role has a collection of {@link Option}s, manipulable through
 {@link #getOptions()}, {@link #getOption(String)}, {@link #getOptionIndex(String)}, 
 {@link #addOption(Deikto, com.storytron.uber.Role.Option)},
 {@link #addOption(Deikto, int, com.storytron.uber.Role.Option)}
 {@link #addOption(Deikto, Verb)}, {@link #addOptionWithWordSockets(Verb)}, 
 {@link #deleteOption(Deikto, int)}, {@link #deleteOption(Deikto, String)},
 {@link #deleteOption(Deikto, com.storytron.uber.Role.Option)}.
 <p>
 A Role has also an {@link Script} that determines if an actor can assume the Role,
 manipulable through {@link #getAssumeRoleIfScript()}, {@link #setAssumeRoleIfScript(Script)}.
 It also has a label which serves to identify it.
 * */
public final class Role implements Cloneable, Serializable {
		private static final long serialVersionUID = 1l;
		private Script assumeRoleIfScript;
		private ArrayList<Script> emotions = new ArrayList<Script>();
		private ArrayList<Option> options;

		/** A class for representing links to roles. They have a role and a name. */
		public static class Link implements Cloneable {
			private Role r;
			private String label;
			/** Creates a link to the role with the given name. */
			public Link(Role r,String label){
				this.r = r;
				this.label = label;
			}
			public Link(Link l){
				r = l.r;
				label = l.label;
			}
			public Role getRole(){ return r; }
			public String getLabel(){ return label; }
			public void setLabel(String label){ this.label=label; }
			
			public Link clone(boolean cloneOptions)  {
				try {
					Link newl = (Link)super.clone();
					newl.r = r.clone(cloneOptions);
					return newl;
				} catch(CloneNotSupportedException e){
					throw new RuntimeException(e);
				}
			}
			
			@Override
			public String toString() {
				return getLabel();
			}
		}
		
		/**
		   An Option describe a possible reaction for an {@link Actor}
		   assuming a given {@link Role}. Its main attribute is the verb
		   that is evaluated as a possible reaction ({@link #getPointedVerb()}).
		   <p>
		    The Option has an Inclination {@link Script} that tells how
		    inclined is the actor towards the option.
		   <p>
		    The Option holds a {@link OptionWordSocket} collection that determines 
		    how to fill the verb components to form a {@link Sentence}.
		    Manipulable through {@link #getWordSocket(int)}, 
		    {@link #getWordSocket(String)}, 
		    {@link #setWordSocket(int, com.storytron.uber.Role.Option.OptionWordSocket)},
		    {@link #initWordSocket(int)},
		    {@link #isWordSocketActive(int)}.		    
		 * */
		public static final class Option implements Cloneable, Serializable {
			private static final long serialVersionUID = 1l;
			private Verb verb;
			private Script acceptable, desirable;
			private OptionWordSocket wordSocket[];
			
			/**
			   A WordSocket defines a component of a verb for a given reaction.
			   It contains scripts that decide which values are acceptable,
			   and which of the acceptable values is the most desirable.  
			 * */
			public static final class OptionWordSocket implements Cloneable, Serializable {
				private static final long serialVersionUID = 1l;
				private int iWordSocket;
				private Script acceptableScript, desirableScript;
				//---------------------------------------------------------------------
				private OptionWordSocket(Option tOption, int tiWordSocket,boolean setDefaultScripts) {
					iWordSocket = tiWordSocket;
					if (setDefaultScripts){
						acceptableScript = getDefaultAcceptableScript(tOption, iWordSocket);
						desirableScript = new Script(Script.Type.Desirable,iWordSocket,null,OperatorDictionary.getDesirableOperator(), false);
						desirableScript.addNode(false, OperatorDictionary.getBNumberConstantOperator(), "", 0.0f, 0);
					}
				}
				//---------------------------------------------------------------------
				public Script getAcceptableScript() {
					return acceptableScript;
				}
				//---------------------------------------------------------------------
				void setAcceptableScript(Script acceptableScript) {
					this.acceptableScript = acceptableScript;
				}
				//---------------------------------------------------------------------
				public Script getDesirableScript() {
					return desirableScript;
				}
				//---------------------------------------------------------------------
				void setDesirableScript(Script desirableScript) {
					this.desirableScript = desirableScript;
				}
				//---------------------------------------------------------------------
				public int getIWordSocket() {
					return iWordSocket;
				}
				//---------------------------------------------------------------------
				public void setIWordSocket(int wordSocket) {
					iWordSocket = wordSocket;
				}
				//---------------------------------------------------------------------
				public OptionWordSocket clone() {
					try {
						OptionWordSocket newWordSocket = (OptionWordSocket)super.clone();
						newWordSocket.acceptableScript = (Script)this.acceptableScript.clone();
						newWordSocket.desirableScript = (Script)this.desirableScript.clone();
						return newWordSocket;
					} catch (CloneNotSupportedException e) {
							throw new Error("This should never happen!");
					}
				}
				public static void fillDefaultDefaultAcceptableScript(Option tOption,int tiSocket,Script s){
					s.clear();
					s.addNode(false, OperatorDictionary.getAllWordsWhichOperator(tOption.getPointedVerb().getWordSocketType(tiSocket)), "", 0.0f, 1);
					if (tiSocket<=Sentence.Verb)
						s.addNode(false, OperatorDictionary.getTrueOperator(), "", 1.0f, 0);
					else 
						s.addNode(false, OperatorDictionary.getUndefinedOperator("?Boolean?"), "", null, 0);
				}
				public static Script getDefaultAcceptableScript(Option tOption,int tiSocket){
					final Operator.Type t = tOption.getPointedVerb().getWordSocketType(tiSocket);
					Script s = new Script(Script.Type.Acceptable,tiSocket,null,OperatorDictionary.getAcceptableOperator(t), false);
					fillDefaultDefaultAcceptableScript(tOption, tiSocket, s);
					return s;
				}
			}
			//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			Option(Verb verb,boolean setDefaultScripts) {
				this.verb = verb;
				wordSocket = new OptionWordSocket[Sentence.MaxWordSockets];
				wordSocket[Sentence.Subject] = new OptionWordSocket(this,Sentence.Subject,setDefaultScripts);
				wordSocket[Sentence.Verb] = new OptionWordSocket(this,Sentence.Verb,setDefaultScripts);
				if (setDefaultScripts) {
					acceptable = new Script(Script.Type.OptionAcceptable,-1,null,OperatorDictionary.getOptionAcceptableOperator(), false);
					desirable = new Script(Script.Type.OptionDesirable,-1,null,OperatorDictionary.getDesirableOperator(), false);
				}
			}
			
			public static Option createOptionWithWordSockets(Verb verb) {
				Option zOption = new Option(verb,true);

				zOption.desirable.addNode(true, OperatorDictionary.getDesirableOperator(), "", 0.0f, 0);
				zOption.desirable.addNode(false, OperatorDictionary.getBNumberConstantOperator(), "", 0.0f, 0);

				zOption.acceptable.addNode(true, OperatorDictionary.getOptionAcceptableOperator(), "", 0.0f, 0);
				zOption.acceptable.addNode(false, OperatorDictionary.getTrueOperator(), "", "", 0);

				// Start from 2 because wordSockets 0 and 1 are Subject and Verb.
				for (int i = Sentence.DefDirObject; (i < Sentence.MaxWordSockets); ++i) {
					if (verb.isWordSocketActive(i)) {
						if (zOption.wordSocket[i]==null)
							zOption.wordSocket[i] = new Option.OptionWordSocket(zOption,i,true);
					}
					else
						zOption.wordSocket[i] = null;
				}
				return zOption;
			}

			public Verb getPointedVerb(){
				return verb;
			}

			/** Initializes an option wordsocket. */
			public void initWordSocket(int tiWordSocket,boolean setDefaultScripts){
				wordSocket[tiWordSocket] = new OptionWordSocket(this,tiWordSocket,setDefaultScripts);
			}
			//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			public Iterable<OptionWordSocket> getActiveWordSockets(){
				return new IterableFilter<OptionWordSocket>(Arrays.asList(wordSocket)){
					@Override
					protected boolean evaluatePredicate(OptionWordSocket t) {
						return t!=null;
					}
				};
			}
			//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			public boolean isWordSocketActive(int i){
				return wordSocket[i]!=null;
			}
			//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			public String getLabel() {
				return verb.getLabel();
			}
			//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			public Script getAcceptableScript() {
				return acceptable;
			}
			public Script getDesirableScript() {
				return desirable;
			}
			void setDesirableScript(Script desirableScript) {
				desirable = desirableScript;
			}
			void setAcceptableScript(Script acceptableScript) {
				acceptable = acceptableScript;
			}
			//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			public OptionWordSocket getWordSocket(int tIndex) {
				return wordSocket[tIndex];
			}
			//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
			public void setWordSocket(int i,OptionWordSocket ws){
				wordSocket[i] = ws;
			}

			public Option clone() {
				int i;
				Option newOption;
				
				try {
					newOption = (Option)super.clone();
					newOption.acceptable = (Script)this.acceptable.clone();
					newOption.desirable = (Script)this.desirable.clone();
					
					newOption.wordSocket = new OptionWordSocket[Sentence.MaxWordSockets];
					for (i = 0; i < Sentence.MaxWordSockets; ++i) { 
						if (this.wordSocket[i]!=null)
							newOption.wordSocket[i] = (OptionWordSocket)this.wordSocket[i].clone();
					}
					return newOption;
				} catch (CloneNotSupportedException e) {
						throw new Error("This should never happen!");
				}
			}
			
			public OptionWordSocket getWordSocket(String wsLabel){
				for(Role.Option.OptionWordSocket w:wordSocket)
					if (w!=null && verb.getWordSocketFullLabel(w.getIWordSocket()).equals(wsLabel)) 
						return w;
				return null;
			}
			
			@Override
			public String toString(){ return getLabel(); }

		} // end of Option class
//**********************************************************************	
		public Role(boolean addDefaultScripts) throws LimitException {
			options = new ArrayList<Option>();
			assumeRoleIfScript = new Script(Script.Type.AssumeRoleIf,-1,null,OperatorDictionary.getAssumeRoleIfOperator(), false);
			if (addDefaultScripts) {
				assumeRoleIfScript.addNode(true, OperatorDictionary.getAssumeRoleIfOperator(), "", 0.0f, 1);
				assumeRoleIfScript.addNode(false, OperatorDictionary.getUndefinedBooleanOperator(), "", 0.0f, 0);
			}
 		}
//**********************************************************************	
	Option addOption(Verb verb) throws LimitException {
		return addOption(new Option(verb,true));
	}
//**********************************************************************	
	Option addOption(Option newOption) {
		return addOption(options.size(),newOption);
	}
	Option addOption(int index,Option newOption) {
		options.add(index,newOption);
		return newOption;
	}
//**********************************************************************	
	void deleteOption(int iOption) {
		options.remove(iOption);
	}
//**********************************************************************	
	public void deleteOption(Deikto dk,String label) {
		dk.optionCount--;
		options.remove(getOptionIndex(label));
	}
//	**********************************************************************	
	public int getOptionIndex(String label) {
		for(int i=0;i<options.size();i++) if (options.get(i).getLabel().equals(label)) return i;
		return -1;
	}
//**********************************************************************	
	public Role clone(boolean cloneOptions) {
		try {
			Role newRole = (Role)super.clone();
			newRole.assumeRoleIfScript = (Script)this.assumeRoleIfScript.clone();
			newRole.emotions = new ArrayList<Script>();
			for (int i = 0; (i < emotions.size()); ++i) {
				Script oldScript = (Script)emotions.get(i);
				Script newScript = (Script)oldScript.clone();
				newRole.emotions.add(newScript);
			}
			
			// now clone the various options
			newRole.options = new ArrayList<Option>(options);
			if (cloneOptions) {
				for (int i = 0; (i < options.size()); ++i)
					newRole.options.set(i,newRole.options.get(i).clone());
			}
			return(newRole);
		} catch (CloneNotSupportedException e) {
			throw new Error("This should never happen!");
		}
	}
//**********************************************************************	
	public Script getAssumeRoleIfScript() {	return assumeRoleIfScript;	}
	void setAssumeRoleIfScript(Script tNewScript) {	assumeRoleIfScript = tNewScript;	}
	public ArrayList<Option> getOptions() {	return options;	}
	public Iterable<Script> getEmotions() { return emotions; }
	public int getEmotionCount() { return emotions.size(); }
//	**********************************************************************	
	void addEmotion(int i,Script script){
		emotions.add(i,script);
	}
//		**********************************************************************
	public Script getEmotion(int i) { return emotions.get(i); }
	public Script getEmotionScript(String scriptLabel){
		int i=getEmotionIndex(scriptLabel);
		if (i!=-1)
			return emotions.get(i);
		else return null;
	}
	public int getEmotionIndex(String emotion){
		for(int i=0;i<emotions.size();i++)
			if (emotions.get(i).getLabel().equals(emotion)) 
				return i;
		return -1;
	}
	public int getEmotionIndex(Script s){ return emotions.indexOf(s); }
	void deleteEmotion(int i){
		emotions.remove(i);
	}
//		**********************************************************************	
	public Option getOption(String optLabel){
		for(Role.Option o:getOptions())
			if (o.getLabel().equals(optLabel)) return o;
		return null;
	}
}
