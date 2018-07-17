package com.storytron.swat.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.CellRendererPane;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.plaf.basic.BasicTreeUI;

import com.storytron.swat.Swat;

/**
 * This is an implementation of a tree display.
 * It was needed when the nodes of the script tree started
 * changing its size when they toggled from expanded to collapsed
 * state. JTree did not handled this properly by default and there 
 * were not visible way of fixing this.
 * */
public class Tree extends JComponent {
	private static final long serialVersionUID = 1L;
	private TreeCellRenderer cr=createTreeCellRenderer();
	private TreeCellEditor ce=createTreeCellEditor();
	private Icon collapsedIcon, expandedIcon;
	private int indent=20;
	private TNode selectedNode=null;
	private CellRendererPane crpane = new CellRendererPane();
	private MouseListener mouseListener;
	private KeyListener keyListener;
	private FocusListener focusListener;
	private boolean reactToUserInput=true;
	
	public Tree() {
		super();
		ToolTipManager.sharedInstance().registerComponent(this);
		JTree t=new JTree();			
		collapsedIcon=((BasicTreeUI)t.getUI()).getCollapsedIcon();
		expandedIcon=((BasicTreeUI)t.getUI()).getExpandedIcon();
		installEventListeners();
		
		setBackground(Color.white);
		crpane.add(getCellRenderer().getComponent());
		add(crpane);
		validate();
	}
	
	/** Tells if the tree should react to mouse and keyboard input. */
	public void setReactToUserInput(boolean react){
		if (react==reactToUserInput)
			return;
		reactToUserInput = react;
		if (react) {
			addMouseListener(mouseListener);
			addKeyListener(keyListener);
			addFocusListener(focusListener);
		} else {
			removeMouseListener(mouseListener);
			removeKeyListener(keyListener);
			removeFocusListener(focusListener);
		}
	}
	
