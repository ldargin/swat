package com.storytron.swat;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
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
import com.storytron.swat.util.UndoableSlider;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Role;
import com.storytron.uber.Script;
import com.storytron.uber.Verb;
import com.storytron.uber.Word;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.Operator;

/**
 * <p>This class implements an editor for custom traits.
 * It handles a panel of labeled sliders and a button.
 * </p>
 * <p> Each slider is used to edit a custom trait value.
 * The labels of the sliders can be edited to change the
 * name of the custom traits. Next to the label there is also
 * a button to delete the corresponding custom trait. 
 * The panel containing all the sliders
 * can be retrieved with {@link #getSlidersPanel()}.
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
public abstract class CustomTraitsControl<Entity extends Word> {
	private Box slidersPanel = new SlidersPanel();
	private JButton addCustomTraitButton=new AddButton("custom trait");
	private final LightweightPopup descriptionPopup = new LightweightPopup();
	private final JTextArea descriptionField = new JTextArea();

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
	protected abstract float getValue(Entity e,FloatTrait t);
	/** Sets the value of a trait for a given entity. */
	protected abstract void setValue(Entity e,FloatTrait t,float value);
	/** Called when the traits are renamed added or deleted. */
	protected void onTraitChange(){};
	
	/** Updates the sliders to show the trait values of the 
	 * entity being edited.
	 */
	public void refresh(){
		int i=0;
		Entity editedEntity=getEditedEntity();
		for(FloatTrait t:swat.dk.getTraits(tt)){
			((Swat.Slider)((JComponent)slidersPanel.getComponent(i)).getComponent(1)).mSetValue(toSlider(getValue(editedEntity,t)));
			i++;
		}
	}
	
	/**
	 * Constructs a CustomTraitsControl for editing traits of the given
	 * trait type. 
	 * */
	public CustomTraitsControl(Swat sw,Deikto.TraitType traitType){
		tt=traitType;
		swat=sw;

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
				while(swat.dk.nameExists(i==0?"new "+tt.name()+" trait":"new "+tt.name()+" trait"+i)!=null) i++;
				final String newv=i==0?"new "+tt.name()+" trait":"new "+tt.name()+" trait"+i;
				final int idx = tt==Deikto.TraitType.Actor?Deikto.predefinedActorTraits.length:0;
				try {
					swat.dk.createTrait(tt,idx,newv,true,null);
				} catch (LimitException ex){ throw new RuntimeException(ex); }
				final FloatTrait trait = swat.dk.getTrait(tt, newv);
				final JComponent box=createBoxFor(trait);
				slidersPanel.add(box,idx);
				((JComponent)box.getParent()).scrollRectToVisible(box.getBounds());
				((JComponent)box.getComponent(0)).getComponent(1).requestFocusInWindow();
				((JTextComponent)((JComponent)box.getComponent(0)).getComponent(1)).selectAll();
				checkAddCustomTraitButton();
				onTraitChange();
				slidersPanel.revalidate();

				new UndoableAction(swat,false,"add trait "+newv){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						try {
							swat.dk.addTrait(tt,idx,trait);					
						} catch (LimitException ex){ throw new RuntimeException(ex); }
						slidersPanel.add(box,idx);
						((JComponent)box.getParent()).scrollRectToVisible(box.getBounds());
						((JComponent)box.getComponent(0)).getComponent(1).requestFocusInWindow();
						((JTextComponent)((JComponent)box.getComponent(0)).getComponent(1)).selectAll();
						checkAddCustomTraitButton();
						onTraitChange();
						slidersPanel.revalidate();
					}
					@Override
					public void myUndo() {
						slidersPanel.remove(box);
						swat.dk.removeTrait(tt,newv);
						checkAddCustomTraitButton();
						onTraitChange();
						slidersPanel.revalidate();
					}
				};
			}
		});
	}

	/** 
	 * Routines for converting numbers from slider to model and the other 
	 * way around. 
	 * */
	private static float fromSlider(int v){ return (float)((v-50)/50.0*Utils.MAXI_VALUE); }
	private static int toSlider(float f){ return (int)(f*50/Utils.MAXI_VALUE+50); }

	private void checkAddCustomTraitButton() {
		addCustomTraitButton.setEnabled(swat.dk.getTraitCount(tt)<swat.dk.limits.maximumTraitCount);
		if (!addCustomTraitButton.isEnabled())
			addCustomTraitButton.setToolTipText("Cannot have more than "+swat.dk.limits.maximumTraitCount+" traits.");
		else
			addCustomTraitButton.setToolTipText("creates a new trait");
	}

	public void init(Deikto dk){
		for (FloatTrait t:dk.getTraits(tt)){
			JComponent box=createBoxFor(t);
			slidersPanel.add(box);
		}
		slidersPanel.add(Box.createVerticalGlue());
		checkAddCustomTraitButton();
	}
	
	/** Gets the button for adding traits. */
	public JButton getAddButton(){ return addCustomTraitButton; }
	/** Gets the panel containing the trait sliders. */
	public JComponent getSlidersPanel(){ return slidersPanel; }

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
	 * Stores for a SET operator being deleted, the script
	 * setting the value, and its index in the consequences array.  
	 * */
	private static final class ConsequencePos {
		public int i;
		public Script s;
		public Verb v;
		public ConsequencePos(Verb v,Script s,int i){
			this.i=i;
			this.s=s;
			this.v=v;
		};
	}
	/**
	 * Stores for an Adjust operator being deleted, the role, the script
	 * adjusting the value, and its index in the consequences array.  
	 * */
	private static final class EmotionPos {
		public int i;
		public Script s;
		public Role r;
		public EmotionPos(Role r,Script s,int i){
			this.i=i;
			this.s=s;
			this.r=r;
		};
	}

	/** Creates a box containing a slider, its label and a button to 
	 * delete it.
	 */
	private JComponent createBoxFor(final FloatTrait t){
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
		
		Swat.Slider sld = new Swat.Slider(JSlider.HORIZONTAL, 0, 100, 50){
			private static final long serialVersionUID = 1L;
			@Override
			public String getToolTipText() {
				return Utils.toHtmlTooltipFormat(t.getDescription());
			}
		};
		ToolTipManager.sharedInstance().registerComponent(sld);
		
		//sld.setMaximumSize(new Dimension(200,30));
		//sld.setMinimumSize(new Dimension(200,30));
		//sld.setPreferredSize(new Dimension(200,30));
		sld.setMajorTickSpacing(50);
		sld.setMinorTickSpacing(10);
		sld.setPaintTicks(true);			
		new UndoableSlider(swat,sld) {
			Entity editedEntity;
			@Override
			public int init() {
				editedEntity = getEditedEntity();
				return toSlider(getValue(editedEntity,t));
			}
			@Override
			public void setValue(int value) {
				CustomTraitsControl.this.setValue(editedEntity,t,fromSlider(value));
			}
			@Override
			public void undoRedoExecuted() {
				showEditedEntity(editedEntity);
				Utils.scrollToVisible(box);
			}
			@Override
			public String getPresentationName(){
				return "change "+floatLabel.getText()+" of "+editedEntity.getLabel();
			}
		};
		
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
		sld.setOpaque(false);
		sld.setAlignmentX(0.5f);
		box.add(sld);		
		
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setOpaque(false);
		box.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		box.setAlignmentX(0.5f);
		box.setMinimumSize(new Dimension(10,TRAIT_HEIGHT));
		box.setPreferredSize(new Dimension(212,TRAIT_HEIGHT));
		box.setMaximumSize(new Dimension(250,TRAIT_HEIGHT));
		TRAIT_HEIGHT = box.getPreferredSize().height;
		return box;
	}
	
	private void deleteTrait(final JComponent box,final FloatTrait t){
		// collect verbs where trait is set as consequence
		final LinkedList<String> setnames = new LinkedList<String>();
		Deikto.operatorNamesSet(tt, t, setnames);
		final String adjustPName = "AdjustP"+t.getLabel();
		final LinkedList<ConsequencePos> sl=new LinkedList<ConsequencePos>();
		final LinkedList<EmotionPos> el=new LinkedList<EmotionPos>();
		for(Verb v:swat.dk.getVerbs()){
			for(String name:setnames){
				int p=v.getConsequenceIndex(name);
				if (p>-1) sl.add(new ConsequencePos(v,v.getConsequence(p),p));
			}
			if (tt==Deikto.TraitType.Actor && !t.isVisible())
				for(Role.Link r:v.getRoles()){
					int p=r.getRole().getEmotionIndex(adjustPName);
					if (p>-1) el.add(new EmotionPos(r.getRole(),r.getRole().getEmotion(p),p));
				}
		}
		// collect script nodes where trait is used
		final LinkedList<NodePos> nl=new LinkedList<NodePos>();
		final LinkedList<String> names = new LinkedList<String>();
		Deikto.operatorNamesGet(tt, t, names);
		swat.dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Node n) {
				if ((names.contains(n.getOperator().getLabel()) ||
					 n.getOperator().getOperatorType()==Operator.OpType.Constant &&
					 n.getConstant().toString().equals(t.getLabel())
					 )
					&& n.getParent()!=null)
					nl.add(new NodePos((Node)n.getParent(),n.getParent().getIndex(n),n));
				return true;
			}
		});
		
		final int i=swat.dk.getTraitIndex(tt,t.getLabel());
		// Find the box index.
		int boxi=0;
		while(boxi<slidersPanel.getComponentCount() && slidersPanel.getComponent(boxi)!=box)
			boxi++;
		final int boxIndex = boxi;
		
		new UndoableAction(swat,"delete trait "+t.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				swat.dk.removeTrait(tt,t);
				slidersPanel.remove(box);
				slidersPanel.revalidate();
				slidersPanel.repaint();
				checkAddCustomTraitButton();
				onTraitChange();
			}
			@Override
			public void myUndo() {
				try {
					swat.dk.addTrait(tt,i,t);
				} catch(LimitException ex) { throw new RuntimeException(ex); };
				for(ConsequencePos p:sl)
					swat.dk.addConsequence(p.v,p.i,p.s);
				for(EmotionPos p:el)
					swat.dk.addEmotion(p.r,p.i,p.s);
				for(NodePos np:nl){
					np.parent.remove(np.i);
					np.parent.insert(np.n,np.i);
				}
				slidersPanel.add(box,boxIndex);
				((JComponent)box.getParent()).scrollRectToVisible(box.getBounds());
				checkAddCustomTraitButton();
				onTraitChange();
			}
		};
	}
	
	private boolean renameTrait(final FloatTrait t,final JComponent box,final String newv){
		// trait is being renamed
		final String oldv=t.getLabel();
		if (newv.equals(oldv)) return true;
		
		String s=swat.dk.nameExists(newv);
		if (s!=null) {
			if (SwingUtilities.getWindowAncestor(box)!=null) 
				errorPopup.showError(swat.getMyFrame(),((JComponent)box.getComponent(0)).getComponent(1).getLocationOnScreen(),s);
			return false;
		}
		
		swat.dk.renameTrait(tt,t,newv);
		onTraitChange();
		
		new UndoableAction(swat,false,""){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				swat.dk.renameTrait(tt,t,newv);
				((JComponent)box.getParent()).scrollRectToVisible(box.getBounds());
				JTextComponent tc = (JTextComponent)((JComponent)box.getComponent(0)).getComponent(1); 
				tc.setText(newv);
				tc.requestFocusInWindow();
				tc.selectAll();
				onTraitChange();
			}
			@Override
			public void myUndo() {
				swat.dk.renameTrait(tt,t,oldv);
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
	
	void showDescriptionPopup(FloatTrait t,DescriptionPopupDispatcher popupDispatcher){
		JComponent descriptionButton = (JComponent)((JComponent)((JComponent)((JComponent)slidersPanel.getComponent(swat.dk.getTraits(tt).indexOf(t))).getComponent(0)).getComponent(0)).getComponent(0);
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
		private FloatTrait t;
		public DescriptionPopupDispatcher(FloatTrait t){ 
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

		public boolean getScrollableTracksViewportWidth() {	return false; }

		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 20;
		}
		
	}
	
	public static abstract class Test {
		
		public static void renameTrait(CustomTraitsControl<?> ct,String oldName,String newName){
			int i = ct.swat.dk.getTraitIndex(ct.tt, oldName);
			JComponent box=(JComponent)ct.slidersPanel.getComponent(i);
			if (!((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).getText().equals(oldName))
				throw new RuntimeException("expected: "+oldName+" found: "+((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).getText());
			((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).setText(newName);
			ct.renameTrait(ct.swat.dk.getTrait(ct.tt,oldName), box, newName);
		}
		
		public static void setTraitValue(CustomTraitsControl<?> ct,String name,float value){
			int i = ct.swat.dk.getTraitIndex(ct.tt, name);
			JComponent box=(JComponent)ct.slidersPanel.getComponent(i);
			if (!((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).getText().equals(name))
				throw new RuntimeException("expected: "+name+" found: "+((Swat.TextField)((JComponent)box.getComponent(0)).getComponent(1)).getText());
			((JSlider)box.getComponent(1)).setValue(toSlider(value));
		};
		
		public static void deleteTrait(CustomTraitsControl<?> ct,String traitName){
			int i = ct.swat.dk.getTraitIndex(Deikto.TraitType.Actor, traitName);
			ct.deleteTrait(((JComponent)ct.getSlidersPanel().getComponent(i)),ct.swat.dk.getTraits(Deikto.TraitType.Actor).get(i));
		}

	}
}
