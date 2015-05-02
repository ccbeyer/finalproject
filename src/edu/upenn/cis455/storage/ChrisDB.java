package edu.upenn.cis455.storage;

import java.util.ArrayList;

public class ChrisDB {
	
	public static ArrayList<Channel> channels;
	static ArrayList<XMLFile> xmlFiles;
	
	public ChrisDB() {
		channels = new ArrayList<Channel>();
		xmlFiles = new ArrayList<XMLFile>();
	}
	
	public static void addXMLFile(XMLFile file) {
		if (xmlFiles == null) {
			xmlFiles = new ArrayList<XMLFile>();
		}
		xmlFiles.add(file);
	}
	
	public static void addChannel(Channel channel) {
		if (channels == null) {
			channels = new ArrayList<Channel>();
		}
		channels.add(channel);
	}
	
	public static void printAllXmlUrls() {
		for (XMLFile file : xmlFiles) {
			System.out.println(file.location);
		}
	}
	
	
	
	
	

}
