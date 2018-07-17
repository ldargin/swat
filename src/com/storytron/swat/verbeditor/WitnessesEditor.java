package com.storytron.swat.verbeditor;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.Swat;
import com.storytron.swat.util.UndoableAction;
import com.storytron.uber.Sentence;
import com.storytron.uber.Verb;
import com.storytron.uber.operator.Operator;

/** An editor for editing the witness attributes. */
public class WitnessesEditor extends JPanel {
	private static final long serialVersionUID = 1L;
	private Swat swat;
	private JComponent theseActorsPanel;
	private JRadioButton anybodyRB;
	private JRadioButton everybodyRB;
	private JRadioButton nobodyRB;
	private JRadioButton subjectRB;
	private JRadioButton customRB;
	JCheckBox[] jcbs = new JCheckBox[Sentence.MaxWordSockets];

	public WitnessesEditor(Swat swat){
		super(null);
		this.swat = swat;

		initWidgets();
		
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Witnesses"));
		
		anybodyRB.setAlignmentX(0.0f);
		add(anybodyRB);
		everybodyRB.setAlignmentX(0.0f);
		add(everybodyRB);
		subjectRB.setAlignmentX(0.0f);
		add(subjectRB);
		nobodyRB.setAlignmentX(0.0f);
		add(nobodyRB);
		
		theseActorsPanel = Box.createVerticalBox();
		theseActorsPanel.setBorder(BorderFactory.createTitledBorder("These actors:"));
		theseActorsPanel.setMinimumSize(new Dimension(150,20));

		JComponent expandActorsPanel = new JPanel(new GridLayout(1,1));
		expandActorsPanel.setOpaque(false);
		expandActorsPanel.add(theseActorsPanel);
		
		JComponent auxActorsPanel = Box.createHorizontalBox();
		customRB.setAlignmentY(0.0f);
		auxActorsPanel.add(customRB);
		expandActorsPanel.setAlignmentY(0.0f);
		auxActorsPanel.add(expandActorsPanel);
		auxActorsPanel.setAlignmentX(0.0f);
		add(auxActorsPanel);
	}
	
