package com.storytron.uber;
import java.io.Serializable;
import java.util.ArrayList;

import com.storytron.uber.operator.Operator;

public final class Sentence implements Cloneable, Serializable {
		private static final long serialVersionUID = 1l;
		public static final int MaxWordSockets = 15;
		public static final int Subject = 0;
		public static final int Verb = 1;
		public static final int DefDirObject = 2;
		boolean poisoned; // poison flag for this Sentence as a plan
		boolean hijacked;
		ArrayList<Boolean> whoKnows = new ArrayList<Boolean>(); // who knows about this event?
		int time;
		int location; // index of Stage on which this Sentence took place
		int causalEvent;
		int pageNumber;
		WordSocket[] wordSocket;
		float inclination; // inclination for this Sentence as a plan
		ArrayList<Float> belief = new ArrayList<Float>(); // degree belief (0.0 - 1.0) in
		ArrayList<Integer> outcomes = new ArrayList<Integer>(); // usually just one
		boolean isPartOfEpilogue;
		// seed used for calculating labels of this event.
		public long seed = 0;
		
		public int getOutcomeSize() { return outcomes.size(); }
		
//**********************************************************************	
	public static final class WordSocket implements Serializable {
		private static final long serialVersionUID = 1l;
		int iWord;
		Operator.Type type;
		//----------------------------------------------------------------	
		public WordSocket() {
			iWord = -1;
			type = Operator.Type.UnType;
		}
		//----------------------------------------------------------------
		public int getIWord() { return iWord;}
		//----------------------------------------------------------------
		public Operator.Type getType() { return type; }
		//----------------------------------------------------------------
		@Override
		public String toString(){
			return "w["+String.valueOf(iWord)+","+String.valueOf(type)+"]";
		}
		@Override
		public boolean equals(Object obj) {
			if (obj!=null && obj instanceof WordSocket) {
				WordSocket s = (WordSocket)obj; 
				return iWord==s.iWord && type==s.type;
			} else
				return false;
		}
	}
//**********************************************************************	
	public Sentence(int actorCount) {
		location = 0;
		causalEvent = -1;
		pageNumber = -1;
		inclination = 0.0f;
		poisoned = false;
		hijacked = false;
		isPartOfEpilogue = false;
		wordSocket = new WordSocket[MaxWordSockets];
		for (int i = 0; (i < MaxWordSockets); ++i) {
			wordSocket[i] = new WordSocket();
		}
		for (int i=0; i < actorCount; ++i) {
			whoKnows.add(false);
			belief.add(0.0f);
		}
	}
	
