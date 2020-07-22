package engPackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.InflaterInputStream;

import javax.swing.UIManager;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.storytron.enginecommon.BadStoryworldException;
import com.storytron.enginecommon.BgItemData;
import com.storytron.enginecommon.EngineDiedException;
import com.storytron.enginecommon.IncompatibleVersionException;
import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.LocalJanus;
import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.RehearsalResult;
import com.storytron.enginecommon.SessionLogoutException;
import com.storytron.enginecommon.SharedConstants;
import com.storytron.enginecommon.StorytellerRemote;
import com.storytron.enginecommon.StorytellerReturnData;
import com.storytron.enginecommon.SwatRemote;
import com.storytron.enginecommon.Triplet;
import com.storytron.enginecommon.Utils;
import com.storytron.enginecommon.VerbData;
import com.storytron.swat.util.Compressed;
import com.storytron.test.JanusStressTest;
import com.storytron.uber.Actor;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Prop;
import com.storytron.uber.Script;
import com.storytron.uber.Sentence;
import com.storytron.uber.Stage;
import com.storytron.uber.Verb;
import com.storytron.uber.Deikto.LogIssue;
import com.storytron.uber.Deikto.TraitType;
import com.storytron.uber.operator.OperatorDictionary;

/**
 * This is the server implementation.
 * It provides services for running a story, navigating the log data
 * of it, and doing rehearsals.
 * <p>
 * The interface of the server is defined by {@link com.storytron.enginecommon.StorytellerRemote}.
 * All of the server methods excepting
 * {@link com.storytron.enginecommon.StorytellerRemote#login(int, String, String)} receive a
 * session identifier to indicate over which session the operation must be applied.
 * <p>
 * The server creates for each session threads and buffers that are used
 * during the session lifetime. Both buffers and thread usage is bounded to preserve
 * the server resources. 
 * In addition to bounding resources, the server has a session timeout after which
 * inactive sessions are logout. 
 * <p>
 * The buffers used for each session may include:
 * <ul>
 * <li>A very small buffer for sentences meant to be delivered to storyteller.</li>
 * <li>A very small buffer for sentences meant to be processed by the engine.</li>
 * <li>A medium sized buffer for storing surface log data generated while playing a story.
 *     This log data must be downloaded by storyteller in order to have the engine continue
 *     its execution when it is full.</li>  
 * <li>A medium sized buffer for storing detailed log data generated while playing a story.
 *     This detailed log data is generated only for small portions of a story on user
 *     demand. When the buffer is full, no more data is produced until the client empties
 *     the buffer by downloading the data.</li>
 * <li>Not really a buffer, but a file on disk to save intermediate engine states. These
 *     are used for collecting detailed log data. </li>
 * <li>A buffer for storing Rehearsal results.</li>
 * </ul>
 * The sentence and rehearsal buffers are maintained by Janus. The log data buffers are maintained
 * by {@link TreeLogger}. The disk file for engine states is handled by {@link LogDataCollector}.
 * For more information information on logging workings see {@link com.storytron.swat.loglizard}. 
 * <p>
 * The detailed log data collection is delegated to the class {@link LogDataCollector}. This
 * class handles a thread where the engine is executed solely for the purpose
 * of collecting detailed log data. 
 * <p>
 * The threads per session are
 * <ul>
 * <li>A thread for running the engine playing the story.</li>
 * <li>A thread for running the engine collecting log data. This thread is started only if the
 * session is started with logging enabled (usually author sessions).</li>
 * <li>A thread for running rehearsals.</li>
 * </ul>
 * <p>
 * The server also implements logging statistics with the {@link Statistics} class.
 * Every certain period of time the collected statistics are sent to a database.
 * <p>
 * When starting a story with 
 * {@link #startStory(String, String, int[], byte[], boolean)},
 * a thread is created to run the engine.
 * When the client sends or asks data, the rmi thread attending the
 * client waits for the engine thread to synchronize with it using
 * one of the queues {@link SessionData#toEngine} and 
 * {@link SessionData#toStoryteller}.
 * <p>
 * On all the major requests, a time stamp in the session is updated.
 * The time stamp tells when a session did its last meaningful interaction.
 * If the time stamp is too old, the server can destroy the session.
 * A task is scheduled to run periodically to destroy sessions with old
 * time stamps.
 * */
public class Janus implements StorytellerRemote, LocalJanus, SwatRemote {
	private static final long serialVersionUID = 1L;
	
	/** 
	 * The size in bytes of the buffer for storing engine state.
	 * This affects performance by a few hundred milliseconds per 
	 * interaction when the server is loaded.
	 *
	 * Please, if you want to change this parameter use 
	 * {@link JanusStressTest} to test performance.
	 */
	private static final int ENGINE_STATE_BUFFER_SIZE = 200000;

	/** 
	 * Amount of milliseconds that should wait while a session is inactive
	 * before making it expire.
	 * */
	private static final long SESSION_TIMEOUT = 20*60*1000;
	
	/**
	 * Amount of milliseconds to wait between successive searches of 
	 * inactive sessions.
	 * */
	private static final long CHECK_FOR_EXPIRED_SESSIONS_INTERVAL = 20*60*1000;
	
	/** Amount of millisecond to wait before saving statistics. */
	private static final long SAVE_STATISTICS_INTERVAL = 2*3600000;
	
	private static final boolean DEBUG_STORYTELLER = false;	// Show debug messages
	private static int registryPortNumber = 1099;		// The IP port that the created RMI registry will run from
	private static Logger logger = Logger.getLogger(Janus.class.getName());
	private static Level loggerDetail = Level.OFF;
	private static FileHandler loggingFileHandler;
	private final int REHEARSAL_TIMEOUT = 20*60*1000; // 20 minutes
	private final Statistics stats = new Statistics();
	private static final boolean checkLogins = false;
	
	
	private Timer sessionExpiredTimer = new Timer();
	private Timer saveStatisticsTimer;

	/**
	 * This holds the data for every user session, as a collection of SessionData objects.
	 * Each session data object is stored with a session ID string as the key.
	 */
	private ConcurrentHashMap<String, SessionData> sessionList = new ConcurrentHashMap<String, SessionData>();

	/** 
	 * This maps user names to session identifiers, for the users
	 * that are logged in.
	 *  */
	private ConcurrentHashMap<String, String> userSessionList = new ConcurrentHashMap<String, String>();
	