	private void initWidgets(){
		ActionListener l = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (e.getSource()==anybodyRB)
					setWitnesses(Verb.Witnesses.ANYBODY_ON_STAGE);
				else if (e.getSource()==everybodyRB)
					setWitnesses(Verb.Witnesses.EVERYBODY_EVERYWHERE);
				else if (e.getSource()==subjectRB)
					setWitnesses(Verb.Witnesses.SUBJECT_ONLY);
				else if (e.getSource()==nobodyRB)
					setWitnesses(Verb.Witnesses.NOBODY_AT_ALL);
				else if (e.getSource()==customRB)
					setWitnesses(Verb.Witnesses.CUSTOM_SPEC);
				else
					System.out.println("WitnessesEditor.RBListener: unkown source");
			}
		};
		
		ButtonGroup btGroup = new ButtonGroup();
		anybodyRB = new JRadioButton("Anybody on stage");
		anybodyRB.setOpaque(false);
		btGroup.add(anybodyRB);
		anybodyRB.addActionListener(l);
		anybodyRB.setToolTipText(Utils.toHtmlTooltipFormat("Anyone present on the Stage where this Verb is executed."));
		
		everybodyRB = new JRadioButton("Everybody everywhere");
		everybodyRB.setOpaque(false);
		btGroup.add(everybodyRB);
		everybodyRB.addActionListener(l);
		everybodyRB.setToolTipText(Utils.toHtmlTooltipFormat("All Actors on all Stages are instantly aware of the Event containing this Verb."));
		
		subjectRB = new JRadioButton("Subject only");
		subjectRB.setOpaque(false);
		btGroup.add(subjectRB);
		subjectRB.addActionListener(l);
		subjectRB.setToolTipText(Utils.toHtmlTooltipFormat("Only the subject will witness the Event containing this verb."));
		
		nobodyRB = new JRadioButton("Nobody at all");
		nobodyRB.setOpaque(false);
		btGroup.add(nobodyRB);
		nobodyRB.addActionListener(l);
		nobodyRB.setToolTipText(Utils.toHtmlTooltipFormat("Nobody witnesses the Event containing this Verb."));
		
		customRB = new JRadioButton();
		customRB.setOpaque(false);
		btGroup.add(customRB);
		customRB.addActionListener(l);
		customRB.setToolTipText(Utils.toHtmlTooltipFormat("Specify who should witness the event."));
	}
	
	private void setWitnesses(final Verb.Witnesses newWitnesses){
		final Verb v = swat.verbEditor.getVerb();
		final Verb.Witnesses oldWitnesses = v.getWitnesses();
		
		if (oldWitnesses==newWitnesses)
			return;
			
		v.setWitnesses(newWitnesses);
		setEnabledWitnessCBs(newWitnesses==Verb.Witnesses.CUSTOM_SPEC);
			
		new UndoableAction(swat,false,"change witnesses of "+v.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				v.setWitnesses(newWitnesses);
				swat.verbEditor.setVerb(v);
				setWitnessesCB(newWitnesses);
				swat.verbEditor.verbPropertiesEditor.setVisible(true);
			}
			@Override
			protected void myUndo() {
				v.setWitnesses(oldWitnesses);
				swat.verbEditor.setVerb(v);
				setWitnessesCB(oldWitnesses);
				swat.verbEditor.verbPropertiesEditor.setVisible(true);
			}
		};
	}
	
	/** 
	 * Call this after changing the amount of Actor wordsockets in the current verb,
	 * or immediately after changing the current verb.
	 * */
	public void reloadActorWordsockets(){
		theseActorsPanel.removeAll();
		Verb v = swat.verbEditor.getVerb();
		for(int i=2;i<Sentence.MaxWordSockets;i++){
			if (v.isWordSocketActive(i) && v.getWordSocketType(i)==Operator.Type.Actor)
				theseActorsPanel.add(createCheckBox(i));
		}
		refreshValues();
		theseActorsPanel.revalidate();
		theseActorsPanel.repaint();
	}
	
	/** Called to set the Witnesses value shown in the GUI. */
	private void setWitnessesCB(Verb.Witnesses w){
		switch(w){
		case ANYBODY_ON_STAGE:
			anybodyRB.setSelected(true);
			anybodyRB.requestFocusInWindow();
			setEnabledWitnessCBs(false);
			break;
		case EVERYBODY_EVERYWHERE:
			everybodyRB.setSelected(true);
			everybodyRB.requestFocusInWindow();
			setEnabledWitnessCBs(false);
			break;
		case NOBODY_AT_ALL:
			nobodyRB.setSelected(true);
			nobodyRB.requestFocusInWindow();
			setEnabledWitnessCBs(false);
			break;
		case SUBJECT_ONLY:
			subjectRB.setSelected(true);
			subjectRB.requestFocusInWindow();
			setEnabledWitnessCBs(false);
			break;
		case CUSTOM_SPEC:
			customRB.setSelected(true);
			customRB.requestFocusInWindow();
			setEnabledWitnessCBs(true);
			break;
		default:
			System.out.println("WitnessesEditor.setWitnesses: unkown witnesses spec");
		}
	}
	
	/** 
	 * Call this when the witness values of a verb have changed but not the amount or
	 * type of the wordsockets.
	 * */
	public void refreshValues(){
		Verb v = swat.verbEditor.getVerb();
		setWitnessesCB(v.getWitnesses());
		for(int i=2;i<Sentence.MaxWordSockets;i++){
			if (v.isWordSocketActive(i) && v.getWordSocketType(i)==Operator.Type.Actor)
				jcbs[i].setSelected(v.isWitness(i));
		}
	}
	
	private void setEnabledWitnessCBs(boolean enabled){
		for(int i=0;i<theseActorsPanel.getComponentCount();i++)
			((JCheckBox)theseActorsPanel.getComponent(i)).setEnabled(enabled);
	}
	
	private final ActionListener cbListener = new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			final JCheckBox cb = (JCheckBox)e.getSource();
			setWitness(Utils.indexOf(jcbs,cb),cb.isSelected());
		}
	}; 
	public JCheckBox createCheckBox(int iSocket){
		if (jcbs[iSocket]==null) {
			final String label = Verb.getWordSocketFullLabel(Operator.Type.Actor, iSocket);
			JCheckBox cb = new JCheckBox(label);
			cb.addActionListener(cbListener);
			cb.setOpaque(false);
			cb.setToolTipText(Utils.toHtmlTooltipFormat("Check to have "+label+" witness the event."));
			jcbs[iSocket] = cb;
		}
		return jcbs[iSocket];
	}
	
	private void setWitness(final int iSocket,final boolean w){
		final Verb v = swat.verbEditor.getVerb();
		if (w==v.isWitness(iSocket))
			return;
		
		v.getWSData(iSocket).witness = w;
		new UndoableAction(swat,false,"change witnessing of "+v.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				v.getWSData(iSocket).witness = w;
				swat.verbEditor.setVerb(v);
				setWitnessCB(iSocket,w);
				swat.verbEditor.verbPropertiesEditor.setVisible(true);
			}
			@Override
			protected void myUndo() {
				v.getWSData(iSocket).witness = !w;
				swat.verbEditor.setVerb(v);
				setWitnessCB(iSocket,!w);
				swat.verbEditor.verbPropertiesEditor.setVisible(true);
			}
		};
	}
	
	/** Called to set the value shown by the GUI. */
	private final void setWitnessCB(int iSocket,boolean w){
		jcbs[iSocket].setSelected(w);
		jcbs[iSocket].requestFocusInWindow();
	};
	
	public static void main(String[] args){
		JFrame f = new JFrame();
		f.getContentPane().add(new WitnessesEditor(null));
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public abstract static class Test {
		public static void setWitnesses(WitnessesEditor we,Verb.Witnesses w){
			we.setWitnesses(w);
		}
	}
}
