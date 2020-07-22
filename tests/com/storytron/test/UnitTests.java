package com.storytron.test;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.storytron.enginecommon.Graph;

import engPackage.CustomRandom;
import engPackage.Statistics;


/** Test of various features of Swat GUI. */
public class UnitTests {

	@Test
	public void testGraphAlgorithms() throws Exception {
		Graph<Integer> graph  = new Graph<Integer>();
		graph.addEdge(0, 1);
		graph.addEdge(1, 2);
		graph.addEdge(2, 3);
		graph.addEdge(3, 4);
		graph.addEdge(4, 2);
		
		List<Integer> path=graph.getPath(0, 4);
		Assert.assertEquals(4, (int)path.size());
		Assert.assertEquals(0, (int)path.get(0));
		Assert.assertEquals(1, (int)path.get(1));
		Assert.assertEquals(2, (int)path.get(2));
		Assert.assertEquals(3, (int)path.get(3));
		
		List<Integer> cycle=graph.getCycle();
		Assert.assertEquals(3, (int)cycle.size());
		Assert.assertEquals(3, (int)cycle.get(0));
		Assert.assertEquals(4, (int)cycle.get(1));
		Assert.assertEquals(2, (int)cycle.get(2));
		
		graph.removeEdge(2, 3);
		cycle=graph.getCycle();
		Assert.assertEquals(0, (int)cycle.size());
		
		graph.addEdge(5, 5);
		cycle=graph.getCycle();
		Assert.assertEquals(1, (int)cycle.size());
		Assert.assertEquals(5, (int)cycle.get(0));

		graph.removeEdge(5, 5);
		cycle=graph.getCycle();
		Assert.assertEquals(0, (int)cycle.size());

		graph.addEdge(4, 3);
		cycle=graph.getCycle();
		Assert.assertEquals(2, (int)cycle.size());
		Assert.assertEquals(4, (int)cycle.get(0));
		Assert.assertEquals(3, (int)cycle.get(1));

		graph.removeEdge(4, 3);
		graph.removeEdge(4, 2);
		cycle=graph.getCycle();
		Assert.assertEquals(0, (int)cycle.size());

		graph.addEdge(2, 3);
		graph.addEdge(1, 5);
		graph.addEdge(5, 3);
		graph.addEdge(1, 6);
		graph.addEdge(6, 3);
		cycle=graph.getCycle();
		Assert.assertEquals(0, (int)cycle.size());
	}
	
