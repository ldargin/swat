package com.storytron.uber.deiktotrans;

import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.storytron.uber.Deikto;

/** The main upgrade here is a change in the way in 
 * which P2NotMine_Mine, P2Worthless_Valuable and KnowsMe traits are loaded.
 * */
public final class Transform919 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.919";	}

	public Document transform(Document doc) {
		// Change the shape of props in the propset.
		NodeList stageList = doc.getElementsByTagName("propSet").item(0).getChildNodes();
		for (int i=0; i<stageList.getLength(); i++) {
			Node current = stageList.item(i);
			String tag = current.getNodeName();
			if (current.getNodeType() == Element.ELEMENT_NODE &&
				"prop".equals(tag)) {
					NodeList propChildren = current.getChildNodes();
					LinkedList<Node> wv=new LinkedList<Node>();
					LinkedList<Node> nm=new LinkedList<Node>();
					LinkedList<Node> ks=new LinkedList<Node>();
					LinkedList<Node> att=new LinkedList<Node>();
					for(int j=0;j<propChildren.getLength();j++){
						if (propChildren.item(j).getNodeType()!=Element.ELEMENT_NODE) continue;
						
						String name=propChildren.item(j).getNodeName();
						if (name.equals("p2Worthless_Valuable"))
							wv.add(propChildren.item(j));
						else if (name.equals("p2NotMine_Mine"))
							nm.add(propChildren.item(j));
						else if (name.equals("knowsMe"))
							ks.add(propChildren.item(j));
						else if (name.equals("attribute"))
							att.add(propChildren.item(j));
					}
					if (!wv.isEmpty()){
						Element wve=doc.createElement("p2Worthless_Valuable");
						current.appendChild(wve);
						for(Node n:wv){
							current.removeChild(n);
							NodeList propGrandChildren=n.getChildNodes();
							wve.appendChild(propGrandChildren.item(Deikto.nextElement(propGrandChildren,-1)));
						}
					}
					if (!nm.isEmpty()){
						Element nme=doc.createElement("p2NotMine_Mine");
						current.appendChild(nme);
						for(Node n:nm){
							current.removeChild(n);
							NodeList propGrandChildren=n.getChildNodes();
							nme.appendChild(propGrandChildren.item(Deikto.nextElement(propGrandChildren,-1)));
						}
					}
					if (!ks.isEmpty()){
						Element kse=doc.createElement("knowsMe");
						current.appendChild(kse);
						for(Node n:ks){
							current.removeChild(n);
							NodeList propGrandChildren=n.getChildNodes();
							kse.appendChild(propGrandChildren.item(Deikto.nextElement(propGrandChildren,-1)));
						}
					}
					for(Node n:att)
						current.appendChild(n);
				}
		}
		return doc;
	}
}
