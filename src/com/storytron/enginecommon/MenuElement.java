package com.storytron.enginecommon;

import java.io.Serializable;

public final class MenuElement implements Serializable {
	private static final long serialVersionUID = 1l;
	private String label;
	private String description;
	public MenuElement(String tLabel, String tDescription) {
		label = tLabel;
		description = tDescription;
	}
	@Override
	public String toString(){
		return getLabel();
	}
	public String getLabel() {	return label;	}
	public String getDescription() { return description;	}
}
