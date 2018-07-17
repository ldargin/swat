package com.storytron.swat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.ScaledImage;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.AddButton;
import com.storytron.swat.util.ComponentLabeledPanel;
import com.storytron.swat.util.DeleteButton;
import com.storytron.swat.util.DropDown;
import com.storytron.swat.util.EditorListener;
import com.storytron.swat.util.ErrorPopup;
import com.storytron.swat.util.LineBox;
import com.storytron.swat.util.PopupButton;
import com.storytron.swat.util.UndoableAction;
import com.storytron.swat.verbeditor.OperatorMenu.NonOverlappedPopupMenu;
import com.storytron.uber.Actor;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Prop;
import com.storytron.uber.Script;
import com.storytron.uber.Stage;
import com.storytron.uber.TextTrait;
import com.storytron.uber.Word;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.OperatorDictionary;

public final class PropEditor {
	private static final long serialVersionUID = 1L;
	private DropDown selectionBox;
	private DropDown ownerBox, locationBox;
	private Deikto dk;
	private Swat swat;
	private JPanel myPanel;
	private JCheckBox carriedCheckBox, inPlayCheckBox, visibleCheckBox;
	private JComboBox actor2Box;
	private CustomTraitsControl<Prop> customTraits;
	private CustomTextTraitsControl<Prop> customTextTraits;
	private Prop mProp = null;
	private Actor mActor = null;
	private KnowsMenu knowsMenu;
	private DeleteButton deleteButton;
	private AddButton addButton;
	private boolean userInput = true;
	private EditorListener selectionEditorListener;
	private JPanel mainPanel;
	private CustomPTraitsControl<Prop> customPTraits;
	private JButton backgroundInformationButton = new JButton("Background information");
	private BackgroundEditor bgEditor;
	private JLabel propLabel = new JLabel(); 
	private Map<FloatTrait,JCheckBox> visibilityCBs = new TreeMap<FloatTrait,JCheckBox>();
	private JComponent visibilityCBPanel;

	
//**********************************************************************	
	private class KnowsMenu extends PopupButton {
		private static final long serialVersionUID = 1L;
		NonOverlappedPopupMenu myPopup;
		ActionListener myActionListener;
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++		
		class PopupListener extends MouseAdapter {
			public void mousePressed(MouseEvent e) {
				if (isEnabled())
					myPopup.showPopup();
			}
		}
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++		
		public KnowsMenu() {
			super("Knows Me");
			addMouseListener(new PopupListener());
			myPopup = new NonOverlappedPopupMenu(this);			
			setEnabled(true);
			myActionListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final JCheckBoxMenuItem selectedItem = (JCheckBoxMenuItem)e.getSource();
					final Prop prop = mProp;
					final int iActor = dk.getActorIndex(selectedItem.getText()); 
					final boolean newValue = selectedItem.isSelected();
					prop.setKnowsMe(dk.getActor(iActor),newValue);
					showProp(prop);
					new UndoableAction(swat,false,"change Knows Me for "+selectedItem.getText()){
						private static final long serialVersionUID = 1L;
						public void myRedo(){
							prop.setKnowsMe(dk.getActor(iActor),newValue);
							showProp(prop);
							myPopup.showPopup();
						}
						public void myUndo(){
							prop.setKnowsMe(dk.getActor(iActor),!newValue);
							showProp(prop);
							myPopup.showPopup();
						}
					};				

				}
			};
		}
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++		
		public void loadKnowsMenu() {
			myPopup.removeAll();
			for (int i=0; (i < dk.getActorCount()); ++i) {
				Actor ac = dk.getActor(i);
				JCheckBoxMenuItem zCheckBoxMenuItem = new JCheckBoxMenuItem(ac.getLabel());
				zCheckBoxMenuItem.addActionListener(myActionListener);
				zCheckBoxMenuItem.setState(mProp.getKnowsMe(dk.getActor(i)));
				myPopup.add(zCheckBoxMenuItem);
			}
		}
	}

