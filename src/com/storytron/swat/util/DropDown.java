package com.storytron.swat.util;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;


import com.storytron.enginecommon.Utils;
import com.storytron.swat.Swat;

import java.awt.*;
import java.awt.event.*;

/**
 * A dropdown class that allows to drag items to reorder them.
 * */
public class DropDown extends Box implements WindowFocusListener, AWTEventListener{
	private static final long serialVersionUID = 1L;
	
	private Dimension preferredSize = null;
	
	/**
	 * Override this method for taking actions when an element changes
	 * from a position to another. 
	 * @param from position of the moved item before moving it.
	 * @param to position of the moved item after moving it.
	 * */
	public void indexMoved(int from,int to){
		//System.out.println("changing "+from+" to "+to);
	}
	/** 
	 * Override this method to specify the tooltip to show
	 * for an object in the dropdown. 
	 * */
	public String getTooltipText(Object o) {
		return null;
	}
	
	/**
	 * Set the selected item corresponding to the given index.
	 * */
	public void setSelectedIndex(int i){ setSelectedIndex(i,true); };
	public void setSelectedItem(Object item){ 
		int i=listModel.indexOf(item);
		if (i!=-1) setSelectedIndex(i,true);
	};
	
	public int getSelectedIndex(){ 
		if (!getSelectedItem().equals(jtext.getText()))
			setSelection();
		return selectedIndex; 
	};
	public Object getSelectedItem(){ 
		if (selectedIndex!=-1) return listModel.get(selectedIndex); 
		else return jtext.getText();
	};
	public Swat.TextComponent getTextComponent() { return jtext; }
	public JList getJList() { return jlist; }
	public JTextComponent getJTextComponent() { return jtext.getJTextComponent(); }
	public void setEditable(boolean ed){ jtext.setEditable(ed); }
	public void setAllowReordering(boolean allow){ allowReordering=allow; }
	public void setActionCommand(String command) { actionCommand=command; }
	public void removeAllItems(){ listModel.removeAllElements(); }
	public int getItemCount(){ return listModel.size(); };
	public void addItem(Object o){ listModel.addElement(o); };
	public void setBackground(Color bg){ 
		jtext.setBackground(bg); 
		jlist.setBackground(bg);
		scroll.setBackground(bg);
		scroll.getViewport().setBackground(bg);
	};
	public void setMaximumRowCount(int rowCount){ 
		jlist.setVisibleRowCount(rowCount);
		jlist.revalidate();
	}
	public void removeItemAt(int index){ listModel.removeElementAt(index);	}
	
	public DefaultListModel getModel() { return listModel; }
	
	public static String mouseButton(MouseEvent e){
		switch(e.getButton()){
		case MouseEvent.BUTTON1: return "BUTTON1";
		case MouseEvent.BUTTON2: return "BUTTON2";
		case MouseEvent.BUTTON3: return "BUTTON3";
		case MouseEvent.NOBUTTON: return "NOBUTTON";
		default: return "OTHER";
		}
	} 
	
	private void setSelection(){
		selectedIndex=-1;
		for(int i=0;i<listModel.size();i++)
			if (listModel.get(i).toString().equals(jtext.getText()))
				setSelectedIndex(i);
	}
	
	private void cancel(){
		if (selectedIndex==-1) jtext.setText("");
		else {
			jtext.setText(listModel.get(selectedIndex).toString());
			jlist.setSelectedIndex(selectedIndex);
			jlist.ensureIndexIsVisible(selectedIndex);
		}
		hidePopup();
	}
	
