package com.storytron.swat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

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
import com.storytron.swat.util.UndoableSlider;
import com.storytron.swat.verbeditor.OperatorMenu.NonOverlappedPopupMenu;
import com.storytron.uber.Actor;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Script;
import com.storytron.uber.Stage;
import com.storytron.uber.TextTrait;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.OperatorDictionary;

/**
 * A editor for stages.
 * Layouts are set in {@link #setLayouts()}. Widgets are initialized
 * in {@link #initWidgets()}.
 */
public final class StageEditor {
	private static final long serialVersionUID = 1L;
	private static DropDown unwelcoming_HomeyBox;
	private DropDown selectionBox;
	private static Deikto dk;
	private static Swat swat;
	private JPanel myPanel;
	private int iActor;
	private JCheckBox doorOpenCheckBox;
	private MapPanel mapPanel;
	private CustomTraitsControl<Stage> customTraits;
	private CustomTextTraitsControl<Stage> customTextTraits;
	private CustomPTraitsControl<Stage> customPTraits;
	private Swat.Slider unwelcoming_HomeySlider;
	private Stage mStage;
	private Actor mActor = null;
	private JComboBox actor2Box;
	private KnowsMenu knowsMenu = new KnowsMenu();
	private JButton addButton = new AddButton("Stage");
	private DeleteButton deleteButton = new DeleteButton("Stage");
	private ScaleIndicator scaleIndicator = new ScaleIndicator();
	private JButton backgroundInformationButton = new JButton("Background information");
	private JPanel zcPanel=new JPanel();
	private boolean userInput = true; 
	private static Color lightBackground = new Color(248, 236, 255);
	private EditorListener selectionEditorListener;
	private BackgroundEditor bgEditor;
	private JLabel stageLabel = new JLabel();
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
					final Stage stage = mStage;
					final int iActor = dk.getActorIndex(selectedItem.getText()); 
					final boolean newValue = selectedItem.isSelected();
					stage.setKnowsMe(dk.getActor(iActor),newValue);
					showStage(stage);
					new UndoableAction(swat,false,"change Knows Me for "+selectedItem.getText()){
						private static final long serialVersionUID = 1L;
						public void myRedo(){
							stage.setKnowsMe(dk.getActor(iActor),newValue);
							showStage(stage);
							myPopup.showPopup();
						}
						public void myUndo(){
							stage.setKnowsMe(dk.getActor(iActor),!newValue);
							showStage(stage);
							myPopup.showPopup();
						}
					};				

				}
			};
		}
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++		
		public void loadKnowsMenu() {
			if (mStage==null)
				return;
			
			myPopup.removeAll();
			for (int i=0; (i < dk.getActorCount()); ++i) {
				Actor ac = dk.getActor(i);
				JCheckBoxMenuItem zCheckBoxMenuItem = new JCheckBoxMenuItem(ac.getLabel());
				zCheckBoxMenuItem.addActionListener(myActionListener);
				zCheckBoxMenuItem.setState(mStage.getKnowsMe(dk.getActor(i)));
				myPopup.add(zCheckBoxMenuItem);
			}
		}
	}
