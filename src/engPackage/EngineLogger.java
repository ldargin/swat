package engPackage;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.Triplet;
import com.storytron.swat.util.Compressed;
import com.storytron.uber.Actor;
import com.storytron.uber.Role;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Verb;
import com.storytron.uber.operator.Operator;

/**
 * An extension of the {@link TreeLogger} with custom messages.
 * A message type is used to identify the different messages
 * the engine may log.
 * <p>
 * Each method of this class inserts a message of a different type.
 * <p>
 * Some methods only inserts a new message, and some other methods
 * additionally set the current node as the newly created one. Methods
 * setting the current node have suffix Msg. Methods not setting the current
 * node have suffix MsgChild. 
 * <p>
 * By convention the message type is stored at the first position in the message.
 * <p> 
 * This class provide a mechanism for avoiding generation off all the data.
 * Descendants of nodes of types in the {@link #frontierTypes} set are cut,
 * to avoid the overhead of storing all the logged nodes.
 * <p>
 * Also logging can be turned off at all by calling {@link #setLogging(boolean)}
 * with false.
 * <p>
 * If it is needed to collect all the nodes in a branch, including descendants
 * of the nodes in the {@link #frontierTypes} set, it is possible to specify
 * the specific branches that must be fully collected. This is done through 
 * the {@link #collectables} queue, where requests should be inserted.
 * It is needed, then, that the {@link Engine} execution generating the requested
 * branches be executed again, so the logger can do its work.
 * <p>
 * The requested branches will be returned in the {@link #compressed} queue.
 * Each entry in this queue holds a a branch identifier and a bunch of 
 * consecutive nodes of the log tree. The branch identifier is the index of its
 * root, which will always be a child of the log tree root. 
 * The bunch of nodes starts at the beginning of the first branch requested
 * and ends somewhere in the branch identified in this entry. This identified 
 * branch is guaranteed to be between the first requested branch and the last.
 * <p>
 * The reason to have an entry in the {@link #compressed} queue that does not 
 * contain all the requested nodes is that they could be too many, and therefore
 * have to be collected in stages. Execution of the engine will be suspended
 * if this happens, and will be resumed after the caller gets rid of the first
 * chunk of data.
 * <p>
 * When collecting full branches, the nodes in every other branch will be 
 * discarded. The method {@link #setBranchCounter(int)} can be used to
 * indicate at which branch execution of the {@link Engine} will start.
 * */
public final class EngineLogger extends TreeLogger {
	public enum MsgType {
		EXECUTE,
		ROLE,
		OPTION,
		WORDSOCKETS,
		WORDSOCKET,
		DISQUALIFIED,
		CHOOSE_OPTION,
		FATE_REACTING,
		WITNESS,
		DIROBJECT,
		SUBJECT,
		SCRIPT,
		TOKEN,
		PARENTVALUE,
		SIBLINGVALUE,
		POISON,
		ABORT,
		SEARCHMARK
	};

	public static final Set<MsgType> frontierTypes = EnumSet.of(MsgType.SCRIPT, MsgType.SUBJECT, MsgType.DIROBJECT,MsgType.WITNESS,MsgType.FATE_REACTING);

	/** Number of compressed chunks to store before blocking. */
	public static final int COMPRESSED_LOGTREE_BUFFER_SIZE = 30;

	/** 
	 * Used to count nodes that would have been logged.
	 * It is used by {@link LogDataCollector} to determine when enough computations have
	 * passed to save an intermediate state. Whenever a state is saved this counter
	 * is reset to 0.
	 *  */
	public int commandCount=0;
	
	/** 
	 * Used to count nodes that would have been logged.
	 * This is used to have the thread yield() if too many computations have 
	 * been performed.
	 *  */
	private int nodeCount=0;
	private static final int YIELD_LIMIT = 300000;
	
	/** 
	 * Called when attempting to insert a node in the log tree.
	 * If the nodeCount exceeds YIELD_LIMIT it calls the thread yield method.
	 *  */
	private void checkForYield(){
		if (++nodeCount>YIELD_LIMIT) {
			Thread.yield();
			nodeCount=0;
		}
	}
	
	public NodeSearcher nodeSearcher = new NodeSearcher(); 
	
	public EngineLogger(){
		compressed = new ArrayBlockingQueue<Pair<Integer,Compressed<Object[]>[]>>(COMPRESSED_LOGTREE_BUFFER_SIZE);		
	}

	public EngineLogger(BlockingQueue<Pair<Integer,Compressed<Object[]>[]>> compressed){
		this.compressed = compressed;
	}

	/** Tells if script nodes should be skipped. */
	public boolean logOnlyUpperLevels = false;
	
	/** Tells if the logger should ignore all the node insertion calls. */
	private boolean ignoreAll = false;

	/**
	 * It stores the depth of the skipped node with respect
	 * to the last inserted ancestor.
	 * */
	private int depth = 0;
	/** Amount of children of the root of a subtree being skipped. */
	private int childCount = 0;