	public DropDown(int maxLength){
		super(BoxLayout.X_AXIS);		

		listModel = new DefaultListModel();		
		jlist=new JList(listModel){
			private static final long serialVersionUID = 1L;

			@Override
			public Point getToolTipLocation(MouseEvent event) {
				if (!listModel.isEmpty()) {
					Point p = indexToLocation(locationToIndex(event.getPoint()));
					p.x += jlist.getWidth();
					return p;
				} else
					return null;
			}
		};
		jlist.setCellRenderer(new DefaultListCellRenderer(){
			private static final long serialVersionUID = 1L;
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) { 
				if (value!=null)
					list.setToolTipText(getTooltipText(value));
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		});
		for(MouseMotionListener l:jlist.getMouseMotionListeners()) jlist.removeMouseMotionListener(l);
		for(MouseListener l:jlist.getMouseListeners()) jlist.removeMouseListener(l);
		scroll = new JScrollPane(jlist,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setFocusable(false);
		
		jtext.setDocument(new MaxLengthDocument(maxLength));
		jtext.setFont(jlist.getFont());
		jtext.setEditable(false);
		addAncestorListener(new AncestorListener(){
			public void ancestorAdded(AncestorEvent event) { cancel();validate(); }
			public void ancestorMoved(AncestorEvent event) { movePopup(); }
			public void ancestorRemoved(AncestorEvent event) { cancel(); }			
		});
		addComponentListener(new ComponentListener(){
			public void componentHidden(ComponentEvent e) { cancel(); }
			public void componentMoved(ComponentEvent e) { movePopup(); }
			public void componentResized(ComponentEvent e) { cancel();validate(); }
			public void componentShown(ComponentEvent e) { cancel(); }
		});
		addMouseListener(mouseGivesFocus);
		jtext.addKeyListener(new KeyListener(){
			public void keyPressed(KeyEvent e) {
				if (e.getModifiersEx()!=KeyEvent.SHIFT_DOWN_MASK
					&& e.getModifiersEx()!=0) {
					switch(e.getKeyCode()){
					case KeyEvent.VK_CONTROL:		
					case KeyEvent.VK_ALT_GRAPH:
					case KeyEvent.VK_ALT:
						break;
					default:
						cancel();
					}
					return;
				}
				switch(e.getKeyCode()){
				case KeyEvent.VK_DOWN:
					setListPicking(true);
					if (!popupList.isVisible()) showPopup();
					else if (!listModel.isEmpty()) {						
						int i=(jlist.getSelectedIndex()+1)%listModel.size();
						jtext.setText(listModel.get(i).toString());
						jlist.setSelectedIndex(i);
						jlist.ensureIndexIsVisible(i);
					}
					e.consume();
					break;
				case KeyEvent.VK_UP:
					setListPicking(true);
					if (!popupList.isVisible()) showPopup();
					else if (!listModel.isEmpty()) {						
						int s=listModel.size();
						int i=(jlist.getSelectedIndex()+s-1)%s;
						jtext.setText(listModel.get(i).toString());
						jlist.setSelectedIndex(i);
						jlist.ensureIndexIsVisible(i);
					}
					e.consume();
					break;
				case KeyEvent.VK_ESCAPE:
				case KeyEvent.VK_ENTER:
					setSelection();
					fireAction();					
					e.consume();
					break;
				case KeyEvent.VK_PAGE_UP:
					setListPicking(true);
					if (!jlist.isSelectionEmpty()) 
						setSelectedIndex(Math.max(jlist.getSelectedIndex()-jlist.getVisibleRowCount(),0));
					e.consume();
					break;
				case KeyEvent.VK_PAGE_DOWN:					
					setListPicking(true);
					if (!jlist.isSelectionEmpty()) 
						setSelectedIndex(Math.min(jlist.getSelectedIndex()+jlist.getVisibleRowCount(),listModel.size()-1));
					e.consume();
					break;
				default:
					setListPicking(false);
				}
			}
			public void keyReleased(KeyEvent e) {}
			public void keyTyped(KeyEvent e) {}
		});
		jtext.setBorder(new DropDownBorder());
		int rowHeight=jtext.getFontMetrics(jtext.getFont()).getHeight()+4;
		jtext.setMinimumSize(new Dimension(20,rowHeight+4));
		jtext.setPreferredSize(jtext.getMinimumSize());
		jtext.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!jtext.isEditable()) 
					if (popupList.isVisible()) hidePopup(); 
					else showPopup();
			}
		});
		
		Dimension bd = new Dimension(20,24);
		jbt.setMinimumSize(bd);		
		jbt.setPreferredSize(jbt.getMinimumSize());
		bd.height=1000;
		jbt.setMaximumSize(jbt.getMinimumSize());
		jbt.setFocusable(false);
		jbt.setRolloverEnabled(false);
		jbt.setAction(new AbstractAction("",new ImageIcon(Utils.getImagePath("arrowdown.png"))){
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				if (!popupList.isVisible()) showPopup();
				else hidePopup();
			}
		});
		jbt.addMouseListener(mouseGivesFocus);
							
		add(jtext);
		add(jbt);
		Dimension d=jtext.getMinimumSize();
		Dimension d2=jbt.getMinimumSize();
		d.width+=d2.width;		
		setMinimumSize(d);
		setPreferredSize(d);
		
		jlist.setValueIsAdjusting(true);
		jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlist.setFocusable(false);
		jlist.setFixedCellHeight(rowHeight);
		listModel.addListDataListener(new ListDataListener(){
			public void contentsChanged(ListDataEvent e) {validate();}
			public void intervalAdded(ListDataEvent e) { validate(); }
			public void intervalRemoved(ListDataEvent e) { validate(); }
		});		
		
		MouseInputAdapter ma = new MouseInputAdapter(){
			boolean in=false;
			int oldIndex=-1;
			@Override
			public void mouseEntered(MouseEvent e) {
				in=true;
			}
			@Override
			public void mouseExited(MouseEvent e) {
				in=false;
				if (oldIndex!=-1) {
					moveIndex(last,oldIndex);
					last=oldIndex;
					makeAroundVisible(oldIndex);
				}
			}
			@Override
			public void mousePressed(MouseEvent e) {
				last=oldIndex=jlist.locationToIndex(e.getPoint());
			}
			@Override
			public void mouseReleased(MouseEvent e){
				if (allowReordering && in && last!=oldIndex)
					indexMoved(oldIndex,last);
				last=oldIndex=-1;
				jtext.requestFocusInWindow();
			}
			@Override
			public void mouseMoved(MouseEvent e) {	
				int i=jlist.locationToIndex(e.getPoint());
				if (i!=last) {
					jlist.setSelectedIndex(i);
					last=i;
				}
			}			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!jlist.isSelectionEmpty()) {
					setSelectedIndex(jlist.getSelectedIndex());	
					setListPicking(true);
					fireAction();
				}
			}			
			// dragCount is used to slow down a little the velocity at 
			// which a item is interchanged when dragging. 
			// This is mostly useful to control autoscrolling velocity. 
			private int dragCount=0;
			@Override
			public void mouseDragged(MouseEvent e) {
				if (!in) return;
								
				int i=jlist.locationToIndex(e.getPoint());
				if (last!=i && dragCount==0) {
					moveIndex(last,i);					
					mouseMoved(e);
					last=i;
					makeAroundVisible(i);
				}
				dragCount=(dragCount+1)%3;
			}			
		};
		jlist.addMouseListener(ma);
		jlist.addMouseMotionListener(ma);
		setMaximumSize(getMaximumSize());
		validate();
	}
	
	@Override
	public void setPreferredSize(Dimension d){
		preferredSize = d;
		super.setPreferredSize(preferredSize);
	}
	
	@Override
	public Dimension getPreferredSize(){
		if (preferredSize==null) {
			if (!listModel.isEmpty()) {
				int cwidth = calculateWidth();
				Dimension d = jtext.getPreferredSize();
				d.width=cwidth+2+jbt.getPreferredSize().width;
				return fitTo(d,getMaximumSize());
			} else
				return super.getPreferredSize();
		} else
			return new Dimension(preferredSize);
	}

	private MouseListener mouseGivesFocus = new MouseAdapter(){
		public void mousePressed(MouseEvent e) {jtext.requestFocusInWindow();}
	};
	private int selectedIndex=-1;
	private void setSelectedIndex(int i,boolean updateList){	
		if (updateList){
			jlist.setSelectedIndex(i);
			jlist.ensureIndexIsVisible(i);
		}
		selectedIndex=i;
		jtext.setText(listModel.get(i).toString());
	};

	/** Swaps elements in the list, but do not calls {@link #indexMoved(int, int)}. */
	public void moveIndex(int from,int to){
		if (!allowReordering)
			return;

		if (from==selectedIndex)
			selectedIndex=to;
		else if (to==selectedIndex)
			selectedIndex=from;
		listModel.insertElementAt(listModel.remove(from), to);
	}
	private void makeAroundVisible(int index){
		if (index<listModel.getSize()-1) jlist.ensureIndexIsVisible(index+1);
		else {
			jlist.ensureIndexIsVisible(index);
			return;
		}
		if (index>0) jlist.ensureIndexIsVisible(index-1);
		else jlist.ensureIndexIsVisible(index);
		 
	} 
	private void fireAction(){
		ActionEvent ae=new ActionEvent(this,ActionEvent.ACTION_PERFORMED,actionCommand);
		for(ActionListener l:jtext.getActionListeners())					
			l.actionPerformed(ae);
		hidePopup();
	}
	private DefaultListModel listModel; 
	private JList jlist;
	private Swat.TextField jtext = new Swat.TextField();
	private JButton jbt = new JButton();
	private JScrollPane scroll;
	//private Popup popupList = null;
	private LightweightPopup popupList = new LightweightPopup();
	private int last=-1;
	private boolean allowReordering=false;
	private String actionCommand = "";
	
	public void movePopup(){
		if (!popupList.isVisible()) return;
		
		hidePopup();
		showPopup();
	}
	public void showPopup() {
		if (popupList.isVisible()) return;
		
		if(!listModel.isEmpty() && jlist.isSelectionEmpty())
			setSelectedIndex(Math.max(0,selectedIndex));

		SwingUtilities.getWindowAncestor(this).addWindowFocusListener(this);
		Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.MOUSE_EVENT_MASK);

		fireVisiblePopup();

		popupList.setContents(scroll);
		Dimension d = scroll.getPreferredSize();
		popupList.setSize(d);
		popupList.showPopup(this,0,getHeight());
		MenuTooltipManager.sharedInstance().registerComponent(jlist);
		jtext.requestFocusInWindow();
	} 
	public void hidePopup() {
		setListPicking(false);
		if (!popupList.isVisible()) return;
		
		MenuTooltipManager.sharedInstance().unregisterComponent(jlist);
		popupList.hidePopup();
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
	}
	public void setAction(Action a){jtext.setAction(a);}
	public void addActionListener(ActionListener a){jtext.addActionListener(a);}
	public void removeActionListener(ActionListener a){jtext.removeActionListener(a);}
	
	@Override
	public void validate(){
		int cwidth = calculateWidth();
		if (!listModel.isEmpty()) {
			if (preferredSize==null) {
				Dimension d=jtext.getPreferredSize();
				d.width=jbt.getPreferredSize().width+2+cwidth;
				super.setPreferredSize(fitTo(d,getMaximumSize()));
			} else
				super.setPreferredSize(preferredSize);
			if(selectedIndex==-1) 
				setSelectedIndex(0,false);
		} else {
			jtext.setText("");			
			selectedIndex=-1;
		}
		Dimension d=jtext.getSize();
		d.width+=jbt.getWidth()+2;
		if (!listModel.isEmpty()) {
			Insets insets = scroll.getBorder().getBorderInsets(scroll);
			d.height=Math.min(jlist.getPreferredScrollableViewportSize().height,jlist.getPreferredSize().height+insets.top+insets.bottom);
		}
		if (listModel.getSize()>jlist.getVisibleRowCount()){
			if (d.width<cwidth+scroll.getVerticalScrollBar().getWidth())
				d.width=cwidth+scroll.getVerticalScrollBar().getWidth();
		} else if (d.width<cwidth+4) d.width=cwidth+4;

		scroll.setPreferredSize(d);

		super.validate();	
	}
	private Dimension fitTo(Dimension d,Dimension max){
		if (d.height>max.height) d.height=max.height;
		if (d.width>max.width) d.width=max.width;
		return d;
	} 
	private int calculateWidth(){
		int max = 0;
		FontMetrics f = jlist.getFontMetrics(jlist.getFont());
		for(int i=0;i<listModel.getSize();i++) {
			int temp=f.stringWidth(listModel.get(i).toString());
			if (temp>max) max=temp;
		}
		return max;
	}
	private static class DropDownBorder extends AbstractBorder {
		private static final long serialVersionUID = 1L;
		private Color dark;
		private Color light;
		public DropDownBorder(){
			UIDefaults table = UIManager.getLookAndFeelDefaults();
			dark=table.getColor("TextField.darkShadow");
			light=table.getColor("TextField.shadow");
		}
		@Override
		public Insets getBorderInsets(Component c, Insets insets) {			
			insets.set(2,2,2,1);			
			return insets;
		}
		@Override
		public Insets getBorderInsets(Component c) {
			return new Insets(2,2,2,1);
		}
		@Override
		public boolean isBorderOpaque() {return true;}
		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			g.setColor(dark);
			g.drawLine(c.getX(),c.getY(), c.getX()+c.getWidth()-1,c.getY());
			g.drawLine(c.getX(),c.getY()+c.getHeight()-1, c.getX()+c.getWidth()-1,c.getY()+c.getHeight()-1);
			g.drawLine(c.getX(),c.getY(), c.getX(),c.getY()+c.getHeight()-1);

			g.setColor(light);
			g.drawLine(c.getX()+1,c.getY()+1, c.getX()+c.getWidth()-2,c.getY()+1);
			g.drawLine(c.getX()+1,c.getY()+c.getHeight()-2, c.getX()+c.getWidth()-2,c.getY()+c.getHeight()-2);
			g.drawLine(c.getX()+1,c.getY()+1, c.getX()+1,c.getY()+c.getHeight()-2);
			g.drawLine(c.getX()+c.getWidth()-1,c.getY()+1, c.getX()+c.getWidth()-1,c.getY()+c.getHeight()-2);
		}		
	} 
	
