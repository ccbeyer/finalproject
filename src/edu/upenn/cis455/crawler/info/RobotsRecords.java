package edu.upenn.cis455.crawler.info;

import java.util.HashMap;

public class RobotsRecords {
	public static HashMap<String, RobotsTxtInfo> robotsTxtInfo;
	public static HashMap<String, Long> robotsLastModified;
	public static HashMap<String, Long> lastCrawled;
	
	public RobotsRecords() {
		robotsTxtInfo = new HashMap<String, RobotsTxtInfo>();
		robotsLastModified = new HashMap<String, Long>();
		lastCrawled = new HashMap<String, Long>();
	}
	
	public static void setup() {
		robotsTxtInfo = new HashMap<String, RobotsTxtInfo>();
		robotsLastModified = new HashMap<String, Long>();
		lastCrawled = new HashMap<String, Long>();
	}
	
	public static boolean crawlDelayPassed(String userAgent, String host) {
		if (!lastCrawled.containsKey(host)) {
			return true;
		}
		RobotsTxtInfo info = robotsTxtInfo.get(host);
		long delay = info.getCrawlDelay(userAgent);
		long now = System.currentTimeMillis();
		long lastTimeCrawled = lastCrawled.get(host);
		if (now > lastTimeCrawled + delay) {
			return true;
		} else {
			return false;
		}
		
	}

}
