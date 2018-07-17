package com.storytron.swat.tree;

/**
 * Editor for editing nodes in a tree.
 * This is like the {@link javax.swing.tree.TreeCellEditor} for 
 * the {@link Tree} class. 
 * */
public interface TreeCellEditor {
	/**
	 * Signals start of node editing.
	 * @param value is the node to edit.
	 * @param x coordinate of the node in the tree coordinate space. 
	 * @param y coordinate of the node in the tree coordinate space.
	 * */
	public void startEditing(TNode value);
	/**
	 * Signals end of node editing.
	 * */
	public void stopEditing();
}