package com.storytron.swat;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.tree.TNode;
import com.storytron.swat.tree.Tree;
import com.storytron.swat.tree.TreeCellEditor;
import com.storytron.swat.tree.TreeCellRenderer;
import com.storytron.swat.util.FilterBox;
import com.storytron.swat.util.LightweightPopup;
import com.storytron.swat.verbeditor.ScriptEditor;
import com.storytron.uber.Deikto;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.CustomOperator;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;

//**********************************************************************		
public class DynamicTree extends Tree
                         implements Cloneable, AWTEventListener, KeyListener, WindowFocusListener {
	private static final long serialVersionUID = 1L;

	private ScriptEditor<?> scriptEditor;
	private Script myScript; // the only reason this exists is to provide an instance for a method call
	public Deikto dk;
	
    private LightweightPopup operatorPopup = new LightweightPopup();
    private ArrayList<Object> operatorList = new ArrayList<Object>(100);
    private FilterBox fb = new FilterBox(operatorList) {
		private static final long serialVersionUID = 0L;
		@Override
    	public String getItemToolTipText(Object o) {
    		if (o!=null) {
    			if (o instanceof OpValue)
    				return Utils.breakStringHtml(((OpValue)o).op.getMyToolTipText());
    			else
    				return Utils.breakStringHtml(((Operator)o).getMyToolTipText());
    		} else
    			return null;
    	}
		@Override
		protected int getDefaultSelectedIndex(JList list) {
			final ListModel l=list.getModel();
			for(int i=0;i<l.getSize();i++){
				if (l.getElementAt(i).toString().compareToIgnoreCase(fb.getFilterText())==0)
					return i;
			}
			return 0;
		}
    };
    
    public void showOperatorPopup(){
		if (getSelectedNode()!=null){
			Point p = new Point();
			getTopLeftCorner(getSelectedNode(),p);

			SwingUtilities.getWindowAncestor(this).addWindowFocusListener(this);
			Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.MOUSE_EVENT_MASK|AWTEvent.KEY_EVENT_MASK);

			fb.setListTextColor(Operator.getColor(((Node)getSelectedNode()).getOperator().getDataType()));
			fb.clearText();
			fb.refresh();
			Dimension popupSize = fb.getPreferredSize();
			operatorPopup.setSize(popupSize);
	    	operatorPopup.showPopup(this, 
	    			new int[]{
	    				p.x+((ScriptTreeCellRenderer)getCellRenderer()).getTokenLabelWidth(getSelectedNode())+30
	    				,p.x+20
	    				,p.x+20
	    				}
	    			, new int[]{ 
	    				p.y
	    				, p.y+((ScriptTreeCellRenderer)getCellRenderer()).getHeight(getSelectedNode())
	    				, p.y-popupSize.height
	    				}
	    			);
	    	SwingUtilities.invokeLater(new Runnable(){
	    		public void run() {
	    			fb.requestFocusInWindow();
	    		}
	    	});
		}
    }
    /** Hides the filter popup. */
    private void hideOperatorPopup(){
    	if (!operatorPopup.isVisible())
    		return;
    	
    	Toolkit.getDefaultToolkit().removeAWTEventListener(this);
    	SwingUtilities.getWindowAncestor(this).removeWindowFocusListener(this);
    	operatorPopup.hidePopup();
    	requestFocusInWindow();
    }
	public void windowGainedFocus(WindowEvent e) {}
	/** When the window looses focus we want the popup to be hidden. */
	public void windowLostFocus(WindowEvent e) { hideOperatorPopup(); }

	/** A class implementing some of the entries of the operator popup. */
	private static final class OpValue {
		public Operator op;
		public Object value;
		public OpValue(Operator op,Object value){
			this.op = op;
			this.value = value;
		}
		@Override
		public String toString(){
			return value.toString();
		}
	}
	
    /** 
     * Adds an operator to the list of valid operators.
     * The parameter {@code value} is used when the operator is a constant.  
     * */
    public void addOperatorToValidList(Operator op,Object value){
    	if (value!=null)
    		operatorList.add(new OpValue(op,value));
    	else
    		operatorList.add(op);
    }

    private static Comparator<Object> OPCOMP = new Comparator<Object>(){
		public int compare(Object o1, Object o2) {
			return o1.toString().compareToIgnoreCase(o2.toString());
		}
	}; 
    public void sortValidOperatorList(){
		Collections.sort(operatorList,OPCOMP);
    }
    public void clearValidOperatorList(){
		operatorList.clear();
    }
    
	public DynamicTree(ScriptEditor<?> tse, Deikto dk) {
			
		scriptEditor = tse;
		this.dk = dk;
		myScript = null;
		setRootVisible(false);

		operatorPopup.setContents(fb);
		fb.setRefreshSelectionType(FilterBox.REFRESH_TO_FISRT);
		fb.addKeyListener(this);
		fb.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (fb.getSelected() instanceof Operator)
					scriptEditor.processTokenMenu((Operator)fb.getSelected(), 
							Swat.isControlDown(),Swat.isAltDown(),null);
				else if (fb.getSelected()!=null)
					scriptEditor.processTokenMenu(((OpValue)fb.getSelected()).op,
							((OpValue)fb.getSelected()).value,
							Swat.isControlDown(),
							Swat.isAltDown());
				else switch (getSelectedNode().getOperator().getDataType()) {
				case BNumber:
				case Number:
				case Text:
					scriptEditor.processTokenMenu(DynamicTree.this.dk.getOperatorDictionary().getOperator(getSelectedNode().getOperator().getDataType().name()+"Constant"),
							Swat.isControlDown(),Swat.isAltDown(),fb.getFilterText());
				}
				if (((Node)getSelectedNode()).getOperator().getLabel().endsWith("?"))
					showOperatorPopup();
				else {
					hideOperatorPopup();
					requestFocusInWindow();
				}
			}
		});
		addMouseListener(new MouseListener(){
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger() && !operatorPopup.isVisible())
					showOperatorPopup();
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (getSelectedNode()==null) return;
					if (!operatorPopup.isVisible())
						showOperatorPopup();
				} else if ((e.getModifiers() & Swat.keyMask)!=0) {
					if (getSelectedNode()==null) return;
					Operator op = ((Node)getSelectedNode()).getOperator();
					if (op instanceof CustomOperator)
						scriptEditor.swat.showScript(new ScriptPath(null,null,null),((CustomOperator)op).getBody());
				}
			}

		});
		addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				switch(e.getKeyCode()){
				case KeyEvent.VK_DELETE:
					if (getSelectedNode()!=null){
						scriptEditor.deleteSelection();
						e.consume();
					}
					break;
				case KeyEvent.VK_ENTER:
					if (getSelectedNode()!=null){
						if (Swat.isControlDown()) {
							if (getSelectedNode()==null) return;
							Operator op = ((Node)getSelectedNode()).getOperator();
							if (op instanceof CustomOperator)
								scriptEditor.swat.showScript(new ScriptPath(null,null,null),((CustomOperator)op).getBody());
						} else {
							startNodeEditing(getSelectedNode());
							e.consume();
						}
					}
					break;
				case KeyEvent.VK_SPACE:
					showOperatorPopup();
					e.consume();
					break;
				default:;
				}
			}
			public void keyReleased(KeyEvent e) {}
			public void keyTyped(KeyEvent e) {}
		});

		setBackground(Color.white);			
		validate();			
	}

	@Override
	public Node getSelectedNode(){
		return (Node)super.getSelectedNode();
	}
	
	/**
	 * When other keys than characters and editor shortcuts are pressed
	 * we want the popups to close.
	 * */
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()){
		case KeyEvent.VK_ESCAPE:
		case KeyEvent.VK_TAB:
		case KeyEvent.VK_ENTER:
			((OperatorTreeCellEditor)getCellEditor()).stopEditing();
			e.consume();
			break;
		case KeyEvent.VK_CONTROL:		
		case KeyEvent.VK_ALT_GRAPH:
		case KeyEvent.VK_ALT:
			break;
		default:
			if (e.getModifiersEx()!=KeyEvent.SHIFT_DOWN_MASK && e.getModifiersEx()!=0) {				
				((OperatorTreeCellEditor)getCellEditor()).stopEditing();
				hideOperatorPopup();
			}
		}
	}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}
	
	// Added the following methods to show a popup that shows an error message
	// when there is one in the script being displayed.
	private JLabel errorLabel = new JLabel();
	private JPanel errorPanel=null;
	public void showError(String error){ showError(error,getSelectedNode());}
	public void showError(String error,TNode n){
		if (errorPanel==null){
			errorPanel=new JPanel(new FlowLayout(FlowLayout.CENTER,2,2));
			errorPanel.add(errorLabel);
			errorPanel.setBorder(BorderFactory.createLineBorder(Color.black));
			errorPanel.setBackground(new Color(255,255,225));	
		}
		errorLabel.setText(Utils.toHtmlTooltipFormat(error));
		showPopup(n);
	}
	javax.swing.Popup popupList=null;
	private void showPopup(TNode n) {
		if (popupList!=null) return;
		
		Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.MOUSE_EVENT_MASK|AWTEvent.KEY_EVENT_MASK);
				
		Point p = getLocationOnScreen();
		Tree.NodeEntry ne=getNodeEntry(n);
		p.x+=ne.x;
		p.y+=ne.y-errorPanel.getPreferredSize().height;
		popupList=PopupFactory.getSharedInstance().getPopup(this, errorPanel, p.x,p.y);
		popupList.show();
	} 
	private void hidePopup() {
		if (popupList==null) return;
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
		popupList.hide();
		popupList=null;		
	}
	
	public void eventDispatched(AWTEvent event) {
		if (event.getID()==MouseEvent.MOUSE_PRESSED || event.getID()==KeyEvent.KEY_PRESSED) {
			hidePopup();
			if ((event instanceof KeyEvent && ((KeyEvent)event).getKeyCode()==KeyEvent.VK_ESCAPE)
				|| !(event.getSource() instanceof Component)
				|| !SwingUtilities.isDescendingFrom((Component)event.getSource(), fb))
				hideOperatorPopup();
		}
	}
	//**********************************************************************		
 	public Script getScript() {	return myScript;	}
