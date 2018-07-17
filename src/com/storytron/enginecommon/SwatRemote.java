package com.storytron.enginecommon;

import java.rmi.RemoteException;

/** 
 * This interface extends {@link StorytellerRemote} with methods
 * for doing rehearsals.
 * <p>
 * To make a rehearsal a session must have been previously created.
 * The rehearsal can be initiated with 
 * {@link #startRehearsal(String, byte[])}, canceled with 
 * {@link #cancelRehearsal(String)}. Progress can be measured with
 * {@link #getRehearsalProgress(String)}. And results can be retrieved
 * with {@link #getRehearsalResults(String)}.  
 * */
public interface SwatRemote extends StorytellerRemote {
	/** Starts a rehearsal from a given verb. */
	public void startRehearsal(String sessionID, int[] input,byte[] worldState) throws RemoteException;
	
	/** 
	 * Gets the results and destroys the session data.
	 * Returns null if called before the rehearsal is finished,
	 * but the session is destroyed anyway. 
	 * */
	public RehearsalResult getRehearsalResults(String sessionID) throws RemoteException;
	/** Cancels the rehearsal and destroys the session data. */
	public void cancelRehearsal(String sessionID) throws RemoteException;
	
	/** 
	 * Returns progress of a rehearsal in a scale from 0 (no progress)
	 * to 100 (rehearsal finished).
	 * */
	public int getRehearsalProgress(String sessionID)  throws RemoteException;
}