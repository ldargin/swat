package engPackage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.SessionLogoutException;
import com.storytron.enginecommon.StorytellerRemote;
import com.storytron.enginecommon.Triplet;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.Compressed;
import com.storytron.swat.util.DiskArray;
import com.storytron.test.JanusStressTest;
import com.storytron.uber.Deikto;

import engPackage.Janus.SessionData;
import engPackage.Janus.StopEngineException;
import engPackage.Janus.SessionData.Command;

/**
 * Takes care of collecting log data for a given session.
 * <p> 
 * While a story is being played with logging turned on, successive states
 * of the engine are saved to disk (see 
 * ({@link #saveEngineState(engPackage.Janus.SessionData)},
 * {@link #saveStateBuffer(engPackage.Janus.SessionData)}). 
 * Disk space is limited to a few states per session by discarding 
 * states as new ones are available
 * ({@link #dropEngineStates(engPackage.Janus.SessionData)}).
 * <p>
 * An engine thread is created to collect data for requests. 
 * When a request arrives, the log data is collected by picking the
 * closest saved state and playing the story, feeding the engine
 * thread with recorded input ({@link PlayerInputRecorder}). Sometimes, 
 * loading a state can be avoided if the engine thread is close enough 
 * to the story point where data must be collected.
 * <p>
 * Requests are queued up to a certain limit after which no more requests
 * are allowed until some of them be served. When the limit is exceeded
 * calling {@link #requestLogData(SessionData, int, int, int)} does not
 * have any effect.
 * */
class LogDataCollector {
	
	/** 
	 * The state of the engine is saved for each session every few
	 * interactions. This parameter controls how often. 
	 * Save too often and performance will decrease because saving effort.
	 * Save too sparingly and performance will decrease when
	 * loading a story because a lot of interactions must be recomputed.
	 * 
	 * The effort done by the engine between savings is measured by
	 * counting the amount of logger commands that are executed.
	 * 
	 * Please, if you want to change this parameter use 
	 * {@link JanusStressTest} to test performance.
	 */
	private static final int LOGGER_COMANDS_BEFORE_SAVING_STATE = 300000;

	/** Maximum amount of states a session can store on disk. */
	private static final int ENGINE_STATES_PER_SESSION = 20;

	public BlockingQueue<Pair<Integer,Compressed<Object[]>[]>> loglizardResponses;
	Engine loglizardEngine;
	private LinkedList<Pair<Triplet<Integer,Integer,Integer>,Long>> loglizardCurrentRequests;
	private TreeSet<Pair<Triplet<Integer,Integer,Integer>,Long>> loglizardPendingRequests;
	private Thread loglizardThread;
	private SessionData.Command loglizardCommand = Command.NONE;
	DiskArray<byte[]> engineStatesArray;
	ArrayList<Integer> engineStateBranches;   
	ArrayList<Integer> engineStateInputOffsets;

	@SuppressWarnings("deprecation")
	public void reset() throws InterruptedException {
		loglizardResponses=null;
		loglizardEngine=null;
		loglizardCurrentRequests=null;
		loglizardPendingRequests=null;
		loglizardCommand = Command.NONE;
		if (loglizardThread!=null) {
			loglizardThread.stop();
			loglizardThread.join(30000);
			if (loglizardThread.isAlive())
				loglizardThread.stop();

			loglizardThread = null;
		}
		
		if (engineStatesArray!=null) {
			engineStatesArray.dispose();
			engineStateBranches.clear();
			engineStateInputOffsets.clear();
			engineStatesArray=null;
			engineStateBranches=null;
			engineStateInputOffsets=null;
		}
	}
	
	/** Initializes the state needed for log lizard. */
	void initLogLizardState() throws IOException {
		engineStatesArray = new DiskArray<byte[]>(new DiskArray.ReadWriter<byte[]>(){
			public byte[] read(RandomAccessFile raf, int bytes)	throws IOException {
				byte[] bs = new byte[bytes];
				raf.readFully(bs);
				return bs;
			}
			public int write(byte[] e, RandomAccessFile raf, int available)	throws IOException {
				raf.write(e);
				return e.length;
			}
		});
		engineStateBranches = new ArrayList<Integer>();   
		engineStateInputOffsets = new ArrayList<Integer>();
	} 