// 	**********************************************************************
 	public void setScript(Script tScript) {
 		myScript = tScript;
 		super.setRoot(myScript!=null?myScript.getRoot():null);
 	}
// 	**********************************************************************
 	@Override
 	public void setRoot(TNode root) {
 		super.setRoot(root); 		
 		myScript.setRoot((Node)root);
 	} 	
//**********************************************************************		
 	public Script deleteNode(Script.Node selectedNode) {
 		Script.Node child=myScript.deleteNode(dk,selectedNode); 		
		scriptEditor.setSelection(child, getRoot());
		revalidate();
		setSelectedNode(child);
		return myScript;
 	}
//**********************************************************************		
 	public Script replaceWithNode(Script.Node tMyDMTN) {
 		Script.Node selectedNode = (Script.Node)getSelectedNode();

		if (tMyDMTN.getDescription().length()==0) 
			tMyDMTN.setDescription(selectedNode.getDescription());		
 		Script.Node parent = (Script.Node)selectedNode.getParent();
		int index = parent.getIndex(selectedNode);
		parent.remove(index);
  		parent.insert(tMyDMTN, index);
		revalidate();
 		setSelectedNode(tMyDMTN);
		scriptEditor.setSelection(tMyDMTN, getRoot());
		return(myScript);
	}
