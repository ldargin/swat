package com.storytron.swat.verbeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.Swat;
import com.storytron.swat.util.EditorListener;
import com.storytron.swat.util.UndoableAction;
import com.storytron.swat.util.UndoableSlider;
import com.storytron.uber.Script;
import com.storytron.uber.Verb;
import com.storytron.uber.operator.Operator;

/** 
 * Implementation of an editor for verb properties.
 * <p> 
 * All the properties of a verb
 * that are not consequences, scripts, emotional reactions or
 * options, are considered to belong here.
 * */
public final class VerbPropertiesEditor extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final ArrayList<String> expressionList = new ArrayList<String>();

	private final VerbEditor verbEditor;
	private boolean blockSpinner = false;
	private final SocketEditor se;
	final PresenceEditor presenceEditor;
	final WitnessesEditor witnessesEditor;

	private final SpinnerNumberModel timeToPrepareModel = new SpinnerNumberModel(1, 1, 1000, 1);
	private final JSpinner timeToPrepareSpinner = new JSpinner(timeToPrepareModel);
	private final SpinnerNumberModel timeToExecuteModel = new  SpinnerNumberModel(1, 1, 1000, 1);
	private final JSpinner timeToExecuteSpinner = new JSpinner(timeToExecuteModel);
	private final Swat.Slider trivial_MomentousSlider = new Swat.Slider(JSlider.HORIZONTAL, 0, 196, 99);	
	private JComboBox expressionBox;
	private JLabel trivial_MomentousLabel, expressionIcon;
	private final JCheckBox hijackableCheckBox=new JCheckBox("hijackable");
	private final JCheckBox occupiesDirObjectCheckBox=new JCheckBox("occupiesDirObject");
	final JCheckBox useAbortScriptCheckBox=new JCheckBox("use abort script");
	private final Swat.TextArea descriptionTextArea = new Swat.TextArea();
	private ExpressionUndoableAction expressionUndoableAction;
	private EditorListener descriptionTextAreaListener;
		
	public VerbPropertiesEditor(VerbEditor ve){
		super(ve.swat.getMyFrame());

		verbEditor = ve;
		se = new SocketEditor(verbEditor,this);
		presenceEditor = new PresenceEditor(ve.swat);
		witnessesEditor = new WitnessesEditor(ve.swat);

		initWidgets();
		
		hijackableCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If checked, the first Actor to react to this Verb prevents other Actors from reacting. Otherwise, any and all Actors can react to it."));
		occupiesDirObjectCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If checked, then the DirObject can't do anything else while this Verb is executing. Otherwise, the DirObject can do something else even while this Verb is executing."));
		useAbortScriptCheckBox.setToolTipText(Utils.toHtmlTooltipFormat("If checked, then an AbortIf Script button is created just above the Consequences area and the execution of the Verb will be aborted if the condition specified in that AbortIf Script is TRUE."));
		timeToPrepareSpinner.setToolTipText(Utils.toHtmlTooltipFormat("Specifies the amount of time that must elapse between the time of creating a Plan using this Verb and the earliest time that the Verb can be executed."));
		timeToExecuteSpinner.setToolTipText(Utils.toHtmlTooltipFormat("Specifies the amount of time that the Subject and DirObject are occupied in the execution of the Verb."));
		trivial_MomentousSlider.setToolTipText(Utils.toHtmlTooltipFormat("Specifies just how important this Verb is in the overall scheme of things."));
		trivial_MomentousLabel.setToolTipText(Utils.toHtmlTooltipFormat("Specifies just how important this Verb is in the overall scheme of things."));
		descriptionTextArea.setToolTipText(Utils.toHtmlTooltipFormat("Describes the Verb and its effects for the player."));
		expressionBox.setToolTipText(Utils.toHtmlTooltipFormat("The facial expression that will appear when this Verb is presented to the player."));
		expressionIcon.setToolTipText(Utils.toHtmlTooltipFormat("The facial expression that will appear when this Verb is presented to the player."));
		
		JComponent timeToPrepareBox = Box.createVerticalBox();
		final JLabel timeToPrepareLabel = new JLabel("<html>timeTo<br>Prepare </html>");
		timeToPrepareLabel.setAlignmentX(0.5f);
		timeToPrepareBox.add(timeToPrepareLabel);
		timeToPrepareSpinner.setAlignmentY(1.0f);
		timeToPrepareBox.add(timeToPrepareSpinner);
		timeToPrepareBox.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		Dimension d=timeToPrepareBox.getPreferredSize();
		d.width=65;
		timeToPrepareBox.setPreferredSize(d);
		timeToPrepareBox.setMaximumSize(d);

		JComponent timeToExecuteBox = Box.createVerticalBox();
		final JLabel timeToExecuteLabel = new JLabel("<html>timeTo<br>Execute </html>");
		timeToExecuteLabel.setAlignmentX(0.5f);
		timeToExecuteBox.add(timeToExecuteLabel);
		timeToExecuteBox.add(timeToExecuteSpinner);
		timeToExecuteBox.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		timeToExecuteBox.setPreferredSize(d);
		timeToExecuteBox.setMaximumSize(d);

		JComponent auxTimePanel = Box.createHorizontalBox();
		auxTimePanel.add(timeToPrepareBox);
		auxTimePanel.add(timeToExecuteBox);

		JComponent trivial_MomentousBox = Box.createVerticalBox();			
		trivial_MomentousBox.add(trivial_MomentousLabel);
		trivial_MomentousBox.add(trivial_MomentousSlider);
		d=trivial_MomentousBox.getPreferredSize();
		d.width = 180;
		trivial_MomentousBox.setMinimumSize(d);
		trivial_MomentousBox.setPreferredSize(d);
		trivial_MomentousBox.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));

		JComponent timingPanel = Box.createVerticalBox();
		timingPanel.setBorder(BorderFactory.createTitledBorder("Timing"));
		timingPanel.add(auxTimePanel);
		trivial_MomentousBox.setAlignmentX(0.5f);
		timingPanel.add(trivial_MomentousBox);

		JComponent cbBox=Box.createVerticalBox();
		hijackableCheckBox.setAlignmentX(0.0f);
		cbBox.add(hijackableCheckBox);
		occupiesDirObjectCheckBox.setAlignmentX(0.0f);
		cbBox.add(occupiesDirObjectCheckBox);
		useAbortScriptCheckBox.setAlignmentX(0.0f);
		cbBox.add(useAbortScriptCheckBox);
		
		d=expressionBox.getPreferredSize();
		d.width=130;
		expressionBox.setMaximumSize(d);
		
		JComponent expressionPanel = Box.createVerticalBox();
		expressionIcon.setAlignmentX(0.5f);
		expressionPanel.add(expressionIcon);
		expressionPanel.add(Box.createRigidArea(new Dimension(5,5)));
		expressionBox.setAlignmentX(0.5f);
		expressionPanel.add(expressionBox);
		expressionPanel.setBorder(BorderFactory.createTitledBorder("Expression"));

		JScrollPane textScroll = new JScrollPane(descriptionTextArea);
		
		Box descriptionPanel = Box.createHorizontalBox();
		descriptionPanel.setBorder(BorderFactory.createTitledBorder("Description"));
		descriptionPanel.add(textScroll);

		JComponent topLeftPanel = Box.createHorizontalBox();
		topLeftPanel.add(expressionPanel);
		topLeftPanel.add(descriptionPanel);
		
		JComponent middlePanel = Box.createHorizontalBox();
		middlePanel.add(cbBox);
		middlePanel.add(timingPanel);

		JComponent leftPanel = Box.createVerticalBox();
		topLeftPanel.setAlignmentX(0.5f);
		leftPanel.add(topLeftPanel);
		middlePanel.setAlignmentX(0.5f);
		leftPanel.add(middlePanel);
		se.setAlignmentX(0.5f);
		leftPanel.add(se);
		JComponent padding = new JPanel(new BorderLayout());
		padding.setBackground(se.getBackground());
		padding.add(Box.createRigidArea(new Dimension(30,30)),BorderLayout.NORTH);
		padding.setAlignmentX(0.5f);
		leftPanel.add(padding);
		
		witnessesEditor.setOpaque(false);
		presenceEditor.setOpaque(false);
		
		JComponent presencePanel = new JPanel(new BorderLayout());
		presencePanel.setOpaque(false);
		presencePanel.add(presenceEditor,BorderLayout.NORTH);
		
		JComponent rightPanel = Box.createVerticalBox();
		witnessesEditor.setAlignmentX(0.5f);
		rightPanel.add(witnessesEditor);
		presencePanel.setAlignmentX(0.5f);
		rightPanel.add(presencePanel);
		
		
		JComponent mainPanel = Box.createHorizontalBox();
		leftPanel.setAlignmentY(0f);
		mainPanel.add(leftPanel);
		rightPanel.setAlignmentY(0f);
		mainPanel.add(rightPanel);
		
		getContentPane().add(mainPanel);
		getContentPane().setBackground(Utils.lightBackground);
		setLocation(35,80);
		setResizable(false);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	}

	public static void loadExpressionList() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(Utils.getImagePath("emoticubes/listing.txt").openStream()));
		String line=br.readLine();
		while(line!=null) {
			if (line.toLowerCase().endsWith(".png")) {
				expressionList.add(line.substring(0,line.length()-4));
			}
			line=br.readLine();
		}
		br.close();
	} 
	
	/**
	 * Initializes widgets for timeToPrepare, timeToExecute, momentous and expression.
	 * */
	private void initWidgets(){
		timeToPrepareSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!blockSpinner) {
					final Verb verb = verbEditor.getVerb();
					final int oldValue = verb.getTimeToPrepare();
					final int newValue = Integer.parseInt((timeToPrepareSpinner.getValue()).toString());
					new UndoableAction(verbEditor.swat,"change time to prepare of "+verb.getLabel()){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							verbEditor.setVerb(verb);							
							verb.setTimeToPrepare(newValue);
							blockSpinner = true;
							timeToPrepareSpinner.setValue(newValue);
							blockSpinner = false;
							setVisible(true);
						}
						@Override
						public void myUndo() {
							verbEditor.setVerb(verb);							
							verb.setTimeToPrepare(oldValue);
							blockSpinner = true;
							timeToPrepareSpinner.setValue(oldValue);
							blockSpinner = false;
							setVisible(true);
						}
					};				
				}
			}
		});
		
		timeToExecuteSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (!blockSpinner) {
					final Verb verb = verbEditor.getVerb();
					final int oldValue = verb.getTimeToExecute(); 
					final int newValue = Integer.parseInt((timeToExecuteSpinner.getValue()).toString());				
					new UndoableAction(verbEditor.swat,"change time to execute of "+verb.getLabel()){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							verbEditor.setVerb(verb);							
							verb.setTimeToExecute(newValue);
							blockSpinner = true;
							timeToExecuteSpinner.setValue(newValue);
							blockSpinner = false;
							setVisible(true);
						}
						@Override
						public void myUndo() {
							verbEditor.setVerb(verb);							
							verb.setTimeToExecute(oldValue);
							blockSpinner = true;
							timeToExecuteSpinner.setValue(oldValue);
							blockSpinner = false;
							setVisible(true);
						}
					};				
				}
			}
		});
		timeToExecuteSpinner.setAlignmentX(0.5f);

		trivial_MomentousLabel = new JLabel("Trivial_Momentous");
		trivial_MomentousLabel.setOpaque(false);
		trivial_MomentousLabel.setAlignmentX(0.0f);
		trivial_MomentousLabel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));

		trivial_MomentousSlider.setMajorTickSpacing(49);
		trivial_MomentousSlider.setMinorTickSpacing(7);
		trivial_MomentousSlider.setPaintTicks(true);
		trivial_MomentousSlider.setOpaque(false);
		trivial_MomentousSlider.setAlignmentX(0.0f);
		trivial_MomentousSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				trivial_MomentousLabel.setText("Trivial_Momentous: "+String.format("%1.2f",((float)(trivial_MomentousSlider.getValue()-98))*99.0f/9800));
			}
		});
		new UndoableSlider(verbEditor.swat,trivial_MomentousSlider){
			Verb verb;			
			@Override
			public int init() {
				verb=verbEditor.getVerb();
				return toTrivialMomentousSlider(verb.getTrivial_Momentous());
			}
			@Override
			public void setValue(int value) {
				verb.setTrivial_Momentous(fromTrivialMomentousSlider(value));
				trivial_MomentousLabel.setText("Trivial_Momentous: "+String.format("%1.2f",verb.getTrivial_Momentous()));
			}
			@Override
			public void undoRedoExecuted() {
				verbEditor.setVerb(verb);							
				setVisible(true);
			}
			public String getPresentationName() { return "change trivial momentous of "+verb.getLabel();	}
		};				

		expressionBox = new JComboBox();
		expressionBox.setBackground(Color.white);
		for (String st: expressionList)
			expressionBox.addItem(st);
		expressionBox.setMaximumRowCount(17); // this determines the maximum displayed height of the drop down menu

		expressionBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!e.getActionCommand().equals("permit")) return;

				long current = System.currentTimeMillis();

				final Verb verb = verbEditor.getVerb();
				final String oldExpression = verb.getExpression();
				final int index = expressionBox.getSelectedIndex();

				verb.setExpression(expressionList.get(index));
				expressionIcon.setIcon(new ImageIcon(Utils.getImagePath("emoticubes/"+expressionList.get(index)+".png")));

				if (expressionUndoableAction!=null &&
						current-expressionUndoableAction.timestamp<5000) {
					expressionUndoableAction.timestamp = current;
					expressionUndoableAction.index = expressionBox.getSelectedIndex();
					return;
				}


				expressionUndoableAction = new ExpressionUndoableAction(verbEditor.swat,false,"change expresion of "+verb.getLabel(),
													current,index){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						verbEditor.setVerb(verb);
						verb.setExpression(expressionList.get(this.index));
						expressionBox.setActionCommand("block");
						expressionBox.setSelectedIndex(this.index);
						expressionBox.setActionCommand("permit");
						expressionIcon.setIcon(new ImageIcon(Utils.getImagePath("emoticubes/"+verb.getExpression()+".png")));
						setVisible(true);
					}
					@Override
					public void myUndo() {
						verbEditor.setVerb(verb);
						verb.setExpression(oldExpression);
						expressionBox.setActionCommand("block");
						expressionBox.setSelectedItem(oldExpression);
						expressionBox.setActionCommand("permit");
						expressionIcon.setIcon(new ImageIcon(Utils.getImagePath("emoticubes/"+verb.getExpression()+".png")));
						setVisible(true);
					}
				};
			}
		});

		expressionIcon = new JLabel();
		expressionIcon.setOpaque(false);
		
		descriptionTextArea.setLineWrap(true);
		descriptionTextArea.setWrapStyleWord(true);
		descriptionTextAreaListener = new EditorListener(descriptionTextArea){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean timedActionPerformed(ActionEvent e) {
				final Verb v = verbEditor.getVerb();
				final String oldDesc = v.getDescription();
				final String newDesc = descriptionTextArea.getText().trim();
				if (newDesc.equals(oldDesc))
					return true;
				
				v.setDescription(newDesc);
				new UndoableAction(verbEditor.swat,false,"edit description of "+v.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						verbEditor.setVerb(v);
						v.setDescription(newDesc);
						descriptionTextArea.setText(newDesc);
						setVisible(true);
					}
					@Override
					public void myUndo() {
						verbEditor.setVerb(v);
						v.setDescription(oldDesc);
						descriptionTextArea.setText(oldDesc);
						setVisible(true);
					}
				};
				return true;
			}
			@Override
			public String getText() { return verbEditor.getVerb().getDescription(); }
		};

		hijackableCheckBox.setOpaque(false);
		hijackableCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final boolean oldValue = verbEditor.getVerb().getHijackable();
				final Verb verb = verbEditor.getVerb();
				verb.setHijackable(!oldValue);
				new UndoableAction(verbEditor.swat,false,"set hijackable of "+verb.getLabel()) {
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						verbEditor.setVerb(verb);
						verb.setHijackable(!oldValue);
						hijackableCheckBox.setSelected(!oldValue);	
						setVisible(true);
					}
					@Override
					public void myUndo() {
						verbEditor.setVerb(verb);
						verb.setHijackable(oldValue);
						hijackableCheckBox.setSelected(oldValue);
						setVisible(true);
					}
				};
			}
		});
		
		
		occupiesDirObjectCheckBox.setOpaque(false);
		occupiesDirObjectCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final Verb verb = verbEditor.getVerb();
				final boolean oldValue = verb.getOccupiesDirObject();
				verb.setOccupiesDirObject(!oldValue);
				new UndoableAction(verbEditor.swat,false,"set occupiesDirObject of "+verb.getLabel()) {
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo() {
						verbEditor.setVerb(verb);
						verb.setOccupiesDirObject(!oldValue);
						occupiesDirObjectCheckBox.setSelected(!oldValue);	
						setVisible(true);
					}
					@Override
					public void myUndo() {
						verbEditor.setVerb(verb);
						verb.setOccupiesDirObject(oldValue);
						occupiesDirObjectCheckBox.setSelected(oldValue);
						setVisible(true);
					}
				};
			}
		});
		
		useAbortScriptCheckBox.setOpaque(false);
		useAbortScriptCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					toggleUseAbortScript();
				} catch (RuntimeException ex) {
					if (ex.getCause()!=null && ex.getCause() instanceof LimitException) {
						ex.getCause().printStackTrace();
						Utils.displayLimitExceptionMessage((LimitException)ex.getCause(), "Editing error", "Error when toggling the abort if script.");
					} else
						throw ex;
				}
			}
		});

	}

	private void toggleUseAbortScript() {
		final Script oldScript = verbEditor.getVerb().getAbortScript();
		final Verb verb = verbEditor.getVerb();
		new UndoableAction(verbEditor.swat,"set use abort script of "+verb.getLabel()) {
			private static final long serialVersionUID = 1L;
			private Script defaultScript;
			@Override
			protected void myRedo(){
				try {
				if (oldScript==null) {
					if (defaultScript==null)
						defaultScript = verb.defaultAbortScript();
					verbEditor.swat.dk.setAbortScript(verb,defaultScript);
				} else
					verbEditor.swat.dk.setAbortScript(verb,null);
				} catch (LimitException le){
					throw new RuntimeException(le);
				}
			}
			protected void myUndo(){
				try {
					verbEditor.swat.dk.setAbortScript(verb,oldScript);
				} catch (LimitException le){
					throw new RuntimeException(le);
				}
			}
			@Override
			public void update() {
				verbEditor.setVerb(verb);
				verbEditor.updateAbortButton();
				useAbortScriptCheckBox.setSelected(verb.getAbortScript()!=null);
				setVisible(true);
			}
		};
		verbEditor.updateAbortButton();
	}
	
	public void refresh(){
		blockSpinner = true;

		hijackableCheckBox.setSelected(verbEditor.getVerb().getHijackable());
		occupiesDirObjectCheckBox.setSelected(verbEditor.getVerb().getOccupiesDirObject());
		useAbortScriptCheckBox.setSelected(verbEditor.getVerb().getAbortScript()!=null);
		
		descriptionTextArea.setText(verbEditor.getVerb().getDescription());
		timeToPrepareModel.setValue(verbEditor.getVerb().getTimeToPrepare());
		timeToExecuteModel.setValue(verbEditor.getVerb().getTimeToExecute());
		expressionBox.setActionCommand("block");
		expressionBox.setSelectedItem(verbEditor.getVerb().getExpression());
		expressionBox.setActionCommand("permit");
		trivial_MomentousSlider.mSetValue(toTrivialMomentousSlider(verbEditor.getVerb().getTrivial_Momentous()));
		trivial_MomentousLabel.setText("Trivial_Momentous: "+String.format("%1.2g",verbEditor.getVerb().getTrivial_Momentous()));
		expressionIcon.setIcon(new ImageIcon(Utils.getImagePath("emoticubes/"+verbEditor.getVerb().getExpression()+".png")));

		setTitle(verbEditor.getVerb().getLabel());
		expressionUndoableAction = null;
		
		blockSpinner = false;
		
		se.refresh();
		witnessesEditor.reloadActorWordsockets();
		presenceEditor.reloadActorWordsockets();
	}
	
	private static float fromTrivialMomentousSlider(int v){
		return (v-98)*99.0f/9800;
	}
	private static int toTrivialMomentousSlider(float v){
		return (int)(98 + v * 9800/99.0f);
	}

	/** Undoable action for the expression box. */
	private class ExpressionUndoableAction extends UndoableAction {
		private static final long serialVersionUID = 0L;
		public long timestamp;
		public int index;
		ExpressionUndoableAction(Swat swat,boolean runRedo,String presentationName,
				long timestamp,int index){
			super(swat,runRedo,presentationName);
			this.timestamp = timestamp;
			this.index = index;
		}
	}
	
	public abstract static class Test {
		/** @return the trivial momentous slider value. */
		public static float getTrivialMomentousValue(VerbPropertiesEditor vpe){
			return fromTrivialMomentousSlider(vpe.trivial_MomentousSlider.getValue());
		}

		public static void setTrivialMomentousValue(VerbPropertiesEditor vpe,float value){
			vpe.trivial_MomentousSlider.setValue(toTrivialMomentousSlider(value));
		}

		public static void setTimeToPrepareValue(VerbPropertiesEditor vpe,int value){
			vpe.timeToPrepareSpinner.setValue(value);
		}

		public static void setTimeToExecuteValue(VerbPropertiesEditor vpe,int value){
			vpe.timeToExecuteSpinner.setValue(value);
		}

		public static void setDescription(VerbPropertiesEditor vpe,String desc){
			vpe.descriptionTextArea.setText(desc);
			vpe.descriptionTextAreaListener.actionPerformed(null);
		}

		public static void setExpression(VerbPropertiesEditor vpe,String expression){
			vpe.expressionBox.setSelectedIndex(expressionList.indexOf(expression));
		}

		public static void disableWordSocket(VerbPropertiesEditor vpe,int i){
			SocketEditor.Test.disableWordSocket(vpe.se,i);
		}

		public static void setWordSocketType(VerbPropertiesEditor vpe,int i,Operator.Type t){
			SocketEditor.Test.setWordSocketType(vpe.se,i,t);
		}

		public static void setWordSocketNote(VerbPropertiesEditor vpe,int i,String note){
			SocketEditor.Test.setWordSocketNote(vpe.se,i,note);
		}

		public static void toggleHijackable(VerbPropertiesEditor vpe){
			vpe.hijackableCheckBox.doClick();
		}

		public static void toggleOccupiesDirObject(VerbPropertiesEditor vpe){
			vpe.occupiesDirObjectCheckBox.doClick();
		}

		public static void toggleAbortScript(VerbPropertiesEditor vpe){
			vpe.useAbortScriptCheckBox.doClick();
		}

		public static void toggleUseAbortScript(VerbPropertiesEditor vpe) {
			vpe.toggleUseAbortScript();
		}
		
		public static JCheckBox[] getWitnessJCBs(VerbPropertiesEditor vpe) {
			return vpe.witnessesEditor.jcbs;
		}

		public static PresenceEditor.BooleanUndefinedControl[] getPresenceControls(VerbPropertiesEditor vpe) {
			return vpe.presenceEditor.controls;
		}
		
		public static void setWitnesses(VerbPropertiesEditor vpe,Verb.Witnesses w) {
			WitnessesEditor.Test.setWitnesses(vpe.witnessesEditor,w);
		}

	}
	
}
