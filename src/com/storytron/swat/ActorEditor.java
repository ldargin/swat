package com.storytron.swat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;

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
import com.storytron.uber.Word;
import com.storytron.uber.Actor.PTraitType;
import com.storytron.uber.Actor.TraitType;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.OperatorDictionary;

public final class ActorEditor {
	private static final long serialVersionUID = 1L;
// 07/12/2108 As far as I can tell, neither of the next two constants are used anywhere	
//	static final int cBoxes = 65;
//	static final int cBodyTraits = 9;
	DropDown selectionBox;
	DropDown spyingOnBox;
	DropDown locationBox;
	Deikto dk;
	Swat swat;
	private JPanel myPanel;
	private CustomTraitsControl<Actor> customTraits;
	private CustomTextTraitsControl<Actor> customTextTraits;
	private JCheckBox femaleCheckBox, activeCheckBox, dontMoveMeCheckBox, unconsciousCheckBox;
	private EnumMap<Actor.TraitType,Map<FloatTrait,Component>> boxes = new EnumMap<Actor.TraitType,Map<FloatTrait,Component>>(Actor.TraitType.class);
	private Map<FloatTrait,JCheckBox> visibilityCBs = new TreeMap<FloatTrait,JCheckBox>();
	private EnumMap<Actor.MoodTrait,Box> moodBoxes = new EnumMap<Actor.MoodTrait,Box>(Actor.MoodTrait.class);
	private JComponent visibilityCBPanel;
	public BackgroundEditor bgEditor;
	private KnowsMenu knowsMenu = new KnowsMenu();

	private JLabel actorToEditLabel = new JLabel("Actor to edit");
	private JLabel locLabel = new JLabel("Location");
	private JLabel spyingOnLabel = new JLabel("SpyingOn");
	private JButton addButton = new AddButton("Actor");
	private JButton backgroundInformationButton = new JButton("Background information");
	private static Actor.TraitType[] AUX_TRAIT_TYPES = new Actor.TraitType[]{Actor.TraitType.Weight,Actor.TraitType.Accord};

	private EditorListener selectionEditorListener;

	Actor mActor;
	private DeleteButton deleteButton = new DeleteButton("Actor");
		
	private Color singlesColor3 = new Color(230, 230, 255);
	
	public boolean userInput = true;

	// Descriptive Strings for attributes
	public static Actor.PropertyMap<String,String,String> pmDesc = new Actor.PropertyMap<String,String,String>();
	
	static {
		loadDescriptions();
	}
	
	/*
	 * Now the text is hard coded but later it may be loaded from a file.
	 * */
	private static void loadDescriptions(){
		
		// Initialize every description to the empty string.
		for(Actor.MoodTrait t:Actor.MoodTraits)
			pmDesc.set(t,null);

		pmDesc.set(Actor.ExtraTrait.debt_Grace,"The degree to which the lower Actor is obliged to the upper Actor, as perceived by the upper Actor.");
		pmDesc.set(Actor.ExtraTrait.stranger_Kin,"The closeness of the familial relationship between the two Actors, as perceived by the upper Actor.");
		
		pmDesc.set(Actor.MoodTrait.Tired_Energetic,
			"Think of this as a Mood-like version of Sickly_Healthy. This" +
			" Attribute automatically recovers to 1 over time. An Actor with a" +
			" value of 1 is at maximum energy, and a value of -1 denotes an Actor" +
			" who just collapsed from exhaustion. An Actor with 0 for this" +
			" Attribute isn't at full force, but isn't exhausted, either.");

		
/*		pmDesc.set(FloatTrait.Clumsy_Agile,
			"This is a BNumber which represents the relative agility of the" +
			" Actor, measured from -1 for an obese couch potato, through 0 for" +
			" an average person, to 1 for an Olympic gymnast.");
		pmDesc.set(FloatTrait.Attribute1,
			"A custom attribute which can be used however the author sees fit.");
		pmDesc.set(FloatTrait.Attribute2,
			"A custom attribute which can be used however the author sees fit.");
		pmDesc.set(FloatTrait.Attribute3,
			"A custom attribute which can be used however the author sees fit.");
		pmDesc.set(FloatTrait.Attribute4,
			"A custom attribute which can be used however the author sees fit.");
		pmDesc.set(FloatTrait.Short_Tall,
			"This is a BNumber which represents the Actor's height. An Actor" +
			" with -1 Short_Tall is an especially short midget - one with 1" +
			" Short_Tall is a giant. An Actor with 0 Short_Tall is of average" +
			" height.");
		pmDesc.set(FloatTrait.Weak_Strong, 
			"This is a BNumber which represents the relative strength of the" +
			" Actor, measured from -1 for an utter weakling, through 0 for an" +
			" average person, to 1 for a bodybuilder.");
		pmDesc.set(FloatTrait.Young_Old,
			"This is a BNumber which represents the Actor's age. An Actor with -1" +
			" Young_Old is a newborn - one with Young_Old 1 is Methuselah. An" +
			" Actor with 0 Young_Old is average-aged - whatever that means in" +
			" your storyworld. ");*/

		/*
		public static final String light_heavyDescription =
			"This is a BNumber which represents the Actor's weight. An Actor with" +
			" -1 Light_Heavy is blown away when you sneeze at him or her. An" +
			" Actor with 1 Light_Heavy shakes the earth when he or she walks. An" +
			" Actor with 0 Light_Heavy is of average weight.";
		public static final String poor_richDescription =
			"This is a BNumber which provides an abstract assessment of the" +
			" amount of money possessed by the Actor, measured in terms of some" +
			" immensely large unit of wealth, so large that nobody could ever" +
			" have a value of 1 for Poor_Rich. Note that this value does not" +
			" correlate with Props' Worthless_Valuable Attribute (see Props)." +
			" For example, an Actor with 0.5 Poor_Rich who buys a 0.25" +
			" Worthless_Valuable Prop will not necessarily have 0.25 Poor_Rich" +
			" left. You can create whatever correlation you want between the two" +
			" Attributes, or none at all (depending on how you define the" +
			" Consequence Scripts of Verbs that have to do with trading Props)." +
			" An Actor with -1 Poor_Rich is so poor he or she probably can't" +
			" afford a used pencil. An Actor with 0 Poor_Rich makes an average" +
			" amount of money.";
		public static final String sickly_healthyDescription =
			"This is a BNumber which is similar in its meaning to Health in some" +
			" computer games. A value of 1 indicates somebody at the peak of" +
			" health, rosy-cheeked and twinkling of eye. A value of -1" +
			" represents the health level of a fresh corpse. A value of 0" +
			" indicates somewhat diminished health - no serious illness or maim," +
			" but perhaps some bruises or a cold.";
			
			*/
	}
	
