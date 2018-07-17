package com.storytron.enginecommon;

import com.storytron.uber.Deikto;

/** 
 * Interface to be used by the stand-alone Storyteller and Swat.
 * <p>
 * We can put here all the Janus methods that should not be accessible 
 * through the remote interface, but must be available to call from the
 * stand-alone tools. 
 * */
public interface LocalJanus {

	/** Clones the storyworld. */
	public void copyLocalDeikto(Deikto originalDk, String sessionID)
	                 throws BadStoryworldException;

}