//**********************************************************************	
	public StageEditor(Swat tSwat) {
		dk = null;
		swat = tSwat;
	    // See bug 469 to find out why we instantiate this here.
		bgEditor = createBackgroundEditor();
		myPanel = new JPanel();
        initWidgets();
		setLayouts();
		
		doorOpenCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If unchecked, no Actors may enter or leave this Stage."));
		backgroundInformationButton.setToolTipText(Utils.toHtmlTooltipFormat("What the players will see when they look at the \"Places\" display."));
		knowsMenu.setToolTipText(Utils.toHtmlTooltipFormat("Checked Actors start the story knowing the values of the Traits of this Stage."));
		mapPanel.setToolTipText(Utils.toHtmlTooltipFormat("Click and drag Stages to move them around."));
	}
	public void init(Deikto tdk){
		dk=tdk;
		customTraits.init(dk);
		customTextTraits.init(dk);
		reloadVisibilityCBs();
		bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Stage));

		if (dk.getStageCount()>1) mStage=dk.getStage(1);
		else mStage=null;
		if (dk.getActorCount()>1) mActor = dk.getActor(1);
		else mActor=null;
		loadSelectionBox(mStage);
		loadUnwelcoming_HomeyBox();
		onStageAddOrRemove();
	}

	private ErrorPopup errorPopup=new ErrorPopup();
	/**
	 * Method for initializing widgets.
	 * Layout stuff goes in {@link #setLayouts()}. 
	 */
	private void initWidgets(){
		iActor = 1;

		actor2Box = new JComboBox();
		actor2Box.setBackground(Color.white);
		actor2Box.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!userInput) return;
				showStage(mStage,(Actor)actor2Box.getSelectedItem());
			}
		});
		
		customTraits = new CustomTraitsControl<Stage>(swat,Deikto.TraitType.Stage){
			@Override
			protected Stage getEditedEntity() {	return mStage; }
			@Override
			protected float getValue(Stage e, FloatTrait t) { return e.getTrait(t); }
			@Override
			protected void setValue(Stage e, FloatTrait t, float value) {
				e.setTrait(t,value);
			}
			@Override
			protected void showEditedEntity(Stage e) { showStage(e); }
			
			@Override
			protected void onTraitChange(){
				reloadVisibilityCBs();
				customPTraits.reloadTraits(); 
				customPTraits.refreshValues();
				if (bgEditor.isVisible()){
					bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Stage));
					reloadBGEditorTraitValues();
				}
				myPanel.validate();
			}
		};
		
		customTextTraits = new CustomTextTraitsControl<Stage>(swat,Deikto.TraitType.Stage){
			@Override
			protected Stage getEditedEntity() {	return mStage; }
			@Override
			protected String getValue(Stage e, TextTrait t) { return e.getText(t); }
			@Override
			protected void setValue(Stage e, TextTrait t, String value) {
				e.setText(t,value);
				if (swat.verbEditor.sentenceDisplayEditor.isVisible())
					swat.verbEditor.sentenceDisplayEditor.refresh();
			}
			@Override
			protected void showEditedEntity(Stage e) { showStage(e); }
			@Override
			protected void onTraitChange() {
				customTextTraits.getTextFieldsPanel().revalidate();
				customTextTraits.getTextFieldsPanel().repaint();
				if (swat.verbEditor.sentenceDisplayEditor.isVisible())
					swat.verbEditor.sentenceDisplayEditor.refresh();
			}
		};

		mapPanel = new MapPanel();
		
		selectionBox = new DropDown(Deikto.MAXIMUM_FIELD_LENGTH){
			private static final long serialVersionUID = 1L;
			@Override
			public String getTooltipText(Object o) {
				return Utils.toHtmlTooltipFormat(((Stage)o).getDescription());
			}
			@Override
			public void indexMoved(final int from,final int to){
				final Stage stage = mStage;
				new UndoableAction(swat,"reorder stages"){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						dk.moveStage(from+1,to+1);
						mStage=stage;
						refresh();
						selectionBox.showPopup();
					}
					@Override
					public void myUndo() {
						dk.moveStage(to+1,from+1);
						mStage=stage;
						refresh();
						selectionBox.showPopup();
					}
				};
			}
		};
		selectionBox.setEditable(true);
		selectionBox.setAllowReordering(true);
		selectionEditorListener=new Swat.DropDownListener(selectionBox) {
			private static final long serialVersionUID = 1L;
			public boolean timedActionPerformed(ActionEvent e) {
				if (e!=null && mStage!=null && (e.getActionCommand()==null||e.getActionCommand().equals("permit")))
				if (selectionBox.isListPicking())
					showStage((Stage)selectionBox.getSelectedItem());
				else { // if not listPicking, assume the storybuilder
					// is trying to rename the current prop.
					String zLabel = ((String)selectionBox.getTextComponent().getJTextComponent().getText()).trim();
					if (zLabel==null || mStage==null || mStage.getLabel().equals(zLabel))
						return true;
					
					String s=swat.dk.nameExists(zLabel);
					if (s!=null) {
						errorPopup.showError(swat.getMyFrame(),selectionBox.getLocationOnScreen(),s);
						return false;
					}

					final Stage stage = mStage;
					final String newValue = zLabel;
					final String oldValue = stage.getLabel();
					new UndoableAction(swat,"rename stage"){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							stage.setLabel(newValue);							
							loadSelectionBox(stage);
							showStage(stage);
						}
						@Override
						public void myUndo() {
							stage.setLabel(oldValue);
							loadSelectionBox(stage);
							showStage(stage);
						}
					};
				}
				return true;
			}
			@Override
			public String getText() { return mStage!=null?mStage.getLabel():""; }
		};
		selectionBox.addActionListener(selectionEditorListener);
		
		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteSelectedStage();
			}
		});
		
		backgroundInformationButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				showBackgroundEditor();
			}
		});
		
		doorOpenCheckBox = new JCheckBox("doorOpen");
		doorOpenCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!userInput) return;
				final Stage stage = mStage;
				final boolean newValue = doorOpenCheckBox.isSelected();
				new UndoableAction(swat,"change doorOpen of "+mStage.getLabel()){
					private static final long serialVersionUID = 1L;
					public void myRedo(){
						stage.setDoorOpen(newValue);
						showStage(stage);
					}
					public void myUndo(){
						stage.setDoorOpen(!newValue);
						showStage(stage);	
					}
				};				
			}
		});

		unwelcoming_HomeyBox = new DropDown(100){
			private static final long serialVersionUID = 1L;
			@Override
			public String getTooltipText(Object o) {
				return Utils.toHtmlTooltipFormat(dk.getActor((String)o).getDescription());
			}	
		};
		unwelcoming_HomeyBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!userInput) return;
				iActor = unwelcoming_HomeyBox.getSelectedIndex()+1;
				unwelcoming_HomeySlider.mSetValue(toSlider(mStage.getUnwelcoming_Homey(dk.getActor(iActor)))); 	
			}
		});

		unwelcoming_HomeySlider = new Swat.Slider(JSlider.HORIZONTAL, 0, 100, 50);
		unwelcoming_HomeySlider.setMajorTickSpacing(50);
		unwelcoming_HomeySlider.setMinorTickSpacing(10);
		unwelcoming_HomeySlider.setPaintTicks(true);		
		new UndoableSlider(swat,unwelcoming_HomeySlider) {
			Stage stage;
			int iActor;
			@Override
			public int init() {
				stage = mStage;				 
				iActor = StageEditor.this.iActor;
				return toSlider(stage.getUnwelcoming_Homey(dk.getActor(iActor)));
			}
			@Override
			public void setValue(int value) {
				stage.setUnwelcoming_Homey(dk.getActor(iActor),fromSlider(value));
			}
			@Override
			public void undoRedoExecuted() {
				showStage(stage);
				unwelcoming_HomeyBox.setSelectedIndex(iActor-1);
			}			
			@Override
			public String getPresentationName(){
				return "change unwelcoming_Homey for "+dk.getActor(iActor).getLabel();
			}
		};
		
		customPTraits = new CustomPTraitsControl<Stage>(swat,Deikto.TraitType.Stage){
			private static final long serialVersionUID = 1L;
			@Override
			protected float getCValue(Actor e, FloatTrait t, Stage other) {
				return e.getC(t, other);
			}

			@Override
			protected Actor getEditedActor() {	return mActor; }

			@Override
			protected Stage getOtherEntity() { return mStage; }

			@Override
			protected boolean isOverrided(Actor e, FloatTrait t, Stage other) {
				return e.isOverrided(t, other);
			}

			@Override
			protected float getPValue(Actor e, FloatTrait t, Stage other) {
				return e.getP(t, other);
			}

			@Override
			protected void setCValue(Actor e, FloatTrait t, Stage other, Float value) {
				e.setC(t, other, value);
				if (bgEditor.isVisible() && e == dk.getActor(1))
					reloadBGEditorTraitValues();
			}

			@Override
			protected void setPValue(Actor e, FloatTrait t, Stage other, Float value) {
				e.setP(t, other, value);
				if (bgEditor.isVisible() && e == dk.getActor(1))
					reloadBGEditorTraitValues();
			}

			@Override
			protected void showEditedActor(Actor e, Stage other) {
				showStage(other,e);
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
				return "Check this box to have the system automatically set the perceived value of this"
						+" trait to the correct value if KnowsMe is checked, and to zero if KnowsMe is"
						+" unchecked.";
			}
		};
	}

