package com.storytron.uber;

import java.io.Serializable;
import java.util.Map;

import com.storytron.enginecommon.ScaledImage;

/**
 * A class to represent words in the system.
 * <p>
 * For most purposes is used like a representation of
 * a world element, rather than as a denotation of it.
 * But it is called Word because sometimes it works as a word 
 * when it appears in a sentence.
 * <p>
 * The words have functionality for storing them in {@link Map}s (unique
 * generated identifier, implements {@link Comparable}),
 * and also implements indirect references to make easier implementation
 * of some model manipulations.
 */
public abstract class Word implements Cloneable, Comparable<Word>, Serializable  {
	private static final long serialVersionUID = 1l;	
	private String  label;
	/**
	 * Unique identifiers are generated for each word.
	 * This is in order to have a suitable key to insert words
	 * into collections of words such as {@link java.util.TreeMap} or
	 * {@link java.util.TreeSet}.
	 */ 
	private static long nextId = 0; 
	private final long wordId = nextId++;
	private Reference r = new Reference();

//***********************************************************************
	Word(String tLabel) { label = tLabel;	}
//***********************************************************************
	public Word clone() {
		Word newWord;
		try {
			newWord = (Word)super.clone();
			newWord.r = (Reference)r.clone(); 
			return(newWord);
		} catch (CloneNotSupportedException e) {
				throw new Error("Word.clone: This should never happen!");
		}
	}
//***********************************************************************	
	public String getLabel() { return (label); }
//***********************************************************************	
	public void setLabel(String newLabel) { label = newLabel; }
//***********************************************************************	
	@Override
	public String toString(){ return getLabel(); }
//	***********************************************************************
	/**
	 * Comparison of words based on its unique identifiers.
	 */
	public int compareTo(Word o) {	
		if (wordId<o.wordId) return -1;
		else if (wordId==o.wordId) return 0;
		else return 1;	
	}
	/**
	 * <p>A class to store the index assigned in {@link Deikto} main word collections
	 * to this Word object. It helps managing deletions and interchanges
	 * of words, as you can share a reference instance among many objects
	 * that need to reference to the same Word.</p>
	 * <p>
	 * Whenever words are removed or reordered all the references to the word can be updated
	 * with a single assignment.
	 * <p>
	 * Class {@link Deikto} is responsible for updating the reference as
	 *  appropriate when deleting or swapping words.</p>
	 * */
	public static final class Reference implements Cloneable, Serializable {
		private static final long serialVersionUID = 1l;
		private int index = 0;
		private Reference() {}
		private Reference(int i) { index=i; }
		public int getIndex() { return index; }
		public void setIndex(int i) { index=i; }
		@Override
		public Object clone() throws CloneNotSupportedException { return super.clone(); }
	}
	/**
	 * Returns the reference that will be updated when this word
	 * be deleted or swapped in {@link Deikto} main word collections. 
	 */
	public Reference getReference() { return r; }
	/**
	 * Reference to be used for zeroWords (Fate,Nothing,Nowhere,etc).
	 */
	public final static Reference zeroReference = new Reference(0);

	/** Returns the image for the given word. */
	public ScaledImage getImage(Deikto d) { return null; }
	/** Tells if the image for the given word has been modified. */
	public boolean isImageModified() { return false; }
	/** Sets the image change count to 0. */
	public void resetImageChangeCount() {}
	/** Returns the filename where this image is loaded from/saved to. */
	public String getImageName() { return null; }
}
