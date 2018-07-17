package com.storytron.swat.verbeditor;
//
//VerbEditor.java
//Erasmatron4
//
//Created by Chris Crawford on 7/5/05.
//Copyright 2005 Chris Crawford. All rights reserved.
//
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.storytron.enginecommon.LimitException;
import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.Scriptalyzer;
import com.storytron.swat.Swat;
import com.storytron.swat.util.AddButton;
import com.storytron.swat.util.ComponentLabeledPanel;
import com.storytron.swat.util.DeleteButton;
import com.storytron.swat.util.DropDown;
import com.storytron.swat.util.EditorListener;
import com.storytron.swat.util.ErrorPopup;
import com.storytron.swat.util.ExpandableMenu;
import com.storytron.swat.util.PopupButton;
import com.storytron.swat.util.UndoableAction;
import com.storytron.uber.Deikto;
import com.storytron.uber.Role;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Sentence;
import com.storytron.uber.Verb;
import com.storytron.uber.Role.Option;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;
import com.storytron.uber.operator.OperatorDictionary.Menu;

/*
 * NESTING STRUCTURE FOR SWING CONTAINERS
 * 
 * VerbEditor (BorderLayout: Fits better whole screen to the space available.)  
 *   auxPanel (horizontal box)
 * 	   outerScriptsPanel (BorderLayout: Show always the left and middle panels, stretch the right one)
 *		 nonExpandablePanel (horizontal box)            
 * 		   leftScriptPanel (the one that has the tree)
 * 		   verbPanel
 * 			 consequenceScriptsPanel (vertical box)
 * 			 roleSelectionPanel (vertical box)
 * 			   assumeRoleIfPanel 
 * 			   emotionSelectionPanel (vertical box)
 * 				 emotionScriptPanel (vertical box)
 * 			 optionPanel (vertical box)
 * 			   optionWordsocketsPanel (GridLayout(0,2))
 *			     WordSockets panels
 *			   inclinationPanel
 * 		   rightScriptPanel (the one with the script tree) 
 * 		mainMenuPanel (vertical box)
 * 		  tokenMenus
 * 
 */
public final class VerbEditor {
	private static final long serialVersionUID = 1L;
	public static final String SCRIPT_DIRECTORY = "res/data";	

	private static final int VERB_PANEL_WIDTH = 285;
	private int VERTICAL_SCROLLBAR_WIDTH;
	
	private VerbTree verbTree;
	public Swat swat;	
	private Deikto dk;
	private JPanel myPanel;
	private JButton deleteRoleButton, addOptionButton, deleteOptionButton, roleLinkButton, optionLinkButton;
	private JPopupMenu roleLinkPopup = new JPopupMenu();
	private JPopupMenu optionLinkPopup = new JPopupMenu();
	private JButton addRoleButton=new AddButton("Role");
	private JButton propertiesButton, sentenceDisplayButton;
	private DropDown roleComboBox, optionComboBox;
	private JPanel optionSelectionPanel;
	ScriptEditor<State> scriptEditor;
	public JMenuItem newverbMenuItem;
	private JComponent emotionalReactionScriptsPanel, 
					optionWordsocketsPanel, 
					optionPanel, 
					inclinationPanel,
					consequenceScriptsPanel,
					topVerbButtonPanel,
					verbPanel;
	private Role.Link mRole, clipboardRole, clipboardRoleLink;
	private Role.Option mOption, clipboardOption, clipboardOptionLink;
	private Verb mVerb;
	private int miVerb, miRole, miOption;
	private ScriptDisplayButton optionDesirableButton, optionAcceptableButton, 
								assumeRoleIfButton, abortButton;
	private JComponent abortPanel;  
	private Color darkFill = new Color(218, 218, 254);
	private ArrayList<VerbListener> verbListeners = new ArrayList<VerbListener>();
	private ButtonGroup scriptButtons = new ButtonGroup();
	private JToggleButton unselectScript = new JToggleButton();
	public interface VerbListener { public void verbChanged(VerbEditor ve); }
	public void addVerbListener(VerbListener l){ verbListeners.add(l); }
	public void removeVerbListener(VerbListener l){ verbListeners.remove(l); }
	public VerbPropertiesEditor verbPropertiesEditor=null;
	public SentenceDisplayEditor sentenceDisplayEditor=null;

	private JLabel verbLabel;
	private JButton emotionbt;
	private OperatorMenu.NonOverlappedPopupMenu consequencebtpopup, emotionbtpopup;
	private EditorListener roleEditorListener;
	
	private final ActionListener scriptDisplayActionListener = new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			if (((ScriptDisplayButton)e.getSource()).getScript()!=getScriptBeingEdited())
				VerbEditor.this.setScriptPath(((ScriptDisplayButton)e.getSource()).getScriptPath(),((ScriptDisplayButton)e.getSource()).getScript());
			else 
				VerbEditor.this.setScriptPath(null,null);
		};
	};

//**********************************************************************
	private StateStack stateStack = new StateStack(this);
	private JButton backwardButton = new JButton(stateStack.backwardAction);
	private JButton forwardButton = new JButton(stateStack.forwardAction);
	
//**********************************************************************		
	public VerbEditor(Swat tSwat) {
		dk = null;
		swat = tSwat;
		
		myPanel = new JPanel(new BorderLayout());
		myPanel.setOpaque(true); // content panes must be opaque
		myPanel.setBackground(Utils.lightBackground);
		tSwat.verbEditor = this;

		myPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		scriptButtons.add(unselectScript);
		EnumSet<Menu> verbScriptMenus = EnumSet.copyOf(OperatorDictionary.ScriptMenus);
		verbScriptMenus.remove(OperatorDictionary.Menu.Text);
		scriptEditor = new ScriptEditor<State>(swat,verbScriptMenus){
			private static final long serialVersionUID = 0L;

			@Override
			protected State getContainerState() { return new State();	}

			@Override
			protected void setContainerState(State state) {
				setState(state);
			}
			
		};
		myPanel.add(setupOuterScriptsPanel(),BorderLayout.CENTER);

		forwardButton.setToolTipText(Utils.toHtmlTooltipFormat("Show the next page."));
		backwardButton.setToolTipText(Utils.toHtmlTooltipFormat("Show the previous page."));
		assumeRoleIfButton.setToolTipText(Utils.toHtmlTooltipFormat("Decides which Actors execute this Role."));
		propertiesButton.setToolTipText(Utils.toHtmlTooltipFormat("Details of how this Verb operates"));
		abortButton.setToolTipText(Utils.toHtmlTooltipFormat("Conditions under which execution of this Verb will be aborted."));
		emotionbt.setToolTipText(Utils.toHtmlTooltipFormat("Scripts to be executed for each Actor who executes this Role."));
		
		OperatorMenu.init(this);
	} 
//***********************************************************************  
	public void init(Deikto dk){
		stateStack.setIgnoringPushState(true);
		this.dk=dk;
		verbTree.init(dk);
		Option temp = dk.getStartingOption();
		Role.Link r = dk.getStartingRole();
        setVerb(dk.getStartingVerb());
        if (dk.getStartingRole()!=null && r!=null) {
       		dk.setStartingRole(r);
       		setRole(r); // this line leads to the overwrite of dk.startingOption
        	if (mRole != null && temp!=null) {
        		dk.setStartingOption(temp); // ergo we restore it here. KLUGE!!!
        		setOption(temp);
        	}
        }
        verbTree.requestFocusInWindow();
        stateStack.initStates();
        stateStack.setIgnoringPushState(false);
        
        for (OperatorDictionary.Menu m:OperatorDictionary.ConsequenceMenus){
			JMenu menu=new ExpandableMenu(m.name());
			for(Operator op:dk.getOperatorDictionary().getOperators(m)){
				JMenuItem mi=new JMenuItem(op.getLabel());
				mi.addActionListener(new ConsequenceActionListener(op));
				menu.add(mi);
			}
			consequencebtpopup.add(menu);
		}
        // The following two lines are here because of a mysterious
        // concurrency problem during initialization.
        verbPropertiesEditor = new VerbPropertiesEditor(this);
		verbPropertiesEditor.refresh();
		verbPropertiesEditor.pack();
		
		sentenceDisplayEditor = new SentenceDisplayEditor(swat);
		sentenceDisplayEditor.pack();
		sentenceDisplayEditor.setLocationRelativeTo(swat.getMyFrame());
		
		scriptEditor.init(dk);
	}
//**********************************************************************		
	private JPanel setupOuterScriptsPanel() {

		JComponent nonExpandablePanel=Box.createHorizontalBox();
		nonExpandablePanel.add(setupLeftScriptPanel());
		nonExpandablePanel.add(setupMiddleScriptPanel());

		JPanel outerScriptsPanel = new JPanel(new BorderLayout());
		outerScriptsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		outerScriptsPanel.setOpaque(false);
		outerScriptsPanel.add(nonExpandablePanel,BorderLayout.WEST);
		JComponent scriptPanel=new JPanel(new BorderLayout());
		scriptPanel.setOpaque(false);
		scriptPanel.add(scriptEditor.mainMenuPanel,BorderLayout.EAST);
		scriptPanel.add(scriptEditor.scriptPanel,BorderLayout.CENTER);
		outerScriptsPanel.add(scriptPanel,BorderLayout.CENTER);
		return outerScriptsPanel;
	}
//**********************************************************************		
	private JComponent setupLeftScriptPanel() {
		
		verbTree = new VerbTree(this, swat);
		Dimension d=new Dimension(225,200);
		verbTree.setPreferredSize(d);
		verbTree.setOpaque(false);
		
		JMenuBar mb = new JMenuBar();
		JMenu menu=new JMenu("Verbs");
		menu.setMnemonic('V');
		mb.add(menu);
		for(Action a:verbTree.actions)
			menu.add(a);
		newverbMenuItem = menu.getItem(2);
		mb.setMinimumSize(menu.getPreferredSize());
		mb.setMaximumSize(mb.getPreferredSize());
		mb.setBorder(BorderFactory.createMatteBorder(1,1,0,1,Color.black));
		
		Box verbButtonsPanel = Box.createHorizontalBox();
		backwardButton.setMaximumSize(new Dimension(30,30));
		backwardButton.setPreferredSize(new Dimension(30,30));		
		verbButtonsPanel.add(backwardButton);				
		forwardButton.setMaximumSize(new Dimension(30,30));
		forwardButton.setPreferredSize(new Dimension(30,30));		
		verbButtonsPanel.add(forwardButton);

		
		JComponent verbTreeTop = Box.createHorizontalBox();
		mb.setAlignmentY(0.9f);
		verbTreeTop.add(mb);
		verbTreeTop.add(Box.createRigidArea(new Dimension(5,5)));
		verbButtonsPanel.setAlignmentY(1.0f);
		verbTreeTop.add(verbButtonsPanel);
		
		JComponent aux=Box.createVerticalBox();
		verbTreeTop.setAlignmentX(0.0f);
		aux.add(verbTreeTop);
		verbTree.setAlignmentX(0.0f);
		aux.add(verbTree);

		return aux;
	}	