//**********************************************************************	
	public PropEditor(Swat tSwat) {
		myPanel = new JPanel();
		dk = null;
		swat = tSwat;
	    // See bug 469 to find out why we instantiate this here.
		bgEditor = createBackgroundEditor();
		addButton = new AddButton("Prop");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i=0;
				while(dk.getPropIndex(i==0?"new prop":"new prop"+i)!=-1) i++;
				addProp(i==0?"new prop":"new prop"+i);
				selectionBox.getJTextComponent().requestFocusInWindow();
				selectionBox.getJTextComponent().selectAll();
			}
		});

		myPanel.setOpaque(true);		

		initWidgets();
		setLayout();		
		
		backgroundInformationButton.setToolTipText(Utils.toHtmlTooltipFormat("What the players will see when they look at the \"Things\" display."));
		carriedCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If checked, the Prop will be carried from Stage to Stage by its Owner."));
		inPlayCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If unchecked, the Engine acts as if the Prop does not exist."));
		visibleCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If checked, the Prop is immediately visible to all Actors on its Stage. If unchecked, the Prop is not visible to other Actors."));
		knowsMenu.setToolTipText(Utils.toHtmlTooltipFormat("Check the Actors who start the story knowing the values of the Traits of this Prop."));
		
		myPanel.validate();
	}
	public void init(Deikto tdk){
		dk=tdk;
		customTraits.init(dk);
		customTextTraits.init(dk);
		reloadVisibilityCBs();
		bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Prop));

		if (dk.getPropCount()>1) mProp = dk.getProp(1);
		else mProp=null;
		if (dk.getActorCount()>1) mActor = dk.getActor(1);
		else mActor=null;
		onPropAddOrRemove();
	}
