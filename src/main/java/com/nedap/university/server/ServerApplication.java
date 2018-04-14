package com.nedap.university.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import com.nedap.university.utils.Utils;

public class ServerApplication extends Thread {
	private DatagramSocket serverSocket;
	private byte[] buf = new byte[Utils.BUFLENGTH];
	
	public ServerApplication() {
		try {
			serverSocket = new DatagramSocket(Utils.DEFAULTPORT);
		} catch (SocketException e) {
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	public void run() {
		boolean running = true;
		
		while(running) {
			DatagramPacket pkt = new DatagramPacket(buf, buf.length);
			try {
				serverSocket.receive(pkt);
			}
			catch (IOException e) {
				e.getMessage();
				e.printStackTrace();
			}
			
		}
	}
	
	
	public static void print(String msg) {
		System.out.println(msg);
	}

	protected static void err(String msg) {
		System.err.println(msg);
	}

}
