package edu.upenn.cis455.storage;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

public class DBWrapperTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DBWrapper.setDirectory("/home/cis455/workspace/HW2/database");
		DBWrapper.setup();
	}

	@Test
	public void testString() {
		DBWrapper.put("hello", "world");
		String result = (String) DBWrapper.get("hello");
		assertTrue(result.equals("world"));
	}
	
	@Test
	public void testArrayList() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("Hello");
		DBWrapper.put("list", list);
		ArrayList<String> result = (ArrayList<String>) DBWrapper.get("list");
		
		assertTrue(result.get(0).equals("Hello"));
	}

}