	/** 
	 * <p>Queues request of log branches starting from fromTree to toTree.
	 * Less subtrees are returned if they are not available.</p> 
	 * */
	public boolean requestLogData(SessionData sD,int fromTree, int toTree, int skipCommands) {

		if (loglizardThread==null){
			loglizardCurrentRequests = new LinkedList<Pair<Triplet<Integer,Integer,Integer>,Long>>();
			loglizardPendingRequests = new TreeSet<Pair<Triplet<Integer,Integer,Integer>,Long>>();
			loglizardResponses = new ArrayBlockingQueue<Pair<Integer,Compressed<Object[]>[]>>(EngineLogger.COMPRESSED_LOGTREE_BUFFER_SIZE);
		}

		if (loglizardPendingRequests.size()<30 && fromTree>=0 && fromTree<=toTree) {
			loglizardPendingRequests.add(new Pair<Triplet<Integer,Integer,Integer>,Long>(
					new Triplet<Integer,Integer,Integer>(fromTree,toTree,skipCommands)
					,System.currentTimeMillis()));
				
			if (loglizardThread==null)
				restartLogLizardThread(sD);
			else
				processLogLizardRequests();
				
			return true;
		} else 
			return false;
	}
	
	/** 
	 * Gets the data requested with {@link #requestLogData(String, int, int)}.
	 * <p>
	 * The external Pair contains the log data in the first component, and the timestamp
	 * of the moment at which the request arrived.
	 * <p>
	 * The log data is contained in a Triplet which is described in 
	 * {@link StorytellerRemote#getRequestedLogData(String)}. 
	 * */
	@SuppressWarnings("unchecked")
	public Pair<Triplet<Integer,Integer,Compressed<Object[]>[]>,Long> getRequestedLogData() 
					throws RemoteException, SessionLogoutException {
		if (loglizardEngine==null)
			return null;
		
		ArrayList<Pair<Integer,Compressed<Object[]>[]>> results = new ArrayList<Pair<Integer,Compressed<Object[]>[]>>(loglizardEngine.logger.compressed.size());
		if (loglizardEngine.logger.compressed.drainTo(results)==0)
			return null;
		else { // collect all data relative to the same request
			// calculate the total length
			int totalLength=0;
			for(Pair<Integer,Compressed<Object[]>[]> c:results) {
				totalLength+=c.second.length;
				if (c.first<0)
					break;
			}
			// fill an array with all the data
			final Compressed<Object[]>[] data = new Compressed[totalLength];
			Pair<Integer,Compressed<Object[]>[]> last = null;
			totalLength=0;
			for(Pair<Integer,Compressed<Object[]>[]> pc:results) {
				for(int i=0;i<pc.second.length;i++)
					data[totalLength+i]=pc.second[i];
				totalLength+=pc.second.length;
				if (pc.first<0) {
					last = pc;
					break;
				}
			}
			if (last==null)
				last = results.get(results.size()-1);
			
			// build the output
			Pair<Triplet<Integer,Integer,Compressed<Object[]>[]>,Long> p =
				new Pair<Triplet<Integer,Integer,Compressed<Object[]>[]>,Long>(
						new Triplet<Integer,Integer,Compressed<Object[]>[]>(loglizardCurrentRequests.getFirst().first.first,last.first,data)
						,loglizardCurrentRequests.getFirst().second);
			if (last.first<0) {
				loglizardCurrentRequests.removeFirst();
				processLogLizardRequests();
			} else // update the initial branch
				loglizardCurrentRequests.getFirst().first.first=last.first;
			return p;
		}
	}

	/** Commands the collector to collect the next data request. */
	public void processNextRequest() throws InterruptedException {
		loglizardEngine.logger.queueCommands();
		loglizardEngine.logger.setNextCollectRequest();
	}
	
	/** Moves requests from the pending queue to the current queue. */
	private void processLogLizardRequests(){
		int size = loglizardCurrentRequests.size();
		int last;
		if (size>0)
			last = loglizardCurrentRequests.getLast().first.second;
		else
			last = loglizardEngine.logger.getBranchCounter()-1;

		// Search for insertable elements
		Iterator<Pair<Triplet<Integer,Integer,Integer>,Long>> it = loglizardPendingRequests.iterator();
		Pair<Triplet<Integer,Integer,Integer>,Long> p = null;
		while(it.hasNext()){
			p=it.next();
			if (p.first.first>last)
				break;
		}
		// insert new elements
		if (p!=null && size<10 && p.first.first>last) {
			// compare engine states to use
			if (p.first.first<last+10 ||
					Utils.binarySearch(engineStateBranches, p.first.first)==
						Utils.binarySearch(engineStateBranches, last+1)) {
				loglizardCurrentRequests.add(p);
				loglizardEngine.logger.collectables.add(p.first);
				it.remove();

				while (it.hasNext() && size<10) {
					int mLast = p.first.second;
					p = it.next();
					if (p.first.first<mLast+10 ||
							Utils.binarySearch(engineStateBranches, p.first.first)==
								Utils.binarySearch(engineStateBranches, mLast)){
						
						loglizardCurrentRequests.add(p);
						loglizardEngine.logger.collectables.add(p.first);
						it.remove();
						size++;
					} else
						break;
				}
			}
		}
		
		// if there are no requests being processed and there are pending requests
		// restart log lizard thread
		if (size==0 && !loglizardPendingRequests.isEmpty()){
			loglizardEngine.setStoryIsOver(true);
			loglizardCommand = SessionData.Command.RESTART;
			loglizardThread.interrupt();
		}	
	}
	
