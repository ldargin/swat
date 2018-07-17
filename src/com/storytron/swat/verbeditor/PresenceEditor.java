package com.storytron.swat.verbeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.Swat;
import com.storytron.swat.util.UndoableAction;
import com.storytron.uber.Sentence;
import com.storytron.uber.Verb;
import com.storytron.uber.operator.Operator;

/** An editor for presence requirements. */
public class PresenceEditor extends JPanel {
	private static final long serialVersionUID = 1L;
	private Swat swat;
	BooleanUndefinedControl[] controls = new BooleanUndefinedControl[Sentence.MaxWordSockets];

	public PresenceEditor(Swat swat){
		super(null);
		this.swat = swat;
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("must (not) be present"));
		
		final BooleanUndefinedControl c = new BooleanUndefinedControl("Fate"){
			private static final long serialVersionUID = 1L;

			public void controlActionPerformed() {
				if (leftCB.isSelected())
					System.out.println("Must");
				else if (rightCB.isSelected())
					System.out.println("Must Not");
				else
					System.out.println("don't care");
			}
		};
		c.setAlignmentX(0.0f);
		add(c);
	}

	
	/** 
	 * Call this after changing the amount of Actor wordsockets in the current verb,
	 * or immediately after changing the current verb.
	 * */
	public void reloadActorWordsockets(){
		removeAll();
		Verb v = swat.verbEditor.getVerb();
		for(int i=2;i<Sentence.MaxWordSockets;i++){
			if (v.isWordSocketActive(i) && v.getWordSocketType(i)==Operator.Type.Actor) {
				JComponent c = createBox(i); 
				c.setAlignmentX(0.0f);
				add(c);
			}
		}
		refreshValues();
		revalidate();
		repaint();
	}

	/** 
	 * Call this when the presence values of a verb have changed but not the amount or
	 * type of the wordsockets.
	 * */
	public void refreshValues(){
		Verb v = swat.verbEditor.getVerb();
		for(int i=2;i<Sentence.MaxWordSockets;i++){
			if (v.isWordSocketActive(i) && v.getWordSocketType(i)==Operator.Type.Actor) {
				controls[i].leftCB.setSelected(v.getPresence(i)==Verb.Presence.REQUIRED);
				controls[i].rightCB.setSelected(v.getPresence(i)==Verb.Presence.ABSENT);
			}
		}
	}
	
	private JComponent createBox(final int iSocket){
		if (controls[iSocket]==null){
			final String label = Verb.getWordSocketFullLabel(Operator.Type.Actor, iSocket);
			controls[iSocket]=new BooleanUndefinedControl(label){
				private static final long serialVersionUID = 1L;
				public void controlActionPerformed() {
					if (leftCB.isSelected())
						setPresence(iSocket,Verb.Presence.REQUIRED);
					else if (rightCB.isSelected())
						setPresence(iSocket,Verb.Presence.ABSENT);
					else
						setPresence(iSocket,Verb.Presence.NOT_REQUIRED);
				}
			};
			controls[iSocket].leftCB.setToolTipText(Utils.toHtmlTooltipFormat("Check to indicate that "+label+" has to be present for the event to execute."));
			controls[iSocket].rightCB.setToolTipText(Utils.toHtmlTooltipFormat("Check to indicate that "+label+" has to be absent for the event to execute."));
		}
		return controls[iSocket]; 
	}
	
	private void setPresence(final int iSocket,final Verb.Presence p){
		final Verb v = swat.verbEditor.getVerb();
		final Verb.Presence oldP = v.getPresence(iSocket);
		if (p==oldP)
			return;
		
		v.getWSData(iSocket).presence = p;
		new UndoableAction(swat,false,"change presence requirements of "+v.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				v.getWSData(iSocket).presence = p;
				swat.verbEditor.setVerb(v);
				setPresenceControl(iSocket,p);
				swat.verbEditor.verbPropertiesEditor.setVisible(true);
			}
			@Override
			protected void myUndo() {
				v.getWSData(iSocket).presence = oldP;
				swat.verbEditor.setVerb(v);
				setPresenceControl(iSocket,oldP);
				swat.verbEditor.verbPropertiesEditor.setVisible(true);
			}
		};
	}
	
	/** Sets a presence value to show in the GUI. */
	private void setPresenceControl(int iSocket,Verb.Presence p){
		controls[iSocket].leftCB.setSelected(p==Verb.Presence.REQUIRED);
		controls[iSocket].rightCB.setSelected(p==Verb.Presence.ABSENT);
		switch(p){
		case NOT_REQUIRED:
		case REQUIRED:
			controls[iSocket].leftCB.requestFocusInWindow();
			break;
		case ABSENT:
			controls[iSocket].rightCB.requestFocusInWindow();
			break;
		}
	}
	
	/** A control implementing two exclusive checkboxes. */
	public static class BooleanUndefinedControl extends JPanel {
		private static final long serialVersionUID = 1L;
		
		public JCheckBox leftCB, rightCB;
		private static final String LEFT = "left";
		private static final String RIGHT = "right";
		
		private BooleanUndefinedControl(String label){
			super(null);
			setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
			setOpaque(false);
			final ActionListener l = new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					if (LEFT==e.getActionCommand())
						rightCB.setSelected(false);
					else if (RIGHT==e.getActionCommand())
						leftCB.setSelected(false);
					controlActionPerformed();
				}
			};
			leftCB = new JCheckBox();
			leftCB.setOpaque(false);
			leftCB.setActionCommand(LEFT);
			leftCB.addActionListener(l);
			rightCB = new JCheckBox();
			rightCB.setOpaque(false);
			rightCB.setActionCommand(RIGHT);
			rightCB.addActionListener(l);
			
			add(new JLabel("  "));
			add(leftCB);
			add(new JLabel("/"));
			add(rightCB);
			add(new JLabel("  "));
			add(new JLabel(label));
		}

		protected void controlActionPerformed() {}
	}

	public static void main(String[] args){
		JFrame f = new JFrame();
		f.getContentPane().add(new PresenceEditor(null));
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
