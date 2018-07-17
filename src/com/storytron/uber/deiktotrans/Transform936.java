package com.storytron.uber.deiktotrans;

import java.util.ArrayList;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/** 
 * Renames assumeRoleIf scripts to use the tag "assumeRoleIf".
 */
public class Transform936 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.936";	}

	public Document transform(Document doc) {
		NodeList l = doc.getElementsByTagName("categorySet");
		if (l.getLength()==0)
			return doc;
		
		for(Node roleElement : DeiktoLoader.getNodesByTagName(l.item(0), "role")) {
			Node scriptElement = getNodeByTag(roleElement.getChildNodes(),"script");
			Element assumeRoleIfElement = doc.createElement("assumeRoleIf");
			
			NodeList children = scriptElement.getChildNodes();
			ArrayList<Node> c = new ArrayList<Node>(children.getLength());
			for(int i=0;i<children.getLength();i++) 
				c.add(children.item(i));
			for(Node n:c)
				assumeRoleIfElement.appendChild(n);
			
			NamedNodeMap atts = scriptElement.getAttributes();
			for(int i=0;i<atts.getLength();i++)
				assumeRoleIfElement.setAttributeNode((Attr)atts.item(i));
					
			roleElement.replaceChild(assumeRoleIfElement,scriptElement);
		}

		{
			Element optionSet = doc.createElement("optionSet");
			l.item(0).getParentNode().insertBefore(optionSet, l.item(0));
			int i=0;
			for(Node optionElement : DeiktoLoader.getNodesByTagName(l.item(0), "option")) {
				Element optionlink = doc.createElement("optionlink");
				optionlink.setAttribute("Index", String.valueOf(i));
				optionElement.getParentNode().replaceChild(optionlink, optionElement);

				optionSet.appendChild(optionElement);
				i++;
			}
		}

		{
			Element roleSet = doc.createElement("roleSet");
			l.item(0).getParentNode().insertBefore(roleSet, l.item(0));
			int i=0;
			for(Node roleElement : DeiktoLoader.getNodesByTagName(l.item(0), "role")) {
				Element rolelink = doc.createElement("rolelink");
				rolelink.setAttribute("Label", roleElement.getAttributes().getNamedItem("Label").getNodeValue());
				rolelink.setAttribute("Index", String.valueOf(i));
				roleElement.getParentNode().replaceChild(rolelink, roleElement);

				roleElement.getAttributes().removeNamedItem("Label");
				roleSet.appendChild(roleElement);
				i++;
			}
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