//**********************************************************************
	private JComponent setupMiddleScriptPanel() {
		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		VERTICAL_SCROLLBAR_WIDTH = scrollPane.getVerticalScrollBar().getPreferredSize().width;
		
		JPanel verbScrolledPanel = new JPanel();
		verbScrolledPanel.setLayout(new BoxLayout(verbScrolledPanel, BoxLayout.Y_AXIS));
		verbScrolledPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		verbScrolledPanel.setOpaque(false);
		verbScrolledPanel.add(setupConsequenceScriptsPanel());
		verbScrolledPanel.add(setupRoleSelectionPanel());		
		verbScrolledPanel.setBorder(BorderFactory.createEmptyBorder());

		// The purpose of compressPanel is to compress vertically
		// the widgets in junkPanel. Another solution would be
		// to set the maximum sizes of the widgets in junkPanel to its
		// preferred sizes.
		JPanel compressPanel = new JPanel(new BorderLayout());
		compressPanel.setBackground(darkFill);
		compressPanel.add(verbScrolledPanel,BorderLayout.NORTH);
		Dimension d=new Dimension(VERB_PANEL_WIDTH,550);
		scrollPane.setViewportView(compressPanel);
		scrollPane.setPreferredSize(d);
		scrollPane.setBorder(BorderFactory.createLineBorder(Swat.shadow));
		
		verbLabel = new JLabel(" ");
		verbLabel.setFont(new Font(verbLabel.getFont().getName(),verbLabel.getFont().getStyle(),14));

		propertiesButton=new JButton("Properties");
		propertiesButton.setMargin(new Insets(0,0,0,0));
		
		verbLabel.setAlignmentY(0.5f);

		propertiesButton.setAlignmentY(0.5f);
		
		propertiesButton.addActionListener(new ActionListener(){
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				verbPropertiesEditor.setVisible(true);
				SwingUtilities.invokeLater(new Runnable(){
					public void run() {
						verbPropertiesEditor.refresh();
					}
				});
			}
		});

		sentenceDisplayButton = new JButton("Sentence Display");
		sentenceDisplayButton.setMargin(new Insets(0,0,0,0));
		sentenceDisplayButton.addActionListener(new ActionListener(){
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				showSentenceDisplayEditor(null,null);
			}
		});
		
		topVerbButtonPanel = Box.createHorizontalBox();
		topVerbButtonPanel.setBorder(BorderFactory.createEmptyBorder(0,5,5,0));
		topVerbButtonPanel.add(propertiesButton);
		topVerbButtonPanel.add(Box.createRigidArea(new Dimension(5,10)));
		topVerbButtonPanel.add(sentenceDisplayButton);
		topVerbButtonPanel.add(Box.createHorizontalGlue());
		
		JComponent innerVerbPanel = Box.createVerticalBox();
		innerVerbPanel.add(topVerbButtonPanel);
		innerVerbPanel.add(scrollPane);
		verbPanel=new ComponentLabeledPanel(verbLabel,Swat.darkShadow,true);
		verbPanel.setBorder(BorderFactory.createEmptyBorder(0, 2,0,2));
		verbPanel.setOpaque(false);
		verbPanel.add(innerVerbPanel);
		 
		return verbPanel;
	}
//	**********************************************************************
	private JComponent setupConsequenceScriptsPanel() {
		consequenceScriptsPanel = Box.createVerticalBox();
		consequenceScriptsPanel.setAlignmentX(0.0f);
		consequenceScriptsPanel.setBorder(BorderFactory.createCompoundBorder(
				  BorderFactory.createLoweredBevelBorder(),
				  BorderFactory.createEmptyBorder(4,4,4,4)
				));
		consequenceScriptsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
		consequenceScriptsPanel.setMinimumSize(new Dimension(0,2));

		abortButton = new ScriptDisplayButton("AbortIf",scriptButtons);
		abortButton.addActionListener(scriptDisplayActionListener);

		JButton deleteAbortButton = new DeleteButton();
		deleteAbortButton.setToolTipText("delete the abort script");
		deleteAbortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setScriptPath(null,null);
				final Verb v = getVerb();
				final Script oldScript = v.getAbortScript();
				new UndoableAction(swat,"delete the abort script of "+v.getLabel()){
					private static final long serialVersionUID = 1L;
					@Override
					public void myRedo(){
						setVerb(v);
						try {
							dk.setAbortScript(v,null);
						} catch (LimitException e) {
							throw new RuntimeException(e);
						}
						verbPropertiesEditor.useAbortScriptCheckBox.setSelected(false);
						updateAbortButton();
					}
					@Override
					public void myUndo(){
						setVerb(v);
						try {
							dk.setAbortScript(v,oldScript);
						} catch (LimitException e) {
							throw new RuntimeException(e);
						}
						verbPropertiesEditor.useAbortScriptCheckBox.setSelected(true);
						updateAbortButton();
					}
				};
			}
		});
		Dimension db=deleteAbortButton.getPreferredSize();
		db.height=abortButton.getPreferredSize().height;
		deleteAbortButton.setPreferredSize(db);

		abortPanel = Box.createHorizontalBox();
		abortPanel.add(abortButton);
		abortPanel.add(deleteAbortButton);
		
		JButton bt = new PopupButton("Consequences");
		bt.setToolTipText(Utils.toHtmlTooltipFormat("Scripts to be executed immediately upon execution of this Verb."));
		consequencebtpopup = new OperatorMenu.NonOverlappedPopupMenu(bt);
		
		bt.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				consequencebtpopup.showPopup();
			}
		});
		Insets insets=bt.getInsets();
		insets.bottom=insets.top=0;
		bt.setMargin(insets);
		
		JComponent outerPanel = new ComponentLabeledPanel(bt,Swat.shadow,true);
		outerPanel.setBackground(Utils.lightBackground);
		outerPanel.add(consequenceScriptsPanel);
		outerPanel.setAlignmentX(0.5f);
		outerPanel.setBorder(BorderFactory.createEmptyBorder(4,2,4,2));
		
		return outerPanel;
	}
//**********************************************************************
	private ErrorPopup errorPopup=new ErrorPopup();
	private JComponent setupRoleSelectionPanel() {
		
		roleComboBox = new DropDown(Deikto.MAXIMUM_FIELD_LENGTH){
			private static final long serialVersionUID = 1L;
			@Override
			public void indexMoved(final int from, final int to) {
				if (getVerb()!=null) {
					final Verb verb = mVerb;
					try {
						dk.addRole(verb,to,dk.deleteRole(verb,from));
					} catch(LimitException e){
						e.printStackTrace();
					}
					new UndoableAction(swat,false,"reorder roles"){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							try {
								dk.addRole(verb,to,dk.deleteRole(verb,from));
							} catch(LimitException e){
								e.printStackTrace();
							}
							if (mVerb!=verb)
								setVerb(verb);
							else
								roleComboBox.moveIndex(from,to);
							roleComboBox.showPopup();
						}
						@Override
						public void myUndo() {
							try {
								dk.addRole(verb,from,dk.deleteRole(verb,to));
							} catch(LimitException e){
								e.printStackTrace();
							}
							if (mVerb!=verb)
								setVerb(verb);
							else
								roleComboBox.moveIndex(to,from);
							roleComboBox.showPopup();
						}
					};
				}
			}
		};
		roleComboBox.setBackground(Color.white);
		roleComboBox.setEditable(true);
		roleComboBox.setAllowReordering(true);
		
		roleEditorListener=new Swat.DropDownListener(roleComboBox) {
			private static final long serialVersionUID = 1L;
			public boolean timedActionPerformed(ActionEvent e){ 
				if (roleComboBox.isListPicking()) {
					setScriptPath(null,null);
					setRole(mVerb.getRole(roleComboBox.getSelectedIndex()));									
				} else { // if the user is not list picking, assume the storybuilder
					// is trying to rename the current role.
					String zLabel = ((String)roleComboBox.getTextComponent().getJTextComponent().getText()).trim();
					if (zLabel==null || getRole()==null || zLabel.equals(getRole().getLabel()))
						return true;
						
					if (getVerb().getRoleIndex(zLabel)!=-1){
						errorPopup.showError(swat.getMyFrame(),roleComboBox.getLocationOnScreen(),
								"A role with name "+zLabel+" does already exist.");
						return false;
					}

					//Swat.playSound("add.aiff");
					final Role.Link role = mRole;
					final State s=new State();
					final String newValue = zLabel;
					final String oldValue = role.getLabel();
					role.setLabel(newValue);
					new UndoableAction(swat,false,""){
						private static final long serialVersionUID = 1L;
						@Override
						public void myRedo() {
							role.setLabel(newValue);
							setState(s);
							roleComboBox.setSelectedItem(role);
						}
						@Override
						public void myUndo() {
							role.setLabel(oldValue);
							setState(s);
							roleComboBox.setSelectedItem(role);
						}
						@Override
						public String getRedoPresentationName() {
							return "rename role "+oldValue;
						}
						@Override
						public String getUndoPresentationName() {
							return "rename role "+newValue;
						}
					};						
				}
				return true;
			}
			@Override
			public String getText(){ return getRole()!=null?getRole().getLabel():""; };
		};
		roleComboBox.addActionListener(roleEditorListener);

		ImageIcon linkImage = new ImageIcon(Utils.getImagePath("linkbt.png"));
		Insets linkInsets = new Insets(2,1,2,7);
		roleLinkButton = new PopupButton(null);
		roleLinkButton.setIcon(linkImage);
		roleLinkButton.setMargin(linkInsets);
		roleLinkButton.setEnabled(false);
		roleLinkButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				roleLinkPopup.show(roleLinkButton,0,roleLinkButton.getHeight());
			}
		});

		optionLinkButton = new PopupButton(null);
		optionLinkButton.setIcon(linkImage);
		optionLinkButton.setMargin(linkInsets);
		optionLinkButton.setEnabled(false);
		optionLinkButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				optionLinkPopup.show(optionLinkButton,0,optionLinkButton.getHeight());
			}
		});

		deleteRoleButton = new DeleteButton();
		deleteRoleButton.setToolTipText("delete this role");
		deleteRoleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteRole(getRole());
				if (getVerb().getRoleCount()==0){
					roleComboBox.getTextComponent().getJTextComponent().requestFocusInWindow();
				}
			}
		});
		deleteRoleButton.setBackground(Color.red);
		deleteRoleButton.setEnabled(false);
		
		JComponent assumeRoleIfScriptBox = Box.createHorizontalBox();
		assumeRoleIfScriptBox.setOpaque(false);
		assumeRoleIfScriptBox.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
		assumeRoleIfScriptBox.add(assumeRoleIfButton=new ScriptDisplayButton("AssumeRoleIf",scriptButtons));
		assumeRoleIfButton.addActionListener(scriptDisplayActionListener);

		addRoleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i=0;
				while(getVerb().getRoleIndex(i==0?"new role":"new role"+i)!=-1) i++;
				addRole(i==0?"new role":"new role"+i);
				roleComboBox.getJTextComponent().requestFocusInWindow();
				roleComboBox.getJTextComponent().selectAll();
			}
		});

		Dimension db=addRoleButton.getPreferredSize();
		db.height=roleComboBox.getPreferredSize().height;
		addRoleButton.setPreferredSize(db);
		deleteRoleButton.setPreferredSize(db);
		roleComboBox.setMaximumSize(new Dimension(VERB_PANEL_WIDTH-18-2*db.width-roleLinkButton.getPreferredSize().width-VERTICAL_SCROLLBAR_WIDTH,Integer.MAX_VALUE));
		
		Box roleSelectionPanel = Box.createHorizontalBox();
		roleSelectionPanel.setOpaque(false);
		roleSelectionPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
		roleSelectionPanel.add(roleLinkButton);
		roleSelectionPanel.add(roleComboBox);
		roleSelectionPanel.add(addRoleButton);
		roleSelectionPanel.add(deleteRoleButton);

		JComponent outerRolePanel = new JPanel();
		outerRolePanel.setLayout(new BoxLayout(outerRolePanel,BoxLayout.Y_AXIS));
		outerRolePanel.setBackground(darkFill);
		outerRolePanel.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.darkGray),"Role"));

		roleSelectionPanel.setAlignmentX(0.0f);
		outerRolePanel.add(roleSelectionPanel);
		assumeRoleIfScriptBox.setAlignmentX(0.0f);
		outerRolePanel.add(assumeRoleIfScriptBox);
		JComponent emsPanel = setupEmotionSelectionPanel();
		emsPanel.setBorder(BorderFactory.createEmptyBorder(0,2, 0, 2));
		emsPanel.setAlignmentX(0.0f);
		outerRolePanel.add(emsPanel);
		outerRolePanel.add(setupOptionSelectionPanel());
		outerRolePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
		outerRolePanel.setAlignmentX(0.5f);
		
		return outerRolePanel;
	}
