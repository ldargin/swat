package com.storytron.swat;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.UndoableAction;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Deikto.TraitType;

/** An editor for settings related to the relationship browser of Storyteller. */
public class RelationshipSettings extends JDialog {
	private static final long serialVersionUID = 0L;
	private Swat swat;
	public JCheckBox showRelationshipsJCB = new JCheckBox("show relationships in storyteller");
	private JComponent relationshipsPanel = Box.createVerticalBox();
	private JPopupMenu relationshipsMenu = new JPopupMenu();
	private Map<FloatTrait,JCheckBox> relationshipBoxes = new TreeMap<FloatTrait, JCheckBox>();

	/** Constructs a relationships settings editor for the storyworld edited by swat. */
	public RelationshipSettings(Swat swat){
		super(swat!=null?swat.getMyFrame():null);
		this.swat = swat;

		showRelationshipsJCB.setOpaque(false);
		showRelationshipsJCB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				final boolean newValue = showRelationshipsJCB.isSelected();
				new UndoableAction(RelationshipSettings.this.swat,"make relationships "+
									(newValue?"":"not ")+"visible"){
					private static final long serialVersionUID = 0L;
					@Override
					protected void myRedo() {
						RelationshipSettings.this.swat.dk.setRelationshipsVisible(newValue);
						setRelationshipBoxesEnabled(newValue);
						showRelationshipsJCB.setSelected(newValue);
						setVisible(true);
						showRelationshipsJCB.requestFocusInWindow();
					}
					@Override
					protected void myUndo() {
						RelationshipSettings.this.swat.dk.setRelationshipsVisible(!newValue);
						setRelationshipBoxesEnabled(!newValue);
						showRelationshipsJCB.setSelected(!newValue);
						setVisible(true);
						showRelationshipsJCB.requestFocusInWindow();
					}
				};
			}
		});
		final JScrollPane scrollPane = new JScrollPane(relationshipsPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
	
		JComponent relationshipsBox = Box.createVerticalBox();
		relationshipsBox.setBorder(BorderFactory.createTitledBorder("Relationships visible to the player"));
		scrollPane.setAlignmentX(0.0f);
		relationshipsBox.add(scrollPane);
		
		setBackground(Utils.lightBackground);
		getContentPane().setBackground(Utils.lightBackground);
		getContentPane().setLayout(new BoxLayout(getContentPane(),BoxLayout.Y_AXIS));
		showRelationshipsJCB.setAlignmentX(0.0f);
		getContentPane().add(showRelationshipsJCB);
		relationshipsBox.setAlignmentX(0.0f);
		getContentPane().add(relationshipsBox);

		if (swat!=null)
			refresh();
		
		pack();
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		Dimension d = getPreferredSize();
		d.width = Math.min(Math.max(d.width,250),400);
		d.height = Math.min(Math.max(d.height,400),700);
		setSize(d);
		setMinimumSize(d);
		setPreferredSize(d);
	}
	
	@Override
	public void setVisible(boolean visible){
		if (visible && swat!=null && !isVisible())
			refresh();
		super.setVisible(visible);
	}
	
	/** Performs UI tasks to show the relationship as visible. */
	private void makeRelationshipBox(final FloatTrait t){
		if (relationshipBoxes.containsKey(t)) {
			JCheckBox box = relationshipBoxes.get(t);
			box.setText(t.getLabel());
			box.setSelected(swat.dk.isRelationshipVisible(t));
			relationshipsPanel.add(box);
			return;
		}
			
		final JCheckBox box = new JCheckBox(t.getLabel());
		box.setToolTipText(Utils.toHtmlTooltipFormat("If checked, this relationship will be displayed in the Relationships display in the Storyteller."));
		box.setOpaque(false);
		box.setSelected(swat.dk.isRelationshipVisible(t));
		box.setAlignmentX(0.0f);
		box.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				final FloatTrait t = swat.dk.getTrait(TraitType.Actor, box.getText());
				final boolean newValue = box.isSelected();
				new UndoableAction(swat,"make relationship "+(newValue?"":"not ")+"visible") {
					private static final long serialVersionUID = 0L;
					@Override
					protected void myRedo() {
						swat.dk.setRelationshipVisible(t, newValue);
						box.setSelected(newValue);
						setVisible(true);
						box.requestFocusInWindow();
						box.scrollRectToVisible(box.getBounds());
					}
					@Override
					protected void myUndo() {
						swat.dk.setRelationshipVisible(t, !newValue);
						box.setSelected(!newValue);
						setVisible(true);
						box.requestFocusInWindow();
						box.scrollRectToVisible(box.getBounds());
					}
				};
			}
		});
		relationshipsPanel.add(box);
		relationshipBoxes.put(t, box);
	}

	/** Enables or disables relationship boxes. */
	private void setRelationshipBoxesEnabled(boolean enabled){
		for(int i=0;i<relationshipsPanel.getComponentCount();i++)
			relationshipsPanel.getComponent(i).setEnabled(enabled);
	}
	
	/** 
	 * Updates the UI to show the current state of the settings.
	 * Useful if they were changed by other means than using this editor. 
	 * */
	public void refresh(){
		relationshipsMenu.removeAll();
		relationshipsPanel.removeAll();
		for(FloatTrait t:swat.dk.getActorTraits())
			makeRelationshipBox(t);
		setRelationshipBoxesEnabled(swat.dk.areRelationshipsVisible());
		showRelationshipsJCB.setSelected(swat.dk.areRelationshipsVisible());
		relationshipsPanel.revalidate();
		repaint();
	}
	
	public static void main(String[] args) {
		RelationshipSettings rs = new RelationshipSettings(null);
		rs.setVisible(true);
	}
	
	public static abstract class Test {
		
		public static void toggleRelationshipVisibility(RelationshipSettings r,FloatTrait trait){
			r.relationshipBoxes.get(trait).doClick();
		}

		public static void toggleRelationshipsVisible(RelationshipSettings r){
			r.showRelationshipsJCB.doClick();
		}
	}
}