//	----------------------------------------------------------------------
	/**
	 * Method for laying out the widgets.
	 * Widget initialization stuff goes in {@link #initWidgets()}. 
	 */
	private void setLayouts(){
		JComponent auxMapPanel = Box.createHorizontalBox();
		auxMapPanel.add(mapPanel);
		auxMapPanel.add(Box.createHorizontalGlue());
		
		JPanel outerMapPanel = new JPanel();
		outerMapPanel.setOpaque(false);
		outerMapPanel.setLayout(new BorderLayout());
		outerMapPanel.add(auxMapPanel,BorderLayout.CENTER);
		outerMapPanel.validate();

		JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.X_AXIS));
		lowerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		lowerPanel.setOpaque(false);
		selectionBox.setBackground(Color.white);	
		selectionBox.setMaximumRowCount(35);
		selectionBox.setAlignmentY(0.0f);
		Dimension d = selectionBox.getPreferredSize();
		selectionBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,d.height));
		lowerPanel.add(selectionBox);
		
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i=0;
				while(dk.getStageIndex(i==0?"new stage":"new stage"+i)!=-1) i++;
				addStage(i==0?"new stage":"new stage"+i);
				selectionBox.getJTextComponent().requestFocusInWindow();
				selectionBox.getJTextComponent().selectAll();
			}
		});
		addButton.setAlignmentY(0.0f);
		lowerPanel.add(addButton);
		deleteButton.setAlignmentY(0.0f);
		lowerPanel.add(deleteButton);

		Dimension db=addButton.getPreferredSize();
		db.height=selectionBox.getPreferredSize().height;
		addButton.setPreferredSize(db);
		deleteButton.setPreferredSize(db);

		JPanel selectionPanel = new JPanel();
		selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
		selectionPanel.setOpaque(false);
		JLabel mStageLabel = new JLabel("Stage to edit");
		mStageLabel.setAlignmentX(0.5f);
		selectionPanel.add(mStageLabel);
		selectionPanel.add(lowerPanel);

		JPanel unwelcoming_HomeyPanel = new JPanel();
		unwelcoming_HomeyPanel.setLayout(new BoxLayout(unwelcoming_HomeyPanel, BoxLayout.Y_AXIS));
		unwelcoming_HomeyPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(5, 0, 5, 0),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder("Unwelcoming_Homey"),
						BorderFactory.createEmptyBorder(3,3,3,3)
					)
				)
		);
		unwelcoming_HomeyPanel.setToolTipText(Utils.toHtmlTooltipFormat("The frequency with which each Actor prefers to go to this Stage."));
		unwelcoming_HomeyPanel.setOpaque(false);
		unwelcoming_HomeyBox.setBackground(Color.white);		
		unwelcoming_HomeyPanel.add(unwelcoming_HomeyBox);
		unwelcoming_HomeySlider.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		unwelcoming_HomeySlider.setOpaque(false);
		unwelcoming_HomeyPanel.add(unwelcoming_HomeySlider);

		ZoomScrollControl zc=new ZoomScrollControl(1.0);
		zc.addZoomScrollListener(mapPanel);
		zc.addZoomScrollListener(new ZoomScrollListener() {
			public void zoomScrollChanged(double scale, double x, double y) {
				scaleIndicator.setScale((float)(scale*200/758));
				scaleIndicator.repaint();				
			}
		});
		zc.setAlignmentX(0.5f);
		zcPanel.setToolTipText(Utils.toHtmlTooltipFormat("Click and drag the corners inward to zoom in the main map."));
		zcPanel.setLayout(new BoxLayout(zcPanel,BoxLayout.Y_AXIS));			
		zcPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLoweredBevelBorder(),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		zcPanel.add(scaleIndicator);
		zcPanel.add(zc);
		zcPanel.setBackground(new Color(232, 224, 255));

		visibilityCBPanel = new JPanel(null);
		visibilityCBPanel.setLayout(new BoxLayout(visibilityCBPanel,BoxLayout.Y_AXIS));
		visibilityCBPanel.setBackground(Utils.darkColumnBackground);
		visibilityCBPanel.setMaximumSize(new Dimension(20,Integer.MAX_VALUE));
		
		JComponent traitsPanel = Box.createHorizontalBox();
		customTraits.getSlidersPanel().setAlignmentY(0.0f);
		traitsPanel.add(Box.createHorizontalGlue());
		traitsPanel.add(customTraits.getSlidersPanel());
		visibilityCBPanel.setAlignmentY(0.0f);
		traitsPanel.add(visibilityCBPanel);
		
		final JScrollPane sp = new JScrollPane(traitsPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.getViewport().setBackground(lightBackground);
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
		
		JComponent coreStageTraits=Box.createVerticalBox();
		coreStageTraits.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Core Stage Traits"),
				BorderFactory.createEmptyBorder(3,3,3,3)
				)
		);
		customTraits.getAddButton().setAlignmentX(1.0f);
		coreStageTraits.add(customTraits.getAddButton());
		sp.setAlignmentX(1.0f);
		coreStageTraits.add(sp);
		coreStageTraits.setMaximumSize(new Dimension(coreStageTraits.getPreferredSize().width,Integer.MAX_VALUE));


		JComponent textTraits = Box.createHorizontalBox();
		textTraits.add(customTextTraits.getTextFieldsPanel());
		
		JScrollPane textTraitScroll = new JScrollPane(textTraits,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		textTraitScroll.getViewport().setBackground(lightBackground);
		textTraitScroll.getVerticalScrollBar().setUnitIncrement(20);
		
		JComponent textTraitsPanel = Box.createHorizontalBox();
		textTraitsPanel.add(new JLabel("Text Traits"));
		textTraitsPanel.add(new LineBox(2,Swat.shadow));
		textTraitsPanel.add(customTextTraits.getAddButton());
		
		ComponentLabeledPanel customTextTraitsPanel = new ComponentLabeledPanel(textTraitsPanel,Swat.shadow,false);
		customTextTraitsPanel.setOpaque(false);
		customTextTraitsPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		customTextTraitsPanel.add(textTraitScroll);

		
		doorOpenCheckBox.setOpaque(false);
		doorOpenCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		doorOpenCheckBox.setAlignmentX(0.5f);

		unwelcoming_HomeyPanel.setAlignmentX(0.5f);
		unwelcoming_HomeyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,unwelcoming_HomeyPanel.getPreferredSize().height));

		Box stateTraitsPanel = Box.createVerticalBox();
		stateTraitsPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("State Traits"),
				BorderFactory.createEmptyBorder(0,5,0,5))
				);
		doorOpenCheckBox.setAlignmentX(0.0f);
		stateTraitsPanel.add(doorOpenCheckBox);		
		knowsMenu.setAlignmentX(0.0f);
		stateTraitsPanel.add(knowsMenu);
		unwelcoming_HomeyPanel.setAlignmentX(0.0f);
		stateTraitsPanel.add(unwelcoming_HomeyPanel);		
		
		Box stageLeftPanel = Box.createVerticalBox();
		stageLeftPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		selectionPanel.setAlignmentX(0.5f);
		stageLeftPanel.add(selectionPanel);
		backgroundInformationButton.setAlignmentX(0.5f);
		stageLeftPanel.add(backgroundInformationButton);
		stageLeftPanel.add(Box.createRigidArea(new Dimension(5,5)));
		stateTraitsPanel.setAlignmentX(0.5f);
		stageLeftPanel.add(stateTraitsPanel);
		customTextTraitsPanel.setAlignmentX(0.5f);
		stageLeftPanel.add(customTextTraitsPanel);
	
		Box actor2LabelPanel = Box.createHorizontalBox();
		actor2LabelPanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
		actor2LabelPanel.add(actor2Box);
		actor2LabelPanel.add(new JLabel(" thinks of "));
		actor2LabelPanel.add(stageLabel);
		actor2LabelPanel.add(Box.createHorizontalGlue());

		
		JComponent perceivedStageTraisPanel = Box.createVerticalBox();
		perceivedStageTraisPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Perceived Stage Traits"),
				BorderFactory.createEmptyBorder(3,3,3,3)
				)
		);
		actor2LabelPanel.setAlignmentX(0.0f);
		perceivedStageTraisPanel.add(actor2LabelPanel);
		customPTraits.setAlignmentX(0.0f);
		//customPTraits.setPreferredSize(new Dimension(200,10));
		perceivedStageTraisPanel.add(customPTraits);
		
		Box bottomPanel = Box.createHorizontalBox();
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		bottomPanel.add(zcPanel);
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(coreStageTraits);
		bottomPanel.add(perceivedStageTraisPanel);
		bottomPanel.setMinimumSize(new Dimension(10,240));
		bottomPanel.setPreferredSize(new Dimension(10,240));
		
    	JPanel centerPanel = new JPanel(new BorderLayout());
    	centerPanel.setOpaque(false);
    	centerPanel.add(outerMapPanel,BorderLayout.CENTER);
    	centerPanel.add(stageLeftPanel,BorderLayout.EAST);    	
		
    	myPanel.setOpaque(true);
    	myPanel.setBackground(lightBackground);
    	myPanel.setLayout(new BorderLayout());
    	myPanel.add(BorderLayout.CENTER, centerPanel);
    	myPanel.add(BorderLayout.SOUTH, bottomPanel);
		zc.fireZoomScrollEvent();
	}
	
	
	/**
	 * Implements a Map display where you can place and drag stages.
	 * Stages are gathered from the editor in order to 
	 * display them. Note that this class implements {@link ZoomScrollListener}
	 * so it can be hooked to a ZoomScrollControl.
	 */
	private class MapPanel extends JPanel implements ZoomScrollListener {
		private static final long serialVersionUID = 1L;
		int movingStage = 0;
		double distance = 0.0d;
		boolean mousein = false;
		int mousex = 0, mousey = 0, oldmousex = 0, oldmousey = 0;
		double viewx=0,viewy=0;
		double scale=1.0;
		private final double WORLD_MAX_COORD=1.0;
		private final static int MAP_WIDTH = 790;
		private final static int MAP_HEIGHT = (int)(MAP_WIDTH*0.6f);
//	----------------------------------------------------------------------
	public MapPanel() {
		setMaximumSize(new Dimension(MAP_WIDTH,MAP_HEIGHT));
		setMinimumSize(new Dimension(MAP_WIDTH,MAP_HEIGHT));
		setPreferredSize(new Dimension(MAP_WIDTH,MAP_HEIGHT));
		
		MouseInputAdapter ma = new MouseInputAdapter() {
			float oldX, oldY;
			@Override
			public void mouseEntered(MouseEvent e) {
				mousein=true;				
			}
			@Override
			public void mouseExited(MouseEvent e) {
				mousein=false;
				repaint();
			}
			@Override
            public void mousePressed(MouseEvent e) {
            	int x, y, mx, my;
    			mx = e.getX();
    			my = e.getY();
	  			for (int i = 1; (i < dk.getStageCount()); ++i) {
	  				x = world2viewX(((Stage)dk.getStage(i)).getXCoord());
	  				y = world2viewY(((Stage)dk.getStage(i)).getYCoord());
	  				if ((Math.abs(mx-x)<4) & (Math.abs(my-y)<4)) {
	  					movingStage = i;
	  					oldX = dk.getStage(movingStage).getXCoord();
	  					oldY = dk.getStage(movingStage).getYCoord();
	  					break;
	  				}
	  			}
	  			repaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
            	if (movingStage>0) {
            		final Stage stage = dk.getStage(movingStage); 
            		final float oldx = oldX;
            		final float oldy = oldY;
            		final float newx = stage.getXCoord();
            		final float newy = stage.getYCoord();
            		new UndoableAction(swat,false,"move stage "+stage.getLabel()){
         				private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							stage.setXCoord(newx);
			    			stage.setYCoord(newy);
			    			repaint();
						}
						@Override
						public void myUndo() {
							stage.setXCoord(oldx);
							stage.setYCoord(oldy);
							repaint();
						}
            		};
            		repaint();
            		movingStage = 0;
            	}
            }
            @Override
            public void mouseDragged(MouseEvent e) {
		      	int width, height, ix, iy;
		      	if (movingStage > 0) {
		    			height = (int)getSize().getHeight();
		    			width = (int)getSize().getWidth();
		    			ix = e.getX();
		    			if (ix > width)
		    				ix = width;
		    			if (ix < 0)
		    				ix = 0;
		    			iy = e.getY();
		    			if (iy > height)
		    				iy = height;
		    			if (iy < 0)
		    				iy = 0;
		    			dk.getStage(movingStage).setXCoord((float)fitInterval(-1.0,1.0,view2worldX(ix)));
		    			dk.getStage(movingStage).setYCoord((float)fitInterval(-1.0,1.0,view2worldY(iy)));
		    			repaint();
		      	}
		      	mouseMoved(e);
            }
            @Override
            public void mouseMoved(MouseEvent e) {	    	  
            	mousex=e.getX();
            	mousey=e.getY();
            	calculateDistance();
		      }
		};
		addMouseListener(ma);
		addMouseMotionListener(ma);
	   	calculateDistance();
	   }
	   private void calculateDistance() {
		   if (mStage!=null) {    	
			   distance = mStage.getTravelingTime(view2worldX(mousex),view2worldY(mousey));
			   if (mousein) repaint(Math.min(mousex,oldmousex),Math.min(mousey,oldmousey),
					   				Math.abs(mousex-oldmousex)+75,Math.abs(mousey-oldmousey)+75);
		  }
	   } 
