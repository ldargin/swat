package com.storytron.swat.verbeditor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.storytron.enginecommon.Pair;
import com.storytron.swat.Swat;
import com.storytron.swat.util.AddButton;
import com.storytron.swat.util.DeleteButton;
import com.storytron.swat.util.EditorListener;
import com.storytron.swat.util.ErrorPopup;
import com.storytron.swat.util.MaxLengthDocument;
import com.storytron.swat.util.UndoableAction;
import com.storytron.swat.verbeditor.OperatorMenu.NonOverlappedPopupMenu;
import com.storytron.uber.Deikto;
import com.storytron.uber.Script;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.CustomOperator;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.OperatorDictionary;
import com.storytron.uber.operator.ParameterOperator;

/** 
 * And editor for a list of operator parameters. 
 * Allows renaming and deleting parameters.
 * <p>
 * The GUI for adding new parameters must be implemented
 * externally and communicate the actions to this class
 * through {@link #createNewParameter(com.storytron.uber.operator.Operator.Type)}.
 * */
public final class ParameterListEditor extends JPanel {
	private static final long serialVersionUID = 1L;
	private ArrayList<Swat.TextField> names = new ArrayList<Swat.TextField>();
	private ScriptEditor<?> scriptEditor;
	private CustomOperator mOperator;
	private ErrorPopup errorPopup=new ErrorPopup();
	private boolean parametersNormalized = false;
	public JButton addParameterButton = new AddButton();

