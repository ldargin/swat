package com.storytron.uber.deiktotrans;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Transform920  implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.920";	}

	public Document transform(Document doc) {

		// Delete p1 prefixes of traits in the actor set.
		for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("actorSet").item(0), "attribute")){
			Node n=att.getAttributes().getNamedItem("Name");
			String name = n==null?null:n.getNodeValue();
			if (name!=null)
				if (name.startsWith("p1")){
					Node newAtt=doc.createAttribute("Name");
					newAtt.setNodeValue(name.substring(2));
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.startsWith("accord") || name.endsWith("Weight"))
					att.getParentNode().appendChild(att);
		}

		// Change p2 prefixes of traits in the actor set to p.
		// Change up2 prefixes of traits in the actor set to c.
		for(Node about : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("pValueSet").item(0), "AboutWhom")){
			NodeList atts = about.getChildNodes();
			for(int j=0;j<atts.getLength();j++){
				String name = atts.item(j).getNodeName();
				if (name.startsWith("p2")) {
					Node n = atts.item(j);
					Node newChild = doc.createElement("p"+name.substring(2));
					newChild.setTextContent(n.getTextContent());
					about.replaceChild(newChild, n);
				} else if (name.startsWith("up2")) {
					// reverse value
					float v = -1*Float.parseFloat(atts.item(j).getTextContent());
					atts.item(j).setTextContent(Float.toString(v));

					// change name
					Node n = atts.item(j);
					Node newChild = doc.createElement("c"+name.substring(3));
					newChild.setTextContent(n.getTextContent());
					about.replaceChild(newChild, n);
				}
			}
			
			// Change elements from <trait>value</trait> to 
			// <attribute Name="trait">value</attribute>.
			atts = about.getChildNodes();
			for(int j=0;j<atts.getLength();j++){
				Node item = atts.item(j);
				if (item.getNodeType()==Element.ELEMENT_NODE) {
					Element newItem = doc.createElement("attribute");
					newItem.setAttribute("Name", item.getNodeName());
					newItem.setTextContent(item.getTextContent());
					about.replaceChild(newItem, item);
				}
			}
		}
		
		// Change P1, P2 and UP2 prefixes in scripts.
		for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "token")){
			Node n=att.getAttributes().getNamedItem("Label");
			String name = n==null?null:n.getNodeValue();
				
			if (name!=null)
				if (name.startsWith("P1")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue(name.substring(2));
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.startsWith("P2") && !name.equals("P2Worthless_Valuable")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("P"+name.substring(2));
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.startsWith("UP2")){
					Element binverse = doc.createElement("token");
					binverse.setAttribute("Arg", "");
					binverse.setAttribute("Label", "BInverse");
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("C"+name.substring(3));
					att.getAttributes().setNamedItem(newAtt);
					att.getParentNode().replaceChild(binverse, att);
					binverse.appendChild(att);
				} else if (name.equals("AdjustP2ThisInnerTrait")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("AdjustPThisActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.startsWith("AdjustP2")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("AdjustP"+name.substring(8));
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.startsWith("CorrespondingP2")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("CorrespondingPActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.startsWith("CorrespondingUP2")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("CorrespondingCActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.startsWith("CorrespondingOuterTrait") || name.startsWith("CorrespondingP1")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("CorrespondingActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.equals("AreSameInnerTrait") || name.equals("AreSameOuterTrait")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("AreSameActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.equals("ChosenInnerTrait") || name.equals("ChosenOuterTrait")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("ChosenActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.equals("CandidateInnerTrait") || name.equals("CandidateOuterTrait")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("CandidateActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.equals("ThisInnerTrait") || name.equals("ThisOuterTrait")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("ThisActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.equals("PickBestInnerTrait") || name.equals("PickBestOuterTrait")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("PickBestActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				} else if (name.equals("PastInnerTrait") || name.equals("PastOuterTrait")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("PastActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				}
			}
		
		for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "wordSocket"))
			if ("InnerTrait".equals(att.getTextContent()) || "OuterTrait".equals(att.getTextContent()))
					att.setTextContent("ActorTrait");

		for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "wordSocketSpecs")){
			Node n=att.getAttributes().getNamedItem("Label");
			String name = n==null?null:n.getNodeValue();
			if (name!=null)
				if (name.equals("InnerTrait") || name.equals("OuterTrait")){
					Node newAtt=doc.createAttribute("Label");
					newAtt.setNodeValue("ActorTrait");
					att.getAttributes().setNamedItem(newAtt);
				}
		}

		return doc;
	}
}