    /**
     * Initializes widgets.
     * Sets the layout using lot of auxiliary panels
     * Then shows the UI.
     */
	public ActorEditor(Swat tSwat) {	
		myPanel = new JPanel(null);
		myPanel.setLayout(new BoxLayout(myPanel,BoxLayout.X_AXIS));
	    initWidgets(tSwat);
	    // See bug 469 for an explanation about why we instantiate this here.
	    bgEditor = createBackgroundEditor();
	    setPanels();
	    
	    backgroundInformationButton.setToolTipText(Utils.toHtmlTooltipFormat("What the players will see when they select the \"People\" display for this Actor."));
		knowsMenu.setToolTipText(Utils.toHtmlTooltipFormat("Checked Actors start the story knowing the values of the traits of this Actor."));
	    activeCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If checked, the Actor starts the story in an active state and behaves normally; otherwise, the Actor starts the story in an inactive state and the story proceeds as if the Actor does not exist."));
	    unconsciousCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If checked, the Actor starts the story in an unconscious state and can't do anything but is still present on their Stage."));
	    dontMoveMeCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If checked, the Engine will not move the Actor to different Stages."));
	    locationBox.getJTextComponent().setToolTipText(Utils.toHtmlTooltipFormat("The Stage in which the Actor resides at the start of the story"));
	    spyingOnBox.getJTextComponent().setToolTipText(Utils.toHtmlTooltipFormat("The Actor whom this Actor is spying upon at the start of the story"));
	    femaleCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("The gender of the Actor. Unchecked means 'male'."));
	}	

	public void init(Deikto tdk){
		dk=tdk;
	    if (dk.getActorCount()>1)
	    	mActor=dk.getActor(1);
	    customTraits.init(dk);
	    customTextTraits.init(dk);
		bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Actor));

		for(FloatTrait t:dk.getActorTraits()) {
			for(PTraitType tt:PTraitType.values())
				pmDesc.set(tt,t,t.getDescription());
			for(Actor.TraitType tt:Actor.TraitType.values())
				pmDesc.set(tt,t,null);
		}

		reloadAccordAndWeightTraits();

	    // Disable editing of predefined traits.
	    int i=0;
	    for(FloatTrait t:dk.getActorTraits()){
	    	if (contains(Deikto.predefinedActorTraits,t.getLabel())){
	    		JComponent c=(JComponent)((JComponent)customTraits.getSlidersPanel().getComponent(i)).getComponent(0);
	    		((JComponent)c.getComponent(0)).getComponent(0).setEnabled(false);
	    		((JComponent)c.getComponent(0)).getComponent(1).setEnabled(false);
	    		((Swat.TextField)c.getComponent(1)).setDisabledTextColor(Color.gray);
	    		c.getComponent(1).setEnabled(false);
	    		visibilityCBPanel.getComponent(i).setEnabled(false);
	    	}
	    	i++;
	    }
	    
		for(final Actor.TraitType tt:AUX_TRAIT_TYPES)
			for(FloatTrait t:dk.getActorTraits())
				if (!t.isVisible())
					((JComponent)((JComponent)boxes.get(tt).get(t)).getComponent(1)).setToolTipText(Utils.breakStringHtml(pmDesc.get(tt,t)));
		for(Actor.MoodTrait t:Actor.MoodTraits)
			((JComponent)moodBoxes.get(t).getComponent(1)).setToolTipText(Utils.breakStringHtml(pmDesc.get(t)));
		
		onActorAddOrRemove();
	}
	
	private ErrorPopup errorPopup=new ErrorPopup();

	/**
	 *  Initializes the UI widgets. The code that places the widgets
	 *  on the UI go in <code>setPanels</code>.
	 */
	private void initWidgets(Swat tSwat) {
		dk = null;
		swat = tSwat;

		customTraits = new CustomTraitsControl<Actor>(swat,Deikto.TraitType.Actor){
			@Override
			protected Actor getEditedEntity() {	return mActor; }
			@Override
			protected float getValue(Actor e, FloatTrait t) { return e.get(t); }
			@Override
			protected void setValue(Actor e, FloatTrait t, float value) {
				e.set(t,value);
				if (swat.verbEditor.sentenceDisplayEditor.isVisible())
					swat.verbEditor.sentenceDisplayEditor.refresh();
			}
			@Override
			protected void showEditedEntity(Actor e) { showActor(e); }
			@Override
			protected void onTraitChange() {
				reloadAccordAndWeightTraits();
				if (bgEditor.isVisible()){
					bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Actor));
					reloadBGEditorTraitValues();
				}
				swat.refreshRelationshipSettings();
				if (swat.verbEditor.sentenceDisplayEditor.isVisible())
					swat.verbEditor.sentenceDisplayEditor.refresh();
			}
		};

		customTextTraits = new CustomTextTraitsControl<Actor>(swat,Deikto.TraitType.Actor){
			@Override
			protected Actor getEditedEntity() {	return mActor; }
			@Override
			protected String getValue(Actor e, TextTrait t) { return e.getText(t); }
			@Override
			protected void setValue(Actor e, TextTrait t, String value) {
				e.setText(t,value);
				if (swat.verbEditor.sentenceDisplayEditor.isVisible())
					swat.verbEditor.sentenceDisplayEditor.refresh();
			}
			@Override
			protected void showEditedEntity(Actor e) { showActor(e); }
			@Override
			protected void onTraitChange() {
				customTextTraits.getTextFieldsPanel().revalidate();
				customTextTraits.getTextFieldsPanel().repaint();
				if (swat.verbEditor.sentenceDisplayEditor.isVisible())
					swat.verbEditor.sentenceDisplayEditor.refresh();
			}
		};

		for(TraitType t:TraitType.values())
			boxes.put(t,new TreeMap<FloatTrait,Component>());

		actorToEditLabel.setAlignmentX(0.5f);
		selectionBox = new DropDown(Deikto.MAXIMUM_FIELD_LENGTH){
			private static final long serialVersionUID = 1L;
			@Override
			public String getTooltipText(Object o) {
				return Utils.toHtmlTooltipFormat(((Actor)o).getDescription());
			}
			@Override
			public void indexMoved(final int from, final int to) {			
				final Actor actor = mActor;
				new UndoableAction(swat,"reorder actors"){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						dk.moveActor(from+1,to+1);
						mActor=actor;
						refresh();
						selectionBox.showPopup();
					}
					@Override
					public void myUndo() {
						dk.moveActor(to+1,from+1);
						mActor=actor;
						refresh();
						selectionBox.showPopup();
					}
				};
			}
		};
		selectionBox.setAllowReordering(true);
		selectionBox.setPreferredSize(new Dimension(160,25));
		selectionBox.setMaximumRowCount(35);
		selectionBox.setEditable(true);
		selectionBox.setBackground(Color.white);
		selectionEditorListener=new Swat.DropDownListener(selectionBox) {
			private static final long serialVersionUID = 1L;
			public boolean timedActionPerformed(ActionEvent e) {
				if (!userInput) return true;
				if (selectionBox.isListPicking()) {
					showActor((Actor)selectionBox.getSelectedItem());
					return true;
				} else { // if not listPicking, assume the storybuilder
					// is trying to rename the current prop.
					return renameActor(((String)selectionBox.getTextComponent().getJTextComponent().getText()).trim());
				}			
			}
			@Override
			public String getText() { return mActor!=null?mActor.getLabel():""; }
		};
		selectionBox.addActionListener(selectionEditorListener);

		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i=0;
				while(dk.getActorIndex(i==0?"new actor":"new actor"+i)!=-1) i++;
				addActor(i==0?"new actor":"new actor"+i);
				selectionBox.getJTextComponent().requestFocusInWindow();
				selectionBox.getJTextComponent().selectAll();
			}
		});

		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteSelectedActor();
			}
		});

		backgroundInformationButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				showBackgroundEditor();
			}
		});

		activeCheckBox = new JCheckBox("active");
		activeCheckBox.setOpaque(false);
		activeCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		activeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {	
				if (!userInput) return;
				final Actor actor = mActor;
				final boolean newValue = activeCheckBox.isSelected();
				new UndoableAction(swat,"change active of "+actor.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						actor.setActive(newValue);
						showActor(actor);					
					}
					@Override
					public void myUndo() {
						actor.setActive(!newValue);
						showActor(actor);
					}
				};
			}
		});

		femaleCheckBox = new JCheckBox("female");
		femaleCheckBox.setOpaque(false);
		femaleCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		femaleCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!userInput) return;
				final Actor actor = mActor;			
				final boolean newValue = femaleCheckBox.isSelected();
				new UndoableAction(swat,"change male of "+actor.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						actor.setFemale(newValue);
						showActor(actor);					
					}
					@Override
					public void myUndo() {
						actor.setFemale(!newValue);
						showActor(actor);
					}
				};
			}
		});

		unconsciousCheckBox = new JCheckBox("unconscious");
		unconsciousCheckBox.setOpaque(false);
		unconsciousCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		unconsciousCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!userInput) return;
				final Actor actor = mActor;
				final boolean newValue = unconsciousCheckBox.isSelected();
				new UndoableAction(swat,"change unconscious of "+actor.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						actor.setUnconscious(newValue);
						showActor(actor);					
					}
					@Override
					public void myUndo() {
						actor.setUnconscious(!newValue);
						showActor(actor);					
					}
				};
			}
		});

		dontMoveMeCheckBox = new JCheckBox("dontMoveMe");
		dontMoveMeCheckBox.setOpaque(false);
		dontMoveMeCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		dontMoveMeCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (!userInput) return;
				final Actor actor = mActor;
				final boolean newValue = dontMoveMeCheckBox.isSelected();
				new UndoableAction(swat,"change dontMoveMe of "+actor.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						actor.setDontMoveMe(newValue);
						showActor(actor);					
					}
					@Override
					public void myUndo() {
						actor.setDontMoveMe(!newValue);
						showActor(actor);					
					}
				};
			}
		});

		locLabel.setAlignmentX(0.5f);
		locationBox = new DropDown(100){
			private static final long serialVersionUID = 1L;
			@Override
			public String getTooltipText(Object o) {
				return Utils.toHtmlTooltipFormat(((Stage)o).getDescription());
			}
		};
		locationBox.setBackground(Color.white);
		locationBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!userInput) return;

				final Actor actor = mActor;
				final int newValue = locationBox.getSelectedIndex();
				final Word.Reference oldValue = actor.getLocationRef();
				new UndoableAction(swat,"change location of "+actor.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						actor.setLocation(dk.getStage(newValue));				
						showActor(actor);					
					}
					@Override
					public void myUndo() {
						actor.setLocationRef(oldValue);
						showActor(actor);					
					}
				};
			}
		});
		locationBox.setMaximumRowCount(35);
		userInput = false;
		userInput = true;

		spyingOnLabel.setAlignmentX(0.5f);
		spyingOnBox = new DropDown(100){
			private static final long serialVersionUID = 1L;
			@Override
			public String getTooltipText(Object o) {
				return Utils.toHtmlTooltipFormat(((Actor)o).getDescription());
			}
		};
		spyingOnBox.setBackground(Color.white);
		spyingOnBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!userInput) return;

				final Actor actor = mActor; 
				final int newValue = spyingOnBox.getSelectedIndex();
				final Word.Reference oldValue = actor.getSpyingOnRef();
				new UndoableAction(swat,"change spyingOn of "+actor.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						actor.setSpyingOn(dk.getActor(newValue));				
						showActor(actor);					
					}
					@Override
					public void myUndo() {
						actor.setSpyingOnRef(oldValue);
						showActor(actor);					
					}
				};
			}
		});
		spyingOnBox.setMaximumRowCount(35);

		for(final Actor.MoodTrait t:Actor.MoodTraits) {		
			Box b = createFloatBox(t.name(),pmDesc.get(t));
			moodBoxes.put(t,b);
			new UndoableSlider(swat,(Swat.Slider)b.getComponent(1)){
				Actor actor;
				@Override
				public int init() {
					actor = ActorEditor.this.mActor;
					return toSlider(actor.get(t));
				}

				@Override
				public void setValue(int value) {
					actor.set(t,fromSlider(value));
				}
				@Override
				public void undoRedoExecuted() { showActor(actor);	}

				public String getPresentationName() { return "change "+t.name()+" of "+actor.getLabel();	}
			};				
		}
	}

	private JPanel accordPanel, weightPanel;

	/**
	 * Sets the layout for all auxiliary panels.
	 * Widget initialization stuff goes in <code>initWidgets</code>.
	 */
	private void setPanels() {
		JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.X_AXIS));
		selectionBox.setMaximumSize(selectionBox.getPreferredSize());
		lowerPanel.setOpaque(false);
		lowerPanel.add(selectionBox);
		lowerPanel.add(addButton);
		lowerPanel.add(deleteButton);

		Dimension db=addButton.getPreferredSize();
		db.height=selectionBox.getPreferredSize().height;
		addButton.setPreferredSize(db);
		deleteButton.setPreferredSize(db);

		JPanel selectionPanel = new JPanel();
		selectionPanel
		.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
		selectionPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		selectionPanel.setOpaque(false);		
		selectionPanel.add(actorToEditLabel);
		selectionPanel.add(lowerPanel);

		JPanel locationPanel = new JPanel();
		locationPanel.setLayout(new BoxLayout(locationPanel, BoxLayout.Y_AXIS));
		locationPanel.setOpaque(false);
		locationPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
		locationPanel.add(locLabel);
		locationBox.setMaximumSize(new Dimension(200,locationBox.getPreferredSize().height));
		locationPanel.add(locationBox);

		JPanel captorPanel = new JPanel();
		captorPanel.setOpaque(false);
		captorPanel.setLayout(new BoxLayout(captorPanel, BoxLayout.Y_AXIS));
		captorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

		JPanel spyingOnPanel = new JPanel();
		spyingOnPanel.setOpaque(false);
		spyingOnPanel.setLayout(new BoxLayout(spyingOnPanel, BoxLayout.Y_AXIS));
		spyingOnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
		spyingOnPanel.add(spyingOnLabel);
		spyingOnBox.setMaximumSize(new Dimension(200,spyingOnBox.getPreferredSize().height));
		spyingOnPanel.add(spyingOnBox);

		Box statePanel = Box.createVerticalBox();
		statePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("State"),
				BorderFactory.createEmptyBorder(0,5,5,5))
		);
		knowsMenu.setAlignmentX(0.0f);
		statePanel.add(knowsMenu);
		statePanel.add(Box.createRigidArea(new Dimension(6,6)));
		activeCheckBox.setAlignmentX(0.0f);
		statePanel.add(activeCheckBox);
		unconsciousCheckBox.setAlignmentX(0.0f);
		statePanel.add(unconsciousCheckBox);
		dontMoveMeCheckBox.setAlignmentX(0.0f);
		statePanel.add(dontMoveMeCheckBox);
		locationPanel.setAlignmentX(0.0f);
		statePanel.add(locationPanel);
		captorPanel.setAlignmentX(0.0f);
		statePanel.add(captorPanel);
		spyingOnPanel.setAlignmentX(0.0f);
		statePanel.add(spyingOnPanel);

		visibilityCBPanel = new JPanel(null);
		visibilityCBPanel.setLayout(new BoxLayout(visibilityCBPanel,BoxLayout.Y_AXIS));
		visibilityCBPanel.setBackground(Utils.darkColumnBackground);
		visibilityCBPanel.setMaximumSize(new Dimension(20,Integer.MAX_VALUE));
		
		accordPanel = new JPanel(null);
		accordPanel.setLayout(new BoxLayout(accordPanel,BoxLayout.Y_AXIS));
		accordPanel.setBorder(BorderFactory.createEmptyBorder(6, 5, 5,5));
		accordPanel.setBackground(singlesColor3);

		weightPanel = new JPanel(null);
		weightPanel.setLayout(new BoxLayout(weightPanel,BoxLayout.Y_AXIS));
		weightPanel.setBorder(BorderFactory.createEmptyBorder(6, 5, 5,5));
		weightPanel.setBackground(Utils.darkColumnBackground);

		customTraits.getSlidersPanel().setMaximumSize(new Dimension(220,Integer.MAX_VALUE));
		customTraits.getSlidersPanel().setMinimumSize(new Dimension(220,10));
		customTraits.getSlidersPanel().setPreferredSize(new Dimension(220,10));
		
		JComponent traitsPanel = Box.createHorizontalBox();
		customTraits.getSlidersPanel().setAlignmentY(0.0f);
		traitsPanel.add(customTraits.getSlidersPanel());
		visibilityCBPanel.setAlignmentY(0.0f);
		traitsPanel.add(visibilityCBPanel);
		accordPanel.setAlignmentY(0.0f);
		traitsPanel.add(accordPanel);
		weightPanel.setAlignmentY(0.0f);
		traitsPanel.add(weightPanel);		
		
		JScrollPane scroll = new JScrollPane(traitsPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getViewport().setBackground(Utils.lightBackground);
		scroll.getVerticalScrollBar().setUnitIncrement(20);
		
		JComponent aux=Box.createHorizontalBox();
		femaleCheckBox.setAlignmentX(0.0f);
		aux.add(femaleCheckBox);
		aux.add(Box.createHorizontalGlue());
		aux.add(customTraits.getAddButton());
		
		Box coreActorTraitsPanel = Box.createVerticalBox();
		coreActorTraitsPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Core Actor Traits"),
				BorderFactory.createEmptyBorder(5,5,5,5))
		);
		aux.setAlignmentX(0.0f);
		coreActorTraitsPanel.add(aux);
		scroll.setAlignmentX(0.0f);
		coreActorTraitsPanel.add(scroll);
		coreActorTraitsPanel.setPreferredSize(new Dimension(675,10));
		
		JComponent textTraits = Box.createHorizontalBox();
		textTraits.add(customTextTraits.getTextFieldsPanel());
		
		JScrollPane textTraitScroll = new JScrollPane(textTraits,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		textTraitScroll.getViewport().setBackground(Utils.lightBackground);
		textTraitScroll.getVerticalScrollBar().setUnitIncrement(20);
		
		JComponent headerPanel = Box.createHorizontalBox();
		headerPanel.add(new JLabel("Text Traits"));
		headerPanel.add(new LineBox(2,Swat.shadow));
		headerPanel.add(customTextTraits.getAddButton());
		
		ComponentLabeledPanel textTraitsPanel = new ComponentLabeledPanel(headerPanel,Swat.shadow,false);
		textTraitsPanel.setOpaque(false);
		textTraitsPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		textTraitsPanel.add(textTraitScroll);
		
		Box moodPanel = Box.createVerticalBox();
		moodPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Mood"),
				BorderFactory.createEmptyBorder(5,5,5,5))
		);

		for (Actor.MoodTrait t:Actor.MoodTraits)
			moodPanel.add(moodBoxes.get(t));
		moodPanel.add(Box.createVerticalGlue());
		
		Box leftLeftPanel = Box.createHorizontalBox();		
		statePanel.setAlignmentY(0.5f);
		leftLeftPanel.add(statePanel);
		moodPanel.setAlignmentY(0.5f);
		leftLeftPanel.add(moodPanel);
		
		JComponent auxLeftPanel = new JPanel(new BorderLayout());
		auxLeftPanel.setOpaque(false);
		auxLeftPanel.add(leftLeftPanel,BorderLayout.NORTH);
		auxLeftPanel.add(textTraitsPanel,BorderLayout.CENTER);
		
		Box leftPanel = Box.createVerticalBox();
		leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		selectionPanel.setAlignmentX(0.5f);
		leftPanel.add(selectionPanel);
		leftPanel.add(Box.createRigidArea(new Dimension(10,5)));
		backgroundInformationButton.setAlignmentX(0.5f);
		leftPanel.add(backgroundInformationButton);
		leftPanel.add(Box.createRigidArea(new Dimension(10,5)));
		auxLeftPanel.setAlignmentX(0.5f);
		leftPanel.add(auxLeftPanel);
		
		JPanel singlesPanel = new JPanel(null);
		singlesPanel.setLayout(new BoxLayout(singlesPanel,BoxLayout.X_AXIS));
		singlesPanel.setBackground(Utils.lightBackground);
		singlesPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		singlesPanel.add(leftPanel);
		singlesPanel.add(coreActorTraitsPanel);
		singlesPanel.setMaximumSize(new Dimension(singlesPanel.getPreferredSize().width,Integer.MAX_VALUE));

		myPanel.add(Box.createHorizontalGlue());
		myPanel.add(singlesPanel);
		myPanel.add(Box.createHorizontalGlue());
		
		myPanel.setBackground(Utils.darkBackground);		
	}

	private boolean renameActor(String zLabel){
		if (zLabel==null || mActor==null || mActor.getLabel().equals(zLabel))
			return true;

		String s=swat.dk.nameExists(zLabel);
		if (s!=null) {
			errorPopup.showError(swat.getMyFrame(),selectionBox.getLocationOnScreen(),s);
			return false;
		}

		final Actor actor = mActor;
		final String newValue = zLabel;
		final String oldValue = actor.getLabel();
		actor.setLabel(newValue);
		new UndoableAction(swat,false,""){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				actor.setLabel(newValue);
				loadSelectionBox(mActor);
				showActor(actor);
			}
			@Override
			public void myUndo() {
				actor.setLabel(oldValue);							
				loadSelectionBox(actor);
				showActor(actor);
			}
			@Override
			public String getRedoPresentationName() {
				return "Redo rename actor "+oldValue;
			}
			@Override
			public String getUndoPresentationName() {
				return "Undo rename actor "+newValue;
			}
		};						
		return true;
	}
	
	private void reloadAccordAndWeightTraits(){
		
		for(final Actor.TraitType tt:AUX_TRAIT_TYPES) {
			for(final FloatTrait t:dk.getActorTraits()) {
				Component c = boxes.get(tt).get(t);
				if (c!=null){
					((JLabel)((Box)c).getComponent(0)).setText(Actor.traitName(tt, t));
					continue;
				}
				
				Box b = createFloatBox(Actor.traitName(tt,t),tt==Actor.TraitType.Accord
						 									?"The degree to which the Actor is inclined to believe that other Actors have high values of this Trait."
															:"The importance that the Actor places on being perceived as having a high value of this Trait."
										);
				b.setMinimumSize(new Dimension(10,CustomTraitsControl.TRAIT_HEIGHT));
				b.setMaximumSize(new Dimension(250,CustomTraitsControl.TRAIT_HEIGHT));
				b.setPreferredSize(new Dimension(10,CustomTraitsControl.TRAIT_HEIGHT));
				
				boxes.get(tt).put(t,b);
				new UndoableSlider(swat,(Swat.Slider)b.getComponent(1)){
					Actor actor;
					@Override
					public int init() {
						actor = ActorEditor.this.mActor;
						return toSlider(actor.get(tt,t));
					}
					@Override
					public void setValue(int value) {
						actor.set(tt,t,fromSlider(value));
					}
					@Override
					public void undoRedoExecuted() { showActor(actor); }
					
					public String getPresentationName() { return "change "+Actor.traitName(tt,t)+" of "+actor.getLabel();	}
				};				
			}
		}
		
		for(final FloatTrait t:dk.getActorTraits()) {
			Component cb = visibilityCBs.get(t);
			if (cb!=null)
				continue;
			
			final JCheckBox vcb = new JCheckBox();
			vcb.setToolTipText("<html><b>Make visible</b><br>"+
							   "If this is checked, then actors will<br>"+
							   "perceive this trait in other actors<br>" +
							   "every time they meet.<html>");
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
								bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Actor));
								reloadBGEditorTraitValues();
							}
							((JComponent)vcb.getParent()).scrollRectToVisible(vcb.getBounds());
						}
						@Override
						public void myUndo() {
							dk.changeTraitVisibility(t);
							vcb.setSelected(oldValue);
							if (bgEditor.isVisible()) {
								bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Actor));
								reloadBGEditorTraitValues();
							}
							((JComponent)vcb.getParent()).scrollRectToVisible(vcb.getBounds());
						}
					};
				}
			});
			visibilityCBs.put(t,vcb);
		}

		
		accordPanel.removeAll();
		weightPanel.removeAll();
		visibilityCBPanel.removeAll();
		for (FloatTrait t:dk.getActorTraits()){
			accordPanel.add(boxes.get(Actor.TraitType.Accord).get(t));
			weightPanel.add(boxes.get(Actor.TraitType.Weight).get(t));
			visibilityCBPanel.add(visibilityCBs.get(t));
		}
		accordPanel.add(Box.createVerticalGlue());
		weightPanel.add(Box.createVerticalGlue());
		visibilityCBPanel.add(Box.createVerticalGlue());
		visibilityCBPanel.getParent().validate();
		visibilityCBPanel.getParent().repaint();
		
		for(FloatTrait t:dk.getActorTraits())
			((JCheckBox)visibilityCBs.get(t)).setSelected(t.isVisible());
	};
	private static <T> boolean contains(T[] ts,T t){
		for(T tt:ts)
			if (tt==t || tt!=null && tt.equals(t))
				return true;
		return false;
	}
	private Box createFloatBox(String label,String description){
		
		JLabel zLabel = new JLabel(label);
		zLabel.setToolTipText(Utils.toHtmlTooltipFormat(description));
		zLabel.setAlignmentX(0.5f);
		zLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		
		Swat.Slider zSlider = new Swat.Slider(JSlider.HORIZONTAL, 0, 100, 50);
		zSlider.setPreferredSize(new Dimension(150, zSlider.getPreferredSize().height));
		zSlider.setMinimumSize(zSlider.getPreferredSize());		
		zSlider.setBackground(Color.white);

		Box zBox = Box.createVerticalBox();
		zBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		zBox.add(zLabel);
		zSlider.setAlignmentX(0.5f);
		zSlider.setOpaque(false);
		zBox.add(zSlider);
		zBox.setOpaque(false);
		zBox.setAlignmentX(0.5f);
		zSlider.setToolTipText(Utils.toHtmlTooltipFormat(description));
		return zBox;
	}
	
	public void addActor(String tLabel) {
		final Actor actor=new Actor(tLabel);
		final int iActor = mActor==null?0:mActor.getReference().getIndex();
		new UndoableAction(swat,"add actor "+tLabel){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				try {
					dk.addActor(actor);
				} catch(LimitException ex){ throw new RuntimeException(ex); }
				addActor(actor);
				showActor(actor);
			}
			@Override
			public void myUndo() {				
				deleteActor(actor);
				if (iActor>0)
					showActor(dk.getActor(iActor));
			}
		};
	}
	
	private void onActorAddOrRemove() {
		addButton.setEnabled(swat.dk.getActorCount()<swat.dk.limits.maximumActorCount);
		if (!addButton.isEnabled())
			addButton.setToolTipText("Cannot have more than "+dk.limits.maximumActorCount+" actors.");
		else
			addButton.setToolTipText("creates a new actor");
		
		deleteButton.setEnabled(swat.dk.getActorCount()>2);
		backgroundInformationButton.setEnabled(swat.dk.getActorCount()>1);
		
		knowsMenu.setEnabled(swat.dk.getActorCount()>1);
		if (!knowsMenu.isEnabled())
			knowsMenu.myPopup.setVisible(false);
	}

	private void addActor(Actor actor){
		// now add all the associated elements in Props and Stages
		loadSelectionBox(actor);
		userInput = false;
		selectionBox.setSelectedItem(actor);
		userInput = true;
		loadSpyingOnBox();
		onActorAddOrRemove();
		Swat.playSound("add.aiff");
	}
