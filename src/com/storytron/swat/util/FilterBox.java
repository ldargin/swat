package com.storytron.swat.util;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.storytron.enginecommon.Utils;

/** 
 * Implements a filter box that allows the user selecting
 * options from a menu using the keyboard or the mouse.
 * */
public class FilterBox extends JPanel {
	private static final long serialVersionUID = 0L;

	public static final int REFRESH_TO_CURRENT = 0;
	public static final int REFRESH_TO_FISRT = 1;
	
	/** Override this to set your own tooltips.*/
	public String getItemToolTipText(Object o){
		return null;
	}

	/** Listener is notified every time a selection is made. */
	public void addActionListener(ActionListener l){
		textField.addActionListener(l);
	}

	/** Listener is notified of key events. */
	public void addKeyListener(KeyListener l){
		textField.addKeyListener(l);
	}

	public void setListCellRenderer(ListCellRenderer cellRenderer){
		jlist.setCellRenderer(cellRenderer);
	}
	
	/** Returns the selected item. null if non is selected. */
	public Object getSelected(){
		return jlist.getSelectedValue();
	}

	/** Tells if the list displayed is empty. */
	public boolean isListDisplayEmpty(){
		return jlist.getModel().getSize()==0;
	}

	/** Sets the selected item. */
	public void setSelected(Object o){
		jlist.setSelectedValue(o,true);
	}

	/** Sets the selected index. */
	public void setSelectedIndex(int i){
		jlist.setSelectedIndex(i);
		jlist.ensureIndexIsVisible(i);
	}

	/** Sets the tooltip text for the text field. */
	public void setTextFieldTooltip(String tooltipText){
		textField.setToolTipText(tooltipText);
	}
	
	/** 
	 * Refreshes the filter box. Needed after modifying
	 * the list of available options.
	 * */
	public void refresh(){
		filterListElems(textField.getText().trim());
	}

	/** Clears the text in the text field. */
	public void clearText(){
		textField.setText("");
	}

	@Override
	public boolean requestFocusInWindow(){
		return textField.requestFocusInWindow();
	}
	
	/** Creates a filter box. */
	public FilterBox(Iterable<?> l){
		super(null);
		initWidgets(l);
	}
	
	/** Sets the text color for the list. */
	public void setListTextColor(Color fg){
		jlist.setForeground(fg);
	}

	/** 
	 * Tells how the selection must be updated when refreshing the filter box.
	 * <p>
	 * Possible values are REFRESH_TO_CURRENT and REFRESH_TO_FIRST.
	 * */
	public void setRefreshSelectionType(int refreshType){
		refreshSelectionType = refreshType;
	}
	
	/** Returns the text in the filter. */
	public String getFilterText(){
		return textField.getText();
	}
	
	/** 
	 * Tells which is the index to select by default if the selection type is 
	 * not {@link #REFRESH_TO_CURRENT}. 
	 * */
	protected int getDefaultSelectedIndex(JList list){ return 0; }
	
	private int refreshSelectionType = REFRESH_TO_CURRENT;
	private JTextField textField = new JTextField();
	private Iterable<?> list;
	private JList jlist;

	private void initWidgets(Iterable<?> l){
		list = l;
		
		// Layout and panels
		setOpaque(false);
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		add(textField);
		Dimension d = textField.getPreferredSize();
		d.width = Integer.MAX_VALUE;
		textField.setMaximumSize(d);
		
		// elements
		DefaultListModel lm = new DefaultListModel();
		for(Object o:list)
			lm.addElement(o);
		
		jlist = new JList(lm){
			private static final long serialVersionUID = 0L;

			@Override
			public String getToolTipText(MouseEvent event) {
				int row = locationToIndex(event.getPoint());
				if (row==-1)
					return FilterBox.this.getItemToolTipText(null);
				else
					return FilterBox.this.getItemToolTipText(getModel().getElementAt(row));
			}
		};
		JScrollPane scroll = new JScrollPane(jlist);
		add(scroll);
		
		// event handlers
		textField.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e) {
				filterListElems(textField.getText().trim());
			}
			public void insertUpdate(DocumentEvent e) {
				filterListElems(textField.getText().trim());
			}
			public void removeUpdate(DocumentEvent e) {
				filterListElems(textField.getText().trim());
			}
			
		});
		jlist.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				switch(e.getKeyCode()){
				case KeyEvent.VK_ENTER:
					textField.dispatchEvent(e);
				}
			}
			public void keyReleased(KeyEvent e) {}

			public void keyTyped(KeyEvent e) {
			}
		});
		textField.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				switch(e.getKeyCode()){
				case KeyEvent.VK_ENTER:
					e.consume();
					textField.postActionEvent();
					break;
				case KeyEvent.VK_UP:
					moveSelection(-1);
					e.consume();
					break;
				case KeyEvent.VK_DOWN:
					moveSelection(1);
					e.consume();
					break;
				case KeyEvent.VK_PAGE_DOWN:
					if (jlist.getSelectedIndex()!=-1 
							&& jlist.getSelectedIndex()==((DefaultListModel)jlist.getModel()).size()-1){
						jlist.setSelectedIndex(0);
						jlist.ensureIndexIsVisible(0);
					}else 
						jlist.dispatchEvent(e);
					break;
				case KeyEvent.VK_PAGE_UP:
					if (jlist.getSelectedIndex()!=-1 && jlist.getSelectedIndex()==0) {
						int newindex = ((DefaultListModel)jlist.getModel()).size()-1;
						jlist.setSelectedIndex(newindex);
						jlist.ensureIndexIsVisible(newindex);
					} else 
						jlist.dispatchEvent(e);
					break;
				case KeyEvent.VK_HOME:
				case KeyEvent.VK_END:
					jlist.dispatchEvent(e);
				}
			}
			public void keyReleased(KeyEvent e) {}

			public void keyTyped(KeyEvent e) {
			}
		});
		jlist.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent e) {
				textField.dispatchEvent(new KeyEvent(textField,KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0,KeyEvent.VK_ENTER,'\n'));
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});
	}

	/** 
	 * Changes the selection, making it behave as in a circular list:
	 * going up in the first element jumps to the last one and vice versa.
	 * */
	private void moveSelection(int i){
		DefaultListModel lm = (DefaultListModel)jlist.getModel();
		if (lm.size()==0)
			return;
		
		int index = jlist.getSelectedIndex();
		if (index==-1) {
			if (i<0)
				jlist.setSelectedIndex(lm.size()+i);
			else
				jlist.setSelectedIndex(i);
		} else {
			index += i;
			if (index<0)
				index += lm.size();
			else if (index>=lm.size())
				index -= lm.size();
			jlist.setSelectedIndex(index);
		}
		jlist.ensureIndexIsVisible(jlist.getSelectedIndex());
	}
	
	private void filterListElems(String selector){
		DefaultListModel lm = (DefaultListModel)jlist.getModel();
		Object oldSelection = jlist.getSelectedValue();
		lm.removeAllElements();
		for(Object o:list)
			if (Utils.containsIgnoreCase(o.toString(),selector.toLowerCase()))
				lm.addElement(o);
		if (!lm.isEmpty()) {
			if (refreshSelectionType==REFRESH_TO_CURRENT)
				jlist.setSelectedValue(oldSelection, true);
			else {
				int i=getDefaultSelectedIndex(jlist);
				jlist.setSelectedIndex(i);
				jlist.ensureIndexIsVisible(i);
			}
		}
		jlist.revalidate();
		validate();
	}

}