	/** This maps storyworld names to instances of Deikto, for Storyteller */
	private ConcurrentHashMap<String, DeiktoCountedRef> deiktoList = new ConcurrentHashMap<String, DeiktoCountedRef>();
	private static final class DeiktoCountedRef {
		Deikto d; int refcount=1;
		DeiktoCountedRef(Deikto d){ this.d=d; }
	}
	
	/** A map of semaphores to control access to deiktoList. */
	private LockMap<String> deiktoListSems = new LockMap<String>();
	
	static class StopEngineException extends RuntimeException {
		public static final long serialVersionUID = 0L; 
		protected StopEngineException() {
			super();
		}
	}
	
	private class CancelRehearsalTask extends TimerTask {
		private SessionData sD;
		
		protected CancelRehearsalTask(SessionData sD) {
			super();
			this.sD = sD;
		}
		
		public void run() {
			try {
				cancelRehearsal(sD.sessionID);
			} catch (RemoteException e) {
				e.printStackTrace();
				logger.fine("Exception when cancelling rehearsal: " + e.toString());
			}
			
		}
	}
	
//**********************************************************************
	/**
	 * The purpose of the SessionData class is to enable multiple users to 
	 * access Swat by separating the server data and objects that each user 
	 * session would access.  Those objects include the storyteller thread, Engine,
	 * the storyteller synchronization queues, and Deikto.
	 */
	public static class SessionData {
		public enum Command {
			NONE,
			RESTART
		}
		public Deikto dk;
		private String storyworldID;
		private boolean isAuthorSession; 
		Deikto swatDk;
		private long timestamp = System.currentTimeMillis();
		private long storytellerTimestamp = timestamp;
		private long rehearsalTimestamp = timestamp;
		private String sessionID, loginName;
		//private Storyteller st;
		protected ArrayList<VerbData> verbData = new ArrayList<VerbData>();
		//private Verb centralVerb;
		ArrayList<Triplet<Script.Type,String[],String>> poisonings = new ArrayList<Triplet<Script.Type,String[],String>>();
		public Engine engine;
		private Thread storytellerThread;
		private Thread rehearsalThread;

		private Exception engineException = null;
		
		/** Used to move values from storyteller to the engine. */
		BlockingQueue<Integer> toEngine = new ArrayBlockingQueue<Integer>(2);
		/** Used to move values from engine to storyteller. */
		BlockingQueue<StorytellerReturnData> toStoryteller = new ArrayBlockingQueue<StorytellerReturnData>(2);
		
		LogDataCollector logDataCollector;
		
		volatile boolean storyIsStopped = false;
		//private String loginName = "";
		protected Script alteredScript;
		private int rehearsalProgress = 0;	// Used for the rehearsal progress bar
		private RehearsalResult rehearsalResult;
		private boolean rehearsalIsCancelled = false;
		private CancelRehearsalTask timeoutTimer;
		public LinkedList<Integer> lastInputs = new LinkedList<Integer>();
		public ByteArrayOutputStream savedStateBuffer = new ByteArrayOutputStream(ENGINE_STATE_BUFFER_SIZE);
		
		PlayerInputRecorder inputRecorder = null;

		public SessionData(String session, String login) {
			sessionID = session;
			loginName = login;
		}
		
		@SuppressWarnings("deprecation")
		public void reset() throws InterruptedException {
			timeoutTimer = null;
			storyworldID = null;

			if (storytellerThread!=null) {
				storytellerThread.interrupt();
				// wait for the thread to die
				// kill it if it delays too much 
				storytellerThread.join(30000); 
				if (storytellerThread.isAlive())
					storytellerThread.stop();
				storytellerThread = null;
			}
			if (rehearsalThread!=null) {
				rehearsalThread.stop();
				rehearsalThread = null;
			}
			
			if (logDataCollector!=null) {
				logDataCollector.reset();
				logDataCollector = null;
			}
			
			//private Storyteller st;
			engineException = null;
			verbData.clear();
			//centralVerb=null;
			poisonings.clear();
			engine=null;
			storyIsStopped = false;
			alteredScript=null;
			rehearsalProgress=0;
			rehearsalResult=null;
			rehearsalIsCancelled = false;
			inputRecorder = null;
			lastInputs.clear();
			savedStateBuffer.reset();
			timestamp = System.currentTimeMillis();
			storytellerTimestamp = timestamp;
			rehearsalTimestamp = timestamp;
			toStoryteller.clear();
			toEngine.clear();
		}
		
	}
	
	public Janus() throws RemoteException,
						SAXException, IOException, ParserConfigurationException {
		// Set the logger to record detail at the finest level
		Handler[] handlers = Logger.getLogger( "" ).getHandlers();
	    for ( int index = 0; index < handlers.length; index++ )
	      handlers[index].setLevel( loggerDetail );
	   
		logger.setLevel(loggerDetail);
		
		OperatorDictionary.loadOperators(); 

		// Logout everybody on exit.
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){
				try {
					for(SessionData sd:sessionList.values())
						logout(sd);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				sessionList.clear();
			}
		});
		
		// Check every hour for the existence of inactive sessions.
		sessionExpiredTimer.schedule(new TimerTask(){
			@Override
			public void run() {
				long t = System.currentTimeMillis();
				for(Map.Entry<String,SessionData> e:sessionList.entrySet())
					if (t-e.getValue().timestamp>SESSION_TIMEOUT) {
						try {
							logger.finest(e.getKey()+" session expired");
							logout(e.getKey());
						} catch(Exception ex){
							logger.log(Level.WARNING,"session timeout exception",ex);
						}
					}
			}
		}, 2*CHECK_FOR_EXPIRED_SESSIONS_INTERVAL, CHECK_FOR_EXPIRED_SESSIONS_INTERVAL);

		// log statistics to db only if this is a remote server
		setCollectStatistics(SharedConstants.isRemote);
		
		logger.fine("Constructed Janus");
	}

	/** Tells if we should collect statistics or not. */
	private void setCollectStatistics(boolean collect) {
		if (collect) {
			if (saveStatisticsTimer==null) {
				stats.reset();
				saveStatisticsTimer = new Timer();
				saveStatisticsTimer.schedule(new TimerTask(){
					@Override
					public void run() {
						try {
							if (saveStatisticsTimer!=null)
								stats.saveStatistics(SharedConstants.getDefaultServiceNum());
						} catch (Exception e) {
							logger.log(Level.WARNING,"thrown when saving statistics",e);
						}
					}
				}, 0, SAVE_STATISTICS_INTERVAL);
			}
		} else if (saveStatisticsTimer!=null) {
			saveStatisticsTimer.cancel();
			saveStatisticsTimer=null;
		}
	}
	
	private boolean isCollectingStatistics(){ return saveStatisticsTimer!=null; }
	
	/** 
	 * Kills timer threads so applications running Janus
	 * wont be prevented from finishing. 
	 * */
	public void shutdown(){
		sessionExpiredTimer.cancel();
		if (isCollectingStatistics())
			setCollectStatistics(false);
	}
	
	/** 
	 * For testing purposes only. Returns the reference count 
	 * for the given storyworld.
	 * @throws NullPointerException if the storyworld is not loaded.
	 * */
	public int getDeiktoListRefCount(String storyworldID){
		return deiktoList.get(storyworldID).refcount;
	}
	
	private static class RehearsalThread extends Thread {
		private byte[] worldState;
		private int[] input;
		private SessionData sD;
		public RehearsalThread(SessionData sD, int[] input,byte[] worldState){
			this.sD=sD;
			this.worldState = worldState;
			this.input = input;
			setPriority(Thread.MIN_PRIORITY);
		} 
		public void run() {
			try {
				runRehearsal(sD, input, worldState);
			} catch (ThreadDeath td) {
				logger.info("Rehearsal thread was stopped");
			}
		}		
	}
	
