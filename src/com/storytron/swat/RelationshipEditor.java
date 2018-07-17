package com.storytron.swat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumMap;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.ComponentLabeledPanel;
import com.storytron.swat.util.UndoableSlider;
import com.storytron.uber.Actor;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;

public final class RelationshipEditor extends JPanel {
	private static final long serialVersionUID = 1L;
	static final int cBoxes = 65;
	static final int cBodyTraits = 9;
	JComboBox actor1Box, actor2Box;
	Deikto dk;
	Swat swat;
	private JLabel actor2to1Label = new JLabel("<html>Fred<br>thinks of<br>Mary </html>");

	EnumMap<Actor.ExtraTrait,Box> actor1ExtraBoxes = new EnumMap<Actor.ExtraTrait,Box>(Actor.ExtraTrait.class);
	EnumMap<Actor.ExtraTrait,Box> actor2ExtraBoxes = new EnumMap<Actor.ExtraTrait,Box>(Actor.ExtraTrait.class);
	
	private Actor mActor, mActor2;
	private CustomPTraitsControl<Actor> custom1PTraits, custom2PTraits;
		
	private boolean userInput = true;
	
    /**
     * Initializes widgets.
     * Sets the layout using lot of auxiliary panels
     * Then shows the UI.
     */
	public RelationshipEditor(Swat tSwat) {	
		super(null);
	    initWidgets(tSwat);
	    setPanels();                
	}	

	public void init(Deikto dk){
		this.dk=dk;
	}
	