//	----------------------------------------------------------------------
	   private double fitInterval(double min,double max,double v){
		   return Math.max(min,Math.min(v,max));
	   }	   
//	----------------------------------------------------------------------
		public void paintComponent(Graphics g) {
			int x, y;
			
			g.setColor(Color.pink);
			g.fillRect(0, 0, getWidth(), getHeight());
			validate();
			for (int i = 1; (i < dk.getStageCount()); ++i) {
				// Transform real coords to view coords
				x = world2viewX(((Stage)dk.getStage(i)).getXCoord());			
				y = world2viewY(((Stage)dk.getStage(i)).getYCoord());
				if (dk.getStage(i) == mStage)
					g.setColor(Color.black);
				else
					g.setColor(Color.white);
				g.fillOval(x-2, y-2, 4, 4);
				g.drawString(((Stage)dk.getStage(i)).getLabel(), x + 4, y);
			}
			if (mousein && mStage!=null) {
				g.setColor(Color.black);
				g.drawString(""+(int)distance+"'",mousex+15,mousey+30);
				oldmousex=mousex;oldmousey=mousey;
			}
		}
		/**
		 * Tells the horizontal coordinate of the display that matches 
		 * a horizontal coordinate of the world. 
		 * */
		private int world2viewX(double x) { 
			return (int)(MAP_WIDTH/2.0 * ((x-viewx)/scale/WORLD_MAX_COORD+1.0)); 
		}
		/**
		 * Same as before for vertical coordinates. 
		 * */
		private int world2viewY(double y) { 
			return (int)(MAP_WIDTH/2.0 * ((y-viewy)/scale/WORLD_MAX_COORD+1.0)); 
		}
		/**
		 * Inverse of <code>world2viewX</code>.
		 * */
		private double view2worldX(int x) { 
			return (x * 2.0 / MAP_WIDTH-1) * scale * WORLD_MAX_COORD + viewx;
		}
		/**
		 * Inverse of <code>world2viewY</code>.
		 * */
		private double view2worldY(int y) { 
			return (y * 2.0 / MAP_WIDTH - 1 ) * scale * WORLD_MAX_COORD + viewy;
		}
		public void zoomScrollChanged(double sc, double x, double y) {
			this.scale=sc;
			this.viewx=-WORLD_MAX_COORD+2*WORLD_MAX_COORD*x;
			this.viewy=-WORLD_MAX_COORD+2*WORLD_MAX_COORD*y;
			repaint();
		}
   }

	private enum Corner {SW,NW,NE,SE,NONE};
	
	/**
	 * <p>A class for scrolling and zooming. 
	 * It has a minimap representing the whole world as a big square.
	 * Inside there's a (sometimes) little square rectangle which can
	 * be resized and moved inside the outer one. The inner square 
	 * represents the current view of the world.</p>
	 * 
	 * <p>
	 *    To pass events to a display whenever the user zooms or scroll,
	 *    the display must register a listener using the <code>addZoomScrollListener</code>
	 *    method. 
	 * </p>
	 */
	private class ZoomScrollControl extends JPanel {
		private static final long serialVersionUID = 1L;
		// The inner square
		private JComponent box = new JPanel(null);
		// The square representing the world.
		private int oldmx,oldmy, xIni, yIni, wIni, hIni;
		// Range limits for zooming
		private double minscale=0.1;
		private double maxscale=1.0;
		// scale ranges in the interval [minscale, maxscale]
		// invariant: scale*(minimap.getWidth()-2) == box.getWidth()
		// We substract 2 of the minimap width because of its 1 pixel border 
		// that is considered outside of the world.
		private boolean isScrolling=false;
		private Corner c = Corner.NONE;
		
		private LinkedList<ZoomScrollListener> listeners=new LinkedList<ZoomScrollListener>();
		
		/**
		 * Creates a ZoomScroll control centered on the world and
		 * with the zoom specified by <code>initialScale</p>. 
		 */
		public ZoomScrollControl(double initialScale){
			super(null);
			setLayouts(initialScale);
			addMouseListener(new MouseAdapter() {
				// On this event we save the initial position of the mouse.
				// We need to drag the inner square in the same direction
				// the mouse moves. 
				@Override
				public void mousePressed(MouseEvent e) {
					oldmx=e.getX();oldmy=e.getY();
					xIni=box.getX();
					yIni=box.getY();
					wIni=box.getWidth();
					hIni=box.getHeight();
					isScrolling=boxContains(oldmx,oldmy);
					// Find if the press was on a corner. 
					if (isScrolling) { c=getCorner(oldmx,oldmy); }
				}
				@Override
				public void mouseReleased(MouseEvent e) {
					isScrolling=false;
				}
			});
			addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					// exit if the mouse was not pressed over the inner square 
					if (!isScrolling) return;

					moveBox(e.getX(),e.getY());
					fireZoomScrollEvent();

					oldmx=e.getX();
					oldmy=e.getY();					
					// This a trick to have the inner square behave properly
					// when the mouse goes outside of the minimap.
					// We don't want the box run away when the mouse turns back. 
					if (oldmx<1) 
						oldmx=1; 
					else if (getWidth()-1<oldmx) 
						oldmx=getWidth()-1;
					if (oldmy<1) 
						oldmy=1; 
					else if (getHeight()-1<oldmy) 
						oldmy=getHeight()-1;	
				}				
			});
		}
		
		/**
		 * Layouts control components.  
		 * Widget initialization stuff goes in {@link ZoomScrollControl#ZoomScrollControl(double)}.
		 */
		private void setLayouts(double scale){
			setSize(280,190);
			setMinimumSize(getSize());
			setPreferredSize(getSize());
			setMaximumSize(getSize());
			setBorder(BorderFactory.createLineBorder(Color.black));
			setBackground(Color.pink);
			add(box);

			box.setBorder(BorderFactory.createLineBorder(Color.white,4));
			int w=getWidth()-2;
			int h=getHeight()-2;
			box.setLayout(new BorderLayout());
			box.setBounds((int)(1+w*(1-scale)/2),(int)(1+h*(1-scale)/2),(int)(w*scale),(int)(h*scale));
			box.setOpaque(false);

			JPanel c1=new JPanel(null);
			c1.setMaximumSize(new Dimension(3,3));
			c1.setPreferredSize(c1.getMaximumSize());
			c1.setBackground(Color.white);
			JPanel c2=new JPanel(null);
			c2.setMaximumSize(new Dimension(3,3));
			c2.setPreferredSize(c2.getMaximumSize());
			c2.setBackground(Color.white);
			JPanel boxLeftPanel=new JPanel(new BorderLayout());
			boxLeftPanel.setOpaque(false);
			boxLeftPanel.add(c1,BorderLayout.NORTH);
			boxLeftPanel.add(c2,BorderLayout.SOUTH);
			
			JPanel c3=new JPanel(null);
			c3.setMaximumSize(new Dimension(3,3));
			c3.setPreferredSize(c3.getMaximumSize());
			c3.setBackground(Color.white);
			JPanel c4=new JPanel(null);
			c4.setMaximumSize(new Dimension(3,3));
			c4.setPreferredSize(c4.getMaximumSize());
			c4.setBackground(Color.white);
			JPanel boxRightPanel=new JPanel(new BorderLayout());
			boxRightPanel.setOpaque(false);
			boxRightPanel.add(c3,BorderLayout.NORTH);
			boxRightPanel.add(c4,BorderLayout.SOUTH);

			box.add(boxLeftPanel,BorderLayout.WEST);
			box.add(boxRightPanel,BorderLayout.EAST);
		}
		/**
		 * Tells if the (x,y) falls inside the inner square in minimap coordinates.
		 * */
		private boolean boxContains(int x,int y){
			return box.getX()<=x && x<box.getX()+box.getWidth() &&
					box.getY()<=y && y<box.getY()+box.getHeight();
		}
		/**
		 * Gets the corner of the box a given point is over.
		 * @param x horizontal coordinate of the point 
		 * @param y vertical coordinate of the point
		 * @return the corner under the point. 
		 */
		private Corner getCorner(int x,int y){
			if (Math.abs(box.getX()-x)<7 && Math.abs(box.getY()-y)<7)
				return Corner.NW;
			else if (Math.abs(box.getX()-x)<7 && Math.abs(box.getY()+box.getHeight()-y)<7)
				return Corner.SW;
			else if (Math.abs(box.getX()+box.getWidth()-x)<7 && Math.abs(box.getY()-y)<7)
				return Corner.NE;
			else if (Math.abs(box.getX()+box.getWidth()-x)<7 && Math.abs(box.getY()+box.getHeight()-y)<7)
				return Corner.SE;
			else return Corner.NONE;
		}
		/**
		 * Moves and resizes the box for a point (x,y).
		 * If there is no corner selected we drag the box using the
		 * point shift from the last mouse position ({@link #oldmx},{@link #oldmy}).
		 */
		private void moveBox(int x,int y){
			if ( c==Corner.NONE)
				// If there is not corner selected just move the inner square
				// the same as the mouse.
				box.setLocation(box.getX()+x-oldmx,box.getY()+y-oldmy);
			else {			
				final int w=wIni, h=hIni;
				// Translate modifying vector from any corner to top left
				// This is a trick to write the following code once
				// as if the box was always dragged from the top left corner. 
				switch (c){
				case NW:
					x-=xIni;y-=yIni;
					break;
				case SW:
					int t=x-xIni;x=-(y-(yIni+h));y=t;
					break;
				case SE:
					x=-(x-(xIni+w));y=-(y-(yIni+h));
					break;
				case NE:
					t=-(x-(xIni+w));x=y-yIni;y=t;
					break;
				default:;
				}
				double d = (x*w+y*h)/((double)(w*w+h*h)); // length of projection of the corner to mouse vector on the box diagonal
				//int d=(x+y)/2; // length of projection of the corner to mouse vector on the box diagonal
				// Clip the projection if the inner square cannot be further reduced.
				if (2*d*h>h-(getHeight()-2)*minscale) 
					d=((double)(h-(getHeight()-2)*minscale))/(2*h);
				int nx = (int)(d*w);
				int ny = (int)(d*h);
				box.setBounds(xIni+nx,yIni+ny,w-2*nx,h-2*ny);
			}
			
			// Make sure the inner square is inside of the minimap.
			fitBox();			
		}
		/**
		 * This is called whenever the inner square is resized or moved to
		 * ensure the inner square is inside the minimap.
		 * */
		private void fitBox(){
			box.setSize((int)Math.min((getWidth()-2)*maxscale,box.getWidth()),
					    (int)Math.min((getHeight()-2)*maxscale,box.getHeight()));
			box.validate();

			int lx=box.getX(), ly=box.getY();
			// Check for collision with borders
			if (lx<1) lx=1;
			else if (lx+box.getWidth()>getWidth()-1)
				lx=getWidth()-1-box.getWidth();
			if (ly<1) ly=1;
			else if (ly+box.getHeight()>getHeight()-1) 
				ly=getHeight()-1-box.getHeight();
			box.setLocation(lx,ly);
		}
		public void addZoomScrollListener(ZoomScrollListener l){
			listeners.add(l);
		}
		public void fireZoomScrollEvent(){
			double scale=(double)box.getWidth()/(getWidth()-2);
			double x=(box.getX()+(double)box.getWidth()/2)/(getWidth()-2);
			double y=(box.getY()+(double)box.getHeight()/2)/(getHeight()-2);
			for(ZoomScrollListener zsl :listeners) 
				zsl.zoomScrollChanged(scale,x,y);
		}
	}
	/**
	 * Interface for handling of listeners of the ZoomScrollControl
	 * */
	interface ZoomScrollListener {
		/**
		 * Tells the listener the new scale and position.
		 * @param scale the scale is 1:sc, meaning that the 
		 *              display width and height represents sc
		 *              of its maximum size. 
		 * @param x fraction of the world width where the view center is.
		 * @param y fraction of the world height where the view center is.
		 * */
		void zoomScrollChanged(double scale,double x,double y);
	}  
	/**
	 * Implements a bar which describes the current scale of the view.
	 * The only thing there is to do with this class is setting
	 * the scale to show {@link #setScale(float)}. 
	 */
	class ScaleIndicator extends JPanel {
		private static final long serialVersionUID = 1L;
		private float scale=1.0f;
		private final int x=10;
		private final int h=5;

		public ScaleIndicator(){
			super(null);
			setSize(200,30);
			setPreferredSize(getSize());
			setMinimumSize(getSize());
			setMaximumSize(getSize());
			setOpaque(false);
		}
		/**
		 * This sets the scale. 
		 * @param scale is the amount of units that each pixel represents.
		 * */
		public void setScale(float scale) {
			this.scale = scale;
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			int totalW = getWidth()-30;
			int y=getHeight()-h-5;
			g.setColor(Color.black);
			g.fillRect(x,y,totalW/4,h);

			g.setColor(Color.white);
			g.fillRect(x+totalW/4+1,y+1,totalW/4,h-2);

			g.setColor(Color.black);
			g.fillRect(x+totalW/4*2,y,totalW/4,h);

			g.setColor(Color.white);
			g.fillRect(x+totalW/4*3,y+1,totalW/4,h-2);

			g.setColor(Color.black);
			g.drawRect(x+totalW/4-1,y,totalW/4+2,h-1);
			g.drawRect(x+totalW/4*3-1,y,totalW/4+1,h-1);
			
			String s0="0'"; 
			String s1=""+(int)(scale*(totalW/4*2))+"'";
			String s2=""+(int)(scale*(totalW/4*4))+"'";
			g.drawString(s0,x-g.getFontMetrics().stringWidth(s0)/2,y-7);
			g.drawString(s1,x+totalW/4*2-g.getFontMetrics().stringWidth(s1)/2,y-7);
			g.drawString(s2,x+totalW/4*4-g.getFontMetrics().stringWidth(s2)/2,y-7);
		}		
	} 
	