//**********************************************************************		
	private JComponent setupEmotionSelectionPanel() {		
		
		emotionbt=new PopupButton("EmotionalReaction");
		emotionbtpopup = new OperatorMenu.NonOverlappedPopupMenu(emotionbt);
		emotionbt.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				emotionbtpopup.showPopup();
			}
		});
		
		emotionalReactionScriptsPanel = Box.createVerticalBox();
		emotionalReactionScriptsPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(4,0,0,0),
				BorderFactory.createCompoundBorder(
				  BorderFactory.createLoweredBevelBorder(),
				  BorderFactory.createEmptyBorder(4,4,4,4)
				)));
		emotionalReactionScriptsPanel.setOpaque(false);
		emotionalReactionScriptsPanel.setMinimumSize(new Dimension(0,2));
		emotionalReactionScriptsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
				
		Insets insets=emotionbt.getInsets();
		insets.bottom=insets.top=0;
		emotionbt.setMargin(insets);
		
		JComponent emotionalReactionsPanel = new ComponentLabeledPanel(emotionbt,Color.darkGray,true);
		emotionalReactionsPanel.setOpaque(false);
		emotionalReactionsPanel.add(emotionalReactionScriptsPanel);
		emotionalReactionsPanel.setAlignmentX(0.0f);

		return emotionalReactionsPanel;
	}
//**********************************************************************		
	private JComponent setupOptionSelectionPanel() {		

		optionSelectionPanel = new JPanel();
		optionSelectionPanel.setLayout(new BoxLayout(optionSelectionPanel, BoxLayout.X_AXIS));
		optionSelectionPanel.setOpaque(false);
		optionSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		optionSelectionPanel.add(optionLinkButton);

		addOptionButton = new AddButton();
		addOptionButton.setEnabled(false);
		addOptionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String zLabel = verbTree.getSelectedVerbLabel();
				if (zLabel != null) {
					addOption(zLabel);
					addOptionButton.setEnabled(false);
				}
			}
		});

		optionComboBox = new DropDown(Deikto.MAXIMUM_FIELD_LENGTH){
			private static final long serialVersionUID = 1L;
			@Override
			public void indexMoved(int from, int to) {
				if (getRole()!=null)
					moveOption(from,to);
			}
		};
		optionComboBox.setBackground(Color.white);
		optionComboBox.setAllowReordering(true);
		optionComboBox.setBorder(BorderFactory.createEmptyBorder());
		{
			Dimension d = roleComboBox.getPreferredSize(); 
			Dimension db=addOptionButton.getPreferredSize();
			optionComboBox.setMaximumSize(new Dimension(VERB_PANEL_WIDTH-26-2*db.width-optionLinkButton.getPreferredSize().width-VERTICAL_SCROLLBAR_WIDTH,d.height));
			optionComboBox.setPreferredSize(new Dimension(125,d.height));
		}
		optionComboBox.setAction(new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				if (optionComboBox.getSelectedIndex()!=-1) {
					setScriptPath(null,null);
					setOption((Role.Option)mRole.getRole().getOptions().get(optionComboBox.getSelectedIndex()));				
				}
			}
		});
		optionComboBox.getJTextComponent().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount()==2 && optionComboBox.getSelectedIndex()!=-1)
					setVerb(((Role.Option)optionComboBox.getSelectedItem()).getPointedVerb());
			}
		});
		optionSelectionPanel.add(optionComboBox);
		
		verbTree.addTreeSelectionListener(new TreeSelectionListener(){
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getPath()!=null && ((VerbTreeNode)e.getPath().getLastPathComponent()).isVerb())
					checkAddOptionButton();
				else
					addOptionButton.setEnabled(false);
			}
		});
		optionSelectionPanel.add(addOptionButton);
		Dimension db=addOptionButton.getPreferredSize();
		db.height=optionComboBox.getPreferredSize().height;
		addOptionButton.setPreferredSize(db);

		deleteOptionButton = new DeleteButton();
		deleteOptionButton.setEnabled(false);
		deleteOptionButton.setToolTipText("delete this option");
		deleteOptionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteOption(getOption());				
			}
		});
		optionSelectionPanel.add(deleteOptionButton);
		deleteOptionButton.setPreferredSize(db);

		optionPanel = Box.createVerticalBox();
		optionPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.darkGray),"Option"),
				BorderFactory.createEmptyBorder(1,2,2,0)
				));
		optionPanel.setOpaque(false);
		optionPanel.add(optionSelectionPanel);
		optionWordsocketsPanel = new JPanel(new GridLayout(0,3));
		optionWordsocketsPanel.setOpaque(false);
		optionWordsocketsPanel.setBorder(BorderFactory.createCompoundBorder(				
				BorderFactory.createEmptyBorder(2,0,2,0),
				BorderFactory.createCompoundBorder(
				   BorderFactory.createLoweredBevelBorder(),
				   BorderFactory.createEmptyBorder(0,2,2,2)
				)));
		inclinationPanel = Box.createHorizontalBox();
		inclinationPanel.setOpaque(false);
		
		JPanel osPanel = new JPanel(new BorderLayout());
		osPanel.setOpaque(false);
		osPanel.add(optionWordsocketsPanel,BorderLayout.NORTH);		
		osPanel.add(inclinationPanel,BorderLayout.CENTER);
		osPanel.setAlignmentX(0.0f);

		optionPanel.add(osPanel);
		optionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));

		JComponent optionAuxPanel = Box.createHorizontalBox();
		optionAuxPanel.add(optionPanel);
		optionAuxPanel.add(Box.createHorizontalGlue());
		optionAuxPanel.setAlignmentX(0.0f);
		optionAuxPanel.setMaximumSize(new Dimension(VERB_PANEL_WIDTH-10-VERTICAL_SCROLLBAR_WIDTH,Integer.MAX_VALUE));
		
		return optionAuxPanel;
	}
	
	public void showSentenceDisplayEditor(ScriptPath sp,Script s){
		if (!sentenceDisplayEditor.isVisible()) {
			sentenceDisplayEditor.setVisible(true);
			sentenceDisplayEditor.refresh();
		} 
		sentenceDisplayEditor.setScriptPath(sp,s);
	}
	
//**********************************************************************		
	public void reloadVerbInfo() {
		loadVerbInfo(miVerb);
	}
//**********************************************************************		
	public void loadVerbInfo(int verbIndex) {
		mVerb = dk.getVerb(verbIndex);
		miVerb = verbIndex;
		dk.setStartingVerb(mVerb);
		dk.setStartingRole(null);
		dk.setStartingOption(null);

		if (verbPropertiesEditor!=null)
			verbPropertiesEditor.refresh();
		
		if (sentenceDisplayEditor!=null && sentenceDisplayEditor.isVisible())
			sentenceDisplayEditor.refresh();
		
		updateAbortButton();
		reloadVerbName();

		reloadConsequences();
		miRole = mVerb.getRoleCount() > 0 ? 0 : -1;
		scriptEditor.clearSelection();
		mLoadRoleInfo();
		swat.enableLizards();		
		checkAddRoleButton();
		updateSwatMenu();
	}
	
	void updateAbortButton(){
		abortButton.setSelected(false);
		setScriptPath(null,null);
		topVerbButtonPanel.remove(abortPanel);
		if (getVerb().getAbortScript()!=null) {
			abortButton.setScriptPath(new ScriptPath(getVerb(),null,null),getVerb().getAbortScript());
			abortButton.reformat();
			topVerbButtonPanel.add(abortPanel);
		}
		topVerbButtonPanel.revalidate();
	}
	
	private LinkedList<ScriptDisplayButton> consequenceButtons=new LinkedList<ScriptDisplayButton>();
	private JPopupMenu fillBoxButtonPopup = new JPopupMenu();
	private void reloadConsequences(){
		if (getVerb()==null) return;
		
		consequenceScriptsPanel.removeAll();
		consequenceButtons.clear();
		for (int i = 0; (i < mVerb.getConsequenceCount()); ++i) {
			final ScriptDisplayButton zButton = new ScriptDisplayButton(new ScriptPath(mVerb,null,null),mVerb.getConsequence(i),scriptButtons);
			zButton.addActionListener(scriptDisplayActionListener);
			if (mVerb.getConsequence(i).getLabel().startsWith("Fill")
				&& mVerb.getConsequence(i).getLabel().endsWith("Box")) {

				final Operator op = dk.getOperatorDictionary().getOperator(mVerb.getConsequence(i).getLabel().substring(4));
				zButton.addMouseListener(new MouseAdapter(){
					@Override
					public void mousePressed(MouseEvent e) {
						if (e.isPopupTrigger()) triggerPopup(e);
					};
					public void mouseReleased(MouseEvent e) {
						if (e.isPopupTrigger()) triggerPopup(e);
					};
					public void triggerPopup(MouseEvent e) {
						fillBoxButtonPopup.removeAll();
						dk.traverseScripts(getVerb(),new Script.NodeTraverser(){
							public boolean traversing(final Script s, Node n) {
								if (n.getOperator()==op) {
									final ScriptPath sp = new ScriptPath(verb,role,option);
									JMenuItem mi = new JMenuItem(Utils.concatStrings(sp.getPathComponents(s),": ",1));
									mi.addActionListener(new ActionListener(){
										public void actionPerformed(ActionEvent e) {
											VerbEditor.this.setScriptPath(sp,s);
										}
									});
									fillBoxButtonPopup.add(mi);
									return false;
								}
								return true;
							}
						});
						if (fillBoxButtonPopup.getComponentCount()==0){
							JMenuItem mi = new JMenuItem("The value of this box is never used.");
							mi.setEnabled(false);
							fillBoxButtonPopup.add(mi);
						}
						fillBoxButtonPopup.show(zButton,e.getX(),e.getY());
					}
				});
			}
			consequenceButtons.add(zButton);
		
			JButton deleteButton = new DeleteButton();
			deleteButton.setToolTipText("delete this consequence");
			final int index = i;
			deleteButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteConsequence(index);
				}
			});
			Dimension db=deleteButton.getPreferredSize();
			db.height=zButton.getPreferredSize().height;
			deleteButton.setPreferredSize(db);
			
			JComponent zPanel = Box.createHorizontalBox();
			zPanel.setOpaque(false);
			zPanel.add(deleteButton);			
			zPanel.add(zButton);
			zPanel.setAlignmentX(0.0f);
			zPanel.setMaximumSize(zPanel.getPreferredSize());
			consequenceScriptsPanel.add(zPanel);
		}
		verbPanel.validate();
	}
