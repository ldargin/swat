package com.storytron.uber.deiktotrans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class provides static methods for loading a dictionary with
 * an old format.
 * <p>
 * It works by applying reading a xml data stream into a {@link Document},
 * and applies to it a list of transformations that will hopefully bring
 * the Document to the format expected by the current Deikto implementation.
 * <p>
 * Whenever the format changes, a new transformation must be specified
 * to bring current storyworlds to the new format version.
 * <p> 
 * If you are a programmer needing to modify the current format, 
 * you may want to specify the conversion that existing documents
 * need in order to be loaded after modifying the format. This is done 
 * by implementing the {@link DocumentTransformation} interface and 
 * registering the implementation in the list of available 
 * transformations ({@link #trs}).
 * You will need to update, then, the {@link #dictionaryCurrentVersion}
 * so the new format can be distinguished from the previous ones.
 * </p> 
 * */
public class DeiktoLoader {
	/** This is the most up to date version. Change it as needed when
	 * you create new formats. */
	public static final String dictionaryCurrentVersion = "0.939";

	/** Implement this interface to define a new transformation. */
	public interface DocumentTransformation {
		/** Version that this transformation upgrades from. */
		public String getVersion();
		/** 
		 * Applies the transformation to the given document.
		 * This method should be applied only on documents that
		 * are in the format version specified by {@link #getVersion()}.
		 * The returned {@link Document} is the one given as parameter,
		 * probably with some modifications. 
		 * */
		public Document transform(Document doc);
	}
	
	/** This is the list of available transformations. */
	private static final LinkedList<DocumentTransformation> trs=new LinkedList<DocumentTransformation>();
	/** Here you must put your new transformation (at the end of the list of calls). */
	static {
		trs.add(new Transform918());
		trs.add(new Transform919());
		trs.add(new Transform920());
		trs.add(new Transform921());
		trs.add(new Transform922());
		trs.add(new Transform923());
		trs.add(new Transform924());
		trs.add(new Transform925());
		// Dummy transformation to compensate for shift in version numbers
		// without actually changing the format (from "0.926" to "0.930")
		trs.add(new DocumentTransformation(){
			public String getVersion() { return "0.926"; }
			public Document transform(Document doc) {	return doc;	}
		});
		trs.add(new Transform930());
		trs.add(new Transform931());
		trs.add(new Transform932());
		trs.add(new Transform933());
		trs.add(new Transform934());
		trs.add(new Transform935());
		trs.add(new Transform936());
		trs.add(new Transform937());
		trs.add(new Transform938());
	}
	
	/** Loads a dictionary from a file. Applying format conversions as needed. */
	public static Document loadDictionary(String filePath) throws BadVersionException, SAXException, IOException  {
		filePath = filePath.replace('/', File.separatorChar);
		return loadStoryworldXML(new FileInputStream(filePath));
	}

	/** Loads a dictionary from an input stream. Applying format conversions as needed. */
	public static Document loadStoryworldXML(InputStream is) 
			throws BadVersionException, SAXException, IOException {
		DocumentBuilder builder = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
		try {
			builder = factory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document doc = null;
		doc = builder.parse(is);			
		
		
		Node thisNode = doc.getElementsByTagName("dictionaryVersion").item(0);
		String version = thisNode.getAttributes().getNamedItem("Label").getNodeValue();

		// Transform old format documents with the available transformations. 
		boolean found=false;
		for(DocumentTransformation tr:trs){
			found=found||tr.getVersion().equals(version);
			if (found) 
				doc=tr.transform(doc);
		}

		// If a transformation was not found, complain.
		if (!found && !dictionaryCurrentVersion.equals(version))
			throw new BadVersionException(version);
		
		return doc;	
	}

	/** This is used just for debugging purposes whenever a new transformation is written. 
	 * Please, do not delete it. */
	private static int i=0;
	public static void debugWriteDocument(Document doc){
		try{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

//			initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);

			String xmlString = result.getWriter().toString();
			File f = new File("pp"+i+".xml");
			FileWriter fw=new FileWriter(f);
			i++;
			fw.write(xmlString);
			fw.close();
			System.out.println("DeiktoLoader output written to: "+f.getName());
			} catch (Exception e){}
	}
	
	/** Search nodes of a given tag */
	public static LinkedList<Node> getNodesByTagName(Node root,String tagName) {
		LinkedList<Node> l = new LinkedList<Node>();
		collectNodes(l,root,tagName);
		return l;
	}
	private static void collectNodes(LinkedList<Node> l,Node root,String tagName){
		if (root.getNodeName().equals(tagName))
			l.add(root);
		NodeList nl = root.getChildNodes();
		for (int i=0;i<nl.getLength();i++)
			collectNodes(l, nl.item(i), tagName);
	}
	
	/** A class for exceptions when a given dictionary format version can not be loaded. */
	public static class BadVersionException extends Exception {
		private static final long serialVersionUID = 1L;
		public String version;
		private BadVersionException(String v){ 
			super("Dictionary version "+v+" can not be loaded."); 
			version=v; 
		}
	}
	
}
