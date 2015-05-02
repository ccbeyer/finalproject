package edu.upenn.cis455.storage;

import java.util.ArrayList;

import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.DBWrapper;

public class DBTester {
	
	public static void main(String[] args) {
		
		DBWrapper.setup();
		ArrayList<Channel> newChannels = new ArrayList<Channel>();
		DBWrapper.put("channels", newChannels);
		
		
		/*
		DBWrapper.put("Chris", "Beyer");
		DBWrapper.put("Hello", "World");
		
		Channel c = new Channel();
		c.setName("ChrisChannel");
		DBWrapper.put("channel1", c);
		
		System.out.println(DBWrapper.get("Chris"));
		System.out.println(DBWrapper.get("Hello"));
		System.out.println(((Channel) DBWrapper.get("channel1")).name);
		*/
		
	}

}
