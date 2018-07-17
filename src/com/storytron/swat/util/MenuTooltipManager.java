package com.storytron.swat.util;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

/** 
 * Custom implementation for showing tooltips.
 * <p>
 * It prevents the default tooltip position from overlapping the component,
 * but the component can override that if it provides a positions that
 * fits completely in the screen via {@link JComponent#getToolTipLocation(MouseEvent)}. 
 * */
public final class MenuTooltipManager extends MouseAdapter 
								implements MouseMotionListener, AWTEventListener {
	private static final MenuTooltipManager instance = new MenuTooltipManager();
	/** Returns the shared tooltip manager instance. */
	public static MenuTooltipManager sharedInstance(){ return instance; } 
	private MenuTooltipManager(){};
	
	private int initialDelay = 1000;
	
	private Popup tipWindow;
	private JComponent insideComponent;
	private Timer initialShowTimer;
	
	/** Shows a tooltip for a given component. */
	private void showPopup(JComponent c,MouseEvent me) {
		String tooltipText = c.getToolTipText();
		if (tooltipText==null)
			return;
		
		JToolTip tip = c.createToolTip();
		tip.setTipText(tooltipText);
		Point placement=placeTooltip(c,tip,me);
		
		tipWindow = PopupFactory.getSharedInstance().getPopup(c, tip,placement.x,placement.y);

		Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.MOUSE_EVENT_MASK|AWTEvent.KEY_EVENT_MASK);
		tipWindow.show();
	};

	/** Decides location of a tooltip. */
	private Point placeTooltip(JComponent c,JToolTip tip,MouseEvent me){
		Dimension d=tip.getPreferredSize();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize(); 

		Point loc = c.getToolTipLocation(me);
		if (loc==null)
			loc = c.getLocationOnScreen();
		else {
			SwingUtilities.convertPointToScreen(loc, c);
			if (loc.x + d.width<screen.width) {
				int diffy = loc.y+d.height-screen.height;
				if (diffy>0)
					loc.y -= diffy;
				return loc;
			} else {
				int y=loc.y;
				loc = c.getLocationOnScreen();
				loc.y = y;
			}
		}
		
		if (loc.x+c.getWidth()+d.width<screen.width)
			loc.x += c.getWidth();
		else 
			loc.x -= d.width;

		int diffy = loc.y+d.height-screen.height;
		if (diffy>0)
			loc.y -= diffy;

		return loc;
	}
	
	/** Hides the tooltip. */
	private void hidePopup() {
		if (tipWindow!=null) {
			tipWindow.hide();
			Toolkit.getDefaultToolkit().removeAWTEventListener(this);
			tipWindow = null;
		}
	};
	
	/** 
	 * Makes this tooltip manager to show tooltips for the
	 * given component.
	 * */
	public void registerComponent(JComponent c){
		ToolTipManager.sharedInstance().unregisterComponent(c);
		c.addMouseListener(this);
		c.addMouseMotionListener(this);
	}
	/** 
	 * Stops this tooltip manager from showing tooltips for the
	 * given component.
	 * */
	public void unregisterComponent(JComponent c){
		c.removeMouseListener(this);
		c.removeMouseMotionListener(this);
	}
	
	/** Clears the timers and popups. */
	private void clearTooltip() {
		if (initialShowTimer!=null) {
			initialShowTimer.stop();
			initialShowTimer = null;
		}
		hidePopup();
	};

	/** Restarts the initial show timer. */
	private void fireInitialShowTimer(final MouseEvent me) {
		clearTooltip();
		
		initialShowTimer = new Timer(initialDelay,new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				clearTooltip();
				if (insideComponent!=null && insideComponent.isShowing())
					showPopup(insideComponent,me);
				if (initialShowTimer!=null) {
					initialShowTimer.stop();
					initialShowTimer = null;
				}
			}
		});
		initialShowTimer.setRepeats(false);
		initialShowTimer.start();
	}
	
	public void mouseMoved(final MouseEvent e) {
		fireInitialShowTimer(e);
	}
	
	public void mouseDragged(MouseEvent e) {}
	
	@Override
	public void mouseEntered(MouseEvent e) {
		insideComponent = (JComponent)e.getSource();
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
		clearTooltip();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		clearTooltip();
	}
	
	public void eventDispatched(AWTEvent event) {
		if (event.getID()==MouseEvent.MOUSE_PRESSED || event.getID()==KeyEvent.KEY_PRESSED)
			hidePopup();
	}
}