//	**********************************************************************
	private ErrorPopup errorPopup=new ErrorPopup();
	private void initWidgets(){
		
		actor2Box = new JComboBox();
		actor2Box.setBackground(Color.white);
		actor2Box.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!userInput) return;
				showProp(mProp,(Actor)actor2Box.getSelectedItem());
			}
		});

		customTraits = new CustomTraitsControl<Prop>(swat,Deikto.TraitType.Prop){
			@Override
			protected Prop getEditedEntity() {	return mProp; }
			@Override
			protected float getValue(Prop e, FloatTrait t) { return e.getTrait(t); }
			@Override
			protected void setValue(Prop e, FloatTrait t, float value) {
				e.setTrait(t,value);
			}
			@Override
			protected void showEditedEntity(Prop e) { showProp(e); }
			@Override
			protected void onTraitChange(){ 
				reloadVisibilityCBs();
				customPTraits.reloadTraits(); 
				customPTraits.refreshValues();
				if (bgEditor.isVisible()){
					bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Prop));
					reloadBGEditorTraitValues();
				}
				myPanel.validate();
			}
		};

		customTextTraits = new CustomTextTraitsControl<Prop>(swat,Deikto.TraitType.Prop){
			@Override
			protected Prop getEditedEntity() {	return mProp; }
			@Override
			protected String getValue(Prop e, TextTrait t) { return e.getText(t); }
			@Override
			protected void setValue(Prop e, TextTrait t, String value) {
				e.setText(t,value);
				if (swat.verbEditor.sentenceDisplayEditor.isVisible())
					swat.verbEditor.sentenceDisplayEditor.refresh();
			}
			@Override
			protected void showEditedEntity(Prop e) { showProp(e); }
			@Override
			protected void onTraitChange() {
				customTextTraits.getTextFieldsPanel().revalidate();
				customTextTraits.getTextFieldsPanel().repaint();
				if (swat.verbEditor.sentenceDisplayEditor.isVisible())
					swat.verbEditor.sentenceDisplayEditor.refresh();
			}
		};

		selectionBox = new DropDown(Deikto.MAXIMUM_FIELD_LENGTH){
			private static final long serialVersionUID = 1L;
			@Override
			public String getTooltipText(Object o) {
				return Utils.toHtmlTooltipFormat(((Prop)o).getDescription());
			}
			@Override
			public void indexMoved(final int from,final int to) {
				final Prop prop = mProp;
				new UndoableAction(swat,"reorder props"){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						dk.moveProp(from+1,to+1);
						mProp=prop;
						refresh();
						selectionBox.showPopup();
					}
					@Override
					public void myUndo() {
						dk.moveProp(to+1,from+1);
						mProp=prop;
						refresh();
						selectionBox.showPopup();
					}
				};
			}
		};
		selectionBox.setAllowReordering(true);
		selectionBox.setEditable(true);
		selectionEditorListener=new Swat.DropDownListener(selectionBox) {
			private static final long serialVersionUID = 1L;
			public boolean timedActionPerformed(ActionEvent e) {
				if (!userInput) return true; 
				if (selectionBox.isListPicking())
					showProp((Prop)selectionBox.getSelectedItem());					
				else { // if not listPicking, assume the storybuilder
					// is trying to rename the current prop.
					String zLabel = ((String)selectionBox.getTextComponent().getJTextComponent().getText()).trim();
					if (zLabel==null || mProp == null || mProp.getLabel().equals(zLabel))
						return true;

					String s=swat.dk.nameExists(zLabel);
					if (s!=null) {
						errorPopup.showError(swat.getMyFrame(),selectionBox.getLocationOnScreen(),s);
						return false;
					}

					final Prop prop = mProp;
					final String newValue = zLabel;
					final String oldValue = prop.getLabel();
					prop.setLabel(newValue);
					new UndoableAction(swat,false,"rename prop"){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							prop.setLabel(newValue);
							loadSelectionBox(prop);
							showProp(prop);
						}
						@Override
						public void myUndo() {
							prop.setLabel(oldValue);
							loadSelectionBox(prop);
							showProp(prop);
						}
					};
				}	
				return true;
			}
			@Override
			public String getText() { return mProp!=null?mProp.getLabel():""; }
		};
		selectionBox.addActionListener(selectionEditorListener);
		selectionBox.setMaximumRowCount(35);

		deleteButton = new DeleteButton("Prop");
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteSelectedProp();
			}
		});

		backgroundInformationButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				showBackgroundEditor();
			}
		});
		
		carriedCheckBox = new JCheckBox("carried");
		carriedCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		carriedCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!userInput) return;
				final Prop prop = mProp;
				final boolean newValue = carriedCheckBox.isSelected();
				new UndoableAction(swat,"change carried of "+prop.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						prop.setCarried(newValue);							
						showProp(prop);
					}
					@Override
					public void myUndo() {
						prop.setCarried(!newValue);							
						showProp(prop);
					}
				};
			}
		});
		
		inPlayCheckBox = new JCheckBox("inPlay");
		inPlayCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		inPlayCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!userInput) return;
				final Prop prop = mProp;
				final boolean newValue = inPlayCheckBox.isSelected();
				new UndoableAction(swat,"change inPlay of "+prop.getLabel()){
					private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							prop.setInPlay(newValue);							
							showProp(prop);
						}
						@Override
						public void myUndo() {
							prop.setInPlay(!newValue);							
							showProp(prop);
						}
					};
			}
		});
		
		visibleCheckBox = new JCheckBox("visible");
		visibleCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		visibleCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!userInput) return;
				final Prop prop = mProp;
				final boolean newValue = visibleCheckBox.isSelected();
				new UndoableAction(swat,"change visible of "+prop.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						prop.setVisible(newValue);							
						showProp(prop);
					}
					@Override
					public void myUndo() {
						prop.setVisible(!newValue);							
						showProp(prop);
					}
				};
			}
		});
		
		knowsMenu = new KnowsMenu();

		ownerBox = new DropDown(100){
			private static final long serialVersionUID = 1L;
			@Override
			public String getTooltipText(Object o) {
				return Utils.toHtmlTooltipFormat(dk.getActor((String)o).getDescription());
			}
		};
		ownerBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!userInput) return;

				setPropOwner(ownerBox.getSelectedIndex());
			}
		});
	
		locationBox = new DropDown(100){
			private static final long serialVersionUID = 1L;
			@Override
			public String getTooltipText(Object o) {
				return Utils.toHtmlTooltipFormat(dk.getStage((String)o).getDescription());
			}
		};
		locationBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!userInput) return;
				
				final Prop prop = mProp;
				final int newValue = locationBox.getSelectedIndex();
				final Word.Reference oldValue = prop.getLocationRef();
				new UndoableAction(swat,"change location of "+prop.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						prop.setLocation(dk.getStage(newValue));							
						showProp(prop);
					}
					@Override
					public void myUndo() {
						prop.setLocationRef(oldValue);
						showProp(prop);
					}
				};
			}
		});

		customPTraits = new CustomPTraitsControl<Prop>(swat,Deikto.TraitType.Prop){
			private static final long serialVersionUID = 1L;
			@Override
			protected float getCValue(Actor e, FloatTrait t, Prop other) {
				return e.getC(t, other);
			}

			@Override
			protected Actor getEditedActor() {	return mActor; }

			@Override
			protected Prop getOtherEntity() { return mProp; }

			@Override
			protected boolean isOverrided(Actor e, FloatTrait t, Prop other) {
				return e.isOverrided(t, other);
			}

			@Override
			protected float getPValue(Actor e, FloatTrait t, Prop other) {
				return e.getP(t, other);
			}

			@Override
			protected void setCValue(Actor e, FloatTrait t, Prop other, Float value) {
				e.setC(t, other, value);
				if (bgEditor.isVisible() && e == dk.getActor(1))
					reloadBGEditorTraitValues();
			}

			@Override
			protected void setPValue(Actor e, FloatTrait t, Prop other, Float value) {
				e.setP(t, other, value);
				if (bgEditor.isVisible() && e == dk.getActor(1))
					reloadBGEditorTraitValues();
			}

			@Override
			protected void showEditedActor(Actor e, Prop other) {
				showProp(other,e);
			}
			
			@Override
			protected String getPerceptionTooltip(FloatTrait t) {
				return "The Actor's perceived value of this trait";
			}
			
			@Override
			protected String getConfidenceTooltip(FloatTrait t) {
				return "The Actor's confidence in their perceived value of this trait";
			}
			
			@Override
			protected String getOverrideTooltip(FloatTrait t) {
				return "Uncheck this box to have the system automatically set the perceived value of this"
						+" trait to the correct value if KnowsMe is checked, and to zero if KnowsMe is"
						+" unchecked.";
			}
		};

	}
