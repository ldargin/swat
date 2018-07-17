package com.storytron.swat.util;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import com.storytron.enginecommon.Utils;

public final class AddButton extends JButton {
	private static final long serialVersionUID = 1L;
	private static Icon icon=new ImageIcon(Utils.getImagePath("AddButton.png"));
	public AddButton(String tLabel) {
		this();
		setToolTipText("adds a new "+tLabel);
	}
	public AddButton() {
		super(icon);
		setBackground(Color.green);
		setMargin(new Insets(2,2,2,2));
		setEnabled(true);
	}
}
