package edu.upenn.cis455.crawler.info;

import java.util.ArrayList;
import java.util.HashMap;

import com.sleepycat.persist.model.Persistent;

@Persistent
public class RobotsTxtInfo {
	
	private HashMap<String,ArrayList<String>> disallowedLinks;
	private HashMap<String,ArrayList<String>> allowedLinks;
	
	private HashMap<String,Integer> crawlDelays;
	private ArrayList<String> sitemapLinks;
	private ArrayList<String> userAgents;
	
	private final static int DEFAULT_DELAY = 5000;
	
	public RobotsTxtInfo(){
		disallowedLinks = new HashMap<String,ArrayList<String>>();
		allowedLinks = new HashMap<String,ArrayList<String>>();
		crawlDelays = new HashMap<String,Integer>();
		sitemapLinks = new ArrayList<String>();
		userAgents = new ArrayList<String>();
	}
	
	public static RobotsTxtInfo parseRobotsTxtString(String contents)
	{
		RobotsTxtInfo result = new RobotsTxtInfo();
		
		String currentUserAgent = null;
		
		// Split the string into new lines
		String lines[] = contents.split("\\r?\\n");
		
		for (String line : lines)
		{

			// If the line is empty, assume it is delimiting two user agent sections, so reset the user agent
			if (line.trim().isEmpty())
			{
				currentUserAgent = null;
				continue;
			}
			

			// Comments start with "#"
			if (line.trim().startsWith("#"))
				continue;
			
			// Split each line into key value pair
			String pair[] = line.split(":", 2);
			
			
			// A line may not be a key value pair so skip it
			if (pair.length < 2)
				continue;
			
			// Get the key and value, removing whitespace and lowercasing the keys
			String key = pair[0].trim().toLowerCase();
			String value = pair[1].trim();
			
			// User-agent, add to the user agents array and set the current value
			if (key.equals("user-agent"))
			{
				currentUserAgent = value;
				result.addUserAgent(value);
			}
			// Disallowed, only process if theres an associated user agent and a non-empty string
			else if (key.equals("disallow") && currentUserAgent != null && !value.isEmpty())
			{
				result.addDisallowedLink(currentUserAgent, value);
			}
			// Allowed, same as disallowed
			else if (key.equals("allow") && currentUserAgent != null && !value.isEmpty())
			{
				result.addAllowedLink(currentUserAgent, value);
			}
			// Crawl-delay, add only if it parses to an integer
			else if (key.equals("crawl-delay") && currentUserAgent != null)
			{
				try 
				{
					// Convert from seconds to milliseconds
					int parsedDelay = Integer.parseInt(value) * 1000;
					result.addCrawlDelay(currentUserAgent, parsedDelay);
				} 
				catch (NumberFormatException e)
				{
					// Do nothing if not an integer
				}
			}
			// Sitemap is not associated with a particular user agent
			else if (key.equals("sitemap"))
			{
				result.addSitemapLink(value);
			}
		}
		
		return result;
	}
	
	public void addDisallowedLink(String key, String value){
		if(!disallowedLinks.containsKey(key)){
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(value);
			disallowedLinks.put(key, temp);
		}
		else{
			ArrayList<String> temp = disallowedLinks.get(key);
			if(temp == null)
				temp = new ArrayList<String>();
			temp.add(value);
			disallowedLinks.put(key, temp);
		}
	}
	
	public void addAllowedLink(String key, String value){
		if(!allowedLinks.containsKey(key)){
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(value);
			allowedLinks.put(key, temp);
		}
		else{
			ArrayList<String> temp = allowedLinks.get(key);
			if(temp == null)
				temp = new ArrayList<String>();
			temp.add(value);
			allowedLinks.put(key, temp);
		}
	}
	
	public void addCrawlDelay(String key, Integer value){
		crawlDelays.put(key, value);
	}
	
	public void addSitemapLink(String val){
		sitemapLinks.add(val);
	}
	
	public void addUserAgent(String key){
		userAgents.add(key);
	}
	
	public boolean containsUserAgent(String key){
		return userAgents.contains(key);
	}
	
	public ArrayList<String> getDisallowedLinks(String key){
		return disallowedLinks.get(key);
	}
	
	public ArrayList<String> getAllowedLinks(String key){
		return allowedLinks.get(key);
	}
	
	public int getCrawlDelay(String key){
		return crawlDelays.containsKey(key) ? crawlDelays.get(key) : 
			crawlDelays.containsKey("*") ? crawlDelays.get("*") : DEFAULT_DELAY;
	}
	
	public void print(){
		for(String userAgent:userAgents){
			System.out.println("User-Agent: "+userAgent);
			ArrayList<String> dlinks = disallowedLinks.get(userAgent);
			if(dlinks != null)
				for(String dl:dlinks)
					System.out.println("Disallow: "+dl);
			ArrayList<String> alinks = allowedLinks.get(userAgent);
			if(alinks != null)
					for(String al:alinks)
						System.out.println("Allow: "+al);
			if(crawlDelays.containsKey(userAgent))
				System.out.println("Crawl-Delay: "+crawlDelays.get(userAgent));
			System.out.println();
		}
		if(sitemapLinks.size() > 0){
			System.out.println("# SiteMap Links");
			for(String sitemap:sitemapLinks)
				System.out.println(sitemap);
		}
	}
	
	public boolean crawlContainAgent(String key){
		return crawlDelays.containsKey(key);
	}
	
	// Note: assumes the entries of robots.txt are ended with a trailing slash if a directory
	public boolean canCrawlUrl(String key, URLInfo url)
	{
		String path = url.getFilePath();
		String longestMatch = null;
		boolean isAllowed = true;
		
		// A little bit of a hack, just add a trailing slash so that directories match properly
		if (!path.endsWith("/"))
			path += "/";
		
		ArrayList<String> allowed, disallowed;
		
		// We search for our robot specifically first
		if (userAgents.contains(key))
		{
			allowed = allowedLinks.get(key);
			disallowed = disallowedLinks.get(key);
		}
		// Otherwise use default rules
		else
		{
			allowed = allowedLinks.get("*");
			disallowed = disallowedLinks.get("*");
		}
		
		// Get the longest rule from either set and record if allowed or not
		if (allowed != null)
		{
			for (String rule : allowed)
			{
				if (path.startsWith(rule))
				{
					if (longestMatch == null || longestMatch.length() < rule.length())
						longestMatch = rule;
				}
			}
		}
		
		if (disallowed != null)
		{
			for (String rule : disallowed)
			{
				if (path.startsWith(rule))
				{
					if (longestMatch == null || longestMatch.length() < rule.length())
					{
						longestMatch = rule;
						isAllowed = false;
					}
				}
			}
		}
		
		// Return true if no match, otherwise return the rule by longest match
		return (longestMatch == null) ? true : isAllowed;
	}


}
