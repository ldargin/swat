package com.storytron.enginecommon;

import java.io.Serializable;
import java.util.ArrayList;

import com.storytron.uber.Sentence;

public final class LabeledSentence implements Serializable {
	private static final long serialVersionUID = 1l;
	public Sentence rawSentence;
	public String[] labels = new String[Sentence.MaxWordSockets];
	public String[] descriptions = new String[Sentence.MaxWordSockets];
	public String expressionLabel;
	public String stageLabel;
	public ArrayList<String> actorsPresent = new ArrayList<String>();
	public ArrayList<String> propsPresent = new ArrayList<String>();
	public String[] suffixes = new String[Sentence.MaxWordSockets];
	public boolean[] visible = new boolean[Sentence.MaxWordSockets];
//----------------------------------------------------------------------
	public LabeledSentence() {
		expressionLabel = "";
		stageLabel = "";
		for (int i = 0; (i < Sentence.MaxWordSockets); ++i) {
			labels[i] = "";
			descriptions[i] = "";
			suffixes[i] = "";
			visible[i] = false;
		}
		visible[Sentence.Subject]=true;
		visible[Sentence.Verb]=true;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i<Sentence.MaxWordSockets; i++) {
			if (visible[i]) {
				if (i>0 && !startsWithPunctuation(labels[i]))
					sb.append(" ");
				sb.append(labels[i]);
				if (suffixes[i].length()>0) {
					if (!startsWithPunctuation(suffixes[i]))
						sb.append(" ");
					sb.append(suffixes[i]);
				}
			}
		}
		return sb.toString();
	}
	
	static private boolean startsWithPunctuation(String s) {
		return s.length()>0 && ".,:;".contains(s.subSequence(0,1));
	}
//----------------------------------------------------------------------

}
