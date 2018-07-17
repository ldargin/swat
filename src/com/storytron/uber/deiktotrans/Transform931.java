package com.storytron.uber.deiktotrans;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/** 
 * Replaces ActorSum and GroupSum with Actor@Sum and GroupAverage with Actor@Average.
 * Also renames ActorCount to TotalActors, PropCount to TotalProps and StageCount to
 * TotalStages.
 */
public class Transform931 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.931";	}

	public Document transform(Document doc) {
		// for each token node
		for(Node token : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "token")) {
			String label = token.getAttributes().getNamedItem("Label").getNodeValue();
			if (label.startsWith("AdjustP") && !label.equals("AdjustPActorTrait")) {
				token.getAttributes().getNamedItem("Label").setNodeValue("AdjustPActorTrait");

				Element trait = doc.createElement("token");
				trait.setAttribute("Label", "ActorTraitConstant");
				trait.setAttribute("Arg",label.substring(7));
				
				token.insertBefore(trait, getFirstChild(token,"token"));
			}
		}

		return doc;
	}
	
	private static Node getFirstChild(Node n,String tag){
		NodeList children = n.getChildNodes();
		for(int i=0;i<children.getLength();i++) {
			if (tag.equals(children.item(i).getNodeName()))
				return children.item(i);
		}
		return null;
	}

}
