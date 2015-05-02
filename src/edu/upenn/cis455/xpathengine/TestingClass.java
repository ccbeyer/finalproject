package edu.upenn.cis455.xpathengine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

import com.sleepycat.je.DatabaseEntry;

import edu.upenn.cis455.storage.DBWrapper;

public class TestingClass {
	
	public static void main(String[] args) throws IOException {
		//testValid();
		//testEvaluate();
	}
	
	public static void testValid() {
		String xpathQuery = "/a/b/c[text()=\"theEntireText\"]";
		String xpathQuery2 = "/a/b/c[text()   =  \"whiteSpacesShouldNotMatter\"]";
		String xpathQuery3 =  "/blah[anotherElement]";
		String xpathQuery4 = "/a/\"b/c[t.e:'[.[xt()   =  \"whiteSpacesShouldNotMatter\"]"; 
		
		String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		xpathQuery = xpathQuery.replaceAll(regex, "");
		xpathQuery2 = xpathQuery2.replaceAll(regex, "");
		xpathQuery3 = xpathQuery3.replaceAll(regex, "");
		xpathQuery4 = xpathQuery4.replaceAll(regex, "");
		
		XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
		
		String[] xpaths = {xpathQuery, xpathQuery2, xpathQuery3, xpathQuery4};
		xpathEngine.setXPaths(xpaths);
		System.out.println(xpathEngine.isValid(3));
		/*String xpathQuery = "/a/\"b/c[t.ext()   =  \"whiteSpacesShouldNotMatter\"]";
		
		String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		xpathQuery = xpathQuery.replaceAll(regex, "");
		
		XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
		
		String[] xpaths = {xpathQuery};
		xpathEngine.setXPaths(xpaths);
				
		System.out.println("xpathQuery: " + xpathQuery + " " + xpathEngine.isValid(0));*/
	}
	
	public static void testEvaluate() throws IOException {
		//String xpathQuery = "/html/body/table/tr/td[@align=\"right\"]";
				//String xpathQuery = "/html/body/table/tr/td/nobr/img[@src=\"img/pic1.jpg\"]";
				//String xpathQuery = "/html/body/ table/tr/td/h2[text()=\"Homework assignments for CIS 455 / 555\"]";
				//String xpathQuery = "/html/body/ta ble /tr/td/h2[contains(text(),\"Homework a\")]";
				//String xpathQuery = "/html/body/table/tr[td[@align]]/td/nobr";
				String xpathQuery = "/html/body/table/tr[td[@align=\"right\"]][td/nobr]";
				
				
				//String xpathQuery = "/CATALOG/CD/TITLE[text()=\"Hide your heart\"]";
				//String urlLocation = "http://www.w3schools.com/xml/cd_catalog.xml";

				String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
				xpathQuery = xpathQuery.replaceAll(regex, "");
				String urlLocation = "http://www.cis.upenn.edu/~cis455/";
				
				XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
				
				String[] xpaths = {xpathQuery};
				
				URL url = new URL(urlLocation);
				InputStream is = url.openStream();
				File out = new File("output.txt");
			    FileOutputStream Fos = new FileOutputStream(out);
				Tidy t = new Tidy(); // obtain a new Tidy instance
				//t.setXmlTags(true);
				Document d = t.parseDOM(is,Fos);
				
				xpathEngine.setXPaths(xpaths);
				xpathEngine.evaluate(d);
				
				
				System.out.println("xpathQuery: " + xpathQuery + ",urlLocation: " + urlLocation);
	}

}
