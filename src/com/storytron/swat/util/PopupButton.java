package com.storytron.swat.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Polygon;

import javax.swing.JButton;

/**
 * Buttons decorated with a black arrow pointing down. 
 * They are usually used for buttons which display a popup menu when
 * pressed. 
 * */
public class PopupButton extends JButton {
	private static final long serialVersionUID = 1L;
	private final static int arroww=7;
	private final static int arrowh=4;
	public PopupButton(String label){
		super(label);
		setMargin(new Insets(2,3,2,3+arroww));
	}
	
	@Override
	protected void paintComponent(Graphics g){
		super.paintComponent(g);			
		int h=getHeight()-arrowh-4;
		Polygon p=new Polygon();
		p.addPoint(getWidth()-arroww-4, h);
		p.addPoint(getWidth()-4, h);
		p.addPoint(getWidth()-4-arroww/2-1, h+arrowh);
		if (!isEnabled()) g.setColor(Color.lightGray);
		g.fillPolygon(p);
		g.setColor(Color.white);
		g.drawLine(getWidth()-4-1, h+1, getWidth()-4-arroww/2,h+arrowh-1);
		g.drawLine(getWidth()-4, h+1, getWidth()-4-arroww/2,h+arrowh);
	}
}
