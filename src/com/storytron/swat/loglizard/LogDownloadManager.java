package com.storytron.swat.loglizard;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.tree.TreePath;

import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.SessionLogoutException;
import com.storytron.enginecommon.Triplet;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.Swat.LogDownloaderThread;
import com.storytron.swat.util.Compressed;

import engPackage.EngineLogger;

/** A class for managing downloading of log data.
 * <p> 
 * Log data is generated from the server as the story advances 
 * and also as the author expands the log tree.</p>
 * <p>
 * The surface log data generated while playing a story is reported 
 * to this class through the {@link #reportNodes(Compressed[])} method.
 * <p>
 * When full branches of the log tree are requested the method 
 * {@link #request(int, TreePath, boolean)} is called. This method submits
 * the request only if the branch has not been previously requested.
 * This is to avoid unnecessary traffic and improve reaction times.
 * <p>
 * The method {@link #processRequest(Triplet)} is called when full branches
 * arrive from the engine. These branches are in turn reported to the 
 * tree model and then they are expanded.
 * <p>
 * This class is accessed both by the logDownloader thread {@link com.storytron.swat.Swat.LogDownloaderThread})
 * and by the event dispatcher thread. The logDownloader is the thread in which 
 * downloaded data is reported. The event dispatcher thread is the thread where
 * request for full log tree branches are submitted.
 * 
 * <h4>Interval sets</h4>
 * The implementation of this class relies on a small set of routines for manipulating
 * sets of disjoint integer intervals. These sets are used to describe
 * the branches that have been already requested and the branches that have
 * been already received.
 * <p>
 * An integer interval is represented as a Pair<Integer,Integer>,
 * and a set of intervals is represented as set of pairs (Set<Pair<Integer,Integer>>).
 * The functions for manipulating these sets are:
 * <ul>
 * <li> {@link #findInterval(Set, int)} </li>
 * <li> {@link #removeInterval(Set, int, int)}</li>
 * <li> {@link #removeInterval(Set, int, int, int, int)}</li>
 * <li> {@link #isFull(Set)}</li>
 * <li> {@link #addInterval(Set, int, int)} </li>
 * </ul> 
 * */
public class LogDownloadManager implements ActionListener {

	private Set<Pair<Integer,Integer>> nonFullBranches = new TreeSet<Pair<Integer,Integer>>(); 
	private Set<Pair<Integer,Integer>> nonRequestedBranches = new TreeSet<Pair<Integer,Integer>>();
	private LinkedList<Pair<TreePath,Boolean>> treesToExpand = new LinkedList<Pair<TreePath,Boolean>>();
	private AtomicInteger acceptedRequests = new AtomicInteger();
	private LogTreeModel treeModel;
	private LogDownloaderThread logDownloader;
	public JTree tree;
	
	/** 
	 * Creates a log manager. The log downloader thread will be used to get
	 * data from the server. 
	 * */
	public LogDownloadManager(LogDownloaderThread lt) throws IOException {
		treeModel = new LogTreeModel();
		logDownloader = lt;
		reset();
	}

	/** Gets the tree model used to provide the log data. */
	public LogTreeModel getTreeModel(){
		return treeModel;
	}
	
	/** Resets the manager state for a new server session. */
	public synchronized void reset(){
		nonFullBranches.clear();
		nonRequestedBranches.clear();
		treesToExpand.clear();
		Pair<Integer,Integer> p = new Pair<Integer,Integer>(0,Integer.MAX_VALUE);
		nonFullBranches.add(p);
		nonRequestedBranches.add(p);
		
		acceptedRequests.set(0);
		treeModel.clear();
	}