//**********************************************************************
	public void startRehearsal(String sessionID, int[] input, byte[] worldState) throws RemoteException { 
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			return;

		synchronized (sD) {
			sD.timestamp = System.currentTimeMillis();
			sD.rehearsalTimestamp = sD.timestamp;
			
			// If the rehearsal has already been started skip
			// the rest of the call.
			if (sD.rehearsalThread!=null)
				return;

			if (sD.swatDk==null) 
				sD.swatDk = sD.dk;
		
			sD.logDataCollector = null;
			sD.rehearsalThread = new RehearsalThread(sD,input,worldState); 
			sD.rehearsalThread.start();
			
			// Cancel rehearsal thread if it takes too long to run
			sD.timeoutTimer = new CancelRehearsalTask(sD);
			sessionExpiredTimer.schedule(sD.timeoutTimer, REHEARSAL_TIMEOUT);
			
		}
	}
	
	private static void runRehearsal(SessionData sD, int[] input,byte[] worldState) {
		sD.rehearsalProgress = 0;
		
		sD.verbData.clear();
		for (Verb zVerb: sD.swatDk.getVerbs())
			sD.verbData.add(new VerbData(zVerb));

		// Make the rehearsal repeatable
		RehearsalIO.random = new Random(0);
		
		int totalVerbActivations = 0;
		int totalVerbCandidacies = 0;
		int firstVerbIndex=0;
		for (int i=0; (i < 10); ++i) {
			if (sD.rehearsalIsCancelled)
				break;

			sD.dk = sD.swatDk.cloneWorldShareLanguage();
			
			sD.engine = new Engine(sD.dk,null);
			sD.engine.logger.setLogging(false);
			sD.engine.rehearsalLogger = new RehearsalLogger(sD);
			RehearsalIO rio = new RehearsalIO(sD.engine);
			
			Pair<int[],Boolean> loadOutput = null;
			if (worldState!=null) {
				loadOutput = loadWorldState(sD,worldState);
				if (loadOutput==null)
					return;
			}
			int[] totalInput=Utils.concat(loadOutput!=null?loadOutput.first:null,input);
			if (totalInput!=null && totalInput.length>0)
				sD.engine.setPlayerInputIO(new ResumerIO(totalInput,sD,rio));
			else {
				sD.engine.runningRehearsal = true;
				sD.engine.setPlayerInputIO(rio);
			}

			if (loadOutput==null || loadOutput.second)
				sD.engine.init();
			
			try {
				sD.engine.run();
			} catch (InterruptedException e){
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
				/*System.out.println("Storybook:");
				for(String entry:sD.engine.storybook)
					System.out.println(entry);
				System.out.println();*/
			}
			firstVerbIndex=rio.firstVerbIndex;

			sD.rehearsalProgress+=5;
			findLoopyBoobies(sD);
			sD.rehearsalProgress+=5;
		}
		for (VerbData vd: sD.verbData) {
			totalVerbActivations += vd.activations;
			totalVerbCandidacies += vd.candidacies;
		}
		System.out.println("Verb activations: "+totalVerbActivations);
		sD.rehearsalResult = new RehearsalResult(sD.verbData, firstVerbIndex, sD.poisonings);
	}
	
	public RehearsalResult getRehearsalResults(String sessionID) throws RemoteException {
		RehearsalResult result;
		SessionData sD = sessionList.get(sessionID);
		if (sD == null)
			result = null;
		else
			result = sD.rehearsalResult;
		
		if (isCollectingStatistics())
			stats.reportRehearsalReactionTime(System.currentTimeMillis()-sD.rehearsalTimestamp);
		
		// Remove the session data, to clear the memory
		logger.finest(sessionID+" finished rehearsal");
		logout(sessionID);
		
		return result;
		
	}
	
	public void cancelRehearsal(String sessionID) throws RemoteException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			return;

		if (isCollectingStatistics())
			stats.reportRehearsalCancelTime(System.currentTimeMillis()-sD.rehearsalTimestamp);
		sendDebugMessage("cancelRehearsal()");
		logout(sD.sessionID);
	}
	//**********************************************************************
	public static void findLoopyBoobies(SessionData sD) {
		boolean[] verbIsDone = new boolean[9999];
		for (int i=0; (i<9999); ++i)
			verbIsDone[i]=false;
		for (int j=sD.engine.getHistoryBookSize()-1; (j>0); --j) {
			Sentence sn=sD.engine.getHistoryBookPage(j);
			int mainVerbIndex = sn.getIWord(Sentence.Verb);
			if (!verbIsDone[mainVerbIndex]) {
				verbIsDone[mainVerbIndex]=true;
				int mainSubject = sn.getIWord(Sentence.Subject);
				int mainDirObject = sn.getIWord(Sentence.DefDirObject);
				int i=0;
				Sentence tempSentence = sn;
				while (tempSentence.getCausalEvent()>=0) {
					++i;
//					if (tempSentence.getCausalEvent()==engine.historyBook.indexOf(tempSentence))
//						System.out.println("invalid CausalEvent");
//					if (tempSentence.getCausalEvent()==0)
//						System.out.println("invalid CausalEvent");
					tempSentence = sD.engine.getHistoryBookPage(tempSentence.getCausalEvent());
					if ((tempSentence.getIWord(Sentence.Verb) == mainVerbIndex) 
							& (tempSentence.getIWord(Sentence.Subject) == mainSubject) 
							& (tempSentence.getIWord(Sentence.DefDirObject) == mainDirObject)) {
						if (i<10)
							++(sD.verbData.get(mainVerbIndex).cLoopyBoobies[i]);
						i=0; // reset loop length counter
					}
				}
			}
		}
	}
