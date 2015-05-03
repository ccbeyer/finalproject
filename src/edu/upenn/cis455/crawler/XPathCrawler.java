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
import java.io.StringReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
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
	public final static String AMAZON_S3_BUCKET_NAME = "cis455docs";
	
	static LinkedList<String> urlsToCrawl;
	static HashSet<String> visitedUrls;
	static long numFilesCrawled;
	static long numFilesToCrawl;
	static RobotsTxtInfo robotsTxtInfo;
	static double maxDocSize;
		
	public static void main(String[] args) {
		
		String startingURL = args[0];
		String databaseDirectory = args[1];
		DBWrapper.setDirectory(databaseDirectory);
		maxDocSize = Double.parseDouble(args[2]);
		if (args.length > 3) {
			numFilesToCrawl = Long.parseLong(args[3]);
		} else {
			numFilesToCrawl = -1;
		}
		
		RobotsRecords.setup();
		urlsToCrawl = new LinkedList<String>();
		visitedUrls = new HashSet<String>();
		urlsToCrawl.add(startingURL);		
		robotsTxtInfo = new RobotsTxtInfo();
		
		try {
			crawl();
		} catch(Exception e) {
			System.out.println("FATAL EXCEPTION, ABORTING CRAWL.");
			e.printStackTrace();
		}
		
		//crawl finished
		System.out.println("CRAWL FINISHED; " + numFilesCrawled + " FILES CRAWLED.");
		
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

			String robotsHeadersString = "";
			String nextLine = input.readLine();
			while ((nextLine = input.readLine()) != null && !nextLine.equals("")) {
				robotsHeadersString += nextLine + "\n";
			}
			HashMap<String, String> robotsHeaders = parseHeaders(robotsHeadersString);
			while (nextLine != null) {
				robotsText += nextLine;
				robotsText += "\n";
				nextLine = input.readLine();
			}
			
			RobotsTxtInfo robotsTxtInfo = getNewestRobots(robotsHeaders, nextURLInfo.getHostName(), robotsText);

			/*
			String lastModified = robotsHeaders.get("last-modified");
			if (RobotsRecords.robotsLastModified.get(nextURLInfo.getHostName()) != null &&
					convertDate(lastModified) <= RobotsRecords.robotsLastModified.get(nextURLInfo.getHostName()) ) {
				robotsTxtInfo = RobotsRecords.robotsTxtInfo.get(nextURLInfo.getHostName());
			} else {
				robotsTxtInfo = RobotsTxtInfo.parseRobotsTxtString(robotsText);
				RobotsRecords.robotsTxtInfo.put(nextURLInfo.getHostName(), robotsTxtInfo);
				RobotsRecords.robotsLastModified.put(nextURLInfo.getHostName(), convertDate(lastModified));
			}
			*/
			
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
			
			output.write("GET " + nextURLInfo.getFilePath() + " HTTP/1.0\r\n");
			output.write("User-Agent: cis455crawler\r\n");
			output.write("\r\n");
			output.flush();
			
			StringBuilder sb = new StringBuilder();
			
			//Get rid of headers from response
			while ((line = input.readLine()) != null && !line.equals("")) {
				
			}
			
			/* THIS WAY DOES NOT WORK because docs can start with other things (ie <!DOCTYPE html>)
			while ((line = input.readLine()) != null && 
					!( line.equals("!DOCTYPE") || line.toLowerCase().equals("<html>") ) ) {
				
			}
			*/
			
			sb.append(line); //append either !DOCTYPE or <html>
			while ((line = input.readLine()) != null) {
	            sb.append(line);
	            sb.append("\n");
	        }
			String htmlString = sb.toString();
			if (htmlString != null && !htmlString.equals("null")) {
				AmazonS3Uploader.uploadFile(htmlString, AMAZON_S3_BUCKET_NAME, nextURL);
			} else {
				System.out.println("null");
			}
			
			System.out.println("Crawling: " + nextURL);

			//Set up tidy for parsing
			InputStream inputStream = new ByteArrayInputStream(htmlString.getBytes(Charset.forName("UTF-8")));
			File out = new File("output.txt");
		    FileOutputStream Fos = new FileOutputStream(out);
		    Tidy t = new Tidy(); // obtain a new Tidy instance
			t.setShowWarnings(false);
			t.setHideComments(true);
			t.setQuiet(true);
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
			//Add all links in page to queue
			try {
				//if (d != null && d.getDoctype().getNodeName().startsWith("html")) {
					NodeList links = d.getElementsByTagName("a");
					for (int i = 0; i < links.getLength(); i++) {
						if (links.item(i).getAttributes().getNamedItem("href") == null) {
							continue;
						}
						String newLink = links.item(i).getAttributes().getNamedItem("href").getNodeValue();
						if (newLink.startsWith("http")) {
							addToCrawlQueue(newLink);
						} else { //relative link -> add hostname
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
							addToCrawlQueue(urlPath + newLink);
						}
						
					//}
				}
			} catch (NullPointerException e) {
				System.err.println("null pointer exception parsing http doc");
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			System.err.println("File not found at url: " + nextURL);
			visitedUrls.add(nextURL);
		}  catch (UnknownHostException e) {
			System.err.println("Unknown Host at url: " + nextURL);
			visitedUrls.add(nextURL);
		}  catch (ConnectException e) {
			System.err.println("Failed to connect to url: " + nextURL);
			visitedUrls.add(nextURL);
		} catch (IOException e) {
			System.err.println("IOException at: " + nextURL);
			System.out.println(e.getMessage());
			visitedUrls.add(nextURL);
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
				
				RobotsTxtInfo robotsTxtInfo = RobotsTxtInfo.parseRobotsTxtString(robotsText);
				/*
				long lastModified = connection.getLastModified();
				if (RobotsRecords.robotsLastModified.get(nextURLInfo.getHostName()) != null &&
						lastModified <= RobotsRecords.robotsLastModified.get(nextURLInfo.getHostName())) {
					robotsTxtInfo = RobotsRecords.robotsTxtInfo.get(nextURLInfo.getHostName());
				} else {
					robotsTxtInfo = RobotsTxtInfo.parseRobotsTxtString(robotsText);
					RobotsRecords.robotsTxtInfo.put(nextURLInfo.getHostName(), robotsTxtInfo);
					RobotsRecords.robotsLastModified.put(nextURLInfo.getHostName(), lastModified);
				}
				*/
				//RobotsTextFile robotsTextFile = new RobotsTextFile(input);
				
				
				// Check if we can crawl it
				if (!robotsTxtInfo.canCrawlUrl(CRAWLER_USER_AGENT, nextURLInfo)) {
					System.out.printf("Not allowed to crawl path \"%s\" on \"%s\" by robots.txt.\n", nextURLInfo.getFilePath(), nextURLInfo.getHostName());
					return;
				}
				
				RobotsRecords.robotsTxtInfo.put(nextURLInfo.getHostName(), robotsTxtInfo);
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
				
				StringBuilder sb = new StringBuilder();
				
				while ((line = input.readLine()) != null) {
		            sb.append(line);
		            sb.append("\n");
		        }
				String htmlString = sb.toString();
				if (htmlString != null && !htmlString.equals("null")) {
					AmazonS3Uploader.uploadFile(htmlString, AMAZON_S3_BUCKET_NAME, nextURL);
				} else {
					System.out.println("null html string");
				}
				
				System.out.println("Crawling: " + nextURL);
				InputStream inputStream = new ByteArrayInputStream(htmlString.getBytes(Charset.forName("UTF-8")));
				
				File out = new File("output.txt");
			    FileOutputStream Fos = new FileOutputStream(out);
				Tidy t = new Tidy(); // obtain a new Tidy instance
				t.setShowWarnings(false);
				t.setHideComments(true);
				t.setQuiet(true);
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
				
				try {
					if (d != null && d.getDoctype().getNodeName().startsWith("html")) {
						NodeList links = d.getElementsByTagName("a");
						for (int i = 0; i < links.getLength(); i++) {
							if (links.item(i).getAttributes().getNamedItem("href") == null) {
								continue;
							}
							String newLink = links.item(i).getAttributes().getNamedItem("href").getNodeValue();
							if (newLink.startsWith("http")) {
								addToCrawlQueue(newLink);
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
								addToCrawlQueue(urlPath + newLink);
							}
							
						}
					}
				} catch(NullPointerException e) {
					System.err.println("null pointer exception parsing https doc");
					e.printStackTrace();
				}
				

				
			}  catch (FileNotFoundException e) {
				System.err.println("File not found at url: " + nextURL);
				visitedUrls.add(nextURL);
			}  catch (UnknownHostException e) {
				System.err.println("Unknown host at url: " + nextURL);
				visitedUrls.add(nextURL);
			}  catch (ConnectException e) {
				System.err.println("Failed to connect to url: " + nextURL);
				visitedUrls.add(nextURL);
			} catch (IOException e) {
				System.err.println("IOException at: " + nextURL);
				System.out.println(e.getMessage());
				visitedUrls.add(nextURL);
			}
		
	}
	
	//private void openConnection
	
	private static HashMap<String, String> parseHeaders(String s){
		try {
			BufferedReader input = new BufferedReader(new StringReader(s));
			String line = input.readLine();
			HashMap<String, String> headers = new HashMap<String, String>();
			while (line != null && !line.equals("")) {
				int i = line.indexOf(":");
				if (i > 0) {
					System.out.println(line);
					headers.put(line.substring(0, i).toLowerCase(), line.substring(i+2));
				} else break;
				line = input.readLine();
			}
			return headers;
		} catch (IOException e) {
			System.err.println("Problem parsing headers");
		}
		return null;
	}
	
	private static RobotsTxtInfo getNewestRobots(HashMap<String, String> robotsHeaders, String hostname, String robotsText) {
		long newLastModified = 0;
		try {
			String lastModified = robotsHeaders.get("last-modified");
			newLastModified = convertDate(lastModified);
			
			RobotsTxtInfo oldRobotsTxtInfo = RobotsRecords.robotsTxtInfo.get(hostname);
			long oldLastModified = oldRobotsTxtInfo.getLastModified();
			
			if (newLastModified > oldLastModified) {
				RobotsTxtInfo robotsTxtInfo = RobotsTxtInfo.parseRobotsTxtString(robotsText);
				robotsTxtInfo.setLastModified(newLastModified);
				RobotsRecords.robotsTxtInfo.put(hostname, robotsTxtInfo);
				return robotsTxtInfo;
			} else {
				return oldRobotsTxtInfo;
			}
		} catch (Exception e) {
			RobotsTxtInfo robotsTxtInfo = RobotsTxtInfo.parseRobotsTxtString(robotsText);
			robotsTxtInfo.setLastModified(newLastModified);
			RobotsRecords.robotsTxtInfo.put(hostname, robotsTxtInfo);
			return robotsTxtInfo;
		}
	}
	
	private static void addToCrawlQueue(String url) {
		if (url.startsWith("#")) {
			return;
		}
		if (!visitedUrls.contains(url)) {
			System.out.println("--Adding to queue: " + url);
			urlsToCrawl.add(url);
		}
	}
	
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
