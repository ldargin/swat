package com.storytron.swat.verbeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.tree.TreeNode;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.DynamicTree;
import com.storytron.swat.Scriptalyzer;
import com.storytron.swat.Swat;
import com.storytron.swat.tree.TNode;
import com.storytron.swat.util.FlowScrollLayout;
import com.storytron.swat.util.UndoableAction;
import com.storytron.swat.verbeditor.VerbEditor.ButtonShortcut;
import com.storytron.swat.verbeditor.VerbEditor.OctagonalButton;
import com.storytron.uber.Actor;
import com.storytron.uber.Deikto;
import com.storytron.uber.FloatTrait;
import com.storytron.uber.Prop;
import com.storytron.uber.Quantifier;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Stage;
import com.storytron.uber.Verb;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.CustomOperator;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;
import com.storytron.uber.operator.ParameterOperator;

/** 
 * An editor for scripts. 
 * <p>
 * The script editor holds operator menus, a panel for shortcut buttons,
 * and a header for identifying the edited script and accessing scriptalyzer. 
 * */
public abstract class ScriptEditor <ContainerState> {
	private static final long serialVersionUID = 0L;
	
	public final ArrayList<Operator> extraOperators = new ArrayList<Operator>();
	public JComponent mainMenuPanel, scriptPanel;
	private ScriptPath mScriptPath;
	private Script mScript;
	private JComponent toolBarPanel, rightScriptPanel;
	private JLabel rightScriptLabel;
	private DynamicTree theDynamicTree;
	private JMenuBar scriptMenuBar;
	private JMenu scriptMenu;
	private Color rightColor = new Color(0.90f, 0.90f, 1.00f);
	private JMenuItem exportMenuItem, scriptalyzerMenuItem;
	public Swat swat;
	private Set<CustomOperator> scriptUsers = new HashSet<CustomOperator>();
	private Scriptalyzer scriptalyzer;
	private EnumMap<OperatorDictionary.Menu,OperatorMenu> operatorMenus = new EnumMap<OperatorDictionary.Menu,OperatorMenu>(OperatorDictionary.Menu.class);

	/** Callback used to get the state of the container editor so it can be restored later. */
	protected abstract ContainerState getContainerState();
	/** Callback used to set the state of the container editor, retrieved with {@link #getState()}. */
	protected abstract void setContainerState(ContainerState state);
	
	/** Callback used to notify of a script change. */
	protected void scriptChanged(){};
	
