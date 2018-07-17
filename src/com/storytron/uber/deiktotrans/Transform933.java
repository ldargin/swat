package com.storytron.uber.deiktotrans;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.storytron.uber.Sentence;


/** 
 * Replaces Acceptable operators by its group versions.
 */
public class Transform933 implements DeiktoLoader.DocumentTransformation {
	public String getVersion() { return "0.933";	}

	public Document transform(Document doc) {
		Map<String,Map<String,String>> verbSocketTypes = new HashMap<String, Map<String,String>>();
		// collect wordsockets
		for(Node verbElement : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "verb")) {
			NodeList socketList=((Element)verbElement).getElementsByTagName("socket");
			String verbLabel = verbElement.getAttributes().getNamedItem("Label").getNodeValue();
			Map<String,String> socketTypes = new TreeMap<String,String>();
			verbSocketTypes.put(verbLabel,socketTypes);
			for(int i=0;i<socketList.getLength();i++) {
				String index = socketList.item(i).getAttributes().getNamedItem("Index").getNodeValue();
				String typeString = socketList.item(i).getAttributes().getNamedItem("Type").getNodeValue();
				socketTypes.put(index,typeString);
			}
		}
		// modify acceptable scripts
		for(Node optionElement : DeiktoLoader.getNodesByTagName(doc.getElementsByTagName("categorySet").item(0), "option")) {
			String optionLabel = optionElement.getAttributes().getNamedItem("Label").getNodeValue();
			Map<String,String> socketTypes = verbSocketTypes.get(optionLabel);
			for(Node s:DeiktoLoader.getNodesByTagName(optionElement,"socketSpecs")) {
				String index = s.getAttributes().getNamedItem("Index").getNodeValue();
				String typeString = socketTypes.get(index);
				String type = Sentence.getTypeFromLabel(typeString).toString();
				Element acceptableScript=(Element)s.getChildNodes().item(1);
				Element acceptableNode = (Element)acceptableScript.getElementsByTagName("token").item(0);
				
				Element allWordsWhich = doc.createElement("token");
				if (type.equals("Actor"))
					allWordsWhich.setAttribute("Label", "All"+type+"sWho");
				else
					allWordsWhich.setAttribute("Label", "All"+type+"sWhich");
				allWordsWhich.appendChild(acceptableNode.getElementsByTagName("token").item(0));
				acceptableNode.appendChild(allWordsWhich);
				acceptableNode.setAttribute("Label", "Acceptable"+type+"s");
			};
		}

		return doc;
	}
}
