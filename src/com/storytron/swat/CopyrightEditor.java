package com.storytron.swat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.EditorListener;

/** An editor for copyright info. */
public abstract class CopyrightEditor extends JDialog {
	private static final long serialVersionUID = 1L;
	private JTextComponent textComponent;

	/** Called when the copyright text changes. */
	public abstract void onTextChange(String newText);

	/** 
	 * Creates a copyright editor with the given parent.
	 * @param editable tells if the text in the editor is editable. 
	 * */
	public CopyrightEditor(Frame owner,boolean editable){
		super(owner);

		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		final Swat.TextArea jta = new Swat.TextArea();
		jta.setLineWrap(true);
		jta.setWrapStyleWord(true);
		textComponent = jta;
		if (editable) {
			new EditorListener(jta){
				private static final long serialVersionUID = 1L;
				@Override
				public boolean timedActionPerformed(ActionEvent e) {
					onTextChange(jta.getText());
					return true;
				}
				@Override
				public String getText() { return null; }
			};
			getContentPane().setBackground(Utils.lightlightBackground);
			setBackground(Utils.lightlightBackground);
		} else {
			textComponent.setOpaque(false);
			getContentPane().setBackground(Utils.lightGrayBackground);
			setBackground(Utils.lightGrayBackground);
		}
		textComponent.setEditable(editable);
//			JTextPane textPane = new JTextPane();
//			textPane.setContentType("text/html");
//			textPane.setEditable(false);
//			textPane.setOpaque(false);
//			textComponent = textPane;
//			getContentPane().setBackground(Utils.lightGrayBackground);
//			setBackground(Utils.lightGrayBackground);
//		}

		textComponent.setMargin(new Insets(30,30,10,30));
		
		JScrollPane paneScrollPane = new JScrollPane(textComponent);
		paneScrollPane.setVerticalScrollBarPolicy(
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		paneScrollPane.setMinimumSize(new Dimension(30, 30));

		if (editable) {
			JLabel explanationLabel = new JLabel();
			JTextArea explanation = new JTextArea(
					"Use this notepad to include attributions and copyright"
					+" information for your storyworld and its components"
					+" (e.g., images or text you take from other sources)."
					+" HTML formatting is permitted. The text input here will"
					+" be available to players via the Storyteller menu.");
			explanation.setFont(explanationLabel.getFont());
			explanation.setWrapStyleWord(true);
			explanation.setLineWrap(true);
			explanation.setOpaque(false);
			explanation.setEditable(false);
			explanation.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
			getContentPane().add(explanation,BorderLayout.NORTH);
		}
		getContentPane().add(paneScrollPane);
		setPreferredSize(new Dimension(640, 740));
		pack();
	}
	
	/** Sets the text to display in the editor. */
	public void setText(String text){
		textComponent.setText(text);
	}
	
	public static void main(String[] args) throws Exception {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame fr = new JFrame("Copyright");
				fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				fr.setVisible(true);
				CopyrightEditor ce = new CopyrightEditor(fr,false) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onTextChange(String text) {
					}
				};
				ce.setLocationRelativeTo(fr);
				ce.setText("a sample text <b>for</b> the editor");
				ce.setVisible(true);
			}
		});
	}
}
