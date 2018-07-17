package com.storytron.swat.util;

import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.border.EtchedBorder;

/** A dialog for showing a progress bar */
public class ProgressDialog extends JDialog {
	public static final long serialVersionUID = 0L;
	
	private JProgressBar bar = new JProgressBar();
	
	/** Creates a non-modal progress dialog. */
	public ProgressDialog(Frame owner){
		this(owner,false);
	}
	/** Creates a progress dialog. */
	public ProgressDialog(Frame owner,boolean modal){
		super(owner,modal);
		initWidgets();
	}

	public JProgressBar getBar(){
		return bar;
	}
	
	/** Sets the status of the progress bar. */
	public void setStatus(String st){
		//status.setText(st);
		bar.setString(st);
		bar.setStringPainted(true);
	} 
	
	@Override
	public void setVisible(boolean visible){
		if (visible) {
			setLocation(getOwner().getX()+(getOwner().getWidth()-getPreferredSize().width)/2,getOwner().getY()+(getOwner().getHeight()-getHeight())/4);
			pack();
		}
		super.setVisible(visible);
	}
	
	private void initWidgets(){
		bar.setIndeterminate(true);
		bar.setMinimumSize(new Dimension(200,25));
		//bar.setPreferredSize(bar.getMinimumSize());
		//bar.setMaximumSize(bar.getMinimumSize());
		
		JComponent main = Box.createVerticalBox(); 
		main.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)
				));
		bar.setAlignmentX(0.0f);
		main.add(bar);
		//main.add(Box.createRigidArea(new Dimension(5,5)));
		
		getContentPane().add(main);
		setUndecorated(true);
		setResizable(false);
	}
}