	@Test
	public void testStatistics() throws Exception {
		Statistics st = new Statistics();
		
		// Login/logout
		Assert.assertEquals(0,Statistics.Test.getMaxSimAuthorSessions(st));
		Assert.assertEquals(0,Statistics.Test.getMaxSimPlayerSessions(st));
		st.reportAuthorLogin();
		Assert.assertEquals(1,Statistics.Test.getMaxSimAuthorSessions(st));
		Assert.assertEquals(0,Statistics.Test.getMaxSimPlayerSessions(st));
		st.reportPlayerLogin();
		Assert.assertEquals(1,Statistics.Test.getMaxSimAuthorSessions(st));
		Assert.assertEquals(1,Statistics.Test.getMaxSimPlayerSessions(st));
		st.reportAuthorLogin();
		st.reportAuthorLogout();
		st.reportAuthorLogout();
		Assert.assertEquals(2,Statistics.Test.getMaxSimAuthorSessions(st));
		Assert.assertEquals(1,Statistics.Test.getMaxSimPlayerSessions(st));
		Assert.assertEquals(0,Statistics.Test.getAuthorSessions(st));
		Assert.assertEquals(1,Statistics.Test.getPlayerSessions(st));
		st.reportPlayerLogout();
		Assert.assertEquals(2,Statistics.Test.getMaxSimAuthorSessions(st));
		Assert.assertEquals(1,Statistics.Test.getMaxSimPlayerSessions(st));
		Assert.assertEquals(0,Statistics.Test.getAuthorSessions(st));
		Assert.assertEquals(0,Statistics.Test.getPlayerSessions(st));
		
		// Loaded/Saved sessions.
		Assert.assertEquals(0,Statistics.Test.getSavedSessions(st));
		st.reportSavedSession();
		Assert.assertEquals(1,Statistics.Test.getSavedSessions(st));
		Assert.assertEquals(0,Statistics.Test.getLoadedSessions(st));
		st.reportLoadedSession();
		Assert.assertEquals(1,Statistics.Test.getLoadedSessions(st));
		
		// Loaded stories
		Assert.assertEquals(0,Statistics.Test.getLoadedStoryTraces(st));
		st.reportLoadedStoryTrace();
		Assert.assertEquals(1,Statistics.Test.getLoadedStoryTraces(st));

		// Storyteller reaction time.
		Assert.assertEquals(0,Statistics.Test.getStorytellerReactionTimeCount(st));
		Assert.assertEquals(0,Statistics.Test.getStorytellerReactionTimeSum(st));
		Assert.assertEquals(0,Statistics.Test.getMaxStorytellerReactionTime(st));
		st.reportStorytellerReactionTime(10);
		st.reportStorytellerReactionTime(5);
		Assert.assertEquals(2,Statistics.Test.getStorytellerReactionTimeCount(st));
		Assert.assertEquals(15,Statistics.Test.getStorytellerReactionTimeSum(st));
		Assert.assertEquals(10,Statistics.Test.getMaxStorytellerReactionTime(st));

		// LogLizard reaction time.
		Assert.assertEquals(0,Statistics.Test.getLogLizardReactionTimeCount(st));
		Assert.assertEquals(0,Statistics.Test.getLogLizardReactionTimeSum(st));
		Assert.assertEquals(0,Statistics.Test.getMaxLogLizardReactionTime(st));
		st.reportLogLizardReactionTime(20);
		st.reportLogLizardReactionTime(5);
		Assert.assertEquals(2,Statistics.Test.getLogLizardReactionTimeCount(st));
		Assert.assertEquals(25,Statistics.Test.getLogLizardReactionTimeSum(st));
		Assert.assertEquals(20,Statistics.Test.getMaxLogLizardReactionTime(st));

		// Rehearsal reaction time.
		Assert.assertEquals(0,Statistics.Test.getRehearsalReactionTimeCount(st));
		Assert.assertEquals(0,Statistics.Test.getRehearsalReactionTimeSum(st));
		Assert.assertEquals(0,Statistics.Test.getMaxRehearsalReactionTime(st));
		st.reportRehearsalReactionTime(30);
		st.reportRehearsalReactionTime(5);
		Assert.assertEquals(2,Statistics.Test.getRehearsalReactionTimeCount(st));
		Assert.assertEquals(35,Statistics.Test.getRehearsalReactionTimeSum(st));
		Assert.assertEquals(30,Statistics.Test.getMaxRehearsalReactionTime(st));

		// Rehearsal cancel time.
		Assert.assertEquals(0,Statistics.Test.getMaxRehearsalCancelTime(st));
		st.reportRehearsalCancelTime(40);
		st.reportRehearsalCancelTime(5);
		Assert.assertEquals(40,Statistics.Test.getMaxRehearsalCancelTime(st));

		// LogLizard sent bytes.
		Assert.assertEquals(0,Statistics.Test.getSurfaceLogLizardSentBytes(st));
		st.reportSurfaceLogLizardSentBytes(40);
		st.reportSurfaceLogLizardSentBytes(5);
		Assert.assertEquals(45,Statistics.Test.getSurfaceLogLizardSentBytes(st));
		
		Assert.assertEquals(0,Statistics.Test.getDeepLogLizardSentBytes(st));
		st.reportDeepLogLizardSentBytes(50);
		st.reportDeepLogLizardSentBytes(5);
		Assert.assertEquals(55,Statistics.Test.getDeepLogLizardSentBytes(st));

		// Background information sent bytes.
		Assert.assertEquals(0,Statistics.Test.getBackgroundInformationSentBytes(st));
		st.reportBackgroundInformationSentBytes(60);
		st.reportBackgroundInformationSentBytes(5);
		Assert.assertEquals(65,Statistics.Test.getBackgroundInformationSentBytes(st));
	}

	@Test
	public void testCustomRandom(){
		long seed = 10;
		Random r= new Random(seed);
		CustomRandom cr= new CustomRandom(seed);
		for(int i=0;i<100;i++)
			Assert.assertEquals(r.nextDouble(), cr.nextDouble(), 0);
		long mseed=cr.getSeed();
		for(int i=0;i<100;i++)
			Assert.assertEquals(r.nextDouble(), cr.nextDouble(), 0);
		cr.setSeed(mseed);
		// restore r state at 100th call.
		r.setSeed(10);
		for(int i=0;i<100;i++)
			r.nextDouble();
		// Check that the sequences after restoring are the same.
		for(int i=0;i<100;i++)
			Assert.assertEquals(r.nextDouble(), cr.nextDouble(), 0);
	}

}
