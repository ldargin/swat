package storyTellerPackage;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.storytron.enginecommon.Utils;
import com.storytron.uber.Quantifier;

/** Browser for inspecting relationship values. */
public abstract class RelationshipBrowser extends JDialog {
	private static final long serialVersionUID = 0L;

	/** 
	 * Redefine this to fetch the relationship values from 
	 * a user defined source.
	 * */
	public abstract void getRelationValues(String relationshipName);

	private String[] knownActors;
	private JRadioButton[] actorRbs;
	private String selectedActor;
	private JComboBox relationshipCB;
	private JLabel whoThinksOfLabel = new JLabel(" ");
	private JLabel whoOthersThinkOfLabel = new JLabel(" ");
	private JLabel relationshipLabel = new JLabel(" ");
	private String relationshipName = " "; 
	private JPanel labelsPanel = new JPanel(new GridLayout(0,1));
	private JPanel toValuesPanel = new JPanel(new GridLayout(0,1)); 
	private JPanel fromValuesPanel = new JPanel(new GridLayout(0,1)); 

	/** 
	 * Construct a relationship browser showing the given actor names and
	 * relationships.
	 * */
	public RelationshipBrowser(Frame owner,String[] actorNames,String[] relationshipNames,final String[] relationshipDescriptions){
		super(owner);
	
		actorRbs = new JRadioButton[actorNames.length];
		labelsPanel.setOpaque(false);
		toValuesPanel.setBackground(Utils.STORYTELLER_LEFT_COLOR);
		fromValuesPanel.setOpaque(false);
		
		relationshipCB = new JComboBox(relationshipNames);
		relationshipCB.setRenderer(new DefaultListCellRenderer(){
			private static final long serialVersionUID = 1L;
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected,
									cellHasFocus);
				setToolTipText(index>=0?relationshipDescriptions[index]:null);
				return c;
			}
		});
		relationshipCB.setBackground(Color.white);
		relationshipCB.setMaximumSize(relationshipCB.getPreferredSize());
		relationshipCB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				relationshipName = (String)relationshipCB.getSelectedItem();
				if (relationshipName!=null) {
					relationshipCB.setToolTipText(relationshipDescriptions[relationshipCB.getSelectedIndex()]);
					relationshipLabel.setText(relationshipName);
					getRelationValues(relationshipName);
				} else {
					setRelationValues(null);
					relationshipCB.setToolTipText(null);
				}
			}
		});

		setKnownActors(actorNames);
		
		JComponent relationshipRows = Box.createHorizontalBox();
		relationshipRows.add(labelsPanel);
		relationshipRows.add(toValuesPanel);
		relationshipRows.add(fromValuesPanel);

		JScrollPane relationshipScrollPane = new JScrollPane(relationshipRows,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		relationshipScrollPane.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.black));
		relationshipScrollPane.setOpaque(false);
		relationshipScrollPane.getViewport().setOpaque(false);
		
		JComponent topLeftSpace = Box.createHorizontalBox();
		topLeftSpace.add(Box.createHorizontalGlue());
		topLeftSpace.add(Box.createHorizontalGlue());
		topLeftSpace.setPreferredSize(new Dimension(labelsPanel.getPreferredSize().width,10));
		topLeftSpace.setMinimumSize(new Dimension(labelsPanel.getMinimumSize().width,10));
		
		JComponent thinksOfOthersPanel = Box.createVerticalBox();
		whoThinksOfLabel.setAlignmentX(0.5f);
		thinksOfOthersPanel.add(whoThinksOfLabel);
		JLabel label = new JLabel("thinks of other's");
		label.setAlignmentX(0.5f);
		thinksOfOthersPanel.add(label);
		relationshipCB.setAlignmentX(0.5f);
		thinksOfOthersPanel.add(relationshipCB);
		thinksOfOthersPanel.setPreferredSize(new Dimension(150,thinksOfOthersPanel.getPreferredSize().height));

		JComponent thinksOfOthersAdjustPanel = new JPanel(null);
		thinksOfOthersAdjustPanel.setLayout(new BoxLayout(thinksOfOthersAdjustPanel,BoxLayout.X_AXIS));
		thinksOfOthersAdjustPanel.setBackground(Utils.STORYTELLER_LEFT_COLOR);
		thinksOfOthersAdjustPanel.add(Box.createHorizontalGlue());
		thinksOfOthersAdjustPanel.add(thinksOfOthersPanel);
		thinksOfOthersAdjustPanel.add(Box.createHorizontalGlue());
		thinksOfOthersAdjustPanel.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));

		JComponent othersThinkOfPanel = Box.createVerticalBox();
		label = new JLabel("What others think");
		label.setAlignmentX(0.5f);
		othersThinkOfPanel.add(label);
		whoOthersThinkOfLabel.setAlignmentX(0.5f);
		othersThinkOfPanel.add(whoOthersThinkOfLabel);
		relationshipLabel.setAlignmentX(0.5f);
		othersThinkOfPanel.add(relationshipLabel);
		othersThinkOfPanel.setPreferredSize(new Dimension(150,othersThinkOfPanel.getPreferredSize().height));

		JComponent othersThinkOfAdjustPanel = Box.createHorizontalBox();
		othersThinkOfAdjustPanel.add(Box.createHorizontalGlue());
		othersThinkOfAdjustPanel.add(othersThinkOfPanel);
		othersThinkOfAdjustPanel.add(Box.createHorizontalGlue());
		othersThinkOfAdjustPanel.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
		
		JComponent headersPanel = Box.createHorizontalBox();
		headersPanel.add(topLeftSpace);
		headersPanel.add(thinksOfOthersAdjustPanel);
		headersPanel.add(othersThinkOfAdjustPanel);
		headersPanel.add(Box.createRigidArea(new Dimension(relationshipScrollPane.getVerticalScrollBar().getPreferredSize().width,10)));
		
		JComponent mainPanel = Box.createVerticalBox();
		headersPanel.setAlignmentX(0.0f);
		mainPanel.add(headersPanel);
		relationshipScrollPane.setAlignmentX(0.0f);
		mainPanel.add(relationshipScrollPane);
		
		getContentPane().add(mainPanel);
		getContentPane().setBackground(Utils.STORYTELLER_RIGHT_COLOR);
		setBackground(Utils.STORYTELLER_RIGHT_COLOR);
		
		setSelectedActor(0);
		// Insert blank boxes for size calculations.
		for(int i=0;i<actorNames.length;i++) {
			toValuesPanel.add(createQuantifierBox(" ",i));
			fromValuesPanel.add(createQuantifierBox(" ",i));
		}

		relationshipCB.setSelectedIndex(0);
		
		pack();
		Dimension d = getPreferredSize();
		d.width = Math.min(d.width, 700);
		d.height = Math.min(d.height, 700);
		setTitle("Relationships");
		setSize(d);
	}

	/** Sets the actor whose values are displayed. */
	private void setSelectedActor(int actorIndex){
		selectedActor = actorIndex>=0?knownActors[actorIndex]:"nobody"; 
		actorRbs[actorIndex].setSelected(true);
		whoThinksOfLabel.setText("What "+selectedActor);
		whoOthersThinkOfLabel.setText("of "+selectedActor+"'s");
	}
	
	/** Sets the values to display for the current actor and relationship. */
	public void setRelationValues(float[] vs){
		toValuesPanel.removeAll();
		fromValuesPanel.removeAll();
		if (vs==null) {
			for(int i=0;i<knownActors.length;i++) {
				toValuesPanel.add(createQuantifierBox(" ",i));
				fromValuesPanel.add(createQuantifierBox(" ",i));
			}
		} else {
			int s2=vs.length/2;
			for(int i=0;i<s2;i++) {
				if (!selectedActor.equals(knownActors[i])) {
					toValuesPanel.add(createQuantifierBox(float2Quantifier(relationshipName,vs[i]),i));
					fromValuesPanel.add(createQuantifierBox(float2Quantifier(relationshipName,vs[s2+i]),i));
				} else {
					toValuesPanel.add(createQuantifierBox(" ",i));
					fromValuesPanel.add(createQuantifierBox(" ",i));
				}
			}
		}
		toValuesPanel.revalidate();
		fromValuesPanel.revalidate();
	}
	
	/** Sets the known actors that must be displayed. */
	public void setKnownActors(String[] knownActors){
		if (Arrays.equals(this.knownActors, knownActors))
			return;
		labelsPanel.removeAll();
		
		this.knownActors = knownActors;
		ButtonGroup bg = new ButtonGroup();
		
		for(int i=0;i<knownActors.length;i++) {
			final String actorName = knownActors[i];
			final int index = i;
			JComponent box = createActorBox(i);
			actorRbs[i] = new JRadioButton();
			actorRbs[i].setText(actorName);
			actorRbs[i].setHorizontalTextPosition(SwingConstants.LEFT);
			actorRbs[i].setOpaque(false);
			actorRbs[i].addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					if (actorRbs[index].isSelected()) {
						setSelectedActor(index);
						getRelationValues(relationshipName);
					}
				}
			});
			bg.add(actorRbs[i]);
			box.add(actorRbs[i]);
			labelsPanel.add(box);
		}
		int i=Utils.indexOf(knownActors, selectedActor);
		if (i>=0)
			setSelectedActor(i);
		else if (knownActors.length>0)
			setSelectedActor(0);
		else
			setSelectedActor(-1);
		
		labelsPanel.revalidate();
		labelsPanel.repaint();
	}
	
	/** 
	 * Creates a label to display text into.
	 * Setups special border, size and alignment of the label. 
	 * */
	private JComponent createQuantifierBox(String text,int actorIndex){
		JLabel l = new JLabel(text);
		JComponent box = Box.createVerticalBox();
		box.setPreferredSize(new Dimension(150,25));
		if (actorIndex>0)
			box.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(1,0,0,0,Color.black),
					BorderFactory.createEmptyBorder(0,5,0,5)));
		else
			box.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
		l.setAlignmentX(0.5f);
		box.add(Box.createVerticalGlue());
		box.add(l);
		box.add(Box.createVerticalGlue());
		return box;
	}

	/** 
	 * Creates a label to display text into.
	 * Setups the label border and alignment. 
	 * */
	private JComponent createActorBox(int actorIndex){
		JComponent box = Box.createHorizontalBox();
		box.setOpaque(false);
		if (actorIndex>0)
			box.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(1,0,0,0,Color.black),
					BorderFactory.createEmptyBorder(0,5,0,5)));
		else
			box.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
		box.add(Box.createHorizontalGlue());
		return box;
	}

	/** Gets the name of the actor being shown. */
	public String getSelectedActorName(){
		return selectedActor;
	}

	/** Gets the name of the relationship being shown. */
	public String getRelationshipName(){
		return relationshipName;
	}
	
	/** 
	 * Asks to refresh the information being displayed using the 
	 * {@link #getRelationValues(String, String)} method.
	 * */
	public void refresh(){
		getRelationValues(relationshipName);
	}

	/** Converts a value to a quantifier text. */
	private String float2Quantifier(String traitName,float value){
		 int quantifierIndex = (int)((value * 5.0f) + 5.5f);
		 if (quantifierIndex < 0)
			 quantifierIndex = 0;
		 if (quantifierIndex > 10)
			 quantifierIndex = 10;
		return Quantifier.getQuantifierLabel(traitName,quantifierIndex,true);
	}
	
	public static void main(String[] args) {
		
		RelationshipBrowser rb = new RelationshipBrowser(null,new String[]{"Uruguay","Brazil","Argentina","Chile","Colombia","Egipto","Congo","Haiti","Jamaica"},
				new String[] {"Quiet_Chatty","Nasty_Nice"},new String[]{"qc description","nn description"}){
			private static final long serialVersionUID = 0L;
			@Override
			public void getRelationValues(String relationshipName) {
				setRelationValues(new float[]{0.0f,1f,-1f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f});
			}
		};

		rb.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
		rb.setVisible(true);
	}
	
	public static abstract class Test {
		
		public static String getRelationshipValue(RelationshipBrowser rb,String actorName){
			int n=Utils.indexOf(rb.knownActors, actorName);
			if (!rb.actorRbs[n].getText().equals(actorName))
				throw new RuntimeException("Actor name does not much. Expected: "+actorName+" found: "+rb.actorRbs[n].getText());
			return ((JLabel)((JComponent)rb.toValuesPanel.getComponent(n)).getComponent(1)).getText();
		}
		
		public static void setSelectedRelationship(RelationshipBrowser rb,String relationshipName){
			rb.relationshipCB.setSelectedItem(relationshipName);
			if (!rb.relationshipCB.getSelectedItem().equals(relationshipName))
				throw new RuntimeException("name is not in combobox: "+relationshipName);
		}

		public static void setSelectedActor(RelationshipBrowser rb,String actorName){
			int n=Utils.indexOf(rb.knownActors, actorName);
			if (!rb.actorRbs[n].getText().equals(actorName))
				throw new RuntimeException("Actor name does not much. Expected: "+actorName+" found: "+rb.actorRbs[n].getText());
			rb.actorRbs[n].doClick();
		}

	}
}
