package com.nedap.university;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.Before;
import com.nedap.university.utils.Utils;

public class utilsTest {
	@Before
	public void setUp() {
		Utils.setClientPath("C:/Users/marjan.nelis/Desktop/test/");
	}
	
	@Test 
	public void testGetSetClientPath(){
		String newPath = "C:/Users/marjan.nelis/Desktop/";
		String newPathInvalid = "C:/Users/marjan.nelis/Desk";
				
		assertTrue(Utils.setClientPath(newPath));
		assertEquals(newPath, Utils.getClientPath());
		
		assertFalse(Utils.setClientPath(newPathInvalid));
	}
	
	@Test
	public void testGetSetFile() {
		byte[] fileContent = Utils.getFileContents(true, "test.JPG");
		Utils.setFileContents(true, fileContent, "testCopy.JPG");
		
		byte[] fileContentCopy = Utils.getFileContents(true, "testCopy.JPG");
		assertTrue(Arrays.equals(fileContent, fileContentCopy));		
	}

}
