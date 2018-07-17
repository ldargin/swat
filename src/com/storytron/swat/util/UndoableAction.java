package com.storytron.swat.util;

import javax.swing.undo.AbstractUndoableEdit;

import com.storytron.swat.Swat;
import com.storytron.swat.Swat.EditorEnum;

/**
 * <p>This class is for declaring undoable actions.
 * Instances of this class automatically registers themselves
 * into the {@link javax.swing.undo.UndoManager} when instantiated.</p>
 * 
 * <p>Use this class in this way:
 * <pre>
  final Type1 value1 = ... ;
  final Type2 value2 = ... ;
  ...
  new UndoableAction(swat,"name of the operation") {
 	\@Override
  	public void myRedo(){ redoing code goes here }
 	\@Override
 	public void myUndo(){ undoing code goes here }   
  };
  </pre> 
 * The final values declared before the instantiation
 * are the values you want the redo and undo code to reference.
 * Keep in mind that redo or undo may be called at another time
 * when the environment has changed, so explictly declaring
 * the constant values this redo & undo will use is needed.</p>
 * 
 * <p>For example, in verb editor you will want to declare:
 *   <blockquote><code>final Verb verb = ... ;</code></blockquote>
 *  and use verb in the redo & undo code instead of mVerb or getVerb().
 *   Why?
 *   Because you want undo & redo to reference the verb that is
 *   currently selected, not the one that will be selected when the user
 *   decides to undo the action.</p> 
 *   
 * <p>Also keep in mind that this constructor calls automatically the redo
 * method. If you whish to avoid the excecution of redo call the constructor
 * like this;
 *   <pre>
  new UndoableAction(swat,false,"name of the operation") {
 	\@Override
  	public void myRedo(){ redoing code goes here }
 	\@Override
 	public void myUndo(){ undoing code goes here }   
 	\@Override
 	public void update(){ update the GUI state, called after either redo or undo }   
  };
  </pre>
 * </p>
 * */
public abstract class UndoableAction extends AbstractUndoableEdit {
	private static final long serialVersionUID = 0L;

	/**
	 * Override these methods to implement the undo & redo operations. 
	 * */
	protected void myRedo() { doOperation(true); };
	protected void myUndo() { doOperation(false); };
	protected void doOperation(boolean redo) {};
	protected void update() {};
	
	public UndoableAction(Swat swat,boolean runRedo,String presentationName){
		this.presentationName = presentationName;
		this.swat = swat;
		editorInFocus = swat.getEditorInFocus();
		if (runRedo) myRedo();
		swat.getUndoManager().addEdit(this);
	}

	public UndoableAction(Swat swat,String presentationName){
		this(swat,true,presentationName);
	};

	@Override
	public final void redo(){
		swat.setEditorInFocus(editorInFocus);
		myRedo();
		update();
	}
	@Override
	public final void undo(){
		swat.setEditorInFocus(editorInFocus);
		myUndo();			
		update();
	}
	private EditorEnum editorInFocus; 
	private Swat swat; 

	@Override
	public boolean canRedo() {
		return true;
	}
	@Override
	public boolean canUndo() {
		return true;
	}
	private String presentationName;
	@Override
	public String getPresentationName(){
		return presentationName;
	}
	/**
	 * Override this to change the default undo description.  
	 */
	@Override
	public String getUndoPresentationName(){
		return "Undo "+presentationName;
	}
	/**
	 * Override this to change the default redo description.  
	 */
	@Override
	public String getRedoPresentationName(){
		return "Redo "+presentationName;
	}
}