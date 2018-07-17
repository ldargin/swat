package com.storytron.uber.deiktotrans;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


/** 
 * Removes uses of p2Worthless_Valuable by replacing it with a 
 * custom prop trait Worthless_Valuable.
 */
public class Transform922  implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.922";	}

	public Document transform(Document doc) {

		// Find uses of P2Worthless_Valuable
		boolean found = false;
		for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "token")){
			String name=att.getAttributes().getNamedItem("Label").getNodeValue();
			if ("P2Worthless_Valuable".equals(name)){
				Node newAtt=doc.createAttribute("Label");
				newAtt.setNodeValue("PWorthless_Valuable");
				att.getAttributes().setNamedItem(newAtt);
				found = true;
			}
		}
		
		LinkedList<Node> p2wvList = DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("propSet").item(0), "p2Worthless_Valuable");

		if (found || p2wvList.iterator().hasNext()) {
			// Add Worthless_Valuable trait.
			Node propTraits = doc.getElementsByTagName("propTraits").item(0);
			propTraits.appendChild(createTraitElement(doc, "Worthless_Valuable"));
			
			// Collect prop list
			LinkedList<String> props=new LinkedList<String>();
			for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("propSet").item(0), "prop"))
				props.add(att.getAttributes().getNamedItem("Label").getNodeValue());

			// Collect actor pValues nodes
			Map<String,Node> actors=new TreeMap<String,Node>();
			for(Node pValues : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("pValueSet").item(0), "pValues"))
				actors.put(pValues.getAttributes().getNamedItem("OfWhom").getNodeValue(),pValues);

			// remove p2Worthless_Valuable elements.
			for(Node p2wv : p2wvList){
				// Insert each ofWhom value in the pValues of the corresponding actor
				insertAboutWhat(doc,p2wv,props,actors);
				p2wv.getParentNode().removeChild(p2wv);
			}			
		}

		return doc;
	}
	
	private Element createTraitElement(Document doc,String n){
		Element elem = doc.createElement("attribute");
		elem.setAttribute("Label", n);
		return elem;
	}
	
	private void insertAboutWhat(Document doc,Node p2wv,List<String> props,Map<String,Node> actors){
		for(Node ofWhom : DeiktoLoader.getNodesByTagName(p2wv, "ofWhom")){
			String propName=ofWhom.getParentNode().getParentNode().getAttributes().getNamedItem("Label").getNodeValue();
			String actorName=ofWhom.getAttributes().getNamedItem("Label").getNodeValue();
			String value=ofWhom.getTextContent();
		
			int iProp = props.indexOf(propName);
			Iterator<Node> it = DeiktoLoader.getNodesByTagName(actors.get(actorName), "AboutWhat").iterator();
			do {
				Node aboutWhat = null;
				int p;
				if (it.hasNext()){ // Is there and AboutWhat.
					aboutWhat = it.next();
					p = props.indexOf(aboutWhat.getAttributes().getNamedItem("Label").getNodeValue());
				} else
					p = props.size();
				
				if (iProp<p) { // Create new AboutWhat element
					Element newAboutWhat = doc.createElement("AboutWhat");
					newAboutWhat.setAttribute("Label", propName);
					if (aboutWhat!=null) // Insert before current aboutWhat
						aboutWhat.getParentNode().insertBefore(newAboutWhat,aboutWhat);
					else { // Insert before the first AboutWhere
						Iterator<Node> whereIt = DeiktoLoader.getNodesByTagName(actors.get(actorName), "AboutWhere").iterator();
						if (whereIt.hasNext())
							actors.get(actorName).insertBefore(newAboutWhat, whereIt.next());
						else 
							actors.get(actorName).appendChild(newAboutWhat);
					}
					aboutWhat = newAboutWhat;
					p = iProp;
				}
				if (iProp==p) { // Insert value in existing AboutWhat element
					Element attr = doc.createElement("attribute");
					attr.setAttribute("Name", "pWorthless_Valuable");
					attr.setTextContent(value);
					// Search first confidence value
					Node catt = null;
					for(Node att:DeiktoLoader.getNodesByTagName(aboutWhat,"attribute"))
						if (att.getAttributes().getNamedItem("Name").getNodeValue().startsWith("c")){
							catt = att;
							break;
						}
					aboutWhat.insertBefore(attr,catt);
					
					attr = doc.createElement("attribute");
					attr.setAttribute("Name", "cWorthless_Valuable");
					attr.setTextContent("0.99");
					aboutWhat.appendChild(attr);
					break;
				}
			} while(true);
		}
	}
}