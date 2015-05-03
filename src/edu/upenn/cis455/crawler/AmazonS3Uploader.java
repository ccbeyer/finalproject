package edu.upenn.cis455.crawler;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class AmazonS3Uploader {

		public static void main(String[] args) throws IOException {
			File file = new File("test.txt");
			FileUtils.writeStringToFile(file, "test file");
			uploadFile(file, "cis455docs", "test/url");
		}
		
		public static void uploadFile(String fileString, String bucketName, String keyName) {
			File file = new File("file.html");
			try {
				FileUtils.writeStringToFile(file, fileString);
				uploadFile(file, bucketName, keyName);
			} catch (IOException e) {
				e.printStackTrace();
			}
			file.delete();
		}
		
		public static void uploadFile(File file, String bucketName, String keyName) throws IOException {
	        AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());
	        
	        try {
	            System.out.println("\nUploading " + keyName + "to S3\n");
	            s3client.putObject(new PutObjectRequest(
	            		                 bucketName, keyName, file));

	         } catch (AmazonServiceException ase) {
	            System.out.println("Caught an AmazonServiceException, which " +
	            		"means your request made it " +
	                    "to Amazon S3, but was rejected with an error response" +
	                    " for some reason.");
	            System.out.println("Error Message:    " + ase.getMessage());
	            System.out.println("HTTP Status Code: " + ase.getStatusCode());
	            System.out.println("AWS Error Code:   " + ase.getErrorCode());
	            System.out.println("Error Type:       " + ase.getErrorType());
	            System.out.println("Request ID:       " + ase.getRequestId());
	        } catch (AmazonClientException ace) {
	            System.out.println("Caught an AmazonClientException, which " +
	            		"means the client encountered " +
	                    "an internal error while trying to " +
	                    "communicate with S3, " +
	                    "such as not being able to access the network.");
	            System.out.println("Error Message: " + ace.getMessage());
	        }
	    }
	

}
