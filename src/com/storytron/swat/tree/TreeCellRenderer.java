package com.storytron.swat.tree;

import java.awt.Component;

/**
 * Renderer for drawing tree nodes.
 * This is like the {@link javax.swing.tree.TreeCellRenderer} for 
 * the {@link Tree} class. 
 * */
public interface TreeCellRenderer {
	/**
	 * @param tree The tree in which value is stored.
	 * @param value is the node to draw.
	 * @param hasFocus whether the node has focus or not.
	 * @return The component which paint method should be called to make the drawing.
	 * */
	public Component getTreeCellRendererComponent(Tree tree, TNode value,boolean hasFocus);

	/**
	 * Used for getting the height of a node without going through all 
	 * the GUI calls for it.
	 * Must be consistent with the height given for the returned value of
	 * {@link #getTreeCellRendererComponent(Tree, TNode, boolean)}.
	 * */
	public int getHeight(TNode n);
	/**
	 * Used for getting the width of a node without going through all 
	 * the GUI calls for it.
	 * Must be consistent with the width given for the returned value of
	 * {@link #getTreeCellRendererComponent(Tree, TNode, boolean)}.
	 * */
	public int getWidth(TNode n);
	/**
	 * @return the component that is to be reused to draw each node.
	 * */
	public Component getComponent();
}