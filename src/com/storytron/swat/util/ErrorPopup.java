package com.storytron.swat.util;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.PopupFactory;

import com.storytron.enginecommon.Utils;

/** An implementation of an error popup. The popup will leave upon 
 * any kind of input.
 */
public class ErrorPopup extends JPanel implements AWTEventListener {
	static final long serialVersionUID=0;

	/** Shows a popup message with the bottom left corner at 
	 * p (screen coordinates).  
	 */
	public void showError(Component owner,Point p,String error){			
		errorLabel.setText(Utils.toHtmlTooltipFormat(error));
		showPopup(owner,p);
	}

	private JLabel errorLabel = new JLabel();
	public ErrorPopup(){
		super(new FlowLayout(FlowLayout.CENTER,2,2));
		add(errorLabel);
		setBorder(BorderFactory.createLineBorder(Color.black));
		setBackground(new Color(255,255,225));	
	}
	javax.swing.Popup popupList=null;
	private void showPopup(Component owner,Point p) {
		if (popupList!=null) return;
		
		Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.MOUSE_EVENT_MASK|AWTEvent.KEY_EVENT_MASK);
		
		popupList=PopupFactory.getSharedInstance().getPopup(owner, this, p.x,p.y-getPreferredSize().height);
		popupList.show();
	} 
	private void hidePopup() {
		if (popupList==null) return;
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
		popupList.hide();
		popupList=null;		
	}
	
	public void eventDispatched(AWTEvent event) {
		if (event.getID()==MouseEvent.MOUSE_PRESSED || event.getID()==KeyEvent.KEY_PRESSED)
			hidePopup();
	}
}