package com.storytron.swat.verbeditor;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.Triplet;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.Swat;
import com.storytron.swat.util.EditorListener;
import com.storytron.swat.util.ErrorPopup;
import com.storytron.swat.util.LightweightPopup;
import com.storytron.swat.util.MaxLengthDocument;
import com.storytron.swat.util.UndoableAction;
import com.storytron.uber.Category;
import com.storytron.uber.Deikto;
import com.storytron.uber.Role;
import com.storytron.uber.Script;
import com.storytron.uber.Sentence;
import com.storytron.uber.Verb;
import com.storytron.uber.Role.Option;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.OperatorDictionary;

// [change-ld] Created VerbTreeNode to determine whether a tree node holds a 
// category or a verb
final class VerbTreeNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 1L;
	Object containedObject = null;
	
	public VerbTreeNode() {
		super("Verbs");
	}
	public VerbTreeNode(Verb verb, boolean arg1) {
		super(verb.getLabel(), arg1);
		setVerb(verb);
	}
	public VerbTreeNode(Verb verb) {
		this(verb,true);
	}
	public VerbTreeNode(Category cat) {
		super(cat.getName());
		setCategory(cat);
	}
	public boolean isCategory() {
		return containedObject instanceof Category;
	}
	public boolean isVerb() {
		return containedObject instanceof Verb;
	}
	public Verb getVerb() {
		if (isVerb()) {
			return (Verb)containedObject;
		} else {
			System.out.println("Can only use getVerb() for verb nodes");
			return null;
		}
	}
	private void setVerb(Verb newVerb) {
		containedObject = newVerb;
	}
	public Category getCategory() {
		if (isCategory()) {
			return (Category)containedObject;
		} else {
			System.out.println("Can only use getCategory() for category nodes");
			return null;
		}
	}
	private void setCategory(Category newCategory) {
		containedObject = newCategory;
	}
	@Override
	public String toString(){
		if (isVerb()) return getVerb().getLabel();
		else if (isCategory()) return getCategory().getName();
		else return super.toString();
	}
}

public class VerbTree extends JPanel implements WindowFocusListener, AWTEventListener,
					FocusListener, KeyListener {

	private static final long serialVersionUID = 1L;
	static Swat swat;
	static DataFlavor localObjectFlavor;
	static {
		try {
			localObjectFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	static DataFlavor[] supportedFlavors = {localObjectFlavor };
	
	/**
	 * We reimplemented the viewport and the scrollbar model of the 
	 * scrollpane that holds the tree.
	 * 
	 * The thing is done such that the view scrolls horizontally only 
	 * if the user uses the scrollbar. Any other attemp to scroll the 
	 * view horizontally from other events are cancelled.  
	 * */
	private boolean fromScrollBar=false;
	private class MyViewport extends JViewport{
		private static final long serialVersionUID = 1L;
		@Override
		public void setViewPosition(Point p) {
			Point q=getViewPosition();
			if (!fromScrollBar) p.x=q.x;
			super.setViewPosition(p);
		}
		/*void setViewHPosition(int x){
			Point p=getViewPosition();
			p.x=x;
			super.setViewPosition(p);
		}*/
	};
	private class MyScrollBarModel extends DefaultBoundedRangeModel {
		private static final long serialVersionUID = 1L;
		@Override
		public void setValue(int x) {
			fromScrollBar=true;
			super.setValue(x);
			fromScrollBar=false;
		}
	};
	
	private DragSource dragSource;
	private VerbTreeNode dropTargetNode = null;
	
	private VerbDnDTree tree; // the main data structure; this is what is displayed
	private Deikto dk; // an up-reference to the main data structure in the program
	private VerbEditor ve; // another up-reference to the editor that this class works inside
	private boolean useVerbMenu = false, useCatMenu = false;
	private Action renameMenuItem = null;
	private JMenuItem newverbMenuItem = null;
	private Action newverbAction = null;
	private Action duplicateverbMenuItem = null;
	private Action deleteMenuItem = null;
	private Category selectedCat = null;
	private Verb selectedVerb = null;
	private JScrollPane treeView; // a scroll pane holding the JTree
	private JPopupMenu popupMenu;
	private JPopupMenu categoryPopupMenu;
	private VerbTreeNode root; // the root node for the tree
	private DefaultTreeModel treeModel;  // LED new
	private TreePath selectionPath;
	
	private Category selectedCategory;
	private Category targetCategory;
	private MutableTreeNode treeDropNode;
	private MutableTreeNode treeDroppedNode;

	public final AbstractAction actions[] = 
		new AbstractAction[]{ 
			new AbstractAction("Rename"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent event) {
					TreePath path=tree.getSelectionPath();
					if (useVerbMenu)
						startEditing(path);
					else if (useCatMenu){
						startEditing(path);
					}
					tree.requestFocusInWindow();
					
				}		
			},
			new AbstractAction("New Category"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent event) {
					addNewCategory();
					tree.requestFocusInWindow();
				}		
			},
			new AbstractAction("New Verb"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent event) {
					addNewVerb();
					tree.requestFocusInWindow();
				}		
			},
			new AbstractAction("Duplicate Verb"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent event) {
					duplicateVerb();
					tree.requestFocusInWindow();
				}		
			},
			new AbstractAction("Delete"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent event) {
					if (useVerbMenu)
						deleteSelectedVerb(); 
					else if (useCatMenu)
						deleteSelectedCategory();
					tree.requestFocusInWindow();
				}		
			}
		};

	public void init(Deikto tdk){
		dk=tdk;
		createNodes(); // this method populates the tree with the verbs
	}
	
	/**
	 * Document filters take text written by the user as input and output
	 * what they think it should be written on an editor box.
	 * This particular document filter passes the text unmodified,
	 * but after passing the text it updates the editor box in case it needs 
	 * to be resized.  
	 * */
	private class MyDocumentFilter extends DocumentFilter {
		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
			super.insertString(fb, offset, string, attr);
			texteditorpopup.setSize(texteditorField.getPreferredSize().width+4,25);
			texteditorpopup.validate();
		}
		@Override
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			super.remove(fb, offset, length);
			texteditorpopup.setSize(texteditorField.getPreferredSize().width+4,25);
			texteditorpopup.validate();
		}
		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			super.replace(fb, offset, length, text, attrs);
			texteditorpopup.setSize(texteditorField.getPreferredSize().width+4,25);
			texteditorpopup.validate();
		}
	}

	private LightweightPopup texteditorpopup=new LightweightPopup();
	private Swat.TextField texteditorField = new Swat.TextField();
	private ErrorPopup errorPopup=new ErrorPopup();
	