//	**********************************************************************
	private void setLayout(){

		JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.X_AXIS));
		{
			Dimension d = selectionBox.getPreferredSize();
			d.width = 200;
			selectionBox.setMaximumSize(d);
			selectionBox.setPreferredSize(d);
		}
		selectionBox.setBackground(Color.WHITE);
		selectionBox.setAlignmentY(0.0f);
		addButton.setAlignmentY(0.0f);
		deleteButton.setAlignmentY(0.0f);
		lowerPanel.add(selectionBox);
		lowerPanel.add(addButton);
		lowerPanel.add(deleteButton);
		lowerPanel.setOpaque(false);	
		
		Dimension db=addButton.getPreferredSize();
		db.height=selectionBox.getPreferredSize().height;
		addButton.setPreferredSize(db);
		deleteButton.setPreferredSize(db);
		
		JPanel selectionPanel = new JPanel();
		selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
		selectionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		lowerPanel.setAlignmentX(0.5f);
		selectionPanel.add(lowerPanel);
		selectionPanel.setOpaque(false);

		Box checkBoxPanel = Box.createHorizontalBox();
		checkBoxPanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
		carriedCheckBox.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		carriedCheckBox.setOpaque(false);
		checkBoxPanel.add(carriedCheckBox);
		checkBoxPanel.add(Box.createHorizontalGlue());
		inPlayCheckBox.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		inPlayCheckBox.setOpaque(false);
		checkBoxPanel.add(inPlayCheckBox);
		checkBoxPanel.add(Box.createHorizontalGlue());
		visibleCheckBox.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		visibleCheckBox.setOpaque(false);
		checkBoxPanel.add(visibleCheckBox);
		checkBoxPanel.setOpaque(false);

		JPanel ownerPanel = new JPanel();
		ownerPanel.setLayout(new BoxLayout(ownerPanel, BoxLayout.Y_AXIS));
		ownerPanel.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
		JLabel ownerLabel = new JLabel("Owner of Prop");
		ownerLabel.setAlignmentX(0.5f);
		ownerPanel.add(ownerLabel);
		Dimension d = ownerBox.getPreferredSize();
		d.width=225;
		ownerBox.setMaximumSize(d);
		ownerBox.setAlignmentX(0.5f);
		ownerBox.setBackground(Color.white);
		ownerPanel.add(ownerBox);
		ownerPanel.setOpaque(false);
	
		JPanel locationPanel = new JPanel();
		locationPanel.setLayout(new BoxLayout(locationPanel, BoxLayout.Y_AXIS));
		JLabel locationLabel = new JLabel("Location of Prop");
		locationLabel.setAlignmentX(0.5f);
		locationPanel.add(locationLabel);
		locationPanel.setOpaque(false);
		locationPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		d = locationBox.getPreferredSize();
		d.width=225;
		locationBox.setMaximumSize(d);
		locationBox.setBackground(Color.white);
		locationBox.setAlignmentX(0.5f);
		locationPanel.add(locationBox);

		JComponent stateTraitsPanel = Box.createVerticalBox();
		stateTraitsPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("State Traits"),
				BorderFactory.createEmptyBorder(0, 5, 0, 5))
				);
		checkBoxPanel.setAlignmentX(0.5f);
		stateTraitsPanel.add(checkBoxPanel);
		knowsMenu.setAlignmentX(0.5f);		
		stateTraitsPanel.add(knowsMenu);	
		ownerPanel.setAlignmentX(0.5f);
		stateTraitsPanel.add(ownerPanel);
		locationPanel.setAlignmentX(0.5f);
		stateTraitsPanel.add(locationPanel);
		
		visibilityCBPanel = new JPanel(null);
		visibilityCBPanel.setLayout(new BoxLayout(visibilityCBPanel,BoxLayout.Y_AXIS));
		visibilityCBPanel.setBackground(Utils.darkColumnBackground);
		visibilityCBPanel.setMaximumSize(new Dimension(20,Integer.MAX_VALUE));
		
		JComponent corePropTraitsPanel = Box.createHorizontalBox();
		customTraits.getSlidersPanel().setAlignmentY(0.0f);
		corePropTraitsPanel.add(Box.createHorizontalGlue());
		corePropTraitsPanel.add(customTraits.getSlidersPanel());
		visibilityCBPanel.setAlignmentY(0.0f);
		corePropTraitsPanel.add(visibilityCBPanel);

		final JScrollPane sp = new JScrollPane(corePropTraitsPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.getViewport().setBackground(Utils.lightBackground);
		sp.setMinimumSize(new Dimension(255,50));
		sp.setPreferredSize(sp.getMinimumSize());
		sp.getVerticalScrollBar().getModel().addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						customPTraits.getVerticalScrollBar().setValue(((BoundedRangeModel)e.getSource()).getValue());
						customPTraits.revalidate();
					}
				});
		customPTraits.getVerticalScrollBar().getModel().addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						sp.getVerticalScrollBar().setValue(((BoundedRangeModel)e.getSource()).getValue());
						sp.revalidate();
					}
				});


		JComponent perceivedPropTraitsPanel=Box.createVerticalBox();
		perceivedPropTraitsPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Core Prop Traits"),
				BorderFactory.createEmptyBorder(3,3,3,3)
				)
		);
		customTraits.getAddButton().setAlignmentX(1.0f);
		perceivedPropTraitsPanel.add(customTraits.getAddButton());
		sp.setAlignmentX(1.0f);
		perceivedPropTraitsPanel.add(sp);
		perceivedPropTraitsPanel.setMinimumSize(perceivedPropTraitsPanel.getPreferredSize());
		
		JComponent textTraitsPanel = Box.createHorizontalBox();
		textTraitsPanel.add(customTextTraits.getTextFieldsPanel());
		
		JScrollPane textTraitScroll = new JScrollPane(textTraitsPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		textTraitScroll.getViewport().setBackground(Utils.lightBackground);
		textTraitScroll.getVerticalScrollBar().setUnitIncrement(20);
		
		JComponent headerPanel = Box.createHorizontalBox();
		headerPanel.add(new JLabel("Text Traits"));
		headerPanel.add(new LineBox(2,Swat.shadow));
		headerPanel.add(customTextTraits.getAddButton());
		
		ComponentLabeledPanel customTextTraitsPanel = new ComponentLabeledPanel(headerPanel,Swat.shadow,false);
		customTextTraitsPanel.setOpaque(false);
		customTextTraitsPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		customTextTraitsPanel.add(textTraitScroll);

		Box leftPanel = Box.createVerticalBox();
		leftPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
		selectionPanel.setAlignmentX(0.5f);
		leftPanel.add(selectionPanel);
		backgroundInformationButton.setAlignmentX(0.5f);
		leftPanel.add(backgroundInformationButton);
		leftPanel.add(Box.createRigidArea(new Dimension(5,5)));
		checkBoxPanel.setAlignmentX(0.5f);
		leftPanel.add(stateTraitsPanel);
		customTextTraitsPanel.setAlignmentX(0.5f);
		leftPanel.add(customTextTraitsPanel);

		Box actor2LabelPanel = Box.createHorizontalBox();
		actor2LabelPanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
		actor2LabelPanel.add(actor2Box);
		actor2LabelPanel.add(new JLabel(" thinks of "));
		actor2LabelPanel.add(propLabel);
		actor2LabelPanel.add(Box.createHorizontalGlue());
		
		JComponent ptraitsPanel = Box.createVerticalBox();
		ptraitsPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Perceived Prop Traits"),
				BorderFactory.createEmptyBorder(3,3,3,3)
				)
		);
		actor2LabelPanel.setAlignmentX(0.0f);
		ptraitsPanel.add(actor2LabelPanel);
		customPTraits.setAlignmentX(0.0f);
		ptraitsPanel.add(customPTraits);
		ptraitsPanel.setMinimumSize(ptraitsPanel.getPreferredSize());
		
		mainPanel = new JPanel();
		mainPanel.setBackground(Utils.lightBackground);
		mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.X_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15,20,20,20));
		leftPanel.setAlignmentY(0.0f);
		mainPanel.add(leftPanel);
		perceivedPropTraitsPanel.setAlignmentY(0.0f);
		mainPanel.add(perceivedPropTraitsPanel);
		ptraitsPanel.setAlignmentY(0.0f);
		mainPanel.add(ptraitsPanel);

		myPanel.setLayout(new BoxLayout(myPanel,BoxLayout.X_AXIS));
		myPanel.setBorder(BorderFactory.createEmptyBorder(0, -20, 0, -20));
		myPanel.add(Box.createHorizontalGlue());
		myPanel.add(mainPanel);
		myPanel.add(Box.createHorizontalGlue());
		myPanel.setBackground(Utils.darkBackground);
	}