	public ScriptEditor(final Swat swat,Set<OperatorDictionary.Menu> scriptMenus) {
		this.swat = swat;
		
		for (OperatorDictionary.Menu m:scriptMenus)
			operatorMenus.put(m,new OperatorMenu(m));

		exportMenuItem=new JMenuItem("Export");
		exportMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String output = "<html><body>"+getScriptPath().getPath(mScript)+"<br><br>"+getScript().toHtml()+"</body></html>";
				try {
					JFileChooser chooser = new JFileChooser(mScriptPath.getPath(mScript)+".htm");
					int fff = chooser.showSaveDialog(swat.getMyFrame());
					if (fff != JFileChooser.CANCEL_OPTION) {
						FileWriter myFileWriter = new FileWriter(chooser.getSelectedFile());
						BufferedWriter outText = new BufferedWriter(myFileWriter);
						try {
							outText.write(output);
							outText.flush();
						} 
						catch (java.io.IOException ex) {
							System.out.println("can't export Script");
						}
						myFileWriter.close();
					}
				}
				catch (java.io.IOException ey) {
					System.out.println("can't export Script");
				}
			}
		});
		
		scriptalyzerMenuItem=new JMenuItem("Scriptalyzer");
		scriptalyzerMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openScriptalyzer();
			}
		});

		scriptMenu=new JMenu("Script");
		scriptMenuBar=new JMenuBar();
		scriptMenuBar.add(scriptMenu);
		scriptMenuBar.setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(0,1,0,0,Color.darkGray),
						scriptMenuBar.getBorder()
						));
		scriptMenu.add(exportMenuItem);
		scriptMenu.add(scriptalyzerMenuItem);

		setupMainMenuPanel();
		setupRightScriptPanel();
	}
	
	public void init(Deikto dk){
		theDynamicTree.dk = dk;
	}
	
	/** Specifies if the scriptalyzer menu item should be present in the script menu. */
	public void setScriptalyzerMenuItemVisible(boolean visible){
		if (visible) {
			if (scriptalyzerMenuItem.getParent()!=null)
				scriptMenu.add(scriptalyzerMenuItem);
		} else
			scriptMenu.remove(scriptalyzerMenuItem);
	}
	
	private JComponent setupMainMenuPanel() {
		mainMenuPanel = new JPanel(new GridLayout(0,1));
		mainMenuPanel.setOpaque(false);
		for (OperatorMenu om:operatorMenus.values())
			mainMenuPanel.add(om.getMyButton());
		return mainMenuPanel;
	}

	private JComponent setupRightScriptPanel() {
		rightScriptPanel = new JPanel();
		rightScriptLabel = new JLabel();
		rightScriptLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
		rightScriptPanel.setLayout(new BoxLayout(rightScriptPanel, BoxLayout.Y_AXIS));		
		rightScriptPanel.setBackground(Color.white);
		Border treeBorder = BorderFactory.createLineBorder(Color.black);
		rightScriptPanel.setBorder(treeBorder);
		theDynamicTree = new DynamicTree(this,swat.dk);
		
		JPanel rightScriptBoxA = new JPanel(new BorderLayout());
		rightScriptBoxA.setBackground(rightColor);
		rightScriptBoxA.setMaximumSize(new Dimension(10000,20));
		rightScriptBoxA.add(Box.createHorizontalStrut(10));
		rightScriptBoxA.add(rightScriptLabel,BorderLayout.CENTER);
		rightScriptBoxA.add(scriptMenuBar,BorderLayout.EAST);

		JScrollPane scrollPane = new JScrollPane(theDynamicTree);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
		scrollPane.setPreferredSize(new Dimension(330,300));
		
		rightScriptPanel.add(rightScriptBoxA);		
		rightScriptPanel.add(scrollPane);
		
		scriptPanel = new JPanel(new BorderLayout());
		scriptPanel.setOpaque(false);
		scriptPanel.add(setupToolBarPanel(),BorderLayout.NORTH);
		rightScriptPanel.setAlignmentX(0.5f);
		scriptPanel.add(rightScriptPanel,BorderLayout.CENTER);
		
		return scriptPanel;
	}

	private JComponent setupToolBarPanel() {
		final JScrollPane scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		final Border scrollBorder = scroll.getBorder();
		final Border emptyBorder = BorderFactory.createEmptyBorder();
		scroll.setBorder(emptyBorder);
		// we add a listener to hide the JScrollPane border if the
		// vertical scrollbar is not visible.
		scroll.getVerticalScrollBar().setUnitIncrement(12);
		scroll.getVerticalScrollBar().addComponentListener(new ComponentListener(){
			public void componentHidden(ComponentEvent e) { scroll.setBorder(emptyBorder);	}
			public void componentShown(ComponentEvent e) { scroll.setBorder(scrollBorder); }
			public void componentMoved(ComponentEvent e) {};
			public void componentResized(ComponentEvent e) {}
		});
		FlowLayout layout = new FlowScrollLayout(scroll);
		layout.setHgap(0);
		layout.setVgap(0);
		layout.setAlignment(FlowLayout.LEFT);
		JPanel panel = new JPanel(layout);
		panel.setOpaque(false);
		
		scroll.setViewportView(panel);
		scroll.getViewport().setOpaque(false);
		scroll.setOpaque(false);
		scroll.setAlignmentX(0.5f);		
		scroll.setMinimumSize(new Dimension(80,75));
		scroll.setPreferredSize(new Dimension(80,75));
		this.toolBarPanel = panel;
		return scroll;
	}

	public void refresh(){
		theDynamicTree.repaint();

		if (getScriptPath()!=null && theDynamicTree.getSelectedNode()!=null)
			// Reloads operator menus.
			setSelection(theDynamicTree.getSelectedNode(), theDynamicTree.getRootNode());
	}
	
	public void repaintScript(){
		theDynamicTree.revalidate();
		theDynamicTree.repaint();
	}

	public ScriptPath getScriptPath() {
		return mScriptPath;
	}
	
	public Script getScript() {
		return mScript;
	}

	public void setScriptPath(ScriptPath sp,Script s){
		mScriptPath = sp;
		mScript = s;
		Script script = null;
		if (s!=null) {
			script = s;
			String zString;
			if ((script.getType() == Script.Type.Acceptable || script.getType() == Script.Type.Desirable)
					&& sp.getOption().getPointedVerb().isWordSocketActive(script.getIWordSocket()))
				zString = sp.getOption().getPointedVerb().getWordSocketFullLabel(script.getIWordSocket())+": "+script.getLabel();
			else 
				zString = script.getLabel();
			rightScriptLabel.setText(zString);

			theDynamicTree.setScript(script);
			
			if (swat.dk.usageGraph!=null){
				if (script.getType()==Script.Type.OperatorBody)
					swat.dk.usageGraph.addUsers(script.getCustomOperator(), scriptUsers);
				else
					scriptUsers.clear();
			}
		}
		clearSelection();
		
		for(Component c:rightScriptPanel.getComponents())
			c.setVisible(script!=null);
		theDynamicTree.setScript(script);
	}

	/** Tells if the node is an empty text at the root of a suffix script. */
	private boolean isEmptyTextRootAtSuffix(Node n) {
		return getScript().getType()==Script.Type.WordsocketSuffix
				&& ((Node)n.getParent()).getParent()==null
				&& n.getOperator()==OperatorDictionary.getTextConstantOperator()
				&& ((String)n.getConstant()).length()==0;
	}
	
	private void reloadToolBarPanel(Operator.Type tiDataType,Node selectedNode) {
		toolBarPanel.removeAll();
		if (tiDataType != Operator.Type.UnType) {
			// If the selected node is not an undefined operator show the delete button,
			// unless it is an empty text at the top of suffix script.
			if (null==OperatorDictionary.getUndefinedOperator(selectedNode.getOperator().getLabel())
					&& !isEmptyTextRootAtSuffix(selectedNode)) {
				JButton delbt=new ButtonShortcut("delete");
				delbt.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						deleteSelection();
					}
				});
				toolBarPanel.add(delbt);
			}
			
			OperatorDictionary od = swat.dk.getOperatorDictionary();
			ArrayList<Operator> shortCutOps = new ArrayList<Operator>();
			
			for(Operator op:extraOperators)
				shortCutOps.add(op);
			
			switch (tiDataType) {
			case Text: 
				shortCutOps.add(od.getOperator("TextConstant"));
				break;
			case Number: { 
				shortCutOps.add(od.getOperator("sum"));
				shortCutOps.add(od.getOperator("difference"));
				shortCutOps.add(od.getOperator("product"));
				shortCutOps.add(od.getOperator("quotient"));
				shortCutOps.add(od.getOperator("inverse"));
				shortCutOps.add(od.getOperator("NumberConstant"));
				break;
			}
			case Actor: {
				shortCutOps.add(od.getOperator("ReactingActor"));
				shortCutOps.add(od.getOperator("ThisSubject"));
				shortCutOps.add(od.getOperator("ThisDirObject"));
				shortCutOps.add(od.getOperator("ActorConstant"));
				shortCutOps.add(od.getOperator("CandidateActor"));
				break;
			}
			case Boolean: {
				shortCutOps.add(od.getOperator("AND"));
				shortCutOps.add(od.getOperator("OR"));
				shortCutOps.add(od.getOperator("EOR"));
				shortCutOps.add(od.getOperator("NOT"));
				shortCutOps.add(od.getOperator("AreSameActor"));
				if (getScript().getType()==Script.Type.Acceptable || 
						getScript().getType()==Script.Type.Desirable) {
					switch (getScriptPath().getOption().getPointedVerb().getWordSocketType(getScript().getIWordSocket())) {
						case Prop: {shortCutOps.add(od.getOperator("AreSameProp")); break;}
						case Stage: {shortCutOps.add(od.getOperator("AreSameStage")); break;}
						case Verb: {shortCutOps.add(od.getOperator("AreSameVerb")); break;}
						case Quantifier: {shortCutOps.add(od.getOperator("AreSameQuantifier")); break;}
					}
				}
				shortCutOps.add(od.getOperator("true"));
				shortCutOps.add(od.getOperator("false"));
				break;
			}
			case Prop: {
				shortCutOps.add(od.getOperator("PropConstant"));
				shortCutOps.add(od.getOperator("CandidateProp"));
				break;
			}
			case Stage: {
				shortCutOps.add(od.getOperator("StageConstant"));
				shortCutOps.add(od.getOperator("CandidateStage"));
					break;
				}
			case Verb: {
				if (swat.verbEditor.getSelectedVerbLabel() != null)
					shortCutOps.add(od.getOperator("VerbConstant"));
				shortCutOps.add(od.getOperator("CandidateVerb"));
				break;
			}
			case Event:
				shortCutOps.add(od.getOperator("CandidateEvent"));
				break;
			case ActorTrait: 
				shortCutOps.add(od.getOperator("ActorTraitConstant"));
				break;
			case MoodTrait: 
				shortCutOps.add(od.getOperator("MoodTraitConstant"));
				break;
			case PropTrait: 
				shortCutOps.add(od.getOperator("PropTraitConstant"));
				break;
			case StageTrait: 
				shortCutOps.add(od.getOperator("StageTraitConstant"));
				break;
			case Quantifier: {
				shortCutOps.add(od.getOperator("CandidateQuantifier"));
				shortCutOps.add(od.getOperator("QuantifierConstant"));
				break;
			}
			case BNumber: { 
				shortCutOps.add(od.getOperator("Blend"));
				shortCutOps.add(od.getOperator("Blend3"));
				shortCutOps.add(od.getOperator("Blend4"));
				//					shortCutLabels.add("BlendBothily");
				shortCutOps.add(od.getOperator("BInverse"));
				shortCutOps.add(od.getOperator("BNumberConstant"));
				shortCutOps.add(od.getOperator("BAbsval"));
				break;
			}
			}
			for (final Operator op: shortCutOps) {
				if (getScriptPath().isValid(mScript,op,selectedNode)==null){
					OctagonalButton ob=new OctagonalButton(op.getLabel());
					toolBarPanel.add(ob);

					ob.setForeground(op.getColor());
					ob.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							processTokenMenu(op, 
									((e.getModifiers() & ActionEvent.CTRL_MASK) != 0),
									((e.getModifiers() & ActionEvent.ALT_MASK) != 0),null);
						}
					});
				}
			}
		}
		toolBarPanel.getParent().validate();
		toolBarPanel.getParent().repaint();
		return;
	}

	/** Launches Scriptalyzer. */
	public Scriptalyzer openScriptalyzer() {
		Utils.setCursor(swat.getMyFrame(),Cursor.WAIT_CURSOR);
		scriptalyzer = new Scriptalyzer(getScriptPath(),mScript, swat.dk);
		Utils.setCursor(swat.getMyFrame(),Cursor.DEFAULT_CURSOR);
		return scriptalyzer;
	}

	public void processTokenMenu(final Operator zOperator, boolean fControlKey, boolean fAltKey, String textInput) {
		if (zOperator.getOperatorType()==Operator.OpType.Constant) {			
			String message;
			Object value = null;
			ImageIcon nastyChris = new ImageIcon(Utils.getImagePath("NastyChris.jpg"));
			switch (zOperator.getDataType()) {
			case Actor: {
				Object[] choices = new String[swat.dk.getActorCount()];
				int j = 0;
				for (Actor ac: swat.dk.getActors()) {
					choices[j++] = ac.getLabel();
				}
				String chosen = (String)JOptionPane.showInputDialog( 
						swat.getMyFrame(),
						"Select an actor!",
						"Actor constant",
						JOptionPane.QUESTION_MESSAGE, 
						(Icon)nastyChris,
						choices, 
				"nobody");
				if ((chosen != null) && (chosen.length() > 0))
					value = swat.dk.getActor(swat.dk.findActor(chosen));
				break;
			}
			case Prop: {
				Object[] choices = new String[swat.dk.getPropCount()];
				int j = 0;
				for (Prop pr:swat.dk.getProps()) {
					choices[j++] = pr.getLabel();
				}
				String chosen = (String)JOptionPane.showInputDialog( 
						swat.getMyFrame(),
						"Select a prop",
						"Prop constant",
						JOptionPane.QUESTION_MESSAGE, 
						(Icon)nastyChris,
						choices, 
				"nothing");
				if ((chosen != null) && (chosen.length() > 0))
					value=swat.dk.getProp(swat.dk.findProp(chosen));
				break;
			}
			case Stage: {
				Object[] choices = new String[swat.dk.getStageCount()];
				int j = 0;
				for (Stage st:swat.dk.getStages()) {
					choices[j++] = st.getLabel();
				}
				String chosen = (String)JOptionPane.showInputDialog( 
						swat.getMyFrame(),
						"Select a stage",
						"Stage constant",
						JOptionPane.QUESTION_MESSAGE, 
						(Icon)nastyChris,
						choices, 
				"nowhere");
				if ((chosen != null) && (chosen.length() > 0))
					value = swat.dk.getStage(swat.dk.findStage(chosen));
				break;
			}
			case Quantifier: {
				List<Quantifier> qs = swat.dk.getQuantifiers();
				Object[] choices = new String[qs.size()-1];
				for(int i=0;i<qs.size()-1;i++)
					choices[i] = qs.get(i).getLabel();
				String chosen = (String)JOptionPane.showInputDialog( 
						swat.getMyFrame(),
						"Select a quantifier",
						"Quantifier constant",
						JOptionPane.QUESTION_MESSAGE, 
						(Icon)nastyChris,
						choices, 
				"medium");
				if ((chosen != null) && (chosen.length() > 0))
					value = qs.get(swat.dk.findQuantifier(chosen));
				break;
			}
			case Verb: {
				if (swat.verbEditor.getSelectedVerbLabel() != null)
					value = swat.dk.getVerb(swat.dk.findVerb(swat.verbEditor.getSelectedVerbLabel()));
				break;
			}
			case Number: {
				message = "Enter the Number value:";
				boolean validEntry = true;
				do {
					// get current value, if there be one, for initialization
					String defaultEntry = "0.00";
					Node zNode = theDynamicTree.getSelectedNode();
					if (zNode.getOperator()==zOperator) {
						float constantValue = zNode.getNumericValue(swat.dk);
						defaultEntry = String.valueOf(constantValue);
					}
					String chosen = textInput!=null?textInput:(String)JOptionPane.showInputDialog( 
							swat.getMyFrame(),
							message,
							"Number",
							JOptionPane.QUESTION_MESSAGE, 
							(Icon)nastyChris,
							null, 
							defaultEntry);
					if ((chosen != null) && (chosen.length() > 0)) {
						try { 
							Float fchosen = Float.parseFloat(chosen);
							validEntry = true;
							value = fchosen; 
						}
						catch (NumberFormatException e) {
							message = "In NUMERIC format, damn you!";
						}
					}
				} while (!validEntry && textInput==null);
				break;
			}
			case BNumber: {
				message = "Enter the BNumber value:";
				boolean validEntry = false;
				do {
					// get current value, if there be one, for initialization
					String defaultEntry = "0.00";
					Node zNode = theDynamicTree.getSelectedNode();
					if (zNode.getOperator().getLabel().equals("BNumberConstant")) {
						float constantValue = zNode.getNumericValue(swat.dk);
						defaultEntry = String.valueOf(constantValue);
					}
					String chosen = textInput!=null? textInput:
							 (String)JOptionPane.showInputDialog(swat.getMyFrame(), message,
							"BNumber", JOptionPane.QUESTION_MESSAGE, (Icon)nastyChris,
							null, defaultEntry);
					if (chosen != null) {
						if (chosen.length() > 0) {
							try { 
								float x = Float.parseFloat(chosen);
								if ((x < 1.00f) & (x > -1.00f)) {
									value = x; 
									getScript().setRoot(theDynamicTree.getMyScript().getRoot());
									validEntry = true;
								} else message = "BNumbers must be > -1.00 and < +1.00!";
							}
							catch (NumberFormatException e) {
								message = "In NUMERIC format, damn you!";
							}
						}
					} else {
						// a null value was entered; assumed cancellation
						validEntry = true;
					}
				} while (!validEntry && textInput==null);
				break;
			}
			case Text: {
				message = "Enter the Text:";
				// get current value, if there be one, for initialization
				String defaultEntry = "";
				final Node zNode = theDynamicTree.getSelectedNode();
				if (zNode.getOperator().getLabel().equals("TextConstant"))
					defaultEntry = (String)zNode.getConstant();
				
				if (textInput!=null)
					value = textInput;
				else
					value = Utils.showMultilineInputDialog(swat.getMyFrame(), nastyChris, message, "Text", defaultEntry);
				break;
			}
			case MoodTrait:{
				Object[] choices = new Object[Actor.MoodTrait.values().length];
				int j = 0;
				for (Actor.MoodTrait t: Actor.MoodTraits)
					choices[j++] = t.name();
				String chosen = (String)JOptionPane.showInputDialog( 
						swat.getMyFrame(),
						"Select a mood trait!",	"Mood trait constant",
						JOptionPane.QUESTION_MESSAGE, 
						(Icon)nastyChris, choices, null);
				if (chosen != null && chosen.length() > 0)
					value = Actor.MoodTrait.valueOf(chosen);
				break;
			}
			case ActorTrait:{
				Object[] choices = new Object[swat.dk.getActorTraits().size()];
				int j = 0;
				for (FloatTrait t: swat.dk.getActorTraits())
					choices[j++] = t.getLabel();
				String chosen = (String)JOptionPane.showInputDialog( 
						swat.getMyFrame(),	"Select an actor trait!",
						"Actor trait constant",	JOptionPane.QUESTION_MESSAGE, 
						(Icon)nastyChris, choices, null);
				if (chosen != null && chosen.length() > 0)
					value = swat.dk.getOuterTrait(chosen);
				break;
			}
			case StageTrait:{
				Object[] choices = new Object[swat.dk.getStageTraits().size()];
				int j = 0;
				for (FloatTrait t: swat.dk.getStageTraits())
					choices[j++] = t.getLabel();
				String chosen = (String)JOptionPane.showInputDialog( 
						swat.getMyFrame(),
						"Select a stage trait!", "Stage trait constant",
						JOptionPane.QUESTION_MESSAGE, (Icon)nastyChris,
						choices, null);
				if (chosen != null && chosen.length() > 0)
					value = swat.dk.getStageTrait(chosen);
				break;
			}
			case PropTrait:{
				Object[] choices = new Object[swat.dk.getPropTraits().size()];
				int j = 0;
				for (FloatTrait it: swat.dk.getPropTraits())
					choices[j++] = it.getLabel();
				String chosen = (String)JOptionPane.showInputDialog( 
						swat.getMyFrame(),
						"Select a prop trait!",	"Prop trait constant",
						JOptionPane.QUESTION_MESSAGE, 
						(Icon)nastyChris, choices, null);
				if (chosen != null && chosen.length() > 0)
					value = swat.dk.getPropTrait(chosen);
				break;
			}
			} // end of switch statement
			if (value!=null)
				processTokenMenu(zOperator, value, fControlKey, fAltKey);
		} else  // this is the usual situation
			processTokenMenu(zOperator, null, fControlKey, fAltKey);
	}
	
	public void processTokenMenu(final Operator zOperator, final Object value, boolean fControlKey, boolean fAltKey) {
		// Here we will save a copy of the script tree if its modified.
		Node unmodified = null;
		String errorMsg = null;
		final Node unmodifiedNode = (Node)theDynamicTree.getSelectedNode();
		boolean changed=false;

		if (value!=null) {			
			unmodified = theDynamicTree.getMyScript().getRoot();
			setClonedRoot(theDynamicTree);
			changed=theDynamicTree.replace(zOperator, value, fControlKey, fAltKey);
			getScript().setRoot(theDynamicTree.getMyScript().getRoot());
			theDynamicTree.requestFocusInWindow();
		} else { // this is the usual situation
			if (graphNeedsUpdate(getScript(),unmodifiedNode,zOperator))
				swat.dk.usageGraph.remove(getScript().getCustomOperator());
			unmodified = theDynamicTree.getMyScript().getRoot();
			setClonedRoot(theDynamicTree);
			changed=theDynamicTree.replace(zOperator, 0.0f, fControlKey, fAltKey);			
			getScript().setRoot(theDynamicTree.getMyScript().getRoot());
			theDynamicTree.requestFocusInWindow();
			if (graphNeedsUpdate(getScript(),unmodifiedNode,zOperator))
				swat.dk.usageGraph.add(getScript().getCustomOperator());
			if (changed && scriptUsers.contains(zOperator)) {
				List<CustomOperator> cycle=swat.dk.usageGraph.getPath((CustomOperator)zOperator,getScript().getCustomOperator());
				ListIterator<CustomOperator> ops = cycle.listIterator(cycle.size());
				if (ops.hasPrevious()) {
					CustomOperator op = ops.previous();
					StringBuilder msg = new StringBuilder();
					if (ops.hasPrevious())
						msg.append("Custom operator " + getScript().getCustomOperator().getLabel() 
								+ " executes itself through this sequence of calls: "
								+ op.getLabel());
					else
						msg.append("Custom operator " + getScript().getCustomOperator().getLabel() 
								+ " executes itself through "+ op.getLabel());
					while(ops.hasPrevious()) {
						msg.append(", ");
						msg.append(ops.previous());
					}
					errorMsg = msg.toString();
				} 
			}
		}
		// now we register the undoable action.
		if (changed)
			scriptChanged(errorMsg,unmodified,unmodifiedNode,theDynamicTree.getMyScript().getRoot(),(Node)theDynamicTree.getSelectedNode(),zOperator);
	}


	/** Tells if the graph needs to be updated when inserting a given node. */
	private boolean graphNeedsUpdate(Script script,Node oldSelection,Operator newOperator){
		return script.getType()==Script.Type.OperatorBody &&
				(oldSelection.getOperator() instanceof CustomOperator ||
				 newOperator instanceof CustomOperator);
	}
	
	/** Updates the state of the editor to use the newRoot and the newSelection. */
	private void updateStateAndGraphIfNeeded(ScriptPath scriptPath,Script script,Node oldSelection,Node newRoot,Node newSelection,Operator newOperator){
		if (graphNeedsUpdate(script, oldSelection, newOperator))
			swat.dk.usageGraph.remove(script.getCustomOperator());
		script.setRoot(newRoot);
		setScriptPath(scriptPath,script);
		if (graphNeedsUpdate(script, oldSelection, newOperator))
			swat.dk.usageGraph.add(script.getCustomOperator());
		setTreeNode(newSelection);
		scriptChanged();
	}
	/** Updates the state of the editor to use the newRoot and the newSelection. */
	private void updateState(Script script,Node newRoot,Node newSelection){
		if (script.getType()==Script.Type.OperatorBody)
			swat.dk.usageGraph.remove(script.getCustomOperator());
		script.setRoot(newRoot);
		theDynamicTree.setScript(script);
		if (script.getType()==Script.Type.OperatorBody)
			swat.dk.usageGraph.add(script.getCustomOperator());
		setTreeNode(newSelection);
		scriptChanged();
	}
	
	private void scriptChanged(final String errorMsg,
								final Node oldRoot,final Node oldSelection,final Node newRoot,
								final Node newSelection,final Operator newOperator){

		if (errorMsg!=null) {
			theDynamicTree.showError(errorMsg,oldSelection);
			Swat.playSound("eeew.wav");
		} else
			Swat.playSound("editOperator.aiff");

		scriptChanged();
		final ContainerState s = getContainerState();
		final Script script = getScript();
		final ScriptPath scriptPath = getScriptPath();
		new UndoableAction(swat,false,"edit script"){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo(){
				setContainerState(s);
				updateStateAndGraphIfNeeded(scriptPath,script, oldSelection, newRoot, newSelection, newOperator);
				if (errorMsg!=null) {
					theDynamicTree.showError(errorMsg,oldSelection);
					Swat.playSound("eeew.wav");
				} else
					Swat.playSound("editOperator.aiff");
			}
			@Override
			public void myUndo(){
				setContainerState(s);
				updateStateAndGraphIfNeeded(scriptPath,script,oldSelection,oldRoot,oldSelection,newOperator);
				Swat.playSound("delete.aiff");
			}
		};
	}
	
	private void selectionDeleted(final Node oldRoot,final Node oldSelection,final Node newRoot,
			final Node newSelection){
		final ContainerState s = getContainerState();
		final Script script = getScript();
		Swat.playSound("delete.aiff");

		new UndoableAction(swat,false,"delete"){
			private static final long serialVersionUID = 1L;
			public void myRedo(){
				setContainerState(s);
				updateState(script,newRoot,newSelection);
				Swat.playSound("delete.aiff");
			}
			@Override
			public void myUndo(){
				setContainerState(s);
				updateState(script,oldRoot,oldSelection);
				Swat.playSound("editOperator.aiff");
			}
		};
	}

	private void selectionCut(final Node oldRoot,final Node oldSelection,final Node newRoot,
			final Node newSelection){
		final ContainerState s = getContainerState();
		final Script script = getScript();
		scriptChanged();
		new UndoableAction(swat,false,"cut"){
			private static final long serialVersionUID = 1L;
			public void myRedo(){
				setContainerState(s);
				updateState(script,newRoot,newSelection);
				Swat.playSound("cut.aiff");
			}
			@Override
			public void myUndo(){
				setContainerState(s);
				updateState(script,oldRoot,oldSelection);
				Swat.playSound("paste.aiff");
			}

		};
	}

	private void selectionPasted(final Node oldRoot,final Node oldSelection,final Node newRoot,
			final Node newSelection){
		final ContainerState s = getContainerState();
		final Script script = getScript();
		scriptChanged();
		new UndoableAction(swat,false,"paste"){
			private static final long serialVersionUID = 1L;
			public void myRedo(){
				setContainerState(s);
				updateState(script,newRoot,newSelection);
				Swat.playSound("paste.aiff");
			}
			@Override
			public void myUndo(){
				setContainerState(s);
				updateState(script,oldRoot,oldSelection);
				Swat.playSound("cut.aiff");
			}
		};
	}

	
	public void setSelection(Node tNode, TNode tRootNode) {
		Operator.Type ziDataType, zClipboardDataType;
		theDynamicTree.clearValidOperatorList();
		
		for (Operator op:getScriptPath().getValidOperators(extraOperators,mScript,tNode))
			theDynamicTree.addOperatorToValidList(op,null);
		
		Operator nodeOp = tNode.getOperator();
		for (OperatorMenu om: operatorMenus.values()){
			LinkedList<Operator> ops = getScriptPath().getValidOperators(swat.dk.getOperatorDictionary().getOperators(om.getMenu()),mScript,tNode);
			LinkedList<OperatorAction> l = new LinkedList<OperatorAction>();

			for (Operator op:ops){
				OperatorAction opAction = new OperatorAction(op);
				l.add(opAction);
				theDynamicTree.addOperatorToValidList(op,null);
			}
			om.setMenuActions(l);
		}
		// Add constant operators
		for(Operator op:swat.dk.getOperatorDictionary().getOperators(OperatorDictionary.Menu.NONE)) {
			if (op.getDataType()==nodeOp.getDataType() && Operator.OpType.Constant==op.getOperatorType()) {
				theDynamicTree.addOperatorToValidList(op,null);
				switch(nodeOp.getDataType()){
				case Verb:
					for(Verb v:swat.dk.getVerbs())
						theDynamicTree.addOperatorToValidList(op,v);
					break;
				case Actor:
					for(Actor a:swat.dk.getActors())
						theDynamicTree.addOperatorToValidList(op,a);
					break;
				case Quantifier:
					List<Quantifier> qs = swat.dk.getQuantifiers();
					for(int i=0;i<qs.size()-1;i++) // skip the last (interrogative) quantifier
						theDynamicTree.addOperatorToValidList(op,qs.get(i));
					break;
				case ActorTrait:
					for(FloatTrait t:swat.dk.getActorTraits())
						theDynamicTree.addOperatorToValidList(op,t);
					break;
				case Stage:
					for(Stage s:swat.dk.getStages())
						theDynamicTree.addOperatorToValidList(op,s);
					break;
				case StageTrait:
					for(FloatTrait t:swat.dk.getStageTraits())
						theDynamicTree.addOperatorToValidList(op,t);
					break;
				case Prop:
					for(Prop p:swat.dk.getProps())
						theDynamicTree.addOperatorToValidList(op,p);
					break;
				case PropTrait:
					for(FloatTrait t:swat.dk.getPropTraits())
						theDynamicTree.addOperatorToValidList(op,t);
					break;
				case MoodTrait:
					for(Actor.MoodTrait t:Actor.MoodTraits)
						theDynamicTree.addOperatorToValidList(op,t);
					break;
				}
			}
		}
		theDynamicTree.sortValidOperatorList();
		
		if (tNode == tRootNode)
			ziDataType = Operator.Type.UnType;
		else
			ziDataType = nodeOp.getDataType();
		if (ziDataType == Operator.Type.UnType)
			System.out.println("VerbEditor.setSelection finds UnType: "+nodeOp.getLabel());
		reloadToolBarPanel(ziDataType,tNode);
		swat.cutMenuItem.setEnabled(true);
		swat.copyMenuItem.setEnabled(true);
		
		// enable paste command IF the clipboard is populated with the right data type
		if (swat.clipboard != null) {
			zClipboardDataType = nodeOp.getDataType();
			swat.pasteMenuItem.setEnabled(zClipboardDataType == ziDataType);
		}
		else 
			swat.pasteMenuItem.setEnabled(false);
	}

	public void clearSelection() {
		for (OperatorMenu tm: operatorMenus.values()) 
			tm.setMenuActions(null);
		swat.cutMenuItem.setEnabled(false);
		swat.copyMenuItem.setEnabled(false);
		swat.pasteMenuItem.setEnabled(false);
		reloadToolBarPanel(Operator.Type.UnType,null);		
	}

	private void setClonedRoot(DynamicTree dt){
		Node selection = dt.getSelectedNode();
		dt.setRoot((Node)dt.getMyScript().getRoot().cloneTree());
		TNode t = convertCloned(getScript().getRoot(),selection);
		dt.superSetSelectedNode(t);
		dt.scrollNodeToVisible(t);
	}
	
	private TNode convertCloned(TNode cloned,Node n){
		TreeNode[] tp=n.getPath();
		TNode current = cloned;
		for (int i=1;i<tp.length;i++) { 
			TreeNode o = tp[i];
			int index = tp[i-1].getIndex(o);
			if (index==-1) return null;
			current = (TNode)current.getChildAt(index);
		}
		return current;
	}
	
	public void deleteSelection() {
		final Node unmodifiedNode = (Node)theDynamicTree.getSelectedNode();
		
		// if it is an undefined operator there is nothing to delete.
		if (null!=OperatorDictionary.getUndefinedOperator(unmodifiedNode.getOperator().getLabel())
				|| isEmptyTextRootAtSuffix(unmodifiedNode))
			return;
		
		// if we are deleting the root of a suffix script we want to place an empty text
		// instead of the undefined text operator.
		if (getScript().getType()==Script.Type.WordsocketSuffix &&
				((Node)unmodifiedNode.getParent()).getParent()==null) {
			processTokenMenu(OperatorDictionary.getTextConstantOperator(),false,false,"");
			return;
		}
		
		final Node unmodified = (Node)theDynamicTree.getMyScript().getRoot();
		Node spath = (Node)theDynamicTree.getSelectedNode();
		theDynamicTree.setRoot(theDynamicTree.getMyScript().getRoot().cloneTree());
		
		theDynamicTree.deleteNode((Node)convertCloned(theDynamicTree.getRootNode(),spath));
		getScript().setRoot(theDynamicTree.getMyScript().getRoot());
		final Node modifiedNode = (Node)convertCloned(getScript().getRoot(),spath);
		setTreeNode(modifiedNode);
		theDynamicTree.requestFocusInWindow();

		selectionDeleted(unmodified,unmodifiedNode,theDynamicTree.getMyScript().getRoot(),modifiedNode);
	}
	
	public void cutSelection() {
		if (getScript()==null) return;
		
		if (theDynamicTree.getSelectedNode()==null
				&& theDynamicTree.getRoot()!=null
				&& theDynamicTree.getRoot().getChildCount()>0) 
			theDynamicTree.setSelectedNode((Node)theDynamicTree.getRoot().getFirstChild());
		final Node unmodifiedNode = (Node)theDynamicTree.getSelectedNode();
		final Node unmodified = (Node)theDynamicTree.getMyScript().getRoot();
		setClonedRoot(theDynamicTree);
		copySelection();
		theDynamicTree.getSelectedNode().setDescription("");
		theDynamicTree.deleteNode(theDynamicTree.getSelectedNode());
		getScript().setRoot(theDynamicTree.getMyScript().getRoot());
		theDynamicTree.requestFocusInWindow();

		Swat.playSound("cut.aiff");
		selectionCut(unmodified,unmodifiedNode,theDynamicTree.getMyScript().getRoot(),(Node)theDynamicTree.getSelectedNode());
	}