//**********************************************************************		
 	public boolean replace(Operator newOperator, Object newConstant, boolean fControlKey, boolean fAltKey) {
 		Script.Node childNode = Script.createNode(newOperator, newConstant);
		Script.Node selectedNode = (Script.Node)getSelectedNode();
    
		Operator.Type originalDataType = selectedNode.getOperator().getDataType();
		Node parentNode = (Node)selectedNode.getParent();
		int childIndex = parentNode.getIndex(selectedNode);
		boolean changed=false;

 		if (fControlKey) { // this is an outsertion
 			// First we must determine if the outsertion is legal
 			// Does the data type of the original operator match at least one
 			//   of the arguments of the outserted operator?
			int i = 0;
 			boolean dataTypeMatch = false;
 			while (!dataTypeMatch && i<newOperator.getCArguments()) {
				if (originalDataType==newOperator.getArgumentDataType(i))
					dataTypeMatch = true;
				else 
					++i;
 			}
 			if (dataTypeMatch) {
 				parentNode.remove(childIndex);
 				parentNode.insert(childNode, childIndex); 
	 			for (int j = 0; j < newOperator.getCArguments(); ++j) {
	 				if (j == i) {
	 					// this is the special case where the original operator is inserted
	 					childNode.add(selectedNode);
	 				} else {
	 					// this is the normal case for other operators
		 				String argLabel = "?"+newOperator.getArgumentDataType(j)+"?";
		 				Script.Node subNode = Script.createNode(OperatorDictionary.getUndefinedOperator(argLabel), "0");
		 		    	childNode.add(subNode);
	 				}
	 			}	
	 			changed=true;
 			}
		}
 		else if (fAltKey) {// this is a substitution
 			int oldCount = selectedNode.getOperator().getCArguments();
 			int newCount = newOperator.getCArguments();
 			boolean fMatch = true;
			// Now we match the old operator's argument list against the new operator's argument list.
			// We insist that the order of arguments be preserved. 
			fMatch = true;
			for (int k=0; (k<Math.min(oldCount,newCount)); ++k) // examine each of the old operator's arguments
 				if (selectedNode.getOperator().getArgumentDataType(k)!=newOperator.getArgumentDataType(k)){
 					fMatch = false;
 					break;
 				}
 			
 			if (fMatch) { // OK, their arguments match, so we can substitute the new one
 				// Remove remaining arguments
 				childNode.setDescription(selectedNode.getDescription());
 				selectedNode.setOperatorValue(childNode.getOperator(),childNode.getConstant());
 				while (oldCount>newCount)
 					selectedNode.remove(--oldCount);
 				// Now we have to insert any additional undefined arguments
 				while (oldCount<newCount) {
 	 				String argLabel = newOperator.getArgumentDefaultValue(oldCount);
 	 				if (argLabel==null || argLabel.equals(""))
 		 				argLabel = "?"+newOperator.getArgumentDataType(oldCount)+"?";
 	 				Script.Node subNode = Script.createNode(dk.getOperatorDictionary().getOperator(argLabel), "0");
 	 				selectedNode.insert(subNode, oldCount);
 					++oldCount;
 				}
 	 			Script.Node temp=(Script.Node)selectedNode.getNextNode();
 	 			if (temp!=null)
 	 				selectedNode=temp;
 				changed=true;
 			}
 		} else { // this is a straightforward replacement
 			childNode.setDescription(selectedNode.getDescription());
 			selectedNode.setOperatorValue(childNode.getOperator(),childNode.getConstant());
			selectedNode.removeAllChildren();
 			for (int i = 0; i < newOperator.getCArguments(); ++i) {
 				String argLabel = newOperator.getArgumentDefaultValue(i);
 				if (argLabel==null || argLabel.equals(""))
	 				argLabel = "?"+newOperator.getArgumentDataType(i)+"?";
 				Script.Node subNode = Script.createNode(dk.getOperatorDictionary().getOperator(argLabel), 0.0f);
 				selectedNode.insert(subNode, i);
			}
 			Script.Node temp=(Script.Node)selectedNode.getNextNode();
 			if (temp!=null)
 				selectedNode=temp;
			changed=true;
 		}

 		if (changed) {
 			setSelectedNode(selectedNode);
 			revalidate();
 		}
		return changed;
 	}