//**********************************************************************
	public VerbTree(VerbEditor tve, Swat tSwat) {

		Color baseColor = new Color(255, 240, 255);//new Color(1.00f, 0.90f, 0.90f);

		texteditorField.setDocument(new MaxLengthDocument(Deikto.MAXIMUM_FIELD_LENGTH));
		((AbstractDocument)texteditorField.getDocument()).setDocumentFilter(new MyDocumentFilter());
		texteditorField.setBackground(new Color(255,248,255));
		texteditorField.setHorizontalAlignment(SwingConstants.CENTER);
		texteditorField.addFocusListener(this);
		texteditorField.addKeyListener(this);
		texteditorpopup.setContents(texteditorField);
		
		texteditorField.addActionListener(new EditorListener(texteditorField){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean timedActionPerformed(ActionEvent e) {
				return renamingAction(true);
			}
			@Override
			public String getText() {
				return editedPath.getLastPathComponent().toString();
			}
		});
		
		dk = null; // set up-reference pointer for future use
		ve = tve; // set up-reference pointer for future use
		swat = tSwat; // set up-reference pointer for future use
		setOpaque(true); // content panes must be opaque
		setBackground(baseColor);
		setLayout(new BorderLayout());
		//setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		root = new VerbTreeNode();
                
	    /* LED I base the tree off of a "DefaultTreeModel"
	     * so that I can call treeModel.nodeChanged()
	     * after renaming a node 
	     */
	    treeModel = new DefaultTreeModel(root);
		tree = new VerbDnDTree(treeModel);
		ToolTipManager.sharedInstance().registerComponent(tree);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.addKeyListener(new KeyListener(){

			public void keyPressed(KeyEvent e) {
				switch(e.getKeyCode()){
				case KeyEvent.VK_F2:
					{
						TreePath path=tree.getSelectionPath();
						if (path!=null && !isInSystemCategory((VerbTreeNode)path.getLastPathComponent())) 
							startEditing(path);
					}
					break;
				case KeyEvent.VK_DELETE:
					{
						TreePath path=tree.getSelectionPath();
						if (path!=null && !isInSystemCategory((VerbTreeNode)path.getLastPathComponent())) { 
							if (((VerbTreeNode)path.getLastPathComponent()).isVerb())
								deleteSelectedVerb();
							else 
								deleteSelectedCategory();
						}
					}
					break;
				case KeyEvent.VK_ENTER:
					if (tree.getSelectionPath()==null) break;
					VerbTreeNode leafNode = (VerbTreeNode)tree.getSelectionPath().getLastPathComponent();
					if (leafNode.isVerb())
						ve.setVerb(leafNode.getVerb());
					break;
				}
			}
			public void keyReleased(KeyEvent e) {}
			public void keyTyped(KeyEvent e) {}
		});
                
		treeView = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		treeView.setViewport(new MyViewport());
		treeView.setViewportView(tree);
		treeView.getHorizontalScrollBar().setModel(new MyScrollBarModel());
		
		tree.setBackground(baseColor);
		Border treeBorder = BorderFactory.createLineBorder(baseColor);
		tree.setBorder(treeBorder);

		add(treeView, BorderLayout.CENTER);

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath treePath = tree.getSelectionPath();
				validate();
				selectedVerb=null;
				selectedCat=null;
				if (treePath != null) {
			  		VerbTreeNode leaf = (VerbTreeNode)treePath.getLastPathComponent();
			  		useVerbMenu=leaf.isVerb();
		  			useCatMenu=leaf.isCategory();
		  			selectedVerb=useVerbMenu?leaf.getVerb():null;
		  			selectedCat=useCatMenu?leaf.getCategory():null;
			  		if (useVerbMenu) 			  			
				  		ve.checkAddOptionButton();
		  			
					boolean isSystemCategory = isInSystemCategory(leaf);
					deleteMenuItem.setEnabled(!isSystemCategory && (!leaf.isCategory() || !leaf.getCategory().hasAnyVerbs(dk)));
					duplicateverbMenuItem.setEnabled(!isSystemCategory && leaf.isVerb());
					renameMenuItem.setEnabled(!isSystemCategory);
					newverbAction.setEnabled(!isSystemCategory);
				} else {
					deleteMenuItem.setEnabled(false);
					duplicateverbMenuItem.setEnabled(false);
					renameMenuItem.setEnabled(false);
					newverbAction.setEnabled(false);
				}
			}
		});
		
		/* Add popup menu for the verb tree */
		popupMenu = new JPopupMenu();

		for(Action a:actions)			
			popupMenu.add(a);
		renameMenuItem = actions[0]; // This to so that I can disable or enable it.
		newverbAction = actions[2];
		newverbMenuItem = (JMenuItem)popupMenu.getComponent(2);
		duplicateverbMenuItem = actions[3];
		deleteMenuItem = actions[4];
		
		/* Add popup menu for the category tree */
		categoryPopupMenu = new JPopupMenu();
		ActionListener categoryPopupMenuListener = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String actionCommand = event.getActionCommand();

				if (actionCommand.equals("Above")) 
					insertCategory(selectedCategory, targetCategory, treeDroppedNode, treeDropNode);   

				if (actionCommand.equals("Into")) 
					addSubcategory(selectedCategory, targetCategory, treeDroppedNode, treeDropNode);

				System.out.println("Popup menu item [" + event.getActionCommand() + "] was pressed.");
			}

		};				
		JMenuItem catMenuItem;
		// Rename command
		categoryPopupMenu.add(catMenuItem = new JMenuItem("Above"));
		catMenuItem.setHorizontalTextPosition(JMenuItem.RIGHT);
		catMenuItem.addActionListener(categoryPopupMenuListener);

		// New Category command
		categoryPopupMenu.add(catMenuItem = new JMenuItem("Into"));
		catMenuItem.setHorizontalTextPosition(JMenuItem.RIGHT);
		catMenuItem.addActionListener(categoryPopupMenuListener);
				
		
		
		
		
		// Open the selected verb when its double-clicked
		MouseListener dblClickListener = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					selectionPath = tree.getPathForLocation(e.getX(), e.getY());
					tree.setSelectionPath(selectionPath);
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
				} else {
					int selectedRow = tree.getRowForLocation(e.getX(), e.getY());
					if (selectedRow != -1) {
						if (e.getClickCount() == 2) {
							// Open only verb nodes
							
							selectionPath = tree.getPathForLocation(e.getX(), e.getY());
							if (selectionPath != null) {
								VerbTreeNode leafNode = (VerbTreeNode)selectionPath.getLastPathComponent();
								if (leafNode.isVerb())
									ve.setVerb(getSelectedVerbLabel());
							}
						}
			         }
				}			
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					selectionPath = tree.getPathForLocation(e.getX(), e.getY());
					tree.setSelectionPath(selectionPath);
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		 };
		 tree.addMouseListener(dblClickListener);

	  validate();
	}
	
	/** Used to tell if we are about to edit a node in the system category. 
	 * We should prevent such editings. 
	 * */
	private boolean isInSystemCategory(VerbTreeNode n){
		return n.isVerb() && n.getVerb().getCategory().equals("System")
		|| n.isCategory() && n.getCategory().getName().equals("System");
	}
	@Override
	public boolean requestFocusInWindow(){
		return tree.requestFocusInWindow();
	}
	/**
	 * Shows the editor box popup.
	 * */
	private void showPopup(int x,int y) {
		if (texteditorpopup.getParent()!=null) return;
		
		SwingUtilities.getWindowAncestor(tree).addWindowFocusListener(this);
		Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.MOUSE_EVENT_MASK);
					
		texteditorpopup.showPopup(tree,x,y);
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){ texteditorField.requestFocusInWindow();}
		});
	} 
	/**
	 * Hides the editor box popup.
	 * */
	private void hidePopup() {
		hidePopup(true);
	}
	
	/**
	 * Hides the editor box popup.
	 * The argument tell if the tree should be repainted
	 * after closing the popup. 
	 * */
	private void hidePopup(boolean repaint) {
		if (!texteditorpopup.isVisible()) return;
		
		SwingUtilities.getWindowAncestor(tree).removeWindowFocusListener(this);
		texteditorpopup.hidePopup();
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
		if (repaint) tree.repaint();
	}
	/**
	 * These are methods from the {@link WindowFocusListener} interface.
	 * */
	public void windowGainedFocus(WindowEvent e) {}
	/**
	 * When the window looses focus we want the popup to be hidden.  
	 * */
	public void windowLostFocus(WindowEvent e) { cancelEditing(); }

	/** These are methods from the {@link FocusListener} interface. */
	public void focusGained(FocusEvent e) {}
	/**
	* When the textbox looses focus unexpectedly we want it to
	* recover it inmediatly (if it did not become hidden).
	* */
	public void focusLost(FocusEvent e) { texteditorField.requestFocusInWindow();  }

	/**
	 * This event dispatcher gets called whenever the user clicks
	 * somewhere. If the user clicks outside the popup we want the
	 * popup to hide. This method is from the interface 
	 * {@link AWTEventListener}.
	 * */
	public void eventDispatched(AWTEvent e){			
		MouseEvent me = (MouseEvent)e;
		if (me.getID()!=MouseEvent.MOUSE_PRESSED || texteditorpopup==null) return;
		if (me.getComponent()==null) {
			cancelEditing();
			return;
		}
		Point p = me.getPoint();
		SwingUtilities.convertPointToScreen(p,me.getComponent());
		Point sp=texteditorField.getLocationOnScreen();		
		if (!texteditorField.contains(p.x-sp.x,p.y-sp.y))
			cancelEditing();			
	}
	
	/**
	 * When other keys than characters and editor shortcuts are pressed
	 * we want the popup to close.
	 * */
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()){
		case KeyEvent.VK_ESCAPE:
		case KeyEvent.VK_TAB:
		case KeyEvent.VK_ENTER:
			stopEditing();
			e.consume();
			break;
		case KeyEvent.VK_CONTROL:		
		case KeyEvent.VK_ALT_GRAPH:
		case KeyEvent.VK_ALT:
			break;
		default:
			if (e.getModifiersEx()!=KeyEvent.SHIFT_DOWN_MASK && e.getModifiersEx()!=0)				
				cancelEditing();
		}
	}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	private TreePath editedPath=null;
	/**
	 * Shows the editor box popup.
	 * */
	public void startEditing(TreePath path){
		editedPath=path;
		texteditorField.setText(editedPath.getLastPathComponent().toString().trim());
		texteditorField.selectAll();
		Rectangle bounds=tree.getPathBounds(editedPath);
		showPopup(bounds.x+1,bounds.y-5);
	}

	/** Hides the editor box if the input is correct. */
	public void stopEditing(){
		if (renamingAction(true)){
			hidePopup();
			tree.requestFocusInWindow();
		}
	}

	/** Hides the editor box popup and saves the text into the node if correct. */
	public void cancelEditing(){
		hidePopup();
		tree.requestFocusInWindow();
		
		renamingAction(false);
	}

	private boolean renamingAction(boolean showError){
		String newValue=texteditorField.getText().trim();
		if (((VerbTreeNode)editedPath.getLastPathComponent()).isCategory()){
			if (selectedCat!=null && !selectedCat.getName().equals(newValue)) {
				if (dk.categories.findChild(newValue)!=null) {
					if (showError && SwingUtilities.getWindowAncestor(texteditorField)!=null) 
						errorPopup.showError(swat.getMyFrame(),texteditorField.getLocationOnScreen(),"Category "+newValue+" does already exist.");
					return false;
				}
				renameSelectedCategory(newValue);
			}
		} else if (selectedVerb!=null && !selectedVerb.getLabel().equals(newValue)) {
			int i=dk.findVerb(newValue);
			if (i!=-1) {
				if (showError && SwingUtilities.getWindowAncestor(texteditorField)!=null) 
					errorPopup.showError(swat.getMyFrame(),texteditorField.getLocationOnScreen(),"Verb "+newValue+" does already exist in category "+dk.getVerb(i).getCategory()+".");
				return false;
			};
			renameSelectedVerb(newValue);
		}
		return true;
	};