//**********************************************************************		
/*
	public Sentence getPlan() {
	 return(st.getPlan());
	}
*/	
	private static class StorytellerThread extends Thread {
		SessionData sD;
		public StorytellerThread(SessionData sD) { 
			super(sD.sessionID);
			this.sD=sD;
		}
		public void run() {
			sD.engine.rehearsalLogger = null;
			sD.engine.runningRehearsal = false;
			
			try {
				sD.engine.run();
			} catch (StopEngineException stee) {
				sD.engineException = stee;
			} catch (InterruptedException e) {
				sD.engineException = e;
			} catch (Exception e){
				e.printStackTrace();
				sD.engineException = e;
			}
		}
	}
//**********************************************************************		
	//public void startStory(String verbLabel) {
	// Preconditions: 
	// 1) The session, specified in sessionID, must have been created,
	// presumably by calling login()
	// 2) The SessionData instance for the session must refer to an instance
	// of Deikto
	public boolean startStory(final String sessionID, String verbLabel, int[] input, byte[] worldState, boolean logging) 
					throws RemoteException, SessionLogoutException  {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();
		
		synchronized(sD){
			sD.timestamp = System.currentTimeMillis();
			sD.storytellerTimestamp = sD.timestamp;

			try {
				if (sD.storytellerThread!=null)
					resetEngine(sD);
			} catch (InterruptedException e) {
				throw new RemoteException("Interrupted.",e);
			}
			
			if (logging) {
				try {
					sD.logDataCollector = new LogDataCollector();
					sD.logDataCollector.initLogLizardState();
				} catch(IOException e){
					e.printStackTrace();
					return false;
				}
			}
			
			Engine oldEngine = sD.engine;
			sD.engine = new Engine(sD.dk, new StorytellerPlayerIO(sD));
			
			Pair<int[],Boolean> loadOutput = null;
			if (worldState!=null) {
				loadOutput = loadWorldState(sD,worldState);
				if (loadOutput==null) {
					sD.engine = oldEngine;
					return false;
				}
			}
			int[] totalInput=Utils.concat(loadOutput!=null?loadOutput.first:null,input);
			
			sD.engine.logger.setLogging(logging);
			// Don't log scripts. We just want to log a few nodes.
			sD.engine.logger.logOnlyUpperLevels = true;

			// If user wants to log we will want to record player input to
			// reproduce the story later.
			if (logging) {
				sD.inputRecorder = new PlayerInputRecorder(sD,totalInput);
				if (sD.swatDk==null) 
					sD.swatDk = sD.dk.cloneWorldShareLanguage();
			} else
				sD.inputRecorder = null;

			if (totalInput!=null && totalInput.length>0)
				sD.engine.setPlayerInputIO(new ResumerIO(totalInput,sD,sD.engine.getPlayerInputIO()));

			if (loadOutput==null || loadOutput.second)
				sD.engine.init();
			
			// Note: using the name of the engine thread to pass the session ID
			sD.storytellerThread = new StorytellerThread(sD);

			sD.storytellerThread.start();
		}
		
		if (isCollectingStatistics()) {
			if (input!=null && input.length>0)
				stats.reportLoadedStoryTrace();
			if (worldState!=null)
				stats.reportLoadedSession();
			

			// get the ip address
			String ipAddress = "0.0.0.0";
	        try {
	            ipAddress = java.rmi.server.RemoteServer.getClientHost();
	        } catch (ServerNotActiveException snae) {
	        	ipAddress = "1.1.1.1";
	        }
	        
	        stats.reportClick(sD.storyworldID, new java.util.Date(), sD.sessionID, ipAddress, false, true, false);
		}
		
		return true;
	}
	
//**********************************************************************	
	public static void main(String args[]) {
		Registry registry;
		String logFileName;
		
		int serviceNum  = SharedConstants.getDefaultServiceNum();
		
		try {
			// Get the service number from a command line argument
			if (args.length > 0) {
			    try {
			    	serviceNum = Integer.parseInt(args[0]);
			    } catch (NumberFormatException e) {
			        System.err.println("Service number must be an integer");
			        System.exit(1);
			    }
			}
			
			int sessionPort = SharedConstants.getPort(serviceNum);
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			
			Janus.loggerDetail = Level.FINEST;
			Janus server = new Janus();
			
			// Export server using JERI 
			//StorytellerRemote StorytellerStub = JeriCustomServerSetup.export(server,sessionPort+SharedConstants.numServices,sessionPort);
			StorytellerRemote StorytellerStub = (StorytellerRemote) UnicastRemoteObject.exportObject(server, sessionPort);
			
			// Create the RMI registry.  
			// This replaces using the command-line "rmiregistry" tool
			try {
				// First attempt to bind to an existing registry
				registry = LocateRegistry.getRegistry();
				registry.bind(SharedConstants.getServiceName(serviceNum), StorytellerStub);
			} catch (RemoteException re) {
				// Create a registry if one does not already exist
				LocateRegistry.createRegistry(registryPortNumber);
				registry = LocateRegistry.getRegistry();
				registry.bind(SharedConstants.getServiceName(serviceNum), StorytellerStub);
			}
				
			// Log to a file when run on the server.
			logFileName = "log_" + SharedConstants.getServiceName(serviceNum); 
			loggingFileHandler = new FileHandler(logFileName, true);
			logger.addHandler(loggingFileHandler);
			
		} catch (java.io.IOException e) {
			// problem registering server
			System.out.println("Problem registering server");
			e.printStackTrace();
		}	      
		catch (Exception evt) {}
	}
