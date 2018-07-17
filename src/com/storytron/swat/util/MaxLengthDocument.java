package com.storytron.swat.util;

import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import com.storytron.enginecommon.Utils;

/** A document for limiting length of text fields. */
public final class MaxLengthDocument extends PlainDocument {
	private static final long serialVersionUID = 0L;
	private int max;

	// create a Document with a specified max length
	public MaxLengthDocument(int maxLength) {
		max = maxLength;
	}

	public int getLimit() {	return max;	}
	public void setLimit(int max) {	this.max = max;	}

	// don't allow an insertion to exceed the max length
	public void insertString(int offset, String str, AttributeSet a)
	              throws BadLocationException {
		if (getLength() + str.length() > max) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			if (getLength()<max)
				super.insertString(offset, str.substring(0,max-getLength()), a);
		} else 
			super.insertString(offset, str, a);
	}
	
	@Override
	public void replace(int offset, int length, String text, AttributeSet attrs)
			throws BadLocationException {
		if (text!=null && getLength() + text.length()-length > max) {
			java.awt.Toolkit.getDefaultToolkit().beep();
			if (offset<max) {
				final String t = Utils.truncate(text,max-offset);
				final int l = getLength() + t.length()-length;
				if (l>max)
					super.remove(Math.max(offset+length,2*max-l),l-max);
				super.replace(offset, length, t, attrs);
			}
		} else 
			super.replace(offset, length, text, attrs);
	}
}