//**********************************************************************
	public void setSelectedVerb(String verbLabel){
		VerbTreeNode n = searchNode((VerbTreeNode)treeModel.getRoot(),verbLabel);
		if (n != null) {		
			TreePath tp = new TreePath(n.getPath());
			tree.setSelectionPath(tp);
			tree.expandPath(tp);
			tree.scrollPathToVisible(tp);
		} else System.out.println("VerbTree.setSelectedVerb: Verb \""+verbLabel+"\" does not exist.");
	}
    private VerbTreeNode searchNode(VerbTreeNode o,String verbLabel){
    	if (o.isVerb() && o.getVerb().getLabel().equals(verbLabel))
    		return o;
    	else for(int i=0;i<treeModel.getChildCount(o);i++) {
    		VerbTreeNode o2 = searchNode((VerbTreeNode)treeModel.getChild(o,i),verbLabel);
    		if (o2!=null) return o2;
    	}
    	return null;
    };	
//**********************************************************************
	public String getSelectedVerbLabel() {
		return selectedVerb!=null?selectedVerb.getLabel():null;
	}
	public void addTreeSelectionListener(TreeSelectionListener l){
		tree.addTreeSelectionListener(l);
	}
//**********************************************************************
	public void renameNode(boolean isCategory,String oldName, String newName) {
		renameNodeSub(isCategory,oldName, newName, root.children());
	}
  
	@SuppressWarnings("unchecked")
	public void renameNodeSub(boolean isCategory,String oldName, String newName, Enumeration children) {	
	    for (Enumeration e = children; e.hasMoreElements();) {
	    	VerbTreeNode zNode = (VerbTreeNode)e.nextElement();
    		if (isCategory==zNode.isCategory() && zNode.getUserObject().equals(oldName)) {
    			zNode.setUserObject(newName);

	            /* LED Here I tell the tree that
	             * a change has occurred */
	            treeModel.nodeChanged(zNode);
				showAndSelect(zNode);
	            break;
    		} else {
    			if (zNode.children().hasMoreElements()) {
    				renameNodeSub(isCategory,oldName, newName, zNode.children());
    			}
    				
    		}
	    }
	}
    		

