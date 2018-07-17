package com.storytron.swat;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.UndoableAction;
import com.storytron.uber.Deikto;

/** An editor for the inactivity timeout. */
public class TerminationSettings extends JDialog {
	private static final long serialVersionUID = 0L;
	
	private JSpinner inactivityTimeoutSpinner;
	private Swat swat;
	private boolean userInput=true;
	private UndoableTimeoutSet undoTimeoutAction;
	private long timestamp;

	public TerminationSettings(final Swat swat) {
		super(swat.getMyFrame(),"Termination");
		
		this.swat = swat;
		
		inactivityTimeoutSpinner = new JSpinner(new SpinnerNumberModel(10,Deikto.MINIMUM_INACTIVITY_TIMEOUT,Deikto.MAXIMUM_INACTIVITY_TIMEOUT,(Deikto.MAXIMUM_INACTIVITY_TIMEOUT-Deikto.MINIMUM_INACTIVITY_TIMEOUT)/18));
		JFormattedTextField textfield = ((JSpinner.NumberEditor)inactivityTimeoutSpinner.getEditor()).getTextField();
		textfield.setColumns(3);
		textfield.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e) { 
				try {
					inactivityTimeoutSpinner.commitEdit();
				} catch (Exception exc) {
					return;
				}
				TerminationSettings.this.stateChanged();
			}
			public void removeUpdate(DocumentEvent e) {
				try {
					inactivityTimeoutSpinner.commitEdit();
				} catch (Exception exc) {
					return;
				}
				TerminationSettings.this.stateChanged();
			}
			public void insertUpdate(DocumentEvent e) {
				try {
					inactivityTimeoutSpinner.commitEdit();
				} catch (Exception exc) {
					return;
				}
				TerminationSettings.this.stateChanged();
			}
		});
		Dimension d = inactivityTimeoutSpinner.getPreferredSize();
		inactivityTimeoutSpinner.setMaximumSize(d);
		inactivityTimeoutSpinner.setMinimumSize(d);
		inactivityTimeoutSpinner.setPreferredSize(d);
		inactivityTimeoutSpinner.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				TerminationSettings.this.stateChanged();
			}
		});
		
		JComponent spinnerbox = Box.createHorizontalBox();
		spinnerbox.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
		spinnerbox.add(inactivityTimeoutSpinner);
		spinnerbox.add(new JLabel(" moments."));
		spinnerbox.add(Box.createHorizontalGlue());

		JComponent box = Box.createVerticalBox();
		box.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Inactivity timeout"),
				BorderFactory.createEmptyBorder(0,5,0,5)
			));
		spinnerbox.setAlignmentX(0.0f);
		box.add(spinnerbox);
		JLabel l=new JLabel(Utils.toHtmlTooltipFormat(
				"If nothing happens during the specified amount of time, the story ends."
      			+" Raising this value might be necessary if you have Verbs with very"
      			+" long preparation times, or if you have daily PlotPoints after a long delay."
      			+" Otherwise, it's best to use the smallest possible time limit."));
		l.setAlignmentX(0.0f);
		box.add(l);
		getContentPane().setBackground(Utils.lightBackground);
		setBackground(Utils.lightBackground);
		getContentPane().add(box);
		setMinimumSize(getPreferredSize());
		setMaximumSize(getPreferredSize());
		setSize(getPreferredSize());
	}
	
	/** Called whenever the spinner value has been edited. */
	private void stateChanged(){
		if (!userInput)
			return;

		int oldValue = swat.dk.getInactivityTimeout();
		int newValue = (Integer)inactivityTimeoutSpinner.getValue();

		if (undoTimeoutAction!=null && timestamp+500>System.currentTimeMillis()) {
			if (undoTimeoutAction.value != newValue && newValue != undoTimeoutAction.oldValue) { 
				undoTimeoutAction.value=(Integer)inactivityTimeoutSpinner.getValue();
				timestamp = System.currentTimeMillis();
			}
			return;
		}
		
		if (newValue == oldValue)
			return;

		timestamp = System.currentTimeMillis();

		try {
			swat.dk.setInactivityTimeout(newValue);
		} catch (LimitException ex) {
			Utils.showErrorDialog(TerminationSettings.this,"The value set does not fit the allowed range ("+Deikto.MINIMUM_INACTIVITY_TIMEOUT+"-"+Deikto.MAXIMUM_INACTIVITY_TIMEOUT+").", "Range error");
		}
		undoTimeoutAction=new UndoableTimeoutSet(swat,"set inactivity timeout",newValue,oldValue){
			private static final long serialVersionUID = 0L;
			@Override
			protected void myRedo() {
				try {
					swat.dk.setInactivityTimeout(value);
				} catch (LimitException ex) {
					Utils.showErrorDialog(TerminationSettings.this,"The value set does not fit the allowed range ("+Deikto.MINIMUM_INACTIVITY_TIMEOUT+"-"+Deikto.MAXIMUM_INACTIVITY_TIMEOUT+").", "Range error");
				}
				refresh();
			}
			@Override
			protected void myUndo() {
				try {
					swat.dk.setInactivityTimeout(oldValue);
				} catch (LimitException ex) {
					Utils.showErrorDialog(TerminationSettings.this,"The value set does not fit the allowed range ("+Deikto.MINIMUM_INACTIVITY_TIMEOUT+"-"+Deikto.MAXIMUM_INACTIVITY_TIMEOUT+").", "Range error");
				}
				refresh();
			}
		};
	}
	
	public void refresh(){
		userInput=false;
		inactivityTimeoutSpinner.setValue(swat.dk.getInactivityTimeout());
		userInput=true;
	}
	
	private static class UndoableTimeoutSet extends UndoableAction {
		private static final long serialVersionUID = 0L;
		public int value;
		public int oldValue;
		public UndoableTimeoutSet(Swat swat,String presentationName,int value,int oldValue){
			super(swat,false,presentationName);
			this.value = value;
		}
	}
}