//----------------------------------------------------------------------
//**********************************************************************
	public void addStage(String tLabel) {
				
		final Stage stage = new Stage(tLabel);
		new UndoableAction(swat,"add stage "+tLabel){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				try {
					dk.addStage(stage);
				} catch (LimitException e) { throw new RuntimeException(e); }
				addStage(stage);
				showStage(stage);
			}
			@Override
			public void myUndo() {
				deleteStage(stage);
			}
		};
	}
	public void onStageAddOrRemove() {
		addButton.setEnabled(swat.dk.getStageCount()<swat.dk.limits.maximumStageCount);
		if (!addButton.isEnabled())
			addButton.setToolTipText("Cannot have more than "+dk.limits.maximumStageCount+" stages.");
		else
			addButton.setToolTipText("creates a new stage");
		
		deleteButton.setEnabled(swat.dk.getStageCount()>2);
		knowsMenu.setEnabled(swat.dk.getStageCount()>1);
		if (!knowsMenu.isEnabled())
			knowsMenu.myPopup.setVisible(false);
	}
	public void addStage(Stage stage) {
		stage.setXCoord((float)mapPanel.view2worldX(mapPanel.getWidth()/2));
		stage.setYCoord((float)mapPanel.view2worldY(mapPanel.getHeight()/2));
		loadSelectionBox(stage);
		onStageAddOrRemove();
		mapPanel.repaint();
		Swat.playSound("add.aiff");
	}