	/** Installs event listeners for mouse, keyboard and focus. */
	private void installEventListeners(){
		addMouseListener(mouseListener=new MouseListener(){
			/**
			 * Whenever the mouse is pressed we search for a node under it
			 * among the visible nodes, or handle ...
			 * */
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
				NodeEntry n=getNodeEntry(e.getX(), e.getY());
				if (n!=null && n.contains(e.getX(), e.getY())){
					nodeClicked(e, n.n, n.x, n.y, n.w, n.h);
				} else if (n!=null && !n.n.isLeaf()) {
					// clicked on a handle
				    if ((e.getModifiers() & KeyEvent.SHIFT_MASK)!=0) {
				    	unfold(n.n);
				    	revalidate();
				    	repaint();
				    } else
						setExpanded(n.n,!n.n.isExpanded());
				}
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});
		addKeyListener(keyListener=new KeyListener(){
			public void keyPressed(KeyEvent e) {
				if ((e.getModifiers() & ~KeyEvent.SHIFT_MASK)!=0) return;
				switch(e.getKeyCode()){
				case KeyEvent.VK_F2:
					if (getSelectedNode()!=null){
						startNodeEditing(getSelectedNode());
						e.consume();
					}
					break;
				case KeyEvent.VK_LEFT:
					if (getSelectedNode()!=null)
						if (!getSelectedNode().isLeaf() && getSelectedNode().isExpanded()){
							setExpanded(getSelectedNode(),false);
							e.consume();
						}else if (getSelectedNode().getParent()!=null &&
								(getSelectedNode().getParent().getParent()!=null ||
								isRootVisible())){
							setSelectedNode((TNode)getSelectedNode().getParent());
							scrollNodeToVisible(getSelectedNode());
							e.consume();
						}
					break;
				case KeyEvent.VK_RIGHT:
					if (getSelectedNode()!=null)
						if (!getSelectedNode().isLeaf() && !getSelectedNode().isExpanded()){
						    if ((e.getModifiers() & KeyEvent.SHIFT_MASK)!=0) {
						    	unfold(getSelectedNode());
						    	revalidate();
						    } else
						    	setExpanded(getSelectedNode(),true);
							repaint();
							e.consume();
						} else {
							TNode next=(TNode)((TNode)getSelectedNode()).getNextNode();
							if (next!=null)	{
								scrollNodeToVisible(getSelectedNode());
								setSelectedNode(next);
								e.consume();
							}
						}					
					break;
				case KeyEvent.VK_DOWN:
					if (getSelectedNode()!=null){
						int r=getRow(getSelectedNode());
						if (r+1<getRowCount()){
							setSelectedNode(getNode(r+1));
							scrollNodeToVisible(getSelectedNode());
							e.consume();
						}
					}
					break;
				case KeyEvent.VK_UP:
					if (getSelectedNode()!=null){
						int r=getRow(getSelectedNode());
						if (r>0){
							setSelectedNode(getNode(r-1));
							scrollNodeToVisible(getSelectedNode());
							e.consume();
						}
					}
					break;
				case KeyEvent.VK_END:
					if (getRowCount()>0){
						setSelectedNode(getNode(getRowCount()-1));
						scrollNodeToVisible(getSelectedNode());
						e.consume();
					}
					break;
				case KeyEvent.VK_BEGIN:
				case KeyEvent.VK_HOME:
					if (getRowCount()>0){
						setSelectedNode(getNode(0));
						scrollNodeToVisible(getSelectedNode());
						e.consume();
					}
					break;
				case KeyEvent.VK_PAGE_DOWN:
					if (getSelectedNode()!=null){
						int r=getRow(getSelectedNode());
						if (r+10<getRowCount())
							setSelectedNode(getNode(r+10));
						else setSelectedNode(getNode(getRowCount()-1));
						scrollNodeToVisible(getSelectedNode());
						e.consume();
					}
					break;
				case KeyEvent.VK_PAGE_UP:
					if (getSelectedNode()!=null){
						int r=getRow(getSelectedNode());
						if (r>=10)
							setSelectedNode(getNode(r-10));
						else setSelectedNode(getNode(0));
						scrollNodeToVisible(getSelectedNode());
						e.consume();
					}
					break;
				}
				if (getSelectedNode()==null && getRowCount()>0)
					switch(e.getKeyCode()){
					case KeyEvent.VK_LEFT:
					case KeyEvent.VK_RIGHT:
					case KeyEvent.VK_DOWN:
					case KeyEvent.VK_BEGIN:
					case KeyEvent.VK_HOME:
					case KeyEvent.VK_PAGE_DOWN:
						setSelectedNode(getNode(0));
						scrollNodeToVisible(getSelectedNode());
						e.consume();
						break;
					case KeyEvent.VK_UP:
					case KeyEvent.VK_END:
					case KeyEvent.VK_PAGE_UP:
						setSelectedNode(getNode(getRowCount()-1));
						scrollNodeToVisible(getSelectedNode());
						e.consume();
					}
			}
			public void keyReleased(KeyEvent e) {}
			public void keyTyped(KeyEvent e) {}
		});
		
		addFocusListener(focusListener=new FocusListener(){
			public void focusGained(FocusEvent e) {}
			public void focusLost(FocusEvent e) {
				repaintSelectedNode();
			}
		});
	}

	/**
	 * Override this to create a proper tree cell renderer.
	 * Or set one with {@link #setCellRenderer(TreeCellRenderer)}.
	 * */
	public TreeCellRenderer createTreeCellRenderer(){
		return null;
	}
	/**
	 * Override this to create a proper tree cell editor.
	 * Or set one with {@link #setCellEditor(TreeCellEditor)}.
	 * */
	public TreeCellEditor createTreeCellEditor(){
		return null;
	}
	/**
	 * Starts editing a node (if there is a {@link TreeCellEditor}).
	 * */
	public void startNodeEditing(TNode n){
		getCellEditor().startEditing(n);
	}
	@Override
	public Dimension getPreferredSize(){
		return getSize();
	}
	@Override
	public Dimension getSize(){ return getSize(new Dimension(0,0)); }
	public Dimension getSize(Dimension d){	
		if (root==null) return d;
		if (isRootVisible())
			return getSize(root,0,d);
		else {
			getSize(root,0,d);
			d.height-=getCellRenderer().getHeight(root);
			d.width-=indent;
			return d;
		}
	}

