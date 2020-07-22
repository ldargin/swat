package com.storytron.swat;

import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Sentence;
import com.storytron.uber.operator.Operator;

import engPackage.AbstractEngine;

/** 
 * Empty implementation of the engine methods called by the interpreter.
 * This implementation is used in the client for visiscript, where
 * the engine is not present.
 * */
public final class DummyEngine implements AbstractEngine {

	private Sentence dummySentence; 

	public DummyEngine(Sentence s) {
		setSentence(s);
	}

	public void setSentence(Sentence s) {
		dummySentence = s;
	}
	
	public DummyEngine(int actorCount) {
		dummySentence = new Sentence(actorCount);
		dummySentence.setIWord(Sentence.Verb, 0);
	}
	
	public void addAlarmMEETACTOR(int who, int withWhom) {}
	public void addAlarmMEETPROP(int who, int prop) {}
	public void addAlarmMEETSTAGE(int who, int stage) {}
	public void addAlarmMEETTIME(int who, int when) {}

	public int getCMoments() { return 0;	}

	public Sentence getChosenPlan() { return dummySentence; }

	public int getChosenPlanIWord(int wordSocket) { return 0; }

	public int getEventCausalEvent(int pageNumber) { return 0;	}

	public Sentence getHistoryBookPage(int pageNumber) { return dummySentence; }

	public int getHistoryBookSize() { return 0; }

	public Sentence getHypotheticalEvent() { return dummySentence; }

	public int getHypotheticalEventIWord(int wordSocket) { return 0; }

	public int getIDay() { return 0; }

	public int getIHour() { return 0; }

	public int getIMinute() { return 0;	}

	public int getIProtagonist() { return 1; }

	public int getReactingActor() { return 0; }

	public boolean getStoryIsOver() { return false;	}

	public Sentence getThisEvent() { return dummySentence; };

	public void logPoisonMsgChild(String s) throws InterruptedException {}

	public void logScriptMsg(ScriptPath sp,Script s) throws InterruptedException {}
	
	public void logTokenMsg(Operator op) throws InterruptedException {}

	public void logValueMsgChild(String s) throws InterruptedException {}

	public void loggerUp() throws InterruptedException {}

	public void setAbortIf(boolean abort) {}

	public void setPermitFateToReact() {}

	public void setFatesRole() {}

	public void setStoryIsOver(boolean isOver) {}

	public boolean isLogging() {
		return false;
	}
}
