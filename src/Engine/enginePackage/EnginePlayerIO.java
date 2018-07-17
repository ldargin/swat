package Engine.enginePackage;

import java.util.ArrayList;

import Engine.enginePackage.Janus.StopEngineException;

import com.storytron.enginecommon.LabeledSentence;
import com.storytron.enginecommon.MenuElement;

/**
 *  Interface for providing player input to the engine.
 *  <p>
 *  The engine calls the methods of this interface to communicate with the player.
 *  <p>
 * Its main purpose is to provide a clean way to feed the engine with recorded player
 * input when loading a saved story or collecting log data. 
 * */
public interface EnginePlayerIO {
	/** 
	 * Get the player input. Throws a {@link StopEngineException} if 
	 * there is no more input available. 
	 * Can throw InterruptedException because they may block waiting for buffer space to
	 *           place log data or the user my stop the session.  
	 * */ 
	public int getPlayerSelection(LabeledSentence tLabeledSentence, ArrayList<MenuElement> menuElements, 
									int wordSocket, int playerId) throws InterruptedException;
	public int getPlayerDone(LabeledSentence tLabeledSentence,int playerId) throws InterruptedException;
	/** Send output to the player. */
	public void onCycleStart();
	public void sendTriggerSentence(LabeledSentence tLabeledSentence, int tTime, boolean showBottom,int playerId)
		throws InterruptedException;
	public void theEnd() throws InterruptedException;
	void writeStorybook(String tSentence);
}