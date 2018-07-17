package storyTellerPackage;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.storytron.enginecommon.LabeledSentence;
import com.storytron.enginecommon.MenuElement;
import com.storytron.enginecommon.Utils;
import com.storytron.swat.util.FlowScrollLayout;
import com.storytron.swat.util.MenuTooltipManager;
import com.storytron.uber.Sentence;

public final class SentenceDisplay {
	/** must match Engine.deadEndMessage. TODO: move to a shared class */
	public static String deadEndMessage = "No options here; try something else."; 

	private boolean[] hot = new boolean[Sentence.MaxWordSockets];
	private WordButton[] wbs = new WordButton[Sentence.MaxWordSockets];
	private HotButtonListener hbl;
	private JPopupMenu menu;
	boolean isLeftSentence;
	private int hotWordSocket;
	private JComponent panel;
	private JComponent sentencePanel;
	private boolean allowLayout = false; 
	
//**********************************************************************
	SentenceDisplay(Storyteller tst, HotButtonListener hbl, boolean tIsLeftSentence) {
		sentencePanel = Box.createVerticalBox();
		sentencePanel.add(Box.createVerticalGlue());

		this.hbl = hbl;
		for(int i=0;i<hot.length;i++)
			hot[i] = false;
		isLeftSentence = tIsLeftSentence;
		hotWordSocket = Sentence.MaxWordSockets;
		
		sentencePanel.addComponentListener(new ComponentAdapter(){
			public void componentResized(ComponentEvent e) {	doLayout();		}
		});
	}
	
	/** Returns the component used to display the sentence. */
	public JComponent getSentencePanel(){
		return sentencePanel;
	}
	
	private void clearSentencePanel(){
		while(sentencePanel.getComponentCount()>1)
			sentencePanel.remove(0);
	}
	
	/** 
	 * Adds a new paragraph panel to the component hierarchy.
	 * @return the new paragraph panel
	 *  */
	private JComponent newParagraphPanel(){
		final JPanel panel = new JPanel(null);
		
		final JScrollPane scrollPane = new JScrollPane(panel,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER){
			private static final long serialVersionUID = 1L;
			@Override
			public Dimension getMaximumSize() {
				return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);
			}
		};
		FlowScrollLayout l = new FlowScrollLayout(scrollPane) {
			private static final long serialVersionUID = 1L;
			@Override
			public void layoutContainer(Container c) {
				if (allowLayout) { // Layout only when we want to do so explicitly
					Dimension d = scrollPane.getViewport().getExtentSize();
					d.height = 10;
					scrollPane.getViewport().setExtentSize(d);
					super.layoutContainer(c);
					d.height = panel.getPreferredSize().height;
					scrollPane.getViewport().setExtentSize(d);
				}
			}
		};
		l.setAlignment(FlowLayout.LEFT);
		l.setHgap(4);
		l.setVgap(5);
		panel.setLayout(l);

		scrollPane.setBorder(BorderFactory.createEmptyBorder(0,15,5,15));
		scrollPane.setLocation(30,150);
		panel.setOpaque(false);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.getVerticalScrollBar().setUnitIncrement(40);
		scrollPane.getViewport().setExtentSize(new Dimension(sentencePanel.getWidth(),10));
		
		sentencePanel.add(scrollPane,sentencePanel.getComponentCount()-1);
		
		return panel;
	}

	/** Layouts the word elements. */
	public void doLayout(){
		allowLayout=true;
		for(int i=0;i<sentencePanel.getComponentCount()-1;i++)
			((JScrollPane)sentencePanel.getComponent(i)).getViewport().getView().doLayout();
		sentencePanel.doLayout();
		allowLayout=false;
	}
	