//**********************************************************************
	public void addProp(String tLabel) {
		final Prop prop = new Prop(tLabel);
		final int iProp = mProp==null?0:mProp.getReference().getIndex();
		new UndoableAction(swat,"add prop "+tLabel){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				try {
					dk.addProp(prop);
				} catch (LimitException e) { throw new RuntimeException(e); }
				addProp(prop);
				showProp(prop);
			}
			@Override
			public void myUndo() {
				deleteProp(prop);
				if (iProp>0)
					showProp(dk.getProp(iProp));
			}
		};
	}
	public void onPropAddOrRemove() {
		addButton.setEnabled(swat.dk.getPropCount()<swat.dk.limits.maximumPropCount);
		if (!addButton.isEnabled())
			addButton.setToolTipText("Cannot have more than "+dk.limits.maximumPropCount+" props.");
		else
			addButton.setToolTipText("creates a new prop");
		
		deleteButton.setEnabled(swat.dk.getPropCount()>2);
		carriedCheckBox.setEnabled(swat.dk.getPropCount()>1);
		inPlayCheckBox.setEnabled(swat.dk.getPropCount()>1);
		visibleCheckBox.setEnabled(swat.dk.getPropCount()>1);
		ownerBox.setEnabled(swat.dk.getPropCount()>1);
		locationBox.setEnabled(swat.dk.getPropCount()>1);
		knowsMenu.setEnabled(swat.dk.getPropCount()>1);
		customPTraits.setEnabled(swat.dk.getPropCount()>1);
		backgroundInformationButton.setEnabled(swat.dk.getPropCount()>1);
	}
	public void addProp(Prop prop) {
		loadSelectionBox(prop);
		onPropAddOrRemove();
		Swat.playSound("add.aiff");
	}