	/** Depth of the current node in the tree. */
	private int depthFromRoot = 0;
	/** Depth of the last marked node in the tree. */
	private int searchMarkDepth = 0;
	
	/** Tells if we are skipping nodes. */
	private boolean skipping = false;

	/** Used to count commands in a branch. */
	private int branchCommandCounter = 0;
	
	/** Sets internal indicators to collect the next requested branches. */
	public void setNextCollectRequest() throws InterruptedException {
		setCollectableBranches(collectables.take());
	}

	/** Tells which branches to collect, and resets the logger state. */
	public void setCollectableBranches(Triplet<Integer,Integer,Integer> collectables){
		collectableBranches = collectables;
		clear();
		commandCount=0;
		ignoreAll = branchCounter < collectableBranches.first;
	}	
	private Triplet<Integer,Integer,Integer> collectableBranches;
	
	void executeMsg(int cTicks,String sentence) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true))
			insertCurrent(new Object[]{MsgType.EXECUTE,cTicks,sentence});
	};

	void roleMsg(Actor reactingActor, Role.Link role) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true))
			insertCurrent(new Object[]{MsgType.ROLE,reactingActor.getLabel(),role.getLabel()});
	};

	void optionMsg(Role.Option o) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true))
			insertCurrent(new Object[]{MsgType.OPTION,o.getLabel()});
	};

	void wordsocketsMsg() throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true))
			insertCurrent(new Object[]{MsgType.WORDSOCKETS});
	};

	void wordsocketMsg(String label) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true))
			insertCurrent(new Object[]{MsgType.WORDSOCKET,label});
	};

	void disqualifiedMsgChild(Role.Option option, String wordSocketLabel) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(false))
			insertChild(new Object[]{MsgType.DISQUALIFIED,option.getLabel(),wordSocketLabel});
	};
	void chooseOptionMsgChild(float bestInclination, Verb verb) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(false))
			insertChild(new Object[]{MsgType.CHOOSE_OPTION,bestInclination,verb.getLabel()});
	};
	void fateReactingMsg() throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true,true))
			insertCurrent(new Object[]{MsgType.FATE_REACTING});
	};
	void witnessMsg(Actor actor) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true,true))
			insertCurrent(new Object[]{MsgType.WITNESS,actor.getLabel()});
	};
	void dirObjectMsg(Actor actor) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true,true))
			insertCurrent(new Object[]{MsgType.DIROBJECT,actor.getLabel()});
	};
	void subjectMsg(Actor actor) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true,true))
			insertCurrent(new Object[]{MsgType.SUBJECT,actor.getLabel()});
	};
	void scriptMsg(ScriptPath sp,Script s) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true,true))
			insertCurrent(new Object[]{MsgType.SCRIPT,sp.getPath(s)});
	};
	void scriptIsPoisonedMsgChild(boolean poison) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(false))
			insertChild(new Object[]{MsgType.SIBLINGVALUE,poison});
	}
	void tokenMsg(Operator op) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(true))
			insertCurrent(new Object[]{MsgType.TOKEN,op.getLabel()});
	};
	void valueMsgChild(String value) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(false))
			insertChild(new Object[]{MsgType.PARENTVALUE,value});
	};
	void reactsMsgChild() throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(false))
			insertChild(new Object[]{MsgType.SIBLINGVALUE});
	};
	void scriptResultMsgChild(Object res) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(false))
			insertChild(new Object[]{MsgType.SIBLINGVALUE,res});
	};
	void poisonMsgChild(String cause) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else {
			if (aboutToInsert(false)) {
				insertChild(new Object[]{MsgType.POISON,cause});
				if (nodeSearcher.trackPoisons())
					markNode(new Object[]{MsgType.SIBLINGVALUE,MsgType.SEARCHMARK});
			} else if (nodeSearcher.trackPoisons())
				markNode();
		}
	};
	void abortedMsgChild() throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(false))
			insertChild(new Object[]{MsgType.PARENTVALUE,MsgType.ABORT});
	};
	void pageMsgChild(int page) throws InterruptedException {
		checkForYield();
		if (!isLogging() || ignoreAll)
			return;
		else if (aboutToInsert(false))
			insertChild(new Object[]{MsgType.PARENTVALUE,page});
	};

	/** Called whenever a node is about to be inserted. */
	private boolean aboutToInsert(boolean insertCurrent){
		return aboutToInsert(false,insertCurrent);
	}
	/**
	 * Called whenever a node is about to be inserted.
	 * @param skipChildren indicates if the children of the node 
	 *        must not be inserted.
	 * @param insertCurrent indicates if the node will be inserted and made
	 *        current. This affects the depth counter.   
	 * @return true iff the node must be inserted. 
	 * */
	private boolean aboutToInsert(boolean skipChildren,boolean insertCurrent){
		if (insertCurrent)
			depthFromRoot++;
		commandCount++;
		if (collectableBranches==null)
			if (logOnlyUpperLevels) {
				final boolean allow = !skipping;
				if (skipping) {
					if (depth==0 && childCount>=0)
						childCount++;
					if (insertCurrent)
						depth++;
				} else if (skipChildren)
					skipping = true;
				return allow;
			} else 
				return true;
		else {
			branchCommandCounter++;
			if (branchCommandCounter>collectableBranches.third)
				return true;
			else {
				if (insertCurrent)
					branchCommandCounter++;
				return false;
			}
		}
	}
	
	/** Inserts a mark for the last child of the current node. */
	private void markNode(Object[] params) throws InterruptedException {
		markNode();
		insertChild(params);
	}
	/** Update the counters as if marking a node but does not insert any mark. */
	private void markNode() {
		commandCount++;
		if (collectableBranches!=null)
			branchCommandCounter++;
		searchMarkDepth=depthFromRoot;
	}

	@Override 
	public void down() throws InterruptedException {
		if (!isLogging() || ignoreAll || collectableBranches!=null && ++branchCommandCounter<=collectableBranches.third)
			return;
		commandCount++;
		super.down();
	}
	@Override 
	public void up() throws InterruptedException {
		if (!isLogging() || ignoreAll)
			return;

		depthFromRoot--;
		final boolean insertMark = depthFromRoot<searchMarkDepth;
		if (insertMark)
			searchMarkDepth--;

		if (collectableBranches!=null) {
			if (insertMark)
				branchCommandCounter++;
			if (++branchCommandCounter<=collectableBranches.third)
				return;
		}
		
		commandCount++;
		if (insertMark)
			commandCount++;
		if (depth>0)
			depth--;
		else {
			if (skipping) {
				// Insert a dummy node in frontier nodes that have children.
				// This serves LogLizard to distinguish between frontier nodes
				// that have children from those that don't
				if (childCount>0) {
					insertChild(new Object[]{null});
				}
				childCount=0;
				skipping=false;
			}
			super.up();
			if (insertMark)
				insertChild(new Object[]{MsgType.SIBLINGVALUE,MsgType.SEARCHMARK});
		}
	}
	
	/** A counter of branches. */
	private int branchCounter = 0;
	/** @return the last branch collected. */
	public int getBranchCounter() {
		return branchCounter;
	} 
	/** Resets the branch counter. */
	public void setBranchCounter(int newvalue) {
		branchCounter = newvalue;
	} 
	/** A flag to stop the engine. */
	public boolean stopEngine = false;
	
	/** 
	 * Stores current branch as finished and compresses it if there are
	 * enough finished branches to compress.
	 * */
	public synchronized void save() throws InterruptedException {
		if (!isLogging()) return;

		if (!ignoreAll) {
			depthFromRoot--;
			if (depthFromRoot<searchMarkDepth)
				searchMarkDepth--;
			
			if (collectableBranches==null || ++branchCommandCounter>collectableBranches.third)
				super.up();
		}
		branchCommandCounter = 0; 
		
		if (collectableBranches!=null) {
			if (branchCounter<collectableBranches.first){
				branchCounter++;
				ignoreAll = branchCounter < collectableBranches.first;
				return;
			}
			collectableBranches.third=-1;
		}
		branchCounter++;
		
		if (collectableBranches!=null) {
			if (branchCounter>collectableBranches.second){
				queueCommands();
				setNextCollectRequest();
			}
		}
	}
	
	/** 
	 * Saves the current branch but keeps it in place so the caller
	 * can continue to edit it.
	 * */
	public final void partialSave() throws InterruptedException {
		if (!isLogging() || ignoreAll)
			return;
		
		if (skipping && childCount>0) {
			insertChild(new Object[]{null});
			childCount=-1;
		}
		
		flushNodes();
	} 
	
	public final BlockingQueue<Triplet<Integer,Integer,Integer>> collectables = new LinkedBlockingQueue<Triplet<Integer,Integer,Integer>>();
	
	public final BlockingQueue<Pair<Integer,Compressed<Object[]>[]>> compressed;
	
	/** 
	 * Puts the current uncompressed branches into the compressed queue.
	 * Includes the current tree if the root has at least some children.
	 * */
	public void queueCommands() throws InterruptedException {
		queueCommands(false);
	}
	@SuppressWarnings("unchecked")
	private void queueCommands(boolean bufferFull) throws InterruptedException {
		// This method makes sense only when collecting data for log lizard,
		// otherwise the queue where it puts data will never be consumed. 
		if (collectableBranches==null)
			return;
		
		flushNodes();
		LinkedList<Compressed<Object[]>> l = popCompressedNodes();
		if (!l.isEmpty() || !bufferFull) {
			Compressed<Object[]>[] ns=new Compressed[l.size()];
			l.toArray(ns);
		
			compressed.put(new Pair<Integer,Compressed<Object[]>[]>(bufferFull?branchCounter:-1,ns));
		}
	}
	
	@Override
	public void onBufferCommandFull() throws InterruptedException {
		queueCommands(true);
	}

	/** A class for searching nodes in the log tree. */
	public static class NodeSearcher {
		boolean trackPoisons() { return true; };
	}
}