	/** 
	 * Used by the log processor thread to report downloaded nodes.
	 * @param partial tells if the last tree is partial or not. 
	 * */
	public void reportNodes(Compressed<Object[]>[] cns) throws SessionLogoutException {
		TreePath tp=null;
		int lastTree = treeModel.getChildCount(treeModel.getRoot())-1;
		TreeOutputStream stream = treeModel.streams.get(lastTree);
		if (stream!=null)
			tp = tree.getPathForRow(tree.getRowCount()-1);
		if (cns!=null && cns.length>0)
			treeModel.reportNodes(cns);
		
		// request the partial index if the data is already present.
		if (stream!=null) {
			synchronized (this) {
				addInterval(nonFullBranches, lastTree, lastTree);
				switch(request(lastTree,stream.getLastBranchCommandCount())) {
				case 1:
					// data has been requested. Collapse the tree till it be updated.
					// Search the frontier node where to collapse.
					int i=0;
					while(i<tp.getPathCount() && !isFrontierNode(tp.getPathComponent(i)))
						i++;
					if (i<tp.getPathCount()) {
						while(i+1<tp.getPathCount())
							tp = tp.getParentPath();
						final TreePath ftp = tp;
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {
								tree.collapsePath(ftp);
							}
						});
					}
					break;
				default:;
				}
			}
		}
	};
	
	/** A timer for requesting data from the engine. */
	private Timer t = new Timer(2000,this);
	public synchronized void actionPerformed(ActionEvent e) {
		if (isFull(nonRequestedBranches)) {
			t.stop();
			assert acceptedRequests.get()==0 : String.valueOf(acceptedRequests.get());
		} else
			logDownloader.getRequestedLogData();
	}

	/** 
	 * Request the log tree treeIndex, if it is not already requested.
	 * expandAll tells if all the descendants must be expanded once they are 
	 * available. 
	 * <p>
	 * The method is synchronized because it is called from the event dispatcher
	 * thread.
	 * @return true iff the data is already available. 
	 * */
	public synchronized boolean request(int treeIndex,TreePath tp,boolean expandAll) 
				throws SessionLogoutException {
		
		switch (request(treeIndex,0)) {
		case 1:
			// register the branch to be expanded if it is not registered
			// and if it is a cut frontier node
			if (tp.getLastPathComponent() instanceof LogTreeModel.Node &&
				isFrontierNode(tp) && !logDownloader.sessionExpired){
					boolean registered = false; 
					for(Pair<TreePath,Boolean> t:treesToExpand)
						if (t.first.getLastPathComponent().equals(tp.getLastPathComponent())) {
							registered = true;
							t.second |= expandAll;
							break;
						}
					if (!registered) {
						tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						treesToExpand.add(new Pair<TreePath,Boolean>(tp,expandAll));
					}
			}
			return false;
		case 0:
			return true;
		default:
			return false;
		}
	}
	
	/** Tells if a node is in the cut frontier. */
	public static boolean isFrontierNode(TreePath tp){
		return isFrontierNode(tp.getLastPathComponent());
	}

	/** Tells if a node is in the cut frontier. */
	public static boolean isFrontierNode(Object node){
		if (node instanceof LogTreeModel.Node)  {
			LogTreeModel.Node n=(LogTreeModel.Node)node;
			return n.params!=null && EngineLogger.frontierTypes.contains(n.params[0]);
		} else 
			return false;
	}

	/** 
	 * Requests from the server branch treeIndex.
	 * @param treeIndex is the index of the tree to get.
	 * @param skipCommands is the amount of commands to skip when filling the request.
	 *                     Used for branches that are partially present.
	 * @return 0 iff the data is already available.
	 *         1 iff the data is not available but has been requested.
	 *         2 iff the data is not available and has not been requested.
	 * */
	private int request(int treeIndex,final int skipCommands) throws SessionLogoutException {
		
		// exit if it is in a present full branch.
		Pair<Integer,Integer> pNonFull=findInterval(nonFullBranches,treeIndex);
		if (pNonFull==null)
			return 0;
		
		assert skipCommands==0 || pNonFull.first==treeIndex : pNonFull.first;

		// request the branch if it is not requested.
		Pair<Integer,Integer> p=findInterval(nonRequestedBranches,treeIndex);
		if (p==null)
			return 1;

		if (acceptedRequests.incrementAndGet()>30) {
			acceptedRequests.decrementAndGet();
			return 2; 
		}

		final int begin = Math.max(pNonFull.first,Math.max(p.first, treeIndex-4));
		final int end = treeIndex;
		//final int end = Math.min(pNonFull.second,Math.min(p.second, Math.min(begin+29, treeModel.getChildCount(treeModel.getRoot())-1)));

		nonRequestedBranches.remove(p);
		removeInterval(nonRequestedBranches,p.first,p.second,begin,end);

		new Thread() {
			@Override
			public void run() {
				try{
					if (!logDownloader.sessionExpired)
						logDownloader.getJanus().requestLogData(logDownloader.getSessionID(), begin, end, skipCommands);
				} catch(final RemoteException e) {
					e.printStackTrace();
					synchronized (LogDownloadManager.this) {
						addInterval(nonRequestedBranches,begin,end);
						acceptedRequests.decrementAndGet();
					}
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							Utils.showErrorDialog(tree, "LogLizard has failed to get the requested data.","Connection error",e);
						}
					});
				} catch (SessionLogoutException e) {
					logDownloader.sessionExpired = true;
					synchronized (LogDownloadManager.this) {
						addInterval(nonRequestedBranches,begin,end);
						acceptedRequests.decrementAndGet();
					}
					SwingUtilities.invokeLater(new Runnable(){
						public void run() {
							Utils.showErrorDialog(tree, "Your session on the server has expired.","Connection error");
						}
					});
				}
			}
		}.start();

		t.setInitialDelay(200);
		t.start();

		return 1;
	}

	/** Adds full branches to the log tree model. */
	public void processRequest(final Triplet<Integer,Integer,Compressed<Object[]>[]> p){
		if (p.second==-1)
			acceptedRequests.decrementAndGet();
		
		int treeCount=treeModel.addFullBranch(p.first, p.third);
		if (treeModel.streams.get(p.second)!=null && p.second>=0) 
			// ignore the last tree if it is incomplete and
			// will be completed in another request.
			treeCount--;
		final int count = treeCount;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				synchronized (LogDownloadManager.this) {
					removeInterval(nonFullBranches,p.first,p.first+count-1);
					addInterval(nonRequestedBranches,p.first,p.first+count-1);
				}
				Iterator<Pair<TreePath,Boolean>> i = treesToExpand.iterator();
				while(i.hasNext()){
					Pair<TreePath,Boolean> t=i.next();
					if (t.first.getLastPathComponent() instanceof LogTreeModel.Node
							&& treeModel.rootChildrenContains(p.first,p.first+count-1,((LogTreeModel.Node)t.first.getPathComponent(1)).id)){
						i.remove();
						if (t.second)
							LogLizard.expandNonFrontierNodes(tree,t.first);
						else
							tree.expandPath(t.first);
					}
				}
				if (treesToExpand.isEmpty())
					tree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});
	}
	
	/******************************************************************
	 * Operations on interval sets.
	 ******************************************************************/

	/**
	 * Finds the interval containing point i. Returns null if such interval 
	 * does not exists. 
	 * */
	private static Pair<Integer,Integer> findInterval(Set<Pair<Integer,Integer>> is,int i){
		for(Pair<Integer,Integer> p:is)
			if (p.first<=i){
				if (i<=p.second)
					return p;
			} else
				break;
		return null;
	}

	private static boolean isFull(Set<Pair<Integer,Integer>> is){
		Iterator<Pair<Integer,Integer>> it=is.iterator();
		if (it.hasNext()) {
			Pair<Integer,Integer> p=it.next();
			return p.first==0 && p.second == Integer.MAX_VALUE;
		} else
			return false;
	}
	
	/** Removes the interval [begin..end]. */
	private static void removeInterval(Set<Pair<Integer,Integer>> is,int begin,int end){
		LinkedList<Pair<Integer,Integer>> l = new LinkedList<Pair<Integer,Integer>>(); 
		for(Pair<Integer,Integer> p:is)
			if (p.first<=end) {
				if (begin<=p.second)
					l.add(p);
			} else
				break;
		is.removeAll(l);
		if (!l.isEmpty())
			removeInterval(is,l.getFirst().first,l.getLast().second,begin,end);
	}
	/** 
	 * Breaks interval [pfirst..psecond] by removing an overlapped
	 * interval [begin..end].
	 * */
	private static void removeInterval(Set<Pair<Integer,Integer>> is,int pfirst,int psecond,int begin,int end){
		if (pfirst<begin)
			is.add(new Pair<Integer,Integer>(pfirst,begin-1));
		if (end<psecond)
			is.add(new Pair<Integer,Integer>(end+1,psecond));
	}

	/** Adds the interval [begin..end]. */
	private static void addInterval(Set<Pair<Integer,Integer>> is,int begin,int end){
		LinkedList<Pair<Integer,Integer>> l = new LinkedList<Pair<Integer,Integer>>(); 
		for(Pair<Integer,Integer> p:is)
			if (p.first<=end+1) {
				if (begin-1<=p.second)
					l.add(p);
			} else
				break;
		is.removeAll(l);
		if (!l.isEmpty())
			is.add(new Pair<Integer,Integer>(Math.min(begin,l.getFirst().first),Math.max(end,l.getLast().second)));
	}

	/** Some tests for the interval operations. */
	public static void main(String[] argv){
		TreeSet<Pair<Integer,Integer>> is = new TreeSet<Pair<Integer,Integer>>();
		is.add(new Pair<Integer,Integer>(0,Integer.MAX_VALUE));
		
		removeInterval(is,10,20);
		if (is.size()!=2)
			System.out.println("Error1");
		if (is.first().first!=0 || is.first().second!=9)
			System.out.println("Error2");
		if (is.last().first!=21 || is.last().second!=Integer.MAX_VALUE)
			System.out.println("Error3");

		addInterval(is,10,20);
		if (is.size()!=1)
			System.out.println("Error4");

		System.out.println("OK");
	}

}
