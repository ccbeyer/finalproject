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

public class OpenChannelServlet extends HttpServlet {
/* TODO: Implement user interface for XPath engine here */
	
	/* You may want to override one or both of the following methods */

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/xml");
		
		int portNumber = 8080;
		
		Socket client = null;
		
		String channelName = request.getParameter("name");
		

	
		//send("/home/cis455/workspace/HW2/www/form.html", response, client);
	    PrintWriter out = response.getWriter();
	    
		ArrayList<Channel> channels = (ArrayList<Channel>) DBWrapper.get("channels");
		for (Channel c : channels) {
			if (c.getName().equals(channelName)) {
				String xml = c.getChannelString();
				System.out.println(xml);
				out.print(xml);
			}
		}
		
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