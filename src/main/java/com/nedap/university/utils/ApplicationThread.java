package com.nedap.university.utils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import com.nedap.university.client.ClientApplication;
import com.nedap.university.server.ServerApplication;

public class ApplicationThread extends Thread {
	private InetAddress address;
	private int port;

	private boolean isClient;
	private DatagramPacket pkt;

	public ApplicationThread(boolean isClient, DatagramPacket pkt) {
		this.isClient = isClient;
		this.pkt = pkt;

	}

	public ApplicationThread(boolean isClient, String msg) {
		this.isClient = isClient;
	}

	public void run() {
		try (DatagramSocket socket = new DatagramSocket()){
			
			
		}catch (SocketException e) {
			
		}
		
	}

	private void print(String msg) {
		if (isClient) {
			ClientApplication.print(msg);
		}
		else {
			ServerApplication.print(msg);
		}
	}

}