	public ParameterListEditor(final ScriptEditor<?> scriptEditor){
		super(null);
		this.scriptEditor = scriptEditor;
		
		final NonOverlappedPopupMenu addParameterPopup = new NonOverlappedPopupMenu(addParameterButton);
		for(final Operator.Type t:Operator.Type.values())
			if (t!=Operator.Type.UnType && t!=Operator.Type.Procedure && !t.name().contains("Socket"))
				addParameterPopup.add(new AbstractAction("Add "+t.name()+" parameter"){
					private static final long serialVersionUID = 1L;
					public void actionPerformed(ActionEvent e) {
						createNewParameter(t);
					}
				});
		addParameterButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				addParameterPopup.showPopup();
			}
		});
		addParameterButton.setEnabled(false);
		setAddParameterButtonTooltip();
		
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setOpaque(false);
	}
	
	private void setAddParameterButtonTooltip(){
		if (addParameterButton.isEnabled())
			addParameterButton.setToolTipText("Adds a new parameter to the current operator.");
		else if (mOperator==null)
			addParameterButton.setToolTipText("Create or select an operator before adding parameters.");
		else
			addParameterButton.setToolTipText("There too many parameters.");
	}
	
	/** Creates a new parameter. */
	private void createNewParameter(final Operator.Type t) {
		final CustomOperator fop = mOperator;
		normalizeParameterReferences();

		String newName = t.name();
		{
			int i = 0;
			while(i<100 && existsParameter(newName)!=null) {
				i++;
				newName = t.name() + i;
			}
		}
		final String paramName = newName;
		mOperator.addArgument(t,paramName, "");
		final ParameterOperator param = mOperator.getParameterOperator(mOperator.getCArguments()-1);
		mCreateNewParameter(param);
		
		new UndoableAction(scriptEditor.swat,false,"add parameter to "+mOperator.getLabel()){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				setCustomOperator(fop);
				mOperator.addArgument(param,mOperator.getCArguments(),t,paramName, "");
				mCreateNewParameter(param);
				scriptEditor.swat.showOperatorEditor();
				scriptEditor.swat.verbEditor.scriptEditor.refresh();
			}
			@Override
			protected void myUndo() {
				setCustomOperator(fop);
				mDeleteParameter(fop.getCArguments()-1);
				scriptEditor.swat.showOperatorEditor();
				scriptEditor.swat.verbEditor.scriptEditor.refresh();
			}
		};
	}
	
	private void addParameter(ParameterOperator param){
		scriptEditor.extraOperators.add(param.getParameterIndex(),param);
		mAddParameter(param);
	}
	
	private void mCreateNewParameter(final ParameterOperator param){
		addParameter(param);
		names.get(param.getParameterIndex()).selectAll();
		names.get(param.getParameterIndex()).requestFocusInWindow();
		revalidate();
		repaint();

		scriptEditor.refresh();
		addParameterButton.setEnabled(mOperator.getCArguments()<Deikto.MAXIMUM_AMOUNT_OF_PARAMETERS);
		setAddParameterButtonTooltip();
		
		scriptEditor.swat.dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, com.storytron.uber.Script.Node n) {
				if (n.getOperator()==mOperator)
					n.add(Script.createNode(OperatorDictionary.getUndefinedOperator("?"+param.getDataType().name()+"?"), 0.0f));
				return true;
			}
		});
		scriptEditor.swat.verbEditor.scriptEditor.refresh();
	}
	
	/** Tells if a parameter or operator exists with the given name. */
	private String existsParameter(String name){
		for(Operator op:scriptEditor.swat.dk.getOperatorDictionary()) {
			if (op.getLabel().equals(name))
				return "A parameter with name "+name+" already exists.";
		}

		for(int i=0;i<mOperator.getCArguments();i++) {
			if (mOperator.getArgumentLabel(i).equals(name))
				return "A parameter with name "+name+" already exists.";
		}
		return null;
	}
	
	private void mAddParameter(ParameterOperator param){
		final Swat.TextField name = new Swat.TextField();
		name.setDocument(new MaxLengthDocument(Deikto.MAXIMUM_FIELD_LENGTH));
		name.setText(param.getLabel());
		name.setForeground(Operator.getColor(param.getDataType()));
		name.setFont(new Font(name.getFont().getName(),Font.BOLD,name.getFont().getSize()));
		names.add(param.getParameterIndex(),name);
		name.addActionListener(new EditorListener(name){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean timedActionPerformed(ActionEvent e) {
				final int i = names.indexOf(name);
				if (0<=i && i<scriptEditor.extraOperators.size())
					return renameParameter(i,names.get(i).getText().trim());
				else
					return true;
			}
			@Override
			public String getText() {
				int i = names.indexOf(name);
				if (i==-1)
					return name.getJTextComponent().getText();
				else
					return scriptEditor.extraOperators.get(i).getLabel();
			}
		});
		
		JButton deleteButton = new DeleteButton();
		deleteButton.setToolTipText("Deletes this parameter.");
		
		JComponent row = Box.createHorizontalBox();
		row.add(name);
		row.add(deleteButton);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE,deleteButton.getPreferredSize().height));

		add(row,param.getParameterIndex());

		deleteButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				deleteParameter(names.indexOf(name));
			}
		});
	}
	
	/** Deletes parameter at position i. */
	private void deleteParameter(final int i){
		normalizeParameterReferences();
		
		final LinkedList<Pair<Script.Node,Script.Node>> opCalls = new LinkedList<Pair<Script.Node,Script.Node>>(); 
		scriptEditor.swat.dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, com.storytron.uber.Script.Node n) {
				if (n.getOperator()==mOperator)
					opCalls.add(new Pair<Script.Node, Script.Node>(n,(Script.Node)n.getChildAt(i)));
				return true;
			}
		});
		
		final ParameterOperator param = mOperator.getParameterOperator(i);
		final Operator.Type paramType = mOperator.getArgumentDataType(i);
		final String paramName = mOperator.getArgumentLabel(i);
		
		final LinkedList<Pair<Script.Node,Integer>> parameterRefs = new LinkedList<Pair<Script.Node,Integer>>();
		scriptEditor.getScript().traverse(new Script.NodeTraverser(){
			public boolean traversing(Script s, com.storytron.uber.Script.Node n) {
				if (n.getOperator()==param)
					parameterRefs.add(new Pair<Script.Node,Integer>((Script.Node)n.getParent(),n.getParent().getIndex(n)));
				return true;
			}
		});
		
		mDeleteParameter(i);
		
		new UndoableAction(scriptEditor.swat,false,"delete parameter "+paramName){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				mDeleteParameter(i);
				scriptEditor.swat.showOperatorEditor();
				scriptEditor.swat.verbEditor.scriptEditor.refresh();
			}
			@Override
			protected void myUndo() {
				mOperator.addArgument(param,i,paramType,paramName, "");
				addParameter(param);
				for(Pair<Script.Node,Script.Node> p:opCalls)
					p.first.insert(p.second, i);
				for(Pair<Script.Node,Integer> p:parameterRefs)
					((Node)p.first.getChildAt(p.second)).setOperator(param);
				scriptEditor.refresh();
				scriptEditor.swat.showOperatorEditor();
				scriptEditor.swat.verbEditor.scriptEditor.refresh();
			}
		};
	}
	
	/** Deletes parameter at position i. */
	private void mDeleteParameter(final int i){
		scriptEditor.getScript().traverse(new Script.NodeTraverser(){
			public boolean traversing(Script s, com.storytron.uber.Script.Node n) {
				if (n.getOperator()==scriptEditor.extraOperators.get(i)) {
					Node newNode = s.deleteNode(scriptEditor.swat.dk, (Script.Node)n);
					if (n==scriptEditor.getSelectedNode())
						scriptEditor.setSelectedNode(newNode);
				}
				return true;
			}
		});
		
		mOperator.removeArgument(i);
		scriptEditor.extraOperators.remove(i);
		for(int j=i;j<scriptEditor.extraOperators.size();j++)
			((ParameterOperator)scriptEditor.extraOperators.get(j)).setParameterIndex(j);
		addParameterButton.setEnabled(true);
		setAddParameterButtonTooltip();
		
		
		scriptEditor.refresh();
		names.remove(i);
		remove(i);
		revalidate();
		repaint();
		
		scriptEditor.swat.dk.traverseScriptsJustOnce(new Script.NodeTraverser(){
			public boolean traversing(Script s, com.storytron.uber.Script.Node n) {
				if (n.getOperator()==mOperator) {
					s.deleteNode(scriptEditor.swat.dk, (Script.Node)n.getChildAt(i));
					n.remove(i);
				}
				return true;
			}
		});
		scriptEditor.swat.verbEditor.scriptEditor.refresh();
	}
	
	/** Sets the custom operator whose parameters should be displayed. */
	public void setCustomOperator(CustomOperator op){
		if (op==mOperator)
			return;
		
		parametersNormalized = false;
		mOperator = op;
		addParameterButton.setEnabled(mOperator!=null && mOperator.getCArguments()<Deikto.MAXIMUM_AMOUNT_OF_PARAMETERS);
		setAddParameterButtonTooltip();
		scriptEditor.extraOperators.clear();
		names.clear();
		removeAll();
		if (op!=null) {
			for(int i=0;i<mOperator.getCArguments();i++) {
				ParameterOperator param = mOperator.getParameterOperator(i);
				mAddParameter(param);
				scriptEditor.extraOperators.add(param);
			}
		}
		scriptEditor.refresh();
		revalidate();
		repaint();
	}
	
	private boolean renameParameter(final int i,final String newText){
		if (mOperator==null || scriptEditor.extraOperators.get(i).getLabel().equals(newText))
			return true;
		
		final String error = existsParameter(newText);
		if (error!=null) {
			if (SwingUtilities.getWindowAncestor(names.get(i))!=null) 
				errorPopup.showError(scriptEditor.swat.getMyFrame(),names.get(i).getLocationOnScreen(),error);
			return false;
		}
		
		final CustomOperator fop = mOperator;
		final String oldText = scriptEditor.extraOperators.get(i).getLabel();
		normalizeParameterReferences();
		mRenameParameter((ParameterOperator)scriptEditor.extraOperators.get(i),newText);
		scriptEditor.swat.verbEditor.scriptEditor.refresh();
		
		new UndoableAction(scriptEditor.swat,false,"rename parameter to "+newText){
			private static final long serialVersionUID = 1L;
			@Override
			protected void myRedo() {
				setCustomOperator(fop);
				mRenameParameter((ParameterOperator)scriptEditor.extraOperators.get(i),newText);
				scriptEditor.swat.showOperatorEditor();
				names.get(i).selectAll();
				names.get(i).requestFocusInWindow();
				scriptEditor.swat.verbEditor.scriptEditor.refresh();
			}
			@Override
			protected void myUndo() {
				setCustomOperator(fop);
				mRenameParameter((ParameterOperator)scriptEditor.extraOperators.get(i),oldText);
				scriptEditor.swat.showOperatorEditor();
				names.get(i).selectAll();
				names.get(i).requestFocusInWindow();
				scriptEditor.swat.verbEditor.scriptEditor.refresh();
			}
		};
		return true;
	}
	
	private void mRenameParameter(ParameterOperator param,String newText){
		mOperator.setArgumentLabel(param.getParameterIndex(),newText);
		param.setLabel(newText);
		names.get(param.getParameterIndex()).setText(newText);
		scriptEditor.refresh();
	}
	
	/** 
	 * The script might have multiple instances of ParameterOperator which really
	 * refer to the same parameter. This method traverses the script
	 * assigning a unique instance of a parameter operator to all the nodes that
	 * use it. 
	 * */
	private void normalizeParameterReferences(){
		if (parametersNormalized)
			return;
		
		parametersNormalized = true;
		scriptEditor.getScript().traverse(new Script.NodeTraverser(){
			public boolean traversing(Script s, Node n) {
				if (n.getOperator() instanceof ParameterOperator)
					n.setOperator(scriptEditor.extraOperators.get(((ParameterOperator)n.getOperator()).getParameterIndex()));
				return true;
			}
		});
	}
	
	/** A class for testing the operator editor. */
	public static abstract class Test {
	
		public static void createNewParameter(ParameterListEditor ple,Operator.Type t){
			ple.createNewParameter(t);
		}

		public static void renameParameter(ParameterListEditor ple,int iArg,String newName){
			ple.renameParameter(iArg,newName);
		}
		public static void deleteParameter(ParameterListEditor ple,int iArg){
			ple.deleteParameter(iArg);
		}
		
	}
}