//**********************************************************************		
	public void loadRoleInfo() {
		// remove existing stuff
		scriptEditor.clearSelection();
		mLoadRoleInfo();
	}
	private LinkedList<ScriptDisplayButton> emotionButtons=new LinkedList<ScriptDisplayButton>();
	private void mLoadRoleInfo() {
		mSetOption(null);
		assumeRoleIfButton.setEnabled(false);
		
		rebuildRoleComboBox();
		if (mVerb.getRoleCount() > 0 && miRole!=-1) {			
			deleteRoleButton.setEnabled(true);
			roleComboBox.setEditable(true);
			
			swat.copyRole.setEnabled(true);
			mRole = mVerb.getRole(miRole);
			dk.setStartingRole(mRole);
			dk.setStartingOption(null);			
			assumeRoleIfButton.setScriptPath(new ScriptPath(mVerb,mRole,null),mRole.getRole().getAssumeRoleIfScript());
			assumeRoleIfButton.reformat();
			assumeRoleIfButton.setEnabled(true);
			
			emotionbt.setEnabled(true);
			checkAddOptionButton();
			// set the selected Option to the first on the list
			miOption = 0;
			rebuildOptionComboBox();
			if (mRole.getRole().getOptions().size() > 0)
				mSetOption((Role.Option)(mRole.getRole().getOptions().get(0)));
			else {
				mSetOption(null);
				stateStack.pushState();
			}
		}
		else {
			roleLinkButton.setEnabled(false);
			emotionbt.setEnabled(false);
			deleteRoleButton.setEnabled(false);
			if (((JComponent)roleComboBox.getTextComponent()).hasFocus())
				propertiesButton.requestFocusInWindow();
			roleComboBox.setEditable(false);
			addOptionButton.setEnabled(false);
			rebuildOptionComboBox();
			mSetOption(null);
		}		

		updateRoleLinkButton();
		
		reloadEmotions();
		updateEmotionPopupMenu();
		
		updateSwatMenu();
	}

	/** @return true iff the role is linked from other verbs. */
	private static boolean isRoleLinked(Deikto dk,Role r) {
		ArrayList<Verb> verbs = dk.roleVerbs.get(r);
		return verbs.size()>1 || 1<verbs.get(0).countRoleOccurrences(r);
	}	
	
	public void updateRoleLinkButton(){
		if (getRole()!=null)
			roleLinkButton.setEnabled(isRoleLinked(dk,getRole().getRole()));
		else
			roleLinkButton.setEnabled(false);
		
		roleLinkPopup.removeAll();
		if (!roleLinkButton.isEnabled()) {
			roleLinkButton.setToolTipText("This role is not linked from other verbs.");
		} else {
			roleLinkButton.setToolTipText("This role is linked from other verbs.");

			{
				JMenuItem mi = new JMenuItem("unlink");
				mi.setToolTipText("Replaces this link with a new copy of the role.");
				mi.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						unlinkRole(getVerb(),getRole());
					}
				});
				roleLinkPopup.add(mi);
				roleLinkPopup.addSeparator();
			}

			for(final Verb v:dk.roleVerbs.get(getRole().getRole())) {
				for(final Role.Link r:v.getRoles()) {
					if (r.getRole()==getRole().getRole() && r!=getRole()) {
						JMenuItem mi = new JMenuItem(v.getLabel()+": "+r.getLabel());
						mi.setToolTipText("Click to display this link in verb editor.");
						mi.addActionListener(new ActionListener(){
							public void actionPerformed(ActionEvent e) {
								set(v,r);
							}
						});
						roleLinkPopup.add(mi);
					}	
				}
			}
		}
	}

	public void updateOptionLinkButton(){
		optionLinkButton.setEnabled(false);
		optionLinkPopup.removeAll();
		boolean showLinks = false;
		if (getOption()==null || dk.optionRoles.get(getOption()).size()<=1) {
			if (getOption()!=null && getRole()!=null && isRoleLinked(dk,getRole().getRole())) {
				optionLinkButton.setToolTipText(Utils.toHtmlTooltipFormat("This option is shared with other verbs because the parent role is linked from other verbs."));
				optionLinkButton.setIcon(new ImageIcon(Utils.getImagePath("arrowlinkbt.png")));
				optionLinkButton.setEnabled(true);
				showLinks=true;			
			} else {
				if (getOption()!=null)
					optionLinkButton.setToolTipText("This option is not linked from other roles.");
				else
					optionLinkButton.setToolTipText("There are no options.");
				optionLinkButton.setIcon(new ImageIcon(Utils.getImagePath("linkbt.png")));
			}
		} else {
			optionLinkButton.setEnabled(true);
			optionLinkButton.setToolTipText("This option is linked from other roles.");
			optionLinkButton.setIcon(new ImageIcon(Utils.getImagePath("linkbt.png")));
			{
				JMenuItem mi = new JMenuItem("unlink");
				mi.setToolTipText("Replaces this link with a new copy of the option.");
				mi.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						unlinkOption(getVerb(),getRole(),getOption());
					}
				});
				optionLinkPopup.add(mi);
				optionLinkPopup.addSeparator();
			}
			showLinks=true;
		}

		if (showLinks) {
			final Option option = getOption(); 
			for(final Role r:dk.optionRoles.get(getOption())) {
				for(final Verb v:dk.roleVerbs.get(r)) {
					for(final Role.Link rv:v.getRoles()) {
						if (r==rv.getRole() && rv!=getRole()) {
							JMenuItem mi = new JMenuItem(v.getLabel()+": "+rv.getLabel()+": "+option.getLabel());
							mi.setToolTipText("Click to display this link in verb editor.");
							mi.addActionListener(new ActionListener(){
								public void actionPerformed(ActionEvent e) {
									setState(new State(v,rv,option,null,null));
								}
							});
							optionLinkPopup.add(mi);
						}
					}	
				}
			}
		}
	}

	private void unlinkRole(final Verb v,final Role.Link r){
		final Role.Link c = r.clone(false);
		final int pos = v.getRoleIndex(r);
		dk.deleteRole(v, pos);
		try { dk.addRole(v,pos,c);
		} catch(LimitException e) {}
		setState(new State(v,c,getOption(),null,null));
		new UndoableAction(swat,false,"unlink role "+v.getLabel()+": "+r.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				dk.deleteRole(v, pos);
				try {
					dk.addRole(v,pos,c);
				} catch(LimitException e) {}
				set(v,c);
			}
			@Override
			protected void myUndo() {
				dk.deleteRole(v, pos);
				try {
					dk.addRole(v,pos,r);
				} catch(LimitException e) {}
				set(v,r);
			}
		};
	}

	private void unlinkOption(final Verb v,final Role.Link r,final Option o){
		final Option c = o.clone();
		final int pos = r.getRole().getOptionIndex(o.getLabel());
		dk.deleteOption(r.getRole(),pos);
		try { dk.addOption(r.getRole(),pos,c);
		} catch(LimitException e) {}
		setState(new State(v,r,c,null,null));
		new UndoableAction(swat,false,"unlink option "+v.getLabel()+": "+r.getLabel()+": "+o.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				dk.deleteOption(r.getRole(),pos);
				try {
					dk.addOption(r.getRole(),pos,c);
				} catch(LimitException e) {}
				setState(new State(v,r,c,null,null));
			}
			@Override
			protected void myUndo() {
				dk.deleteOption(r.getRole(), pos);
				try {
					dk.addOption(r.getRole(),pos,o);
				} catch(LimitException e) {}
				setState(new State(v,r,o,null,null));
			}
		};
	}

	private void reloadEmotions(){
		emotionButtons.clear();
		emotionalReactionScriptsPanel.removeAll();
		if (mVerb.getRoleCount() > 0 && miRole!=-1) {			
			for (int i = 0; i < mRole.getRole().getEmotionCount(); ++i) {
				final ScriptDisplayButton zButton = new ScriptDisplayButton(new ScriptPath(mVerb,mRole,null),mRole.getRole().getEmotion(i),scriptButtons);
				zButton.addActionListener(scriptDisplayActionListener);
				if (mRole.getRole().getEmotion(i).getLabel().startsWith("Fill")
						&& mRole.getRole().getEmotion(i).getLabel().endsWith("Box")) {

						final Operator op = dk.getOperatorDictionary().getOperator(mRole.getRole().getEmotion(i).getLabel().substring(4));
						zButton.addMouseListener(new MouseAdapter(){
							@Override
							public void mousePressed(MouseEvent e) {
								if (e.isPopupTrigger()) triggerPopup(e);
							};
							public void mouseReleased(MouseEvent e) {
								if (e.isPopupTrigger()) triggerPopup(e);
							};
							public void triggerPopup(MouseEvent e) {
								fillBoxButtonPopup.removeAll();
								dk.traverseScripts(getVerb(),getRole(),new Script.NodeTraverser(){
									public boolean traversing(final Script s, Node n) {
										if (n.getOperator()==op) {
											final ScriptPath sp = new ScriptPath(verb,role,option);
											JMenuItem mi = new JMenuItem(Utils.concatStrings(sp.getPathComponents(s),": ",2));
											mi.addActionListener(new ActionListener(){
												public void actionPerformed(ActionEvent e) {
													VerbEditor.this.setScriptPath(sp,s);
												}
											});
											fillBoxButtonPopup.add(mi);
											return false;
										}
										return true;
									}
								});
								if (fillBoxButtonPopup.getComponentCount()==0){
									JMenuItem mi = new JMenuItem("The value of this box is never used.");
									mi.setEnabled(false);
									fillBoxButtonPopup.add(mi);
								}
								fillBoxButtonPopup.show(zButton,e.getX(),e.getY());
							}
						});
					}

				emotionButtons.add(zButton);
				JButton deleteButton = new DeleteButton();
				deleteButton.setToolTipText("delete this emotion");
				deleteButton.setActionCommand(String.valueOf(i));
				deleteButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						int i = Integer.parseInt(e.getActionCommand());
						deleteEmotion(i);
					}
				});
				Dimension db=deleteButton.getPreferredSize();
				db.height=zButton.getPreferredSize().height;
				deleteButton.setPreferredSize(db);

				Box zBox = Box.createHorizontalBox();
				zBox.add(deleteButton);
				zBox.add(zButton);						
				zBox.setAlignmentX(0.0f);
				emotionalReactionScriptsPanel.add(zBox);
			}
		}
		verbPanel.validate();
	}
	
	private void updateEmotionPopupMenu(){
		if (getRole()==null) return;
		
		// All items on the EmotionalReaction menu are always enabled
		// EXCEPT those that have already been implemented in the selected Role.
		// In other words, only one use of each emotion operator is permitted per Role.
		// This code is in good shape.
		emotionbtpopup.removeAll();
		for(final Operator op:dk.getOperatorDictionary().getOperators(OperatorDictionary.Menu.EmotionalReaction)) {
			JMenuItem mi=new JMenuItem(op.getLabel());
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setScriptPath(null,null);
					addEmotion(op);
				}
			});
			emotionbtpopup.add(mi);
		}
	} 

	/** Adds an emotional reaction script to the current role. */
	private void addEmotion(Operator op) {
		final State s = new State();
		dk.addEmotion(s.role.getRole(), op);

		final int index = s.role.getRole().getEmotionCount()-1;								
		final Script sc = s.role.getRole().getEmotion(index);
		reloadEmotions();
		updateEmotionPopupMenu();
		new UndoableAction(swat,false,"add emotion "+op.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo(){
				dk.addEmotion(s.role.getRole(), sc);
				reloadEmotions();
				updateEmotionPopupMenu();
				setState(s);
				stateStack.updateStackActions();
			}
			@Override
			public void myUndo(){
				dk.deleteEmotion(s.role.getRole(),index);
				reloadEmotions();
				updateEmotionPopupMenu();
				setState(s);
				stateStack.updateStackActions();
			}
		};
	}
	
	public String getSelectedVerbLabel(){
		return verbTree.getSelectedVerbLabel();
	}
	
