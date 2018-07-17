package com.storytron.enginecommon;

import java.io.Serializable;
import java.util.ArrayList;


/**
 * Messages sent to Storyteller from the server.
 * <p>
 * Different kind of messages can be sent. They can be distinguished by the
 * field {@link #returnType}.
 * */
public final class StorytellerReturnData  implements Serializable {
	private static final long serialVersionUID = 1l;
	/** Identifies the differen kinds of messages. */
	public enum engineCallType {
		/** 
		 * Carries a sentence meant to be shown to the player.
		 * More data is available in the engine to be retrieved in another call. 
		 * */
		SEND_TRIGGER_SENTENCE,
		/** 
		 * Carries the options that must be presented to the player so she can
		 * start/continue constructing a sentence. 
		 * Now, the engine is waiting for confirmation. 
		 * */
		GET_PLAYER_SELECTION,
		/** 
		 * Notifies that the sentence being constructed by the player is complete. 
		 * Now, the engine is waiting for confirmation. 
		 * */
		GET_PLAYER_DONE,
		/** Notifies that the story has finished. */
		THE_END
	}
	public engineCallType returnType;
	public LabeledSentence tLabeledSentence;
	public int tTime;
	public boolean showBottom;
	public ArrayList<MenuElement> menuElements;
	public int wordSocket;
	public int playerId;
	
	/** Tells if there is more input to collect from the engine. */
	public boolean inputEnded(){
		return returnType == engineCallType.GET_PLAYER_DONE
				|| returnType == engineCallType.GET_PLAYER_SELECTION
				|| returnType == engineCallType.THE_END;
	}
}
