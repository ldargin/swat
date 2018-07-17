package com.storytron.swat.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * This is a custom class to show popups in the popup layer.
 * If the popup gets off the window it will be truncated.
 * */
public class LightweightPopup extends JPanel {
	private static final long serialVersionUID = 1L;
	private Component contents, owner;
	private JRootPane pane;

	public LightweightPopup(){
		super(new BorderLayout());
		setOpaque(false);
	}
	
	/** Show this! */
	public void setContents(Component contents) {
		this.contents = contents;			
		add(contents,BorderLayout.CENTER);
	}
	/** What did I tell you to show? */
	public Component getContents() {return contents;}

	/** 
	 * Show the popup! At position (xs[0],ys[0]) relative to the given owner.
	 * If the popup does not fit at position (xs[i],ys[i]) try position
	 * (xs[i+1],ys[i+1]). If the popup does not fit in any position use the 
	 * first one.
	 * */
	public void showPopup(Component owner,int[] xs,int[] ys){
		JRootPane pane=SwingUtilities.getRootPane(owner);
		for(int i=0;i<xs.length;i++) {
			if (fitsLayeredPane(owner, pane, xs[i], ys[i])) {
				showPopup(owner,pane,xs[i],ys[i]);
				return;
			}
		}
		showPopup(owner,pane,xs[0],ys[0]);
	}
	
	/** Show the popup! At position (x,y) relative to the given owner. */
	public void showPopup(Component owner,int x,int y){
		showPopup(owner,SwingUtilities.getRootPane(owner),x,y);
	}

	/** Show the popup! At position (x,y) relative to the given owner. */
	private void showPopup(Component owner,JRootPane pane,int x,int y){
		this.owner=owner;
		this.pane=pane;
		Point p=SwingUtilities.convertPoint(owner, x, y,pane);
		if (p.x+getWidth()>pane.getWidth())
			p.x=pane.getWidth()-getWidth();
		setLocation(p);			
		pane.getLayeredPane().add(this,JLayeredPane.POPUP_LAYER, 0);
	}

	/** Hide the popup! */
	public void hidePopup() {        
        Container parent = getParent();

        if (parent != null) {
            Rectangle bounds = getBounds();
            parent.remove(this);
            parent.repaint(bounds.x, bounds.y, bounds.width,bounds.height);
        }
    
	}
	/** Are we showing the popup now? */
	public boolean isVisible(){ return getParent()!=null; }

	/** Which is the position of the popup relative to the current owner? */
	@Override
	public Point getLocation(){			
		Point p=super.getLocation();
		SwingUtilities.convertPoint(pane, p,owner);		
		return p; 
	}
	
	/** 
	 * Tells if the popup fits in the layered pane when displayed at 
	 * position (x,y) with respect to the owner.
	 * */
	private boolean fitsLayeredPane(Component owner,JRootPane pane,int x,int y){
		Point p=SwingUtilities.convertPoint(owner, x, y,pane);
		return p.x+getWidth()<pane.getWidth() 
				&& p.y+getHeight()<pane.getHeight()
				&& 0<=p.x
				&& 0<=p.y;
	}
}
