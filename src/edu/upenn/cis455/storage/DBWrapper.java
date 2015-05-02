package edu.upenn.cis455.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.persist.EntityStore;

public class DBWrapper {
	
	private static String envDirectory = "/home/cis455/workspace/finalproject/database";
	public static boolean isNull = true;
	
	private static Environment myEnv;
	private static EntityStore store;
	private static Database myDatabase;
	
	public DBWrapper() {
		if (myEnv == null) {
			Environment myDbEnvironment = null;
	        Database myDatabase = null;
			
			try {
				EnvironmentConfig envConfig = new EnvironmentConfig();
				envConfig.setAllowCreate(true);
				myDbEnvironment = new Environment(new File(envDirectory), envConfig);

			} catch (DatabaseException dbe) {
				// Exception handling goes here
			} 
			myEnv = myDbEnvironment;
		}
		
		if (myDatabase == null) {
	        Database myDb = null;
			
			try {
				// Open the database, creating one if it does not exist
	            DatabaseConfig dbConfig = new DatabaseConfig();
	            dbConfig.setAllowCreate(true);
	            myDb = myEnv.openDatabase(null,"TestDatabase", dbConfig);
	            
			} catch (DatabaseException dbe) {
				// Exception handling goes here
			} 
			myDatabase = myDb;
		}
		isNull = false;
	}
	
	public static void setup() {
		if (myEnv == null) {
			Environment myDbEnvironment = null;
			
			try {
				EnvironmentConfig envConfig = new EnvironmentConfig();
				envConfig.setAllowCreate(true);
				myDbEnvironment = new Environment(new File(envDirectory), envConfig);

			} catch (DatabaseException dbe) {
				dbe.printStackTrace();
			} 
			myEnv = myDbEnvironment;
		}
		
		if (myDatabase == null) {
	        Database myDb = null;
			
			try {
				// Open the database, creating one if it does not exist
	            DatabaseConfig dbConfig = new DatabaseConfig();
	            dbConfig.setAllowCreate(true);
	            myDb = myEnv.openDatabase(null,"TestDatabase", dbConfig);
	            
			} catch (DatabaseException dbe) {
				dbe.printStackTrace();
			} 
			myDatabase = myDb;
		}
		isNull = false;
	}

	
	public static void put(Object key, Object value) {
		if (isNull) {
			setup();
		}
		try {
		    DatabaseEntry theKey = new DatabaseEntry(serialize(key));
		    DatabaseEntry theData = new DatabaseEntry(serialize(value));
		    myDatabase.put(null, theKey, theData);
		} catch (Exception e) {
			System.err.println(e);
		    System.out.println("Could not put object into database");
		}
		myEnv.sync();
	}
	
	public static Object get(Object key) {
		if (isNull) {
			setup();
		}
		
		try {
		    // Create two DatabaseEntry instances:
		    // theKey is used to perform the search
		    // theData will hold the value associated to the key, if found
		    DatabaseEntry theKey = new DatabaseEntry(serialize(key));
		    DatabaseEntry theData = new DatabaseEntry();
		 
		    // Call get() to query the database
		    if (myDatabase.get(null, theKey, theData, LockMode.DEFAULT) ==
		        OperationStatus.SUCCESS) {
		 
		        // Translate theData into a String.
		        byte[] retData = theData.getData();
		        return deserialize(retData);
		    } else {
		        System.out.println("No record found with key '" + key + "'.");
		        return null;
		    }
		} catch (Exception e) {
			return null;
		    // Exception handling
		}
	}
	
	
	public static void deleteEntry(String de) {
		
		try {
			DatabaseEntry theKey = new DatabaseEntry(serialize(de));
			myDatabase.delete(null, theKey);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void setDirectory(String s) {
		envDirectory = s;
	}
	
	public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(obj);
        return b.toByteArray();
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        return o.readObject();
    }
	
	/* TODO: write object store wrapper for BerkeleyDB */
	
}
