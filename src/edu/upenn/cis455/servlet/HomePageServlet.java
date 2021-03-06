package edu.upenn.cis455.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sleepycat.je.DatabaseException;

import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis455.xpathengine.XPathEngineImpl;

public class HomePageServlet extends HttpServlet {
	
	/* You may want to override one or both of the following methods */

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		System.out.println(request.getParameter("id"));
		if (request.getParameter("id").equals("newaccount")) {
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			
			System.out.println("create");
			System.out.println(username);
			System.out.println(password);
			
		} else if (request.getParameter("id").equals("login")) {
			String username = request.getParameter("username");
			String password = request.getParameter("password");
			
			System.out.println("login");
			System.out.println(username);
			System.out.println(password);
		}
		
		response.sendRedirect("./addchannel");
		
		/*
		
		<form name="loginForm" method="post" action="xpath">
		    Username: <input type="text" name="username"/> <br/>
		    Password: <input type="password" name="password"/> <br/>
		    <input type="hidden" name="id" value="newaccount"/> <br/>
		    <input type="submit" value="Create New Account" />
		</form>
		<br/>
		
		<h3> Login </h3>
		<form name="loginForm" method="post" action="xpath">
		    Username: <input type="text" name="username"/> <br/>
		    Password: <input type="password" name="password"/> <br/>
		    <input type="hidden" name="id" value="login"/> <br/>
		    <input type="submit" value="Login" />
		</form>
		
		 */
		/*
		String xpathQueriesString = request.getParameter("xpaths").replaceAll(regex, "");
		String name = request.getParameter("name");
		String xslStylesheet = request.getParameter("xslstylesheet");
		XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
		
		
		//ArrayList<String> xpathQueries = new ArrayList<String>();
		String[] xpathQueries = xpathQueriesString.split(";");
		Channel channel = new Channel();
		channel.setName(name);
		
		for (int i = 0; i < xpathQueries.length; i++) {
			channel.addValidXpath(xpathQueries[i].trim());
		}
		ChrisDB.addChannel(channel);
		
		
		/*String[] xpaths = {xpathQuery};
		
		URL url = new URL(urlLocation);
		InputStream is = url.openStream();
		File out = new File("output.txt");
	    FileOutputStream Fos = new FileOutputStream(out);
		Tidy t = new Tidy(); // obtain a new Tidy instance
		if (urlLocation.endsWith(".xml")) {
			t.setXmlTags(true);
		}
		Document d = t.parseDOM(is,Fos);
		
		xpathEngine.setXPaths(xpaths);
		boolean[] success = xpathEngine.evaluate(d);
		
		PrintWriter outWriter = response.getWriter();
		outWriter.println("<html><head><title>Result</title></head><body>");
		outWriter.println("<h3>Chris Beyer, pennkey: beyerc</h3>");
	    for (int i = 0; i < xpaths.length;i++) {
	    	if (success[i]) {
	    		outWriter.println("<h2>" + xpaths[i] + "</h2><h1> was found (success)</h1>");
	    	} else {
	    		outWriter.println("<h2>" + xpaths[i] + "</h2><h1> was not found (failure)</h1>");
	    	}
	    }
	    
	    outWriter.println("</body></html>");
	    
	*/
		
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		/* TODO: Implement user interface for XPath engine here */
		response.setContentType("text/html");
		
		int portNumber = 8080;
		
		Socket client = null;
		/*
		try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
			client = serverSocket.accept();

		} catch (IOException ex) {
			System.out.println("Error daemon thread");
			ex.printStackTrace();
		}
		*/
	
		//send("/home/cis455/workspace/HW2/www/form.html", response, client);
	    PrintWriter out = response.getWriter();
	    
	    BufferedReader br = new BufferedReader(new FileReader("/home/cis455/workspace/HW2/www/home.html"));
	    String fileLine;
	    
		while ((fileLine = br.readLine()) != null) {
			out.println(fileLine);
		}
		out.println("\n<h3>Available Channels</h3>");
		try {
			ArrayList<Channel> channels = (ArrayList<Channel>) DBWrapper.get("channels");
			if (channels != null) {
				for (Channel c : channels) {
					out.println("<a href=\"/HW2/openchannel?name=" + c.getName() + "\">" + "<h4>" + c.getName() + "</h4></a>" +  c.getDocuments().size() + " documents<br/>");
					for (String s : c.getValidXpaths()) {
						out.println(s + ", ");
					}
					out.println("<br/>");
				}
			}
		} catch (DatabaseException e) {
			
		}
		
		

	    /*
	    <form name=\"loginForm\" method=\"post\" action=\"loginServlet\">
	    Username: <input type=\"text\" name=\"username\"/> <br/>
	    Password: <input type=\"password\" name=\"password\"/> <br/>
	    <input type=\"submit\" value=\"Login\" />
	    </form>
	    */
	    
		
	}
	
	public void send(String path, HttpServletResponse response, Socket client) {
		//this.status = 200;
		try (
			FileInputStream fis = new FileInputStream(path);
			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
		) {
			int bufferSize = 10000; //client.getReceiveBufferSize();
			byte[] bytes = new byte[bufferSize];
			
			//String statusLine = "HTTP/1.1 " + getHttpReply(status) + "\r\n";
			String statusLine = "HTTP/1.1 " + "200 OK" + "\r\n";
			String headers = getDateHeader() + "\r\n"
						   + "Connection: close \r\n"
						   + "Content-length: " + (fis.getChannel().size()) + "\r\n"
						   + "Content-type: text/html" + "\r\n";
			String beg = statusLine + headers + "\r\n";
			out.write(beg.getBytes(Charset.forName("UTF-8")));
			
			int count;
			while ((count = bis.read(bytes)) > 0) {
				out.write(bytes, 0, count);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//send(404);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
	
	public static String getDateHeader() {
	    SimpleDateFormat format;
	    String ret;

	    format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
	    format.setTimeZone(TimeZone.getTimeZone("GMT"));
	    ret = "Date: " + format.format(new Date()) + " GMT";

	    return ret;
	}

}
