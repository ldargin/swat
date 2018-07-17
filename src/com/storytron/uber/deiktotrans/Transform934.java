package com.storytron.uber.deiktotrans;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/** 
 * Renames parameter labels to remove the last "?" character.
 */
public class Transform934 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.934";	}

	public Document transform(Document doc) {
		NodeList l = doc.getElementsByTagName("customOperatorSet");
		if (l.getLength()==0)
			return doc;
		
		for(Node parameterElement : DeiktoLoader.getNodesByTagName(l.item(0), "parameter")) {
			String parameterLabel = parameterElement.getAttributes().getNamedItem("Label").getNodeValue();
			Node newAtt=doc.createAttribute("Label");
			newAtt.setNodeValue(parameterLabel.substring(0,parameterLabel.length()-1));
			parameterElement.getAttributes().setNamedItem(newAtt);
		}

		return doc;
	}
}
