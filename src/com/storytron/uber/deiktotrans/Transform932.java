package com.storytron.uber.deiktotrans;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/** 
 * Replaces Familiarity operators with undefined operators, 
 * deletes AdjustFamiliarity emotional reactions and
 * deletes familiarity values from actor traits.
 */
public class Transform932 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.932";	}

	public Document transform(Document doc) {
		
		for(Node token : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "token")) {
			String label = token.getAttributes().getNamedItem("Label").getNodeValue();
			if (label.equals("Familiarity")) {
				Element undefined = doc.createElement("token");
				undefined.setAttribute("Label", "?BNumber?");
				undefined.setAttribute("Arg","");
				
				token.getParentNode().replaceChild(undefined, token);
			} else if (label.equals("AdjustFamiliarity")) {
				token.getParentNode().getParentNode().removeChild(token.getParentNode());
			}
		}
		
		NodeList coNodes = doc.getElementsByTagName("customOperatorSet");
		if (coNodes.getLength()>0) {
			for(Node token : DeiktoLoader.getNodesByTagName(coNodes.item(0), "token")) {
				Node n = token.getAttributes().getNamedItem("Label");
				if (n!=null && "Familiarity".equals(n.getNodeValue())) {
					Element undefined = doc.createElement("token");
					undefined.setAttribute("Label", "?BNumber?");
					undefined.setAttribute("Arg","");
				}
			}
		}
		
		for(Node token : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("pValueSet").item(0), "attribute")) {
			String label = token.getAttributes().getNamedItem("Name").getNodeValue();
			if (label.equals("familiarity"))
				token.getParentNode().removeChild(token);
		}

		return doc;
	}
}
