package com.storytron.swat.verbeditor;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.verbeditor.VerbEditor.State;

/** 
 * Implements a state stack for verb editor.
 * <p>
 * This class contains actions for navigating backward and forward
 * the state stack ({@link #backwardAction} and {@link #forwardAction}).
 * These actions are enabled disabled as appropriate when there are no valid
 * states in the corresponding direction.
 * <p>
 * When editing verbs you can call {@link #pushState()} to insert new states
 * in the stack, and you can call {@link #updateStackActions()} to
 * enable/disable the navigation actions as needed.  
 * */
public final class StateStack {
	
	public final Action backwardAction = new AbstractAction(null,
						new ImageIcon(Utils.getImagePath("backward.png"))){
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent e) { stateBackward(); }
	};
	public final Action forwardAction = new AbstractAction(null,
						new ImageIcon(Utils.getImagePath("forward.png"))){
		private static final long serialVersionUID = 1L;
		public void actionPerformed(ActionEvent e) { stateForward(); }
	};
	private VerbEditor ve;
	
	/** 
	 * Creates a StateStack for the given {@link VerbEditor}.
	 * <p>
	 * It is this editor the one from whom states will be taken and set.
	 * */
	public StateStack(VerbEditor ve){
		this.ve = ve;
	}
	
	/** 
	 * stateIterator.next() is the current state
	 * stateIterator.previous() is the backward state
	 * stateIterator.next();stateIterator.next(); is the forward state
	 * */
	private ListIterator<State> stateIterator = (new LinkedList<State>()).listIterator();
	private boolean ignorePushState = true;
	
	/** Tells if the {@link #pushState()} operation must do nothing. */
	public void setIgnoringPushState(boolean ignorePushState) {
		this.ignorePushState = ignorePushState;
	}
	/** Tells if the {@link #pushState()} operation is set to do nothing. */
	public boolean isIgnoringPushState() {
		return ignorePushState;
	}

	/** 
	 * Once verb editor is started this method must be called to insert
	 * the first state in the stack.
	 * */
	public void initStates(){
		setIgnoringPushState(false);
		stateIterator.add(ve.new State());
		stateIterator.previous();
		backwardAction.setEnabled(false);
		forwardAction.setEnabled(false);
	}
	
	/** 
	 * Call this to insert the current state of verb editor in the stack.
	 * <p>
	 * This operation has no effect if {@link #isIgnoringPushState()} returns
	 * true. 
	 * */
	public void pushState(){
		if (ignorePushState) return;
		
		State currentSt=stateIterator.next();
		// If current state is compatible with the one of the stack
		// update the state on the stack.
		if (currentSt.compatibleState())
			currentSt.resetState();
		else {
			// Remove elements that are ahead in the stack.
			while (stateIterator.hasNext()){
				stateIterator.next();
				stateIterator.remove();
			}
			// Add a new state
			stateIterator.add(ve.new State());			
			backwardAction.setEnabled(true);
			forwardAction.setEnabled(false);
		}
		stateIterator.previous();
	}

	/** 
	 * After editing the verbs, call this method to enable disable the stack actions
	 * as appropriate.
	 * */
	public void updateStackActions(){
		forwardAction.setEnabled(validStateExistsForward());
		backwardAction.setEnabled(validStateExistsBackward());
	}

	/** 
	 * Moves the stack pointer a step forward.
	 * <p>
	 * The step can involve skipping many states that are
	 * invalid due to editings in the model that make them
	 * nonsense. 
	 * */
	private void stateForward(){
		setIgnoringPushState(true);
		stateIterator.next();
		while(!ve.setState(stateIterator.next()) && stateIterator.hasNext());
		stateIterator.previous();
		forwardAction.setEnabled(validStateExistsForward());
		backwardAction.setEnabled(true);
		setIgnoringPushState(false);
	}
	/** 
	 * Moves the stack pointer a step backward.
 	 * <p>
	 * The step can involve skipping many states that are
	 * invalid due to editings in the model that make them
	 * nonsense. 
	 * */
	private void stateBackward(){
		setIgnoringPushState(true);
		while(!ve.setState(stateIterator.previous()) && stateIterator.hasPrevious());
		backwardAction.setEnabled(validStateExistsBackward());
		forwardAction.setEnabled(true);
		setIgnoringPushState(false);
	}
	/** Tells if a valid state exists forward in the stack. */
	private boolean validStateExistsForward(){
		boolean exists = false;
		if (stateIterator.hasNext()) {
			int count=1;
			stateIterator.next();
			while(stateIterator.hasNext()){
				count++;
				if (ve.isChangingState(stateIterator.next())) {
					exists = true;
					break;
				}
			}
			while ( (count--) > 0 )
				stateIterator.previous();
		}
		return exists;
	}
	/** Tells if a valid state exists backward in the stack. */
	private boolean validStateExistsBackward(){
		boolean exists = false; 
		int count=0;
		while(stateIterator.hasPrevious()){
			count++;
			if (ve.isChangingState(stateIterator.previous())) {
				exists = true;
				break;
			}
		}
		while ( (count--) > 0 )
			stateIterator.next();
		
		return exists;
	}

}
