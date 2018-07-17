package com.storytron.uber.deiktotrans;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/** 
 * Renames inclination scripts to desirable, and creates dummy acceptable scripts.
 */
public class Transform935 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.935";	}

	public Document transform(Document doc) {
		NodeList l = doc.getElementsByTagName("categorySet");
		if (l.getLength()==0)
			return doc;
		
		for(Node optionElement : DeiktoLoader.getNodesByTagName(l.item(0), "option")) {
			Node scriptElement = getNodeByTag(optionElement.getChildNodes(),"script");
			Node inclinationElement = getNodeByTag(scriptElement.getChildNodes(),"token");
			((Element)inclinationElement).setAttribute("Label", "Desirable");
			
			Element desirableScript = doc.createElement("desirable");
			desirableScript.appendChild(inclinationElement);

			Element trueElement = doc.createElement("token");
			trueElement.setAttribute("Arg", "");
			trueElement.setAttribute("Label", "true");

			Element optionAcceptable = doc.createElement("token");
			optionAcceptable.setAttribute("Arg", "");
			optionAcceptable.setAttribute("Label", "OptionAcceptable");
			optionAcceptable.appendChild(trueElement);
			
			Element acceptableScript = doc.createElement("acceptable");
			acceptableScript.appendChild(optionAcceptable);
			
			optionElement.replaceChild(desirableScript,scriptElement);
			optionElement.insertBefore(acceptableScript,desirableScript);
		}

		return doc;
	}
	
	public Node getNodeByTag(NodeList l,String tag){
		for(int i=0;i<l.getLength();i++) {
			if (l.item(i).getNodeName().equals(tag))
				return l.item(i); 
		}
		return null;
	}
}
