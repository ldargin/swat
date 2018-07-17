package com.storytron.swat;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.AddButton;
import com.storytron.swat.util.DeleteButton;
import com.storytron.swat.util.EditorListener;
import com.storytron.swat.util.ErrorPopup;
import com.storytron.swat.util.LightweightPopup;
import com.storytron.swat.util.MaxLengthDocument;
import com.storytron.swat.util.UndoableAction;
import com.storytron.uber.Deikto;
import com.storytron.uber.Script;
import com.storytron.uber.TextTrait;
import com.storytron.uber.Word;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.Operator;

/**
 * <p>This class implements an editor for custom text traits.
 * It handles a panel of labeled text fields and a button.
 * </p>
 * <p> Each textfield is used to edit a custom text trait value.
 * The labels of the text fields can be edited to change the
 * name of the traits. Next to the label there is also
 * a button to delete the corresponding custom trait, and another button to 
 * edit the trait description. 
 * The panel containing all the text fields
 * can be retrieved with {@link #getTextFieldsPanel()}.
 * </p>
 * <p>
 * The button is used to add new custom traits. This button can be 
 * retrieved using {@link #getAddButton()}.
 * </p>
 * <p>
 * In order to use this class you need to provide the implementation 
 * of the abstract methods, which provides information about the
 * concept owning the traits (Actor, Stage or Prop). 
 * </p>
 * */
public abstract class CustomTextTraitsControl<Entity extends Word> {
	private Box textFieldsPanel = new SlidersPanel();
	private JButton addCustomTraitButton=new AddButton("text trait");
	private final LightweightPopup descriptionPopup = new LightweightPopup();
	private final JTextArea descriptionField = new JTextArea();
	private Font textFont;

	private Deikto.TraitType tt;
	private Swat swat;

	/** Height used for each trait. */
	public static int TRAIT_HEIGHT = 57;
	
	/** Gets the entity currently being edited. 
	 * Used when setting slider values. 
	 */
	protected abstract Entity getEditedEntity();
	/** Sets the entity currently being edited in the editor. 
	 * Used when setting slider values. 
	 */
	protected abstract void showEditedEntity(Entity e);
	/** Gets the value of a trait for a given entity. */
	protected abstract String getValue(Entity e,TextTrait t);
	/** Sets the value of a trait for a given entity. */
	protected abstract void setValue(Entity e,TextTrait t,String value);
	/** Called when the traits are renamed added or deleted. */
	protected void onTraitChange(){};
	
	/** Updates the text fields to show the trait values of the entity being edited. */
	public void refresh(){
		int i=0;
		Entity editedEntity=getEditedEntity();
		for(TextTrait t:swat.dk.getTextTraits(tt)){
			((Swat.TextField)((JComponent)textFieldsPanel.getComponent(i)).getComponent(1)).setText(getValue(editedEntity,t));
			i++;
		}
	}
	