//**********************************************************************		
	public void checkAddOptionButton() {
		addOptionButton.setEnabled(
			mRole != null && verbTree.getSelectedVerbLabel()!=null
			&& mRole.getRole().getOption(verbTree.getSelectedVerbLabel())==null
			&& dk.optionCount<dk.limits.maximumOptionCount);
		if (!addOptionButton.isEnabled() && mRole != null && dk.optionCount>=dk.limits.maximumOptionCount)
			addOptionButton.setToolTipText("Cannot have more than "+dk.limits.maximumOptionCount+" options in a storyworld.");
		else
			addOptionButton.setToolTipText("adds an option from the column on the left");
	}
//**********************************************************************		
	public void addOption(final String newOption) {		
		final State s = new State();
		try {
			dk.addOption(getRole().getRole(),Option.createOptionWithWordSockets(dk.getVerb(dk.findVerb(newOption))));
		} catch (LimitException e) {	throw new RuntimeException(e);	}
		addOption(getRole().getRole().getOptions().size()-1);
		final Option option=getRole().getRole().getOptions().get(getRole().getRole().getOptions().size()-1);
		new UndoableAction(swat,false,"add option "+newOption){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo(){
				setState(s);
				try {
					dk.addOption(getRole().getRole(),option);
				} catch (LimitException e) { throw new RuntimeException(e); }
				addOption(getRole().getRole().getOptions().size()-1);
			}
			@Override
			public void myUndo(){
				set(s.verb,s.role);
				dk.deleteOption(getRole().getRole(),option);
				deleteOption();
				setState(s);
			}
		};
	}
	private void addOption(int index){
		setScriptPath(null,null);
		miOption = index;
		rebuildOptionComboBox();
		setOption((Role.Option)(mRole.getRole().getOptions().get(miOption)));
		deleteOptionButton.setEnabled(true);
		optionComboBox.setSelectedIndex(miOption);
		checkAddOptionButton();
		updateSwatMenu();
		Swat.playSound("add.aiff");
	}
	
	private void moveOption(final int from,final int to){
		final Role.Link role = getRole();
		final State s = new State();
		role.getRole().getOptions().add(to,role.getRole().getOptions().remove(from));
		new UndoableAction(swat,false,"reorder options"){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				role.getRole().getOptions().add(to,role.getRole().getOptions().remove(from));
				if (s.role!=mRole)
					setState(s);
				else
					optionComboBox.moveIndex(from,to);
				optionComboBox.showPopup();
			}
			@Override
			public void myUndo() {
				role.getRole().getOptions().add(from,role.getRole().getOptions().remove(to));
				if (s.role!=mRole)
					setState(s);
				else
					optionComboBox.moveIndex(to,from);
				optionComboBox.showPopup();
			}
		};
	}
//**********************************************************************		
	public void setOption(Role.Option tOption) {
		if (mOption==tOption) return;
		scriptEditor.clearSelection();
		mSetOption(tOption);
	}
	private LinkedList<ScriptDisplayButton> wordsocketButtons=new LinkedList<ScriptDisplayButton>();
	private void mSetOption(Role.Option tOption) {
		if (mOption==tOption) return;
		
		if (getScriptBeingEdited()!=null) {
			switch(getScriptBeingEdited().getType()){
			case OptionAcceptable:
			case OptionDesirable:
			case Acceptable:
			case Desirable:
				setScriptPath(null, null);
			}
		}
		
		optionWordsocketsPanel.removeAll();
		wordsocketButtons.clear();
		inclinationPanel.removeAll();
		mOption = tOption;
		if (tOption != null) {
			for (int i=0; (i<getRole().getRole().getOptions().size()); ++i) {
				if (getRole().getRole().getOptions().get(i).getLabel().equals(mOption.getLabel())) {
					miOption = i;
				}
			}
			optionComboBox.setSelectedIndex(miOption);
			deleteOptionButton.setEnabled(true);
			swat.copyOption.setEnabled(true);
			// Start with 2 because WordSocket items 0 and 1 are Subject and Verb,
			//   for which there is never a choice.
			for(int i=2;i < Sentence.MaxWordSockets;i++)
				if (tOption.isWordSocketActive(i))
					optionWordsocketsPanel.add(new TermPanel(i, tOption.getPointedVerb()));

			optionAcceptableButton = new ScriptDisplayButton("Acceptable",scriptButtons);
			optionAcceptableButton.setForeground(tOption.getAcceptableScript().getBaseColor());
			optionAcceptableButton.setScriptPath(new ScriptPath(mVerb,mRole,mOption),tOption.getAcceptableScript());
			optionAcceptableButton.reformat();
			optionAcceptableButton.addActionListener(scriptDisplayActionListener);

			optionDesirableButton = new ScriptDisplayButton(new ScriptPath(mVerb,mRole,mOption),tOption.getDesirableScript(),scriptButtons);
			optionDesirableButton.addActionListener(scriptDisplayActionListener);
			inclinationPanel.add(optionAcceptableButton);
			inclinationPanel.add(optionDesirableButton);
			dk.setStartingOption(mOption);
		}
		else deleteOptionButton.setEnabled(false);
		verbPanel.validate();
		optionPanel.repaint();
		updateSwatMenu();
		updateOptionLinkButton();
		stateStack.pushState();
	}
//**********************************************************************		
	public void rebuildRoleComboBox() {		
		roleComboBox.removeAllItems();
		if (miRole==-1) {
			roleComboBox.setEnabled(false);
			return;
		}
		for (Role.Link rl: mVerb.getRoles()) {
			roleComboBox.addItem(rl);
		}
		roleComboBox.setEnabled(mVerb.getRoleCount() > 0);
		if (mVerb.getRoleCount() > 0)
			roleComboBox.setSelectedIndex(miRole);
		roleComboBox.setEnabled(roleComboBox.getItemCount() > 0);
	}
//**********************************************************************		
	public void rebuildOptionComboBox() {
		optionComboBox.removeAllItems();
		if (mRole!=null && mVerb.getRoleCount() > 0) {
			for (Option op: mRole.getRole().getOptions()) {
				optionComboBox.addItem(op);
			}			
			if (mRole.getRole().getOptions().size() > 0) {
				optionComboBox.setSelectedIndex(miOption);
			}
		}
		optionComboBox.setEnabled(mRole!=null && mRole.getRole().getOptions().size() > 0);
		optionComboBox.setEnabled(optionComboBox.getItemCount() > 0);
		if (mRole!=null) {
			deleteOptionButton.setEnabled(!mRole.getRole().getOptions().isEmpty());
		}
	}
//**********************************************************************
	
	private void addConsequence(Operator op) {
		setScriptPath(null,null);
		final State s = new State();

		final Script sc=dk.addConsequence(s.verb,op);

		final int index = s.verb.getConsequenceCount()-1;
		reloadConsequences();
		new UndoableAction(swat,false,"add consequence to "+getVerb().getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo(){
				dk.addConsequence(s.verb,sc);
				setState(s);	
				reloadConsequences();
				stateStack.updateStackActions();
			}
			@Override
			public void myUndo(){
				dk.deleteConsequence(s.verb,index);
				setState(s);
				reloadConsequences();
				stateStack.updateStackActions();
			}
		};
	}
	private void deleteConsequence(final int index) {
		setScriptPath(null,null);
		final State s = new State();
		final Script sc = s.verb.getConsequence(index);
		new UndoableAction(swat,"delete consequence "+sc.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo(){
				dk.deleteConsequence(s.verb,index);
				setState(s);
				reloadConsequences();
				stateStack.updateStackActions();
			}
			@Override
			public void myUndo(){
				dk.addConsequence(s.verb,index,sc);
				setState(s);	
				reloadConsequences();
				stateStack.updateStackActions();
			}
		};
	}
//**********************************************************************		
	public void deleteEmotion(final int tIndex) {
		setScriptPath(null,null);
		final Script script = getRole().getRole().getEmotion(tIndex);
		final State s = new State();
		new UndoableAction(swat,"delete emotion "+script.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo(){
				dk.deleteEmotion(s.role.getRole(),tIndex);
				setState(s);
				reloadEmotions();
				updateEmotionPopupMenu();
				stateStack.updateStackActions();
			}
			@Override
			public void myUndo(){
				dk.addEmotion(s.role.getRole(),script);
				setState(s);
				reloadEmotions();
				updateEmotionPopupMenu();
				stateStack.updateStackActions();
			}
		};		
	}
	
	public void cutScriptSelection() {
		scriptEditor.cutSelection();
	}

	public void pasteNode(Node n) {
		scriptEditor.pasteNode(n);
	}

	public void copyScriptSelection() {
		scriptEditor.copySelection();
	}

//**********************************************************************		
	public void copyOption() {
		clipboardOption = getOption().clone();
		clipboardOptionLink = getOption();
		swat.pasteOption.setEnabled(true);
		swat.pasteOptionLink.setEnabled(true);
	}
