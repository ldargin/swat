package com.storytron.swat.util;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import com.storytron.enginecommon.Utils;

public final class DeleteButton extends JButton {
	private static final long serialVersionUID = 1L;
	private static Icon icon=new ImageIcon(Utils.getImagePath("DeleteButton.png"));
//	**********************************************************************	
	public DeleteButton(String tLabel) {
		this();
		setToolTipText("delete selected "+tLabel);
	}
	public DeleteButton() {
		setBackground(Color.red);
		setMargin(new Insets(2,2,2,2));
		setIcon(icon);
		setEnabled(true);
	}
}