//**********************************************************************
	public void revise(final LabeledSentence tEvent) {
		revise(tEvent,null,Sentence.MaxWordSockets);
	}
	public void revise(final LabeledSentence tEvent, ArrayList<MenuElement> tMenuElement, 
						final int tWordSocket) {
		
		if (hotWordSocket<Sentence.MaxWordSockets)
			hot[hotWordSocket] = true;
		hotWordSocket = tWordSocket;
		for(int i=hotWordSocket;i<hot.length;i++) {
			hot[i] = false;
			wbs[i] = null;
		}
		
		if (tMenuElement!=null && !tMenuElement.isEmpty()) {
			if (menu==null) {
				menu = new JPopupMenu("?");
				menu.addPopupMenuListener(new PopupMenuListener(){
					public void popupMenuCanceled(PopupMenuEvent e) {
					}
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						// Theoretically this should be done in the canceled event,
						// but the canceled event does not seem to be executed in an applet. 
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {
								SentenceDisplay.this.requestFocusInWindow();
							}
						});
					}
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						SwingUtilities.invokeLater(new Runnable(){
							public void run() {
								if (menu.getComponentCount()>0) {
									final javax.swing.MenuElement me[] = new javax.swing.MenuElement[2];
									me[0] = menu;
									me[1] = (javax.swing.MenuElement)menu.getComponent(0);
									MenuSelectionManager.defaultManager().setSelectedPath(me);
								}
							}
						});
					}
					
				});
			} else menu.removeAll();
			
			for (int i=0;i<tMenuElement.size();i++) {
				MenuElement me = tMenuElement.get(i);
				final int ind = i;
				final String menuText = me.getLabel();
				JMenuItem localItem = new JMenuItem(menuText);
				localItem.setToolTipText(Utils.toHtmlTooltipFormat(Utils.nullifyIfEmpty(me.getDescription())));
				MenuTooltipManager.sharedInstance().registerComponent(localItem);
				menu.add(localItem);
				localItem.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						wbs[tWordSocket].setText(menuText);
						tEvent.rawSentence.setIWord(tWordSocket, ind);

						hbl.menuActionPerformed(e, ind);
					}
				});	
			}
			
		} else menu = null;
		
		draw(tEvent);
	}
