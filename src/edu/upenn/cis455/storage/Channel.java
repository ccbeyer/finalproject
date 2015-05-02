package edu.upenn.cis455.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import edu.upenn.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis455.xpathengine.XPathEngineImpl;

public class Channel implements Serializable{
	
	String name;
	ArrayList<String> documents; 
	ArrayList<String> validXpaths;
	
	public Channel() {
		documents = new ArrayList<String>();
		validXpaths = new ArrayList<String>();
	}
	
	public void addValidXpath(String s) {
		validXpaths.add(s);
	}
	
	public boolean isValidXpath(String s) {
		return validXpaths.contains(s);
	}
	
	public ArrayList<String> getValidXpaths() {
		return validXpaths;
	}
	
	public ArrayList<String> getDocuments() {
		return documents;
	}
	
	public void setName(String s) {
		this.name = s;
	}
	
	public String getName() {
		return name;
	}
	
	
	public void addDocToChannel(String doc) {
		documents.add(doc);
	}
	
	public String getChannelString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<documentcollection>");
		for (String doc : documents) {
			sb.append(doc);
		}
		sb.append("</documentcollection>");
		
		
		return sb.toString();
	}
	
	public XPathEngineImpl createXPathEngine() {
		XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
		Object[] xpathObjects = validXpaths.toArray();
		String[] xpaths = new String[xpathObjects.length];
		for (int i = 0; i < xpathObjects.length; i++) {
			xpaths[i] = (String) xpathObjects[i];
		}
	
		xpathEngine.setXPaths(xpaths);
		
		return xpathEngine;
	}
	
	public boolean isValid(XMLFile file, XPathEngineImpl xpathEngine) throws IOException {
		if (xpathEngine == null) {
			createXPathEngine();
		}
		String xmlURL = file.location;
		URL url = new URL(xmlURL);
		InputStream is = url.openStream();
		File out = new File("output.txt");
	    FileOutputStream Fos = new FileOutputStream(out);
		Tidy t = new Tidy(); // obtain a new Tidy instance
		t.setXmlTags(true);
		t.setHideComments(true);
		t.setShowWarnings(false);
	
		Document d = t.parseDOM(is,Fos);
		
		boolean[] success = xpathEngine.evaluate(d);
		for (boolean b : success) {
			if (b)
				return true;
		}
		return false;
	}

}
