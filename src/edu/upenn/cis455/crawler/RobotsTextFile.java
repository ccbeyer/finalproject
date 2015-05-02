package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class RobotsTextFile {
	
	private BufferedReader input;
	private ArrayList<String> disallowed;
	private boolean allowAll;
	
	public RobotsTextFile(BufferedReader input) {
		this.input = input;
		disallowed = new ArrayList<String>();
		parse();
		
	}
	
	private void parse() {
		String line;
		try {
			line = readLine();
			while ((line != null)) {
			    if (line.equals("User-agent: cis455crawler")) { //line.equals("User-agent: *")
			    	do {
				    	line = readLine();
				    	
				    	if (line.startsWith("Disallow: ")) {
				    		if (line.equals("Disallow: ")) {
				    			//allow all
				    			allowAll = true;
				    			return;
				    		}
				    	
				    		String path = line.substring(10);
				    		disallowed.add(path);
				    	}
			    	} while (!line.startsWith("User-agent:"));
			    } else {
			    	line = readLine();
			    }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String readLine() throws IOException {
		String line;
		try {
			line = input.readLine();
			while (line.startsWith("#")) {
	    		line = input.readLine();
	    	}
		} catch (IOException e) {
			return null;
		} catch (NullPointerException e) {
			return null;
		}
		return line;
	}
	
	public ArrayList<String> getDisallowed() {
		return disallowed;
	}
	
	public boolean isAllowAll() {
		return allowAll;
	}

}
