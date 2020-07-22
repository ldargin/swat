package com.storytron.swat.loglizard;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.Compressed;

import engPackage.EngineLogger;
import engPackage.EngineLogger.MsgType;

/** 
 * A tree model that manages a big log tree. It uses disk storage
 * to decrease memory requirements.
 * <p>
 * Currently there are two threads that use the methods in this class.
 * <ul>
 * <li>The event dispatcher thread uses the TreeModel interface methods 
 * and the constructor.
 * </li>
 * <li>All the other methods are used by the log processor thread.
 * </li>
 * </ul>
 * */
public final class LogTreeModel implements TreeModel, Serializable {
	private static final long serialVersionUID = 1L;

	/** Nodes on disk. */
    private DiskIndexedTree disk = new DiskIndexedTree();
	/** View of the nodes on disk. */
    private LogTreeAdapter treeView = new LogTreeAdapter(disk);
    /** 
     * Children in memory. The cache is necessary because the nodes
     * are requested many times by the UI, and constructing them is an expensive 
     * operation since it may require disk access.
     * */
    private ConcurrentHashMap<Integer,Node> nodeCache = new ConcurrentHashMap<Integer,Node>();
    /** Root children. */
    private ArrayList<Integer> rootChildren = new ArrayList<Integer>();

    /** Stream to insert surface nodes. */
    public TreeOutputStream surfaceStream = new TreeOutputStream(disk);
    /** 
     * Streams of branches being written.
     * The key is the index of a root child whose descendants are
     * being written. The stored value is a stream used to write there.
     * <p>
     * The streams in this map satisfy the invariant that their cursors
     * are inside of some branch (i.e. not in between of two consecutive 
     * branches). 
     * */
    public final Map<Integer,TreeOutputStream> streams = new TreeMap<Integer,TreeOutputStream>();

