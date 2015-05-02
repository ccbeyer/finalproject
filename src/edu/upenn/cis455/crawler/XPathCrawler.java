package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.net.ssl.HttpsURLConnection;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

import edu.upenn.cis455.crawler.info.RobotsRecords;
import edu.upenn.cis455.crawler.info.RobotsTxtInfo;
import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.XMLFile;
import edu.upenn.cis455.xpathengine.XPathEngineImpl;


public class XPathCrawler {
	
	public final static String CRAWLER_USER_AGENT = "cis455crawler";
	
	static LinkedList<String> urlsToCrawl;
	static HashSet<String> visitedUrls;
	static double numFilesCrawled;
	static double numFilesToCrawl;
	static RobotsTxtInfo robotsTxtInfo;
	static double maxDocSize;
		
	public static void main(String[] args) {
		
		String startingURL = args[0];
		String databaseDirectory = args[1];
		DBWrapper.setDirectory(databaseDirectory);
		maxDocSize = Double.parseDouble(args[2]);
		if (args.length > 3) {
			numFilesToCrawl = Double.parseDouble(args[3]);
		} else {
			numFilesToCrawl = -1;
		}
		
		RobotsRecords.setup();
		urlsToCrawl = new LinkedList<String>();
		visitedUrls = new HashSet<String>();
		urlsToCrawl.add(startingURL);		
		robotsTxtInfo = new RobotsTxtInfo();
		
		crawl();
		
	}
	
	private static void crawl() {
		while (!urlsToCrawl.isEmpty() || (numFilesToCrawl != -1 && numFilesCrawled < numFilesToCrawl)) {
			//open connection
			String nextURL = urlsToCrawl.pop();
			if (visitedUrls.contains(nextURL)) {
				continue;
			}
			
			if (nextURL.startsWith("https")) {
				crawlHttps(nextURL);
			} else {
				crawlHttp(nextURL);
			}
			
			
			
			
			
			//send header
			//check for robots
			//get file
			//extract links
		}
		
	}
	
