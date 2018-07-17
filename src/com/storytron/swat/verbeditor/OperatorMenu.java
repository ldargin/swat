package com.storytron.swat.verbeditor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.storytron.swat.util.PopupButton;
import com.storytron.uber.operator.OperatorDictionary;

/**
 * <p>An OperatorMenu is a menu containing operators. It is used in script 
 * editing to insert, substitute, or replace an operator. The operators that
 * fill the menu are queried from the {@link OperatorDictionary}.
 * </p> 
 * <p>
 * OperatorMenus show up in {@link VerbEditor#mainMenuPanel}.
 * </p>
 */
public final class OperatorMenu {
	
	private static final long serialVersionUID = 1L;
	private static VerbEditor ve; // used to refer back to VerbEditor
	private JButton myButton; // the button that causes this menu to pop up.
	private NonOverlappedPopupMenu myPopup; // the popup Menu
	private OperatorDictionary.Menu menu;
	private Iterable<ScriptEditor<?>.OperatorAction> menuActions;
	
	void showPopup(){
    	myPopup.showPopup();
   		ve.scriptEditor.requestFocusInWindow();
	}
//----------------------------------------------------------------------
	class PopupListener extends MouseAdapter {
	    public void mousePressed(MouseEvent e) {
	    	if (myButton.isEnabled()) showPopup();
	    }
	}
// ----------------------------------------------------------------------
	OperatorMenu(OperatorDictionary.Menu m) {
		menu=m;
		myButton = new PopupButton(m.name());
		myButton.setPreferredSize(new Dimension(150,30));
		
		myButton.addMouseListener(new PopupListener());
		myPopup = new NonOverlappedPopupMenu(myButton);
		myPopup.addPopupMenuListener(new PopupMenuListener(){
			public void popupMenuCanceled(PopupMenuEvent e) {
				myButton.getModel().setPressed(false);
			}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				myButton.getModel().setPressed(false);
			}
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				myButton.getModel().setPressed(true);
			}
			
		});
		myButton.setEnabled(false);
	}

	
	/** <p>Sets the actions for this menu. If the argument is null or empty
	 * the menu is disabled. </p> 
	 * */
	public void setMenuActions(Iterable<ScriptEditor<?>.OperatorAction> menuActions) {
		this.menuActions = menuActions;
		myPopup.removeAll();

		if (menuActions==null || !menuActions.iterator().hasNext()) {
			myButton.setEnabled(false);				
			return;
		}
		for (ScriptEditor<?>.OperatorAction opAction: menuActions){
			JMenuItem mi = new JMenuItem(opAction);
			opAction.install(mi);
			myPopup.add(mi);
		}
		if (myPopup.isVisible()) {
			showPopup();
			myPopup.pack();
		}
		myButton.setEnabled(true);
	}
	
	/** Returns the actions displayed by this menu.  */
	public Iterable<ScriptEditor<?>.OperatorAction> getMenuActions() { 
		return menuActions; 
	}

//----------------------------------------------------------------------
	static public void init(VerbEditor tve) { ve = tve;	}
	public JComponent getMyButton() { return myButton;	}
	public OperatorDictionary.Menu getMenu() { return menu; }
	
	public static class NonOverlappedPopupMenu extends JPopupMenu {
		private static final long serialVersionUID = 1L;
		private Component owner;
		public NonOverlappedPopupMenu(Component owner){
			this.owner=owner;
		}
		
		public void showPopup(){
			if (isVisible()) return;
			
			Dimension d=getPreferredSize();
	    	if (Toolkit.getDefaultToolkit().getScreenSize().height<=
	    			d.height+owner.getLocationOnScreen().y+owner.getHeight())
	    		if (Toolkit.getDefaultToolkit().getScreenSize().width<=
	    			d.width+owner.getLocationOnScreen().x+owner.getWidth())
	    			show(owner, -d.width,0);
	    		else show(owner, owner.getWidth(),0);
	    	else show(owner, 0,owner.getHeight());
		}
	}
}
