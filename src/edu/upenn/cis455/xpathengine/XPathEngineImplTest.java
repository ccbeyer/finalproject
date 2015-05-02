package edu.upenn.cis455.xpathengine;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

public class XPathEngineImplTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testIsValidValidCase() {
		String xpathQuery = "/a/b/c[text()=\"theEntireText\"]";
		String xpathQuery2 = "/a/b/c[text()   =  \"whiteSpacesShouldNotMatter\"]";
		String xpathQuery3 =  "/blah[anotherElement]";
		
		String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		xpathQuery = xpathQuery.replaceAll(regex, "");
		xpathQuery2 = xpathQuery2.replaceAll(regex, "");
		xpathQuery3 = xpathQuery3.replaceAll(regex, "");
		
		XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
		
		String[] xpaths = {xpathQuery, xpathQuery2, xpathQuery3};
		xpathEngine.setXPaths(xpaths);
				
		assertTrue(xpathEngine.isValid(0));
		assertTrue(xpathEngine.isValid(1));
		assertTrue(xpathEngine.isValid(2));
	}
	
	@Test
	public void testIsValidInvalidCase() {
		String xpathQuery = "/a\"wa\"/b/c[text()=\"theEntireText\"]";
		String xpathQuery2 = "/a/b/c[tex[[t()   =  \"whiteSpace sShouldNotMatter\"]";
		String xpathQuery3 =  "/blah[ano{therElement]";
		String xpathQuery4 = "/a/\"b/c[t.ext()   =  \"whiteSpacesShouldNotMatter\"]"; 
		
		String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		xpathQuery = xpathQuery.replaceAll(regex, "");
		xpathQuery2 = xpathQuery2.replaceAll(regex, "");
		xpathQuery3 = xpathQuery3.replaceAll(regex, "");
		xpathQuery4 = xpathQuery4.replaceAll(regex, "");
		
		XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
		
		String[] xpaths = {xpathQuery, xpathQuery2, xpathQuery3, xpathQuery4};
		xpathEngine.setXPaths(xpaths);
				
		assertFalse(xpathEngine.isValid(0));
		assertFalse(xpathEngine.isValid(1));
		assertFalse(xpathEngine.isValid(2));
		assertFalse(xpathEngine.isValid(3));
	}
	
	@Test
	public void testEvaluateTrue() throws Exception {
		String xpathQuery = "/html/body/table/tr/td[@align=\"right\"]";
		String xpathQuery2 = "/html/body/table/tr/td/nobr/img[@src=\"img/pic1.jpg\"]";
		String xpathQuery3 = "/html/body/ table/tr/td/h2[text()=\"Homework assignments for CIS 455 / 555\"]";
		String xpathQuery4 = "/html/body/ta ble /tr/td/h2[contains(text(),\"Homework a\")]";
	

		String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		xpathQuery = xpathQuery.replaceAll(regex, "");
		xpathQuery2 = xpathQuery2.replaceAll(regex, "");
		xpathQuery3 = xpathQuery3.replaceAll(regex, "");
		xpathQuery4 = xpathQuery4.replaceAll(regex, "");
		String urlLocation = "http://www.cis.upenn.edu/~cis455/";
		
		XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
		
		String[] xpaths = {xpathQuery};
		
		URL url = new URL(urlLocation);
		InputStream is = url.openStream();
		File out = new File("output.txt");
	    FileOutputStream Fos = new FileOutputStream(out);
		Tidy t = new Tidy(); // obtain a new Tidy instance
		Document d = t.parseDOM(is,Fos);
		
		xpathEngine.setXPaths(xpaths);
		boolean[] result = xpathEngine.evaluate(d);
		
		for (int i = 0; i < result.length; i++) {
			assertTrue(result[i]);
		}
	}
	
	@Test
	public void testEvaluateFalse() throws Exception {
		String xpathQuery = "/html/body/table/tr/td/td";
		String xpathQuery2 = "/html/body/table/tr/td/nobr/img[@src=\"img/picturess.jpg\"]";
		String xpathQuery3 = "/html/body/ table/tr/td/h2[text()=\"Homework assignmendts for CIS 455 / 555\"]";
		String xpathQuery4 = "/html/body/ta ble /tr/td/h2[contains(text(),\"Homework numberrs\")]";
	

		String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";
		xpathQuery = xpathQuery.replaceAll(regex, "");
		xpathQuery2 = xpathQuery2.replaceAll(regex, "");
		xpathQuery3 = xpathQuery3.replaceAll(regex, "");
		xpathQuery4 = xpathQuery4.replaceAll(regex, "");
		String urlLocation = "http://www.cis.upenn.edu/~cis455/";
		
		XPathEngineImpl xpathEngine = (XPathEngineImpl) XPathEngineFactory.getXPathEngine();
		
		String[] xpaths = {xpathQuery};
		
		URL url = new URL(urlLocation);
		InputStream is = url.openStream();
		File out = new File("output.txt");
	    FileOutputStream Fos = new FileOutputStream(out);
		Tidy t = new Tidy(); // obtain a new Tidy instance
		Document d = t.parseDOM(is,Fos);
		
		xpathEngine.setXPaths(xpaths);
		boolean[] result = xpathEngine.evaluate(d);
		
		for (int i = 0; i < result.length; i++) {
			assertFalse(result[i]);
		}
	}
	
	@Test
	public void testIsAlphaNumeric() {
		assertTrue(XPathEngineImpl.isAlphaNumeric("hello"));
		assertTrue(XPathEngineImpl.isAlphaNumeric("HellO12"));
		assertFalse(XPathEngineImpl.isAlphaNumeric("hel.[:lo"));
	}

}