//**********************************************************************
	private void deleteNode(String nodeName) {
		deleteNodeSub(nodeName, root.children());
	}
	
	@SuppressWarnings("unchecked")
	private void deleteNodeSub(String nodeName, Enumeration children) {
	    for (Enumeration e = children; e.hasMoreElements();) {
	    	VerbTreeNode zNode = (VerbTreeNode)e.nextElement();
    		if (zNode.getUserObject().equals(nodeName))  {
    			VerbTreeNode zParent = (VerbTreeNode)zNode.getParent();
    			zNode.removeFromParent();
    			treeModel.nodeStructureChanged(zParent);
    			treeModel.nodeChanged(zParent);
    			break;
    		} else {
    			if (zNode.children().hasMoreElements()) {
    				deleteNodeSub(nodeName, zNode.children());
    			}    			
    		}
	    }
	}
//	**********************************************************************	
  private void createNodes() {
	  // [change-ld] replaced code with call to a recursive function
	  // to generate nodes an arbitrary number of levels deep
	  /*      
    for (int i = 0; (i < Verb.cMenuCategories); ++i) {
    	DefaultMutableTreeNode category = new DefaultMutableTreeNode(Verb.menuCategory[i]);
      root.add(category);
      */
      createNodeChildren(root, dk.categories);
      /*
      for (int j = 0; (j < dk.getVerbCount()); ++j) {
      		if (dk.getVerb(j).category.equals(Verb.menuCategory[i])) {
      			DefaultMutableTreeNode verb = new DefaultMutableTreeNode(dk.getVerb(j).label);
     			category.add(verb);
      		}
      }
      */

  }
  
  // Generate nodes an arbitrary number of levels deep
  private void createNodeChildren(VerbTreeNode root, Category source) {
	  ArrayList<String> verbNames;
	  int numVerbs = 0;
	  for (Category child: source.getChildren()) {
		  VerbTreeNode cat = new VerbTreeNode(child);		  
		  root.add(cat);
		  if (child.getChildren().size() > 0) 
			  createNodeChildren(cat, child);
		  
		  numVerbs = child.hasNumVerbs(dk);
		  if (numVerbs > 0) {
			  verbNames = child.getVerbNames(dk);
			  for (String label: verbNames) {
				  Verb verbObj = dk.getVerb(dk.findVerb(label));
				  VerbTreeNode verb = new VerbTreeNode(verbObj);
				  cat.add(verb);
			  }
		  }		  
	  }
  }
  