//**********************************************************************
	public void pasteOption() {
		if (dk.optionCount>=dk.limits.maximumOptionCount) {
			Utils.showErrorDialog(null, "Cannot have more than "+dk.limits.maximumOptionCount+" options in a storyworld.","File error");
			return;
		}

		if (getRole().getRole().getOption(clipboardOption.getLabel())!=null) {
			if (SwingUtilities.getWindowAncestor(optionComboBox)!=null) 
				errorPopup.showError(scriptEditor.swat.getMyFrame(),optionComboBox.getLocationOnScreen(),"The current role already contains option "+clipboardOption.getLabel()+".");
			return;
		}

		final Option option1st = clipboardOption.clone();
		final State s = new State();
		new UndoableAction(swat,"paste option "+clipboardOption.getLabel()){
			private static final long serialVersionUID = 1L;
			Option option2;
			@Override
			public void myRedo(){
				set(s.verb,s.role);
				try {
					option2 = dk.addOption(getRole().getRole(),option1st);
					addOption(getRole().getRole().getOptions().size()-1);
				} catch (LimitException e) {}
			}
			@Override
			public void myUndo(){
				set(s.verb,s.role);
				dk.deleteOption(getRole().getRole(),option2);
				deleteOption();
				setState(s);
			}
		};
	}
	
	public void pasteOptionLink() {
		if (dk.optionCount>=dk.limits.maximumOptionCount) {
			Utils.showErrorDialog(null, "Cannot have more than "+dk.limits.maximumOptionCount+" options in a storyworld.","File error");
			return;
		}

		if (getRole().getRole().getOption(clipboardOptionLink.getLabel())!=null) {
			if (SwingUtilities.getWindowAncestor(optionComboBox)!=null) 
				errorPopup.showError(scriptEditor.swat.getMyFrame(),optionComboBox.getLocationOnScreen(),"The current role already contains option "+clipboardOption.getLabel()+".");
			return;
		}

		final Option option1st = clipboardOptionLink;
		final State s = new State();
		new UndoableAction(swat,"paste option link to "+clipboardOptionLink.getLabel()){
			private static final long serialVersionUID = 1L;
			Option option2;
			@Override
			public void myRedo(){
				set(s.verb,s.role);
				try {
					option2 = dk.addOption(getRole().getRole(),option1st);
					addOption(getRole().getRole().getOptions().size()-1);
				} catch (LimitException e) {}
			}
			@Override
			public void myUndo(){
				set(s.verb,s.role);
				dk.deleteOption(getRole().getRole(),option2);
				deleteOption();
				setState(s);
			}
		};
	}

//**********************************************************************		
	public void copyRole() {
		clipboardRoleLink = getRole();
		clipboardRole = getRole().clone(true);
		clipboardRole.setLabel(Utils.truncate("copy of "+mRole.getLabel(),Deikto.MAXIMUM_FIELD_LENGTH));
		swat.pasteRole.setEnabled(true);
		swat.pasteRoleLink.setEnabled(true);
	}
//**********************************************************************		
	public void pasteRole() {
		if (dk.roleCount>=dk.limits.maximumRoleCount) {
			Utils.showErrorDialog(null, "Cannot have more than "+dk.limits.maximumRoleCount+" roles in a storyworld.","File error");
			return;
		}
			
		final Role.Link role1st = clipboardRole.clone(true);
		
		String baseName = role1st.getLabel();
		baseName = baseName.substring(0,Math.min(baseName.length(),Deikto.MAXIMUM_FIELD_LENGTH-4));
		int i=0;
		while(getVerb().getRole(i==0?baseName:baseName+" "+i)!=null) i++;
		role1st.setLabel(i==0?baseName:baseName+" "+i);

		final State s = new State();
		new UndoableAction(swat,"paste role "+role1st.getLabel()){
			private static final long serialVersionUID = 1L;
			int irole2;
			@Override
			public void myRedo(){
				set(s.verb,s.role);
				try {
					mRole = dk.addRole(s.verb,role1st);
					addRole(mVerb.getRoleCount()-1);
					irole2 = miRole;
				} catch(LimitException e){}
			}
			@Override
			public void myUndo(){
				set(s.verb,s.role);
				dk.deleteRole(getVerb(),irole2);
				deleteRole();
				setState(s);
			}
		};
	}
	
	public void pasteRoleLink() {
		if (dk.roleCount>=dk.limits.maximumRoleCount) {
			Utils.showErrorDialog(null, "Cannot have more than "+dk.limits.maximumRoleCount+" roles in a storyworld.","File error");
			return;
		}
			
		final Role.Link role1st = new Role.Link(clipboardRoleLink);
		
		String baseName = Utils.truncate("link to "+role1st.getLabel(),Deikto.MAXIMUM_FIELD_LENGTH);
		baseName = baseName.substring(0,Math.min(baseName.length(),Deikto.MAXIMUM_FIELD_LENGTH-4));
		int i=0;
		while(getVerb().getRole(i==0?baseName:baseName+" "+i)!=null) i++;
		role1st.setLabel(i==0?baseName:baseName+" "+i);

		final State s = new State();
		new UndoableAction(swat,"paste role "+role1st.getLabel()){
			private static final long serialVersionUID = 1L;
			int irole2;
			@Override
			public void myRedo(){
				set(s.verb,s.role);
				try {
					mRole = dk.addRole(s.verb,role1st);
					addRole(mVerb.getRoleCount()-1);
					irole2 = miRole;
				} catch(LimitException e){}
			}
			@Override
			public void myUndo(){
				set(s.verb,s.role);
				dk.deleteRole(getVerb(),irole2);
				deleteRole();
				setState(s);
			}
		};
	}
//**********************************************************************
		/**
		 * Toggles the button that displays a given script.
		 * */
		private void toggleScriptButton(Script script){
			if (script==null) return;
			switch(script.getType()){
			case AssumeRoleIf:
				assumeRoleIfButton.setSelected(true);
				break;
			case AbortIf:
				abortButton.setSelected(true);
				break;
			case OptionAcceptable:
				optionAcceptableButton.setSelected(true);
				break;
			case OptionDesirable:
				optionDesirableButton.setSelected(true);
				break;
			case Consequence:
				for(ScriptDisplayButton sd:consequenceButtons)
					if (sd.getScript()==script){
						sd.setSelected(true);
						break;
					}
				break;
			case Emotion:
				for(ScriptDisplayButton sd:emotionButtons)
					if (sd.getScript()==script){
						sd.setSelected(true);
						break;
					}
				break;
			case Acceptable:
			case Desirable:
				for(ScriptDisplayButton sd:wordsocketButtons)
					if (sd.getScript()==script){
						sd.setSelected(true);
						break;
					}
				break;
			}
		};
//		**********************************************************************
		public void setScriptPath(ScriptPath sp,Script s) {
			if (s!= null) {
				// is this next line really necessary?
				setVerb(sp.getVerb());
		    	if (sp.getRole()!=null) {
		    		if (sp.getVerb().containsRole(sp.getRole()))
		    			setRole(sp.getRole());
		    		else
		    			return;
		    	}
		    	if (sp.getOption()!=null)
		    		if (sp.getRole().getRole().getOptions().contains(sp.getOption()))
		    			setOption(sp.getOption());
		    		else
		    			return;

		    	if (!isConsistentState(sp.getVerb(), sp.getRole(), sp.getOption(), s))
		    		return;

				scriptEditor.setScriptPath(sp,s);
				toggleScriptButton(s);
			} else {
		    	scriptEditor.setScriptPath(null,null);
				unselectScript.setSelected(true);
			}
			
			if (s!=null)
				stateStack.pushState();
		}
//**********************************************************************		
		public void deleteOption(Option tOption) {
			final State s = new State();
			final Option option = tOption;
			new UndoableAction(swat,"delete option "+tOption.getLabel()){
				private static final long serialVersionUID = 1L;
				int index;
				@Override
				public void myRedo(){
					set(s.verb,s.role);
					index=s.role.getRole().getOptions().indexOf(option);
					dk.deleteOption(s.role.getRole(),index);
					deleteOption();
					stateStack.updateStackActions();
				}
				@Override
				public void myUndo(){
					set(s.verb,s.role);					
					try {
						dk.addOption(s.role.getRole(),index,option);
					} catch (LimitException e) { throw new RuntimeException(e); }
					addOption(index);
					setState(s);
					stateStack.updateStackActions();
				}
			};
		}
		
		private void deleteOption() {	
			setScriptPath(null,null);
			if (mRole.getRole().getOptions().isEmpty()) {
				deleteOptionButton.setEnabled(false);
				setOption(null);			}
			else {
				miOption = 0;
				setOption(mRole.getRole().getOptions().get(miOption));
			}
			checkAddOptionButton();
			rebuildOptionComboBox();
			Swat.playSound("delete.aiff");
		}
//**********************************************************************
	public void addRole(final String tLabel) {		
		if (tLabel != null) {
			final State s = new State();
			Role.Link maybeRole=null;
			try {
				maybeRole = dk.addRole(getVerb(),tLabel,true);
			} catch (LimitException ex) { throw new RuntimeException(ex); }
			
			final Role.Link role = maybeRole;
			addRole(mVerb.getRoleCount()-1);
			final int irole2 = miRole;
			new UndoableAction(swat,false,"add role "+tLabel){
				private static final long serialVersionUID = 1L;
				@Override
				public void myRedo(){
					setVerb(s.verb);
					try {
						dk.addRole(getVerb(),role);
					} catch (LimitException e){
						e.printStackTrace();
					}
					mRole = role;
					addRole(mVerb.getRoleCount()-1);					
				}
				@Override
				public void myUndo(){
					setVerb(s.verb);
					dk.deleteRole(getVerb(),irole2);
					deleteRole();
					setState(s);
				}
			};
		}
	}
	public void deleteRole(Role.Link tRole) {
		final Verb verb = getVerb();
		final Role.Link role = tRole;
		new UndoableAction(swat,"delete role "+role.getLabel()){
			private static final long serialVersionUID = 1L;
			int index;
			@Override
			public void myRedo(){
				setVerb(verb);
				index=verb.getRoleIndex(role);
				dk.deleteRole(verb,index);
				deleteRole();
				stateStack.updateStackActions();
			}
			@Override
			public void myUndo(){
				setVerb(verb);
				try {
					dk.addRole(verb,index,role);
				} catch (LimitException ex) { throw new RuntimeException(ex); }
				addRole(index);
				stateStack.updateStackActions();
			}
		};
	}

//	**********************************************************************
	public void checkAddRoleButton() {
		addRoleButton.setEnabled(dk.roleCount<dk.limits.maximumRoleCount);
		if (!addRoleButton.isEnabled())
			addRoleButton.setToolTipText("Cannot have more than "+dk.limits.maximumRoleCount+" roles in a storyworld.");
		else
			addRoleButton.setToolTipText("creates a new role");
	}
//**********************************************************************		
	private void addRole(int index){
		setScriptPath(null,null);
		miRole = index;
		loadRoleInfo();
		checkAddRoleButton();
		Swat.playSound("add.aiff");				
	}
//	**********************************************************************
	private void deleteRole(){
		setScriptPath(null,null);
		if (mVerb.getRoleCount() > 0) {
			miRole = 0;
			mRole = mVerb.getRole(miRole);
			roleComboBox.setSelectedIndex(0);
		} else {
			miRole = -1;
			mRole = null;
			deleteRoleButton.setEnabled(false);
		}
		checkAddRoleButton();
		loadRoleInfo();
		Swat.playSound("delete.aiff");				
	}
//**********************************************************************	
	public void rename() {
		String zLabel = JOptionPane.showInputDialog("New Role Name:", mRole.getLabel());
		if (zLabel != null) {
			mRole.setLabel(zLabel);
			rebuildRoleComboBox();
		}
	}
