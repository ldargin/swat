package engPackage;

import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Sentence;
import com.storytron.uber.operator.Operator;

/** 
 * This interface defines the methods that the interpreter
 * uses from the engine. This is needed to disentangle the interpreter
 * from the engine, as the interpreter should run in the client,
 *  where the engine is not present.  
 * */
public interface AbstractEngine {

	int getHistoryBookSize();
	
	int getEventCausalEvent(int pageNumber);
	
	int getCMoments();
	
	Sentence getHistoryBookPage(int pageNumber);
	
	int getReactingActor();
//	int getIProtagonist();
	
	Sentence getThisEvent();
	
	boolean isLogging();
	
	Sentence getChosenPlan();
	int getChosenPlanIWord(int wordSocket);
	int getHypotheticalEventIWord(int wordSocket);
	
	int getIMinute();
	int getIHour();
	int getIDay();
	
	boolean getStoryIsOver();
	void setStoryIsOver(boolean isOver);
	void setPermitFateToReact();
	void setFatesRole();
	void setAbortIf(boolean abort);
	
	void addAlarmMEETACTOR(int who, int withWhom);
	void addAlarmMEETSTAGE(int who, int stage);
	void addAlarmMEETPROP(int who, int prop);
	void addAlarmMEETTIME(int who, int when);
	
	Sentence getHypotheticalEvent();
	
	void logScriptMsg(ScriptPath sp,Script s) throws InterruptedException;
	void logValueMsgChild(String s) throws InterruptedException;
	void logPoisonMsgChild(String s) throws InterruptedException;
	void logTokenMsg(Operator op) throws InterruptedException;
	void loggerUp() throws InterruptedException;
}
