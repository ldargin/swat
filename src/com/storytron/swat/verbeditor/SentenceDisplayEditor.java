package com.storytron.swat.verbeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.ToolTipManager;

import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.SentencesPanel;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.DummyEngine;
import com.storytron.swat.Swat;
import com.storytron.swat.util.FlowScrollLayout;
import com.storytron.swat.util.UndoableAction;
import com.storytron.uber.Actor;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Sentence;
import com.storytron.uber.Verb;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;

import engPackage.Interpreter;

/** 
 * This class implements an editor for configuring how sentences should be displayed
 * in storytreller.  
 * <p>
 * The verbs have scripts returning texts for each wordsocket. This class provides
 * an editor for those scripts. It also provides a display to see how sample sentences will
 * look in storyteller. The verb whose wordsockets are edited is taken from VerbEditor.
 * */
public final class SentenceDisplayEditor extends JDialog {
	private static final long serialVersionUID = 1L;

	public ScriptEditor<Pair<Verb,Script>> scriptEditor;
	private Verb mVerb;
	private Swat swat;
	private boolean userInput = true;
	private ButtonGroup scriptButtons = new ButtonGroup();
	private JRadioButton leftRB = new JRadioButton("left panel",false);
	private JRadioButton rightRB = new JRadioButton("right panel",true);
	private JToggleButton unselectScript = new JToggleButton();
	private ButtonGroup panelButtons = new ButtonGroup();
	private ScriptDisplayButton[] scriptBTs = new ScriptDisplayButton[Sentence.MaxWordSockets];
	private JCheckBox[] visibilityCBs = new JCheckBox[Sentence.MaxWordSockets];
	private ScriptDisplayButton[] suffixBTs = new ScriptDisplayButton[Sentence.MaxWordSockets];
	private SentenceDisplayPanel sentenceDisplayPanel = new SentenceDisplayPanel();
	private JComponent bottomLeftPanel;
	private JScrollPane displayScrollPane;
	private DummyEngine dummyEngine;
	private Interpreter interpreter;
	
