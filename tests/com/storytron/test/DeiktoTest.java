package com.storytron.test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import storyTellerPackage.SavedStory;

import com.storytron.enginecommon.BadStoryworldException;
import com.storytron.enginecommon.EngineDiedException;
import com.storytron.enginecommon.IncompatibleVersionException;
import com.storytron.enginecommon.SessionLogoutException;
import com.storytron.enginecommon.SharedConstants;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.Swat;
import com.storytron.uber.Actor;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Prop;
import com.storytron.uber.deiktotrans.DeiktoLoader.BadVersionException;
import com.storytron.uber.operator.OperatorDictionary;

import engPackage.Janus;
import engPackage.StorytellerPlayerIO;


public class DeiktoTest {

	/** Tells if two different streams holds the same data. */
	static void assertStreamEquals(String message,InputStream is,InputStream is2) throws IOException {
		assertTrue(is!=is2);
		int b1, b2;
		int counter=0;
		do {
			b1=is.read();
			b2=is2.read();
			assertTrue("at byte "+counter+", "+b1+"=/="+b2+": "+message,b1==b2);
			//if (b1!=b2)
			//	System.out.println(counter+": "+b1+"=/="+b2);
			counter++;
		} while(b1!=-1 && b2!=-1);
	}
	
	private static Swat swat;
	private static final String storyworld = "res/data/BoP2K.stw";
	private static final String storytraceFile = "res/data/janusstresstest.str";
	
