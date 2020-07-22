package engPackage;

import java.util.ArrayList;

import storyTellerPackage.Storyteller;

import com.storytron.enginecommon.LabeledSentence;
import com.storytron.enginecommon.MenuElement;
import com.storytron.enginecommon.StorytellerReturnData;

import engPackage.Janus.StopEngineException;

/** 
 * The default {@link EnginePlayerIO} requests input from {@link Storyteller}.
 * It records input data in an {@link PlayerInputRecorder}, if one is used
 * for the session associated to this player.
 * */
public class StorytellerPlayerIO implements EnginePlayerIO {

	private Janus.SessionData sD;
	
	StorytellerPlayerIO(Janus.SessionData sD){
		this.sD = sD;
	}
	
	public void onCycleStart(){
		Janus.onCycleStart(sD);
	};
	
	public boolean hasNext(){ return true; }

	public int getPlayerSelection(LabeledSentence tLabeledSentence, ArrayList<MenuElement> menuElements,
									int wordSocket, int playerId) 
			throws InterruptedException {
		Janus.sendDebugMessage("getPlayerSelection()");

		sD.engine.logger.partialSave();
		
		StorytellerReturnData stReturn = new StorytellerReturnData();

		stReturn.returnType = StorytellerReturnData.engineCallType.GET_PLAYER_SELECTION;
		stReturn.tLabeledSentence = tLabeledSentence;
		stReturn.menuElements = new ArrayList<MenuElement>(menuElements);
		stReturn.wordSocket  = wordSocket;
		stReturn.playerId = playerId;
		
		sD.toStoryteller.put(stReturn);

		//	Process expected player selection input from the client
		Janus.sendDebugMessage("Result code of getPlayerSelection()");

		int resultFromStoryteller = sD.toEngine.take();

		if (!sD.storyIsStopped)
			if (resultFromStoryteller==-1 && !sD.lastInputs.isEmpty())
				sD.lastInputs.remove(sD.lastInputs.size()-1);
			else
				sD.lastInputs.add(resultFromStoryteller);
		if (sD.inputRecorder!=null && !sD.storyIsStopped)
			sD.inputRecorder.savePlayerSelection(resultFromStoryteller, tLabeledSentence, menuElements, wordSocket);

		// Stop the engine thread if Storyteller is closed
		if (sD.storyIsStopped) {
			throw new Janus.StopEngineException();
		}
		return resultFromStoryteller;
	}
//	**********************************************************************
	public int getPlayerDone(LabeledSentence tLabeledSentence, int playerId) throws InterruptedException {
		Janus.sendDebugMessage("getPlayerDone()");

		sD.engine.logger.partialSave();

		StorytellerReturnData stReturn = new StorytellerReturnData();

		stReturn.returnType = StorytellerReturnData.engineCallType.GET_PLAYER_DONE;
		stReturn.tLabeledSentence = tLabeledSentence;
		stReturn.playerId = playerId;
		
		sD.toStoryteller.put(stReturn);
		
		// Process expected done button input from the client
		Janus.sendDebugMessage("result code of getPlayerDone()");

		int resultFromStoryteller = sD.toEngine.take();

		if (!sD.storyIsStopped)
			if (resultFromStoryteller==-1 && !sD.lastInputs.isEmpty())
				sD.lastInputs.remove(sD.lastInputs.size()-1);
			else
				sD.lastInputs.add(resultFromStoryteller);
		if (sD.inputRecorder!=null && !sD.storyIsStopped)
			sD.inputRecorder.savePlayerDone(resultFromStoryteller, tLabeledSentence);
		
		// Stop the engine thread if Storyteller is closed
		if (sD.storyIsStopped) {
			throw new StopEngineException();
		}
		return resultFromStoryteller;
	}
	
	public void sendTriggerSentence(LabeledSentence tLabeledSentence, int tTime, boolean showBottom, int playerId) 
			throws InterruptedException {
		Janus.sendDebugMessage("sendTriggerSentence()");
		
		// Set up the trigger sentence
		StorytellerReturnData stReturn = new StorytellerReturnData();
		
		stReturn.returnType = StorytellerReturnData.engineCallType.SEND_TRIGGER_SENTENCE;
		stReturn.tLabeledSentence = tLabeledSentence;
		stReturn.tTime = tTime;
		stReturn.showBottom = showBottom;
		stReturn.playerId = playerId;

		sD.toStoryteller.put(stReturn);
		
		if (sD.storyIsStopped) {
			throw new StopEngineException();
		}
	}

	public void theEnd() throws InterruptedException {
		Janus.sendDebugMessage("theEnd()");
		
		StorytellerReturnData stReturn  = new StorytellerReturnData();
		stReturn.returnType = StorytellerReturnData.engineCallType.THE_END;

		sD.toStoryteller.put(stReturn);
	}

	// Write event to the storybook
	public void writeStorybook(String tSentence) {
		sD.engine.writeStorybook(tSentence);
	}

};
