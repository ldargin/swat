package com.storytron.enginecommon;

import java.io.Serializable;
import java.util.ArrayList;

import com.storytron.uber.Role;
public final class RoleData implements Serializable {
	private static final long serialVersionUID = 1l;
	//public Role myRole;
	public String roleName;
	public int candidacies = 0;
	public int activations = 0;
	public ArrayList<OptionData> optionData = new ArrayList<OptionData>();
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public RoleData(Role.Link tRole) {
		//myRole = tRole;
		roleName = tRole.getLabel();
		for (Role.Option zOption: tRole.getRole().getOptions()) {
			optionData.add(new OptionData(zOption));
		}
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
}