//**********************************************************************		
 	public Script.Node getRootNode() {
 		return (Script.Node)getRoot();
 	}
//**********************************************************************		
 	public Script getMyScript() {
 		return myScript;
 	}

 	/**
 	 * This method tells if the node would show both the token label and its
 	 * description label if it had a description. 
 	 * */
 	private boolean fullDisplaying(TNode n){
 		return n.isExpanded() || n.isLeaf();
 	}
 	
 	/** Selects a node and tells scriptEditor to update its menus. */
 	@Override
 	public void setSelectedNode(TNode selectedNode) {
 		super.setSelectedNode((Script.Node)selectedNode);
 		scriptEditor.setSelection(getSelectedNode(), getRoot());
 	}
 	/** Selects a node without notifying scriptEditor. */
	public void superSetSelectedNode(TNode selectedNode) {
 		super.setSelectedNode((Script.Node)selectedNode);
 	}
	
 	/**
 	 * This method decides if a click selects a node or edits
 	 * a label.
 	 */
 	@Override
 	public void nodeClicked(MouseEvent e,TNode n,int x,int y,int w,int h){
 		if (e.getClickCount()<2 && 
 				(((Script.Node)n).getDescription().length()==0 || 
 						fullDisplaying(n) && y+h/2<e.getY() && e.getY()<y+h)){
			if (getSelectedNode()!=n) {
				setSelectedNode(n);
				repaint();
			}
 		} else if (e.getButton()==MouseEvent.BUTTON1)				
 			getCellEditor().startEditing(n);
 	}

 	@Override
 	public TreeCellRenderer createTreeCellRenderer(){
 		return new ScriptTreeCellRenderer(this);
 	}
 	@Override
 	public TreeCellEditor createTreeCellEditor(){
 		return new OperatorTreeCellEditor(this);
 	}

 	/**
 	 * Gets the tooltip for the node under the cursor.
 	 * It gets the operator descriptor if the mouse is over the
 	 * token label, or the node description label if the mouse
 	 * is over it. 
 	 * */
 	@Override
 	public String getToolTipText(MouseEvent e){
 		NodeEntry n=getNodeEntry(e.getX(),e.getY());
 		if (n!=null){
 			Script.Node t=(Script.Node)n.n;			
 			if (t.getDescription().length()!=0 && (!fullDisplaying(n.n) ||
 					n.y<e.getY() && e.getY()<n.y+n.h/2))
 				return Utils.breakStringHtml(t.getDescription());
 			else return Utils.breakStringHtml(t.getOperator().getToolTipText());
 		} else return null;
 	}
 	
 	/**
 	 * This renderer implements the look of the script tree nodes.
 	 * It handles colour, sizes, layout of the node label and the 
 	 * popup editor for the node label.   
 	 * */
 	public static final class OperatorTreeCellEditor implements WindowFocusListener, AWTEventListener,
 													TreeCellEditor, FocusListener {		
 		private static final long serialVersionUID = 1L;
 		private JTextArea descriptionField = new JTextArea();
 		private DynamicTree tree;
 		private Insets insets=new Insets(3,3,3,3);
 		private LightweightPopup textpopup=new LightweightPopup();

 		public OperatorTreeCellEditor(DynamicTree tree) {
 			// Here we initialize the label for showing operators
 			// the label for showing the node labels (if any)
 			// and the field for editing the node labels.
 			this.tree=tree;
 			descriptionField.setFocusable(true);
 			descriptionField.setWrapStyleWord(true);
 			descriptionField.setLineWrap(true);
 			((AbstractDocument)descriptionField.getDocument()).setDocumentFilter(new MyDocumentFilter());
 			descriptionField.setSize(250,20);
 			descriptionField.setMinimumSize(descriptionField.getSize());
 			descriptionField.addKeyListener(tree);
 			descriptionField.setBorder(BorderFactory.createCompoundBorder(
 					BorderFactory.createLineBorder(Color.black),
 					BorderFactory.createEmptyBorder(1,insets.left,0,insets.right)));
 			descriptionField.addFocusListener(this);
 			textpopup.setContents(descriptionField);
 		}
 		/** These are methods from the {@link FocusListener} interface. */
 		public void focusGained(FocusEvent e) {}
 		/**
 		 * When the textbox looses focus unexpectedly we want it to
 		 * recover it inmediatly (if it did not become hidden).
 		 * */
 		public void focusLost(FocusEvent e) { descriptionField.requestFocusInWindow();  }

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
 				textpopup.setSize(250,descriptionField.getPreferredSize().height);
 			}

 			@Override
 			public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
 				super.remove(fb, offset, length);
 				textpopup.setSize(250,descriptionField.getPreferredSize().height);
 			}

 			@Override
 			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
 				super.replace(fb, offset, length, text, attrs);
 				textpopup.setSize(250,descriptionField.getPreferredSize().height);
 			}

 		}

 		/**
 		 * We want to hide node labels in visiscript.  
 		 * */
 		private boolean showDescriptions=true;
 		public void setShowDescriptions(boolean showDescriptions) {
 			this.showDescriptions = showDescriptions;
 		}
 		public boolean getShowDescriptions() {
 			return showDescriptions;
 		}

 		private Script.Node editedNode=null;
 		/**
 		 * Shows the editor box popup.
 		 * */
 		public void startEditing(TNode n){
 			editedNode=(Script.Node)n;
 			tree.scrollNodeToVisible(n);
 			descriptionField.setText(editedNode.getDescription().trim());
 			descriptionField.selectAll();
 			Point p=new Point();
 			tree.getTopLeftCorner(n,p);
 			showDescriptionPopup(p.x+20,p.y);
 			tree.setSelectedNode(n);
 			tree.repaint();
 		}
 		/**
 		 * Hides the editor box popup and saves the text into the node.
 		 * */
 		public void stopEditing(){
 			if (!textpopup.isVisible())
 				return;

 			hideDescriptionPopup();
 			tree.requestFocusInWindow();
 			tree.scriptEditor.setNodeDescription(editedNode,descriptionField.getText().trim());
 		}

 		/**
 		 * Shows the editor box popup.
 		 * */
 		private void showDescriptionPopup(int x,int y) {
 			if (textpopup.getParent()!=null) return;

 			SwingUtilities.getWindowAncestor(tree).addWindowFocusListener(this);
 			Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.MOUSE_EVENT_MASK);

 			String desc=editedNode.getDescription();
 			if (desc.length()==0)
 				editedNode.setDescription(" ");
 			textpopup.setSize(new Dimension(250,descriptionField.getPreferredSize().height));
 			textpopup.showPopup(tree,x,y);
 			SwingUtilities.invokeLater(new Runnable(){
 				public void run(){ descriptionField.requestFocusInWindow();}
 			});
 		} 
 		/**
 		 * Hides the editor box popup.
 		 * */
 		private void hideDescriptionPopup() {
 			hideDescriptionPopup(true);
 		}

 		/**
 		 * Hides the editor box popup.
 		 * The argument tell if the tree should be repainted
 		 * after closing the popup. 
 		 * */
 		private void hideDescriptionPopup(boolean repaint) {
 			if (!textpopup.isVisible()) return;

 			SwingUtilities.getWindowAncestor(tree).removeWindowFocusListener(this);
 			textpopup.hidePopup();
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
 		public void windowLostFocus(WindowEvent e) { stopEditing(); }

 		/**
 		 * This event dispatcher gets called whenever the user clicks
 		 * somewhere. If the user clicks outside the popup we want the
 		 * popup to hide. This method is from the interface 
 		 * {@link AWTEventListener}.
 		 * */
 		public void eventDispatched(AWTEvent e){			
 			MouseEvent me = (MouseEvent)e;
 			if (me.getID()!=MouseEvent.MOUSE_PRESSED || textpopup==null) return;
 			if (me.getComponent()==null) {
 				stopEditing();
 				return;
 			}
 			Point p = me.getPoint();
 			SwingUtilities.convertPointToScreen(p,me.getComponent());
 			Point sp=descriptionField.getLocationOnScreen();		
 			if (!descriptionField.contains(p.x-sp.x,p.y-sp.y))
 				stopEditing();			
 		}
 	}
}