/*	public static void main(String[] args) {
		JFrame frm=new JFrame("DropDown");
		DropDown d = new DropDown();
		d.setEditable(true);

		DefaultListModel lm = d.getModel();
		//DefaultComboBoxModel lm = new DefaultComboBoxModel();
		for(int i=0;i<40;i++) lm.addElement("a longer test "+i);
		//JComboBox c = new JComboBox(lm);
		//c.setEditable(true);

		Box p = Box.createHorizontalBox();
		p.add(new JLabel("test: "));
		//p.add(c);		
		p.add(d);
		p.add(new JTextField("test"));
		frm.add(p);
		frm.pack();
		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frm.setVisible(true);
		//for(int i=0;i<20;i++) lm.removeAllElements();
	}*/

	@Override
	public void addMouseListener(MouseListener l){
		jtext.addMouseListener(l);
		jbt.addMouseListener(l);
	}
	@Override
	public void removeMouseListener(MouseListener l){
		jtext.removeMouseListener(l);
		jbt.removeMouseListener(l);
	}
	private EventListenerList popuplisteners=new EventListenerList();
	public void addPopupListener(PopupMenuListener l){
		popuplisteners.add(PopupMenuListener.class,l);
	}
	protected void fireVisiblePopup(){
		setListPicking(false);
		for(PopupMenuListener l:popuplisteners.getListeners(PopupMenuListener.class))
			l.popupMenuWillBecomeVisible(new PopupMenuEvent(this));
		setListPicking(true);
	} 
	@Override
	public Font getFont(){ return jtext.getFont(); }
	@Override
	public void setFont(Font f){ jtext.setFont(f); }

	/** Tells if the selection is being picked through the list or by typing.
	 * */
	private boolean listPicking=false;
	public void setListPicking(boolean listPicking) {this.listPicking = listPicking;}
	public boolean isListPicking() { return listPicking;}
	
	public void windowGainedFocus(WindowEvent e) {}
	public void windowLostFocus(WindowEvent e) { cancel(); }

	public void eventDispatched(AWTEvent e){			
		MouseEvent me = (MouseEvent)e;
		if (me.getID()!=MouseEvent.MOUSE_PRESSED || !popupList.isVisible()) return;
		if (me.getComponent()==null) {
			cancel();
			return;
		}
		Point p = me.getPoint();
		SwingUtilities.convertPointToScreen(p,me.getComponent());
		Point sp=scroll.getLocationOnScreen();		
		if (!scroll.contains(p.x-sp.x,p.y-sp.y)){
			sp=this.getLocationOnScreen();
			if (!contains(p.x-sp.x,p.y-sp.y)) cancel();
		}
	}
}
