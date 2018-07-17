package com.storytron.uber.deiktotrans;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


/** 
 * Process operator change from "ForceFateToReact" to "PermitFateToReact" 
 * by converting script tokens.
 */

public class Transform924 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.924";	}

	public Document transform(Document doc) {
		// Find tokens with label "ForceFateToReact"
		for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "token")){
			String name=att.getAttributes().getNamedItem("Label").getNodeValue();
			if ("ForceFateToReact".equals(name)){
				// Update token label
				att.getAttributes().getNamedItem("Label").setNodeValue("PermitFateToReact");
			}
		}
		return doc;
	}

}
