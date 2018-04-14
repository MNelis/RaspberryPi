package com.nedap.university.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class Utils {
	public static final int DEFAULTPORT = 8888;
	public static final int BUFLENGTH = 1500;
	public static final byte[] BUF = new byte[BUFLENGTH];

	public static final int HEADER = 4;

	public static final int ACK = 0b01000000;
	public static final int ACK_LIST = 0b01001000;
	public static final int RQ_FILE = 0b01000100;
	public static final int RQ_LIST = 0b01000010;
	public static final int DATA = 0b00100000;
	public static final int DATA_ANN = 0b00101000;
	public static final int MSG_DSC = 0b00010000;
	public static final int MSG_ERR = 0b00011000;

	public static final String RASPBERRYMENU = "     .~~.   .~~.     \n"
			+ "    '. \\ ' ' / .'    Welcome!\n" + "     .~ .~~~..~.     \n"
			+ "    : .~.'~'.~. :    0 .......... send a file.\n"
			+ "   ~ (   ) (   ) ~   1 ....... request a file.\n"
			+ "  ( : '~'.~.'~' : )  2 .... request file list.\n"
			+ "   ~ .~ (   ) ~. ~   EXIT ...............exit.\n"
			+ "    (  : '~' :  )    \n" + "     '~ .~~~. ~'     \n"
			+ "         '~'         \n";

	public void sendPacket(boolean isClient, DatagramPacket pkt, DatagramSocket socket)
			throws IOException {
		socket.send(pkt);
	}

	public void receivePacket(boolean isClient, DatagramSocket socket) throws IOException {
		DatagramPacket pkt = new DatagramPacket(BUF, BUF.length);
		socket.receive(pkt);
	}

	public void processPacket(boolean isClient, DatagramPacket pkt) {
		int flag = pkt.getData()[0];
		byte[] data = Arrays.copyOfRange(pkt.getData(), HEADER,
				pkt.getLength());
		System.out.println("Received data: " + pkt.getLength() + " bytes");

		switch (flag) {
			case ACK :
				break;
				
			case ACK_LIST :
				break;
				
			case RQ_FILE :
				break;
				
			case RQ_LIST :
				break;
				
			case DATA :
				break;
				
			case DATA_ANN :
				break;
				
			case MSG_DSC :
				break;
				
			case MSG_ERR :
				break;

			default :

		}
	}

}
