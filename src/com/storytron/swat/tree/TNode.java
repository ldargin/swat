/**
 * 
 */
package com.storytron.swat.tree;

import javax.swing.tree.TreeNode;

/** Extends nodes with a expanded property. */
public interface TNode extends TreeNode {
	boolean isExpanded();
	void setExpanded(boolean expanded);
	/** Next node in a preorder traversal. */
	TreeNode getNextNode();
	TreeNode getFirstChild();
}