	/**
	 *  Initializes the UI widgets. The code that places the widgets
	 *  on the UI go in <code>setPanels</code>.
	 */
	private void initWidgets(Swat tSwat) {
		dk = null;
		swat = tSwat;

		actor1Box = new JComboBox();
		actor1Box.setBackground(Color.white);
		//actor1Box.setMinimumSize(new Dimension(150, 25));
		//actor1Box.setMaximumSize(actor1Box.getMinimumSize());
		//actor1Box.setPreferredSize(actor1Box.getMinimumSize());
		actor1Box.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!userInput) return;
				actor1Box.hidePopup();
				showActors((Actor)actor1Box.getSelectedItem(),mActor2);
			}
		});

		actor2Box = new JComboBox();
		actor2Box.setBackground(Color.white);
		actor2Box.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!userInput) return;
				actor1Box.hidePopup();
				showActors(mActor,(Actor)actor2Box.getSelectedItem());
			}
		});

		for(final Actor.ExtraTrait t:Actor.ExtraTraits) {		
			final Box b = createFloatBox(t.name(),ActorEditor.pmDesc.get(t));
			actor1ExtraBoxes.put(t,b);
			final Swat.Slider sld = (Swat.Slider)b.getComponent(1);
			new UndoableSlider(swat,sld){
				Actor actor;
				Actor actor2; 
				@Override
				public int init() {
					actor = RelationshipEditor.this.mActor;
					actor2 = RelationshipEditor.this.mActor2;
					return (int)(actor.get(t,actor2)*50.0f+49);
				}
				@Override
				public void setValue(int value) {
					actor.set(t,actor2,(float)((value-49) / 50.0));
				}
				@Override
				public void undoRedoExecuted() { showActors(actor,actor2); }

				public String getPresentationName() { return "change "+t.name()+" of "+actor.getLabel();	}
			};	
		}

		for(final Actor.ExtraTrait t:Actor.ExtraTraits) {		
			Box b = createFloatBox(t.name(),ActorEditor.pmDesc.get(t));
			actor2ExtraBoxes.put(t,b);
			final Swat.Slider sld = (Swat.Slider)b.getComponent(1);
			new UndoableSlider(swat,sld){
				Actor actor1;
				Actor actor2; 
				@Override
				public int init() {
					actor1 = RelationshipEditor.this.mActor;
					actor2 = RelationshipEditor.this.mActor2;
					return (int)(actor2.get(t,actor1)*50.0f+49);
				}
				@Override
				public void setValue(int value) {
					actor2.set(t,actor1,(float)((value-49) / 50.0));
				}
				@Override
				public void undoRedoExecuted() { showActors(actor1,actor2); }

				public String getPresentationName() { return "change "+t.name()+" of "+actor2.getLabel();	}
			};		
		}

		custom1PTraits = new CustomPTraitsControl<Actor>(swat,Deikto.TraitType.Actor){
			private static final long serialVersionUID = 1L;
			@Override
			protected float getCValue(Actor e, FloatTrait t, Actor other) {
				return e.getC(t, other);
			}

			@Override
			protected Actor getEditedActor() {	return mActor; }

			@Override
			protected Actor getOtherEntity() { return mActor2; }

			@Override
			protected boolean isOverrided(Actor e, FloatTrait t, Actor other) {
				return e.isOverrided(t, other);
			}

			@Override
			protected float getPValue(Actor e, FloatTrait t, Actor other) {
				return e.getP(t, other);
			}

			@Override
			protected void setCValue(Actor e, FloatTrait t, Actor other, Float value) {
				e.setC(t, other, value);
				if (swat.actorEditor.bgEditor.isVisible() && e == dk.getActor(1))
					swat.actorEditor.reloadBGEditorTraitValues();
			}

			@Override
			protected void setPValue(Actor e, FloatTrait t, Actor other, Float value) {
				e.setP(t, other, value);
				if (swat.actorEditor.bgEditor.isVisible() && e == dk.getActor(1))
					swat.actorEditor.reloadBGEditorTraitValues();
			}

			@Override
			protected void showEditedActor(Actor e, Actor other) {
				showActors(e, other);
			}

			@Override
			protected String getOverrideTooltip(FloatTrait t) {
				return "If this box is unchecked, then all the pValues and cValues for this Trait will"
				+" be automatically initialized for you, saving you the work of filling them in"
				+" manually. They'll be calculated based on the Accord traits, or"
				+" set to the real value for Actors whose KnowsMe value is true.";
			}
		};

		custom2PTraits = new CustomPTraitsControl<Actor>(swat,Deikto.TraitType.Actor){
			private static final long serialVersionUID = 1L;
			@Override
			protected float getCValue(Actor e, FloatTrait t, Actor other) {
				return e.getC(t, other);
			}

			@Override
			protected Actor getEditedActor() {	return mActor2; }

			@Override
			protected Actor getOtherEntity() { return mActor; }

			@Override
			protected boolean isOverrided(Actor e, FloatTrait t, Actor other) {
				return e.isOverrided(t, other);
			}

			@Override
			protected float getPValue(Actor e, FloatTrait t, Actor other) {
				return e.getP(t, other);
			}

			@Override
			protected void setCValue(Actor e, FloatTrait t, Actor other, Float value) {
				e.setC(t, other, value);
				if (swat.actorEditor.bgEditor.isVisible() && e == dk.getActor(1))
					swat.actorEditor.reloadBGEditorTraitValues();
			}

			@Override
			protected void setPValue(Actor e, FloatTrait t, Actor other, Float value) {
				e.setP(t, other, value);
				if (swat.actorEditor.bgEditor.isVisible() && e == dk.getActor(1))
					swat.actorEditor.reloadBGEditorTraitValues();
			}

			@Override
			protected void showEditedActor(Actor e, Actor other) {
				showActors(other, e);
			}

			@Override
			protected String getOverrideTooltip(FloatTrait t) {
				return "If this box is unchecked, then all the pValues and cValues for this Trait will"
				+" be automatically initialized for you, saving you the work of filling them in"
				+" manually. They'll be calculated based on the Accord traits, or"
				+" set to the real value for Actors whose KnowsMe value is true.";
			}
		};

	}