	@BeforeClass
	public static void startSwat() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable(){
			public void run() {
				swat = new Swat(storyworld);
			}
		});
	}

	
	@Test
	public void testCloneDeiktoState() throws Exception {
		File f=new File(storyworld);
		InputStream is = new FileInputStream(f);
		Deikto dk=new Deikto(is,f,false);
		Deikto clone = dk.cloneWorldShareLanguage();
		
		clone.fixatePTraitValues();

		float oldValue = dk.getActor("Afghanistan").getP(dk.getPropTrait("Undesirable_Desirable"), dk.getProp("Afghanistan: hand over bin Laden"));
		
		float newValue = oldValue>0 ? -0.5f : 0.5f;
		
		Prop binLaden = clone.getProp("Afghanistan: hand over bin Laden");
		Actor afghanistan = clone.getActor("Afghanistan");
		FloatTrait desirable_Undesirable = clone.getPropTrait("Undesirable_Desirable");
		afghanistan.setP(desirable_Undesirable, binLaden, newValue);
		assertEquals(newValue, afghanistan.getP(desirable_Undesirable, binLaden), 0.0001);

		assertEquals(oldValue,dk.getActor("Afghanistan").getP(dk.getPropTrait("Undesirable_Desirable"), dk.getProp("Afghanistan: hand over bin Laden")),0.0001);
	}


	/** Tests if BoP2K.stw is saved exactly the same as it is read.
	 * This intends to catch errors in the save/loading routines. */
	@Test
	public void testDeiktoInputStream() throws IOException, BadVersionException {
		
		final String storyworldCopy = "res/data/BoP2KTest.stw";
		
		List<String> ignoredImages = Arrays.asList(new String[]{
								"image11509.png",  
								"image11510.png", 
								"image14601.png"
								});
		
		swat.writeStoryworld(new File(storyworldCopy));
		
		InputStream is = new FileInputStream(storyworld);
		InputStream is2 = new FileInputStream(storyworldCopy);
		while('\n'!=is.read());  // Skip the first line because it may be different
		while('\n'!=is2.read()); // without being important.

		// Check files
		assertStreamEquals("The file and the copy do not match.",is,is2);
		is.close();
		is2.close();
		
		// See if resources where copied
		File resourceDir = Utils.getResourceDir(new File(storyworld));
		File resourceDirCopy = Utils.getResourceDir(new File(storyworldCopy));
		if (!resourceDir.exists())
			assertTrue(resourceDirCopy.getName()+" exists.",resourceDirCopy.exists());
		else {
			String[] copyList = resourceDirCopy.list();
			assertTrue(resourceDirCopy.getName()+" does not exist.", copyList!=null);
			LinkedList<String> resources = new LinkedList<String>();
			for(String rcopy:resourceDir.list())
				resources.add(new File(rcopy).getName());
			resources.remove(".svn");
			LinkedList<String> copy = new LinkedList<String>();
			for(String rcopy:copyList)
				copy.add(new File(rcopy).getName());
			for(String r:resources)
				assertTrue("File "+r+" is missing in "+resourceDirCopy.getName(),ignoredImages.contains(r) || copy.contains(r));
			for(String r:copy)
				assertTrue("File "+r+" is new in "+resourceDirCopy.getName(),resources.contains(r));
		}
	}

	@Test
	public void testSaveEngineState() throws Exception {
		// load operators
		OperatorDictionary.loadOperators();

		// load saved history
		InputStream br = new FileInputStream(storytraceFile);
		SavedStory ss = new SavedStory();
		ss.readFormatHeader(br);
		DataInputStream di = SavedStory.convertInputStreamToDataInput(br);
		ss.readBody(ss.readHeader(di));
		LinkedList<Integer> input = ss.recordedInput;
		
		// start engine
		Janus janus = new Janus();
	
		System.out.println("playing the whole input: ");
		System.out.println(Arrays.toString(input.toArray()));
		String sessionID1=runEngineInput(null,janus, ss.startingVerbLabel, input, null);
		String sessionTempID = null;

		for(int inputLimit=0;inputLimit<input.size();inputLimit+=3){

			System.out.println("playing the first input half: ");
			System.out.println(Arrays.toString(input.subList(0, inputLimit).toArray()));

			// Now, we will play the story in pieces and see if the final states match.
			sessionTempID=runEngineInput(sessionTempID,janus, ss.startingVerbLabel, input.subList(0, inputLimit), null);

			System.out.println("playing the second input half: ");
			System.out.println(Arrays.toString(input.subList(inputLimit,input.size()).toArray()));

			sessionTempID=runEngineInput(sessionTempID,janus, ss.startingVerbLabel, input.subList(inputLimit,input.size()), janus.getWorldState(sessionTempID));

			Janus.Test.getSession(janus,sessionID1).engine.assertEqualStates(Janus.Test.getSession(janus,sessionTempID).engine);
		}
		janus.logout(sessionID1);
		if (sessionTempID!=null)
			janus.logout(sessionTempID);
	}
	
	private static String runEngineInput(String sessionID,Janus janus,String startVerbLabel,List<Integer> input,byte[] state) 
			throws RemoteException, IOException, InterruptedException, IncompatibleVersionException,
					SessionLogoutException, EngineDiedException, BadStoryworldException {
		if (sessionID==null)
			// create session
			sessionID =  janus.login(SharedConstants.REMOTE_INTERFACE_VERSION,UUID.randomUUID().toString(), "pw");
		
		janus.copyLocalDeikto(swat.dk, sessionID);

		// fire the session
		janus.startStory(sessionID, startVerbLabel, Utils.toArray(input), state, false);

		// wait till all the input has been played
		while(null!=janus.getTrigger(sessionID));
		while(!(Janus.Test.getSession(janus,sessionID).engine.getPlayerInputIO() instanceof StorytellerPlayerIO)) {
			Thread.sleep(300);
			while(null!=janus.getTrigger(sessionID));
		}
	
		Thread.sleep(1000);

		return sessionID;
	}

	
	@AfterClass
	public static void closeSwat(){
		swat.getMyFrame().dispose();
	}
	
	public static void main(String[] args) throws Exception {
		startSwat();
		new DeiktoTest().testSaveEngineState();
		closeSwat();
		System.exit(0);
	}
}