	private static void crawlHttp(String nextURL) {
		URLInfo nextURLInfo;
		nextURLInfo = new URLInfo(nextURL);
		
		/*
		ArrayList<String> disallowedLinks = robotsTxtInfo.getDisallowedLinks(nextURLInfo.getHostName());
		if (disallowedLinks != null && !isAllowed(disallowedLinks, nextURLInfo.getFilePath())) {
			return;
		}
		*/
		
		Socket myClient;
		BufferedReader input;
		BufferedWriter output;
		
		try {
			myClient = new Socket(nextURLInfo.getHostName(), nextURLInfo.getPortNo());
			input = new BufferedReader(new InputStreamReader(myClient.getInputStream()));
			output = new BufferedWriter(new OutputStreamWriter(myClient.getOutputStream(), "UTF8"));

			output.write("HEAD " + nextURLInfo.getFilePath() + " HTTP/1.0\r\n");
			output.write("User-Agent: cis455crawler\r\n");
			output.write("\r\n");
			output.flush();
			
			input.readLine(); 
			String line = input.readLine();
			HashMap<String, String> headers = new HashMap<String, String>();
			while (line != null && !line.equals("")) {
				int i = line.indexOf(":");
				if (i > 0) {
					//System.out.println(line);
					headers.put(line.substring(0, i).toLowerCase(), line.substring(i+2));
				} else break;
				line = input.readLine();
			}
			
			String contentType = headers.get("content-type");
			//check for valid file type
			if (contentType == null || !contentType.startsWith("text/html")) {
				return;
			}
			/*if (contentType == null || !(contentType.startsWith("text/xml") || contentType.startsWith("text/html") || contentType.startsWith("application/xml") || contentType.endsWith("+xml"))) {
				return;
			}*/
			
			//check if file is over max file size
			String contentLength = headers.get("content-length");
			if (contentLength == null || Double.parseDouble(contentLength) / 1000000 > maxDocSize) {
				return;
			}

			myClient = new Socket(nextURLInfo.getHostName(), nextURLInfo.getPortNo());
			input = new BufferedReader(new InputStreamReader(myClient.getInputStream()));
			output = new BufferedWriter(new OutputStreamWriter(myClient.getOutputStream(), "UTF8"));
				
			
			//check robots text file
			output.write("GET /robots.txt HTTP/1.0\r\n");
			output.write("User-Agent: cis455crawler\r\n");
			output.write("\r\n");
			output.flush();
			
			String robotsText = "";
			HashMap<String, String> robotsHeaders = new HashMap<String, String>();


			line = input.readLine();
			if (line.toLowerCase().startsWith("http")) {
				while ((line = input.readLine()) != null && !line.equals("") && 
						!( line.equals("!DOCTYPE") || line.toLowerCase().equals("<html>") ) ) {
					int i = line.indexOf(":");
					if (i > 0) {
						//System.out.println(line);
						robotsHeaders.put(line.substring(0, i).toLowerCase(), line.substring(i+2));
					} else break;
					line = input.readLine();
				}
			} else {
				robotsText += line;
			}
			
			
			
			String nextLine = input.readLine();
			while (nextLine != null) {
				robotsText += nextLine;
				robotsText += "\n";
				nextLine = input.readLine();
			}
			
			RobotsTxtInfo robotsTxtInfo;
			String lastModified = robotsHeaders.get("last-modified");
			if (RobotsRecords.robotsLastModified.get(nextURLInfo.getHostName()) != null &&
					convertDate(lastModified) <= RobotsRecords.robotsLastModified.get(nextURLInfo.getHostName()) ) {
				robotsTxtInfo = RobotsRecords.robotsTxtInfo.get(nextURLInfo.getHostName());
			} else {
				robotsTxtInfo = RobotsTxtInfo.parseRobotsTxtString(robotsText);
				RobotsRecords.robotsTxtInfo.put(nextURLInfo.getHostName(), robotsTxtInfo);
				RobotsRecords.robotsLastModified.put(nextURLInfo.getHostName(), convertDate(lastModified));
			}
			
			//RobotsTextFile robotsTextFile = new RobotsTextFile(input);
			
			
			// Check if we can crawl it
			System.out.println(robotsTxtInfo.canCrawlUrl(CRAWLER_USER_AGENT, nextURLInfo));
			if (!robotsTxtInfo.canCrawlUrl(CRAWLER_USER_AGENT, nextURLInfo)) {
				System.out.printf("Not allowed to crawl path \"%s\" on \"%s\" by robots.txt.\n", nextURLInfo.getFilePath(), nextURLInfo.getHostName());
				return;
			}
			
			if (!RobotsRecords.crawlDelayPassed(CRAWLER_USER_AGENT, nextURLInfo.getHostName())) {
				urlsToCrawl.add(nextURL);
				return;
			} else {
				RobotsRecords.lastCrawled.put(nextURLInfo.getHostName(), System.currentTimeMillis());
			}
			
			/*
			if (robotsTextFile.isAllowAll()) {
				robotsTxtInfo.addAllowedLink(nextURLInfo.getHostName(), "/");
			} else {
				ArrayList<String> disallowed = robotsTextFile.getDisallowed();
				for (int i = 0; i < disallowed.size(); i++) {
					robotsTxtInfo.addDisallowedLink(nextURLInfo.getHostName(), disallowed.get(i));

				}
				if (disallowed.contains("/") || !isAllowed(disallowed, nextURLInfo.getFilePath())) {
					return;
				}
			}
			*/
			
			myClient = new Socket(nextURLInfo.getHostName(), nextURLInfo.getPortNo());
			input = new BufferedReader(new InputStreamReader(myClient.getInputStream()));
			output = new BufferedWriter(new OutputStreamWriter(myClient.getOutputStream(), "UTF8"));
			
			numFilesCrawled++;
			visitedUrls.add(nextURL);
			System.out.println("Crawling: " + nextURL);
			
			
			output.write("GET " + nextURLInfo.getFilePath() + " HTTP/1.0\r\n");
			output.write("User-Agent: cis455crawler\r\n");
			output.write("\r\n");
			output.flush();
			
			StringBuilder sb = new StringBuilder();
			while ((line = input.readLine()) != null && 
					!( line.equals("!DOCTYPE") || line.toLowerCase().equals("<html>") ) ) {
				
			}
			sb.append(line);
			while ((line = input.readLine()) != null) {
	            sb.append(line);
	            sb.append("\n");
	        }
			String htmlString = sb.toString();
			InputStream inputStream = new ByteArrayInputStream(htmlString.getBytes(Charset.forName("UTF-8")));
			
			File out = new File("output.txt");
		    FileOutputStream Fos = new FileOutputStream(out);
		    Tidy t = new Tidy(); // obtain a new Tidy instance
			t.setQuiet(true);
			t.setShowWarnings(false);
			Document d;
			
			/*
			if (nextURL.endsWith(".xml")) {
				d = null;
				Date date = new Date();
				
				XMLFile file = new XMLFile(date.toString(), nextURL, htmlString, null);
				
				DBWrapper.put(nextURL, file);

				ArrayList<Channel> channels = (ArrayList<Channel>) DBWrapper.get("channels");
				if (channels != null) {
					ArrayList<Channel> newChannels = new ArrayList<Channel>();
					for (Channel c : channels) {
						XPathEngineImpl xpathEngineImpl = c.createXPathEngine();
						if (c.isValid(file, xpathEngineImpl)) {
							c.addDocToChannel(file.getStringForChannel());
						}
						newChannels.add(c);
					}
					System.out.println("Channels is null? " + newChannels.isEmpty());
					DBWrapper.put("channels", newChannels);
				}
			} else {
				d = t.parseDOM(inputStream, Fos);
			}
			*/
			
			d = t.parseDOM(inputStream, Fos);
			try {
				//if (d != null && d.getDoctype().getNodeName().startsWith("html")) {
					NodeList links = d.getElementsByTagName("a");
					for (int i = 0; i < links.getLength(); i++) {
						System.out.println("--Adding to queue: " + links.item(i).getAttributes().getNamedItem("href").getNodeValue());
						String newLink = links.item(i).getAttributes().getNamedItem("href").getNodeValue();
						if (newLink.startsWith("http")) {
							urlsToCrawl.add(newLink);
						} else {
							String urlPath;
							if (nextURL.endsWith(".html")) {
								int j = nextURL.length()-1;
								while (nextURL.charAt(j) != '/') {
									j--;
								}
								urlPath = nextURL.substring(0, j+1);
							} else {
								if (!nextURL.endsWith("/")) {
									urlPath = nextURL + "/";
								} else {
									urlPath = nextURL;
								}
							}
							urlsToCrawl.add(urlPath + newLink);
						}
						
					//}
				}
			} catch (NullPointerException e) {
				System.err.println("null pointer exception parsing doc");
				e.printStackTrace();
			}
			
	
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void crawlHttps(String nextURL) {

			URLInfo nextURLInfo = new URLInfo(nextURL, true);

			
			/*ArrayList<String> disallowedLinks = robotsTxtInfo.getDisallowedLinks(nextURLInfo.getHostName());
			if (disallowedLinks != null && !isAllowed(disallowedLinks, nextURLInfo.getFilePath())) {
				return;
			}*/
			
			Socket myClient;
			BufferedReader input;
			BufferedWriter output;
			
			try {
				URL https = new URL(nextURL);
				HttpsURLConnection connection = (HttpsURLConnection) https.openConnection();
				connection.setInstanceFollowRedirects(true);
				connection.setDoInput(true);
				connection.setDoOutput(true);
				
		
				connection.setRequestMethod("HEAD");
				connection.addRequestProperty("User-agent", "cis455crawler");
				connection.setConnectTimeout(5000);
				connection.connect();
				//output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF8"));
				input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	

				
				String line;
				
				String contentType = connection.getContentType();
				//check for valid file type
				if (contentType == null || !contentType.startsWith("text/html")) {
					return;
				}
				/*
				if (contentType == null || !(contentType.startsWith("text/xml") || contentType.startsWith("text/html") || contentType.startsWith("application/xml") || contentType.endsWith("+xml"))) {
					return;
				}
				*/
				
				//check if file is over max file size
				float contentLength = connection.getContentLength();
				if (contentLength == -1 || contentLength / 1000000 > maxDocSize) {
					return;
				}
				try {
					https = new URL("https://" + nextURLInfo.getHostName() + "/robots.txt");
					connection = (HttpsURLConnection) https.openConnection();
					connection.setInstanceFollowRedirects(true);
					connection.setDoInput(true);
					connection.setDoOutput(true);
					//output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF8"));
					input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				} catch(FileNotFoundException e) { //no robots.txt file
					return;
				}

								
				String robotsText = "";
				String nextLine = input.readLine();
				while (nextLine != null) {
					robotsText += nextLine;
					robotsText += "\n";
					nextLine = input.readLine();
				}
				
				RobotsTxtInfo robotsTxtInfo;
				long lastModified = connection.getLastModified();
				if (RobotsRecords.robotsLastModified.get(nextURLInfo.getHostName()) != null &&
						lastModified <= RobotsRecords.robotsLastModified.get(nextURLInfo.getHostName())) {
					robotsTxtInfo = RobotsRecords.robotsTxtInfo.get(nextURLInfo.getHostName());
				} else {
					robotsTxtInfo = RobotsTxtInfo.parseRobotsTxtString(robotsText);
					RobotsRecords.robotsTxtInfo.put(nextURLInfo.getHostName(), robotsTxtInfo);
					RobotsRecords.robotsLastModified.put(nextURLInfo.getHostName(), lastModified);
				}
				//RobotsTextFile robotsTextFile = new RobotsTextFile(input);
				
				
				// Check if we can crawl it
				if (!robotsTxtInfo.canCrawlUrl(CRAWLER_USER_AGENT, nextURLInfo)) {
					System.out.printf("Not allowed to crawl path \"%s\" on \"%s\" by robots.txt.\n", nextURLInfo.getFilePath(), nextURLInfo.getHostName());
					return;
				}
				
				if (!RobotsRecords.crawlDelayPassed(CRAWLER_USER_AGENT, nextURLInfo.getHostName())) {
					urlsToCrawl.add(nextURL);
					return;
				} else {
					RobotsRecords.lastCrawled.put(nextURLInfo.getHostName(), System.currentTimeMillis());
				}
				
				/*
				RobotsTextFile robotsTextFile = new RobotsTextFile(input);
				if (robotsTextFile.isAllowAll()) {
					robotsTxtInfo.addAllowedLink(nextURLInfo.getHostName(), "/");
				} else {
					ArrayList<String> disallowed = robotsTextFile.getDisallowed();
					for (int i = 0; i < disallowed.size(); i++) {
						robotsTxtInfo.addDisallowedLink(nextURLInfo.getHostName(), disallowed.get(i));

					}
					if (disallowed.contains("/") || !isAllowed(disallowed, nextURLInfo.getFilePath())) {
						return;
					}
				}
				*/
				
				https = new URL(nextURL);
				connection = (HttpsURLConnection) https.openConnection();
				connection.setInstanceFollowRedirects(true);
				connection.setDoInput(true);
				connection.setDoOutput(true);
				//output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF8"));
				input = new BufferedReader(new InputStreamReader(connection.getInputStream()));

				numFilesCrawled++;
				visitedUrls.add(nextURL);
				System.out.println("Crawling: " + nextURL);
				
				StringBuilder sb = new StringBuilder();
				
				while ((line = input.readLine()) != null) {
		            sb.append(line);
		        }
				String htmlString = sb.toString();
				InputStream inputStream = new ByteArrayInputStream(htmlString.getBytes(Charset.forName("UTF-8")));
				
				File out = new File("output.txt");
			    FileOutputStream Fos = new FileOutputStream(out);
				Tidy t = new Tidy(); // obtain a new Tidy instance
				t.setQuiet(true);
				t.setShowWarnings(false);
				Document d;
				
				/*
				if (nextURL.endsWith(".xml")) {
					d = null;
					Date date = new Date();
					
					XMLFile file = new XMLFile(date.toString(), nextURL, htmlString, null);
					
					DBWrapper.put(nextURL, file);
					ArrayList<Channel> channels = (ArrayList<Channel>) DBWrapper.get("channels");
					if (channels != null) {
						ArrayList<Channel> newChannels = new ArrayList<Channel>();
						for (Channel c : channels) {
							XPathEngineImpl xpathEngineImpl = c.createXPathEngine();
							if (c.isValid(file, xpathEngineImpl)) {
								System.out.println("xml doc match");
								c.addDocToChannel(file.getStringForChannel());
							}
							newChannels.add(c);
						}
						DBWrapper.put("channels", newChannels);
					}
				} else {
					d = t.parseDOM(inputStream, Fos);
				}
				*/
				
				d = t.parseDOM(inputStream, Fos);
				
				if (d != null && d.getDoctype().getNodeName().startsWith("html")) {
					NodeList links = d.getElementsByTagName("a");
					for (int i = 0; i < links.getLength(); i++) {
						System.out.println("--Adding to queue: " + links.item(i).getAttributes().getNamedItem("href").getNodeValue());
						String newLink = links.item(i).getAttributes().getNamedItem("href").getNodeValue();
						if (newLink.startsWith("http")) {
							urlsToCrawl.add(newLink);
						} else {
							String urlPath;
							if (nextURL.endsWith(".html")) {
								int j = nextURL.length()-1;
								while (nextURL.charAt(j) != '/') {
									j--;
								}
								urlPath = nextURL.substring(0, j+1);
							} else {
								if (!nextURL.endsWith("/")) {
									urlPath = nextURL + "/";
								} else {
									urlPath = nextURL;
								}
							}
							urlsToCrawl.add(urlPath + newLink);
						}
						
					}
				}
				

				
			} catch (IOException e) {
				e.printStackTrace();
			}
		
	}
	
	//private void openConnection
	
	private static boolean isAllowed(ArrayList<String> disallowed, String filepath) {
		for (String s : disallowed) {
			if (filepath.startsWith(s)) {
				return false;
			}
		}
		return true;
	}
	
	private static long convertDate(String stringDate) {
		String str = "Jun 13 2003 23:11:52.454 UTC";
	    SimpleDateFormat df = new SimpleDateFormat(
	        "EEE, dd MMM yyyy HH:mm:ss zzz");
	    Date date = null;
		try {
			date = df.parse(stringDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	    long epoch = date.getTime();
		return epoch;
	}
	
	
}