//**********************************************************************
	/*
	private class CoordinateSet implements Serializable {
		private static final long serialVersionUID = 0L;
		ArrayList<Float> coord = new ArrayList<Float>();
		public CoordinateSet(float x, float y) {
			coord.add(x);
			coord.add(y);
		}			
		public CoordinateSet(float x, float y, float z) {
			coord.add(x);
			coord.add(y);
			coord.add(z);
		}			
		public CoordinateSet(float w, float x, float y, float z) {
			coord.add(w);
			coord.add(x);
			coord.add(y);
			coord.add(z);
		}			
	}
	//*****************************************************************************
	public void relaxRelationships() {
		ArrayList<CoordinateSet> actorPosition = new ArrayList<CoordinateSet>();
		
		int cActors = 9;
		int cDimensions = 2;
		actorPosition.add(new CoordinateSet(0,0));
		actorPosition.add(new CoordinateSet(0,100));
		actorPosition.add(new CoordinateSet(0, -100));
		actorPosition.add(new CoordinateSet(100,0));
		actorPosition.add(new CoordinateSet(-100,0));
		actorPosition.add(new CoordinateSet(75,30));
		actorPosition.add(new CoordinateSet(-75, -30));
		actorPosition.add(new CoordinateSet(50,50));
		actorPosition.add(new CoordinateSet(-50,-50));
		FloatTrait goodTrait = dk.getActorTraits().get(1); // this is a nonsensical initialization
		
		ArrayList<ArrayList<Float>> length = new ArrayList<ArrayList<Float>>();
		for (int i=0; (i<cActors); ++i) {
			length.add(new ArrayList<Float>());
			for (int j=0; (j<cActors); ++j) {
				length.get(i).add(0.0f);
			}
		}
		for(FloatTrait t:dk.getActorTraits()) {
			if (Actor.traitName(Actor.PTraitType.Perception,t).equals("pBad_Good"))
				goodTrait = t;
		}

		float viscosity = 100.0f;
		float f; // net force driving actor i
		// print original lengths
		System.out.println("Original lengths");
		for (int i=0; (i<cActors); ++i) {
			System.out.print(i+": ");
			float sumF = 0.0f;
			Actor thisActor = dk.getActor(i+1); // +1 because we're skipping Fate
			for (int j=0; (j<cActors); ++j) {
				float x = thisActor.getP(goodTrait, dk.getActor(j+1)); // +1 because we're skipping Fate
				length.get(i).set(j, (1.0f - x) / (1.0f + x));
				f = length.get(i).get(j);
				float mf = f * 100;
				int mi = (int)mf;
				f = (float)mi/100;
				System.out.print(f+" ");
				sumF+=f;
			}
			System.out.println("   ---   "+sumF);
		}
		
		double d; // distance between actor i and actor j
		float[] fc = new float[cDimensions];
		float netF = 0.0f;
		for (int n=0; (n<99); ++n) { // 99 runs to converge the relaxation
			netF = 0.0f;
			for (int i=0; (i<cActors); ++i) {
				for (int k=0; (k<cDimensions); ++k) fc[k] = 0.0f;
				for (int j=0; (j<cActors); ++j) {
					if (i != j) {
						d = 0.0f;
						for (int k=0; (k<cDimensions); ++k) {
							d+= (actorPosition.get(i).coord.get(k) - actorPosition.get(j).coord.get(k))
								*(actorPosition.get(i).coord.get(k) - actorPosition.get(j).coord.get(k));
						}
						d = Math.sqrt(d);
						f = (float)d - length.get(i).get(j);
						for (int k=0; (k<cDimensions); ++k) 
							fc[k] += f * (actorPosition.get(j).coord.get(k) - actorPosition.get(i).coord.get(k)) / d;
						netF += f;
					} // if-statement
				} // j-loop
				for (int k=0; (k<cDimensions); ++k) 
					actorPosition.get(i).coord.set(k, actorPosition.get(i).coord.get(k) +fc[k]/viscosity);
			} // i-loop
			if ((n % 9) == (n / 9))
				System.out.println(n+" "+netF);
		} // n-loop
		System.out.println("Final Fit:  "+netF);
		System.out.println();

		System.out.println("final stresses");
		for (int i=0; (i<cActors); ++i) {
			Actor thisActor = dk.getActor(i+1); // +1 because we're skipping Fate
			System.out.print(i+": ");
			float sumF = 0.0f;
			for (int j=0; (j<cActors); ++j) {
				if (i != j) {
					d = 0.0f;
					for (int k=0; (k<cDimensions); ++k) {
						d+= (actorPosition.get(i).coord.get(k) - actorPosition.get(j).coord.get(k))
						*(actorPosition.get(i).coord.get(k) - actorPosition.get(j).coord.get(k));
					}
					d = Math.sqrt(d);
					f = (1-(float)d)/(1+(float)d);
					thisActor.setP(goodTrait, dk.getActor(j+1), f);
					thisActor.setC(goodTrait, dk.getActor(j+1), 0.0f);
					f = (float)d - length.get(i).get(j);	
					float mf = f * 100;
					int mi = (int)mf;
					f = (float)mi/100;
					sumF += f;
				System.out.print(f+" ");
				}
			}
			System.out.println("  "+sumF);
		}
		// print final lengths
		System.out.println();
		System.out.println("final lengths");
		for (int i=0; (i<cActors); ++i) {
			System.out.print(i+": ");
			float sumF = 0.0f;
			for (int j=0; (j<cActors); ++j) {
				if (i != j) {
					d = 0.0f;
					for (int k=0; (k<cDimensions); ++k) {
						d+= (actorPosition.get(i).coord.get(k) - actorPosition.get(j).coord.get(k))
						*(actorPosition.get(i).coord.get(k) - actorPosition.get(j).coord.get(k));
					}
					d = Math.sqrt(d);
					float mf = (float)d * 100;
					int mi = (int)mf;
					f = (float)mi/100;
					sumF += f;
				System.out.print(f+" ");
				}
			}
			System.out.println("  _____   "+sumF);
		}
	}
	
*/
	/**
	 * Sets the layout for all auxiliary panels.
	 * Widget initialization stuff goes in <code>initWidgets</code>.
	 */
	private void setPanels() {
		Box actorSelectionPanel = Box.createVerticalBox();
		actor1Box.setAlignmentX(0.5f);
		actorSelectionPanel.add(actor1Box);
		JLabel selectionLabel = new JLabel("thinks of");
		selectionLabel.setAlignmentX(0.5f);
		actorSelectionPanel.add(selectionLabel);
		actor2Box.setAlignmentX(0.5f);
		actorSelectionPanel.add(actor2Box);

		JLabel pcLabel = new JLabel("Perceived and Confidence Traits");
		pcLabel.setToolTipText(Utils.toHtmlTooltipFormat("The lower Actor's value of the Trait, as perceived by the upper Actor, and how confident the upper Actor is of their perception."));
		JComponent perceived1AndConfidenceTraits1Panel = new ComponentLabeledPanel(pcLabel);
		perceived1AndConfidenceTraits1Panel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		perceived1AndConfidenceTraits1Panel.setOpaque(false);
		perceived1AndConfidenceTraits1Panel.add(custom1PTraits);
		custom1PTraits.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(4,4,4,4),
				custom1PTraits.getBorder()
		)
		);
		custom1PTraits.getVerticalScrollBar().getModel().addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						custom2PTraits.getVerticalScrollBar().setValue(((BoundedRangeModel)e.getSource()).getValue());
						custom2PTraits.revalidate();
					}
				});

		Box actor1BottomPanel = Box.createHorizontalBox();
		actor1BottomPanel.add(Box.createHorizontalGlue());
		actor1BottomPanel.add(perceived1AndConfidenceTraits1Panel);

		JLabel pcLabel2 = new JLabel("Perceived and Confidence Traits");
		pcLabel2.setToolTipText(Utils.toHtmlTooltipFormat("The lower Actor's value of the Trait, as perceived by the upper Actor, and how confident the upper Actor is of their perception."));
		JComponent perceivedAndConfidenceTraits2Panel = new ComponentLabeledPanel(pcLabel2);
		perceivedAndConfidenceTraits2Panel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		perceivedAndConfidenceTraits2Panel.setOpaque(false);
		perceivedAndConfidenceTraits2Panel.add(custom2PTraits);

		custom2PTraits.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(4,4,4,4),
				custom2PTraits.getBorder()
		)
		);
		custom2PTraits.getVerticalScrollBar().getModel().addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e) {
						custom1PTraits.getVerticalScrollBar().setValue(((BoundedRangeModel)e.getSource()).getValue());
						custom1PTraits.revalidate();
					}
				});
		custom2PTraits.setToolTipText(Utils.toHtmlTooltipFormat("The lower Actor's value of the Trait, as perceived by the upper Actor, and how confident the upper Actor is of their perception."));

		Box actor2BottomPanel = Box.createHorizontalBox();
		actor2BottomPanel.add(Box.createHorizontalGlue());
		actor2BottomPanel.add(perceivedAndConfidenceTraits2Panel);

		Box state1Panel = Box.createVerticalBox();
		Box state2Panel = Box.createVerticalBox();
		for(Actor.ExtraTrait t:Actor.ExtraTraits){
			state1Panel.add(actor1ExtraBoxes.get(t));
			state2Panel.add(actor2ExtraBoxes.get(t));
		}
		state1Panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("State"),
				BorderFactory.createEmptyBorder(5,2,5,2))  
		);
		state2Panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("State"),
				BorderFactory.createEmptyBorder(5,2,5,2))
		);

		Box actor1TopPanelAux = Box.createHorizontalBox();
		actor1TopPanelAux.add(Box.createRigidArea(new Dimension(8,8)));
		actor1TopPanelAux.add(actorSelectionPanel);
		actor1TopPanelAux.add(Box.createHorizontalGlue());
		actor1TopPanelAux.add(state1Panel);
		actor1TopPanelAux.add(Box.createVerticalGlue());

		actor2to1Label.setMaximumSize(actor2to1Label.getPreferredSize());
		Box auxLabelPanel = Box.createHorizontalBox();
		auxLabelPanel.add(Box.createHorizontalGlue());
		auxLabelPanel.add(actor2to1Label);
		auxLabelPanel.add(Box.createHorizontalGlue());

		Box actor2TopPanelAux = Box.createHorizontalBox();
		actor2TopPanelAux.add(Box.createRigidArea(new Dimension(8,8)));
		actor2TopPanelAux.add(auxLabelPanel);		
		actor2TopPanelAux.add(Box.createHorizontalGlue());
		actor2TopPanelAux.add(state2Panel);
		actor2TopPanelAux.add(Box.createVerticalGlue());

		JComponent leftPanel = new JPanel(new BorderLayout());
		leftPanel.setBackground(Utils.lightBackground);
		leftPanel.setBorder(BorderFactory.createEmptyBorder(0,20,0,20));
		leftPanel.add(actor1TopPanelAux,BorderLayout.NORTH);
		leftPanel.add(actor1BottomPanel,BorderLayout.CENTER);
		leftPanel.setMaximumSize(new Dimension(leftPanel.getPreferredSize().width,Integer.MAX_VALUE));

		JComponent rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBackground(Utils.lightBackground);
		rightPanel.setBorder(BorderFactory.createEmptyBorder(0,20,0,20));
		rightPanel.add(actor2TopPanelAux,BorderLayout.NORTH);
		rightPanel.add(actor2BottomPanel,BorderLayout.CENTER);
		rightPanel.setMaximumSize(new Dimension(rightPanel.getPreferredSize().width,Integer.MAX_VALUE));


		Box centerHorizontalBox = Box.createHorizontalBox();
		centerHorizontalBox.add(Box.createHorizontalGlue());
		centerHorizontalBox.add(leftPanel);
		centerHorizontalBox.add(Box.createHorizontalGlue());
		centerHorizontalBox.add(rightPanel);
		centerHorizontalBox.add(Box.createHorizontalGlue());

		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		add(centerHorizontalBox);
		setBackground(Utils.darkBackground);		
	}

	private Box createFloatBox(String label,String description){

		String tooltip = Utils.toHtmlTooltipFormat(description);

		JLabel zLabel = new JLabel(label);
		zLabel.setAlignmentX(0.5f);
		zLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		zLabel.setToolTipText(tooltip);

		Swat.Slider zSlider = new Swat.Slider(JSlider.HORIZONTAL, 0, 98, 50);
		zSlider.setMaximumSize(new Dimension(150, zSlider.getPreferredSize().height));
		zSlider.setMinimumSize(zSlider.getMaximumSize());
		zSlider.setPreferredSize(zSlider.getMaximumSize());
		zSlider.setBackground(Color.white);
		zSlider.setToolTipText(tooltip);		

		Box zBox = Box.createVerticalBox();
		zBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		zBox.add(zLabel);
		zSlider.setAlignmentX(0.5f);
		zSlider.setOpaque(false);
		zBox.add(zSlider);
		zBox.setOpaque(false);
		zBox.setAlignmentX(0.5f);
		zBox.setMaximumSize(zBox.getPreferredSize());
		return zBox;
	}

	//**********************************************************************	
	public void loadActor1Box() {
		userInput=false;
		actor1Box.removeAllItems();
		for (int i = 0; (i < dk.getActorCount()); ++i)
			actor1Box.addItem(dk.getActor(i));		
		int rows = actor1Box.getItemCount();
		if (rows<35)
			actor1Box.setMaximumRowCount(rows);
		else actor1Box.setMaximumRowCount(35);
		if (dk.getActorCount()>0) 
			actor1Box.setSelectedItem(mActor);
		actor1Box.setMaximumSize(null);
		actor1Box.setMaximumSize(actor1Box.getPreferredSize());
		userInput=true;
	}
	//**********************************************************************
	public void loadActor2Box() {
		userInput=false;
		actor2Box.removeAllItems();
		for (int i = 0; (i < dk.getActorCount()); ++i) {
			actor2Box.addItem(dk.getActor(i));
		}
		int rows = actor2Box.getItemCount();
		if (rows<35)
			actor2Box.setMaximumRowCount(rows);
		else actor2Box.setMaximumRowCount(35);
		if (dk.getActorCount()>0) 
			actor2Box.setSelectedItem(mActor2);
		actor2Box.setMaximumSize(null);
		actor2Box.setMaximumSize(actor2Box.getPreferredSize());
		userInput=true;
	}
	//**********************************************************************
	public void refresh() { 
		loadActor1Box();
		loadActor2Box();
		custom1PTraits.reloadTraits();
		custom2PTraits.reloadTraits();

		if (dk.getActors().contains(mActor) && dk.getActors().contains(mActor2))
			showActors(mActor,mActor2);
		else if (dk.getActorCount()>1) 
			showActors(dk.getActor(1),dk.getActor(1));
	}
	public void showActors(Actor actor,Actor actor2) {
		mActor = actor;
		mActor2 = actor2;
		userInput=false;
		actor2to1Label.setText("<html><center>"+mActor2+"<br>thinks of<br>"+mActor+"</center></html>");
		actor2to1Label.setMaximumSize(actor2to1Label.getPreferredSize());
		actor1Box.setSelectedItem(mActor);
		actor2Box.setSelectedItem(mActor2);
		for(Actor.ExtraTrait t:Actor.ExtraTraits)
			((Swat.Slider)actor1ExtraBoxes.get(t).getComponent(1)).mSetValue((int)(50.0f * mActor.get(t,mActor2) + 49));
		for(Actor.ExtraTrait t:Actor.ExtraTraits)
			((Swat.Slider)actor2ExtraBoxes.get(t).getComponent(1)).mSetValue((int)(50.0f * mActor2.get(t,mActor) + 49));
		custom1PTraits.refreshValues();
		custom2PTraits.refreshValues();
		userInput=true;
	}

	public static abstract class Test {

		public static void setPerceptionValue(RelationshipEditor re,String traitLabel,float value){
			CustomPTraitsControl.Test.setPerceptionValue(re.custom1PTraits,traitLabel,value);
		}

		public static void setConfidenceValue(RelationshipEditor re,String traitLabel,float value){
			CustomPTraitsControl.Test.setConfidenceValue(re.custom1PTraits,traitLabel,value);
		}

		public static void setBackPerceptionValue(RelationshipEditor re,String traitLabel,float value){
			CustomPTraitsControl.Test.setPerceptionValue(re.custom2PTraits,traitLabel,value);
		}
		public static void setBackConfidenceValue(RelationshipEditor re,String traitLabel,float value){
			CustomPTraitsControl.Test.setConfidenceValue(re.custom2PTraits,traitLabel,value);
		}


		public static void setSelectedFromActor(RelationshipEditor re,String actorLabel){
			re.actor1Box.setSelectedItem(re.swat.dk.getActor(actorLabel));
		}

		public static void setSelectedToActor(RelationshipEditor re,String actorLabel){
			re.actor2Box.setSelectedItem(re.swat.dk.getActor(actorLabel));
		}
	}
}
