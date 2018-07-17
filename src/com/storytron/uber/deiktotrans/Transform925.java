package com.storytron.uber.deiktotrans;

import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/** 
 * Change knowsMe elements. ofWhom elements that are true can be removed as true is the default
 * value now. Missing ofWhom entries must be added with value false.
 * <p>
 * This is done for props and stages.
 */
public class Transform925 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.925";	}

	@SuppressWarnings("unchecked")
	public Document transform(Document doc) {
		LinkedList<String> actors=new LinkedList<String>();
		// collect all actor names
		for(Node actor : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("actorSet").item(0), "actor"))
			actors.add(actor.getAttributes().getNamedItem("Label").getNodeValue());
		
		// Find knowsMe element of stages
		for(Node knowsMe : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("stageSet").item(0), "knowsMe")){
			LinkedList<String> actorsNotDefined=(LinkedList<String>)actors.clone();
			// find the ofWhom
			for(Node ofWhom : DeiktoLoader.getNodesByTagName(knowsMe, "ofWhom")){
				// remove true ofWhom's, they are defaults now. 
				if (ofWhom.getTextContent().equals("true"))
					actorsNotDefined.remove(ofWhom.getAttributes().getNamedItem("Label").getNodeValue());
				ofWhom.getParentNode().removeChild(ofWhom);
			}
			// add missing ofWhom entries
			for(String actor:actorsNotDefined) {
				Element ofWhom = doc.createElement("ofWhom");
				ofWhom.setAttribute("Label", actor);
				ofWhom.setTextContent("false");
				knowsMe.insertBefore(ofWhom, null);
			}
		}
		
		// Find knowsMe element of props
		for(Node knowsMe : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("propSet").item(0), "knowsMe")){
			LinkedList<String> actorsNotDefined=(LinkedList<String>)actors.clone();
			// find the ofWhom
			for(Node ofWhom : DeiktoLoader.getNodesByTagName(knowsMe, "ofWhom")){
				// remove true ofWhom's, they are defaults now. 
				if (ofWhom.getTextContent().equals("true"))
					actorsNotDefined.remove(ofWhom.getAttributes().getNamedItem("Label").getNodeValue());
				ofWhom.getParentNode().removeChild(ofWhom);
			}
			// add missing ofWhom entries
			for(String actor:actorsNotDefined) {
				Element ofWhom = doc.createElement("ofWhom");
				ofWhom.setAttribute("Label", actor);
				ofWhom.setTextContent("false");
				knowsMe.insertBefore(ofWhom, null);
			}
		}

		return doc;
	}

}