//**********************************************************************
	private final static Pattern wordSplitter = Pattern.compile(" ");
	private void draw(LabeledSentence tEvent) {
		// first we clear out everything
		clearSentencePanel();
		panel = newParagraphPanel();		
		// Now we add the appropriate WordButtons
		final String[] suffixes = tEvent.suffixes.clone();
		final boolean[] visible = tEvent.visible.clone();
		int i = 0;
		while (i<Sentence.MaxWordSockets && i<hotWordSocket) {
			if (visible[i]) {
				if (hot[i] || tEvent.labels[i].length()>0) {
					WordButton wb = createWordSocket(tEvent,i,suffixes,visible);
					wbs[i]=wb;
					panel.add(wb);
				}
				String[] line = suffixes[i].split("\n");
				if (line.length>0 && line[0].trim().length()>0) {
					for(String word:wordSplitter.split(line[0]))
						panel.add(new JLabel(word));
				}
				for(int j=1;j<line.length;j++) {
					if (line[j].trim().length()>0) {
						panel.revalidate();
						panel.repaint();
						panel = newParagraphPanel();
						for(String word:wordSplitter.split(line[j]))
							panel.add(new JLabel(word));
					}
				}
			}
			++i;
		}
		if (hotWordSocket < Sentence.MaxWordSockets) {
			final WordButton wb = createWordSocket(tEvent,hotWordSocket,suffixes,visible);
			wbs[hotWordSocket]=wb;
			panel.add(wb);
			panel.revalidate();
			sentencePanel.revalidate(); // This code must be executed before queuing to show the popup.
			sentencePanel.repaint();
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					showMenu(wb);
				}
			});
		} else {
			panel.revalidate();
			sentencePanel.revalidate();
			sentencePanel.repaint();
		}
	}
	
	/** Creates a wordsocket button for a given wordsocket. */
	private WordButton createWordSocket(LabeledSentence tEvent,final int tiWordSocket
										,final String[] suffixes,final boolean[] visible){
		WordButton wb;
		if (isLeftSentence) {
			
			wb = new WordButton(false);
			wb.setText(tEvent.labels[tiWordSocket]);
			
		} else if (tiWordSocket == Sentence.Subject) {
			
			wb = new WordButton(false);
			wb.setText(tEvent.labels[tiWordSocket]);
			
		} else if (tiWordSocket==hotWordSocket 
					&& tEvent.labels[tiWordSocket].equals(deadEndMessage)
					&& !existsWordSocketsToUndo()) {
			
			wb = new WordButton(false);
			wb.setToolTipText("There's no valid choice!");
			wb.setText("Dead End!");
			
		} else if (tiWordSocket==hotWordSocket && menu!=null) {

			wb = new WordButton(true);
			final WordButton fwb = wb;
			wb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					showMenu(fwb);
				}
			});
			switch (tEvent.rawSentence.getWordSocketType(tiWordSocket)) {
			case Actor: { wb.setText("Who?"); break; }
			case Prop: { wb.setText("What?"); break; }
			case Stage: { wb.setText("Where?"); break; }
			case Verb: { wb.setText("Do What?"); break; }
			case ActorTrait: { wb.setText("Which?"); break; }
			case PropTrait: { wb.setText("Which?"); break; }
			case StageTrait: { wb.setText("Which?"); break; }
			case MoodTrait: { wb.setText("Which?"); break; }
			case Quantifier: { wb.setText("How Much?"); break; }
			case Event: { wb.setText("Which?"); break; }
			default: { wb.setText("this WordSocket is set to be invisible!"); break; }
			}
			wb.setToolTipText(Utils.toHtmlTooltipFormat("You need to select the word that goes here"));

		} else if (hot[tiWordSocket]) {
			
			wb = new WordButton(true);
			wb.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					int fromLast=0;
					for(int i=tiWordSocket+1;i<Sentence.MaxWordSockets;i++)
						if (hot[i])
							fromLast++;
					hbl.actionPerformed(e, fromLast);						
				}
			});
			wb.setText(tEvent.labels[tiWordSocket]);
			
		} else {
			
			wb = new WordButton(false);
			wb.setText(tEvent.labels[tiWordSocket]);
			
		}
		
		wb.setToolTipText(Utils.toHtmlTooltipFormat(Utils.nullifyIfEmpty(tEvent.descriptions[tiWordSocket])));
		return wb;
	}

	/** Tells if this sentence has any wordsocket selection to undo. */
	private boolean existsWordSocketsToUndo() {
		for(int i=0;i<hotWordSocket;i++)
			if (hot[i])
				return true;
		return false;
	}
	
	/** Adds a button "." with the given action at the end of the sentence. */
	public JButton putPeriodButton(ActionListener periodPressed){
		JButton b = new JButton(".");
		b.addActionListener(periodPressed);
		b.setToolTipText("Press to confirm your sentence!");
		panel.add(b);
		panel.revalidate();
		panel.repaint();
		return b;
	}
	
	public void setEnabled(boolean enabled) {
		for(int i=0;i<hot.length;i++)
			if (hot[i] || i==hotWordSocket)
				wbs[i].setEnabled(enabled);
	}

	/** Set hot wordsockets as specified by the given array. */
	public void setHotWordSockets(boolean[] hots){
		for(int i=0;i<hot.length;i++)
			hot[i] = hots[i];
	}
	/** Specifies through a boolean array which are the hot wordsockets. */
	public boolean[] getHotWordSockets(){
		return hot.clone();
	}

	/** 
	 * Interface to perform actions when a hot button is pressed, 
	 * or a menu item is selected.
	 * */
	public interface HotButtonListener {
		/** 
		 * Called when a hot button is pressed. 
		 * @param fromLast tells how many hot buttons are ahead in the 
		 *        sentence in front of the one pressed.  
		 * */
		public void actionPerformed(ActionEvent e,int fromLast);

		/** 
		 * Called when a menu item is selected. 
		 * @param i tells the selected menu index.  
		 * */
		public void menuActionPerformed(ActionEvent e,int i);
	} 
	
	
	private static class WordButton extends JButton {
		private static final long serialVersionUID = 1L;
	//**********************************************************************
		WordButton(boolean hot) {
			if (!hot) {
				setBackground(new Color(250,250,250));
				setFocusable(false);
				setRolloverEnabled(false);
				setModel(new DefaultButtonModel() {
					private static final long serialVersionUID = 0L;
					public boolean isPressed() { return false;	}
					public boolean isArmed() {	return false;	}
				});
			}
			setMargin(new Insets(2,4,2,4));

			setHorizontalAlignment(0);
		}
	}
	
	private void showMenu(WordButton wb){
		if (menu!=null)
			menu.show(wb, 0, wb.getHeight());
	}

	public boolean requestFocusInWindow() {
		if (menu!=null && menu.isVisible())
			return true;
		
		if (menu!=null && hotWordSocket < Sentence.MaxWordSockets)
			return wbs[hotWordSocket].requestFocusInWindow();
		else {
			for(int i=hotWordSocket-1;0<=i;i--){
				if (hot[i])
					return wbs[i].requestFocusInWindow();
			}
			return false;
		}
	}
}