//**********************************************************************
	private void deleteSelectedProp() {
		if (mProp==null) return;
		
		final LinkedList<Script.Node> modifiedNodes = new LinkedList<Script.Node>();
		dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Node n) {
				if (n.getConstant() == mProp)
					modifiedNodes.add(n);
				return true;
			}
		});

		final Prop prop = mProp;
		final int iProp = mProp.getReference().getIndex();
		new UndoableAction(swat,"delete prop "+prop.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				deleteProp(prop);
				if (dk.getPropCount()>1) showProp(dk.getProp(1));
				else mProp = null;
			}
			@Override
			public void myUndo() {
				try {
					dk.addProp(iProp,prop);				
				} catch (LimitException e) { throw new RuntimeException(e); }
				addProp(prop);
				for(Script.Node n:modifiedNodes)
					n.setOperatorValue(OperatorDictionary.getPropConstantOperator(),prop);
				showProp(prop);
			}
		};
	}
	private void deleteProp(Prop prop){
		userInput = false;
		selectionBox.getModel().removeElement(prop);
		userInput = true;
		dk.removeProp(prop);
		onPropAddOrRemove();
		Swat.playSound("delete.aiff");
	}
	
	private void setPropOwner(final int newValue){
		final Prop prop = mProp;
		final Word.Reference oldValue = prop.getOwnerRef();
		new UndoableAction(swat,"change owner of "+prop.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				prop.setOwner(dk.getActor(newValue));	
				showProp(prop);
			}
			@Override
			public void myUndo() {
				prop.setOwnerRef(oldValue);
				showProp(prop);
			}
		};
	}
	
	private void reloadVisibilityCBs(){
		for(final FloatTrait t:dk.getPropTraits()) {
			Component cb = visibilityCBs.get(t);
			if (cb!=null)
				continue;
			
			final JCheckBox vcb = new JCheckBox();
			vcb.setToolTipText("<html><b>Make visible</b><br>"+
				   "If this is checked, then actors will<br>"+
				   "perceive this trait in Props<br>" +
				   "every time they encounter those Props.<html>");
			vcb.setOpaque(false);
			vcb.setMinimumSize(new Dimension(5,CustomTraitsControl.TRAIT_HEIGHT));
			vcb.setMaximumSize(new Dimension(20,CustomTraitsControl.TRAIT_HEIGHT));
			vcb.setPreferredSize(new Dimension(20,CustomTraitsControl.TRAIT_HEIGHT));
			vcb.setSelected(t.isVisible());
			vcb.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					final boolean oldValue = t.isVisible();
					new UndoableAction(swat,"set visibility of "+t.getLabel()){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							dk.changeTraitVisibility(t);
							vcb.setSelected(!oldValue);
							if (bgEditor.isVisible()) {
								bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Prop));
								reloadBGEditorTraitValues();
							}
							((JComponent)vcb.getParent()).scrollRectToVisible(vcb.getBounds());
						}
						@Override
						public void myUndo() {
							dk.changeTraitVisibility(t);
							vcb.setSelected(oldValue);
							if (bgEditor.isVisible()) {
								bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Prop));
								reloadBGEditorTraitValues();
							}
							((JComponent)vcb.getParent()).scrollRectToVisible(vcb.getBounds());
						}
					};
				}
			});
			visibilityCBs.put(t,vcb);
		}

		visibilityCBPanel.removeAll();
		for (FloatTrait t:dk.getPropTraits())
			visibilityCBPanel.add(visibilityCBs.get(t));
		visibilityCBPanel.add(Box.createVerticalGlue());
		visibilityCBPanel.getParent().validate();
		visibilityCBPanel.getParent().repaint();
		
		for(FloatTrait t:dk.getPropTraits())
			((JCheckBox)visibilityCBs.get(t)).setSelected(t.isVisible());
	};
	