	private int rowHeight=0;
	/**
	 * The tree guarantees this height to be the one used for nodes.
	 * If set to zero the height of the component returned by the
	 * renderer is used instead. The height is set to zero by default. 
	 * */
	public void setRowHeight(int rowHeight) {
		this.rowHeight = rowHeight;
	}
	/**
	 * Returns the height used for drawing tree nodes 
	 * (if setted with {@link #setRowHeight(int)}). 
	 * */
	public int getRowHeight() {
		return rowHeight;
	}

	private Dimension getSize(TNode n,int depth,Dimension d){
		int w=getCellRenderer().getWidth(n)+depth*indent;
		if (w>d.width) 
			d.width=w;
		d.height+=getCellRenderer().getHeight(n);
		depth++;
		if (n.isExpanded())
			for(int i=0;i<n.getChildCount();i++)
				getSize((TNode)n.getChildAt(i),depth,d);
		return d;
	}
	private Dimension mSize=new Dimension();
	@Override
	public void revalidate(){
		mSize.height=mSize.width=0;
		// we recompute the size here to force other validations
		getSize(mSize);
		setSize(mSize);
		setMinimumSize(mSize);
		super.revalidate();
	}
	/**
	 * This is used to handle a cache of the visible nodes.
	 * Its useful for handling mouse events quickly.
	 * */
	private ArrayList<NodeEntry> nodeMap = new ArrayList<NodeEntry>();
	/**
	 * This utility class holds a node and its boundings as shown in screen
	 * in the tree coordinate space. 
	 * */
	public class NodeEntry {
		public int x,y,w,h;
		public TNode n;
		public NodeEntry(int x0,int y0,int w0,int h0,TNode node){
			n=node;
			x=x0;
			y=y0;
			w=w0;
			h=h0;
		}
		/**
		 * Tells if the boundings contains a point.
		 * */
		public boolean contains(int x0,int y0){
			return x<=x0 && y<=y0 && x0<x+w && y0<y+h;
		} 
	}
	
	/** This method paints the tree. */
	@Override
	protected void paintComponent(Graphics g){
		g.setColor(getBackground());			
		g.fillRect(0,0, getWidth(),getHeight());			
		drawTree(g);
	}
	
	private boolean rootVisible;
	/**
	 * Tells if the root should be visible.
	 * */
	public void setRootVisible(boolean b){ rootVisible=b; }
	/**
	 * Tells if the root is visible.
	 * */
	public boolean isRootVisible(){ return rootVisible; }
	
	private TNode root=null;
	/**
	 * Sets the root of the tree.
	 * */
	public void setRoot(TNode root) {
		this.root = root;
		selectedNode=null;
		this.revalidate();
	}
	/**
	 * Gets the root of the tree.
	 * */
	public TNode getRoot() {
		return root;
	}
	/**
	 * Sets the height of the handle or decoration for a given node.
	 * */
	public int getHandleHeight(TNode n){
		return 9;
	}
	
	private Insets insets;
	private void drawTree(Graphics g){
		nodeMap.clear();
		if (root==null) return;
		insets = getInsets(insets);
		Point p=new Point(insets.left,insets.top);			
		if (isRootVisible()){
			drawNode(g,p,root);
			p.x+=indent;
		}
		if (root.isExpanded())
			drawTreeChildren(g,p,root);
		if (isRootVisible()){
			p.x-=indent;
		}
	}
	/**
	 * Draws the children of a given node.
	 * The point returns the bottom corner of the node in the
	 * tree coordinate space.  
	 * */
	private void drawTreeChildren(Graphics g,Point p,TNode n){						
		int oldy=p.y;
		for(int i=0;i<n.getChildCount()-1;i++){
			TNode child=(TNode)n.getChildAt(i);
			// Here we draw the line from the bottom of the previous node
			// to the floor of the current one.
			g.setColor(Swat.shadow);
			g.drawLine(p.x+8, oldy, p.x+8, p.y+getCellRenderer().getHeight(child));
			oldy=p.y+getCellRenderer().getHeight(child);
			drawNodeAndChildren(g,p,child);
		}
		if (n.getChildCount()>0) {
			TNode child=(TNode)n.getChildAt(n.getChildCount()-1);
			// Here we draw the line from the bottom of the previous node
			// to the handle height of the current one.
			g.setColor(Swat.shadow);
			g.drawLine(p.x+8, oldy, p.x+8, p.y+getHandleHeight(child)-1);
			drawNodeAndChildren(g,p,child);
		}
	}
	/**
	 * Draws a node and then its children. 
	 * The point returns the bottom corner of the node in the
	 * tree coordinate space.  
	 * */
	private void drawNodeAndChildren(Graphics g,Point p,TNode n){
		drawNodeWithHandle(g,p,n);
		if (n.isExpanded()){
			p.x+=indent;
			drawTreeChildren(g,p,n);
			p.x-=indent;
		}
	}; 
	/**
	 * Draws a node and its handle. 
	 * The point returns the bottom corner of the node in the
	 * tree coordinate space.  
	 * */
	private void drawNodeWithHandle(Graphics g,Point p,TNode n){
		Component c=getCellRenderer().getTreeCellRendererComponent(this, n, false);
		drawHandle(g,c,p,n);
		p.x+=indent;
		drawNode(g,c,p,n);			
		p.x-=indent;
	}

