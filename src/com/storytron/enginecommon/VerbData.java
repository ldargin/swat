package com.storytron.enginecommon;

import java.util.ArrayList;

import java.io.Serializable;

import com.storytron.uber.Role;
import com.storytron.uber.Verb;

public final class VerbData implements Serializable {
	private static final long serialVersionUID = 1l;
	//public Verb myVerb;
	public String verbName;
	public int candidacies = 0;
	public int activations = 0;
	public ArrayList<RoleData> roleData = new ArrayList<RoleData>();
	public int[] cLoopyBoobies = new int[10];
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public VerbData(Verb tVerb) {
		//myVerb = tVerb;
		verbName = tVerb.getLabel();
		for (Role.Link zRole: tVerb.getRoles()) {
			roleData.add(new RoleData(zRole));
		}
		for (int i=0; (i<9); ++i)
			cLoopyBoobies[i]=0;
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
}
