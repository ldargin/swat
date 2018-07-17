package com.storytron.swat.util;


import javax.swing.JMenu;
import javax.swing.JMenuItem;

/** 
 * An auxiliary class for handling menus with many items.
 * A expandable menu is a set of nested menus. When a menu
 * gets too many items a new menu is created and nested within
 * the set. In the current layout there is a root menu where
 * items are placed. When the root menu fills up new menus
 * are created and added as children of the root menu. So there
 * is only one level of nesting.   
 * <p>
 * This is a dirty hack. Most methods of JMenu which are not
 * overridden here will fail most likely.
 * <p>
 * To have a good behavior add items with {@link #add(JMenuItem)}
 * and delete them with {@link #removeAll()}.
 * <p>
 * If you want to make anything else than using those methods
 * you will need to understand and modify the implementation.  
 * */
public final class ExpandableMenu extends JMenu {
	private static final long serialVersionUID = 1L;
	
	private int maxItems=15;
	private JMenu current;
	private int currentPosition=-1;

	/** Creates an expandable menu with the given label. */
	public ExpandableMenu(String label){
		super(label);
		current=this;
	}
	
	/** 
	 * Adds a menu item to the expandable menu.
	 * If there are too many items in the current menu
	 * a new menu will be created and added as a root child. 
	 * */
	@Override
	public JMenuItem add(JMenuItem c) {
		if (current.getItemCount()<maxItems) {
			return mAddToCurrent(c);
		} else {
			JMenu aux = new JMenu("More...");
			JMenuItem c1 = aux.add(c);
			currentPosition++;
			insert(aux,currentPosition);
			current=aux;
			return c1;
		}
	}
	
	@Override
	public void removeAll() {
		super.removeAll();
		current=this;
		currentPosition=-1;
	}
	/** Sets the maximum amount of items that can be shown per menu. */
	public void setMaxItemsPerMenu(int maxItems){
		this.maxItems = maxItems;
	}
	
	private JMenuItem mAddToCurrent(JMenuItem c){
		return current==this? super.add(c) : current.add(c);
	}

}