//**********************************************************************		
	public void copySelection() {
		if (theDynamicTree.getSelectedNode()==null
				&& theDynamicTree.getRoot()!=null
				&& theDynamicTree.getRoot().getChildCount()>0) 
			theDynamicTree.setSelectedNode((TNode)theDynamicTree.getRoot().getFirstChild());
		theDynamicTree.requestFocusInWindow();
		// this if is just a way for dealing with an empty clipboard; it's meaningless.
		if (swat.clipboard == null)
			swat.clipboard = theDynamicTree.getMyScript().getRoot();
		swat.clipboard = theDynamicTree.getSelectedNode().cloneTree();
		swat.clipboardScript = getScript();
	}
//**********************************************************************		
	public void pasteNode(Node clipboard) {
		if (getScript()==null || clipboard==null) return;
		if (theDynamicTree.getSelectedNode()==null
				&& theDynamicTree.getRoot()!=null
				&& theDynamicTree.getRoot().getChildCount()>0) 
			theDynamicTree.setSelectedNode((Node)theDynamicTree.getRoot().getFirstChild());
		// Do nothing if the type of the selected node does not match the type
		// of the clipboard root node.
		Operator.Type selectedType=((Script.Node)theDynamicTree.getSelectedNode()).getOperator().getDataType();
		Operator.Type clipboardRootType=((Script.Node)clipboard.getRoot()).getOperator().getDataType();
		if (!selectedType.equals(clipboardRootType)){
			theDynamicTree.showError("The type of the copied expression ("+clipboardRootType+") does not match the type of the selected node ("+selectedType+")");
			Swat.playSound("eeew.wav");
			return;
		}
		final Node unmodifiedNode = (Node)theDynamicTree.getSelectedNode();		
		
		final Node unmodified = theDynamicTree.getMyScript().getRoot();		
		setClonedRoot(theDynamicTree);
		
		theDynamicTree.replaceWithNode(nullifyParameters(clipboard.cloneTree()));
		
		getScript().setRoot(theDynamicTree.getMyScript().getRoot());
		setTreeNode((Node)convertCloned(theDynamicTree.getMyScript().getRoot(), unmodifiedNode));
		theDynamicTree.requestFocusInWindow();

		final Node modifiedNode = (Node)theDynamicTree.getSelectedNode();
		if (modifiedNode!=null) {
			final String result=getScriptPath().sniffNode(mScript,modifiedNode);
			if (result!=null) {
				theDynamicTree.showError(result,unmodifiedNode);
				Swat.playSound("eeew.wav");
			} else		
				Swat.playSound("paste.aiff");
		}
		
		selectionPasted(unmodified,unmodifiedNode,theDynamicTree.getMyScript().getRoot(),modifiedNode);
	}
	
	private Node nullifyParameters(Node n){
		if (swat.clipboardScript==getScript())
			return n;
		
		if (n.getOperator() instanceof ParameterOperator)
			n.setOperator(OperatorDictionary.getUndefinedOperator("?"+n.getOperator().getDataType()+"?"));
		for(int i=0;i<n.getChildCount();i++)
			nullifyParameters((Node)n.getChildAt(i));
		return n;
	}
	
	public void setTreeNode(TNode n){
		if (null!=n) {
			theDynamicTree.setSelectedNode(n);
			theDynamicTree.scrollNodeToVisible(n);
		}
	}
	public Node getSelectedNode(){
		return getScript()!=null?(Node)theDynamicTree.getSelectedNode():null;
	}
	public void setSelectedNode(TNode n){
		if (getScript()!=null)
			theDynamicTree.setSelectedNode(n);
	}
	
	public boolean requestFocusInWindow() {
		return theDynamicTree.requestFocusInWindow();
	};

	public void setNodeDescription(Node editedNode,final String newValue){
		final String oldValue = editedNode.getDescription().trim();
		
		final ContainerState s=getContainerState();
		final Script.Node node = editedNode;
		node.setDescription(newValue);
		theDynamicTree.revalidate();
		
		if (!newValue.equals(oldValue)){
			new UndoableAction(swat,false,"edit script node label"){
				private static final long serialVersionUID = 1L;
				@Override
				public void myRedo() {
					setContainerState(s);
					node.setDescription(newValue);
					theDynamicTree.getCellEditor().startEditing(node);
				}
				@Override
				public void myUndo() {
					setContainerState(s);
					node.setDescription(oldValue);
					theDynamicTree.getCellEditor().startEditing(node);
				}
			};
		}
	}
	class OperatorAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private Color foregroundColor; 
		private String tooltipText = null;
		private Operator op;
		public OperatorAction(Operator op){
			super(op.getLabel());
			foregroundColor=Operator.getColor(op.getDataType());
			if (op.getMyToolTipText()!=null && op.getMyToolTipText().length()>0)
				tooltipText=Utils.breakStringHtml(op.getMyToolTipText());
			this.op=op;
		}
		
		public void actionPerformed(ActionEvent e) {
			processTokenMenu(op, 
							((e.getModifiers() & ActionEvent.CTRL_MASK) != 0),
							((e.getModifiers() & ActionEvent.ALT_MASK) != 0),null);
			String c=e.getActionCommand();
			if (c!=null && c.equals("POPUP") && theDynamicTree.getSelectedNode().getOperator().getLabel().endsWith("?"))
				theDynamicTree.showOperatorPopup();
		}

		/** Installs GUI properties on a component owning the action. */
		public void install(JComponent c){
			c.setForeground(foregroundColor);
			c.setToolTipText(tooltipText);
		}
		public Operator getOperator(){ return op; }
	};

	public abstract static class Test {
		/** Closes the auxiliary windows. */
		public static void closeAuxWindows(ScriptEditor<?> se){
			if (se.scriptalyzer!=null)
				se.scriptalyzer.dispose();
		}
	}

}