	private class LoglizardThread extends Thread {
		SessionData sD;
		public LoglizardThread(SessionData sD){
			this.sD = sD;
			setPriority(Thread.MIN_PRIORITY);
		}
		
		@Override
		public void run() {
			try {
				try{
					loglizardEngine.run();
				} catch (StopEngineException e) {					
				} catch (InterruptedException e){
				}
				synchronized(sD){
					if (loglizardCommand == SessionData.Command.RESTART)
						restartLogLizardThread(sD);
					loglizardCommand = SessionData.Command.NONE;
				}
			} catch (IllegalMonitorStateException e){
				// Thrown when stopping log lizard thread.
				if (loglizardEngine!=null)
					// but print if the session state does not look
					// like we are stopping the thread purposefully.
					e.printStackTrace();	
			} catch (RuntimeException e){
				if (e.getCause()!=null && e.getCause() instanceof IllegalMonitorStateException) {
					// Thrown when stopping log lizard thread.
					if (loglizardEngine!=null)
						// but print if the session state does not look
						// like we are stopping the thread purposefully.
						e.printStackTrace();
				} else
					e.printStackTrace();
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
	
	private void restartLogLizardThread(SessionData sD){
		int stateIndex;
		if (loglizardCurrentRequests.isEmpty())
			stateIndex = Utils.binarySearch(engineStateBranches, loglizardPendingRequests.iterator().next().first.first);
		else
			stateIndex = Utils.binarySearch(engineStateBranches, loglizardCurrentRequests.getFirst().first.first);

		Deikto newDk=null;
		if (loglizardEngine==null || stateIndex<0) {
			newDk = sD.swatDk.cloneWorldShareLanguage();
			newDk.fixatePTraitValues();
		} else
			newDk = loglizardEngine.dk;
		
		if (stateIndex<0)
			sD.inputRecorder.setInputsOffset(0);
		else
			sD.inputRecorder.setInputsOffset(engineStateInputOffsets.get(stateIndex));

		loglizardEngine = new Engine(newDk,sD.inputRecorder,new EngineLogger(loglizardResponses));
		loglizardEngine.init();
		
		try {
			if (stateIndex>=0) {
				ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(engineStatesArray.get(stateIndex)));
				loglizardEngine.loadState(ois);
				loglizardEngine.logger.setBranchCounter(engineStateBranches.get(stateIndex));
				ois.close();
			}
		
			processLogLizardRequests();
		
			loglizardEngine.logger.collectables.poll();
			loglizardEngine.logger.setCollectableBranches(loglizardCurrentRequests.getFirst().first);
			loglizardThread = new LoglizardThread(sD);
			loglizardThread.start();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/** Saves the state of the engine. */
	public void saveEngineState(SessionData sD){
		if (sD.engine.logger.commandCount<LOGGER_COMANDS_BEFORE_SAVING_STATE)
			return;

		sD.engine.logger.commandCount=0;
		
		sD.lastInputs.clear();
		sD.savedStateBuffer.reset();
		try {
			ObjectOutputStream savedState = new ObjectOutputStream(sD.savedStateBuffer);
			sD.engine.saveState(savedState);
			savedState.close();
			
			if (engineStatesArray!=null) {
				if (ENGINE_STATES_PER_SESSION<=engineStatesArray.size())
					dropEngineStates();
				saveStateBuffer(sD);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** Saves the state in {@link SessionData#savedStateBuffer}. */
	public void saveStateBuffer(SessionData sD){
		engineStatesArray.add(sD.savedStateBuffer.toByteArray());
		engineStateBranches.add(sD.engine.logger.getBranchCounter());
		engineStateInputOffsets.add(sD.engine.inputCounter);
	}
	
	/** 
	 * Drops the engine states in the even positions. Used when the
	 * maximum amount of disk space has been reached
	 * */
	private void dropEngineStates() throws IOException {
		DiskArray<byte[]> engineStatesArray = this.engineStatesArray;
		ArrayList<Integer> engineStateBranches = this.engineStateBranches;
		ArrayList<Integer> engineStateInputOffsets = this.engineStateInputOffsets;
		for(int i=1;i<engineStatesArray.size();i+=2) {
			initLogLizardState();
			this.engineStatesArray.add(engineStatesArray.get(i));
			this.engineStateBranches.add(engineStateBranches.get(i));
			this.engineStateInputOffsets.add(engineStateInputOffsets.get(i));
		}
		engineStatesArray.dispose();
	}

}