	/**
	 * Constructs a CustomTraitsControl for editing traits of the given
	 * trait type. 
	 * */
	public CustomTextTraitsControl(Swat sw,Deikto.TraitType traitType){
		tt=traitType;
		swat=sw;

		textFont = new Font(descriptionField.getFont().getName(),Font.BOLD,descriptionField.getFont().getSize());
		
		// prepare de description popup for custom traits
		descriptionField.setFocusable(true);
		descriptionField.setWrapStyleWord(true);
		descriptionField.setLineWrap(true);
		((AbstractDocument)descriptionField.getDocument()).setDocumentFilter(new MyDocumentFilter());
		descriptionField.setSize(250,20);
		descriptionField.setMinimumSize(descriptionField.getSize());
		descriptionField.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.black),
				BorderFactory.createEmptyBorder(1,2,0,2)));
		descriptionPopup.setContents(descriptionField);

		// prepare de add button
		addCustomTraitButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int i=0;
				while(swat.dk.nameExists(i==0?"new "+tt.name()+" text trait":"new "+tt.name()+" text trait"+i)!=null) i++;
				final String newv=i==0?"new "+tt.name()+" text trait":"new "+tt.name()+" text trait"+i;
				final int idx = 0;
				try {
					swat.dk.createTextTrait(tt,idx,newv,null);
				} catch (LimitException ex){ throw new RuntimeException(ex); }
				final TextTrait trait = swat.dk.getTextTrait(tt, newv);
				final JComponent box=createBoxFor(trait);
				textFieldsPanel.add(box,idx);
				((JComponent)box.getParent()).scrollRectToVisible(box.getBounds());
				((JComponent)box.getComponent(0)).getComponent(1).requestFocusInWindow();
				((JTextComponent)((JComponent)box.getComponent(0)).getComponent(1)).selectAll();
				checkAddCustomTraitButton();
				onTraitChange();
				textFieldsPanel.revalidate();

				new UndoableAction(swat,false,"add trait "+newv){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						try {
							swat.dk.addTextTrait(tt,idx,trait);					
						} catch (LimitException ex){ throw new RuntimeException(ex); }
						textFieldsPanel.add(box,idx);
						((JComponent)box.getParent()).scrollRectToVisible(box.getBounds());
						((JComponent)box.getComponent(0)).getComponent(1).requestFocusInWindow();
						((JTextComponent)((JComponent)box.getComponent(0)).getComponent(1)).selectAll();
						checkAddCustomTraitButton();
						onTraitChange();
						textFieldsPanel.revalidate();
					}
					@Override
					public void myUndo() {
						textFieldsPanel.remove(box);
						swat.dk.removeTextTrait(tt,newv);
						checkAddCustomTraitButton();
						onTraitChange();
						textFieldsPanel.revalidate();
					}
				};
			}
		});
	}

	private void checkAddCustomTraitButton() {
		addCustomTraitButton.setEnabled(swat.dk.getTextTraitCount(tt)<swat.dk.limits.maximumTextTraitCount);
		if (!addCustomTraitButton.isEnabled())
			addCustomTraitButton.setToolTipText("Cannot have more than "+swat.dk.limits.maximumTextTraitCount+" traits.");
		else
			addCustomTraitButton.setToolTipText("creates a new trait");
	}

	public void init(Deikto dk){
		for (TextTrait t:dk.getTextTraits(tt)){
			JComponent box=createBoxFor(t);
			textFieldsPanel.add(box);
		}
		textFieldsPanel.add(Box.createVerticalGlue());
		checkAddCustomTraitButton();
	}
	
	/** Gets the button for adding traits. */
	public JButton getAddButton(){ return addCustomTraitButton; }
	/** Gets the panel containing the trait sliders. */
	public JComponent getTextFieldsPanel(){ return textFieldsPanel; }

	private ErrorPopup errorPopup=new ErrorPopup();
	
	// Helper classes to store the world state before
	// deleting trait operators.
	/**
	 * Stores for a GET operator being deleted, the node
	 * having the operator, its parent and its index as
	 * child.  
	 * */
	private static final class NodePos {
		public int i;
		public Node parent, n;
		public NodePos(Node parent,int i,Node n){
			this.parent=parent;
			this.i=i;
			this.n=n;
		};
	}

	/** 
	 * Creates a box containing a text field, its label, a button to 
	 * delete it and a button to edit its description.
	 */
	private JComponent createBoxFor(final TextTrait t){
		final JPanel box = new JPanel();
		final Swat.TextField floatLabel = new Swat.TextField();
		floatLabel.setDocument(new MaxLengthDocument(Deikto.MAXIMUM_FIELD_LENGTH));
		floatLabel.setText(t.getLabel());
		floatLabel.addActionListener(new EditorListener(floatLabel){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean timedActionPerformed(ActionEvent e) {
				return renameTrait(t,box,floatLabel.getText().trim());
			}
			@Override
			public String getText() { return t.getLabel(); }
		});
		floatLabel.setBackground(Utils.lightlightBackground);
		//floatLabel.setPreferredSize(new Dimension(23,23));
		
		final Swat.TextField textField = new Swat.TextField();
		textField.setDocument(new MaxLengthDocument(Deikto.MAXIMUM_TEXT_TRAIT_LENGTH));
		textField.setFont(textFont);
		textField.setForeground(Operator.getColor(Operator.Type.Text));
		textField.addActionListener(new EditorListener(textField){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean timedActionPerformed(ActionEvent e) {
				setTraitValue(textField,t,textField.getText().trim());
				return true;
			}
			@Override
			public String getText() {
				return getValue(getEditedEntity(),t);
			}
		});
		
		JButton deleteCustomTraitButton=new DeleteButton();
		deleteCustomTraitButton.setToolTipText(Utils.toHtmlTooltipFormat("Deletes this trait."));
		deleteCustomTraitButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				deleteTrait(box,t);
			}
		});
		Dimension db=deleteCustomTraitButton.getPreferredSize();
		db.height=floatLabel.getPreferredSize().height;
		//deleteCustomTraitButton.setPreferredSize(db);
	
		final QuestionMarkButton descriptionButton = new QuestionMarkButton(t.getLabel());
		descriptionButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				showDescriptionPopup(t,new DescriptionPopupDispatcher(t));
			}
		});
		
		JComponent buttonPanel = Box.createHorizontalBox();
		buttonPanel.add(descriptionButton);
		buttonPanel.add(deleteCustomTraitButton);
		
		JComponent aux=new JPanel(new BorderLayout());
		aux.add(buttonPanel,BorderLayout.EAST);
		aux.add(floatLabel,BorderLayout.CENTER);
		//aux.setMaximumSize(new Dimension(210,25));
		//aux.setPreferredSize(new Dimension(210,aux.getPreferredSize().height));
		
		aux.setAlignmentX(0.5f);			
		box.add(aux);
		textField.setAlignmentX(0.5f);
		box.add(textField);		
		
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setOpaque(false);
		box.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		box.setAlignmentX(0.5f);
		box.setMinimumSize(new Dimension(10,TRAIT_HEIGHT));
		//box.setPreferredSize(new Dimension(212,TRAIT_HEIGHT));
		box.setMaximumSize(new Dimension(Integer.MAX_VALUE,TRAIT_HEIGHT));
		TRAIT_HEIGHT = box.getPreferredSize().height;
		return box;
	}
	
	private void setTraitValue(final Swat.TextField textField,final TextTrait t,final String newValue){
		final String oldValue = getValue(getEditedEntity(),t);
		if (oldValue==null && newValue.length()==0 || newValue.equals(oldValue))
			return;
		
		final Entity entity = getEditedEntity();
		setValue(entity, t, newValue);
		
		new UndoableAction(swat,false,"change value of "+t.getLabel()+" for "+entity.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				setValue(entity, t, newValue);
				showEditedEntity(entity);
				((JComponent)textField.getParent().getParent()).scrollRectToVisible(textField.getParent().getBounds());
				textField.setText(newValue);
				textField.selectAll();
			}
			@Override
			protected void myUndo() {
				setValue(entity, t, oldValue);
				showEditedEntity(entity);
				((JComponent)textField.getParent().getParent()).scrollRectToVisible(textField.getParent().getBounds());
				textField.setText(oldValue);
				textField.selectAll();
			}
		};
	}
	
	private void deleteTrait(final JComponent box,final TextTrait t){
		// collect script nodes where trait is used
		final LinkedList<NodePos> nl=new LinkedList<NodePos>();
		swat.dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Node n) {
				if (t.getLabel().equals(n.getOperator().getLabel()))
					nl.add(new NodePos((Node)n.getParent(),n.getParent().getIndex(n),n));
				return true;
			}
		});
		
		final int i=swat.dk.getTextTraitIndex(tt,t.getLabel());
		// Find the box index.
		int boxi=0;
		while(boxi<textFieldsPanel.getComponentCount() && textFieldsPanel.getComponent(boxi)!=box)
			boxi++;
		final int boxIndex = boxi;
		
		new UndoableAction(swat,"delete trait "+t.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				swat.dk.removeTextTrait(tt,t);
				textFieldsPanel.remove(box);
				textFieldsPanel.revalidate();
				textFieldsPanel.repaint();
				checkAddCustomTraitButton();
				onTraitChange();
			}
			@Override
			public void myUndo() {
				try {
					swat.dk.addTextTrait(tt,i,t);
				} catch(LimitException ex) { throw new RuntimeException(ex); };
				for(NodePos np:nl){
					np.parent.remove(np.i);
					np.parent.insert(np.n,np.i);
				}
				textFieldsPanel.add(box,boxIndex);
				((JComponent)box.getParent()).scrollRectToVisible(box.getBounds());
				checkAddCustomTraitButton();
				onTraitChange();
			}
		};
	}

	private boolean renameTrait(final TextTrait t,final JComponent box,final String newv){
		// trait is being renamed
		final String oldv=t.getLabel();
		if (newv.equals(oldv)) return true;
		
		String s=swat.dk.nameExists(newv);
		if (s!=null) {
			if (SwingUtilities.getWindowAncestor(box)!=null) 
				errorPopup.showError(swat.getMyFrame(),((JComponent)box.getComponent(0)).getComponent(1).getLocationOnScreen(),s);
			return false;
		}
		
		swat.dk.renameTextTrait(tt,t,newv);
		onTraitChange();
		
		new UndoableAction(swat,false,""){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				swat.dk.renameTextTrait(tt,t,newv);
				((JComponent)box.getParent()).scrollRectToVisible(box.getBounds());
				JTextComponent tc = (JTextComponent)((JComponent)box.getComponent(0)).getComponent(1); 
				tc.setText(newv);
				tc.requestFocusInWindow();
				tc.selectAll();
				onTraitChange();
			}
			@Override
			public void myUndo() {
				swat.dk.renameTextTrait(tt,t,oldv);
				((JComponent)box.getParent()).scrollRectToVisible(box.getBounds());
				JTextComponent tc = (JTextComponent)((JComponent)box.getComponent(0)).getComponent(1); 
				tc.setText(oldv);
				tc.requestFocusInWindow();
				tc.selectAll();
				onTraitChange();
			}
			@Override
			public String getRedoPresentationName() {
					return "Redo rename "+tt.name()+" trait "+oldv;
			}
			@Override
			public String getUndoPresentationName() {
				return "Undo rename "+tt.name()+" trait "+newv;
			}
		};
		return true;
	}
	
	void showDescriptionPopup(TextTrait t,DescriptionPopupDispatcher popupDispatcher){
		JComponent descriptionButton = (JComponent)((JComponent)((JComponent)((JComponent)textFieldsPanel.getComponent(swat.dk.getTextTraits(tt).indexOf(t))).getComponent(0)).getComponent(0)).getComponent(0);
		descriptionButton.scrollRectToVisible(descriptionButton.getBounds());
		descriptionField.setText(t.getDescription());
		Toolkit.getDefaultToolkit().addAWTEventListener(popupDispatcher,AWTEvent.MOUSE_EVENT_MASK);
		descriptionField.addFocusListener(popupDispatcher);
		descriptionField.addKeyListener(popupDispatcher);
		descriptionPopup.setSize(new Dimension(250,descriptionField.getPreferredSize().height));
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){ descriptionField.requestFocusInWindow();}
			});
		descriptionPopup.showPopup(descriptionButton, new int[]{0,descriptionButton.getWidth()}, new int[]{descriptionButton.getHeight(),0});
	}
	
	public final class DescriptionPopupDispatcher implements FocusListener, KeyListener, AWTEventListener {
		private TextTrait t;
		public DescriptionPopupDispatcher(TextTrait t){ 
			this.t = t;
		}
		public void focusGained(FocusEvent e) {}
		public void focusLost(FocusEvent e) {	descriptionField.requestFocusInWindow();	}
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
	 				stopEditing();			
			}
		}
		public void keyReleased(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
		public void eventDispatched(AWTEvent e) {
 			MouseEvent me = (MouseEvent)e;
 			if (me.getID()!=MouseEvent.MOUSE_PRESSED) return;
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
		private void stopEditing(){
			descriptionPopup.hidePopup();
			Toolkit.getDefaultToolkit().removeAWTEventListener(this);
			descriptionField.removeFocusListener(this);
			descriptionField.removeKeyListener(this);
			final String oldDescription = t.getDescription()!=null?t.getDescription():"";
			final String newDescription = descriptionField.getText().trim();
			if (newDescription.equals(oldDescription))
				return;
			
			t.setDescription(newDescription);
			new UndoableAction(swat,false,"edit description of trait "+t.getLabel()){
				private static final long serialVersionUID = 1L;
				@Override
				protected void myRedo() {
					t.setDescription(newDescription);
					showDescriptionPopup(t,DescriptionPopupDispatcher.this);
				}
				@Override
				protected void myUndo() {
					t.setDescription(oldDescription);
					showDescriptionPopup(t,DescriptionPopupDispatcher.this);
				}
			};
		}
	}
	
	/**
	 * Document filters take text written by the user as input and output
	 * what they think it should be written on an editor box.
	 * This particular document filter passes the text unmodified,
	 * but after passing the text it updates the editor box in case it needs 
	 * to be resized.  
	 * */
	private final class MyDocumentFilter extends DocumentFilter {
		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
			super.insertString(fb, offset, string, attr);
			descriptionPopup.setSize(250,descriptionField.getPreferredSize().height);
		}

		@Override
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			super.remove(fb, offset, length);
			descriptionPopup.setSize(250,descriptionField.getPreferredSize().height);
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			super.replace(fb, offset, length, text, attrs);
			descriptionPopup.setSize(250,descriptionField.getPreferredSize().height);
		}

	}

	private final static class QuestionMarkButton extends JButton {
		private static final long serialVersionUID = 1L;
		private static Icon icon=new ImageIcon(Utils.getImagePath("questionmark.png"));
		public QuestionMarkButton(String tLabel) {
			this();
			setToolTipText("edit description for "+tLabel);
		}
		public QuestionMarkButton() {
			super(icon);
			setBackground(Color.blue);
			setMargin(new Insets(2,2,2,2));
			setEnabled(true);
		}
	}
	
	private static class SlidersPanel extends Box implements Scrollable {
		private static final long serialVersionUID = 1L;
		
		public SlidersPanel(){
			super(BoxLayout.Y_AXIS);
			setMaximumSize(new Dimension(230,Integer.MAX_VALUE));
		}
		
		@Override
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			d.width = 230;
			return d;
		}
		
		public Dimension getPreferredScrollableViewportSize() {
			return new Dimension(212,100);
		}

		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			if (orientation==SwingConstants.HORIZONTAL) 
				return visibleRect.width;
			else return visibleRect.height;
		}

		public boolean getScrollableTracksViewportHeight() { return false; }

		public boolean getScrollableTracksViewportWidth() {	return true; }

		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 20;
		}
		
	}
	
	public static abstract class Test {
		
		public static void renameTrait(CustomTextTraitsControl<?> ct,String oldName,String newName){
			int i = ct.swat.dk.getTextTraitIndex(ct.tt, oldName);
			JComponent box=(JComponent)ct.textFieldsPanel.getComponent(i);
			if (!((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).getText().equals(oldName))
				throw new RuntimeException("expected: "+oldName+" found: "+((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).getText());
			((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).setText(newName);
			ct.renameTrait(ct.swat.dk.getTextTrait(ct.tt,oldName,true), box, newName);
		}
		
		public static void setTraitValue(CustomTextTraitsControl<?> ct,String name,String value){
			int i = ct.swat.dk.getTextTraitIndex(ct.tt, name);
			JComponent box=(JComponent)ct.textFieldsPanel.getComponent(i);
			if (!((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).getText().equals(name))
				throw new RuntimeException("expected: "+name+" found: "+((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).getText());
			ct.setTraitValue((Swat.TextField)box.getComponent(1), ct.swat.dk.getTextTraits(ct.tt).get(i),value);
		};
		
		public static void deleteTrait(CustomTextTraitsControl<?> ct,String traitName){
			int i = ct.swat.dk.getTextTraitIndex(Deikto.TraitType.Actor, traitName);
			ct.deleteTrait(((JComponent)ct.getTextFieldsPanel().getComponent(i)),ct.swat.dk.getTextTraits(Deikto.TraitType.Actor).get(i));
		}

	}
}