    /** 
     * As this constructor attempts to create a temporal file on disk
     * it can throw an {@link IOException}. */
    public LogTreeModel() throws IOException {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){	disk.dispose();	}
		});
    }

	/** Deletes all the log entries. */
	public void clear(){
		// the event dispatcher thread may be reading 
		// from the rootChildren array 
		synchronized(rootChildren){
			rootChildren.clear();
		}
		// the event dispatcher thread may be reading 
		// from the tree on disk. 
		synchronized(disk){
			disk.clear();
		}
		surfaceStream.reset();
		streams.clear();
		nodeCache.clear();
		reload(new Object[]{root});
	}

	/** 
	 * Gets node from the cache or from the tree on disk if it is
	 * not in the cache. 
	 * */
	private Node getNode(int node) {
		Node n=nodeCache.get(node);
		if (n==null) {
			// This may be the event dispatcher thread, and the
			// log processor thread can be writing the tree.
			synchronized (disk) {
				n = treeView.getNode(node);
			}
			nodeCache.put(node,n);
		}
		return n;
	}
	
    /** Removes given nodes and all of its descendants from the cache. */
    private void removeCachedNodes(int node){
    	Node n = nodeCache.remove(node);
    	if (n!=null && n.children!=null)
			for(int c:n.children)
				removeCachedNodes(c);
    }
    

    /** Adds the given nodes as root children. */
	public void reportNodes(Compressed<Object[]>[] cns){
		int initialIndex = rootChildren.size();
		boolean lastPartial = !surfaceStream.isAtBranchDivision();
		if (lastPartial)
			initialIndex--;
		// The event dispatcher thread can be reading the tree.
		synchronized (disk) {
			for(Compressed<Object[]> c:cns)
				surfaceStream.write(c.getObject());
		}
		
		// the event dispatcher thread may be reading 
		// from the rootChildren array 
		synchronized(rootChildren){
			int sibling = rootChildren.isEmpty()?treeView.getFirstChild(0):treeView.getNextSibling(rootChildren.get(rootChildren.size()-1));
			while(sibling>=0) {
				rootChildren.add(sibling);
				sibling = treeView.getNextSibling(sibling);
			}
		}
		if (initialIndex<rootChildren.size())
			removeCachedNodes(rootChildren.get(initialIndex));
		if (lastPartial) {
			reload(initialIndex);
			if (initialIndex+1<rootChildren.size())
				nodesInserted(initialIndex+1);
		} else if (initialIndex==0)
			// I don't know why the jtree does not understand 
			// node insertions when inserting the first trees.
			reload(); 
		else
			nodesInserted(initialIndex);
	}

    /** 
     * Add an array of consecutive full branches starting at index initial.
     * @return the amount of inserted branches. 
     * */
    public int addFullBranch(int initial,Compressed<Object[]>[] cns){
    	if (cns.length==0)
    		return 1;
    	TreeOutputStream stream = streams.remove(initial);
    	boolean updateFirst=false;
    	if (stream==null){
    		if (initial>0) // make last point at the previous sibling
    			stream = new TreeOutputStream(disk,rootChildren.get(initial-1));
    		else
    			stream = new TreeOutputStream(disk);
    	} else {
    		updateFirst=true;
    	}
    	boolean initiallyAtDivision = stream.isAtBranchDivision();
		int initialSubtreeCount = stream.getSubtreeCount();
    	
    	// execute tree commands on our tree
   		for(int i=0;i<cns.length;i++)
   			// The event dispatcher thread can be reading the tree.
   			synchronized (disk) {
   				stream.write(cns[i].getObject());
   			}
   		
   		// subtreeCount is the amount of branches that have been modified
   		// during this call of addFullBranch. 
   		int subtreeCount = stream.getSubtreeCount()-initialSubtreeCount
   							+(initiallyAtDivision?0:1);

   		boolean updateLast=false;
    	if (!stream.isAtBranchDivision()) {
    		streams.put(initial+subtreeCount-1,stream);
    		updateLast=true;
    	}
    	
    	for(int i=0;i<subtreeCount;i++)
        	removeCachedNodes(rootChildren.get(initial+i));
    	
    	if (updateFirst)
    		reload(initial);
    	if (updateLast && (!updateFirst || subtreeCount>1))
    		reload(initial+subtreeCount-1);

    	return subtreeCount;
    }


	
	/** 
	 * @return true iff any of the root children in the interval [begin,end]
	 *         have the given id. 
	 * */
	public boolean rootChildrenContains(int begin,int end,int id){
		return rootChildren.subList(begin, end+1).contains(id);
	}

    private transient EventListenerList treeModelListeners = new EventListenerList();
    private Object root = new Serializable() {
    	private static final long serialVersionUID = 1L;
    	@Override
    	public String toString(){
    		return "Once upon a time ...";
    	}
    };

    /** 
     * Notify that a root child has changed.  
     * @param i is the index of the root child. 
     * */
    private void reload(int i) {
    	reload(new Object[] {root,getNode(rootChildren.get(i))});
    }

    /** Notify that the whole tree has changed. */
    private void reload() {
    	reload(new Object[] {root});
    }

    /** Notify that the a given node has changed. */
    private void reload(final Object[] path) {
    	SwingUtilities.invokeLater(new Runnable(){
    		public void run() {
    	        TreeModelEvent e = new TreeModelEvent(this,path);
    	        
    	        for (TreeModelListener tml : treeModelListeners.getListeners(TreeModelListener.class))
    	            tml.treeStructureChanged(e);
    		}
    	});
    }

    /** Notify that root children have been inserted from index "from" to the end. */
    public void nodesInserted(final int from) {
    	if (rootChildren.size()<=from)
    		return;

		final int[] is = new int[rootChildren.size()-from];
		final Object[] os = new Object[is.length];
		for(int i=0;i<is.length;i++) {
			is[i]=i+from;
			os[i]=getNode(rootChildren.get(i+from));
		}

    	SwingUtilities.invokeLater(new Runnable(){
    		public void run() {
    			TreeModelEvent e = new TreeModelEvent(this,new Object[] {root},is,os);
        
    			for (TreeModelListener tml : treeModelListeners.getListeners(TreeModelListener.class))
    				tml.treeNodesInserted(e);
    		}
    	});
    }
    
    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     */
    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.add(TreeModelListener.class,l);
    }

    /** Returns the child of parent at index index in the parent's child array.   */
    public Object getChild(Object parent, int index) {
    	if (parent instanceof Node)
    		return getNode(((Node)parent).children[index]);
    	else 
    		// the log processor thread may be writing 
    		// the rootChildren array 
    		synchronized (rootChildren) {
    			return getNode(rootChildren.get(index));
    		}
    }

    /** Returns the number of children of parent. */
    public int getChildCount(Object parent) {
    	if (parent instanceof Node)
    		return ((Node)parent).childCount;
    	else
    		// the log processor thread may be writing 
    		// the rootChildren array 
    		synchronized (rootChildren) {
    			return rootChildren.size();
    		}
    }

    /** Returns the index of child in parent. */
    public int getIndexOfChild(Object parent, Object child) {
    	if (!(child instanceof Node))
    		return -1;
    	
		Node nc = (Node)child;
    	if (parent instanceof Node)
    		return Utils.indexOf(((Node)parent).children,nc.id);
    	else
    		// the log processor thread may be writing 
    		// the rootChildren array 
    		synchronized (rootChildren) {
    			return rootChildren.indexOf(nc.id);
    		}
    }

    /** Returns the root of the tree. */
    public Object getRoot() {
        return root;
    }

    /**
     * Returns true if node is a leaf.
     */
    public boolean isLeaf(Object node) {
        if (node instanceof Node)
        	return ((Node)node).childCount==0;
        else
    		// the log processor thread may be writing 
    		// the rootChildren array 
    		synchronized (rootChildren) {
    			return rootChildren.isEmpty();
    		}
    }

    /**
     * Removes a listener previously added with addTreeModelListener().
     */
    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.remove(TreeModelListener.class, l);
    }

    /**
     * Messaged when the user has altered the value for the item
     * identified by path to newValue.  Not used by this model.
     */
    public void valueForPathChanged(TreePath path, Object newValue) {
        System.out.println("*** valueForPathChanged : "
                           + path + " --> " + newValue);
    }
    

	/** A node for showing log entries in a JTree. */
	public static final class Node {
		private static final long serialVersionUID = 1L;
		public Object[] params;
		public int[] children;
		public int id;
		public int childCount;
		
		public Node(int node,Object[] params,int childCount,int[] children){
			id = node;
			this.params=params;
			this.children=children;
			this.childCount=childCount;
		}
		
		@Override
		public String toString(){
			return showParams(params);
		}
		
		@Override
		public boolean equals(Object obj) {
			return obj instanceof Node && id == ((Node)obj).id;
		}
	}

	/** Builds a string to represent a node with the given parameters. */
	private static String showParams(Object[] params){
		if (params==null || params.length==0)
			return "nothing";
		else if (params[0]==null || !(params[0] instanceof EngineLogger.MsgType))
			return "non-visible";
		
		switch((EngineLogger.MsgType)params[0]){
		case EXECUTE: {
			if (params.length>3)
				if (params[3]==MsgType.ABORT)
					return params[1]+": Aborted: "+params[2];
				else if ((Integer)params[3]==0)
					return params[1]+":     "+params[2];
				else	
					return params[1]+": Page #"+params[3]+": "+params[2];
			else
				return "incorrectly built \"execute\" node: "+params[1]+" "+params[2];
		}
		case ROLE:
			return params[1]+" executing role "+params[2];
		case OPTION:
			if (params.length>2 && (params[2]!=MsgType.SEARCHMARK || params.length>3))
				return "considering option "+params[1]+" ("+(params.length>3 && params[3]!=MsgType.SEARCHMARK?params[3]:params[2])+")";
			else 
				return "considering option "+params[1]+" (Poisoned!)";
		case WORDSOCKETS:
			return "Word sockets";
		case WORDSOCKET:
			if (params.length>2 && params[2]!=MsgType.SEARCHMARK)
				return (String)params[1]+"   ("+params[2]+")";
			else
				return (String)params[1];
		case CHOOSE_OPTION:
			return "best inclination value = "+params[1]+" Choosen option: "+params[2];
		case DIROBJECT:
			if (params.length>2 && params[2]==MsgType.SIBLINGVALUE)
				return "DirObject "+params[1]+" considering reacting: REACTS";
			else
				return "DirObject "+params[1]+" considering reacting";
		case SUBJECT:
			if (params.length>2 && params[2]==MsgType.SIBLINGVALUE)
				return "Subject "+params[1]+" considering reacting: REACTS";
			else
				return "Subject "+params[1]+" considering reacting";
		case WITNESS:
			if (params.length>2 && params[2]==MsgType.SIBLINGVALUE)
				return "Witness "+params[1]+" considering reacting: REACTS";
			else
				return "Witness "+params[1]+" considering reacting";
		case FATE_REACTING:
			if (params.length>1 && params[1]==MsgType.SIBLINGVALUE)
				return "Fate considering reacting: REACTS";
			else
				return "Fate considering reacting";
		case DISQUALIFIED:
			return "Option ("+params[1]+") disqualified by failure to find an acceptable "+params[2];
		case SCRIPT:
			if (params.length>2 && params[2]!=MsgType.SEARCHMARK)
				return params[2]+": Script "+params[1];
			else
				return "Script "+params[1];
		case TOKEN:
			if (params.length<2)
				return "LogLizard error: Bad TOKEN node structure.";
			else if (params.length<3 || params[2]==MsgType.SEARCHMARK)
				return params[1]+" (Poisoned!)";
			else
				return params[1]+"   "+params[2];
		case POISON:
			return "Poison: "+params[1];
		}
		return "";
	}
}