//	**********************************************************************
	/**
	 * Checks compatibility of client and server. Throws an exception if they
	 * are not compatible.
	 * */
	private void checkClientCompatibility(int clientVersion) throws IncompatibleVersionException {
		if (clientVersion!=SharedConstants.REMOTE_INTERFACE_VERSION)
			throw new IncompatibleVersionException(SharedConstants.REMOTE_INTERFACE_VERSION);
	}
	// Log the user in and create the session
	// TODO: Refer to an external password file or database table
	public String login(int clientVersion,String userName, String password)  throws RemoteException, IncompatibleVersionException {
		logger.finer("login " + userName );
		
		checkClientCompatibility(clientVersion);

		// Authenticate the user and create the session
		SessionData sD;
		try {
			if (checkLogins == false) {
				// Create the session
				sD = startSession(userName);
				logger.finer("SessionID " + sD.sessionID);			
			} else {

				if (userName.equalsIgnoreCase("test")) {
					if (password.equals("pw")) {
						// Create the session
						sD = startSession(userName);
						logger.finer("SessionID " + sD.sessionID);

					} else 
						logger.finer("invalid password");
				} else
					logger.finer("invalid user: [" + userName + "]");
			}
		} catch (InterruptedException e) {
			throw new RemoteException("Interrupted.",e);
		}

		sD.isAuthorSession = true;
		if (isCollectingStatistics())
			stats.reportAuthorLogin();
		
		return sD.sessionID;
	}
//	**********************************************************************	
	// Generate a unique identifer and session data structure for the session.
	private SessionData startSession(String login) throws InterruptedException {
		
		UUID sessionObj = UUID.randomUUID();
		
		String sessionID = sessionObj.toString();
		
		SessionData sessionData = new SessionData(sessionID, login);
		logger.info("Started Session with ID:" + sessionData.sessionID);
		sessionList.putIfAbsent(sessionID, sessionData);
		String previousSessionID=userSessionList.put(login, sessionID);
		// logout a previous session if there was any.
		if (previousSessionID!=null) {
			// FD, May 30, 2008: Commenting lines bellow until having different users for testing
			//SessionData previousSd = sessionList.remove(previousSessionID); 
			//if (previousSd!=null) 
			//	synchronized (previousSd) {
			//		logout(previousSd);
			//	}
		}
		return sessionData;
	}
//	**********************************************************************
	/** Copies the storyworld in memory by building and parsing an XML string
	 * !!! This method is intended for use only when running Swat locally.
	 * Use {@link #loadDeiktoFromZip(String, byte[])} to copy Deikto when running Swat remotely
	 */
	public void copyLocalDeikto(Deikto originalDk, String sessionID) throws BadStoryworldException {
		// Create the new copy of Deikto and fill it with the
		// data that Engine changes
		Deikto newDk = null;
		try {
			logger.finer(sessionID + " copyLocalDeikto");
			
			SessionData sD = sessionList.get(sessionID);

			synchronized(sD){
				newDk = originalDk.cloneWorldShareLanguage(); 
				
				if (sD.storytellerThread!=null)
					sD.reset();

				sD.swatDk = originalDk;
				sD.dk = newDk;

				newDk.fixatePTraitValues();
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.INFO,"Exception in Janus.copyLocalDeikto(): ",e);
			throw new RuntimeException(e);
		}
		
		if (newDk==null)
			return;
		
		// check for errors
		LinkedList<LogIssue> errors = newDk.checkScripts(null,false);
		if (!errors.isEmpty()) {
			int count=0;
			ArrayList<Triplet<Script.Type,String[],String>> scriptErrors = new ArrayList<Triplet<Script.Type,String[],String>>(15);
			for(LogIssue l:errors){
				if (count++>=15)
					break;
				scriptErrors.add(new Triplet<Script.Type,String[],String>(l.s.getType(),l.sp.getScriptLocators(l.s),l.result));
			}
			throw new BadStoryworldException(scriptErrors);
		}
	}
	
	private int getProtagonist() { return 1; }

