package edu.upenn.cis455.storage;

import java.io.Serializable;

import org.w3c.dom.Document;

public class XMLFile implements Serializable{
	
	String crawlTime;
	String location;
	String rawData;
	Document document;
	
	public XMLFile(String crawlTime, String location, String rawData, Document document) {
		this.crawlTime = crawlTime;
		this.location = location;
		this.document = document;
		if (rawData.startsWith("<?xml")) {
			this.rawData = rawData.substring(rawData.indexOf('>'));
		} else {
			this.rawData = rawData;
		}
	}
	
	
	public String getStringForChannel() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<document crawled=\"" + crawlTime + "\" location=\"" + location + "\">\n");
		sb.append(rawData + "\n");
		sb.append("</document>\n");
		
		return sb.toString();
	}

}
