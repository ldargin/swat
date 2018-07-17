package com.storytron.swat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.UndoableAction;
import com.storytron.swat.util.UndoableSlider;
import com.storytron.uber.Actor;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Word;


/**
 * <p>This class implements an editor for perceived custom traits.
 * It handles a panel of labeled sliders and checkboxes.
 * </p>
 * <p> Each slider is used to edit a perceived or certainty 
 * custom trait value.
 * Next to the sliders is a checkbox that sets the override
 * attribute for that trait. 
 * </p>
 * <p>
 * In order to use this class you need to provide the implementation 
 * of the abstract methods, which provides information about the
 * concept owning the traits (Actor, Stage or Prop). 
 * </p>
 * */
public abstract class CustomPTraitsControl<Entity extends Word> extends JScrollPane {
	private static final long serialVersionUID = 0L;
	private Deikto.TraitType tt;
	private Swat swat;

	/** Gets the entity currently being edited. 
	 * Used when setting slider values. 
	 */
	protected abstract Actor getEditedActor();
	/** Gets the actor toward which the values are edited. */
	protected abstract Entity getOtherEntity();
	/** 
	 * Sets the entity currently being edited in the editor. 
	 * <p>
	 * Called when setting slider values. This is most useful for undo and redo,
	 * when external components must be notified that the value associated to
	 * an actor-entity pair has changed.
	 * @param a is the actor owning the perceived trait. 
	 */
	protected abstract void showEditedActor(Actor a,Entity other);
	/** Gets the value of a perceived trait for a given actor. */
	protected abstract float getPValue(Actor e,FloatTrait t,Entity other);
	/** Gets the certainty value of a trait for a given actor. */
	protected abstract float getCValue(Actor a,FloatTrait t,Entity other);
	/** Gets the overrided value of a trait for a given actor. */
	protected abstract boolean isOverrided(Actor a,FloatTrait t,Entity other);
	
	/** Sets the value of a perceived trait for a given actor and entity. */
	protected abstract void setPValue(Actor a,FloatTrait t,Entity other,Float value);
	/** Sets the value of a certainty trait for a given actor and entity. */
	protected abstract void setCValue(Actor a,FloatTrait t,Entity other,Float value);
	
	/** Returns the tooltip for a given trait perception. */
	protected String getPerceptionTooltip(FloatTrait t){ return null; }
	/** Returns the tooltip for a given trait confidence. */
	protected String getConfidenceTooltip(FloatTrait t){ return null; }
	/** Returns the tooltip for a given trait override flag. */
	protected String getOverrideTooltip(FloatTrait t){ return null; }
	
	
	Map<FloatTrait,Box> pBoxes = new TreeMap<FloatTrait,Box>();
	Map<FloatTrait,Box> cBoxes = new TreeMap<FloatTrait,Box>();
	Map<FloatTrait,JCheckBox> oCBs = new TreeMap<FloatTrait,JCheckBox>();

	/** Updates the sliders to show the trait values of the 
	 * entity being edited.
	 */
	public void refreshValues(){
		Actor editedEntity=getEditedActor();
		Entity other=getOtherEntity();
		
		for(FloatTrait t:swat.dk.getTraits(tt)) {
			((Swat.Slider)pBoxes.get(t).getComponent(1)).mSetValue(toSlider(getPValue(editedEntity,t,other)));
			((Swat.Slider)cBoxes.get(t).getComponent(1)).mSetValue(toSlider(getCValue(editedEntity,t,other)));
			oCBs.get(t).setSelected(isOverrided(editedEntity,t,other));
		}
	}

	
	/**
	 * Constructs a CustomTraitsControl for editing traits of the given
	 * trait type. 
	 * */
	public CustomPTraitsControl(Swat sw,Deikto.TraitType traitType){
		tt=traitType;
		swat=sw;
		setPanels();
	}


	private JComponent oCBPanel, pPanel, cPanel;
	private Color borderColor = UIManager.getLookAndFeelDefaults().getColor("TextField.shadow");
	
