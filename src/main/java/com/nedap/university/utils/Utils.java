package com.nedap.university.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {
	public static final String CLIENTPATH = "C:/Users/marjan.nelis/Desktop/client/";
	public static final String SERVERPATH = "C:/Users/marjan.nelis/Desktop/server/";
	
	public static final int DEFAULTPORT = 8888;
	public static final int BUFLENGTH = 1500;
	public static final byte[] BUF = new byte[BUFLENGTH];

	public static final int HEADER = 4;
	public static final int DATASIZE = 1024;
	private static File[] listOfFiles;
	
	public static final int PKTNORANGE = 128;

	public static final int ACK = 0b01000000;
	public static final int ACK_LIST = 0b01001000;
	public static final int RQ_FILE = 0b01000100;
	public static final int RQ_LIST = 0b01000010;
	public static final int DATA = 0b00100000;
	public static final int DATA_ANN = 0b00101000;
	public static final int MSG_DSC = 0b00010000;
	public static final int MSG_ERR = 0b00011000;

	public static final String RASPBERRYMENU = 
			  "     .~~.   .~~.     \n"
			+ "    '. \\ ' ' / .'    Welcome!\n" 
			+ "     .~ .~~~..~.     \n"
			+ "    : .~.'~'.~. :    0 <filename> ....... send a file.\n"
			+ "   ~ (   ) (   ) ~   1 <filename> .... request a file.\n"
			+ "  ( : '~'.~.'~' : )  2 ............ request file list.\n"
			+ "   ~ .~ (   ) ~. ~   EXIT .......................exit.\n"
			+ "    (  : '~' :  )    \n" 
			+ "     '~ .~~~. ~'     \n"
			+ "         '~'         \n";

	
	protected static byte[] getFileContents(boolean isCLient, String fileName) {
		String path = SERVERPATH;
		if (isCLient) {
			path = CLIENTPATH;
		}
		
		Path pathFile = Paths.get(path + fileName);
		try {
			byte[] data = Files.readAllBytes(pathFile);
			return data;
		}
		catch (IOException e) {			
			e.printStackTrace();
			return null;
		}
	}
	
	protected static void setFileContents(boolean isClient, byte[] fileContents, String fileName) {
		String path = SERVERPATH;
		if (isClient) {
			path = CLIENTPATH;
		}
		
		Path pathFile = Paths.get(path + fileName);
		try {
			Files.write(pathFile, fileContents);
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static byte[] createFlagData(int flag, byte[] data) {
		if (data != null) {
			byte[] pkt = new byte[Utils.HEADER + data.length];

			pkt[0] = (byte) flag;
			System.arraycopy(data, 0, pkt, Utils.HEADER, data.length);
			return pkt;
		}
		else {
			byte[] pkt = new byte[Utils.HEADER];
			pkt[0] = (byte) flag;
			return pkt;
		}
	}
	
	protected static byte[] getList(boolean isClient) {
		File folder;
		if (isClient) {
			folder = new File(CLIENTPATH);
		} else {
			folder = new File(SERVERPATH);
		}
		
		
		listOfFiles = folder.listFiles();

		String list = "";
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				list += listOfFiles[i].getName() + "\n";
			} 
		}
		return list.getBytes();
	}
	
	protected static boolean containsFile(boolean isClient, String fileName) {
		getList(isClient);
		boolean result = false;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].getName().equals(fileName)) {
				result = true;
				break;
			}
		}
		return result;
	}
	

	protected static byte[] defragmentData(byte[][] pkts) {
		int length = 0;
		for (int i = 1; i < pkts.length;  i ++) {
			length += pkts[i].length;
		}
		byte[] fileContents = new byte[length];
		
		int filePointer = 0;
		for (int i = 1; i< pkts.length; i++) {
			System.arraycopy(pkts[i], 0, fileContents, filePointer, pkts[i].length);
			
			filePointer += pkts[i].length;
		}
		return fileContents;
	}
}
