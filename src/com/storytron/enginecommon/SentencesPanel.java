package com.storytron.enginecommon;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;

/** A panel for displaying sentence inside JScrollPanes. */
public class SentencesPanel extends JPanel implements Scrollable {
	private static final long serialVersionUID = 1L;

	public SentencesPanel(LayoutManager lm){ super(lm); }
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}
	public int getScrollableBlockIncrement(Rectangle visibleRect,int orientation, int direction) {
		return 20;
	}
	public boolean getScrollableTracksViewportHeight() { return false;	}
	public boolean getScrollableTracksViewportWidth() {	return true; }
	public int getScrollableUnitIncrement(Rectangle visibleRect,int orientation, int direction) {
		return 40;
	}
}
