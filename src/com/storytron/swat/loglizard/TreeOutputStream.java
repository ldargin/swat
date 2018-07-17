package com.storytron.swat.loglizard;

import java.util.LinkedList;


/**
 * A stream to insert nodes into a {@link DiskIndexedTree}.
 * <p>
 * It modifies a tree by applying to it the commands
 * produced by the EngineLogger.
 * <p>
 * Through this class, new children can be added to existing nodes
 * on a tree, or whole new branches can be added to it.
 * <p>
 * To add new branches to a tree call the constructor passing it
 * the index of the last branch.
 * <p>
 * To add children to existing nodes, pass to the constructor the 
 * index of the root child which is the previous sibling of the
 * ancestor of those nodes. 
 * How does the implementation know which of the descendants you want 
 * to write? The branch will be navigated down by the commands passed
 * to {@link #write(Object[])}. When the commands start talking of
 * nodes that do not match those of the existing nodes, it will be
 * interpreted that those commands describe the new nodes that must be
 * inserted at the current position in the tree.
 * <p>
 * To keep track of where in the tree the stream is standing, a "cursor"
 * concept is implemented. The cursor is nothing else than a path in
 * the tree, together with an indicator of which was the last 
 * visited/inserted node.
 * */
public final class TreeOutputStream {

	/** Tree being modified by the stream. */
	private DiskIndexedTree tree;
	/** Path inside the tree where the cursor stands. */
	private LinkedList<Integer> stack;
	/** Index of the last visited/inserted node. */
	private int last;
	/** 
	 * Amount of commands that have been written in the current branch
	 * through this stream. 
	 * */
	private int commandCount=0;
	/** Amount of subtrees changed or added through this stream. */
	private int subtreeCount=0;
    /** Index of the root child whose descendants are being edited. */
    private int lastSurfaceBranch = -1;
    /** The listener for nodes being written. Leave as null if you need no listener. */
    public WriterListener writerListener;
    
	/** 
	 * Constructs a stream to add or modify a sequence of
	 * root children starting from the first one. How many root 
	 * children are modified depends on the data to be written to 
	 * the stream. 
	 * */
	public TreeOutputStream(DiskIndexedTree tree){
		this(tree,-1);
	}

	/** 
	 * Constructs a stream to add or modify a given sequence of
	 * root children. How many root children are modified depends 
	 * on the data to be written to the stream. 
	 * @param previousRootChild the root child previous to the one
	 *                          that will be modified or added. Can be
	 *                          -1 if the branch to modify is the first one.
	 * */
	public TreeOutputStream(DiskIndexedTree tree,int previousRootChild){
		this.tree = tree;
		stack = new LinkedList<Integer>();
		stack.add(0);
		if (previousRootChild>=0)
			last = previousRootChild;
		else
			last = 0;
	}
	
	/** Resets the stream and sets the new cursor position at the given branch. */
	public void reset(){
		stack.clear();
		stack.add(0);
		commandCount=0;
		subtreeCount=0;
		lastSurfaceBranch=-1;
		last = 0;
	}

	/** 
	 * Writes commands to modify the tree.
	 * @param commands is an array of commands to modify the tree at
	 *                 the current cursor position. 
	 * */
	public void write(Object[] commands){
		for(int j=0;j<commands.length;j++) {
			Object[] params = (Object[])commands[j];
			if (writerListener!=null) 
				writerListener.aboutToWrite(params);
			last = executeTreeCommand(tree, stack, last, params);
			commandCount++;
			if (stack.getLast()==0 && last!=lastSurfaceBranch){
				lastSurfaceBranch = last;
				commandCount=1;
				subtreeCount++;
			}
    	}
	}
	
	/** 
	 * Returns the amount of commands that have been written in the 
	 * current branch through this stream. 
	 * */
	int getLastBranchCommandCount(){
		return commandCount;
	}

	/** 
	 * Returns the amount of subtrees that have been changed or added 
	 * through this stream. 
	 * */
	int getSubtreeCount(){
		return subtreeCount;
	}

	/** Tells if the cursor is between one branch and the next. */
	boolean isAtBranchDivision(){
		return stack.getLast()==0 && last!=lastSurfaceBranch;
	}
	
	/** 
	 * <p>Insert nodes from a tree into the tree on disk.
	 * Insertion is done whenever a node with no children is found
	 * and the corresponding node to be inserted has children.
	 * </p>
	 * <p>The algorithm is designed so execution can be resumed when new commands
	 * arrive using the stack and the last inserted node.</p>
	 * @param c is the tree where nodes are inserted.
	 * @param stack is the path to the node being edited by the current command.
	 *              If the last node in the path is negative it is a node about to 
	 *              be inserted.
	 * @param last is the index of the last inserted node.   
	 * @param params is the next command to execute on the tree.
	 * */
	private static int executeTreeCommand(DiskIndexedTree c,LinkedList<Integer> stack,int last,Object[] params){
		if (params==null) { // edit the parent
			last = stack.getLast();
			stack.removeLast();
			return last;
		} else if (params.length==0) { // edit the last inserted node
			stack.add(last);
			return last;
		} else if (params.length==1 && params[0]==null) { // a frontier node with children
			assert last==stack.getLast();
			assert c.getFirstChild(last)<0;
			// Mark the node as having children.
			c.setFirstChild(last,-2);
			return last;
		} else if (last==stack.getLast()) { // insert a new child
			int firstChild = c.getFirstChild(last);
			if (firstChild<0)
				return c.addChild(last, params);
			else {
				assert c.getParamCount(firstChild)==params.length;
				assert checkParams(c,firstChild,params);
				return firstChild;
			}
		} else { // insert a sibling of last
			int sib = c.getNextSibling(last); 
			if (sib<0)
				return c.addChild(stack.getLast(), params);
			else {
				assert c.getParamCount(sib)==params.length;
				assert checkParams(c,sib,params);
				return sib;
			}
		} 
	}

	private static boolean checkParams(DiskIndexedTree c,int last,Object[] params0){
		for(int i=0;i<params0.length;i++)
			assert c.getParam(last, i).equals(params0[i]):"Parameters at position "+i+" do not match: "+c.getParam(last, i)+" =/= "+params0[i];
		return true;
	}
	
	/** An interface for a callback to announce the writing of a given node. */
	public static interface WriterListener {
		public void aboutToWrite(Object[] params);
	}
	
}
