package com.storytron.swat.util;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Timer;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import com.storytron.swat.Swat;
import com.storytron.swat.Swat.EventInfo;
import com.storytron.swat.Swat.TextComponent;

/**
 * <p>This class implements a timer for renaming things.
 * When the user types a text in a text editor without pressing enter
 * this timer commits the change after a while.
 * </p>
 * <p>This class also changes the foreground color of the editor
 * to signal an invalid input.</p>
 * <p>
 * This class must be inherited implementing the commitment in 
 * {@link #timedActionPerformed(ActionEvent)}. The implementation
 * must provide the method {@link #setText()} which serves to synchronize
 * the text in the editor with the current one in case the user
 * entered a non valid input.  
 * </p>
 * */
public abstract class EditorListener extends Timer implements 
	DocumentListener, ActionListener, KeyListener, FocusListener, AncestorListener {
	private static final long serialVersionUID = 0L;
	private final static Color ERROR_BACKGROUND_COLOR = new Color(255,222,222);
	
	/**
	 * Returns true if the input was valid, false if there was an error.
	 * This is called when:
	 * <ul>
	 *  <li>the action listeners of the editor are called</li>
	 *  <li>the editor looses focus</li>
	 *  <li>the user hits CMD-Z CMD-Y or CMD-S</li>
	 *  <li>once when the user does not enter input for at least a second</li>
	 * </ul>
	 * */
	abstract public boolean timedActionPerformed(ActionEvent e);

	/** This should return the current text in the logic model.
	 * Return null if you don't care of the model value.
	 *  */
	public abstract String getText();

	private TextComponent editor;
	private Color backgroundColor;
	protected boolean error=false;
	/**
	 * @param editor is the editor to listen to.
	 * */
	public EditorListener(TextComponent editor){
		super(1000,null);
		this.editor=editor;
		editor.getJTextComponent().addAncestorListener(this);
		addActionListener(this);
		editor.getJTextComponent().getDocument().addDocumentListener(this);
		editor.getJTextComponent().addKeyListener(this);
		editor.getJTextComponent().addFocusListener(this);
	}
	
	protected void updated() {
		if (editor.isUserInput()){
			if (error){
				editor.getJTextComponent().setBackground(backgroundColor);
				error=false;
			}
			restart();
		};
	}
	
	public void changedUpdate(DocumentEvent e) { updated();	}
	public void insertUpdate(DocumentEvent e) { updated(); }
	public void removeUpdate(DocumentEvent e) { updated(); }

	public void actionPerformed(ActionEvent e){ 
		stop();
		String s = getText();
		if (s!=null && s.equals(editor.getJTextComponent().getText())) return;
			
		int cpos=editor.getJTextComponent().getCaretPosition();
		int selStart = editor.getJTextComponent().getSelectionStart();
		if (cpos==selStart)
			selStart = editor.getJTextComponent().getSelectionEnd();
		
		if (backgroundColor==null)
			backgroundColor = editor.getJTextComponent().getBackground();
		else
			editor.getJTextComponent().setBackground(backgroundColor);
		
		if (error=!timedActionPerformed(e))
			editor.getJTextComponent().setBackground(ERROR_BACKGROUND_COLOR);

		JTextComponent tc = editor.getJTextComponent(); 
		int length = tc.getText().length();
		tc.setCaretPosition(Math.min(selStart, length));
		tc.moveCaretPosition(Math.min(cpos, length));
	};
	
	public void keyPressed(KeyEvent e) {
		if ((e.getModifiers() & Swat.keyMask)!=0 && e.getKeyCode()==KeyEvent.VK_Z){
			if (!error)	
				actionPerformed(null);
			else {// If there's an error
				// capture the Undo keystroke for showing the
				// original text. 
				e.setSource(new EventInfo(this));
				error=false;
				editor.setText(getText());
				editor.getJTextComponent().setBackground(backgroundColor);
			}						
		}
	}
	public void keyReleased(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void focusGained(FocusEvent e) {}
	public void focusLost(FocusEvent e) {
		if (error) {
			if (getText()!=null) {
				editor.getJTextComponent().setBackground(backgroundColor);
				editor.getJTextComponent().setText(getText());
			}
		} else
			actionPerformed(null);
	}

	public void ancestorAdded(AncestorEvent event) {}
	public void ancestorMoved(AncestorEvent event) {}
	public void ancestorRemoved(AncestorEvent event) {
		if (error){
			editor.setText(getText());
			editor.getJTextComponent().setBackground(backgroundColor);
			error=false;
		}
	}
}