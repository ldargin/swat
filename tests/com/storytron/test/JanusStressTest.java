package com.storytron.test;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import storyTellerPackage.SavedStory;

import com.storytron.enginecommon.BadStoryworldException;
import com.storytron.enginecommon.IncompatibleVersionException;
import com.storytron.enginecommon.SessionLogoutException;
import com.storytron.enginecommon.SharedConstants;
import com.storytron.enginecommon.Utils;
import com.storytron.uber.Deikto;
import com.storytron.uber.operator.OperatorDictionary;

import Engine.enginePackage.Janus;

/** 
 * <p>This class implements a stress test for Janus.
 * The test loads BoP2K, and a trace for it with {@value #traceStepsCount} steps.
 * Then {@value #NUM_SESSIONS} sessions are fired to run the trace.
 * Then the total amount of time to finish all the runs is divided by
 * {@value #traceStepsCount} to get the "average" response time.</p>
 * <p>The test described above is executed more than once to collect
 * the performance when the runtime has "warmed" for the latest runs.</p>
 * <p>Please pass these arguments to the java runtime when running this test:
 *  <pre>-mx400m -server</pre>
 *  </p>
 * */
public class JanusStressTest {

	/** Number of sessions that will be used during the test. */
	private static final int NUM_SESSIONS = 100;
	/** Amount of repetitions to run the test. */
	private static final int REPETITIONS = 5;
	private static final String storyworldFile = "res/data/BoP2K.stw";
	private static final String storytraceFile = "res/data/janusstresstest.str";
	/** Number of steps in {@link #storytraceFile}. */
	private static final int traceStepsCount = 15;
	/** Are we logging or not the engine activity for all the sessions. */
	private static boolean logging = false;
	
	/** Tests the engine by running a lot of simultaneous threads. */
	public static void main(String[] args) throws Exception {
		
		// load operators
		OperatorDictionary.loadOperators();

		// load storyworld
		InputStream is = new FileInputStream(storyworldFile);
		Deikto dk = new Deikto(is,new File(storyworldFile),false);
		is.close();

		// load saved history
		InputStream br = new FileInputStream(storytraceFile);
		SavedStory ss = new SavedStory();
		ss.readFormatHeader(br);
		DataInputStream di = SavedStory.convertInputStreamToDataInput(br);
		ss.readBody(ss.readHeader(di));
		LinkedList<Integer> input = ss.recordedInput;
		
		// start engine
		Janus janus = new Janus();		
		
		long[] times = new long[REPETITIONS];
		for(int i=0;i<REPETITIONS;i++) {
			System.out.print("running repetition "+(i+1)+".");
			times[i]=runSessions(dk,janus,ss.startingVerbLabel,input);
			System.out.println(" Response time: "+times[i]/traceStepsCount+" per player sentence");
			System.gc();
		}
		
		/** Calculate average. */
		if (REPETITIONS>1) {
			int i=1;
			if (REPETITIONS>30)
				i=10;
			long sum=0;
			for(;i<times.length;i++)
				sum+=times[i]/traceStepsCount;
			if (REPETITIONS>30)
				System.out.println("Average (without accounting for the first 10 repetitions): "+sum/(times.length-10));
			else 
				System.out.println("Average (without accounting for the first repetition): "+sum/(times.length-1));
		}
		
		System.exit(0);
	}

	private static long runSessions(Deikto dk,Janus janus,String startVerbLabel,LinkedList<Integer> input) 
			throws RemoteException, InterruptedException, IncompatibleVersionException,
					SessionLogoutException, BadStoryworldException {
		
		Janus.Test.stressTestLatch = new CountDownLatch(NUM_SESSIONS);
		
		// create NUM_SESSIONS session
		ArrayList<String> sessions = new ArrayList<String>(NUM_SESSIONS);
		for(int i=0;i<NUM_SESSIONS;i++) {
			sessions.add(janus.login(SharedConstants.REMOTE_INTERFACE_VERSION,UUID.randomUUID().toString(), "pw"));
			janus.copyLocalDeikto(dk, sessions.get(i));
		}
		
		long initial = System.currentTimeMillis();
		
		// fire the sessions
		for(String session:sessions)
			janus.startStory(session, startVerbLabel, Utils.toArray(input), null, logging);
		
		Janus.Test.stressTestLatch.await();
		final long totaltime = System.currentTimeMillis()-initial;
		
		// finish sessions
		for(String session:sessions)
			janus.logout(session);

		return totaltime;
	} 
}
