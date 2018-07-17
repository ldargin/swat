package com.storytron.uber.deiktotrans;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/** 
 * Removes the OK verb.
 */
public class Transform937 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.937";	}

	public Document transform(Document doc) {

		// Delete OK verb references from scripts
		for(Node tokElement : DeiktoLoader.getNodesByTagName(doc, "token")) {
			Node labelNode = tokElement.getAttributes().getNamedItem("Label");
			Node argNode = tokElement.getAttributes().getNamedItem("Arg");
			if (labelNode!=null && "VerbConstant".equals(labelNode.getNodeValue())
								&& "OK".equals(argNode.getNodeValue())) {
				tokElement.getAttributes().getNamedItem("Label").setNodeValue("?Verb?");
				tokElement.getAttributes().getNamedItem("Arg").setNodeValue("?Verb?");
			}
		}		
		
		// Delete OK options
		// optIndexes.get(i)==k iff option i!=OK and k is its new index  
		Map<Integer,Integer> optIndexes = new HashMap<Integer,Integer>(); 
		{
			NodeList options = doc.getElementsByTagName("optionSet");
			if (options.getLength()>0) {
				int i=0;
				int removed=0;
				for(Node optElement : DeiktoLoader.getNodesByTagName(options.item(0), "option")) {
					if (optElement.getAttributes().getNamedItem("Label").getNodeValue().equals("OK")) {
						optElement.getParentNode().removeChild(optElement);
						removed++;
					} else
						optIndexes.put(i,i-removed);
					i++;
				}
			}
		}

		// Delete references to OK from roles
		{
			NodeList roles = doc.getElementsByTagName("roleSet");
			if (roles.getLength()>0) {
				for(Node optlinkElement : DeiktoLoader.getNodesByTagName(roles.item(0), "optionlink")) {
					Integer k = optIndexes.get(Integer.parseInt(optlinkElement.getAttributes().getNamedItem("Index").getNodeValue()));
					if (k==null)
						optlinkElement.getParentNode().removeChild(optlinkElement);
					else
						optlinkElement.getAttributes().getNamedItem("Index").setNodeValue(String.valueOf(k));
				}
			}
		}

		// Remove the verb OK
		{
			NodeList l = doc.getElementsByTagName("categorySet");
			if (l.getLength()==0)
				return doc;

			for(Node verbElement : DeiktoLoader.getNodesByTagName(l.item(0), "verb")) {
				if (verbElement.getAttributes().getNamedItem("Label").getNodeValue().equals("OK")) {
					verbElement.getParentNode().removeChild(verbElement);
					break;
				}
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
