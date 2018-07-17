package com.storytron.uber;

public final class Quantifier extends Word {
	private static final long serialVersionUID = 1l;
	public static String[] predefinedQuantifierLabels = {
		"extra tiny",
		"tiny",
		"very small",
		"small",
		"medium-small",
		"medium",
		"medium-large",
		"large",
		"very large",
		"huge",
		"extra huge",
	};
	int ID;
//**********************************************************************	
	public Quantifier(String label,int ID) {
			super(label);
			this.ID = ID;
		}
//**********************************************************************	
	public int getID() {
		return ID;
	}
//**********************************************************************
	/** 
	 * @param shortNeither ask for "neither" to be returned like that. If false, "neither"
	 *                     is returned as "neither t nor g".
	 * */
	public static String getQuantifierLabel(String bipolarLabel, int quantifierIndex, boolean shortNeither) {

		int _index = bipolarLabel.indexOf('_');
		if (_index==0 || _index ==-1 || _index == bipolarLabel.length() 
				|| _index!=bipolarLabel.lastIndexOf('_'))
			if (0<=quantifierIndex && quantifierIndex<predefinedQuantifierLabels.length) 
				return predefinedQuantifierLabels[quantifierIndex];
			else {
				System.out.println("WordButton:replaceQuantifierLabel:Quantifier out of range: "+quantifierIndex);
				return "";
			}

		String newLabel = "";
		switch (quantifierIndex) {
			case 0: { newLabel = "extremely "; break; }
			case 1: { newLabel = "very "; break; }
			case 2: { newLabel = "somewhat "; break; }
			case 3: { newLabel = "a little "; break; }
			case 4: { newLabel = "slightly "; break; }
			case 6: { newLabel = "slightly "; break; }
			case 7: { newLabel = "a little "; break; }
			case 8: { newLabel = "somewhat "; break; }
			case 9: { newLabel = "very "; break; }
			case 10: { newLabel = "extremely "; break; }
			default:;
		}
		if (quantifierIndex < 5) {
			String suffix = bipolarLabel.substring(0, _index);
			suffix = suffix.toLowerCase();
			newLabel = newLabel.concat(suffix);
		} else if (quantifierIndex==5) {
			if (shortNeither)
				newLabel = "neither";
			else
				newLabel = "neither "+bipolarLabel.substring(0, _index).toLowerCase()+" nor "+
							bipolarLabel.substring(_index+1).toLowerCase();
		} else {
			String suffix = bipolarLabel.substring(_index+1);
			suffix = suffix.toLowerCase();
			newLabel = newLabel.concat(suffix);
		}
		return (newLabel);
	}

}