//**********************************************************************
	private void deleteSelectedActor() {
		if (mActor==null) return;
		final Actor actor=mActor;
		final int iActor=mActor.getReference().getIndex(); 

		final LinkedList<Script.Node> modifiedNodes = new LinkedList<Script.Node>();
		dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, Node n) {
				if (n.getConstant() == actor)
					modifiedNodes.add(n);
				return true;
			}
		});
		
		new UndoableAction(swat,"delete actor "+actor.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {	
				deleteActor(actor);				
				if (dk.getActorCount()>1)
					showActor(dk.getActor(1));
			}
			@Override
			public void myUndo() {
				try {
					dk.addActor(iActor,actor);
				} catch(LimitException ex){ throw new RuntimeException(ex); }
				addActor(actor);
				for(Script.Node n:modifiedNodes)
					n.setOperatorValue(OperatorDictionary.getActorConstantOperator(),actor);
				showActor(actor);
			}
		};
	}
	public void deleteActor(Actor actor) {
		userInput = false;
		selectionBox.getModel().removeElement(actor);
		userInput = true;
		dk.removeActor(actor);
		if (dk.getActorCount()<3)
			deleteButton.setEnabled(false);
		swat.propEditor.loadOwnerBox();
		swat.stageEditor.loadUnwelcoming_HomeyBox();
		onActorAddOrRemove();
		loadSpyingOnBox();
		Swat.playSound("delete.aiff");
	}