//	**********************************************************************
	public byte[] getWorldState(String sessionID) throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		
		if (sD==null)
			throw new SessionLogoutException();

		byte[] bs;
		synchronized(sD){
			ByteArrayOutputStream bos = new ByteArrayOutputStream(ENGINE_STATE_BUFFER_SIZE);
			try {
				DataOutputStream dos = new DataOutputStream(bos);
				dos.writeInt(sD.lastInputs.size());
				for(int i:sD.lastInputs)
					dos.writeInt(i);
				dos.write(sD.savedStateBuffer.toByteArray());
				dos.close();
			} catch(IOException e){
				e.printStackTrace();
			}
			bs = bos.toByteArray();
		}
		
		if (isCollectingStatistics())
			stats.reportSavedSession();
		
		return bs;
	};
	
	/**
	 * Returns null on error, or the inputs to play to recover the state, and whether
	 * the engine needs to be initialized (Calling {@link Engine#init(String, boolean)}).
	 * */
	private static Pair<int[],Boolean> loadWorldState(SessionData sD, byte[] worldState) {
		try {
			ByteArrayInputStream ba = new ByteArrayInputStream(worldState);
			DataInputStream dis = new DataInputStream(ba);	
			int inputLength = dis.readInt();
			if (inputLength*4>=worldState.length)
				return null;
			
			int[] input = new int[inputLength];
			for(int i=0;i<input.length;i++)
				input[i]=dis.readInt();

			boolean needsInit = true;
			if (ba.available()>0) {
				needsInit = false;
				ObjectInputStream ois = new ObjectInputStream(ba); 
				sD.engine.loadState(ois);
				ois.close();
			}
			sD.engine.logger.commandCount=0;
			
			if (sD.logDataCollector!=null) {
				sD.savedStateBuffer.reset();
				int offset = 4+input.length*4;
				if (offset<worldState.length) {
					sD.savedStateBuffer.write(worldState, offset, worldState.length-offset);
					sD.logDataCollector.saveStateBuffer(sD);
				}
			}
			
			return new Pair<int[],Boolean>(input,needsInit);
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("Exception in Janus.loadWorldState(): " + e);
			return null;
		}		 
	}
	
	/** Resets all the session data except the loaded storyworld. */
	private void resetEngine(SessionData sD) throws InterruptedException {
		if (sD.storytellerThread!=null) {
			Deikto dk=sD.swatDk;
			String storyworldID=sD.storyworldID;
			sD.reset();
			sD.swatDk=dk;
			sD.storyworldID=storyworldID;
			sD.dk = sD.swatDk.cloneWorldShareLanguage(); 
			sD.dk.fixatePTraitValues();
		}
	} 
	
	/** Called whenever the engine initiates a cycle. */
	static void onCycleStart(SessionData sD){
		if (sD.logDataCollector!=null)
			sD.logDataCollector.saveEngineState(sD);
	}

	
	// Read in the compressed dictionary
	public void loadDeiktoFromZip(String sessionID, byte[] zipData)  
						throws RemoteException, SessionLogoutException, BadStoryworldException, LimitException {
		SessionData sD = sessionList.get(sessionID);

		if (sD==null)
			throw new SessionLogoutException();

		logger.fine(sD.sessionID + " loadDeiktoFromZip");

		Deikto newDk = null;
		try {
			synchronized(sD){
				sD.reset();

				// Load the new dictionary as deikto
				LimitedInputStream is = new LimitedInputStream(new InflaterInputStream(new ByteArrayInputStream(zipData)));
				// limit the amount of uncompressed data that we will accept.
				is.resetByteCount(5000000);
				sD.dk = newDk = new Deikto(is,new File("Dictionary"),false);
				sD.dk.fixatePTraitValues();
			}
		} catch (LimitException e) {
			logger.log(Level.INFO,"Exception in Janus.loadDeiktoFromZip(): ",e);
			throw e;
		} catch (Exception e) {
			logger.log(Level.INFO,"Exception in Janus.loadDeiktoFromZip(): ",e);
			throw new RuntimeException(e);
		}
		
		if (newDk==null)
			return;
		
		// check for errors
		LinkedList<LogIssue> errors = newDk.checkScripts(null,false);
		if (!errors.isEmpty()) {
			int count=0;
			ArrayList<Triplet<Script.Type,String[],String>> scriptErrors = new ArrayList<Triplet<Script.Type,String[],String>>(15);
			for(LogIssue l:errors){
				if (count++>=15)
					break;
				scriptErrors.add(new Triplet<Script.Type,String[],String>(l.s.getType(),l.sp.getScriptLocators(l.s),l.result));
			}
			throw new BadStoryworldException(scriptErrors);
		}

	}
	
	// Tell Engine which option was pressed
	public boolean setResult(String sessionID, int newResult) 
				throws RemoteException, SessionLogoutException, EngineDiedException {

		boolean isDone = false;
		if (newResult == 1000) {
			// The "Done" button was pressed
			isDone = true;
			newResult = 0;
		}
		
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		sendDebugMessage("setResult");
		
		if (isCollectingStatistics()) {
			// get the ip address
			String ipAddress = "0.0.0.0";
	        try {
	            ipAddress = java.rmi.server.RemoteServer.getClientHost();
	        } catch (ServerNotActiveException snae) {
	        	ipAddress = "1.1.1.1";
	        }
	        
	        stats.reportClick(sD.storyworldID, new java.util.Date(), sD.sessionID, ipAddress, isDone, false, false);
		}
		return setResult(sD,newResult);
	}

	private boolean setResult(SessionData sD, int newResult) 
				throws EngineDiedException, SessionLogoutException {
		sD.timestamp = System.currentTimeMillis();
		sD.storytellerTimestamp = sD.timestamp;
		
		if (sD.engineException!=null) {
			if (!sessionList.containsKey(sD.sessionID))
				throw new SessionLogoutException();
			else
				throw new EngineDiedException(sD.engineException);
		}
		return sD.toEngine.offer(newResult);
	}

	
	int countNodes(Iterable<TreeLogger.Node> ns){
		int count = 0;
		for(TreeLogger.Node n:ns){
			count++;
			if (n.children!=null)
				count+=countNodes(n.children);
		}
		return count;
	};

	// Called by Storyteller to get trigger sentences and return data from Engine
	public StorytellerReturnData getTrigger(String sessionID) 
			throws RemoteException, SessionLogoutException, EngineDiedException {
		SessionData sD = sessionList.get(sessionID);
		StorytellerReturnData result=null;

		if (sD==null)
			throw new SessionLogoutException();

		sendDebugMessage("getTrigger()");
		
		if (sD.engineException!=null) {
			if (!sessionList.containsKey(sD.sessionID))
				throw new SessionLogoutException();
			else
				throw new EngineDiedException(sD.engineException);
		}
		result = sD.toStoryteller.poll();
	
		if (isCollectingStatistics()) {
			if (result!=null &&	result.inputEnded())
				stats.reportStorytellerReactionTime(System.currentTimeMillis()-sD.storytellerTimestamp);
			stats.reportSentenceSentBytes(serializedSize(result));
		}

		return result;
	}
	
	/** Finds the size of a serialized object. */
	private static int serializedSize(Serializable result) {
		if (result==null)
			return 0;

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(result);
			oos.close();
			return bos.toByteArray().length;

		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/** Returns the queued log data for a given session. */
	@SuppressWarnings("unchecked")
	public Compressed<Object[]>[] getLogData(String sessionID) throws RemoteException{
		
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			return null;
		
		if (sD.engine!=null){
			LinkedList<Compressed<Object[]>> loggerNodes = new LinkedList<Compressed<Object[]>>();
			int bytesum = 0;
			for(Compressed<Object[]> cn:sD.engine.logger.popCompressedNodes()) {
				loggerNodes.add(cn);
				bytesum+=cn.object.length;
			}
			
			if (!loggerNodes.isEmpty()) {
				if (isCollectingStatistics())
					stats.reportSurfaceLogLizardSentBytes(bytesum);
				Compressed<Object[]>[] lns = new Compressed[loggerNodes.size()];
				loggerNodes.toArray(lns);
				return lns;
			}
		}
		
		return null;
	};
	
	/** 
	 * <p>Queues request of log branches starting from fromTree to toTree.
	 * Less subtrees are returned if they are not available.</p> 
	 * */
	public boolean requestLogData(String sessionID, int fromTree, int toTree, int skipCommands) 
					throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();
		
		//System.out.println("requesting: "+fromTree+" "+toTree);
		synchronized(sD){
			
			sD.timestamp = System.currentTimeMillis();

			if (sD.logDataCollector!=null)
				return sD.logDataCollector.requestLogData(sD,fromTree, toTree, skipCommands);
			else 
				return false;
		}
	}

	/** Gets the data requested with {@link #requestLogData(String, int, int)}. */
	public Triplet<Integer,Integer,Compressed<Object[]>[]> getRequestedLogData(String sessionID) 
					throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();
		
		synchronized(sD){
			
			sD.timestamp = System.currentTimeMillis();

			if (sD.logDataCollector!=null) {
				Pair<Triplet<Integer,Integer,Compressed<Object[]>[]>,Long> p = 
												sD.logDataCollector.getRequestedLogData();
				
				if (p==null)
					return null;
				else {
					if (isCollectingStatistics()) {
						stats.reportLogLizardReactionTime(sD.timestamp-p.second);
						int bytesum=0;
						for(Compressed<Object[]> c:p.first.third)
							bytesum+=c.object.length;
						if (p.first.third.length>0)
							stats.reportDeepLogLizardSentBytes(bytesum);
					}
					return p.first;
				}
			} else
				return null;
		}
	}
	
	// Prints debug messages that show the status of the latches
	static void sendDebugMessage(String methodName) {
		if (DEBUG_STORYTELLER)
				System.out.print("Running " + methodName);
	}
	
	// End the story and restore Deikto to the old state when closing storyteller
	public void closeStoryteller(String sessionID) throws RemoteException {
		logger.finest(sessionID + " closeStoryTeller ");
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			return;
		releaseEngine(sD);
	}
	
	// removes session data
	public void logout(String sessionID) throws RemoteException {
		logger.finest(sessionID+" logout");

		// Remove the session data, to clear the memory
		SessionData sD=sessionID==null?null:sessionList.remove(sessionID);
		// Stop log lizard thread.
		if (sD!=null) {
			try {
				synchronized (sD) {
					
					if (isCollectingStatistics()) {
						// get the ip address
						String ipAddress = "0.0.0.0";
				        try {
				            ipAddress = java.rmi.server.RemoteServer.getClientHost();
				        } catch (ServerNotActiveException snae) {
				        	ipAddress = "1.1.1.1";
				        }
				        
				        stats.reportClick(sD.storyworldID, new java.util.Date(), sD.sessionID, ipAddress, false, false, true);
					}			

					if (sD.loginName!=null)
						userSessionList.remove(sD.loginName);
					logout(sD);
				}
			} catch (InterruptedException e) {
				throw new RemoteException("Interrupted.",e);
			}
		}
	}
	@SuppressWarnings("deprecation")
	private void logout(SessionData sD) throws InterruptedException {
		if (isCollectingStatistics()) {
			if (sD.isAuthorSession)
				stats.reportAuthorLogout();
			else
				stats.reportPlayerLogout();
		}
		
		synchronized (sD) {
			if (sD.storyworldID!=null) {
				try {
					// Decrease reference count of the storyworld and purge it
					// if it reaches zero.
					deiktoListSems.acquire(sD.storyworldID);
					try {
						DeiktoCountedRef dr = deiktoList.get(sD.storyworldID);
						if (dr!=null) {
							dr.refcount--;
							if (dr.refcount<=0)
								deiktoList.remove(sD.storyworldID);
						}
					} finally {
						deiktoListSems.release(sD.storyworldID);
					}
				} catch (InterruptedException e) {}
				
				if (isCollectingStatistics() && sD.logDataCollector==null)
					stats.reportStoryworldCount(deiktoList.size());
			}

			if (sD.timeoutTimer!=null) {
				sD.timeoutTimer.cancel();
				sessionExpiredTimer.purge();
			}

			if (sD.rehearsalThread!=null){
				sD.rehearsalThread.stop();
				sD.rehearsalThread=null;
			}

			if (sD.logDataCollector!=null)
				sD.logDataCollector.reset();

			if (!sD.storyIsStopped)
				releaseEngine(sD);
		}
	}

	private void releaseEngine(SessionData sD){
		/* Release latches that can block the engine
		 * To be uncommented later when the excecptions that
		 * it causes are prevented or handled.
		 */
		sD.storyIsStopped = true;
		if (sD.engine!=null)
			sD.engine.setStoryIsOver(true);
		if (sD.storytellerThread!=null)
			sD.storytellerThread.interrupt();
	}
	
