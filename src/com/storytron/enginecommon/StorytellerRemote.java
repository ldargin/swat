package com.storytron.enginecommon;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import com.storytron.swat.util.Compressed;
 
/**
 * This is the server interface.
 * It provides services for running a story and navigating the log data
 * generated by it.
 * <p>
 * Before using the services, each client must create a session
 * which is referenced by the client with a session identifier string
 * ({@link #login(int, String, String)}, {@link #startStorytellerSession(int, String, String)}).
 * One user can have only one session at a time. So, creating a new session will
 * destroy the previous one if any.
 * <p>
 * If the session is created with the {@link #login(int, String, String)} method 
 * a storyworld must be specified later with 
 * {@link #loadDeiktoFromZip(String, byte[])}.
 * <p>
 * Once there is a storyworld specified for a session, a story can be initiated
 * with {@link #startStory(String, String, int[], byte[], boolean)}.
 * <p>
 * Further communication can proceed then by using {@link #setResult(String, int)}
 * to indicate player selections and {@link #getTrigger(String)} to obtain
 * feedback about what's happening in the simulation.
 * <p>
 * The exact order in which the calls must be made is dictated mainly by the Engine
 * implementation. Currently getTrigger(String) must be called until getting all of the
 * available data ({@link com.storytron.enginecommon.StorytellerReturnData#inputEnded()} returns true).
 * Then {@link #setResult(String, int)} must be called and the whole interaction must
 * be repeated again. 
 * <p>
 * When a story is started, after each interaction with the server there
 * may be log data to download. Log data may be buffered up to certain capacity,
 * and then, subsequent calls to {@link #setResult(String, int)} or
 * {@link #getTrigger(String)} may block to prevent generation of more log
 * data. So it is necessary to download every now and then the log data with
 * {@link #getLogData(String)}. 
 * <p>  
 * The log data is a big tree collected in stages. While playing the story, 
 * only the closer levels to the root are collected. This is the data obtained
 * with {@link #getLogData(String)}.
 * <p>
 * While playing a story, complete log data branches can be obtained by 
 * requesting them with {@link #requestLogData(String, int, int, int)}.
 * This will instruct the server to collect the data in that branch and
 * make it available to download with {@link #getRequestedLogData(String)}.
 * It is possible that a call to {@link #getRequestedLogData(String)}
 * does not return all of the requested data, but just the data that
 * was collected at the time the call was made. It is also possible that 
 * the log data buffer is full, and the collector is blocked to wait for the
 * buffer to be emptied. In those situations more calls to 
 * {@link #getRequestedLogData(String)} will be needed to have all the
 * requested data. It is the responsibility of the client to keep track
 * of which requested data remains to be downloaded. 
 * <p>
 * Sessions will timeout after a while of inactivity from the part of the 
 * client. So some methods my throw a {@link SessionLogoutException} if
 * called after the timeout.
 * <p>
 * Clients are kindly requested to logout ({@link #logout(String)}) when 
 * detecting that they will no longer use a session.
 * <p>
 * While playing a session it is possible to obtained background information
 * about the storyworld by using the methods {@link #getPeople(String)},
 * {@link #getPlaces(String)} and {@link #getThings(String)}.
 * And the story book can be retrieved with {@link #getStorybookEntries(String)}.
 * <p>
 * At any time the client can retrieve the engine state 
 * ({@link #getWorldState(String)}). The engine state can be used later 
 * to resume the story with 
 * {@link #startStory(String, String, int[], byte[], boolean)}.
 * */
public interface StorytellerRemote extends Remote {
	
	/** 
	 * Logins a user with a given password. Returns a session identifier.
	 * @throws IncompatibleVersionException if the client is not compatible with the server.
	 * */
	public String login(int clientVersion,String userName, String password) throws RemoteException, IncompatibleVersionException;
	/** 
	 * Destroys the session. Calls {@link #closeStoryteller(String)} if
	 * it as not been called already. 
	 * */
	public void logout(String sessionID) throws RemoteException;
	
	public boolean startStory(String sessionID, String verbLabel, int[] playerInput,byte[] worldState,boolean logging) 
			throws RemoteException, SessionLogoutException;
	/** Loads a storyworld provided by an author. */
	public void loadDeiktoFromZip(String sessionID, byte[] zipData) 
			throws RemoteException, SessionLogoutException, BadStoryworldException, LimitException;
	
