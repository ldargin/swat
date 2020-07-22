package com.storytron.test;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.UUID;

import com.storytron.enginecommon.IncompatibleVersionException;
import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.SessionLogoutException;
import com.storytron.enginecommon.SharedConstants;
import com.storytron.uber.operator.OperatorDictionary;

import engPackage.Janus;


public class JanusDeiktoListTest {

	/** Number of sessions that will be used during the test. */
	private static final int NUM_SESSIONS = 30;
	private static final String storyworldID = "BoP2K.stw";

	/** Tests if the deikto reference count in {@link Janus} reaches zero. */
	@org.junit.Test(expected=NullPointerException.class)
	public void testJanusDeiktoList() throws Exception {
		// load operators
		OperatorDictionary.loadOperators();
		// start engine
		Janus janus = new Janus();		

		runSessions(janus,"once upon a time");
		
		int i=0;
		try {
			while(i<10) {
				Thread.sleep(1000);
				janus.getDeiktoListRefCount(storyworldID);
				i++;
			}
		} finally {
			janus.shutdown();
		}
	}
	
	private static void runSessions(Janus janus,String startVerbLabel) 
			throws RemoteException, InterruptedException, IncompatibleVersionException,
					SessionLogoutException, LimitException {

//		create NUM_SESSIONS session
		ArrayList<String> sessions = new ArrayList<String>(NUM_SESSIONS);
		for(int i=0;i<NUM_SESSIONS;i++) {
			sessions.add((String)janus.startStorytellerSession(SharedConstants.REMOTE_INTERFACE_VERSION, UUID.randomUUID().toString(), storyworldID).get(0));
		}

//		fire the sessions
		for(String session:sessions)
			janus.startStory(session, startVerbLabel, null, null, false);

//		finish sessions
		for(String session:sessions)
			janus.logout(session);
	} 
}
