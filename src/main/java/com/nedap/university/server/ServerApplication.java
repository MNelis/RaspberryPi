package com.nedap.university.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import com.nedap.university.utils.ApplicationThread;
import com.nedap.university.utils.Utils;

public class ServerApplication extends Thread {
	private static DatagramSocket serverSocket;
	private byte[] buf = new byte[Utils.BUFLENGTH];

	public ServerApplication() {
		try {
			serverSocket = new DatagramSocket(Utils.DEFAULTPORT);
		}
		catch (SocketException e) {
			e.getMessage();
			e.printStackTrace();
		}
	}

	/**
	 * Runs the server application. It keeps receiving packets until infinity.
	 */
	public void run() {
		boolean running = true;

		while (running) {
			DatagramPacket pkt = new DatagramPacket(buf, buf.length);
			try {
				serverSocket.receive(pkt);
				new ApplicationThread(false, pkt).start();
			}
			catch (IOException e) {
				e.getMessage();
				e.printStackTrace();
			}

		}
	}

	/**
	* Prints the message.
	* @param msg message
	*/
	public static void print(String msg) {
		System.out.println(msg);
	}

	/**
	 * Prints the error message.
	 * @param msg message
	 */
	public static void err(String msg) {
		System.err.println(msg);
	}

}