//**********************************************************************
	private void deleteSelectedStage() {
		if (mStage==null) return;
		
		final LinkedList<Script.Node> modifiedNodes = new LinkedList<Script.Node>();
		dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Node n) {
				if (n.getConstant() == mStage)
					modifiedNodes.add(n);
				return true;
			}
		});
		
		final Stage stage = mStage;
		final int iStage = mStage.getReference().getIndex();
		final float x = stage.getXCoord();
		final float y = stage.getYCoord();
		new UndoableAction(swat,"delete stage "+stage.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				deleteStage(stage);
			}
			@Override
			public void myUndo() {
				try {
					dk.addStage(iStage,stage);
				} catch (LimitException e) { throw new RuntimeException(e); }
				addStage(stage);
				stage.setXCoord(x);
				stage.setYCoord(y);
				for(Script.Node n:modifiedNodes)
					n.setOperatorValue(OperatorDictionary.getStageConstantOperator(),stage);
				showStage(stage);
			}
		};
	}
	public void deleteStage(Stage stage) {
		selectionBox.setActionCommand("block");
		selectionBox.getModel().removeElement(stage);
		selectionBox.setActionCommand("permit");
		
		dk.removeStage(mStage);
		if (dk.getStageCount()<3)
			deleteButton.setEnabled(false);
		swat.propEditor.loadLocationBox(-1);
		swat.actorEditor.loadLocationBox();	
		showStage(dk.getStageCount()>1?dk.getStage(1):null);
		mapPanel.repaint();
		onStageAddOrRemove();

		Swat.playSound("delete.aiff");
	}
