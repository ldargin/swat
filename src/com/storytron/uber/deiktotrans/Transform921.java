package com.storytron.uber.deiktotrans;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class Transform921  implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.921";	}

	public Document transform(Document doc) {

		// Add predefined non visible traits.
		Node outerTraits = doc.getElementsByTagName("outerTraits").item(0);
		for(String trait:new String[]{ "CustomTrait", "Ugly_Attractive", "Stupid_Smart",
				"Meek_Bossy", "Nasty_Nice", "False_Honest", "Ascetic_Sensual" }) {
			outerTraits.insertBefore(createTraitElement(doc, trait), outerTraits.getFirstChild());
		}
		
		// Reorder trait list
		Node patt = null;
		for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("actorSet").item(0), "attribute")){
			String name=att.getAttributes().getNamedItem("Name").getNodeValue();
			String pname = patt==null? null: patt.getAttributes().getNamedItem("Name").getNodeValue();
			if (pname!=null)
				if (pname.startsWith("Ascetic_Sensual") && name.startsWith("Cool_Volatile") ||
					pname.startsWith("Ascetic_SensualWeight") && name.startsWith("Cool_VolatileWeight")	||
					pname.startsWith("accordAscetic_Sensual") && name.startsWith("accordCool_Volatile"))					
						att.getParentNode().insertBefore(att,patt);
			
			patt = att;
		}
		
		// Scale stage y coordinate.
		for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("stageSet").item(0), "yCoord")){
			float v = (1+Float.parseFloat(att.getTextContent()))*0.6f-1;
			att.setTextContent(Float.toString(v));
		}
		
		// Reorder trait list
		for(Node att : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("pValueSet").item(0), "attribute")){
			String name=att.getAttributes().getNamedItem("Name").getNodeValue();
			String pname = patt==null? null: patt.getAttributes().getNamedItem("Name").getNodeValue();
			if (pname!=null)
				if (pname.startsWith("pAscetic_Sensual") && name.startsWith("pCool_Volatile") ||
					pname.startsWith("cAscetic_Sensual") && name.startsWith("cCool_Volatile"))					
						att.getParentNode().insertBefore(att,patt);
			
			patt = att;
		}

		return doc;
	}
	
	private Element createTraitElement(Document doc,String n){
		Element elem = doc.createElement("attribute");
		elem.setAttribute("Label", n);
		elem.setAttribute("visible", "false");
		return elem;
	}
}