	/** The action listener to be fired each time a script display button is pressed. */
	private final ActionListener scriptDisplayActionListener = new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			Script s = ((ScriptDisplayButton)e.getSource()).getScript(); 
			if (((ScriptDisplayButton)e.getSource()).isSelected() && (s==null || !mVerb.isWordSocketActive(s.getIWordSocket()))) {
				unselectScript.setSelected(true);
				setScriptPath(null,null);
				switch(Utils.showOptionDialog(SentenceDisplayEditor.this, "If you want to use this WordSocket, use\nthe Properties window to specify what\nkind of word you want to put in it."
						, "Inactive Wordsocket", 0, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"Open Properties window","Close"}, "Open Properties window")){
				case 0:
					swat.verbEditor.verbPropertiesEditor.setVisible(true);
					break;
				default:;
				};
				return;
			}
				
			if (s!=scriptEditor.getScript())
				setScriptPath(((ScriptDisplayButton)e.getSource()).getScriptPath(),((ScriptDisplayButton)e.getSource()).getScript());
			else 
				setScriptPath(null,null);
		};
	};

	/** Constructs a sentence display editor connected to the given Swat instance. */
	public SentenceDisplayEditor(final Swat swat){
		super(swat.getMyFrame(),"Sentence Display Editor");
		this.swat=swat;
		this.dummyEngine = new DummyEngine(null);
		this.interpreter = new Interpreter(dummyEngine,swat.dk);
		initWidgets();
		setupPanels();
	}

	/** Initializes widgets in this editor. */
	private void initWidgets(){
		leftRB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (leftRB.isSelected()) {
					sentenceDisplayPanel.redisplaySentence();
					bottomLeftPanel.setBackground(Utils.STORYTELLER_LEFT_COLOR);
					displayScrollPane.setBackground(Utils.STORYTELLER_LEFT_COLOR);
					displayScrollPane.getViewport().setBackground(Utils.STORYTELLER_LEFT_COLOR);
				}
			}
		});
		rightRB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (rightRB.isSelected()) {
					sentenceDisplayPanel.redisplaySentence();
					bottomLeftPanel.setBackground(Utils.STORYTELLER_RIGHT_COLOR);
					displayScrollPane.setBackground(Utils.STORYTELLER_RIGHT_COLOR);
					displayScrollPane.getViewport().setBackground(Utils.STORYTELLER_RIGHT_COLOR);
				}
			}
		});

		scriptEditor = new ScriptEditor<Pair<Verb,Script>>(swat,OperatorDictionary.ScriptMenus){
			private static final long serialVersionUID = 1L;
			@Override
			protected Pair<Verb,Script> getContainerState() {
				return new Pair<Verb,Script>(mVerb,scriptEditor.getScript());
			}
			@Override
			protected void setContainerState(Pair<Verb,Script> state) {
				swat.verbEditor.setVerb(state.first);
				toggleScriptButton(state.second);
				if (!SentenceDisplayEditor.this.isVisible()) {
					SentenceDisplayEditor.this.setVisible(true);
					SentenceDisplayEditor.this.refresh();
				}
			};
			@Override
			protected void scriptChanged() {
				if (SentenceDisplayEditor.this.isVisible())
					sentenceDisplayPanel.redisplaySentence();
				else {
					SentenceDisplayEditor.this.setVisible(true);
					SentenceDisplayEditor.this.refresh();
				}
			}
		};
		scriptEditor.setScriptalyzerMenuItemVisible(false);

		scriptButtons.add(unselectScript);

		for(int i=0;i<Sentence.MaxWordSockets;i++){
			scriptBTs[i]=new ScriptDisplayButton("",scriptButtons) {
				private static final long serialVersionUID = 1L;
				@Override
				public String getToolTipText(MouseEvent evt) {
					if (getText().length()>0)
						return getScriptPath()!=null && mVerb.isWordSocketActive(getScript().getIWordSocket())?
								Utils.nullifyIfEmpty(mVerb.getNote(getScript().getIWordSocket())):null;
					else
						return Utils.toHtmlTooltipFormat("If you want to use this WordSocket, use the Properties window to specify what kind of word you want to put in it.");
				}
			};
			ToolTipManager.sharedInstance().registerComponent(scriptBTs[i]);
			scriptBTs[i].addActionListener(scriptDisplayActionListener);
			visibilityCBs[i]=new JCheckBox();
			suffixBTs[i]=new ScriptDisplayButton("",scriptButtons){
				private static final long serialVersionUID = 1L;
				@Override
				public String getToolTipText(MouseEvent evt) {
					if (isEnabled())
						return null;
					else
						return Utils.toHtmlTooltipFormat("If you want to use this WordSocket, use the Properties window to specify what kind of word you want to put in it.");
				}
			};
			ToolTipManager.sharedInstance().registerComponent(suffixBTs[i]);
			suffixBTs[i].addActionListener(scriptDisplayActionListener);
		
			final int iSocket = i;
			visibilityCBs[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final Verb verb = mVerb; 
					final boolean oldValue = verb.isVisible(iSocket);
					sentenceDisplayPanel.redisplaySentence();
					
					new UndoableAction(swat,"change wordsocket visibility"){
						private static final long serialVersionUID = 1L;
						@Override
						protected void myRedo() {
							swat.verbEditor.setVerb(verb);
							verb.getWSData(iSocket).visible = !oldValue;
							visibilityCBs[iSocket].setSelected(!oldValue);
							SentenceDisplayEditor.this.setVisible(true);
							visibilityCBs[iSocket].requestFocusInWindow();
							sentenceDisplayPanel.redisplaySentence();
						}
						protected void myUndo() {
							swat.verbEditor.setVerb(verb);
							verb.getWSData(iSocket).visible = oldValue;
							visibilityCBs[iSocket].setSelected(oldValue);
							SentenceDisplayEditor.this.setVisible(true);
							visibilityCBs[iSocket].requestFocusInWindow();
							sentenceDisplayPanel.redisplaySentence();
						}
					};
				}
			});
		}
	}
	
	/** Layouts the widgets in this editor. */
	private void setupPanels(){
		panelButtons.add(leftRB);
		panelButtons.add(rightRB);

		JComponent topBox = Box.createHorizontalBox();
		topBox.add(Box.createRigidArea(new Dimension(88,20)));
		JLabel label = new JLabel("Visible?");
		label.setMaximumSize(label.getPreferredSize());
		label.setToolTipText(Utils.toHtmlTooltipFormat("If checked this WordSocket and its suffix will be shown to the player; otherwise, neither will be shown to the player."));
		topBox.add(label);
		topBox.add(Box.createRigidArea(new Dimension(35,20)));
		label = new JLabel("Suffix");
		label.setToolTipText(Utils.toHtmlTooltipFormat("The text that will be displayed to the player AFTER the word in this WordSocket."));
		topBox.add(label);

		
		JComponent wordsocketsPanel = Box.createVerticalBox();
		final Dimension buttonSize = new Dimension(100,25);
		final Dimension suffixSize = new Dimension(150,25);
		topBox.setAlignmentX(0f);
		wordsocketsPanel.add(topBox);
			
		for(int i=0;i<Sentence.MaxWordSockets;i++){
			scriptBTs[i].setPreferredSize(buttonSize);
			scriptBTs[i].setMinimumSize(buttonSize);
			scriptBTs[i].setMaximumSize(buttonSize);
			visibilityCBs[i].setOpaque(false);
			suffixBTs[i].setPreferredSize(suffixSize);
			suffixBTs[i].setMinimumSize(suffixSize);
			suffixBTs[i].setMaximumSize(suffixSize);
		
			JComponent wordSocketPanel = Box.createHorizontalBox();
			wordSocketPanel.add(scriptBTs[i]);
			wordSocketPanel.add(visibilityCBs[i]);
			wordSocketPanel.add(suffixBTs[i]);
			
			wordSocketPanel.setAlignmentX(0f);
			wordsocketsPanel.add(wordSocketPanel);
		}
		
		JComponent topLeftPanel = new JPanel(new BorderLayout());
		topLeftPanel.setOpaque(false);
		topLeftPanel.add(wordsocketsPanel,BorderLayout.WEST);
		topLeftPanel.add(scriptEditor.scriptPanel,BorderLayout.CENTER);
		topLeftPanel.setMaximumSize(topLeftPanel.getPreferredSize());
		
		JComponent auxFlowPanel = new SentencesPanel(new BorderLayout());
		auxFlowPanel.setOpaque(false);
		auxFlowPanel.add(sentenceDisplayPanel.getSentencePanel());
		
		displayScrollPane = new JScrollPane(auxFlowPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		displayScrollPane.getVerticalScrollBar().setUnitIncrement(10);
		displayScrollPane.setBorder(BorderFactory.createEmptyBorder());
		displayScrollPane.setBackground(Utils.STORYTELLER_RIGHT_COLOR);
		displayScrollPane.getViewport().setBackground(Utils.STORYTELLER_RIGHT_COLOR);
		
		leftRB.setOpaque(false);
		rightRB.setOpaque(false);
		
		JComponent titlePanel = Box.createHorizontalBox();
		titlePanel.setAlignmentX(0f);
		titlePanel.add(new JLabel("Sample sentence for the"));
		titlePanel.add(leftRB);
		titlePanel.add(new JLabel("/"));
		titlePanel.add(rightRB);
		titlePanel.add(new JLabel(":"));
		
		bottomLeftPanel = new JPanel(null);
		bottomLeftPanel.setLayout(new BoxLayout(bottomLeftPanel,BoxLayout.Y_AXIS));
		bottomLeftPanel.setBackground(Utils.STORYTELLER_RIGHT_COLOR);
		bottomLeftPanel.add(titlePanel);
		displayScrollPane.setAlignmentX(0f);
		bottomLeftPanel.add(displayScrollPane);
		
		JSplitPane leftPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		leftPanel.setOpaque(false);
		leftPanel.add(topLeftPanel);
		leftPanel.add(bottomLeftPanel);
		
		getContentPane().add(scriptEditor.mainMenuPanel,BorderLayout.EAST);
		getContentPane().add(leftPanel,BorderLayout.CENTER);
		setBackground(Utils.lightlightBackground);
		getContentPane().setBackground(Utils.lightlightBackground);
	}
	
	/** 
	 * Refreshes the enabled/disabled script display buttons, the comboboxes at
	 * the bottom sentence display and redisplays the sample sentence.
	 * <p>
	 * This method may need to be called if the wordsockets of the current verb were edited,
	 * or if the edited verb in verb editor is changed. 
	 * */
	public void refresh(){
		if (mVerb!=swat.verbEditor.getVerb() || !isValid(scriptEditor.getScript()))
			setScriptPath(null,null);
		else
			scriptEditor.refresh();
		mVerb = swat.verbEditor.getVerb();
		for(int i=0;i<Sentence.MaxWordSockets;i++){
			if (mVerb.isWordSocketActive(i)) {
				visibilityCBs[i].setEnabled(true);
				suffixBTs[i].setEnabled(true);
				
				scriptBTs[i].setText(mVerb.getWordSocketFullLabel(i));
				scriptBTs[i].setScriptPath(new ScriptPath(mVerb,null,null),mVerb.getWordsocketTextScript(i));
				suffixBTs[i].setScriptPath(new ScriptPath(mVerb,null,null),mVerb.getSuffix(i));
				visibilityCBs[i].setSelected(mVerb.isVisible(i));
			} else {
				visibilityCBs[i].setEnabled(false);
				visibilityCBs[i].setSelected(false);
				suffixBTs[i].setEnabled(false);
				
				scriptBTs[i].setText("");
				suffixBTs[i].setText("");
			}
				
		}
		sentenceDisplayPanel.refresh();
	}
	
	/** Tells if the script is valid, or has been removed by previous editings. */
	private boolean isValid(Script s){
		return s==null 
				|| s.getType()==Script.Type.WordsocketLabel 
				   && mVerb.isWordSocketActive(s.getIWordSocket())
				   && mVerb.getWordsocketTextScript(s.getIWordSocket())==s
				|| s.getType()==Script.Type.WordsocketSuffix 
				   && mVerb.isWordSocketActive(s.getIWordSocket())
				   && mVerb.getSuffix(s.getIWordSocket())==s;
	}

	/** 
	 * Sets the script being edited in the script editor.
	 * <p>
	 * The script must be of type WordsocketLabel or WordsocketSuffix.
	 * */
	public void setScriptPath(ScriptPath sp,Script s){
		scriptEditor.setScriptPath(sp,s);
		if (s==null)
			unselectScript.setSelected(true);
		else
			toggleScriptButton(s);
	}
	/** Toggles the script display button that corresponds to the given script. */
	private void toggleScriptButton(Script s){
		if (s.getType()==Script.Type.WordsocketLabel)
			scriptBTs[s.getIWordSocket()].setSelected(true);
		else
			suffixBTs[s.getIWordSocket()].setSelected(true);
	}
	
	private final static Pattern wordSplitter = Pattern.compile(" ");
	private final static Insets insets = new Insets(2,4,2,4);
	/**
	 * This class implements the sentence display shown at the bottom
	 * of the sentence display editor.
	 * <p>
	 * It presents a combobox for each wordsocket, filled with the possible values.
	 * Below each combobox is a label with the result of executing the scripts.
	 * */
	private final class SentenceDisplayPanel {
		private static final long serialVersionUID = 1L;
		private final Dimension cbHeightBox = new Dimension(5,0);
		private final JLabel cbHeightLabel = new JLabel(" ");
		private JComponent sentencePanel;
		private boolean allowLayout = false;
		
		JComponent[] cb = new JComponent[Sentence.MaxWordSockets];
		
		public SentenceDisplayPanel(){
			sentencePanel = Box.createVerticalBox();
			sentencePanel.add(Box.createVerticalGlue());
			sentencePanel.addComponentListener(new ComponentAdapter(){
				public void componentResized(ComponentEvent e) {	doLayout();		}
			});

			ActionListener al = new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					if (!userInput)
						return;
					
					redisplaySentence();
				}
			};
			for(int i=0;i<Sentence.MaxWordSockets;i++) {
				if (i==Sentence.Verb)
					cb[i]=cbHeightLabel;
				else {
					cb[i] = new JComboBox();
					((JComboBox)cb[i]).addActionListener(al);
				}
			}
			cbHeightBox.height = cb[0].getPreferredSize().height;
			cbHeightLabel.setPreferredSize(cbHeightBox);
			cbHeightLabel.setMinimumSize(cbHeightBox);
			cbHeightLabel.setMaximumSize(cbHeightBox);
		}
		
		/** Returns the component used to display the sentence. */
		public JComponent getSentencePanel(){
			return sentencePanel;
		}
		
		private void clearSentencePanel(){
			while(sentencePanel.getComponentCount()>1)
				sentencePanel.remove(0);
		}
		
		/** 
		 * Adds a new paragraph panel to the component hierarchy.
		 * @return the new paragraph panel
		 *  */
		private JComponent newParagraphPanel(){
			final JPanel panel = new JPanel(null);
			
			final JScrollPane scrollPane = new JScrollPane(panel,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER){
				private static final long serialVersionUID = 1L;
				@Override
				public Dimension getMaximumSize() {
					return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);
				}
			};
			FlowScrollLayout l = new FlowScrollLayout(scrollPane) {
				private static final long serialVersionUID = 1L;
				@Override
				public void layoutContainer(Container c) {
					if (allowLayout) { // Layout only when we want to do so explicitly
						Dimension d = scrollPane.getViewport().getExtentSize();
						d.height = 10;
						scrollPane.getViewport().setExtentSize(d);
						super.layoutContainer(c);
						d.height = panel.getPreferredSize().height;
						scrollPane.getViewport().setExtentSize(d);
					}
				}
			};
			l.setAlignment(FlowLayout.LEFT);
			l.setHgap(4);
			l.setVgap(15);
			panel.setLayout(l);

			scrollPane.setBorder(BorderFactory.createEmptyBorder(0,15,5,15));
			scrollPane.setLocation(30,150);
			panel.setOpaque(false);
			scrollPane.setOpaque(false);
			scrollPane.getViewport().setOpaque(false);
			scrollPane.getVerticalScrollBar().setUnitIncrement(20);
			scrollPane.getVerticalScrollBar().setUnitIncrement(40);
			scrollPane.getViewport().setExtentSize(new Dimension(sentencePanel.getWidth(),10));
			
			sentencePanel.add(scrollPane,sentencePanel.getComponentCount()-1);
			
			return panel;
		}

		/** Layouts the word elements. */
		public void doLayout(){
			allowLayout=true;
			for(int i=0;i<sentencePanel.getComponentCount()-1;i++)
				((JScrollPane)sentencePanel.getComponent(i)).getViewport().getView().doLayout();
			sentencePanel.doLayout();
			allowLayout=false;
		}


		/** 
		 * Reloads the comboboxes and redisplays the sample sentence.
		 * <p>
		 * This method may need to be called after adding actors, props, or any
		 * other value that appears in a wordsocket.  
		 * */
		public void refresh(){
			userInput = false;
			for(int i=0;i<Sentence.MaxWordSockets;i++) {
				if (i!=Sentence.Verb && mVerb.isWordSocketActive(i))
					reloadCBvalues((JComboBox)cb[i],mVerb.getWordSocketType(i));
			}
			redisplaySentence();
			userInput = true;
		} 
		
		/** Redisplays the sample sentence. 
		 * <p>
		 * This method may need to be called after editing the scripts
		 * or changing any of the wordsocket visibilities.  
		 * */
		public void redisplaySentence(){
			clearSentencePanel();
			JComponent panel = newParagraphPanel();
			
			Sentence s = new Sentence(swat.dk.getActorCount());
			s.setWordSocket(Sentence.Subject, ((JComboBox)cb[Sentence.Subject]).getSelectedIndex(), Operator.Type.Actor);
			s.setWordSocket(Sentence.Verb, mVerb.getReference().getIndex(), Operator.Type.Verb);
			for(int i=Sentence.Verb+1;i<Sentence.MaxWordSockets;i++) {
				if (mVerb.isWordSocketActive(i))
					s.setWordSocket(i, ((JComboBox)cb[i]).getSelectedIndex(), mVerb.getWordSocketType(i));
			}
					
			ScriptPath sp = new ScriptPath(mVerb,null,null);
			
			dummyEngine.setSentence(s);
			interpreter.leftPanel = leftRB.isSelected();
			for(int i=0;i<Sentence.MaxWordSockets;i++) {
				if (mVerb.isWordSocketActive(i)) {
					interpreter.executeScript(sp,mVerb.getSuffix(i));
					final String suffix = interpreter.getText()!=null?interpreter.getText():
										interpreter.getPoison()?"POISON: "+interpreter.getPoisonCause():"???";
					suffixBTs[i].setText(suffix);
					if (mVerb.isVisible(i)) {
						interpreter.executeScript(sp,mVerb.getWordsocketTextScript(i));
						final String text = interpreter.getText()!=null?interpreter.getText():
										interpreter.getPoison()?"POISON: "+interpreter.getPoisonCause():"???";
						panel.add(makeWSPanel(cb[i],text));
						
						String[] line = suffix.split("\n");
						if (line.length>0 && line[0].trim().length()>0) {
							for(String word:wordSplitter.split(line[0]))
								panel.add(makeSuffixPanel(word));
						}
						for(int j=1;j<line.length;j++) {
							if (line[j].trim().length()>0) {
								panel.revalidate();
								panel.repaint();
								panel = newParagraphPanel();
								for(String word:wordSplitter.split(line[j]))
									panel.add(makeSuffixPanel(word));
							}
						}
					} else if (i!=Sentence.Verb)
						panel.add(makeWSPanel(cb[i],null));
				}
			}
			sentencePanel.revalidate();
			doLayout();
			sentencePanel.repaint();
		}
		
		/** 
		 * Returns a panel for a wordsocket. It will have two rows. The top
		 * row will contain the given component, and the bottom row will
		 * contain a label with the text msg.
		 * */
		private JComponent makeWSPanel(JComponent c,String msg){
			JComponent res = new JPanel(new GridLayout(0,1));
			res.setOpaque(false);
			c.setAlignmentX(0.0f);
			res.add(c);
			if (msg!=null) {
				JButton bt= new JButton(msg);
				bt.setAlignmentX(0.0f);
				bt.setBackground(new Color(250,250,250));
				bt.setFocusable(false);
				bt.setRolloverEnabled(false);
				bt.setModel(new DefaultButtonModel() {
					private static final long serialVersionUID = 0L;
					public boolean isPressed() { return false;	}
					public boolean isArmed() {	return false;	}
				});
				bt.setMargin(insets);
				res.add(bt);
			} else {
				JLabel l = new JLabel(" ");
				l.setPreferredSize(cbHeightBox);
				l.setMinimumSize(cbHeightBox);
				l.setMaximumSize(cbHeightBox);
				res.add(l);
			}
			
			return res;
		}
		/** 
		 * Makes a panel for a suffix. It will have two rows. The top row will be empty
		 * and the second row will contain a label with the text given in msg. 
		 * */
		private JComponent makeSuffixPanel(String msg){
			JComponent res = Box.createVerticalBox();
			res.add(Box.createRigidArea(cbHeightBox));
			res.add(new JLabel(msg));
			return res;
		}
		
		/** Reloads a given combobox with values of the given type. */
		private void reloadCBvalues(JComboBox cb,Operator.Type t){
			Object o = cb.getSelectedItem();
			switch(t){
			case Actor:
				cb.setModel(new ListComboBoxModel(swat.dk.getActors()));
				break;
			case Stage:
				cb.setModel(new ListComboBoxModel(swat.dk.getStages()));
				break;
			case Prop:
				cb.setModel(new ListComboBoxModel(swat.dk.getProps()));
				break;
			case ActorTrait:
				cb.setModel(new ListComboBoxModel(swat.dk.getActorTraits()));
				break;
			case StageTrait:
				cb.setModel(new ListComboBoxModel(swat.dk.getStageTraits()));
				break;
			case PropTrait:
				cb.setModel(new ListComboBoxModel(swat.dk.getPropTraits()));
				break;
			case MoodTrait:
				DefaultComboBoxModel m = (DefaultComboBoxModel) cb.getModel();
				for(Actor.MoodTrait trait:Actor.MoodTraits)
					m.addElement(trait);
				break;
			case Quantifier:
				cb.setModel(new ListComboBoxModel(swat.dk.getQuantifiers().subList(0, swat.dk.getQuantifiers().size()-1)));
				break;
			case Verb:
				cb.setModel(new ListComboBoxModel(swat.dk.getVerbs()));
				break;
			}
			// restore the selection
			if (o!=null)
				cb.setSelectedItem(o);
			if (cb.getSelectedItem()==null) {
				if (cb.getItemCount()>1)
					cb.setSelectedIndex(1);
				else if (cb.getItemCount()>0)
					cb.setSelectedIndex(0);
			}
		}
		
	}

	/**
	 * A custom ComboBox model, front end for displaying a List of elements 
	 * in a combobox.  
	 * */
	private static class ListComboBoxModel extends AbstractListModel implements ComboBoxModel {
		private static final long serialVersionUID = 1L;
		private List<?> l;
		private Object selected;
		ListComboBoxModel(List<?> l){
			this.l=l;
		}
		public Object getSelectedItem() {
			return selected;
		}
		public void setSelectedItem(Object anItem) {
			if (selected==anItem || selected!=null && selected.equals(anItem) || l.contains(anItem)) {
				selected=anItem;
        	    fireContentsChanged(this, -1, -1);
			}
		}
		public Object getElementAt(int index) {
			return l.get(index);
		}
		public int getSize() {
			return l.size();
		}
	};

	/** A class for testing the sentence display editor. */
	public abstract static class Test {
		public static void toggleWordSocketVisible(SentenceDisplayEditor se,int i){
			se.visibilityCBs[i].doClick();
		}
	}
}