	/** Gets the world state that can later be used to restore a storyteller session. */
	public byte[] getWorldState(String sessionID) 
			throws RemoteException, SessionLogoutException;
	
	public boolean setResult(String sessionID, int newResult) 
			throws RemoteException, SessionLogoutException, EngineDiedException;
	public StorytellerReturnData getTrigger(String sessionID) 
			throws RemoteException, SessionLogoutException, EngineDiedException;

	/** 
	 * Frees the session data associated to storyteller (but keeps the session
	 * data associated to log lizard). 
	 * */
	public void closeStoryteller(String sessionID) throws RemoteException;
	
	/**
	 * Returns the story book entries available since last call. The entries are 
	 * removed from the server immediately after the call.
	 * */
	public String[] getStorybookEntries(String sessionID) throws RemoteException, SessionLogoutException;

	/** 
	 * Starts a story for the given username and storyworld.
	 * @return a vector containing 
	 *         <ul>
	 *         <li>sessionID,</li>
	 *         <li>storyworld version,</li>
	 *         <li>the visible relationship names and descriptions,</li>
	 *         <li>actor trait names and descriptions,</li>
	 *         <li>stage trait names and descriptions,</li>
	 *         <li>prop trait names and descriptions,</li>
	 *         <li>actor names,</li>
	 *         <li>stage names</li>
	 *         <li>and prop names.</li>
	 *         </ul>
	 *         in that order.  
	 * @throws IncompatibleVersionException if the client is not compatible with the server.
	 * */
	public ArrayList<Object> startStorytellerSession(int clientVersion,String userName, String storyworldFile)
				throws LimitException, RemoteException, IncompatibleVersionException;

	/*** Gets the copyright info for the storyworld. */
	public String getCopyright(String sessionID) throws RemoteException, SessionLogoutException;
	
	/** Gets the visible traits of a given actor. */
	public float[] getActorTraits(String sessionID,String actor) 
				throws RemoteException, SessionLogoutException;
	/** Gets the visible traits of a given stage. */
	public float[] getStageTraits(String sessionID,String stage) 
				throws RemoteException, SessionLogoutException;
	/** Gets the visible traits of a given prop. */
	public float[] getPropTraits(String sessionID,String prop) 
				throws RemoteException, SessionLogoutException;
	
	public BgItemData getActorBgData(String sessionID,String actorName)
				throws RemoteException, SessionLogoutException;
	public BgItemData getStageBgData(String sessionID,String stageName)
				throws RemoteException, SessionLogoutException;
	public BgItemData getPropBgData(String sessionID,String propName) 
				throws RemoteException, SessionLogoutException;
	public float[][] getRelationshipValues(String sessionID,String relationshipName)
				throws RemoteException, SessionLogoutException;
	
	public String[] getActorsUnknownToProtagonist(String sessionID)
		throws RemoteException, SessionLogoutException;
	public String[] getStagesUnknownToProtagonist(String sessionID)
		throws RemoteException, SessionLogoutException;
	public String[] getPropsUnknownToProtagonist(String sessionID)
		throws RemoteException, SessionLogoutException;
	
	/** 
	 * Called to get the topmost levels of the log tree that have been collected 
	 * so far. The returned boolean is true iff the last tree is not complete,
	 * in such case it will be returned again in a future call to 
	 * {@link #getLogData(String)}.
	 * */
	public Compressed<Object[]>[] getLogData(String sessionID) throws RemoteException;
	/** 
	 * Requests the engine to collect the branches starting at child fromTree and ending at
	 * child toTree (inclusive) of the root node.
	 * @return false if the request is not accepted. Can happen if too many requests are
	 *               queued, or if fromTree>=toTree or if fromTree<0.  
	 * */
	public boolean requestLogData(String sessionID, int fromTree, int toTree, int skipCommands) 
				throws RemoteException, SessionLogoutException;
	/**
	 * Gets data collected by the engine.
	 * The first Integer returned value is the index of the first returned tree.
	 * The second Integer returned value is -1 if the sent trees matches the requested,
	 * or is the index of the last delivered requested tree (likely to be partial). 
	 * In case that this value is not -1, the client must expect more data to come from 
	 * the server to complete the request. 
	 * @return null if the data has not been requested, yet.
	 * */
	public Triplet<Integer,Integer,Compressed<Object[]>[]> getRequestedLogData(String sessionID) 
				throws RemoteException, SessionLogoutException;
}