//**********************************************************************		
  public void reload() {
	  root.removeAllChildren();

	  // [change-ld] replaced code with call to a recursive function
	  // to generate nodes an arbitrary number of levels deep  		
	  createNodeChildren(root, dk.categories);
	  /*
	   for (int i = 0; (i < Verb.cMenuCategories); ++i) {
    		DefaultMutableTreeNode category = new DefaultMutableTreeNode(Verb.menuCategory[i]);
    		root.add(category);
    		for (int j = 0; (j < dk.getVerbCount()); ++j) {
	      		if (dk.getVerb(j).category.equals(Verb.menuCategory[i])) {
	      			DefaultMutableTreeNode verb = new DefaultMutableTreeNode(dk.getVerb(j).label);
	     			category.add(verb);
	      		}
           }
      
      	}
      	*/
    treeModel.reload();
  }
  
	private void addNewVerb() {
		int i=0;
		while(dk.findVerb(i==0?"new verb":"new verb "+i)!=-1) i++;
		final String verbLabel=i==0?"new verb":"new verb "+i;
 	
		final Verb verb = new Verb(verbLabel);
		for(int iWordsocket=0;iWordsocket<Sentence.MaxWordSockets;iWordsocket++){
			if (verb.isWordSocketActive(iWordsocket)) {
				if (verb.getWordsocketTextScript(iWordsocket)==null)
					verb.getWSData(iWordsocket).text = verb.defaultWordsocketTextScript(iWordsocket);
				if (verb.getSuffix(iWordsocket)==null)
					verb.getWSData(iWordsocket).suffix = verb.defaultSuffixScript(iWordsocket,"");
			}
		}
		final VerbTreeNode vn=(VerbTreeNode)tree.getLastSelectedPathComponent();
		
		VerbTreeNode vn1=vn==null?null
				:vn.isVerb()? vn
				:vn.isCategory()?vn:null;
		VerbTreeNode cat = vn1==null ? findCategoryNode(dk.categories.getChildren().get(0).getName())
				:vn1.isVerb() ? findCategoryNode(vn1.getVerb().getCategory())
				:vn1.isCategory() ? vn1
				:findCategoryNode(dk.categories.getChildren().get(0).getName());
		int pos = vn1!=null && vn1.isVerb() ? cat.getIndex(vn1): cat.getChildCount();
		verb.setCategory(cat.getCategory().getName());
		addVerb(pos,cat,verb);
		ve.setVerb(verb);
		startEditing(tree.getSelectionPath());
		
		new UndoableAction(swat,false,"add verb "+verb.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				VerbTreeNode vn1=vn==null?null
						:vn.isVerb()?findVerbNode(vn.getVerb())
						:vn.isCategory()?findCategoryNode(vn.getCategory().getName()):null;
				VerbTreeNode cat = vn1==null ? findCategoryNode(dk.categories.getChildren().get(0).getName())
						:vn1.isVerb() ? findCategoryNode(vn1.getVerb().getCategory())
						:vn1.isCategory() ? vn1
						:findCategoryNode(dk.categories.getChildren().get(0).getName());
				int pos = vn1!=null && vn1.isVerb() ? cat.getIndex(vn1): cat.getChildCount();
				verb.setCategory(cat.getCategory().getName());
				addVerb(pos,cat,verb);
				ve.setVerb(verb);
			}
			@Override
			public void myUndo() {
				deleteVerb(verb);						
			}
		};
	}
	private void checkAddVerbAction() {
		newverbAction.setEnabled(dk.getVerbCount()<dk.limits.maximumVerbCount);
		String t;
		if (!newverbAction.isEnabled())
			t="Cannot have more than "+swat.dk.limits.maximumVerbCount+" verbs.";
		else
			t=null;
		newverbMenuItem.setToolTipText(t);
		ve.newverbMenuItem.setToolTipText(t);
	}
	/** Converts a location in the verb tree into an index in the verb array of the storyworld. */
	private int findLinearVerbIndex(int pos,VerbTreeNode catN){
		if (catN.getChildCount()==0) {
			int catIndex = dk.categories.getIndex(catN.getCategory());
			for(int i=catIndex-1;i>=0;i--) {
				if (root.getChildAt(i).getChildCount()>0)
					return ((VerbTreeNode)root.getChildAt(i).getChildAt(root.getChildAt(i).getChildCount()-1)).getVerb().getReference().getIndex()+pos;
			}
			return pos;
		} else
			return ((VerbTreeNode)catN.getChildAt(0)).getVerb().getReference().getIndex()+pos;
	}
	private void addVerb(int pos,VerbTreeNode catN,Verb verbObj){
		try {
			dk.addVerb(findLinearVerbIndex(pos,catN),verbObj);
		} catch (LimitException e) { throw new RuntimeException(e); }
		
		// Add a new node for the verb
		VerbTreeNode verb = new VerbTreeNode(verbObj);
		catN.insert(verb,pos);
		// Notify the tree that a node was added
		treeModel.nodesWereInserted(catN, new int[]{pos});
		checkAddVerbAction();
		showAndSelect(verb);
	}
	private void addVerb(int pos,Verb verbObj){
		addVerb(pos,findCategoryNode(verbObj.getCategory()),verbObj);
	}

	private void duplicateVerb(){
		final Verb oldVerb = selectedVerb;
		final Verb newVerb = selectedVerb.clone(false);
		
		String baseName = "copy of "+oldVerb.getLabel();
		baseName = baseName.substring(0,Math.min(baseName.length(),Deikto.MAXIMUM_FIELD_LENGTH-4));
		int i=0;
		while(dk.findVerb(i==0?baseName:baseName+" "+i)!=-1) i++;
		newVerb.setLabel(i==0?baseName:baseName+" "+i);

		final VerbTreeNode vn=(VerbTreeNode)tree.getLastSelectedPathComponent();
		
		int pos = vn.getParent().getIndex(vn);
		addVerb(pos,(VerbTreeNode)vn.getParent(),newVerb);
		ve.setVerb(newVerb);
		startEditing(tree.getSelectionPath());
		
		new UndoableAction(swat,false,"duplicate verb "+oldVerb.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				VerbTreeNode vn1 = findVerbNode(vn.getVerb());
				int pos = vn1.getParent().getIndex(vn);
				addVerb(pos,(VerbTreeNode)vn.getParent(),newVerb);
				ve.setVerb(newVerb);
			}
			@Override
			public void myUndo() {
				ve.setVerb(oldVerb);
				deleteVerb(newVerb);
			}
		};
	};
	
	private void deleteSelectedVerb() {
		final LinkedList<Triplet<Role.Link,Option,Integer>> options=new LinkedList<Triplet<Role.Link,Option,Integer>>();
		for (Verb zVerb: dk.getVerbs()) { 
			for (Role.Link zRole: zVerb.getRoles()) {
				int i=0;
				for (Option zOption:zRole.getRole().getOptions()){ 
					if (zOption.getPointedVerb()==selectedVerb) 
						options.add(new Triplet<Role.Link,Option,Integer>(zRole,zOption,i));
					i++;
				}
			}
		}

		final LinkedList<Script.Node> modifiedNodes = new LinkedList<Script.Node>();
		dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Node n) {
				if (n.getConstant() == selectedVerb)
					modifiedNodes.add(n);
				return true;
			}
		});
		
		final Verb verb = selectedVerb;
		VerbTreeNode verbN = findVerbNode(selectedVerb);
		final int pos = verbN.getParent().getIndex(verbN);
		new UndoableAction(swat,"delete verb "+selectedVerb.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				deleteVerb(verb);
				ve.updateRoleLinkButton();
				ve.updateOptionLinkButton();
				ve.repaintScript();
			}
			@Override
			public void myUndo() {
				addVerb(pos,verb);				
				try {
					for(Triplet<Role.Link,Option,Integer> o:options) { 
						dk.addOption(o.first.getRole(),o.third,o.second);
						if (o.first==ve.getRole()) {
							ve.setScriptPath(null,null);
							ve.loadRoleInfo();
						}
					}
				} catch (LimitException e) { throw new RuntimeException(e); }
				for(Script.Node n:modifiedNodes)
					n.setOperatorValue(OperatorDictionary.getVerbConstantOperator(),verb);
				ve.updateRoleLinkButton();
				ve.updateOptionLinkButton();
				ve.repaintScript();
			}
			
		};
	}
	private void deleteVerb(Verb mVerb) {
		boolean reloadOptions=ve.getRole()!=null && ve.getRole().getRole().getOptionIndex(mVerb.getLabel())>=0;
		
		// remove the verb tree node
		deleteNode(mVerb.getLabel());
		dk.removeVerb(mVerb);
		if (mVerb==ve.getVerb()) 
			ve.loadVerbInfo(0);
		else if (reloadOptions) {
			ve.setScriptPath(null,null);
			ve.loadRoleInfo();
		}
		checkAddVerbAction();
		validate();
	}

	private VerbTreeNode findCategoryNode(String category){
		return findCategoryNode(root,category);
	}
	private VerbTreeNode findCategoryNode(VerbTreeNode n,String category){
		for(int i=0;i<n.getChildCount();i++)
			if (n.getChildAt(i).toString().equals(category))				
				return (VerbTreeNode)n.getChildAt(i);
			else if (n.isCategory()) {
				VerbTreeNode catN=findCategoryNode((VerbTreeNode)n.getChildAt(i),category);
				if (catN!=null) return catN;
			}
		return null;
	}
	
	private VerbTreeNode findVerbNode(Verb verb){
		VerbTreeNode cat=findCategoryNode(verb.getCategory());
		if (cat==null) return null;
		
		for(int i=0;i<cat.getChildCount();i++)
			if (cat.getChildAt(i).toString().equals(verb.getLabel()))				
				return (VerbTreeNode)cat.getChildAt(i);
		return null;
	}
	
	private void addNewCategory() {
		int i=0;
		while(dk.categories.findChild(i==0?"new category":"new category "+i)!=null) i++;
		final String catLabel=i==0?"new category":"new category "+i;

		final Category newCat =  new Category(catLabel, dk.categories);
		addCategory(newCat);
		startEditing(tree.getSelectionPath());
		
		new UndoableAction(swat,false,"add category "+catLabel){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				newCat.setParent(dk,dk.categories);
				addCategory(newCat);						
			}
				@Override
			public void myUndo() {
				deleteCategory(newCat);
			}
		};
	}
	private void addCategory(Category cat) {		
		addCategory(dk.categories.getChildren().size(),cat);
	}
	private void addCategory(int index,Category cat) {		
		cat.setParent(dk,index,dk.categories);
		reload();
		showAndSelect(findCategoryNode(cat.getName()));
	}
	
	private void renameSelectedCategory(String zLabel){
		final Category cat = selectedCat;
		final String oldLabel = cat.getName();
		
		if (zLabel != null && !oldLabel.equals(zLabel)) {
			final String newLabel = zLabel;
			renameCategory(cat,newLabel);
			new UndoableAction(swat,false,""){
				private static final long serialVersionUID = 1L;
				@Override
				public void myRedo() {
					renameCategory(cat,newLabel);
					startEditing(new TreePath(findCategoryNode(newLabel).getPath()));
				}
				@Override
				public void myUndo() {
					renameCategory(cat,oldLabel);
					startEditing(new TreePath(findCategoryNode(oldLabel).getPath()));
				}
				@Override
				public String getRedoPresentationName() {
					return "rename category "+oldLabel;
				}
				@Override
				public String getUndoPresentationName() {
					return "rename category "+newLabel;
				}
			};
		}
	}
	private void renameCategory(Category cat,String zLabel) {
		// Alter all references to this verb label in all options
		for (Verb zVerb: dk.getVerbs()) 
			if (zVerb.getCategory().equalsIgnoreCase(cat.getName()))
				zVerb.setCategory(zLabel);
		renameNode(true,cat.getName(), zLabel);
		cat.setName(zLabel);
		validate();
	}
	
	private void renameSelectedVerb(String zLabel){
		final Verb verb = selectedVerb;
		final String oldLabel = verb.getLabel();
		if (zLabel==null || zLabel.equals(oldLabel)) return;
		
		final String newLabel = zLabel;
		renameVerb(verb,newLabel);
		new UndoableAction(swat,false,""){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				renameVerb(verb,newLabel);
				startEditing(new TreePath(findVerbNode(verb).getPath()));
			}
			@Override
			public void myUndo() {
				renameVerb(verb,oldLabel);
				startEditing(new TreePath(findVerbNode(verb).getPath()));
			}
			@Override
			public String getRedoPresentationName() {
				return "rename verb "+oldLabel;
			}
			@Override
			public String getUndoPresentationName() {
				return "rename verb "+newLabel;
			}
		};
	}
	
	private void renameVerb(Verb mVerb,String newLabel) {
		renameNode(false, mVerb.getLabel(), newLabel);
		mVerb.setLabel(newLabel);
		if (ve.getVerb()==mVerb)
			ve.reloadVerbName();
		ve.repaintScript();
		ve.repaintOptionCombobox();
		validate();
	}
	
	private void deleteSelectedCategory() {
		if (!selectedCat.hasAnyVerbs(dk)) {
			final Category cat = selectedCat;
			final int index = cat.getParent().getIndex(cat);
			new UndoableAction(swat,"delete category "+selectedCat.getName()){
				private static final long serialVersionUID = 1L;
				@Override
				public void myRedo() {
					deleteCategory(cat);
				}
				@Override
				public void myUndo() {
					addCategory(index,cat);					
				}
			};
		}
	}
	private void deleteCategory(Category mCat) {
			// remove the catgory from the category tree
			String catName = mCat.getName();
			mCat.getParent().removeChild(mCat);
			ve.reloadVerbInfo();
			validate();
			
			// remove the verb tree node
			deleteNode(catName);
		
	}
	private void insertCategory(final Category insertee, final Category target, final MutableTreeNode droppedNode, MutableTreeNode dropNode) {
		final VerbTreeNode oldParent = (VerbTreeNode)droppedNode.getParent();
		final int oldIndex = oldParent.getIndex(droppedNode);
		final VerbTreeNode newParent = (VerbTreeNode)dropNode.getParent();
		final int newIndex = newParent.getIndex(dropNode);
		final Category oldCatParent = insertee.getParent();
		final int oldCatIndex = oldCatParent.getIndex(target);
		new UndoableAction(swat,"move category "+insertee.getName()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				insertee.setParent(dk,target.getParent(),target.getParent().getIndex(target));
				treeModel.removeNodeFromParent(droppedNode);
				treeModel.insertNodeInto(droppedNode, newParent, newIndex);
				showAndSelect(droppedNode);
			}
			@Override
			public void myUndo() {
				insertee.setParent(dk,oldCatParent, oldCatIndex);
				treeModel.removeNodeFromParent(droppedNode);
				treeModel.insertNodeInto(droppedNode, oldParent, oldIndex);
				showAndSelect(droppedNode);
			}
		};
	}
	
	private void addSubcategory(final Category insertee, final Category target, final MutableTreeNode droppedNode, final MutableTreeNode dropNode) {
		final VerbTreeNode oldParent = (VerbTreeNode)droppedNode.getParent();
		final int oldIndex = oldParent.getIndex(droppedNode);
		final Category oldCatParent = insertee.getParent();
		final int oldCatIndex = oldCatParent.getIndex(target);
		new UndoableAction(swat,"move category "+insertee.getName()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				insertee.setParent(dk,target.getParent(),target.getParent().getIndex(target));
				treeModel.removeNodeFromParent(droppedNode);
				treeModel.insertNodeInto(droppedNode, dropNode, dropNode.getChildCount());
				showAndSelect(droppedNode);
			}
			@Override
			public void myUndo() {
				insertee.setParent(dk,oldCatParent, oldCatIndex);
				treeModel.removeNodeFromParent(droppedNode);
				treeModel.insertNodeInto(droppedNode, oldParent, oldIndex);
				showAndSelect(droppedNode);
			}
		};
	}

	private void showAndSelect(TreeNode n){
		TreePath p=new TreePath(((DefaultTreeModel)tree.getModel()).getPathToRoot(n));
		tree.setSelectionPath(p);
		tree.scrollPathToVisible(p);
	}


  class VerbDnDTree extends JTree implements DragSourceListener, DropTargetListener, DragGestureListener {
	  private static final long serialVersionUID = 1L;

		public VerbDnDTree(DefaultTreeModel treeModel) {
			super();
			setCellRenderer (new VerbCellRenderer());
			setModel(treeModel);
			dragSource = new DragSource();
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
			new DropTarget(this, this);
		}

		public void dragGestureRecognized(DragGestureEvent arg0) {
			Point clickPoint = arg0.getDragOrigin();
			TreePath path = getPathForLocation(clickPoint.x, clickPoint.y);
			if (path == null) {
				System.out.println("not on a node");
				
				return;
			}
			VerbTreeNode draggedNode = (VerbTreeNode)path.getLastPathComponent();
			if (!isInSystemCategory(draggedNode)){
				Transferable trans = new VerbTransferable(draggedNode);
				dragSource.startDrag(arg0, DragSource.DefaultMoveNoDrop, trans, this);
			}
		}

		public void dragDropEnd(DragSourceDropEvent arg0) {
			dropTargetNode= null;
		}

		public void dragEnter(DragSourceDragEvent arg0) {
		}

		public void dragExit(DragSourceEvent arg0) { 
		}

		public void dragOver(DragSourceDragEvent arg0) {
			boolean nodeIsCat = false;
			VerbTreeNode source=null;
			try {
				source = (VerbTreeNode)arg0.getDragSourceContext().getTransferable().getTransferData(localObjectFlavor);
				nodeIsCat = source.isCategory();
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
			 
			if (dropTargetNode == null) {
				if (nodeIsCat)
					arg0.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
				else
					arg0.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
			} else if (isInSystemCategory(dropTargetNode) || dropTargetNode.isVerb() && source.isCategory())
					arg0.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
				else
					arg0.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
		}

		public void dropActionChanged(DragSourceDragEvent arg0) {
		}

		public void dragEnter(DropTargetDragEvent arg0) {
			arg0.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		}

		public void dragExit(DropTargetEvent arg0) {
		}

		public void dragOver(DropTargetDragEvent arg0) {
			// figure out which cell it's over, dont' drag to self
			Point dragPoint = arg0.getLocation();
			TreePath path = getPathForLocation(dragPoint.x, dragPoint.y);
			if (path == null)
				dropTargetNode = null;
			else
				dropTargetNode = (VerbTreeNode)path.getLastPathComponent();
			
			//repaint();
			
		}

		// This gets called when an item in the verb tree gets dropped in a drag-and-drop operation
		public void drop(DropTargetDropEvent arg0) {
			String nodeName=null, targetName=null;
			VerbTreeNode dropNode = null, parent=null;

			Point dropPoint = arg0.getLocation();
			// int index = locationToIndex(dropPoint);
			TreePath path = getPathForLocation(dropPoint.x, dropPoint.y);

			boolean dropped = false;
			try {
				
				arg0.acceptDrop(DnDConstants.ACTION_MOVE);

				Object droppedObject = arg0.getTransferable().getTransferData(localObjectFlavor);

				// insert into spec'd path.  if dropped into a parent
				// make it last child of that parent
				final MutableTreeNode droppedNode = (MutableTreeNode)droppedObject;
				nodeName =((VerbTreeNode)droppedNode).toString();
				if (path != null) {
					dropNode = (VerbTreeNode)path.getLastPathComponent();
					if (isInSystemCategory(dropNode)){
						arg0.dropComplete(dropped);
						return;
					}
					parent = (VerbTreeNode)dropNode.getParent();
					targetName = dropNode.toString();
				}
				if (droppedNode==dropNode) {
					arg0.dropComplete(dropped);
					return;
				}

				// drop the nodes and rearrange the category and verb model
				if (dropNode == null ) {
					if (((VerbTreeNode)droppedNode).isCategory())  {
						final Category catObj = dk.categories.findChild(nodeName);
						final Category newParentObj = dk.categories;
						final Category oldParentObj = catObj.getParent();
						final int oldIndexObj = oldParentObj.getIndex(catObj); 
						
						final VerbTreeNode oldParent = (VerbTreeNode)droppedNode.getParent();
						final int oldIndex = oldParent.getIndex(droppedNode);
						new UndoableAction(swat,"move category "+catObj.getName()){
							private static final long serialVersionUID = 1L;
							@Override
							public void myRedo() {
								catObj.setParent(dk,newParentObj);
								((DefaultTreeModel)getModel()).removeNodeFromParent(droppedNode);
								((DefaultTreeModel)getModel()).insertNodeInto(droppedNode, root, root.getChildCount());
								showAndSelect(droppedNode);
							}
							@Override
							public void myUndo() {
								catObj.setParent(dk,oldParentObj,oldIndexObj);
								((DefaultTreeModel)getModel()).removeNodeFromParent(droppedNode);
								((DefaultTreeModel)getModel()).insertNodeInto(droppedNode, oldParent, oldIndex);
								showAndSelect(droppedNode);
							}
						};
						
						dropped = true;
					} else {
						// Attempted to drop a verb outside of a category
						System.out.println("Verbs need a category");
					}
					
				} else if (((VerbTreeNode)droppedNode).isCategory() && dropNode.isCategory() && dropNode.getCategory().hasNumVerbs(dk) == 0 && (dropNode != droppedNode) && (((VerbTreeNode)dropNode).isNodeDescendant((VerbTreeNode)droppedNode) == false))  {
					// Show a popup menu requesting whether to move the dropped category above, or into the old category
					selectedCategory = dk.categories.findChild(nodeName);
					targetCategory = dk.categories.findChild(targetName);
					treeDropNode = dropNode;
					treeDroppedNode = droppedNode;
					// Make the default action an insertion. We are not handling nested categories
					// for the time being.
					//categoryPopupMenu.show(this.getComponentAt(dropPoint), (int)(dropPoint.getX()), (int)(dropPoint.getY()));
					insertCategory(selectedCategory, targetCategory, treeDroppedNode, treeDropNode);
					
					dropped = true;
				} else if (((VerbTreeNode)droppedNode).isCategory() && dropNode.isCategory() && dropNode.getCategory().hasNumVerbs(dk) > 0 && (dropNode != droppedNode) && (((VerbTreeNode)dropNode).isNodeDescendant((VerbTreeNode)droppedNode) == false))  {
					final Category catObj = dk.categories.findChild(nodeName);
					final Category targetObj = dk.categories.findChild(targetName);
					final Category oldParentObj = catObj.getParent();
					final int oldIndexObj = oldParentObj.getIndex(catObj); 

					final VerbTreeNode oldParent = (VerbTreeNode)droppedNode.getParent();
					final int oldIndex = oldParent.getIndex(droppedNode);
					final String oldParentName = oldParent.isCategory()?oldParent.getCategory().getName():null;
					final VerbTreeNode newParent = (VerbTreeNode)dropNode.getParent();
					final int newIndex = newParent.getIndex(dropNode);
					new UndoableAction(swat,"move category "+catObj.getName()){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							VerbTreeNode droppedNode = findCategoryNode(catObj.getName());
							VerbTreeNode newParent = (VerbTreeNode)findCategoryNode(targetObj.getName()).getParent();
							catObj.setParent(dk,targetObj.getParent(), targetObj.getParent().getIndex(targetObj));
							((DefaultTreeModel)getModel()).removeNodeFromParent(droppedNode);
							((DefaultTreeModel)getModel()).insertNodeInto(droppedNode, newParent, newIndex);
							showAndSelect(droppedNode);
						}
						@Override
						public void myUndo() {
							VerbTreeNode droppedNode = findCategoryNode(catObj.getName());
							VerbTreeNode oldParent =(VerbTreeNode) (null==oldParentName?getModel().getRoot():findCategoryNode(oldParentName));

							catObj.setParent(dk,oldParentObj, oldIndexObj);
							((DefaultTreeModel)getModel()).removeNodeFromParent(droppedNode);
							((DefaultTreeModel)getModel()).insertNodeInto(droppedNode, oldParent, oldIndex);
							showAndSelect(droppedNode);
						}
					};

					dropped = true;
					
				} else if (((VerbTreeNode)droppedNode).isVerb() && dropNode.isVerb() && parent.isCategory() && parent.getCategory().getChildren().size() == 0) {
					final Verb verbObj = dk.getVerb(dk.findVerb(nodeName));
					final String oldCategoryName = verbObj.getCategory();					
					final Category newCategory = (dropNode.isVerb()?(VerbTreeNode)dropNode.getParent():dropNode).getCategory();
					
					final int verbIndex = dk.getIndexOf(dropNode.getVerb());
					final int oldVerbIndex =dk.getIndexOf(verbObj);

					final VerbTreeNode oldParent = (VerbTreeNode)droppedNode.getParent();
					final int oldIndex = oldParent.getIndex(droppedNode);
					final VerbTreeNode newParent = dropNode.isVerb()?(VerbTreeNode)dropNode.getParent():dropNode;
					final int newIndex = dropNode.isVerb()?newParent.getIndex(dropNode):dropNode.getChildCount();

					new UndoableAction(swat,"move verb "+nodeName){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							VerbTreeNode droppedNode = findVerbNode(verbObj);		
							verbObj.setCategory(newCategory.getName());
							VerbTreeNode newParent = findCategoryNode(newCategory.getName());

							((DefaultTreeModel)getModel()).removeNodeFromParent(droppedNode);
							((DefaultTreeModel)getModel()).insertNodeInto(droppedNode, newParent, newIndex);
							// Move the verb's underlying position, to maintain its position when saved.
							dk.moveVerb(oldVerbIndex, verbIndex); // move the verb in the array
							showAndSelect(droppedNode);
						}
						@Override
						public void myUndo() {
							VerbTreeNode droppedNode = findVerbNode(verbObj);		
							verbObj.setCategory(oldCategoryName);
							VerbTreeNode oldParent = findCategoryNode(oldCategoryName);

							((DefaultTreeModel)getModel()).removeNodeFromParent(droppedNode);
							((DefaultTreeModel)getModel()).insertNodeInto(droppedNode, oldParent, oldIndex);
							// Move the verb's underlying position, to maintain its position when saved.
							dk.moveVerb(verbIndex,oldVerbIndex); // move the verb in the array
							showAndSelect(droppedNode);
						}
					};
					dropped = true;

				} else if (((VerbTreeNode)droppedNode).isVerb() && dropNode.isCategory() && dropNode.getCategory().getChildren().size() == 0) {
					// Change the category structure
					final Verb verbObj = dk.getVerb(dk.findVerb(nodeName));
					final String oldCategoryName = verbObj.getCategory();
					final Category newCategory = (dropNode.isVerb()?(VerbTreeNode)dropNode.getParent():dropNode).getCategory();
					
					final int oldVerbIndex =dk.getIndexOf(verbObj);
					final int verbIndex = dk.getVerbCount()-1;
					
					final VerbTreeNode oldParent = (VerbTreeNode)droppedNode.getParent();
					final int oldIndex = oldParent.getIndex(droppedNode);
					final int newIndex = dropNode.getChildCount();

					new UndoableAction(swat,"move verb "+nodeName){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							VerbTreeNode droppedNode = findVerbNode(verbObj);
							verbObj.setCategory(newCategory.getName());
							VerbTreeNode newParent = findCategoryNode(newCategory.getName());

							((DefaultTreeModel)getModel()).removeNodeFromParent(droppedNode);
							((DefaultTreeModel)getModel()).insertNodeInto(droppedNode, newParent, newIndex);
							// Move the verb's underlying position, to maintain its position when saved.
							dk.moveVerb(oldVerbIndex, verbIndex); // move the verb in the array
							showAndSelect(droppedNode);
						}
						@Override
						public void myUndo() {
							VerbTreeNode droppedNode = findVerbNode(verbObj);		
							verbObj.setCategory(oldCategoryName);
							VerbTreeNode oldParent = findCategoryNode(oldCategoryName);

							((DefaultTreeModel)getModel()).removeNodeFromParent(droppedNode);
							((DefaultTreeModel)getModel()).insertNodeInto(droppedNode, oldParent, oldIndex);
							// Move the verb's underlying position, to maintain its position when saved.
							dk.moveVerb(verbIndex,oldVerbIndex); // move the verb in the array
							showAndSelect(droppedNode);
						}
					};
					dropped = true;

				} else
					System.out.println("Reject");
			} catch (Exception e) {
				e.printStackTrace();
			}
			arg0.dropComplete(dropped);
		}

		public void dropActionChanged(DropTargetDragEvent arg0) {
		}
		

		class VerbTransferable implements Transferable {
			Object object;
			
			public VerbTransferable(Object o) {
				object = o;
			}
		
			public Object getTransferData(DataFlavor arg0) throws UnsupportedFlavorException, IOException {
				if (isDataFlavorSupported(arg0))
					return object;
				else
					throw new UnsupportedFlavorException(arg0);
			}
		
			public DataFlavor[] getTransferDataFlavors() {
				return supportedFlavors;
			}
		
			public boolean isDataFlavorSupported(DataFlavor arg0) {
				return(arg0.equals(localObjectFlavor));
			}
			
		}
	}
  
