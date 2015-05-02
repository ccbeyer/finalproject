package edu.upenn.cis455.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.*;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.XMLFile;
import edu.upenn.cis455.xpathengine.XPathEngineImpl;
import edu.upenn.cis455.xpathengine.XPathEngineFactory;


@SuppressWarnings("serial")
public class XPathServlet extends HttpServlet {
	
	/* TODO: Implement user interface for XPath engine here */
	
	/* You may want to override one or both of the following methods */

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		String xpathQuery = request.getParameter("xpathquery").replaceAll(regex, "");
		String urlLocation = request.getParameter("urllocation");
		XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
		
		String[] xpaths = {xpathQuery};
		
		InputStream is;

		if (urlLocation.startsWith("https")) {
			Socket myClient;
			BufferedReader input;
			BufferedWriter output;
			
			
			try {
				URL https = new URL(urlLocation);
				HttpsURLConnection connection = (HttpsURLConnection) https.openConnection();
				connection.setInstanceFollowRedirects(true);
				connection.setDoInput(true);
				connection.setDoOutput(true);

				input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	
				
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = input.readLine()) != null) {
		            sb.append(line);
		        }
				String htmlString = sb.toString();
				is = new ByteArrayInputStream(htmlString.getBytes(Charset.forName("UTF-8")));
				
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}
		
		URL url = new URL(urlLocation);
		is = url.openStream();
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
	    out.println("<html><head><title>Form</title></head><body>");
	    out.println("<h3>Chris Beyer, pennkey: beyerc</h3>");
	    out.println("<form name=\"loginForm\" method=\"post\" action=\"xpath\">");
	    out.println("Xpath Query: <input type=\"text\" name=\"xpathquery\"/> <br/>");
	    out.println("URL: <input type=\"text\" name=\"urllocation\"/> <br/>");
	    out.println("<input type=\"submit\" value=\"Submit\" />");
	    out.println("</form>");
	    out.println("</body></html>");
	    
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









