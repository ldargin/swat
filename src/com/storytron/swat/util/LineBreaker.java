package com.storytron.swat.util;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to break string into lines.
 * <p>
 * Each line contains a sequence of words defined by a pattern.
 * <p>
 * Each line contains at most a word, or it is guaranteed that
 * it will have less than a specified character length.
 * <pre>   
 * USAGE:
 * 
 *  for(String s : new LineBreaker(inputString,wordPattern,lineLength))
 *    System.out.println(s);
 * </pre>
 * Don't nest for loops iterating with the same LineBreaker.
 * I mean, don't do this:
 * <pre>
 *  LineBreaker lb = new LineBreaker(inputString,wordPattern,lineLength);
 *  for(String s1 : lb)
 *  	for(String s2 : lb) {
 *    		System.out.println(s1);
 *    		System.out.println(s2);
 *    }
 * </pre>
 * The following word pattern could be useful for you if you are trying to separate words.
 * Breaks line "Great Text To Split" as "Great " "Text " "To " "Split" and
 * "GreatTextToSplit" as "Great" "Text" "To" "Split" 
 * <pre>
 * "([^\\p{javaUpperCase}\\s]+)|(\\p{javaUpperCase}+[^\\p{javaUpperCase}\\s]*)|\\s+|\\S+"
 * </pre>
 * <p>
 * If you want to skip some invisible words like html tags, you must override the
 * {@link #wordLength(String)} method to return 0 for tags, and modify the word
 * pattern to recognize html tags as words: 
 * <pre>
 * "(<[^>]*>)|([^\\p{javaUpperCase}<\\s]+)|(\\p{javaUpperCase}+[^\\p{javaUpperCase}<\\s]*)|\\s+|\\S+"
 * </pre>
 */
public class LineBreaker implements Iterable<String>, java.util.Iterator<String> {
	private StringBuilder sb;
	private int lineLength;
	private Matcher matcher;
	private int width;
	/**
	 * Constructs a LineBreaker.
	 * This can be thought of as a collection of strings. The concatenation of all of 
	 * them is equal to <code>s</code>. Each string has only one word that matches 
	 * <code>pattern</code>, or more words matching pattern and a also has a length less 
	 * than <code>lineWidth<code>.  
	 * @param s The string to break.
	 * @param regex regular expression specifying the shape of words.
	 * @param lineWidth the maximum length allowed for a line in characters (not pixels).
	 * */
	public LineBreaker(String s,String regex,int lineWidth){
		sb=new StringBuilder(2*s.length());
		lineLength=0;
		matcher = Pattern.compile(regex).matcher(s);
		width = lineWidth;
		if (matcher.find()) sb.append(matcher.group());
	};
	public LineBreaker(String regex,int lineWidth) {
		this("",regex,lineWidth);
	}
	public void setString(String s){
		sb.setLength(0);
		lineLength=0;
		matcher.reset(s);
		if (matcher.find())
			eatSpaces(matcher.group());
	}
	public void setWidth(int lineWidth){
		width = lineWidth;
	}
	public Iterator<String> iterator() {
		return this;
	}
	public boolean hasNext() {
		return sb.length()>0;
	}
	public String next() {
		String m = null;
		int wl=0;
		while (matcher.find() && lineLength+(wl=wordLength(m=matcher.group()))<=width) {
			sb.append(m);
			lineLength+=wl;
			m=null;
		}
		String line = sb.toString().trim();			
		if (m != null)
			eatSpaces(m);
		else {
			sb.setLength(0);
			lineLength = 0;
		}
		return line;
	}
	public void remove() {
		throw new UnsupportedOperationException("LineBreaker: can not remove item.");			
	}
	
	protected int wordLength(String s){
		return s.length(); 
	}
	
	private void eatSpaces(String nextWord){
		nextWord = nextWord.trim();
		int wl;
		while((wl=wordLength(nextWord))==0 && matcher.find())
			nextWord = matcher.group().trim();
		
		sb.replace(0, sb.length(), nextWord);
		lineLength = wl;
	}
}