//**********************************************************************	
	public void refresh(){
		reloadConsequences();
		reloadEmotions();
		
		scriptEditor.refresh();

		for (OperatorDictionary.Menu m: OperatorDictionary.ConsequenceMenus) 
			switch(m){
			case SetProp:
			case SetStage:
			case SetActor:
			case SetActorP:
			case SetActorC:
				for(int i=0;i<consequencebtpopup.getComponentCount();i++){
					JMenu menu=(JMenu)consequencebtpopup.getComponent(i);
					if (menu.getText().equals(m.name())){
						menu.removeAll();
						for(Operator op:dk.getOperatorDictionary().getOperators(m)){
							JMenuItem mi=new JMenuItem(op.getLabel());
							mi.addActionListener(new ConsequenceActionListener(op));
							menu.add(mi);
						}
					}
				}
				break;
				default:;
			}

		updateEmotionPopupMenu();
		
		if (sentenceDisplayEditor.isVisible())
			sentenceDisplayEditor.refresh();
		
		myPanel.validate();
	}
//	**********************************************************************
	public void setVerb(String verbLabel) {
		if (getVerb()!=null && verbLabel.equals(getVerb().getLabel())) return;
		
		miVerb = dk.findVerb(verbLabel);
		setVerb(dk.getVerb(miVerb));
	}
	public void reloadVerbName(){
		verbLabel.setText(getVerb().getLabel());
		verbPanel.repaint();
		if (verbPropertiesEditor!=null)
			verbPropertiesEditor.setTitle(getVerb().getLabel());
		if (swat.comeFromLizard!=null)
			swat.comeFromLizard.refreshTitle();
	}
	
	public void repaintOptionCombobox(){
		if (optionComboBox.getSelectedItem()!=null)
			optionComboBox.setSelectedItem(optionComboBox.getSelectedItem());
	}
	
	public void repaintScript(){
		scriptEditor.repaintScript();
	}
//	**********************************************************************	
	public void setVerb(Verb v) {
		if (mVerb==v) return;

		miVerb = dk.findVerb(v.getLabel());
		mVerb = v;

		setScriptPath(null,null);
		mSetOption(null);
		setRole((Role.Link)null);
		reloadVerbInfo();
		verbTree.setSelectedVerb(v.getLabel());
		for(VerbListener vl : verbListeners) vl.verbChanged(this);

		stateStack.pushState();
		myPanel.repaint();
	}
//**********************************************************************
	public Verb getVerb() {
		return mVerb;
	}
//**********************************************************************	
	public void setRole(Role.Link r) {
		if (mRole==r) return;
		
		mRole = r;
		miRole = getVerb().getRoleIndex(r);
		loadRoleInfo();
		stateStack.pushState();
	}
//**********************************************************************
	public void setRole(String roleLabel) {
		if (getRole()!=null && roleLabel.equals(getRole().getLabel())) return;
		
		int i=0;
		while (i<getVerb().getRoleCount()	&& 
				!getVerb().getRole(i).getLabel().equals(roleLabel))
			i++;
		if (i<getVerb().getRoleCount()) {
			miRole = i;
			loadRoleInfo();
		} else System.out.println("VerbEditor.setRole(String): \""+roleLabel+"\" not found.");
		stateStack.pushState();
	}
//**********************************************************************
	public Role.Link getRole() {
		return mRole;
	}
//**********************************************************************
	public void updateSwatMenu(){		
		swat.pasteOption.setEnabled(getVerb()!=null && clipboardOption!=null && getVerb().getRoleCount()>0);
		swat.pasteOptionLink.setEnabled(getVerb()!=null && clipboardOption!=null && getVerb().getRoleCount()>0);
		swat.pasteRole.setEnabled(getVerb()!=null && clipboardRole!=null);
		swat.pasteRoleLink.setEnabled(getVerb()!=null && clipboardRoleLink!=null);
		swat.copyRole.setEnabled(getVerb()!=null && getVerb().getRoleCount()>0);
		swat.copyOption.setEnabled(getRole()!=null && getRole().getRole().getOptions().size()>0);
	}
//**********************************************************************
	public Role.Option getOption() {
		return mOption;
	}
//**********************************************************************
	public Script getScriptBeingEdited() {
		return scriptEditor.getScript();
	}
//**********************************************************************
	public JPanel getMyPanel() {
		return myPanel;
	}
//**********************************************************************
	private void set(Verb verb,Role.Link role){
		setVerb(verb);
		setRole(role);
	}