//**********************************************************************	
	public void loadSelectionBox(Stage stage) {		
		selectionBox.setActionCommand("block");
		selectionBox.removeAllItems();
		for (int i = 1; (i < dk.getStageCount()); ++i) {
			selectionBox.addItem(dk.getStage(i));
		}
		if (stage!=null)
			selectionBox.setSelectedItem(stage);
		selectionBox.setActionCommand("permit");
	}
//**********************************************************************	
	public void loadUnwelcoming_HomeyBox() {
		userInput = false;
		unwelcoming_HomeyBox.removeAllItems();
		for (int i = 1; (i < dk.getActorCount()); ++i)
			unwelcoming_HomeyBox.addItem(((Actor)dk.getActor(i)).getLabel());
		if (dk.getActorCount()>1)
			unwelcoming_HomeyBox.setSelectedIndex(0);
		int rowCount = dk.getActorCount();
		if (rowCount > 20)
			rowCount = 20;
		unwelcoming_HomeyBox.setMaximumRowCount(rowCount);
		userInput = true;
	}
//**********************************************************************	
	public void refresh() {
		loadSelectionBox(mStage);
		loadUnwelcoming_HomeyBox();
		loadActor2Box();
		customPTraits.reloadTraits();
		
		showStage(mStage);
		myPanel.validate();
		myPanel.repaint();
	}