	/**
	 * Draws a handle for the given node. 
	 * */
	private void drawHandle(Graphics g,Component c,Point p,TNode n){
		if (n.isLeaf()){
			g.setColor(Swat.shadow);
			g.drawLine(p.x+9, p.y+getHandleHeight(n), p.x+15, p.y+getHandleHeight(n));
		} else if (n.isExpanded())
			expandedIcon.paintIcon(c, g, p.x, p.y+getHandleHeight(n)-expandedIcon.getIconHeight()/2);
		else					
			collapsedIcon.paintIcon(c, g, p.x, p.y+getHandleHeight(n)-collapsedIcon.getIconHeight()/2);
	}
	/**
	 * Draws a node. 
	 * The point returns the bottom corner of the node in the
	 * tree coordinate space.  
	 * */
	private void drawNode(Graphics g,Point p,TNode n){
		Component c=getCellRenderer().getTreeCellRendererComponent(this, n, false);
		drawNode(g,c,p,n);
	}
	/**
	 * Draws a node using the given component. 
	 * The point returns the bottom corner of the node in the
	 * tree coordinate space.  
	 * */
	private void drawNode(Graphics g,Component c,Point p,TNode n){
		c.validate();
		nodeMap.add(new NodeEntry(p.x,p.y,c.getWidth(),c.getHeight(),n));
		crpane.paintComponent(g, c, this, p.x,p.y,c.getWidth(),c.getHeight(),false);
		c.setBounds(p.x, p.y, -c.getX(),-c.getY());
		p.y+=c.getHeight();
	}

	/**
	 * Gets the node under coordinates (x,y) in the tree
	 * coordinate space.
	 * */
	public TNode getNode(int x,int y){
		for(NodeEntry ne:nodeMap)
			if (ne.contains(x,y))
				return ne.n;
		return null;
	}
	public TNode getNode(int row){
		return nodeMap.get(row).n;
	}
	public NodeEntry getNodeEntry(TNode n){
		int i=getRow(n);
		return i!=-1?getNodeEntry(i):null;
	}
	public NodeEntry getNodeEntry(int row){
		return nodeMap.get(row);
	}
	/**
	 * Rerurns the row at which a node is being displayed.
	 * -1 if the node is not visible. 
	 * */
	public int getRow(TNode n){
		int i=0;
		for(NodeEntry ne:nodeMap){
			if (ne.n==n) return i;
			i++;
		}
		return -1;
	}
	/**
	 * Returns the number of visible nodes.
	 * */
	public int getRowCount(){ return nodeMap.size(); }
	/**
	 * Gets the node entry under coordinates (x,y) in the tree
	 * coordinate space.
	 * */
	public NodeEntry getNodeEntry(int x,int y){
		for(NodeEntry ne:nodeMap)
			if (ne.contains(x,y) || (ne.x-20<=x && ne.y<=y && x<ne.x && y<ne.y+getHandleHeight(ne.n)+collapsedIcon.getIconHeight()/2))
				return ne;
		return null;
	}
	/**
	 * Expands all the ancestors of a given node.  
	 * */
	public void expandPath(TNode node){
		node=(TNode)node.getParent();
		while(node!=null){
			node.setExpanded(true);
			node=(TNode)node.getParent();
		}
	}
	