//	**********************************************************************

	/**
	 *  Sets a script in the editor.
	 *  The state could have dated if the parts of it
	 *  where deleted from the model. So here we set state
	 *  attributes only after checking that they still exist.
	 */
	public boolean setState(State s){
		Script.Type scType = s.script==null?Script.Type.None:s.script.getType();
		boolean set = getVerb()!=s.verb;
		if (set && !dk.containsVerb(s.verb)) return false;

		boolean oldIgnoreStack = stateStack.isIgnoringPushState();
		boolean roleExists = s.verb.getRoleIndex(s.role)!=-1;
		// the following line prevents pushing a state for the default role set by setVerb
		stateStack.setIgnoringPushState(oldIgnoreStack || roleExists); 
		setVerb(s.verb);
		if (roleExists) {
			set=set || getRole()!=s.role;
			boolean optionExists = s.role.getRole().getOptions().contains(s.option);
			// the following line prevents pushing a state for the default option set by setRole
			stateStack.setIgnoringPushState(oldIgnoreStack || optionExists);
			setRole(s.role);
			if (optionExists){
				set=set || getOption()!=s.option;
				stateStack.setIgnoringPushState(oldIgnoreStack);
				setOption(s.option);
			}
		}
		// if the default role/options match the intended ones, a state may still need to be pushed
		stateStack.pushState();
		stateStack.setIgnoringPushState(oldIgnoreStack);
		if (isConsistentState(s.verb, s.role, s.option, s.script)){
			Pair<ScriptPath,Script> sp = s.script==null?new Pair<ScriptPath,Script>(null,null):dk.getScriptPath(scType, new ScriptPath(s.verb,s.role,s.option).getScriptLocators(s.script));
			s.script = sp.second;
			set=set || getScriptBeingEdited()!=s.script;
			setScriptPath(sp.first,sp.second);
			if (s.tp!=null) 
				scriptEditor.setTreeNode(s.tp);
		};		
		return set;
	}

	/**
	 *  Tells if setting this state would change the editor.
	 */
	boolean isChangingState(State s){
		Script.Type scType = s.script==null?Script.Type.None:s.script.getType();
		boolean set = getVerb()!=s.verb;
		if (set && !dk.containsVerb(s.verb)) return false;
		if (s.verb.getRoleIndex(s.role)!=-1) {
			set=set || getRole()!=s.role;
			if (s.role.getRole().getOptions().contains(s.option)){
				set=set || getOption()!=s.option;
			}
		}		
		if (isConsistentState(s.verb, s.role, s.option, s.script))
			set=set || getScriptBeingEdited()!=s.script
				|| getScriptBeingEdited()!=null && s.script!=null &&
				getScriptBeingEdited()!=dk.getScriptPath(scType, new ScriptPath(s.verb,s.role,s.option).getScriptLocators(s.script)).second;
		return set;
	}
	
	/** 
	 * Tells if the state is consistent.
	 * <p>
	 * This may return false as a consequence of removing an option,
	 * a role, or an emotion script from the model. 
	 * */
	private static boolean isConsistentState(Verb v,Role.Link r,Option o,Script s){
		if (v==null)
			return false;
		if (r!=null && !v.containsRole(r))
			return false;
		if (o!=null && (r==null || !r.getRole().getOptions().contains(o)))
			return false;
		
		if (s!=null)
			switch(s.getType()){
			case AbortIf:
				return v.getAbortScript()==s;
			case Consequence:
				return v.getConsequences().contains(s);
			case AssumeRoleIf:
				return r!=null && v.containsRole(r) 
					&& r.getRole().getAssumeRoleIfScript()==s;
			case Emotion:
				return r!=null && v.containsRole(r) 
					&& r.getRole().getEmotionIndex(s)>=0;
			case OptionDesirable:
				return r!=null && o!=null 
					&& v.containsRole(r)
					&& r.getRole().getOptions().contains(o)
					&& o.getDesirableScript()==s;
			case OptionAcceptable:
				return r!=null && o!=null 
					&& v.containsRole(r)
					&& r.getRole().getOptions().contains(o)
					&& o.getAcceptableScript()==s;
			case Acceptable:
				return r!=null && o!=null 
					&& v.containsRole(r)
					&& r.getRole().getOptions().contains(o)
					&& o.getPointedVerb().isWordSocketActive(s.getIWordSocket())
					&& o.getWordSocket(s.getIWordSocket()).getAcceptableScript()==s;
			case Desirable:
				return r!=null && o!=null 
					&& v.containsRole(r)
					&& r.getRole().getOptions().contains(o)
					&& o.getPointedVerb().isWordSocketActive(s.getIWordSocket())
					&& o.getWordSocket(s.getIWordSocket()).getDesirableScript()==s;
			default:
				return true;
			}
		else
			return true;
	} 
	
	/**
	 * This class stores the verb, role, option and script being displayed.
	 * It can be used to restore the verb editor state at a later moment using
	 * {@link VerbEditor#setState(com.storytron.swat.verbeditor.VerbEditor.State)}. 
	 * */
	public class State {
		Verb verb;
		Role.Link role;
		Option option;
		Script script;
		Node tp;

		/** Creates a current state of verb editor. */
		public State(){
			resetState();	
		}
		
		/** Creates a state as dictated by the parameter values. */
		public State(Verb verb,Role.Link role,Option option,Script script,Node tp){
			this.verb=verb;
			this.role=role;
			this.option=option;
			this.script=script;
			this.tp=tp;
		}

		/** Gets the current state from the editor. */
		public void resetState(){
			verb = getVerb();
			role = getRole();
			option = getOption();
			script=getScriptBeingEdited();
			tp = scriptEditor.getSelectedNode();
		}
		/** Test if the current verb editor state is compatible with the
		 * state stored in this instance. Roughly a state is compatible with 
		 * another if their non null components are the same.
		 * */
		public boolean compatibleState(){
			return verb == getVerb() && 
				(role == null || getRole() == null || role == getRole()) &&
				(option == null || getOption() == null || option == getOption()) && 
				(script == null ||  script == getScriptBeingEdited());
		}
	}

	/**
	 * Represents the option components associated to wordsockets. 
	 * */
	private class TermPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private ScriptDisplayButton accButton, desButton;
		private Insets insets=new Insets(2,1,0,1);
		private JLabel label;
		private JComponent bts;
		//private final static int MAXIMUM_LABEL_WIDTH=73;
		TermPanel(final int termIndex, final Verb tVerb) {
			super(null);
			setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
			setOpaque(false);
			
			setAlignmentX(Component.LEFT_ALIGNMENT);
			setAlignmentY(Component.CENTER_ALIGNMENT);
			final Script accScript = getOption().getWordSocket(termIndex).getAcceptableScript();
			accButton = new ScriptDisplayButton("Acceptable",scriptButtons);
			accButton.setForeground(accScript.getBaseColor());
			accButton.setScriptPath(new ScriptPath(getVerb(),getRole(),getOption()),accScript);
			accButton.setMargin(insets);
			accButton.addActionListener(scriptDisplayActionListener);			
			desButton = new ScriptDisplayButton(new ScriptPath(getVerb(),getRole(),getOption()),getOption().getWordSocket(termIndex).getDesirableScript(),scriptButtons);
			desButton.addActionListener(scriptDisplayActionListener);
			desButton.setMargin(insets);
			wordsocketButtons.add(accButton);
			wordsocketButtons.add(desButton);

			bts=new JPanel(new GridLayout(0,1));
			bts.setOpaque(false);
			//bts.setBorder(BorderFactory.createMatteBorder(0,0, 1, 1, Color.darkGray));
			bts.add(accButton);
			bts.add(desButton);
			
			label=new JLabel(tVerb.getWordSocketFullLabel(termIndex)){
				private static final long serialVersionUID = 1L;
				@Override
				public String getToolTipText() {
					return Utils.nullifyIfEmpty(tVerb.getNote(termIndex));
				}
			};
			ToolTipManager.sharedInstance().registerComponent(label);
			
			JComponent aux=Box.createHorizontalBox();
			aux.add(Box.createRigidArea(new Dimension(5,5)));
			aux.add(label);
			aux.add(Box.createHorizontalGlue());
			
			add(aux);
			add(bts);
			int h = getPreferredSize().height;
			setPreferredSize(new Dimension(50,h));
			setMaximumSize(new Dimension(50,h));
		}
		
		@Override
		protected void paintBorder(Graphics g){
			g.setColor(Swat.darkShadow);
			g.drawLine(bts.getX(),1+label.getHeight()/2, label.getX()-2,1+label.getHeight()/2);
			g.drawLine(label.getX()+label.getWidth()+4,1+label.getHeight()/2, bts.getX()+bts.getWidth()-1,1+label.getHeight()/2);
			
			g.drawLine(bts.getX(),1+label.getHeight()/2, bts.getX(), 1+label.getHeight());
			g.drawLine(bts.getX()+bts.getWidth()-1,1+label.getHeight()/2, bts.getX()+bts.getWidth()-1, bts.getX()+bts.getHeight()-1);
		}
	}

	/**
	 * Buttons for inserting tokens into scripts.
	 * Currently they have exagonal shape.
	 * */
	public static class ButtonShortcut extends JButton {
		private static final long serialVersionUID = 1L;
		String myLabel;
		Operator mOperator = null;
		ButtonShortcut(String tOperatorLabel) {				
			super();		
			myLabel = tOperatorLabel;
			setText(myLabel);
			setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(Swat.darkShadow,3),getBorder()));
			setEnabled(true);
			setOpaque(false);
			Insets myInsets = getMargin();
			myInsets.left -= 12;
			myInsets.right -= 12;
			myInsets.bottom -= 3;
			myInsets.top -= 3;
			setMargin(myInsets);
		}
	}

	/**
	 * Buttons for inserting tokens into scripts.
	 * Currently they have exagonal shape.
	 * */
	public static class OctagonalButton extends JButton implements MouseListener {
		private static final long serialVersionUID = 1L;
		OctagonalButton(String label) {				
			super(label);		
			setEnabled(true);
			setOpaque(false);
			Insets myInsets = getMargin();
			myInsets.left -= 6;
			myInsets.right -= 6;
			setMargin(myInsets);
			addMouseListener(this);
		}
	
		boolean mouseIn=false;
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) { mouseIn=true; }
		public void mouseExited(MouseEvent e) { mouseIn=false; }
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}

		@Override
		public void paint(Graphics g) {
			Graphics2D g2d=(Graphics2D)g;
			Shape oldclip=g.getClip();
			int cornersize=getHeight()/3;
			Polygon p = new Polygon();
			p.addPoint(cornersize, 0);
			p.addPoint(getWidth()-cornersize, 0);
			p.addPoint(getWidth(), cornersize);
			p.addPoint(getWidth(), getHeight()-cornersize);
			p.addPoint(getWidth()-cornersize, getHeight());
			p.addPoint(cornersize, getHeight());
			p.addPoint(cornersize, getHeight());
			p.addPoint(0, getHeight()-cornersize);
			p.addPoint(0, cornersize);
						
			g2d.clip(p);
			if (!getModel().isArmed() && getModel().isPressed() || !isEnabled()){
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
			}
			super.paint(g);
			
			g.setColor(Swat.darkShadow);
			if (getModel().isPressed())
				g.drawLine(0, cornersize+1, cornersize, 1);
			if (!mouseIn || getModel().isPressed()) {
				g.drawLine(getWidth()-cornersize, 1, getWidth()-1, cornersize);
				g.drawLine(getWidth()-cornersize, getHeight()-1, getWidth()-1,getHeight()-cornersize);
				g.drawLine(1, getHeight()-cornersize+1, cornersize-1, getHeight()-1);
				g.drawLine(1, cornersize-1, cornersize-1, 1);
			} else { // rollover effect
				g.setColor(Swat.shadow);
				g.drawLine(getWidth()-cornersize, 1, getWidth()-1, cornersize);
				g.drawLine(getWidth()-cornersize, getHeight()-1, getWidth()-1,getHeight()-cornersize);
				g.drawLine(1, getHeight()-cornersize+1, cornersize-1, getHeight()-1);
				g.drawLine(1, cornersize-1, cornersize-1, 1);
				g.setColor(Swat.darkShadow);
				g.drawLine(getWidth()-cornersize-1, 1, getWidth()-2, cornersize);
				g.drawLine(getWidth()-cornersize, getHeight()-2, getWidth()-2,getHeight()-cornersize);
				g.drawLine(2, getHeight()-cornersize+1, cornersize-1, getHeight()-2);
				g.drawLine(2, cornersize-1, cornersize, 1);
				g.setColor(Swat.shadow);
				g.drawLine(getWidth()-cornersize-1, 2, getWidth()-3, cornersize);
				g.drawLine(getWidth()-cornersize, getHeight()-3, getWidth()-3,getHeight()-cornersize);
				g.drawLine(3, getHeight()-cornersize+1, cornersize-1, getHeight()-3);
				g.drawLine(3, cornersize-1, cornersize, 2);
			}
			
			g.setClip(oldclip);
		}
	}


	private class ConsequenceActionListener implements ActionListener {
		private static final long serialVersionUID = 1L;
		Operator op;
		public ConsequenceActionListener(Operator op){
			this.op=op;
		}
		public void actionPerformed(ActionEvent e) {
			addConsequence(op);
		}
	};
	
	
	//private class LineBox extends JPanel {
	//	private static final long serialVersionUID = 1L;	
	//	public LineBox(int margin,Color color){
	//		super(new BorderLayout());
	//		setOpaque(false);
	//		add(Box.createHorizontalGlue());
	//		setMaximumSize(new Dimension(10000,3));
	//		setBorder(BorderFactory.createCompoundBorder(
	//				BorderFactory.createEmptyBorder(0,margin,0,margin),
	//				BorderFactory.createMatteBorder(0,0,1,0,color)));
	//	}
	//}

	/** Class with methods used for testing of VerbEditor. */
	public abstract static class Test {
		public static Scriptalyzer openScriptalyzer(VerbEditor ve) {
			return ve.scriptEditor.openScriptalyzer();
		}
		public static VerbPropertiesEditor openPropertiesEditor(VerbEditor ve) {
			ve.propertiesButton.doClick();
			return ve.verbPropertiesEditor; 
		}
		public static SentenceDisplayEditor openSentenceDisplayEditor(VerbEditor ve) {
			ve.sentenceDisplayButton.doClick();
			return ve.sentenceDisplayEditor; 
		}
		public static VerbTree getVerbTree(VerbEditor ve){
			return ve.verbTree;
		}
		public static ScriptEditor<State> getScriptEditor(VerbEditor ve){
			return ve.scriptEditor;
		}
		
		/** Adds a consequence to the current verb. */
		public static void addConsequence(VerbEditor ve,String label) {
			ve.addConsequence(ve.dk.getOperatorDictionary().getOperator(label));
		} 
		/** Deletes a consequence of the current verb. */
		public static void deleteConsequence(VerbEditor ve,String label) {
			ve.deleteConsequence(ve.getVerb().getConsequenceIndex(label));
		} 
		/** Adds a consequence to the current verb. */
		public static void toggleConsequence(VerbEditor ve,String label) {
			for(AbstractButton bt:ve.consequenceButtons)
				if (bt.getText().equals(label)){
					bt.doClick();
					break;
				}
		} 
		/** Adds a consequence to the current verb. */
		public static void toggleAssumeRoleIf(VerbEditor ve) {
			ve.assumeRoleIfButton.doClick();
		} 
		/** Adds an emotional reaction to the current role. */
		public static void addEmotion(VerbEditor ve,String label) {
			ve.addEmotion(ve.dk.getOperatorDictionary().getOperator(label));
		} 
		/** Adds a role to the current verb. */
		public static void addRole(VerbEditor ve,String label) {
			ve.addRole(label);
		} 
		/** Deletes the current role. */
		public static void deleteRole(VerbEditor ve) {
			ve.deleteRole(ve.getRole());
		} 
		/** Adds an option to the current role. */
		public static void moveOption(VerbEditor ve,String label,int to) {
			ve.moveOption(ve.getRole().getRole().getOptionIndex(label),to);
		} 
		/** Adds an option to the current role. */
		public static void addOption(VerbEditor ve,String label) {
			ve.addOption(label);
		} 
		/** Deletes the current option. */
		public static void deleteOption(VerbEditor ve) {
			ve.deleteOption(ve.getOption());
		}
		/** Closes the auxiliary windows. */
		public static void closeAuxWindows(VerbEditor ve){
			if (ve.verbPropertiesEditor!=null)
				ve.verbPropertiesEditor.setVisible(false);
			ScriptEditor.Test.closeAuxWindows(ve.scriptEditor);
		}
		
		public static boolean isPreviousStateButtonEnabled(VerbEditor ve){
			return ve.backwardButton.isEnabled();
		}
		public static boolean isNextStateButtonEnabled(VerbEditor ve){
			return ve.forwardButton.isEnabled();
		}
		public static void gotoPreviousState(VerbEditor ve){
			ve.backwardButton.doClick();
		}
		public static void gotoNextState(VerbEditor ve){
			ve.forwardButton.doClick();
		}
		
		public static boolean isRoleLinkButtonEnabled(VerbEditor ve){
			return ve.roleLinkButton.isEnabled();
		}
		public static boolean isOptionLinkButtonEnabled(VerbEditor ve){
			return ve.optionLinkButton.isEnabled();
		}
		public static ArrayList<JMenuItem> getRoleLinkItems(VerbEditor ve){
			ArrayList<JMenuItem> ms = new ArrayList<JMenuItem>(); 
			for(int i=0;i<ve.roleLinkPopup.getComponentCount();i++) {
				Component comp = ve.roleLinkPopup.getComponent(i);
				if (comp instanceof JMenuItem)
					ms.add((JMenuItem)comp);
			}
			return ms;
		}
		public static ArrayList<JMenuItem> getOptionLinkItems(VerbEditor ve){
			ArrayList<JMenuItem> ms = new ArrayList<JMenuItem>(); 
			for(int i=0;i<ve.optionLinkPopup.getComponentCount();i++) {
				Component comp = ve.optionLinkPopup.getComponent(i);
				if (comp instanceof JMenuItem)
					ms.add((JMenuItem)comp);
			}
			return ms;
		}
	}

}
