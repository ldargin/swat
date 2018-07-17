package Engine.enginePackage;

import java.io.Serializable;
import java.util.LinkedList;

import com.storytron.swat.util.BufferedArray;
import com.storytron.swat.util.Compressed;
import com.storytron.swat.util.CompressedArray;

/** <p>
 * A class for building a log tree.
 * Each node in the log tree is a message, and can have other children 
 * messages.
 * <p>
 * Building of the tree is done through a current node which can be used to
 * insert new nodes in the tree with {@link #insertCurrent(Object[])} and 
 * {@link #insertChild(Object[])}.
 * <p>
 * To indicate that children will be inserted for the last child of the
 * current node call the {@link #down()} method.  
 * <p>
 * To indicate that all the children of the current node have been inserted, 
 * call the {@link #up()} method after inserting the last child. 
 * <p> 
 * The {@link TreeLogger} groups and compresses the nodes as they arrive.
 * The method {@link #popCompressedNodes()} will return the nodes that
 * have been compressed so far. It is possible that a group of inserted
 * nodes has not been compressed yet, so the method will not return them.
 * To get those nodes you will need to call {@link #flushNodes()} prior
 * to the call to {@link #popCompressedNodes()}. 
 * <p>
 * Try to call {@link #flushNodes()} as sparingly as possible, as it implies
 * some extra overhead.
 * <p>
 * The {@link TreeLogger} also will block execution of a thread inserting a node
 * if there are too many nodes. This is a practical limit to control memory usage.
 * The nodes must be removed from the tree. Most likely they will be downloaded
 * by the client which uses the engine. The node limit is {@link #MAX_BUFFERED_NODES}.  
 * <p>
 * If you don't want to log, just set the logging attribute to false
 * with {@link #setLogging(boolean)}.
 * */
public class TreeLogger {

	private LinkedList<Compressed<Object[]>> compressedNodes = new LinkedList<Compressed<Object[]>>();
	private BufferedArray<Object[]> nodes = new BufferedArray<Object[]>(2000,new CompressedArray<Object[]>(compressedNodes));
	private boolean logging = true;
	/** Maximum amount of nodes which can be buffered before blocking. */
	private final static int MAX_BUFFERED_NODES = 20000; 
	
	/** 
	 * Inserts a new child at the end of the children list of the
	 * current node. Then sets the newly inserted child as the
	 * current node. 
	 * @param params an array containing the message to log.
	 * @see #insertChild(Object[])
	 * */
	public final void insertCurrent(Object[] params) throws InterruptedException {
		insertChild(params);
		down();
	}
	
	/** Returns the reported nodes so far. */
	public final synchronized LinkedList<Compressed<Object[]>> popCompressedNodes(){
		LinkedList<Compressed<Object[]>> oldCompressedNodes = new LinkedList<Compressed<Object[]>>(compressedNodes);
		compressedNodes.clear();
		notify();
		return oldCompressedNodes;
	} 
	
	/** Sets the current node to be the last child of the current one.
	 * Does nothing if there the current node has no child. 
	 * */
	public synchronized void down() throws InterruptedException {
		if (MAX_BUFFERED_NODES<=nodes.size()) {
			onBufferCommandFull();
			while (MAX_BUFFERED_NODES<=nodes.size())
				wait();
		}
		nodes.add(new Object[0]);
	}
	
	/** 
	 * Sets the current node to be the parent of the current one.
	 * Does nothing is the current node is the root node.
	 * */
	public synchronized void up() throws InterruptedException {
		if (MAX_BUFFERED_NODES<=nodes.size()) {
			onBufferCommandFull();
			while (MAX_BUFFERED_NODES<=nodes.size())
				wait();
		}
		nodes.add(null);
	}

	/** 
	 * Override to perform some action before blocking for waiting someone
	 * to empty the buffer.
	 * */
	public void onBufferCommandFull() throws InterruptedException {};
	
	/** 
	 * Inserts a new child at the end of the children list of the
	 * current node. 
	 * @param params an array containing the message to log.
	 * */
	public final synchronized void insertChild(Object[] params) throws InterruptedException {
		if (MAX_BUFFERED_NODES<=nodes.size()) {
			onBufferCommandFull();
			while (MAX_BUFFERED_NODES<=nodes.size())
				wait();
		}
		nodes.add(params);
	}

	/**
	 * Compresses the current uncompressed nodes. This makes them available
	 * to fetch them with {@link #popCompressedNodes()}.
	 * */
	public void flushNodes(){
		Object[] elems = nodes.getBufferedElements();
		if (elems.length==0)
			return;
		
		nodes.clearBuffer();
		compressedNodes.add(new Compressed<Object[]>(elems));
	}
	
	/** Removes all the reported nodes. */
	void clear(){
		nodes.clear();
	}
	
	/** Tells if the given logger must log messages. */
	public final void setLogging(boolean logging) {
		this.logging = logging;
	}
	/** Tells if the given logger is logging messages. */
	public final boolean isLogging() {
		return logging;
	}

	/** 
	 * Nodes of the log tree. 
	 * The message is encoded as an array of Objects.
	 * Interpretation of the Object array is application specific.
	 * */
	public static final class Node implements Serializable {
		private static final long serialVersionUID = 1L;
		public LinkedList<Node> children=null;
		public Object[] params;
		public Node(Object[] params){
			this.params=params;
		}
		public Node(){	this(null);	}
		/** 
		 * Clones the node tree structure.
		 * {@link #params} fields are not cloned, though. 
		 * */
		public Node cloneNode(){
			Node n = new Node(params);
			if (children!=null) {
				n.children = new LinkedList<Node>();
				for(Node c:children)
					if (c!=null)
						n.children.add(c.cloneNode());
					else
						n.children.add(null);
			}
					
			return n;
		}
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

}
