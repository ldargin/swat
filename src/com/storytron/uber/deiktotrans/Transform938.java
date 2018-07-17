package com.storytron.uber.deiktotrans;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/** 
 * Replaces old operators NominativePronoun, GenitivePronoun and AccusativePronoun 
 * with new versions using a new argument.
 */
public class Transform938 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.938";	}

	public Document transform(Document doc) {

		for(Node socketElement : DeiktoLoader.getNodesByTagName(doc, "socket")) {
			for(Node tokNode: DeiktoLoader.getNodesByTagName(socketElement, "token")) {
				Element tokElement = (Element)tokNode;
				String label = tokElement.getAttribute("Label");
				if (!tokElement.hasChildNodes()
						&& ("NominativePronoun".equals(label)
						|| "GenitivePronoun".equals(label)
						|| "AccusativePronoun".equals(label))
					) {
					Element theName = doc.createElement("token");
					String type = ((Element)socketElement).getAttribute("Type");
					if ("Actor".equals(type) || "DirObject".equals(type) || "Subject".equals(type)) {
						theName.setAttribute("Label", "This"+getWordsocketName((Element)socketElement));
						theName.setAttribute("Arg", "");
						tokElement.appendChild(theName);
					} else {
						theName.setAttribute("Label", "TheName");
						tokElement.getParentNode().replaceChild(theName,tokElement);
					}
				}
			}	
		}
	
		return doc;
	}
	
	private String getWordsocketName(Element socketElement) {
		final int wi = Integer.parseInt(socketElement.getAttribute("Index"));
		if (wi==0)
			return "Subject";
		else if (wi==1)
			return "Verb";
		
		final String type = socketElement.getAttribute("Type");
		if ("DirObject".equals(type))
			return "DirObject";
		else 
			return (wi+1)+type;
	} 
	
}