//**********************************************************************
	class VerbCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 1L;
		private JLabel cellLabel;
		private Color backgroundSelectionColor;
		private Border focusBorder;
		private FontMetrics fm;
		
		public VerbCellRenderer() {
			cellLabel = new JLabel(" ");
			final Font f = cellLabel.getFont();
			cellLabel.setFont(new Font(f.getName(),Font.PLAIN,f.getSize()));
			cellLabel.setOpaque(true);
			cellLabel.setHorizontalAlignment(SwingConstants.CENTER);
			backgroundSelectionColor = super.getBackgroundSelectionColor();
			focusBorder=BorderFactory.createLineBorder(super.getBorderSelectionColor());
			fm = cellLabel.getFontMetrics(cellLabel.getFont());
		}
	
		public Component getTreeCellRendererComponent(JTree tree, Object value,
	 	 	      boolean selected, boolean expanded, boolean leaf, int row,
	 	 	      boolean hasFocus) {
			Component returnValue = null;

	 		if (value!=null && (value instanceof DefaultMutableTreeNode)) {
	 	 	      Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
	 	 	    if (userObject instanceof String) {

	 	 	    	boolean show=!texteditorpopup.isVisible() || editedPath.getLastPathComponent()!=value;
 	 	    		cellLabel.setText(show?(String)userObject:" ");
 	 	    		int w=fm.stringWidth((String)userObject);
 	 	    		cellLabel.setPreferredSize(new Dimension(w+8,fm.getHeight()));
 	 	    		
	 				if (show && selected) {
	 					cellLabel.setBackground(backgroundSelectionColor);
		 				if (tree.hasFocus()) cellLabel.setBorder(focusBorder);
		 				else cellLabel.setBorder(null);
	 				} else {
	 					cellLabel.setBackground(tree.getBackground());
	 					cellLabel.setBorder(null);
	 				}
	 				if (value instanceof VerbTreeNode && ((VerbTreeNode)value).isVerb())
	 					cellLabel.setToolTipText(Utils.breakStringHtml(Utils.nullifyIfEmpty(((VerbTreeNode)value).getVerb().getDescription())));
	 				else
	 					cellLabel.setToolTipText(null);
	 				
	 				returnValue = cellLabel;
	 	 	    }
	 	   }
	 	   if (returnValue == null) {
	 	       returnValue = super.getTreeCellRendererComponent(tree,
	 	    		   	value, selected, expanded, leaf, row, hasFocus);
	 	 }
	 	 return returnValue;
	 	 }
	}
  
	/** Class for exposing methods needed for testing of VerbTree. */
	public static class Test {
		
		public static void addNewVerb(VerbTree vt) { vt.addNewVerb(); }
		public static void deleteSelectedVerb(VerbTree vt) { vt.deleteSelectedVerb(); }

		public static void renameSelectedVerb(VerbTree vt,String newName) { 
			vt.renameSelectedVerb(newName); 
		}
		
		public static void duplicateSelectedVerb(VerbTree vt) { 
			vt.duplicateVerb(); 
		}

	}
}