	/** Used for testing. Asserts that two sentences have the same state. */
	public void assertEquals(Sentence s){
		assert poisoned==s.poisoned;
		assert hijacked==s.hijacked;
		assert whoKnows.size()==s.whoKnows.size();
		for(int i=0;i<whoKnows.size();i++)
			assert whoKnows.get(i).equals(s.whoKnows.get(i));
		assert time==s.time;
		assert location==s.location;
		assert causalEvent==s.causalEvent;
		assert pageNumber==s.pageNumber;
		assert wordSocket.length==s.wordSocket.length;
		for(int i=0;i<wordSocket.length;i++)
			assert wordSocket[i]==s.wordSocket[i] || wordSocket[i]!=null && wordSocket[i].equals(s.wordSocket[i]):String.valueOf(i)+": "+String.valueOf(wordSocket[i])+"!="+String.valueOf(s.wordSocket[i]);
		assert inclination==s.inclination;
		assert belief.size()==s.belief.size();
		for(int i=0;i<belief.size();i++)
			assert belief.get(i).equals(s.belief.get(i));
		assert outcomes.size()==s.outcomes.size();
		for(int i=0;i<outcomes.size();i++)
			assert outcomes.get(i).equals(s.outcomes.get(i));
	} 
//**********************************************************************	
	public void clear() {
		location = 0;
		causalEvent = 0;
		pageNumber = 0;
		inclination = 0.0f;
		poisoned = false;
		hijacked = false;
		for (int i=0; (i < MaxWordSockets); ++i) {
			wordSocket[i].iWord = -1;
			wordSocket[i].type = Operator.Type.UnType;
		}
		for (int i=0; i < whoKnows.size(); ++i) {
			whoKnows.set(i, false);
			belief.set(i, 0.0f);
		}
	}
//	**********************************************************************	
	 public Sentence clone() {
		 try {
			 Sentence event = (Sentence)super.clone();

			 // copy arrays
			 event.whoKnows = new ArrayList<Boolean>(this.whoKnows);
			 event.belief = new ArrayList<Float>(this.belief);
			 event.wordSocket = new WordSocket[this.wordSocket.length];
			 for (int i = 0; i < this.wordSocket.length; i++) {
				 event.wordSocket[i] = new WordSocket();
				 event.wordSocket[i].iWord = this.wordSocket[i].iWord;
				 event.wordSocket[i].type = this.wordSocket[i].type;
			 }

			 return event;
		 } catch (CloneNotSupportedException e) {
			 throw new RuntimeException(e);
		 }
	}
//	**********************************************************************	
	 public int getLocation() { return (location); }
//	**********************************************************************	
	 public void setLocation(int newLocation) {
		 location = newLocation;
		 return;
	 }
//	**********************************************************************	
	 public int getTime() { return (time); }
//	**********************************************************************	
	 public void setTime(int newTime) {
		 time = newTime;
		 return;
	 }
//	**********************************************************************	
	 public int getCausalEvent() { return (causalEvent); }
//	**********************************************************************	
	 public void setCausalEvent(int newCausalEvent) {
		 causalEvent = newCausalEvent;
		 return;
	 }
//**********************************************************************	
	 public boolean getIsPartOfEpilogue() { return(isPartOfEpilogue); }
//**********************************************************************	
	 public void setIsPartOfEpilogue() { isPartOfEpilogue = true; return; }
//	**********************************************************************	
	 public int getPageNumber() { return (pageNumber); }
//	**********************************************************************	
	 public void setPageNumber(int newPageNumber) {
		 pageNumber = newPageNumber;
		 return;
	 }
//	**********************************************************************	
	 public float getInclination() { return (inclination); }
//	**********************************************************************	
	 public void setInclination(float newInclination) {
		 inclination = newInclination;
		 return;
	 }
//	**********************************************************************	
	 public boolean getPoisoned() { return (poisoned); }
//	**********************************************************************	
	 public void setPoisoned(boolean newPoisoned) {
		 poisoned = newPoisoned;
		 return;
	 }
//**********************************************************************	
	 public boolean getHijacked() { return (hijacked); }
//**********************************************************************	
	 public void setHijacked(boolean newHijacked) {
		 hijacked = newHijacked;
		 return;
	 }
//**********************************************************************	
	 public int getIWord(int partIndex) { return (wordSocket[partIndex].iWord); }
//**********************************************************************	
	 public int getDirObject() {
		 // this handles the simple problem of looking up the DirObject
		 // as well as the trickier problem of determining whether it is
		 // an Actor.
		 // CC 12/30/08: I have redefined DirObject to refer to the first
		 // Actor appearing after the Verb. The constant DefDirObject is 
		 // the "Default Direct Object" and is still 2. But the real 
		 // DirObject can only be found by examining the sentence.
		 int trialDirObject = DefDirObject; 
		 int result = -1;
		 boolean foundIt = false;
		 while (!foundIt & (trialDirObject<MaxWordSockets)) {
			 if (wordSocket[trialDirObject].type == Operator.Type.Actor) {
				 result = wordSocket[trialDirObject].iWord;
				 foundIt = true;
			 }			 
			 ++trialDirObject;
		 }
			 return result;
	 }
//**********************************************************************	
	 public void setIWord(int partIndex, int newIWord) {
		 wordSocket[partIndex].iWord = newIWord;
		 return;
	 }
//**********************************************************************	
	 public void setWordSocket(int partIndex, int newIWord, Operator.Type tType) {
		 wordSocket[partIndex].iWord = newIWord;
		 wordSocket[partIndex].type = tType;
		 return;
	 }
//**********************************************************************	
	 public WordSocket getWordSocket(int partIndex) { return (wordSocket[partIndex]); }
//**********************************************************************	
	 public void setWordSocket(int partIndex, WordSocket newSocket) {
		 wordSocket[partIndex] = newSocket;
		 return;
	 }
//**********************************************************************	
	public Operator.Type getWordSocketType(int tISocket) { return wordSocket[tISocket].type; }
//**********************************************************************	
	public static Operator.Type getTypeFromLabel(String label) {
		if (label.equals("Actor")) return Operator.Type.Actor;
		else if (label.equals("Prop")) return Operator.Type.Prop;
		else if (label.equals("Stage")) return Operator.Type.Stage;
		else if (label.equals("Subject")) return Operator.Type.Actor;
		else if (label.equals("Verb")) return Operator.Type.Verb;
		else if (label.equals("DirObject")) return Operator.Type.Actor;
		else if (label.equals("Quantifier")) return Operator.Type.Quantifier;
		else if (label.equals("ActorTrait")) return Operator.Type.ActorTrait;
		else if (label.equals("PropTrait")) return Operator.Type.PropTrait;
		else if (label.equals("StageTrait")) return Operator.Type.StageTrait;
		else if (label.equals("MoodTrait")) return Operator.Type.MoodTrait;
		else return Operator.Type.UnType;
	}
	//**********************************************************************	
	 public boolean getWhoKnows(int actorIndex) { return (whoKnows.get(actorIndex)); }
//**********************************************************************	
	 public void setWhoKnows(int actorIndex, boolean newWhoKnows) {
		 whoKnows.set(actorIndex, newWhoKnows);
		 return;
	 }
//**********************************************************************	
	 public float getBelief(int actorIndex) { return (belief.get(actorIndex)); }
//**********************************************************************	
	 public void setBelief(int actorIndex, float newBelief) {
		 belief.set(actorIndex, newBelief);
		 return;
	 }
//**********************************************************************	
	 public void addOutcome(int newOutcome) {
		 outcomes.add(newOutcome);
		 return;
	 }
//	**********************************************************************	
	 public int getOutcome(int tiOutcome) { return outcomes.get(tiOutcome); }
//  **********************************************************************	 
	 public int getWhoKnowsCount() {
		 return whoKnows.size();
	 }
//  **********************************************************************	 
	 public int getBeliefCount() {
		 return belief.size();
	 }
//	**********************************************************************
	 public int getOutcomeCount() {
		 return outcomes.size();
	 }
}