//**********************************************************************
	public void showStage(Stage stage) {
		showStage(stage,mActor);
	}
	public void showStage(Stage stage,Actor actor) {			
		userInput = false;
		mStage = stage;
		mActor = actor;
		mapPanel.repaint();
		if (mStage!=null){
			stageLabel.setText(mStage.getLabel());
			doorOpenCheckBox.setSelected(mStage.getDoorOpen());
			customTraits.refresh();
			customTextTraits.refresh();
			customPTraits.refreshValues();
			if (bgEditor.isVisible())
				showBackgroundEditor();
			
			unwelcoming_HomeySlider.mSetValue(toSlider(mStage.getUnwelcoming_Homey(dk.getActor(iActor))));
			selectionBox.setSelectedItem(mStage);
		}
		knowsMenu.loadKnowsMenu();
		if (mActor!=null)
			actor2Box.setSelectedItem(mActor);
		myPanel.validate();
		userInput = true;
	}
	
	/** 
	 * Routines for converting numbers from slider to model and the other 
	 * way around.  
	 * */
	private static float fromSlider(int v){ return (float)((v-50)/50.0*Utils.MAXI_VALUE); }
	private static int toSlider(float f){ return (int)(f*50/Utils.MAXI_VALUE+50); }

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
	public void rename() {
		String message = "New Stage Name:";
		boolean duplicateName;
		do {
			duplicateName = false;
			String zLabel = JOptionPane.showInputDialog(message, mStage.getLabel());
			for (Stage st:dk.getStages()) {
				if (st.getLabel().equals(zLabel) && mStage != st){
					duplicateName = true;
					break;
				}
			}
			if (duplicateName)
				message = "That name is already taken!";
			else if (zLabel != null) {
				mStage.setLabel(zLabel);
				loadSelectionBox(mStage);
				mapPanel.repaint();
			}
		} while (duplicateName);		
	}
//**********************************************************************
	public JPanel getMyPanel() {
		return myPanel;
	}
//**********************************************************************
	public Stage getStage() {
		return mStage;
	}
//**********************************************************************
	private void reloadVisibilityCBs(){
		for(final FloatTrait t:dk.getStageTraits()) {
			Component cb = visibilityCBs.get(t);
			if (cb!=null)
				continue;
			
			final JCheckBox vcb = new JCheckBox();
			vcb.setToolTipText("<html><b>Make visible</b><br>"+
				   "If this is checked, then actors will<br>"+
				   "perceive this trait in a Stage<br>" +
				   "every time they enter it.<html>");
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
								bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Stage));
								reloadBGEditorTraitValues();
							}
							((JComponent)vcb.getParent()).scrollRectToVisible(vcb.getBounds());
						}
						@Override
						public void myUndo() {
							dk.changeTraitVisibility(t);
							vcb.setSelected(oldValue);
							if (bgEditor.isVisible()) {
								bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Stage));
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
		for (FloatTrait t:dk.getStageTraits())
			visibilityCBPanel.add(visibilityCBs.get(t));
		visibilityCBPanel.add(Box.createVerticalGlue());
		visibilityCBPanel.getParent().validate();
		visibilityCBPanel.getParent().repaint();
		
		for(FloatTrait t:dk.getStageTraits())
			((JCheckBox)visibilityCBs.get(t)).setSelected(t.isVisible());
	};

	private void reloadBGEditorTraitValues(){
		float[] values = new float[dk.getVisibleTraitCount(Deikto.TraitType.Stage)];
		int i=0;
		for(FloatTrait t:dk.getVisibleTraits(Deikto.TraitType.Stage)) {
			if (t.isVisible())
				values[i++]=dk.getActor(1).getP(t,mStage);
		}
		bgEditor.setTraitValues(values);
	}
	private void showBackgroundEditor(){
		bgEditor.setImage(mStage.getImage(dk));
		bgEditor.setDescription(mStage.getDescription());
		bgEditor.setTitle("Stage: "+mStage.getLabel());
		if (!bgEditor.isVisible())
			bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Stage));
		reloadBGEditorTraitValues();
		bgEditor.setVisible(true);
		bgEditor.toFront();
	}
	
	private BackgroundEditor createBackgroundEditor(){
		BackgroundEditor bgEditor = new BackgroundEditor(swat.getMyFrame()){
			private static final long serialVersionUID = 1L;
			@Override
			public void onDescriptionChange(final String newDescription) {
				final Stage stage = mStage;
				final String oldDescription = stage.getDescription();
				if (oldDescription.equals(newDescription))
					return;
				stage.setDescription(newDescription);
				
				new UndoableAction(swat,false,"set description for "+mStage.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						stage.setDescription(newDescription);
						showStage(stage);
						showBackgroundEditor();
					}
					@Override
					public void myUndo() {
						stage.setDescription(oldDescription);
						showStage(stage);
						showBackgroundEditor();
					}
				};
			}
			@Override
			public void onImageChange(final ScaledImage newImage) {
				final Stage stage = mStage;
				final ScaledImage oldImage = stage.getImage(dk);
				stage.setImage(newImage);
				stage.increaseImageChangeCount();
				final String oldImageName = stage.getImageName();
				new UndoableAction(swat,false,"set image for "+mStage.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						stage.setImage(newImage);
						stage.increaseImageChangeCount();
						showStage(stage);
						showBackgroundEditor();
					}
					@Override
					public void myUndo() {
						stage.setImage(oldImage);
						stage.setImageName(oldImageName);
						stage.decreaseImageChangeCount();
						showStage(stage);
						showBackgroundEditor();
					}
				};
			}
		};
		bgEditor.getContentPane().setBackground(Utils.STORYTELLER_RIGHT_COLOR);
		bgEditor.setLocationRelativeTo(swat.getMyFrame());
		
		return bgEditor; 
	}
}

