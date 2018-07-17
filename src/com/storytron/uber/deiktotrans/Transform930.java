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
public class Transform930 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.930";	}

	public Document transform(Document doc) {
		// for each token node
		for(Node token : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "token")) {
			String label = token.getAttributes().getNamedItem("Label").getNodeValue();
			if ("ActorSum".equals(label)) {
				token.removeChild(getFirstChild(token,"token"));
				token.getAttributes().getNamedItem("Label").setNodeValue("Actor@Sum");
			} else if ("ActorCount".equals(label)) {
				token.getAttributes().getNamedItem("Label").setNodeValue("TotalActors");
			} else if ("PropCount".equals(label)) {
				token.getAttributes().getNamedItem("Label").setNodeValue("TotalProps");
			} else if ("StageCount".equals(label)) {
				token.getAttributes().getNamedItem("Label").setNodeValue("TotalStages");
			} else if ("GroupSum".equals(label)) {
				Node trait = getFirstChild(token,"token");
				token.removeChild(trait);
				token.getAttributes().getNamedItem("Label").setNodeValue("Actor@Sum");

				Element candidate = doc.createElement("token");
				candidate.setAttribute("Label", "CandidateActor");
				candidate.setAttribute("Arg","");

				Element ptrait = doc.createElement("token");
				ptrait.setAttribute("Label", "P"+trait.getAttributes().getNamedItem("Arg").getNodeValue());
				ptrait.setAttribute("Arg","");
				ptrait.insertBefore(candidate, null);
				ptrait.insertBefore(getFirstChild(token,"token"), null);
				
				token.insertBefore(ptrait, null);
			} else if ("GroupAverage".equals(label)) {
				Node trait = getFirstChild(token,"token");
				token.removeChild(trait);
				token.getAttributes().getNamedItem("Label").setNodeValue("Actor@Average");

				Element candidate = doc.createElement("token");
				candidate.setAttribute("Label", "CandidateActor");
				candidate.setAttribute("Arg","");

				Element ptrait = doc.createElement("token");
				ptrait.setAttribute("Label", "P"+trait.getAttributes().getNamedItem("Arg").getNodeValue());
				ptrait.setAttribute("Arg","");
				ptrait.insertBefore(candidate, null);
				ptrait.insertBefore(getFirstChild(token,"token"), null);
				
				token.insertBefore(ptrait, null);
			}
		}

		return doc;
	}

	private Node getFirstChild(Node n,String tag){
		NodeList children = n.getChildNodes();
		for(int i=0;i<children.getLength();i++) {
			if (tag.equals(children.item(i).getNodeName()))
				return children.item(i);
		}
		return null;
	}
}
