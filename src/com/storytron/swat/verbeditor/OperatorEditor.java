package com.storytron.swat.verbeditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import com.storytron.enginecommon.Triplet;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.Swat;
import com.storytron.swat.util.AddButton;
import com.storytron.swat.util.ComponentLabeledPanel;
import com.storytron.swat.util.DeleteButton;
import com.storytron.swat.util.EditorListener;
import com.storytron.swat.util.ErrorPopup;
import com.storytron.swat.util.FilterBox;
import com.storytron.swat.util.MaxLengthDocument;
import com.storytron.swat.util.UndoableAction;
import com.storytron.swat.verbeditor.OperatorMenu.NonOverlappedPopupMenu;
import com.storytron.uber.Deikto;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.operator.CustomOperator;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;

/** 
 * An editor for creating, deleting and modifying custom
 * operators. 
 * */
public final class OperatorEditor extends JDialog {
	private static final long serialVersionUID = 1L;
	private Swat.TextField operatorNameField = new Swat.TextField();
	public ScriptEditor<Script> scriptEditor;
	private ParameterListEditor parameterList;
	private CustomOperator mOperator;
	private FilterBox filterBox;
	private Swat.TextArea descriptionText = new Swat.TextArea();
	private JButton deleteOperatorButton = new DeleteButton();
	private JButton addOperatorButton = new AddButton();
	private final TreeSet<CustomOperator> operators = new TreeSet<CustomOperator>();
	private ErrorPopup errorPopup=new ErrorPopup();
	
	public OperatorEditor(final Swat swat){
		super(swat.getMyFrame(),"Operator editor");

		initWidgets(swat);
		setPanels();
	}

	private void setAddOperatorButtonTooltip(){
		if (addOperatorButton.isEnabled())
			addOperatorButton.setToolTipText("Creates a custom operator.");
		else 
			addOperatorButton.setToolTipText("There too many custom operators in this storyworld.");
	}

	private void setDeleteOperatorButtonTooltip(){
		if (deleteOperatorButton.isEnabled())
			deleteOperatorButton.setToolTipText("Deletes the custom operator.");
		else 
			deleteOperatorButton.setToolTipText("Select first a custom operator to delete.");
	}

	public void init(Deikto dk){ 
		scriptEditor.init(dk);
		for(Operator op:dk.getOperatorDictionary().getOperators(OperatorDictionary.Menu.Custom))
			operators.add((CustomOperator)op);
		filterBox.refresh();
		setCustomOperator((CustomOperator)filterBox.getSelected());
	}