//**********************************************************************
	public void refresh() {
		loadSelectionBox(mProp);
		loadActor2Box();
		customPTraits.reloadTraits();
		
		loadOwnerBox();
		loadLocationBox(locationBox.getSelectedIndex());
		showProp(mProp, mActor);
		mainPanel.setMaximumSize(new Dimension(mainPanel.getPreferredSize().width,Integer.MAX_VALUE));		
	}
	public void showProp(Prop prop) {
		showProp(prop,mActor);
	}
	public void showProp(Prop prop,Actor actor) {
		if (prop==null)
			return;
		
		mProp = prop;
		mActor = actor;
		userInput = false;
		propLabel.setText(mProp.getLabel());
		selectionBox.setSelectedItem(prop);
		carriedCheckBox.setSelected(mProp.getCarried());
		inPlayCheckBox.setSelected(mProp.getInPlay());
		visibleCheckBox.setSelected(mProp.getVisible());
		ownerBox.setSelectedIndex(mProp.getOwner());
		locationBox.setSelectedIndex(mProp.getLocation());
		actor2Box.setSelectedItem(mActor);

		customTraits.refresh(); 
		customPTraits.refreshValues();
		customTextTraits.refresh();
		if (bgEditor.isVisible())
			showBackgroundEditor();

		userInput = true;
		knowsMenu.loadKnowsMenu();
	}
//	**********************************************************************	
	public void loadSelectionBox(Prop prop) {
		userInput = false;
		selectionBox.removeAllItems();
		for (int i = 1; (i < dk.getPropCount()); ++i)
			selectionBox.addItem(dk.getProp(i));
		selectionBox.setSelectedItem(prop);
		userInput = true;
	}
//	**********************************************************************	
	public void loadActor2Box() {
		userInput=false;
		actor2Box.removeAllItems();
		for (int i = 1; (i < dk.getActorCount()); ++i) {
			actor2Box.addItem(dk.getActor(i));
		}
		int rows = actor2Box.getItemCount();
		if (rows<35)
			actor2Box.setMaximumRowCount(rows);
		else actor2Box.setMaximumRowCount(35);
		if (dk.getActorCount()>1) 
			actor2Box.setSelectedItem(mActor);
		actor2Box.setMaximumSize(null);
		actor2Box.setMaximumSize(actor2Box.getPreferredSize());
		userInput=true;
	}