//**********************************************************************	
	public int getRehearsalProgress(String sessionID)  throws RemoteException {
		logger.finest(sessionID + " getRehearsalProgress ");
		SessionData sD = sessionList.get(sessionID);
		if (sD == null)
			return 100;
		else if (sD.rehearsalIsCancelled)
			return SharedConstants.TASK_CANCELLED;
		else
			return sD.rehearsalProgress;
	}

//**********************************************************************
	// Send the storybook text to storyteller.
	public String[] getStorybookEntries(String sessionID)  throws RemoteException, SessionLogoutException {
		logger.finest(sessionID + " getStorybook ");
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		sD.timestamp = System.currentTimeMillis();
		LinkedList<String> entries = new LinkedList<String>();
		int count = sD.engine.storybookQueue.drainTo(entries);
		return entries.toArray(new String[count]);
	}

	public String getCopyright(String sessionID) throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();
		
		synchronized (sD) {
			sD.timestamp = System.currentTimeMillis();
			return sD.dk.getCopyright();
		}
	};
	
	/** Gets the visible traits of a given actor. */
	public float[] getActorTraits(String sessionID,String actor) 
					throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		synchronized (sD) {
			sD.timestamp = System.currentTimeMillis();
			Actor a = sD.dk.getActor(actor);
			Actor protagonist = sD.dk.getActor(getProtagonist());
			if (!a.getKnowsMe(protagonist))
				return null;

			float[] values = new float[sD.dk.getVisibleTraitCount(TraitType.Actor)];
			int i=0;
			for(FloatTrait t:sD.dk.getVisibleTraits(TraitType.Actor))
				values[i++] = protagonist.getP(t,a);

			return values; 
		}
	}
	
	/** Gets the visible traits of a given stage. */
	public float[] getStageTraits(String sessionID,String stageName) 
					throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		synchronized (sD) {
			sD.timestamp = System.currentTimeMillis();
			final Stage stage = sD.dk.getStage(stageName);
			final Actor protagonist = sD.dk.getActor(getProtagonist());
			if (!stage.getKnowsMe(protagonist))
				return null;

			float[] values = new float[sD.dk.getVisibleTraitCount(TraitType.Stage)];
			int i=0;
			for(FloatTrait t:sD.dk.getVisibleTraits(TraitType.Stage))
				values[i++] = protagonist.getP(t,stage);

			return values; 
		}
	}
	/** Gets the visible traits of a given prop. */
	public float[] getPropTraits(String sessionID,String propName) 
					throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		synchronized (sD) {
			sD.timestamp = System.currentTimeMillis();
			final Prop prop = sD.dk.getProp(propName);
			final Actor protagonist = sD.dk.getActor(getProtagonist());
			if (!prop.getKnowsMe(protagonist))
				return null;

			float[] values = new float[sD.dk.getVisibleTraitCount(TraitType.Prop)];
			int i=0;
			for(FloatTrait t:sD.dk.getVisibleTraits(TraitType.Prop))
				values[i++] = protagonist.getP(t,prop);
			return values; 
		}
	}
	
	public String[] getActorsUnknownToProtagonist(String sessionID)
					throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		Actor protagonist = sD.dk.getActor(getProtagonist());
		synchronized (sD) {
			// count unknown actors
			int count=0;
			for(Actor a:sD.dk.getActors()) {
				if (!a.getKnowsMe(protagonist))
					count++;
			}
			String[] unknowns = new String[count];
			count=0;
			for(Actor a:sD.dk.getActors()) {
				if (!a.getKnowsMe(protagonist)) {
					unknowns[count]=a.getLabel();
					count++;
				}
			}
			
			return unknowns;
		}
	};
		
	public String[] getStagesUnknownToProtagonist(String sessionID)
					throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		Actor protagonist = sD.dk.getActor(getProtagonist());
		synchronized (sD) {
			// count unknown actors
			int count=0;
			for(Stage s:sD.dk.getStages()) {
				if (!s.getKnowsMe(protagonist))
					count++;
			}
			String[] unknowns = new String[count];
			count=0;
			for(Stage s:sD.dk.getStages()) {
				if (!s.getKnowsMe(protagonist)) {
					unknowns[count]=s.getLabel();
					count++;
				}
			}
			
			return unknowns;
		}
	}
	
	public String[] getPropsUnknownToProtagonist(String sessionID)
					throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		Actor protagonist = sD.dk.getActor(getProtagonist());
		synchronized (sD) {
			// count unknown actors
			int count=0;
			for(Prop p:sD.dk.getProps()) {
				if (!p.getKnowsMe(protagonist))
					count++;
			}
			String[] unknowns = new String[count];
			count=0;
			for(Prop p:sD.dk.getProps()) {
				if (!p.getKnowsMe(protagonist)) {
					unknowns[count]=p.getLabel();
					count++;
				}
			}
			return unknowns;
		}
	};


	public BgItemData getActorBgData(String sessionID,String actorName) 
				throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		synchronized (sD) {
			sD.timestamp = System.currentTimeMillis();
			BgItemData bgi = sD.dk.getActorBgData(actorName, true);
			if (isCollectingStatistics())
				stats.reportBackgroundInformationSentBytes(serializedSize(bgi));
			return bgi;	
		}	
	}
	
	public BgItemData getStageBgData(String sessionID,String stageName) throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		synchronized (sD) {
			sD.timestamp = System.currentTimeMillis();
			BgItemData bgi = sD.dk.getStageBgData(stageName, true);
			if (isCollectingStatistics())
				stats.reportBackgroundInformationSentBytes(serializedSize(bgi));
			return bgi;
		}
	}

	public BgItemData getPropBgData(String sessionID,String propName) throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		synchronized (sD) {
			sD.timestamp = System.currentTimeMillis();
			BgItemData bgi = sD.dk.getPropBgData(propName, true);
			if (isCollectingStatistics())
				stats.reportBackgroundInformationSentBytes(serializedSize(bgi));
			return bgi;
		}
	}
	
	public float[][] getRelationshipValues(String sessionID,String relationshipName)
			throws RemoteException, SessionLogoutException {
		SessionData sD = sessionList.get(sessionID);
		if (sD==null)
			throw new SessionLogoutException();

		synchronized (sD) {
			sD.timestamp = System.currentTimeMillis();
			return sD.dk.getRelationshipValues(relationshipName);
		}
	}

	
	// Read in a file
	public ArrayList<Object> startStorytellerSession(int clientVersion,String userName, String storyworldFile)  
					throws LimitException, RemoteException, IncompatibleVersionException {

		checkClientCompatibility(clientVersion);
		
		// Using a null storyworldID is not possible as it would confuse
		// Janus when testing if the session has or not a storyworldID.
		if (storyworldFile==null)
			throw new NullPointerException("storyworldFile cannot be null.");

		ArrayList<Object> result = new ArrayList<Object>();
		try {

			SessionData sD = startSession(userName);
			sD.isAuthorSession=false;
			logger.fine(sD.sessionID + " loadDeiktoFromFile");
			sD.storyworldID = storyworldFile;

			// Control that no other thread is accessing the deiktoList.
			deiktoListSems.acquire(storyworldFile);
			try {
				// Load the storyworld from memory, or from disk
				DeiktoCountedRef dr = deiktoList.get(storyworldFile);
				if (dr == null) {
					// Load the new dictionary as deikto
					FileInputStream fis = new FileInputStream("res/data/" + storyworldFile);
					sD.swatDk = new Deikto(fis,new File("res/data/" + storyworldFile),false);
					fis.close();
					deiktoList.putIfAbsent(storyworldFile, new DeiktoCountedRef(sD.swatDk));
				} else {
					sD.swatDk = dr.d;
					dr.refcount++;
				}

			} finally {			
				deiktoListSems.release(storyworldFile);
			}
			
			sD.dk = sD.swatDk.cloneWorldShareLanguage();
			sD.dk.fixatePTraitValues();
			
			// Get the protagonist's name
			result.add(sD.sessionID);
			result.add(sD.dk.version);
			result.add(sD.dk.getRelationshipNames());
			result.add(sD.dk.getVisibleActorTraitNames());
			result.add(sD.dk.getVisibleStageTraitNames());
			result.add(sD.dk.getVisiblePropTraitNames());
			result.add(sD.dk.getActorNames());
			result.add(sD.dk.getStageNames());
			result.add(sD.dk.getPropNames());
		} catch (LimitException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			result = null;
			logger.info("Exception in Janus.startStorytellerSession(): " + e);
		}
		
		if (isCollectingStatistics()) {
			stats.reportPlayerLogin();
			stats.reportStoryworldCount(deiktoList.size());
		}
		
		return result;
	}

	/** Class for accessing Janus functionality that should be only available for testing. */
	public static class Test {
		/** 
		 * Used for stress test.
		 * @see JanusStressTest
		 *  */
		public static CountDownLatch stressTestLatch; 
		
		/** Used only for testing. */
		public static SessionData getSession(Janus janus,String sessionID) {
			return janus.sessionList.get(sessionID);
		}
	}
}