//**********************************************************************	
	public void loadSelectionBox(Actor actor) {
		userInput=false;
		selectionBox.removeAllItems();
		for (int i = 1; (i < dk.getActorCount()); ++i) {
			selectionBox.addItem(dk.getActor(i));
		}
		selectionBox.setSelectedItem(actor);
		userInput=true;
	}
//**********************************************************************	
	public void loadLocationBox() {
		userInput = false;
		locationBox.removeAllItems();
		for (int i = 0; (i < dk.getStageCount()); ++i) {
			locationBox.addItem(dk.getStage(i));
		}
		userInput = true;
	}

//**********************************************************************	
	public void loadSpyingOnBox() {
		userInput = false;
		spyingOnBox.removeAllItems();
		for (int i = 0; (i < dk.getActorCount()); ++i) {
			spyingOnBox.addItem(dk.getActor(i));
		}
		spyingOnBox.setSelectedIndex(0);
		userInput = true;
	}
//**********************************************************************
	public void refresh() { 
		loadSpyingOnBox();
		loadLocationBox();
		loadSelectionBox(mActor);
		showActor(mActor);
		myPanel.validate();
		myPanel.repaint();
	}
	public void showActor(Actor actor) {
		mActor = actor;
		userInput=false;
		selectionBox.setSelectedItem(mActor);		
		activeCheckBox.setSelected(mActor.getActive());
		femaleCheckBox.setSelected(mActor.getFemale());
		unconsciousCheckBox.setSelected(mActor.getUnconscious());
		dontMoveMeCheckBox.setSelected(mActor.getDontMoveMe());
		locationBox.setSelectedIndex(mActor.getLocation());
		spyingOnBox.setSelectedIndex(mActor.getSpyingOn());
		if (bgEditor.isVisible())
			showBackgroundEditor();
		
		for(TraitType tt:AUX_TRAIT_TYPES)
			for(FloatTrait t:dk.getActorTraits()) 
				if (!t.isVisible())
					((Swat.Slider)((JComponent)boxes.get(tt).get(t)).getComponent(1)).mSetValue(toSlider(mActor.get(tt,t)));
		for(Actor.MoodTrait t:Actor.MoodTraits) 
			((Swat.Slider)moodBoxes.get(t).getComponent(1)).mSetValue(toSlider(mActor.get(t)));
		
		knowsMenu.loadKnowsMenu();
		customTraits.refresh();
		customTextTraits.refresh();
		userInput=true;
	}
	
	/** 
	 * Routines for converting numbers from slider to model and the other 
	 * way around.  
	 * */
	private static float fromSlider(int v){ return (float)((v-50)/50.0*Utils.MAXI_VALUE); }
	private static int toSlider(float f){ return (int)(f*50/Utils.MAXI_VALUE+50); }

	public void reloadBGEditorTraitValues(){
		if (bgEditor!=null){
			float[] values = new float[dk.getVisibleTraitCount(Deikto.TraitType.Actor)];
			int i=0;
			for(FloatTrait t:dk.getVisibleTraits(Deikto.TraitType.Actor))
				if (t.isVisible())
					values[i++]=dk.getActor(1).getP(t,mActor);
			bgEditor.setTraitValues(values);
		}
	}
	private void showBackgroundEditor(){
		bgEditor.setImage(mActor.getImage(dk));
		bgEditor.setDescription(mActor.getDescription());
		bgEditor.setTitle("Actor: "+mActor.getLabel());
		if (!bgEditor.isVisible())
			bgEditor.setTraits(dk.getVisibleTraits(Deikto.TraitType.Actor));
		reloadBGEditorTraitValues();
		bgEditor.pack();
		bgEditor.setVisible(true);
		bgEditor.toFront();
		bgEditor.repaint();
	}
	
	private BackgroundEditor createBackgroundEditor(){
		 BackgroundEditor bgEditor = new BackgroundEditor(swat.getMyFrame()){
			private static final long serialVersionUID = 1L;
			@Override
			public void onDescriptionChange(final String newDescription) {
				final Actor actor = mActor;
				final String oldDescription = actor.getDescription();
				if (oldDescription.equals(newDescription))
					return;
				actor.setDescription(newDescription);
				
				new UndoableAction(swat,false,"set description for "+mActor.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						actor.setDescription(newDescription);
						showActor(actor);
						showBackgroundEditor();
					}
					@Override
					public void myUndo() {
						actor.setDescription(oldDescription);
						showActor(actor);
						showBackgroundEditor();
					}
				};
			}
			@Override
			public void onImageChange(final ScaledImage newImage) {
				final Actor actor = mActor;
				final ScaledImage oldImage = actor.getImage(dk);
				final String oldImageName = actor.getImageName();
				actor.setImage(newImage);
				actor.increaseImageChangeCount();
				new UndoableAction(swat,false,newImage==null?"delete image for "+mActor.getLabel():"set image for "+mActor.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						actor.setImage(newImage);
						actor.increaseImageChangeCount();
						showActor(actor);
						showBackgroundEditor();
					}
					@Override
					public void myUndo() {
						actor.setImage(oldImage);
						actor.setImageName(oldImageName);
						actor.decreaseImageChangeCount();
						showActor(actor);
						showBackgroundEditor();
					}
				};
			}
		};
		bgEditor.getContentPane().setBackground(Utils.STORYTELLER_RIGHT_COLOR);
		bgEditor.setLocationRelativeTo(swat.getMyFrame());
		return bgEditor;
	}
	