//**********************************************************************	
	public void loadOwnerBox() {		
		userInput = false;
		ownerBox.removeAllItems();
		for (int i = 0; (i < dk.getActorCount()); ++i) {
			ownerBox.addItem(((Actor)dk.getActor(i)).getLabel());
		}
		if (mProp!=null) ownerBox.setSelectedIndex(mProp.getOwner());
		int rowCount = dk.getActorCount();
		if (rowCount > 35)
			rowCount = 35;
		ownerBox.setMaximumRowCount(rowCount);
		userInput = true;
	}
//**********************************************************************	
	public void loadLocationBox(int selected) {
		userInput = false;
		locationBox.removeAllItems();
		for (int i = 0; (i < dk.getStageCount()); ++i) {
			locationBox.addItem(((Stage)dk.getStage(i)).getLabel());
		}
		if (-1<selected && selected<locationBox.getItemCount()) locationBox.setSelectedIndex(selected);
		userInput = true;
	}

//	**********************************************************************
	public Prop getProp() { return mProp; }
//**********************************************************************
	public JPanel getMyPanel() { return myPanel; }
//**********************************************************************
	private void reloadBGEditorTraitValues(){
		float[] values = new float[dk.getVisibleTraitCount(Deikto.TraitType.Prop)];
		int i=0;
		for(FloatTrait t:dk.getVisibleTraits(Deikto.TraitType.Prop)) {
			if (t.isVisible())
				values[i++]=dk.getActor(1).getP(t,mProp);
		}
		bgEditor.setTraitValues(values);
	}
	private void showBackgroundEditor(){
		bgEditor.setImage(mProp.getImage(dk));
		bgEditor.setDescription(mProp.getDescription());
		bgEditor.setTitle("Prop: "+mProp.getLabel());
		if (!bgEditor.isVisible())
			bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Prop));
		reloadBGEditorTraitValues();
		bgEditor.setVisible(true);
		bgEditor.toFront();
	}
	
	private BackgroundEditor createBackgroundEditor(){
		BackgroundEditor bgEditor = new BackgroundEditor(swat.getMyFrame()){
			private static final long serialVersionUID = 1L;
			@Override
			public void onDescriptionChange(final String newDescription) {
				final Prop prop = mProp;
				final String oldDescription = prop.getDescription();
				if (oldDescription.equals(newDescription))
					return;
				prop.setDescription(newDescription);
				
				new UndoableAction(swat,false,"set description for "+mProp.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						prop.setDescription(newDescription);
						showProp(prop);
						showBackgroundEditor();
					}
					@Override
					public void myUndo() {
						prop.setDescription(oldDescription);
						showProp(prop);
						showBackgroundEditor();
					}
				};
			}
			@Override
			public void onImageChange(final ScaledImage newImage) {
				final Prop prop = mProp;
				final ScaledImage oldImage = prop.getImage(dk);
				final String oldImageName = prop.getImageName();
				prop.setImage(newImage);
				prop.increaseImageChangeCount();
				new UndoableAction(swat,false,"set image for "+mProp.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						prop.setImage(newImage);
						prop.increaseImageChangeCount();
						showProp(prop);
						showBackgroundEditor();
					}
					@Override
					public void myUndo() {
						prop.setImage(oldImage);
						prop.setImageName(oldImageName);
						prop.decreaseImageChangeCount();
						showProp(prop);
						showBackgroundEditor();
					}
				};
			}
		};
		bgEditor.getContentPane().setBackground(Utils.STORYTELLER_RIGHT_COLOR);
		bgEditor.setLocationRelativeTo(swat.getMyFrame());
		return bgEditor; 
	}

	public static abstract class Test {
		public static void setOwner(PropEditor pe,String actor){
			pe.setPropOwner(pe.dk.getActorIndex(actor));
		}
	} 
	
/*	private static class VerticalLine extends JPanel {
		private static final long serialVersionUID = 1L;
		private Color light;
		public VerticalLine(){
			Dimension d=new Dimension(1,10000);
			UIDefaults table = UIManager.getLookAndFeelDefaults();
			light=table.getColor("TextField.shadow");
			setMaximumSize(d);
		}
		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(light);
			g.drawLine(0, 0, 0, getHeight()-0);
		}
	};*/
}