	/** Sets the expanded state of a node calling
	 * and revalidate after it.
	 * */
	private void setExpanded(TNode node, boolean isExpanded) {
		if (node.isExpanded()!=isExpanded){
			node.setExpanded(isExpanded);
			revalidate();
		}
	}
	
	/**
	 * Gets the top left corner of a visible node.
	 * If the given node is not visible the top left corner of the last
	 * node is returned   
	 * */
	public void getTopLeftCorner(TNode node,Point p){
		p.y=p.x=0;
		if (isRootVisible())
			getTopLeftCorner(root,node,p);
		else for(int i=0;i<root.getChildCount();i++)
			if (getTopLeftCorner((TNode)root.getChildAt(i),node,p))
				break;
	}
	private boolean getTopLeftCorner(TNode current,TNode node,Point p){			
		if (current==node) return true;
		else if (!current.isExpanded() || current.isLeaf()) {
			p.y+=getCellRenderer().getHeight(current);
			return false;
		}
		
		p.y+=getCellRenderer().getHeight(current);
		p.x+=indent;
		for(int i=0;i<current.getChildCount();i++)
			if (getTopLeftCorner((TNode)current.getChildAt(i),node,p))
				return true;
		p.x-=indent;
		return false;
	}
	/**
	 * Expands all the tree nodes.
	 * */
	public void unfold(){ unfold(root); }
	/**
	 * Expands a tree nodes and all of its descendants.
	 * */
	public void unfold(TNode node){
		node.setExpanded(true);
		for(int i=0;i<node.getChildCount();i++)
			unfold((TNode)node.getChildAt(i));
	}
	/**
	 * Makes visible a given node. It expands the path to the node if it is
	 * not visible and also scrolls properly the scrollable containers of the
	 * tree.
	 * */
	public void scrollNodeToVisible(TNode node){
		if (node==null)
			return;
		
		expandPath(node);
		Point p=new Point(0,0);
		getTopLeftCorner(node,p);
		scrollRectToVisible(new Rectangle(p.x,p.y,getCellRenderer().getWidth(node),getCellRenderer().getHeight(node)));
	}		

	/** Sets the selected node. */
	public void setSelectedNode(TNode selectedNode) {
		TNode old = this.selectedNode;
		this.selectedNode = selectedNode;
		repaintSelectedNode(old);
	}
	@Override
	public boolean requestFocusInWindow(){
		boolean temp=super.requestFocusInWindow();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {	repaintSelectedNode(); }});
		return temp;
	}
	/** Repaints only the selected node. */
	private void repaintSelectedNode(){
		NodeEntry ne=getNodeEntry(selectedNode);
		if (ne!=null) repaint(ne.x,ne.y,ne.w,ne.h);
		else repaint();
	};
	/** Repaints the old selected node and the new selected node. */
	private void repaintSelectedNode(TNode old){
		NodeEntry ne=getNodeEntry(old);
		if (ne!=null) repaint(ne.x,ne.y,ne.w,ne.h);
		repaintSelectedNode();
	};
	/**
	 * Gets the selected node.
	 * */
	public TNode getSelectedNode() {	return selectedNode;	}
	
	/**
	 * Event called whenever a node is clicked.
	 * x, y, w and h are the node bounds in the tree
	 * coordinate space. 
	 * */
	public void nodeClicked(MouseEvent e,TNode n,int x,int y,int w,int h){
		setSelectedNode(n);
	}
	
	/**
	 * Sets the cell renderer to be used by this tree.
	 * */
	public void setCellRenderer(TreeCellRenderer cr) {
		this.cr = cr;
	}
	/**
	 * Gets the cell renderer to be used by this tree.
	 * */
	public TreeCellRenderer getCellRenderer() {
		return cr;
	}
	/**
	 * Sets the cell editor to be used by this tree.
	 * */
	public TreeCellEditor getCellEditor() {
		return ce;
	}
	/**
	 * Gets the cell editor to be used by this tree.
	 * */
	public void setCellEditor(TreeCellEditor ce) {
		this.ce=ce;
	}
}