//	**********************************************************************
	public JPanel getMyPanel() {
		return myPanel;
	}

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
					final Actor actor = mActor;
					final int iActor = dk.getActorIndex(selectedItem.getText()); 
					final boolean newValue = selectedItem.isSelected();
					actor.setKnowsMe(dk.getActor(iActor),newValue);
					showActor(actor);
					new UndoableAction(swat,false,"change Knows Me for "+selectedItem.getText()){
						private static final long serialVersionUID = 1L;
						public void myRedo(){
							actor.setKnowsMe(dk.getActor(iActor),newValue);
							showActor(actor);
							myPopup.showPopup();
						}
						public void myUndo(){
							actor.setKnowsMe(dk.getActor(iActor),!newValue);
							showActor(actor);
							myPopup.showPopup();
						}
					};				

				}
			};
		}
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++		
		public void loadKnowsMenu() {
			if (mActor==null)
				return;
			
			myPopup.removeAll();
			for (int i=0; (i < dk.getActorCount()); ++i) {
				Actor ac = dk.getActor(i);
				JCheckBoxMenuItem zCheckBoxMenuItem = new JCheckBoxMenuItem(ac.getLabel());
				zCheckBoxMenuItem.addActionListener(myActionListener);
				zCheckBoxMenuItem.setState(mActor.getKnowsMe(dk.getActor(i)));
				myPopup.add(zCheckBoxMenuItem);
			}
		}
	}

	public static abstract class Test {
		
		public static void addActor(ActorEditor ae){
			ae.addButton.doClick();
		}
		public static void deleteSelectedActor(ActorEditor ae){
			ae.deleteButton.doClick();
		}

		public static void setSelectedActor(ActorEditor ae,String actorName){
			ae.showActor(ae.swat.dk.getActor(actorName));
		}

		public static void renameSelectedActor(ActorEditor ae,String newName){
			ae.renameActor(newName);
		}
		
		public static void addTrait(ActorEditor ae){
			ae.customTraits.getAddButton().doClick();
		}
		public static void deleteTrait(ActorEditor ae,String traitName){
			CustomTraitsControl.Test.deleteTrait(ae.customTraits,traitName);
		}

		public static void addTextTrait(ActorEditor ae){
			ae.customTextTraits.getAddButton().doClick();
		}
		public static void deleteTextTrait(ActorEditor ae,String traitName){
			CustomTextTraitsControl.Test.deleteTrait(ae.customTextTraits,traitName);
		}

		public static void renameTrait(ActorEditor ae,String oldName,String newName){
			CustomTraitsControl.Test.renameTrait(ae.customTraits, oldName, newName);
		}
		public static void renameTextTrait(ActorEditor ae,String oldName,String newName){
			CustomTextTraitsControl.Test.renameTrait(ae.customTextTraits, oldName, newName);
		}
		public static void setTraitValue(ActorEditor ae,String name,float value){
			CustomTraitsControl.Test.setTraitValue(ae.customTraits, name, value);
		}
		public static void setTextTraitValue(ActorEditor ae,String name,String value){
			CustomTextTraitsControl.Test.setTraitValue(ae.customTextTraits, name, value);
		}
		
	}
}