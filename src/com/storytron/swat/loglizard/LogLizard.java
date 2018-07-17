package com.storytron.swat.loglizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import com.storytron.enginecommon.SessionLogoutException;
import com.storytron.enginecommon.Utils;

/** Dialog for showing the log tree. It also offers a button to save the
 * tree in a text file.
 * */
public class LogLizard extends JDialog 
		implements TreeWillExpandListener {
	private static final long serialVersionUID = 1L;
	
	private LogDownloadManager logManager;
	
	public LogLizard(Frame owner,LogDownloadManager lm) {
		super(owner);

		logManager = lm;

		JPanel outerLogLizardPanel = new JPanel();
		outerLogLizardPanel.setLayout(new BoxLayout(
				outerLogLizardPanel, BoxLayout.Y_AXIS));
		
		final JTree tree = new JTree(logManager.getTreeModel());
		tree.setToggleClickCount(0);
		tree.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if ((e.getModifiers() & KeyEvent.SHIFT_MASK) ==0)
					return;
				
				TreePath tp = tree.getClosestPathForLocation(e.getX(), e.getY());
				if (tp==null)
					return;
				// Only allow clicks on a handle.
				if (tree.getPathForLocation(e.getX(), e.getY())!=null)
					return;
				
				askToExpandDescendants(tp);
			}
		});
		tree.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.getModifiers()&KeyEvent.SHIFT_MASK)==0 || e.getKeyCode()!=KeyEvent.VK_RIGHT)
					return;
				TreePath tp = tree.getSelectionPath();
				if (tp==null || tree.isExpanded(tp))
					return;
				
				askToExpandDescendants(tp);
				e.consume();
			}
		});
		tree.setCellRenderer(new DefaultTreeCellRenderer(){
			private static final long serialVersionUID = 1L;
			@Override
			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean sel, boolean expanded, boolean leaf,
					int row, boolean hasFocus) {
				Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
						row, hasFocus);
				if (value instanceof LogTreeModel.Node) {
					LogTreeModel.Node n = (LogTreeModel.Node)value;
					if (Utils.markedBySearch(n.params))
						c.setBackground(Utils.lightGrayBackground);
					else if (isLastRootDescendent(n)) {
							if (containsSearchedDescendant(n))
								c.setBackground(Utils.lightGrayBackground);
							else
								c.setBackground(super.getBackgroundNonSelectionColor());
					} else if (isRootChild(n) && containsSearchedChild(n))
						c.setBackground(Utils.lightGrayBackground);
					else 
						c.setBackground(super.getBackgroundNonSelectionColor());
				} else
					c.setBackground(super.getBackgroundNonSelectionColor());				
				return c;
			}
			/** Tells if a node is the last root child. */
			private boolean isLastRootDescendent(LogTreeModel.Node n){
				final Object root = tree.getModel().getRoot();
				int childCount = tree.getModel().getChildCount(root);
				if (childCount==0)
					return false;
				final LogTreeModel.Node lastChild = (LogTreeModel.Node)tree.getModel().getChild(root,childCount-1); 
				return lastChild==n || isLastDescendent(lastChild,n);
			}
			private boolean isLastDescendent(LogTreeModel.Node root,LogTreeModel.Node n){
				if (root.children==null || root.children.length==0)
					return false;
				else {
					final LogTreeModel.Node lastChild = (LogTreeModel.Node)tree.getModel().getChild(root,root.children.length-1); 
					return lastChild==n || isLastDescendent(lastChild,n);	
				}
			}
			/** Tells if a node is a root child. */
			private boolean isRootChild(LogTreeModel.Node n){
				final Object root = tree.getModel().getRoot();
				return tree.getModel().getIndexOfChild(root,n)>=0;
			}

			private boolean containsSearchedDescendant(LogTreeModel.Node n){
				if (n.children==null || n.children.length==0)
					return false;
				
				int childCount = tree.getModel().getChildCount(n);
				for(int i=0;i<childCount;i++) {
					if (Utils.markedBySearch(((LogTreeModel.Node)tree.getModel().getChild(n,i)).params))
						return true;
						
				}
				return containsSearchedDescendant((LogTreeModel.Node)tree.getModel().getChild(n,n.children.length-1));
			}
			/** Tells if a child is a marked node. */
			private boolean containsSearchedChild(LogTreeModel.Node n){
				if (n.children==null || n.children.length==0)
					return false;
				
				int childCount = tree.getModel().getChildCount(n);
				for(int i=0;i<childCount;i++) {
					if (Utils.markedBySearch(((LogTreeModel.Node)tree.getModel().getChild(n,i)).params))
						return true;
						
				}
				return false;
			}
			
			@Override
			public Color getBackgroundNonSelectionColor() {
				return null;
			}
		});
		logManager.tree = tree;
		tree.addTreeWillExpandListener(this);
		((DefaultTreeCellRenderer)tree.getCellRenderer()).setOpenIcon(null);
		((DefaultTreeCellRenderer)tree.getCellRenderer()).setClosedIcon(null);
		((DefaultTreeCellRenderer)tree.getCellRenderer()).setLeafIcon(null);
		getContentPane().add(new JScrollPane(tree),BorderLayout.CENTER);
		setSize(600, 400);
		setLocation(100, 200);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setVisible(true);
	}
	
	public void treeWillCollapse(TreeExpansionEvent event)
			throws ExpandVetoException {}
	
	public void treeWillExpand(TreeExpansionEvent event)
			throws ExpandVetoException {

		// find the node
		TreePath tp = event.getPath();
		Object o = tp.getLastPathComponent();
		if (!(o instanceof LogTreeModel.Node))
			return;

		int treeIndex=logManager.getTreeModel().getIndexOfChild(logManager.getTreeModel().getRoot(),tp.getPathComponent(1));
		
		// Request data.
		try {
			if (!logManager.request(treeIndex,event.getPath(),false))
				// wait for the requested branch to expand the node
				// if this is a script node.
				if (LogDownloadManager.isFrontierNode(tp))
					throw new ExpandVetoException(event);
		} catch(SessionLogoutException e){
			Utils.showErrorDialog(logManager.tree, "Your session on the server has expired.","Connection error");
			if (LogDownloadManager.isFrontierNode(tp))
				throw new ExpandVetoException(event);
		}
	}
	
	/** Asks for expanding a node and all of its descendants.  */
	private void askToExpandDescendants(TreePath tp){
		// find the node
		if (!(tp.getLastPathComponent() instanceof LogTreeModel.Node))
			return;

		int treeIndex=logManager.getTreeModel().getIndexOfChild(logManager.getTreeModel().getRoot(),tp.getPathComponent(1));
		
		// Request data.
		boolean isFrontierNode = LogDownloadManager.isFrontierNode(tp);
		try{
			if (logManager.request(treeIndex,tp,isFrontierNode) || !isFrontierNode)
				expandNonFrontierNodes(logManager.tree, tp);
		} catch (SessionLogoutException e) {
			Utils.showErrorDialog(logManager.tree, "Your session on the server has expired.","Connection error");
		}
	}
	
	/** 
	 * Expands the last component of tp and all of its subtrees except
	 * the ones having frontier roots.
	 * */
	public static void expandNonFrontierNodes(JTree tree,TreePath tp){
		tree.expandPath(tp);

		int count = tree.getModel().getChildCount(tp.getLastPathComponent());
		for(int i=0;i<count;i++) {
			Object c = tree.getModel().getChild(tp.getLastPathComponent(),i);
			TreePath ntp = new Utils.MTreePath(tp,c);
			if (!LogDownloadManager.isFrontierNode(ntp))
				expandNonFrontierNodes(tree,ntp);
		}
	};
	
}