    /** Sets the layout for all auxiliary panels. */
	private void setPanels() {

		oCBPanel = new JPanel(null);
		oCBPanel.setLayout(new BoxLayout(oCBPanel,BoxLayout.Y_AXIS));
		oCBPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,3));
		oCBPanel.setBackground(Utils.darkColumnBackground);
		pPanel = Box.createVerticalBox();
		pPanel.setOpaque(false);
		cPanel = new JPanel(null);
		cPanel.setLayout(new BoxLayout(cPanel,BoxLayout.Y_AXIS));
		cPanel.setBackground(Utils.darkColumnBackground);		

		Box actor1OuterTraitSlidersPanel = new SlidersPanel();
		actor1OuterTraitSlidersPanel.add(Box.createHorizontalGlue());
		actor1OuterTraitSlidersPanel.add(oCBPanel);
		actor1OuterTraitSlidersPanel.add(pPanel);		
		actor1OuterTraitSlidersPanel.add(cPanel);
		
		JPanel aux = new JPanel(new GridLayout());
		aux.setOpaque(false);
		aux.add(actor1OuterTraitSlidersPanel);
		setViewportView(aux);
		
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		setBorder(BorderFactory.createLineBorder(borderColor));
		setOpaque(false);
		getViewport().setOpaque(false);
		getVerticalScrollBar().setUnitIncrement(20);
		
	}

	/** 
	 * Routines for converting numbers from slider to model and the other 
	 * way around. 
	 * */
	private static float fromSlider(int v){ return (float)((v-50)/50.0*Utils.MAXI_VALUE); }
	private static int toSlider(float f){ return (int)(f*50/Utils.MAXI_VALUE+50); }
	
	private Box createFloatBox(String label,String description){
		String tooltip = Utils.toHtmlTooltipFormat(description);
		
		JLabel zLabel = new JLabel(label);
		zLabel.setAlignmentX(0.5f);
		zLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		zLabel.setToolTipText(tooltip);
		
		Swat.Slider zSlider = new Swat.Slider(JSlider.HORIZONTAL, 0, 100, 50);
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
		if (tt==Deikto.TraitType.Actor){
			Dimension d = zBox.getPreferredSize();
			d.width = 170;
			zBox.setPreferredSize(d);
			zBox.setMaximumSize(d);
		} else {
			zBox.setMinimumSize(new Dimension(170,CustomTraitsControl.TRAIT_HEIGHT));
			zBox.setPreferredSize(new Dimension(170,CustomTraitsControl.TRAIT_HEIGHT));
			zBox.setMaximumSize(new Dimension(170,CustomTraitsControl.TRAIT_HEIGHT));
		}
		return zBox;
	}

	public void reloadTraits(){
		pPanel.removeAll();
		cPanel.removeAll();
		oCBPanel.removeAll();

		// Initialize P sliders
		for(final FloatTrait t:swat.dk.getTraits(tt)) {
			
			final Box tb = pBoxes.get(t); 
			if (tb!=null){
				((JLabel)tb.getComponent(0)).setText("p"+t.getLabel());
				pPanel.add(tb);
				continue;
			}
				
			final Box b = createFloatBox("p"+t.getLabel(),getPerceptionTooltip(t));
			pBoxes.put(t,b);
			final Swat.Slider sld = (Swat.Slider)b.getComponent(1);
			new UndoableSlider(swat,sld){
				Actor editedEntity;
				Entity other; 
				@Override
				public int init() {
					editedEntity = getEditedActor();
					other = getOtherEntity();
					return toSlider(getPValue(editedEntity,t,other));
				}
				@Override
				public void setValue(int value) {
					setPValue(editedEntity,t,other,fromSlider(value));
				}
				@Override
				public void undoRedoExecuted() {
					Utils.scrollToVisible(b);
					showEditedActor(editedEntity,other);					
				}
				public String getPresentationName() { return "change p"+t.getLabel()+" of "+editedEntity.getLabel();	}
			};	
			sld.addMouseListener(new MouseAdapter(){
				@Override
				public void mousePressed(MouseEvent e) {
					if (!sld.isEnabled()) {
						oCBs.get(t).doClick();
						sld.dispatchEvent(e);
					}
				}
			});
			pPanel.add(b);
		}

		// Initialize C sliders
		for(final FloatTrait t:swat.dk.getTraits(tt)) {
			
			final Box tb = cBoxes.get(t); 
			if (tb!=null){
				((JLabel)tb.getComponent(0)).setText("c"+t.getLabel());
				cPanel.add(tb);
				continue;
			}
			
			final Box b = createFloatBox("c"+t.getLabel(),getConfidenceTooltip(t));
			cBoxes.put(t,b);
			final Swat.Slider sld = (Swat.Slider)b.getComponent(1);
			new UndoableSlider(swat,sld){
				Actor editedEntity;
				Entity other; 
				@Override
				public int init() {
					editedEntity = getEditedActor();
					other = getOtherEntity();
					return toSlider(getCValue(editedEntity,t,other));
				}
				@Override
				public void setValue(int value) {
					setCValue(editedEntity,t,other,fromSlider(value));
				}
				@Override
				public void undoRedoExecuted() {
					Utils.scrollToVisible(b);
					showEditedActor(editedEntity,other);					
				}
				public String getPresentationName() { return "change c"+t.getLabel()+" of "+editedEntity.getLabel();	}
			};	
			sld.addMouseListener(new MouseAdapter(){
				@Override
				public void mousePressed(MouseEvent e) {
					if (!sld.isEnabled()) {
						oCBs.get(t).doClick();
						sld.dispatchEvent(e);
					}
				}
			});
			cPanel.add(b);
		}

		// Initialize checkboxes.
		Dimension d = new Dimension(20,cPanel.getComponentCount()>0?
				cPanel.getComponent(0).getMaximumSize().height:20);
		for(final FloatTrait t:swat.dk.getTraits(tt)) {
			
			final JComponent cb = oCBs.get(t); 
			if (cb!=null){
				oCBPanel.add(cb);
				continue;
			}
			
			final JCheckBox jcb = new JCheckBox();
			jcb.setToolTipText(Utils.toHtmlTooltipFormat(getOverrideTooltip(t)));
			jcb.setMaximumSize(d);
			jcb.setPreferredSize(d);
			jcb.setOpaque(false);
			jcb.addChangeListener(new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					pBoxes.get(t).getComponent(1).setEnabled(jcb.isSelected());
					cBoxes.get(t).getComponent(1).setEnabled(jcb.isSelected());
				}
			});
			jcb.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					final Actor editedEntity = getEditedActor();			
					final Entity other = getOtherEntity();
					final boolean newValue = jcb.isSelected();
					final boolean overrided = isOverrided(editedEntity,t,other);
					final Float p21 = !overrided?null:getPValue(editedEntity,t,other); 
					final Float up21 = !overrided?null:getCValue(editedEntity,t,other);
					new UndoableAction(swat,"change overriding of "+t.getLabel()){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							updateTraitValues(editedEntity,other,newValue);
						}
						@Override
						public void myUndo() {
							updateTraitValues(editedEntity,other,!newValue);
							setPValue(editedEntity,t,other,p21); 
							setCValue(editedEntity,t,other,up21);
						}
						@Override
						public void update(){
							((JComponent)jcb.getParent()).scrollRectToVisible(jcb.getBounds());
							showEditedActor(editedEntity,other);
						}

						private void updateTraitValues(Actor actor,Entity actor2,boolean value){
							if (value) 
								setPValue(actor,t,actor2, getPValue(actor,t,actor2));
							else 
								setPValue(actor,t,actor2,null);
							if (value) 
								setCValue(actor,t,actor2, getCValue(actor,t,actor2));
							else 
								setCValue(actor,t,actor2,null);
						}
					};
					((Swat.Slider)pBoxes.get(t).getComponent(1)).mSetValue(toSlider(getPValue(editedEntity,t,other)));
					((Swat.Slider)cBoxes.get(t).getComponent(1)).mSetValue(toSlider(getCValue(editedEntity,t,other)));
				}
			});
			oCBs.put(t,jcb);
			jcb.setSelected(true);
			oCBPanel.add(jcb);
		}
		
		pPanel.add(Box.createVerticalGlue());
		cPanel.add(Box.createVerticalGlue());
		oCBPanel.add(Box.createVerticalGlue());
		
		setPreferredSize(null);
		d = getPreferredSize();
		d.width = Math.max(d.width, 382);
		setPreferredSize(d);
		setMaximumSize(new Dimension(d.width,Integer.MAX_VALUE));
		revalidate();
		repaint();
	}
	
	private static class SlidersPanel extends Box implements Scrollable {
		private static final long serialVersionUID = 1L;
		
		public SlidersPanel(){
			super(BoxLayout.X_AXIS);
		}
		
		public Dimension getPreferredScrollableViewportSize() {
			return new Dimension(getPreferredSize().width,100);
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
		
		public static void setPerceptionValue(CustomPTraitsControl<?> cpt,String traitLabel,float value){
			int i = cpt.swat.dk.getTraitIndex(cpt.tt, traitLabel);
			JComponent box=(JComponent)cpt.pPanel.getComponent(i);
			if (!((JLabel)box.getComponent(0)).getText().equals("p"+traitLabel))
				throw new RuntimeException("expected: "+traitLabel+" found: "+((JLabel)box.getComponent(0)).getText());
			JCheckBox jcb = cpt.oCBs.get(cpt.swat.dk.getTraits(cpt.tt).get(i));
			if (!jcb.isSelected())
				jcb.doClick();
			((JSlider)box.getComponent(1)).setValue(toSlider(value));
		}

		public static void setConfidenceValue(CustomPTraitsControl<?> cpt,String traitLabel,float value){
			int i = cpt.swat.dk.getTraitIndex(cpt.tt, traitLabel);
			JComponent box=(JComponent)cpt.cPanel.getComponent(i);
			if (!((JLabel)box.getComponent(0)).getText().equals("c"+traitLabel))
				throw new RuntimeException("expected: "+traitLabel+" found: "+((JLabel)box.getComponent(0)).getText());
			JCheckBox jcb = cpt.oCBs.get(cpt.swat.dk.getTraits(cpt.tt).get(i));
			if (!jcb.isSelected())
				jcb.doClick();
			((JSlider)box.getComponent(1)).setValue(toSlider(value));
		}

	}
}
