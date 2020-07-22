package engPackage;

import java.util.ArrayList;

import com.storytron.enginecommon.LabeledSentence;
import com.storytron.enginecommon.MenuElement;

import engPackage.Janus.SessionData;

/**
 * Records input from the player in order to play it again later.
 * Player input is recorded in {@link StorytellerPlayerIO}.
 * <p>
 * Input can be recorded with {@link #savePlayerDone(int, LabeledSentence)}
 * and {@link #savePlayerSelection(int, LabeledSentence, ArrayList, int)}.
 * <p>
 * Then, a PlayerInputRecorder can be passed as a {@link EnginePlayerIO}
 * to the engine in order to rerun with the recorded input.
 * The method {@link #setInputsOffset(int)} can be used to start the
 * engine run from an input different from the first one.
 * */
public class PlayerInputRecorder implements EnginePlayerIO {
	private ArrayList<Integer> results = new ArrayList<Integer>();
	private int r; 
	private SessionData sD;
	
	/** Builds a player input recorder with already recorded data. */
	public PlayerInputRecorder(SessionData sD,int[] input){
		this.sD = sD;
		if (input==null)
			return;
		for(int i:input)
			results.add(i);
	}
	
	public PlayerInputRecorder(SessionData sD){
		this(sD,null);
	}
	
	public void onCycleStart(){}
	
	/** Call this to record a new player input. */
	public synchronized void savePlayerSelection(int result, LabeledSentence tLabeledSentence, ArrayList<MenuElement> menuElements, int wordSocket){
		results.add(result);
	}
	/** Call this to record a new player input. */
	public synchronized void savePlayerDone(int result, LabeledSentence tLabeledSentence) {
		results.add(result);
	}
	
	public synchronized boolean hasNext(){ return r<results.size(); }
	
	/** @return the amount of recorded inputs. */
	public int size(){ return results.size(); }
	/** Sets the offset where to start replaying input. */
	public void setInputsOffset(int index){ r=index; }

	public int getPlayerSelection(LabeledSentence tLabeledSentence, ArrayList<MenuElement> menuElements,
									int wordSocket, int playerId) throws InterruptedException {
		if (!hasNext())
			sD.logDataCollector.processNextRequest();
		synchronized (this) {
			return results.get(r++);
		}
	}
	public int getPlayerDone(LabeledSentence tLabeledSentence, int playerId) throws InterruptedException {
		if (!hasNext())
			sD.logDataCollector.processNextRequest();
		synchronized (this) {
			return results.get(r++);
		}
	}
	
	public void sendTriggerSentence(LabeledSentence tLabeledSentence, int tTime, boolean showBottom, int playerId){
	}
	
	public void theEnd(){
	}
	
	public void writeStorybook(String tSentence){
	}
}
