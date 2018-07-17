package com.storytron.swat.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

/** A component that displays an horizontal line. */
public final class LineBox extends JPanel {
	private static final long serialVersionUID = 1L;	
	public LineBox(int stroke,Color color){
		super(new BorderLayout());
		setOpaque(false);
		add(Box.createHorizontalGlue());
		setMaximumSize(new Dimension(10000,3));
		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0,stroke,0,stroke),
				BorderFactory.createMatteBorder(0,0,1,0,color)));
	}
}
