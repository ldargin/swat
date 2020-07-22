package com.storytron.swat.loglizard;

import engPackage.EngineLogger;

/** 
 * Class for navigating a log tree.
 * It hides nodes not intended for presentation and groups
 * the data spread in various nodes that refers to only one.  
 * */
public final class LogTreeAdapter {

	/** Tree that is adapted. */
	private DiskIndexedTree tree;
	
	/** Creates a log tree adapter for the given tree. */
	public LogTreeAdapter(DiskIndexedTree tree){
		this.tree = tree;
	}
	
	/** @return the node for the given id. */
	public LogTreeModel.Node getNode(int node) {
		int[] children = null;
		// Get the children of the node.
		int childCount=getChildCount(node);
		if (childCount>0) {
	    	int child=getFirstChild(node);
			children = new int[childCount];
    		for(int i=0;i<childCount;i++){
    			children[i]=child;
    			child=getNextSibling(child);
    		}
		} else if (getFirstChild(node)==-2) {
			childCount = 1;
			children = new int[0];
		}
		
		// Get the parameters

		// search a value in any of the node children
		Object pval = null;
    	int sib=tree.getFirstChild(node);
    	while(sib>0){
    		pval=getValue(sib);
    		if (pval!=null)
    			break;
    		sib=tree.getNextSibling(sib);
    	}
    	// search for another value
		Object pval2 = null;
    	if (pval!=null) {
			sib=tree.getNextSibling(sib);
    		while(sib>0) {
    			pval2=getValue(sib);
    			if (pval2!=null)
    				break;
    			sib=tree.getNextSibling(sib);
    		}
    	}
		
		// search sibling values
		sib = tree.getNextSibling(node);
		Object sval=getSiblingValue(sib);
		Object sval2 = null;
		if (sval!=null) {
			sib = tree.getNextSibling(sib);
			sval2=getSiblingValue(sib);
		}

		Object[] params=null;
		int paramCount = tree.getParamCount(node);
		if (paramCount>0 || pval!=null) {
			// create array of parameters
			params = new Object[paramCount+(pval!=null?1:0)+(pval2!=null?1:0)+(sval!=null?1:0)+(sval2!=null?1:0)];
			for(int i=0;i<paramCount;i++)
				params[i] = tree.getParam(node,i);
			int next = paramCount;
			if (pval!=null) {
				params[next++]=pval;
				if (pval2!=null)
					params[next++]=pval2;
			}
			if (sval!=null) {
				params[next++]=sval;
				if (sval2!=null)
					params[next++]=sval2;
			}
		}
		return new LogTreeModel.Node(node,params,childCount,children);
	}

    /** Gets the first child skipping meta nodes. */
    public int getFirstChild(int node){
    	int sibling = tree.getFirstChild(node);
		while(sibling>=0) {
			if (isVisible(sibling))
				return sibling;
			sibling = tree.getNextSibling(sibling);
		}
		return sibling;
    }
    
    /** Gets the next sibling skipping meta nodes. */
    public int getNextSibling(int node){
    	int sibling = tree.getNextSibling(node);
		while(sibling>=0) {
			if (isVisible(sibling))
				return sibling;
			sibling = tree.getNextSibling(sibling);
		}
		return sibling;
    }
    
    /** @return the value associated to a child node of type {@link EngineLogger.MsgType#PARENTVALUE} */
    private Object getChildValue(int node){
    	int sib=tree.getFirstChild(node);
    	while(sib>0){
    		Object o=getValue(sib);
    		if (o!=null)
    			return o;
    		sib=tree.getNextSibling(sib);
    	}
    	return null;
    }
    
    /** @return the value associated to a node of type {@link EngineLogger.MsgType#PARENTVALUE} */
    private Object getValue(int node){
    	if (tree.getParamCount(node)>0 && EngineLogger.MsgType.PARENTVALUE==tree.getParam(node,0))
    		return tree.getParam(node,1);
    	else
    		return null;
    }

    /** @return the value associated to a node of type {@link EngineLogger.MsgType#SIBLINGVALUE} */
    private Object getSiblingValue(int node){
    	if (node>=0) {
    		int paramCount = tree.getParamCount(node); 
    		if (paramCount>0 && EngineLogger.MsgType.SIBLINGVALUE==tree.getParam(node,0))
    			if (paramCount>1)
    				return tree.getParam(node,1);
    			else
    				return tree.getParam(node,0);
    		else
    			return null;
    	} else
    		return null;
    }

    /** Tells if the node must be shown. */
    private boolean isVisible(int node){
    	if (tree.getParamCount(node)==0)
    		return true;
    	Object o = tree.getParam(node, 0);
    	if (!(o instanceof EngineLogger.MsgType))
    		return true;
    	
    	switch((EngineLogger.MsgType)o) {
    	case PARENTVALUE:
    	case SIBLINGVALUE:
    		return false;
    	case EXECUTE:
    		return getChildValue(node)!=null;
    	default:
    		return true;
		}
    }
    
    /** Gets the children count skipping meta nodes. */
    private int getChildCount(int node){
    	int sibling = tree.getFirstChild(node);
		int count=0;
		while(sibling>=0) {
			if (isVisible(sibling))
				count++;
			sibling = tree.getNextSibling(sibling);
		}
		return count;
    }
    
}
