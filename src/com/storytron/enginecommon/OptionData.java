package com.storytron.enginecommon;

import java.io.Serializable;
import com.storytron.uber.Role;

public final class OptionData  implements Serializable {
	private static final long serialVersionUID = 1l;
	//public Role.Option myOption;
	public String optionName;
	public int candidacies = 0;
	public int activations = 0;
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public OptionData(Role.Option tOption) {
		//myOption = tOption;
		optionName = tOption.getLabel();
	}
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
}
