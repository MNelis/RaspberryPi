package com.nedap.university.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

	public static String CLIENTPATH = "C:/Users/marjan.nelis/Desktop/client/";
	public static final String SERVERPATH = "C:/Users/marjan.nelis/Desktop/server/";

	public static final int DEFAULTPORT = 8765;
	public static final int BUFLENGTH = 1500;
	public static final byte[] BUF = new byte[BUFLENGTH];

	public static final int HEADER = 4;
	public static final int DATASIZE = 1024;
	private static File[] listOfFiles;

	public static final int PKTNORANGE = 128;

	public static final int ACK = 0b01000000;
	public static final int ACK_LIST = 0b01001000;
	public static final int ACK_PAUSE = 0b01000100;
	public static final int ACK_UNPAUSE = 0b01000110;
	public static final int RQ_FILE = 0b01000010;
	public static final int RQ_LIST = 0b01000001;
	public static final int DATA = 0b00100000;
	public static final int DATA_ANN = 0b00101000;
	public static final int MSG_DSC = 0b00010000;
	public static final int MSG_ERR = 0b00011000;

	public static final String RASPBERRYMENU = "     .~~.   .~~.     \n"
			+ "    '. \\ ' ' / .'    Welcome!\n" + "     .~ .~~~..~.     \n"
			+ "    : .~.'~'.~. :    0 ................. upload a file.\n"
			+ "   ~ (   ) (   ) ~   1 ............... download a file.\n"
			+ "  ( : '~'.~.'~' : )  2 ................ list all files.\n"
			+ "   ~ .~ (   ) ~. ~   3 ..... list current up/downloads.\n"
			+ "    (  : '~' :  )    4 .... pause current up/downloads.\n"
			+ "     '~ .~~~. ~'     5 ................... change path.\n"
			+ "         '~'         6 .............. exit application.\n";

	/**
	 * Gets the contents of a given file.
	 * @param isCLient boolean which indicates if the client is calling this method
	 * @param fileName name of the file
	 * @return byte[] with the contents of the file
	 */
	public static byte[] getFileContents(boolean isCLient, String fileName) {
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

	/**
	 * Writes the contents of a file to an file.
	 * @param isClient boolean which indicates if the client is calling this method
	 * @param fileContents byte[] containing the contents of the file
	 * @param fileName the name of the file 
	 */
	public static void setFileContents(boolean isClient, byte[] fileContents,
			String fileName) {
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

	/**
	 * Creates data for a packet with given flag and content.
	 * @param flag desired flag of the packet
	 * @param data data the packet must contain
	 * @return data with the given flag in the header and given data
	 */
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

	/**
	 * Gets the list of files on the given path.
	 * @param isClient boolean which indicates if the client is calling this method
	 * @return data which contains a String that gives a list of filenames
	 */
	protected static byte[] getList(boolean isClient) {
		File folder;
		if (isClient) {
			folder = new File(CLIENTPATH);
		}
		else {
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

	/**
	 * Checks if a given file is on the given path.
	 * @param isClient boolean which indicates if the client is calling this method
	 * @param fileName the name of the file we are looking for
	 * @return boolean; true if the file is present on the path; false otherwise
	 */
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

	/**
	 * Defragments an array of byte[], i.e. the data of the received packets, and merges it to a
	 * single array containing the file contents. 
	 * @param pkts array of received packets
	 * @return single array with the file contents
	 */
	protected static byte[] defragmentData(byte[][] pkts) {
		int length = 0;
		for (int i = 1; i < pkts.length; i++) {
			length += pkts[i].length;
		}

		byte[] fileContents = new byte[length];

		int filePointer = 0;
		for (int i = 1; i < pkts.length; i++) {
			System.arraycopy(pkts[i], 0, fileContents, filePointer,
					pkts[i].length);

			filePointer += pkts[i].length;
		}
		return fileContents;
	}

	/**
	 * Sets the path of the ClientApplication to a given path.
	 * @param newPath the new path, an empty new path leaves the old path unchanged
	 * @return boolean; true if the new path is valid or empty; false if new path is invalid
	 */
	public static boolean setClientPath(String newPath) {
		if (newPath.length() > 0) {
			Path path = Paths.get(newPath);
			if (Files.exists(path)) {
				if (newPath.substring(newPath.length() - 1).equals("/")) {
					CLIENTPATH = newPath;
				}
				else {
					CLIENTPATH = newPath + "/";
				}

				System.out.println("Successfully set this path.");
				return true;
			}
			else {
				return false;
			}
		}
		else {
			System.out.println("Path is unchanged.");
			return true;
		}
	}

	/**
	 * Gets the path of the ClientApplication.
	 * @return path of the ClientApplication.
	 */
	public static String getClientPath() {
		return CLIENTPATH;
	}
	
	/**
	 * Creates the checksum of a given file.
	 * @param fileContents the contents of the file
	 * @return string containing the checksum
	 */
	public static String checkSum(byte[] fileContents) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA1");
			md.update(fileContents);

			byte[] mdbytes = md.digest();

			// convert the byte to hex format
			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
						.substring(1));
			}

			return sb.toString();
		}
		catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
