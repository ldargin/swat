package com.storytron.uber.deiktotrans;

import java.util.LinkedList;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** The main upgrade here is a change in the way in 
 * which custom traits are loaded.
 * */
public final class Transform918 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.918";	}

	public Document transform(Document doc) {
		// Change the shape of stage custom traits.
		NodeList stageList = doc.getElementsByTagName("stageSet").item(0).getChildNodes();
		for (int i=0; i<stageList.getLength(); i++) {
			Node current = stageList.item(i);
			String tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE)
				if ("stage".equals(tag)) {
					NodeList stageChildren = current.getChildNodes();
					LinkedList<Node> aux=new LinkedList<Node>();
					for(int j=0;j<stageChildren.getLength();j++){
						if (stageChildren.item(j).getNodeType()!=Element.ELEMENT_NODE) continue;
						
						String name=stageChildren.item(j).getNodeName();
						if (!(name.equals("description") || name.equals("doorOpen") ||
								name.equals("population") || name.equals("owner") ||
								name.equals("xCoord") || name.equals("yCoord") ||
								name.equals("unwelcoming_Homey") || name.equals("knowsMe"))){
							aux.add(stageChildren.item(j));
						}
					}
					for(Node n:aux){
						Element newElement = current.getOwnerDocument().createElement("attribute"); 
						newElement.setAttribute("Name", n.getNodeName());
						newElement.setTextContent(n.getChildNodes().item(0).getNodeValue());
						current.removeChild(n);
						current.appendChild(newElement);
					}
				}
		}

		// Change the shape of prop custom traits.
		NodeList propList = doc.getElementsByTagName("propSet").item(0).getChildNodes();
		for (int i=0; i<propList.getLength(); i++) {
			Node current = propList.item(i);
			String tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE)
				if ("prop".equals(tag)) {
					NodeList propChildren = current.getChildNodes();
					LinkedList<Node> aux=new LinkedList<Node>();
					for(int j=0;j<propChildren.getLength();j++){
						if (propChildren.item(j).getNodeType()!=Element.ELEMENT_NODE) continue;
						
						String name=propChildren.item(j).getNodeName();
						if (!(name.equals("description") || name.equals("carried") ||
								name.equals("visible") || name.equals("owner") ||
								name.equals("inPlay") || name.equals("location") ||
								name.equals("p2Worthless_Valuable") || 
								name.equals("p2NotMine_Mine") || 
								name.equals("knowsMe"))){
							aux.add(propChildren.item(j));
						}
					}
					for(Node n:aux){
						Element newElement = doc.createElement("attribute"); 
						newElement.setAttribute("Name", n.getNodeName());
						newElement.setTextContent(n.getChildNodes().item(0).getNodeValue());
						current.removeChild(n);
						current.appendChild(newElement);
					}
				}
		}
		return doc;
	}
}
