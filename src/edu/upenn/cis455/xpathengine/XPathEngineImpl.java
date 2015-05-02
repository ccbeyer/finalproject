package edu.upenn.cis455.xpathengine;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.regex.Pattern;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XPathEngineImpl implements XPathEngine {
	
	boolean canonical = true;
	PrintStream out;
	String[] xpaths;
	boolean[] validPaths;

  public XPathEngineImpl() {
    // Do NOT add arguments to the constructor!!
  }
	
  public void setXPaths(String[] s) {
    xpaths = s;
  }

  public boolean isValid(int i) {
	  //check if valid xpaths[i]
	  return checkValid(buildList(xpaths[i]));
	  
	  //return false;
  }
	
  public boolean[] evaluate(Document d) { 
	
	Document doc = d;
	out = System.out;
	boolean[] success = new boolean[xpaths.length];
	for(int i = 0; i < xpaths.length; i++) {
		try {
			if (xpaths[i].length() != 0) {
				LinkedList<String> xpathList = buildList(xpaths[i]);
				success[i] = checkStep(doc, xpathList);
			} else {
				success[i] = false;
			}
		} catch (NullPointerException e) {
			success[i] = false;
		}
		//validPaths[i] = checkForPath(doc, xpaths[i]);
	}
    return success; 
  }
  
  
  private boolean checkValid(String s) {
	  return checkValid(buildList(s));
  }
  
  private boolean checkValid(LinkedList<String> list) {
	  LinkedList<String> xpath = new LinkedList<String>(list);

      if (xpath.isEmpty()) {
    	  return true;
      }
      String first = xpath.pop();
      
      //Check to see if brackets are in the string, and if so, make sure whole bracketed section is in the string
      int countLeftBracket = first.length() - first.replace("[", "").length();
      int countRightBracket = first.length() - first.replace("]", "").length();
      try {
	      while (countLeftBracket > countRightBracket) {
	    	  first = first + "/" + xpath.pop();
	    	  countLeftBracket = first.length() - first.replace("[", "").length();
	          countRightBracket = first.length() - first.replace("]", "").length();
	      }
      } catch(Exception e) {
    	  return false;
      }
      
      if (first.contains("[")) {
    	 
    	  String beforeBracket = first.substring(0, first.indexOf('['));
    	  String afterBracket = first.substring(first.indexOf('[') + 1, first.lastIndexOf(']'));
    	  if (first.lastIndexOf(']') != first.length()-1) {
    		  if (afterBracket.charAt(afterBracket.length()-1) == ']') {
    			  if (checkValid(first.substring(first.lastIndexOf(']')+2))) {
    				  afterBracket = afterBracket.substring(0, afterBracket.length()-1);
    			  } else {
    				  return false;
    			  }
    		  }else {
    			  return false;
    		  }
    	  }
    	  
    	  if (!isAlphaNumeric(beforeBracket)) {
    		  return false;
    	  }
    	  
    	  //attribute and value
    	  if (afterBracket.charAt(0) == '@') {
    		  if (afterBracket.contains("=")) {
    			  String attribute = afterBracket.substring(1, afterBracket.indexOf('='));
    			  if (!isAlphaNumeric(attribute)) {
    				  return false;
    			  }
        		  String value = afterBracket.substring(afterBracket.indexOf('=') + 1);
        		  if (value.charAt(0) != '\"' || value.charAt(value.length()-1) != '\"') { //value must start and end with quotes
        			  return false;
        		  }
        		  if (checkValid(xpath)) {
        			  return true;
        		  }
    		  } else { //just attribute
    			  String attribute = afterBracket.substring(1);
        		  if (isAlphaNumeric(attribute)) {
        			  if (checkValid(xpath)) {
        				  return true;
        			  }
        		  }
    		  }
  
		      return false;
    	  }  else if(afterBracket.startsWith("text()")) {
    		  String text = afterBracket.substring(8,afterBracket.length()-1); //remove text() and quotes
    		  if(! (afterBracket.charAt(6) == '=' && afterBracket.charAt(7) == '\"' && afterBracket.charAt(afterBracket.length()-1) == '\"')) {
    			  return false;
    		  } else if (isAlphaNumeric(text)) {
    			  return checkValid(xpath);
    		  }
    		  
    	  } else if(afterBracket.startsWith("contains(text()")) {
    		  
    		  String text = afterBracket.substring(17,afterBracket.length()-2); //remove text() and quotes
    		  if(! (afterBracket.charAt(15) == ',' 
    				  && afterBracket.charAt(16) == '\"' 
    				  && afterBracket.charAt(afterBracket.length()-2) == '\"' 
    				  && afterBracket.charAt(afterBracket.length()-1) == ')')) {
    			  return false;
    		  } else if (isAlphaNumeric(text)) {
    			  return checkValid(xpath);
    		  }
    		  
    		 
    	  } else {
    		  if(checkValid(buildList(afterBracket)) && checkValid(xpath)) {
    			  return true;
    		  }
    		  return false;
    	  }
    	  
    	  
      } else {

	      //Regular expression with no brackets
	      if (isAlphaNumeric(first)) {
	    	  if (checkValid(xpath)) {
	    		  return true;
	    	  }
	      }
	      
	      
      }
      return false;
  }
  
  
  static boolean isAlphaNumeric(String s) {
	  //Pattern p = Pattern.compile("[^a-zA-Z0-9]");
	  return s.matches("[A-Za-z0-9]+");
  }
  
  
  

  
  
  public boolean checkStep(Node node, LinkedList<String> list) {
	  LinkedList<String> xpath = new LinkedList<String>(list);
      // is there anything to do?
      if ( node == null ) {
          return false;
      }
      if (xpath.isEmpty()) {
    	  return true;
      }
      String first = xpath.pop();
      
      //Check to see if brackets are in the string, and if so, make sure whole bracketed section is in the string
      int countLeftBracket = first.length() - first.replace("[", "").length();
      int countRightBracket = first.length() - first.replace("]", "").length();
      if (countLeftBracket > countRightBracket) {
    	  first = first + "/" + xpath.pop();
      }
      
      if (first.contains("[")) {
    	 
    	  String beforeBracket = first.substring(0, first.indexOf('['));
    	  String afterBracket = first.substring(first.indexOf('[') + 1, first.length()-1);
    	  
    	  //attribute and value
    	  if (afterBracket.charAt(0) == '@') {
    		  if (afterBracket.contains("=")) {
    			  String attribute = afterBracket.substring(1, afterBracket.indexOf('='));
        		  //System.out.println("Attribute: " + attribute);
        		  String value = afterBracket.substring(afterBracket.indexOf('=') + 1).replace("\"", "");
        		  //System.out.println("Value: " + value);
        		  NodeList children = node.getChildNodes();
    		      for (int iChild = 0; iChild < children.getLength(); iChild++ ) {
    		    	  Node child = children.item(iChild);
    		    	  if (child.getNodeName().equals(beforeBracket) && 
    		    			  child.getAttributes().getNamedItem(attribute) != null
    		    			  && child.getAttributes().getNamedItem(attribute).getNodeValue().equals(value)) {
    		    		  if (checkStep(children.item(iChild),xpath)) {
    		    			  return true;
    		    		  }
    		    	  }
    		      } 
    		  } else { //just attribute
    			  String attribute = afterBracket.substring(1);
        		  //System.out.println("Attribute: " + attribute);
        		  NodeList children = node.getChildNodes();
    		      for (int iChild = 0; iChild < children.getLength(); iChild++ ) {
    		    	  if (children.item(iChild).getAttributes().getNamedItem(attribute) != null) {
    		    		  if (checkStep(children.item(iChild),xpath)) {
    		    			  return true;
    		    		  }
    		    	  }
    		      }
    		  }
  
		      return false;
    	  } else if(afterBracket.startsWith("text()")) {
    		  String text = afterBracket.substring(8,afterBracket.length()-1); //remove text() and quotes
    		  NodeList children = node.getChildNodes();
		      for (int iChild = 0; iChild < children.getLength(); iChild++ ) {
		    	  if (children.item(iChild).getNodeName().equals(beforeBracket)) {
		    		  String textContent = children.item(iChild).getFirstChild().getNodeValue();
			    	  if (textContent != null && textContent.equals(text)) {
			    		  if (checkStep(children.item(iChild),xpath)) {
			    			  return true;
			    		  }
			    	  }
		    	  }
		    	  
		      }
    	  } else if(afterBracket.startsWith("contains(text()")) {
    		  String text = afterBracket.substring(17,afterBracket.length()-3); //remove contains(text(), and quotes
    		  if (text.endsWith("\"")) {
    			  text = text.replace("\"", "");
    		  }
    		  NodeList children = node.getChildNodes();
		      for (int iChild = 0; iChild < children.getLength(); iChild++ ) {
		    	  if (children.item(iChild).getNodeName().equals(beforeBracket)) {
		    		  String textContent = children.item(iChild).getFirstChild().getNodeValue();
			    	  if (textContent != null && textContent.contains(text)) {
			    		  if (checkStep(children.item(iChild),xpath)) {
			    			  return true;
			    		  }
			    	  }
		    	  }
		    	  
		      }
    	  } else {
    		  NodeList children = node.getChildNodes();
		      for (int iChild = 0; iChild < children.getLength(); iChild++ ) {
		    	  Node child = children.item(iChild);
		    	  if (child.getNodeName().equals(beforeBracket) && checkStep(child, buildList(afterBracket))) {
		    		  if (checkStep(children.item(iChild),xpath)) {
		    			  return true;
		    		  }
		    	  }
		      }
    	  }
    	  
    	  
      } else {

	      //Regular expression with no brackets
	      int type = node.getNodeType();
	      NodeList children = node.getChildNodes();
	      //System.out.println("Children = " + children.getLength());
	      for (int iChild = 0; iChild < children.getLength(); iChild++ ) {
	    	  if (children.item(iChild).getNodeName() != null && children.item(iChild).getNodeName().equals(first)) {
	    		  if (checkStep(children.item(iChild),xpath)) {
	    			  return true;
	    		  }
	    	  }
	      }
      }
      return false;
  }
  

  
  
  public LinkedList<String> buildList(String path) {
	  LinkedList<String> list = new LinkedList<String>();
	  if (path.charAt(0) == '/') {
		  path = path.substring(1);
	  }
	  String[] str = path.split("/");
	  for (String s : str) {
		  list.add(s);
	  }	  
	  return list;
  }
  
  
        
}
