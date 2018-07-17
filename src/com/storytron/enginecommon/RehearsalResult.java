package com.storytron.enginecommon;
import java.util.ArrayList;


import java.io.Serializable;

import com.storytron.uber.Script;

public final class RehearsalResult implements Serializable {
	private static final long serialVersionUID = 1l;
	public ArrayList<VerbData> verbData;
	//public Verb centralVerb;
	public int centralVerbIndex;
	public ArrayList<Triplet<Script.Type,String[],String>> poisonings;

	public RehearsalResult(ArrayList<VerbData> verbData, int centralVerbIndex, ArrayList<Triplet<Script.Type,String[],String>> poisonings) {
		this.verbData = verbData;
		this.centralVerbIndex = centralVerbIndex;
		this.poisonings = poisonings;
	}
}