	/** Layouts widgets. */
	private void setPanels(){
		//scriptEditor.setPreferredSize(new Dimension(500,700));
		
		JScrollPane parameterScroll = new JScrollPane(parameterList);
		parameterScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE,300));
		parameterScroll.setPreferredSize(new Dimension(50,250));
		parameterScroll.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		parameterScroll.setOpaque(false);
		parameterScroll.getViewport().setOpaque(false);

		JComponent parameterPanel = Box.createHorizontalBox();
		parameterPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 5, 5));
		parameterList.addParameterButton.setAlignmentY(0.0f);
		parameterPanel.add(parameterList.addParameterButton);
		parameterScroll.setAlignmentY(0.0f);
		parameterPanel.add(parameterScroll);
		
		descriptionText.setLineWrap(true);
		descriptionText.setWrapStyleWord(true);

		JComponent descriptionPanel = Box.createHorizontalBox();
		descriptionPanel.setBorder(BorderFactory.createTitledBorder("Description"));
		descriptionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));
		descriptionPanel.setPreferredSize(new Dimension(50,120));
		descriptionPanel.add(new JScrollPane(descriptionText,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		
		JComponent operatorContentsPanel = Box.createVerticalBox();
		operatorContentsPanel.add(parameterPanel);
		operatorContentsPanel.add(descriptionPanel);
		
		filterBox.setBorder(BorderFactory.createEmptyBorder(0,0,0,5));
		filterBox.setPreferredSize(new Dimension(20,250));
		filterBox.setListCellRenderer(new DefaultListCellRenderer(){
			private static final long serialVersionUID = 1L;
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected,cellHasFocus);
				c.setForeground(Operator.getColor(((Operator)value).getDataType()));
				return c;
			}
		});


		operatorNameField.setFont(new Font(operatorNameField.getFont().getName(),Font.BOLD,operatorNameField.getFont().getSize()));
		operatorNameField.setMaximumSize(new Dimension(160,deleteOperatorButton.getPreferredSize().height));
		operatorNameField.setPreferredSize(new Dimension(160,deleteOperatorButton.getPreferredSize().height));
		operatorNameField.setEnabled(false);

		deleteOperatorButton.setEnabled(false);
		setDeleteOperatorButtonTooltip();

		JComponent operatorNamePanel = Box.createHorizontalBox();
		operatorNamePanel.add(operatorNameField);
		operatorNamePanel.add(addOperatorButton);
		operatorNamePanel.add(deleteOperatorButton);
		
		JComponent operatorPanel = new ComponentLabeledPanel(operatorNamePanel);
		operatorPanel.setBorder(BorderFactory.createEmptyBorder(5,0,5,5));
		operatorPanel.setOpaque(false);
		operatorPanel.add(operatorContentsPanel);
		
		JComponent leftPanel = new JPanel(new BorderLayout());
		leftPanel.setOpaque(false);
		leftPanel.setPreferredSize(new Dimension(275,700));
		leftPanel.setMaximumSize(new Dimension(275,Integer.MAX_VALUE));
		leftPanel.add(filterBox,BorderLayout.CENTER);
		leftPanel.add(operatorPanel,BorderLayout.SOUTH);
		
		JComponent scriptPanel=new JPanel(new BorderLayout());
		scriptPanel.setOpaque(false);
		scriptPanel.add(scriptEditor.mainMenuPanel,BorderLayout.EAST);
		scriptPanel.add(scriptEditor.scriptPanel,BorderLayout.CENTER);

		JComponent mainPanel = Box.createHorizontalBox();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		mainPanel.add(leftPanel);
		mainPanel.add(scriptPanel);
		
		setBackground(Utils.lightlightBackground);
		getContentPane().setBackground(Utils.lightlightBackground);
		getContentPane().add(mainPanel);
	}
	
	/** Instantiates widgets. */
	private void initWidgets(final Swat swat){
		scriptEditor = new ScriptEditor<Script>(swat,OperatorDictionary.ScriptMenus){
			private static final long serialVersionUID = 1L;
			@Override
			protected Script getContainerState() {
				return scriptEditor.getScript();
			}
			@Override
			protected void setContainerState(Script state) {
				setCustomOperator(state.getCustomOperator());
				swat.showOperatorEditor();
			}
		};
		
		parameterList = new ParameterListEditor(scriptEditor);

		descriptionText.setEnabled(false);
		new EditorListener(descriptionText){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean timedActionPerformed(ActionEvent e) {
				setOperatorDescription(descriptionText.getText().trim());
				return true;
			}
			@Override
			public String getText() {
				if (mOperator==null)
					return descriptionText.getJTextComponent().getText();
				else
					return mOperator.getToolTipText(); 
			}
		};

		filterBox = new FilterBox(operators){
			private static final long serialVersionUID = 0L;
			@Override
	    	public String getItemToolTipText(Object o) {
	    		if (o!=null) {
	    			return Utils.breakStringHtml(((Operator)o).getMyToolTipText());
	    		} else
	    			return null;
	    	}
		};
		filterBox.setTextFieldTooltip("Type text here if you want filter elements in the list below.");
		filterBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				setCustomOperator((CustomOperator)filterBox.getSelected());
			}
		});

		operatorNameField.setDocument(new MaxLengthDocument(Deikto.MAXIMUM_FIELD_LENGTH));
		operatorNameField.addActionListener(new EditorListener(operatorNameField){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean timedActionPerformed(ActionEvent e) {
				if (mOperator!=null)
					return renameOperator(operatorNameField.getText().trim());
				else
					return true;
			}
			@Override
			public String getText() { 
				if (mOperator!=null)
					return mOperator.getLabel();
				else
					return null;
			}
		});
		deleteOperatorButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				deleteOperator();
			}
		});

		final NonOverlappedPopupMenu addOperatorPopup = new NonOverlappedPopupMenu(addOperatorButton);
		for(final Operator.Type t:Operator.Type.values())
			if (t!=Operator.Type.UnType && t!=Operator.Type.Procedure && !t.name().contains("Socket"))
				addOperatorPopup.add(new AbstractAction("Create new "+t.name()+" operator"){
					private static final long serialVersionUID = 1L;
					public void actionPerformed(ActionEvent e) { createNewOperator(t); }
				});
		addOperatorButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				addOperatorPopup.showPopup();				
			}
		});
		setAddOperatorButtonTooltip();
	}
	
	/** Tells if an operator with a given name exists. */
	private String existOperator(String name,OperatorDictionary d){
		for(Operator op:d) {
			if (op.getLabel().equals(name))
				return "An operator with name "+name+" already exists.";
		}
		return null;
	}

	/** Sets the operator being displayed. */
	public void setCustomOperator(CustomOperator op){
		if (mOperator==op)
			return;
		
		mOperator = op;
		operatorNameField.setEnabled(mOperator!=null);
		descriptionText.setEnabled(mOperator!=null);
		deleteOperatorButton.setEnabled(mOperator!=null);
		setDeleteOperatorButtonTooltip();
		parameterList.setCustomOperator(op);

		if (mOperator==null) {
			scriptEditor.setScriptPath(null,null);
			operatorNameField.setText("");
			descriptionText.setText("");
		} else {
			scriptEditor.setScriptPath(new ScriptPath(null,null,null),op.getBody());
			operatorNameField.setText(op.getLabel());
			operatorNameField.setForeground(Operator.getColor(op.getDataType()));
			descriptionText.setText(op.getToolTipText());
			filterBox.setSelected(op);
		}
	}
	
	private void setOperatorDescription(final String newDesc) {
		if (mOperator==null || newDesc==null 
				|| newDesc.length()==0 && mOperator.getToolTipText()==null
				|| newDesc.equals(mOperator.getToolTipText()))
			return;
		
		final CustomOperator op = mOperator;
		final String oldDesc = op.getMyToolTipText();
		op.setToolTipText(newDesc);
		new UndoableAction(scriptEditor.swat,false,"edit description of "+op.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			public void myRedo() {
				setCustomOperator(op);
				op.setToolTipText(newDesc);
				descriptionText.setText(newDesc);
				scriptEditor.swat.showOperatorEditor();
			}
			@Override
			public void myUndo() {
				setCustomOperator(op);
				op.setToolTipText(oldDesc);
				descriptionText.setText(oldDesc);
				scriptEditor.swat.showOperatorEditor();
			}
		};

	}
	
	private void createNewOperator(Operator.Type t){
		String name = "new"+t.name()+"Operator";
		int i=0;
		while (null!=existOperator(name,scriptEditor.swat.dk.getOperatorDictionary())) {
			i++;
			name = "new"+t.name()+"Operator"+i;
		}
		CustomOperator op = scriptEditor.swat.dk.createCustomOperator(t,name);
		addOperator(op); 
		
		final CustomOperator fop = op;
		new UndoableAction(scriptEditor.swat,false,"create new operator"){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				scriptEditor.swat.dk.getOperatorDictionary().addOperator(fop);
				addOperator(fop);
				scriptEditor.swat.showOperatorEditor();
			}
			@Override
			protected void myUndo() {
				deleteOperator(fop);
				scriptEditor.swat.showOperatorEditor();
			}
		};
	}
	
	private void deleteOperator(){
		final CustomOperator fop = mOperator;
		final LinkedList<Triplet<Script.Node,Script.Node,Integer>> opCalls = new LinkedList<Triplet<Script.Node,Script.Node,Integer>>();
		scriptEditor.swat.dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, com.storytron.uber.Script.Node n) {
				if (n.getOperator()==fop)
					opCalls.add(new Triplet<Script.Node,Script.Node,Integer>((Script.Node)n.getParent(),n,n.getParent().getIndex(n)));
				return true;
			}
		});

		deleteOperator(mOperator);
		scriptEditor.swat.verbEditor.scriptEditor.refresh();
		
		new UndoableAction(scriptEditor.swat,false,"delete operator "+fop.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				deleteOperator(fop);
				scriptEditor.swat.showOperatorEditor();
				scriptEditor.swat.verbEditor.scriptEditor.refresh();
			}
			@Override
			protected void myUndo() {
				scriptEditor.swat.dk.getOperatorDictionary().addOperator(fop);
				addOperator(fop);
				for(Triplet<Script.Node,Script.Node,Integer> t:opCalls) {
					t.first.remove(t.third);
					t.first.insert(t.second, t.third);
				}
				scriptEditor.swat.showOperatorEditor();
				scriptEditor.swat.verbEditor.scriptEditor.refresh();
			}
		};
		
	}
	
	private void addOperator(CustomOperator op){
		operators.add(op);
		setCustomOperator(op);
		filterBox.refresh();
		filterBox.setSelected(op);
		operatorNameField.selectAll();
		operatorNameField.requestFocusInWindow();
		addOperatorButton.setEnabled(operators.size()<scriptEditor.swat.dk.limits.maximumCustomOperatorCount);
		setAddOperatorButtonTooltip();
	}
	
	private void deleteOperator(CustomOperator op){
		scriptEditor.swat.dk.deleteCustomOperator(op);
		operators.remove(op);
		filterBox.refresh();
		addOperatorButton.setEnabled(true);
		setAddOperatorButtonTooltip();
		setCustomOperator((CustomOperator)filterBox.getSelected());
	}
	
	private boolean renameOperator(final String newText){
		if (mOperator==null || newText==null || mOperator.getLabel().equals((newText)))
			return true;
		
		final String error = existOperator(newText, scriptEditor.swat.dk.getOperatorDictionary()); 
		if (error!=null) {
			if (SwingUtilities.getWindowAncestor(operatorNameField)!=null) 
				errorPopup.showError(scriptEditor.swat.getMyFrame(),operatorNameField.getLocationOnScreen(),error);
			return false;
		}
		
		final CustomOperator op = mOperator;
		final String oldText = mOperator.getLabel(); 
		renameOperator(op,newText);
		
		new UndoableAction(scriptEditor.swat,false,"rename operator to "+newText){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				renameOperator(op,newText);
				setCustomOperator(op);
				scriptEditor.swat.showOperatorEditor();
				operatorNameField.selectAll();
				operatorNameField.requestFocusInWindow();
			}
			@Override
			protected void myUndo() {
				renameOperator(op,oldText);
				setCustomOperator(op);
				scriptEditor.swat.showOperatorEditor();
				operatorNameField.selectAll();
				operatorNameField.requestFocusInWindow();
			}
		};
		
		return true;
	}

	private void renameOperator(CustomOperator op,String newText) {
		if (!operatorNameField.getText().equals(newText))
			operatorNameField.setText(newText);
		operators.remove(op);
		scriptEditor.swat.dk.getOperatorDictionary().renameOperator(op.getLabel(), newText);
		mOperator.setLabel(newText);
		mOperator.getBody().getRoot().getOperator().setArgumentLabel(0,newText);
		operators.add(op);
		scriptEditor.repaintScript();
		filterBox.refresh();
		scriptEditor.swat.verbEditor.scriptEditor.refresh();
	}
	
	/** A class for testing the operator editor. */
	public static abstract class Test {
	
		public static void createNewOperator(OperatorEditor oe,Operator.Type t){
			oe.createNewOperator(t);
		}
		public static void renameOperator(OperatorEditor oe,String newName){
			oe.operatorNameField.setText(newName);
			oe.renameOperator(newName);
		}
		public static void deleteOperator(OperatorEditor oe){
			oe.deleteOperator();
		}
		
		public static void createNewParameter(OperatorEditor oe,Operator.Type t){
			ParameterListEditor.Test.createNewParameter(oe.parameterList,t);
		}
		public static void renameParameter(OperatorEditor oe,int iArg,String newName){
			ParameterListEditor.Test.renameParameter(oe.parameterList,iArg,newName);
		}
		public static void deleteParameter(OperatorEditor oe,int iArg){
			ParameterListEditor.Test.deleteParameter(oe.parameterList,iArg);
		}

		public static void setOperatorDescription(OperatorEditor oe,String newDesc){
			oe.setOperatorDescription(newDesc);
		}
		
		public static CustomOperator getOperator(OperatorEditor oe){
			return oe.mOperator;
		};

	}
	
}
