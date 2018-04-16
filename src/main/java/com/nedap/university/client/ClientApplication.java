package com.nedap.university.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import com.nedap.university.utils.ApplicationThread;
import com.nedap.university.utils.Utils;

public class ClientApplication {
	private BufferedReader reader;
	private static InetAddress serverAddress;
	private static int port = Utils.DEFAULTPORT;

	public static void main(String args[]) {
		new ClientApplication().start();
	}

	private void start() {
		if (locateServer()) {
			print(Utils.RASPBERRYMENU);
			boolean running = true;
			try {
				reader = new BufferedReader(new InputStreamReader(System.in));

				while (running) {
					byte[] data;
					DatagramPacket pkt;

					String input = reader.readLine();
					String[] splitInput = input.split(" ");
					switch (splitInput[0].toUpperCase()) {
						case "0" :
							if (splitInput.length > 1) {
								new ApplicationThread(true, splitInput[1], serverAddress, port)
										.start();
								break;
							}
							else {
								err("Please enter the file name.");
								break;
							}

						case "1" :
							if (splitInput.length > 1) {
								data = Utils.createFlagData(Utils.RQ_FILE,
										splitInput[1].getBytes());
								pkt = new DatagramPacket(data, data.length,
										serverAddress, port);
								new ApplicationThread(true, pkt).start();
								break;
							}
							else {
								err("Please enter the file name.");
								break;
							}

						case "2" :
							data = Utils.createFlagData(Utils.RQ_LIST, null);
							pkt = new DatagramPacket(data, data.length,
									serverAddress, port);
							new ApplicationThread(true, pkt).start();
							break;

						case "EXIT" :
							running = false;
							break;
						default :
					}
				}
			}
			catch (IOException e) {

			}

		}
		else {
			err("Cannot find the server. ");
		}
	}

	private boolean locateServer() {
		// Find the server using UDP broadcast
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.setBroadcast(true);
			byte[] sendData = new byte[]{(byte) Utils.MSG_DSC};

			// Try 255.255.255.255
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, InetAddress.getByName("255.255.255.255"),
					port);
			socket.send(sendPacket);

			byte[] buf = new byte[15000];
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

			socket.setSoTimeout(3000);
			socket.receive(receivePacket);
			serverAddress = receivePacket.getAddress();
			print("Found the server!    " + serverAddress.getHostAddress()
					+ ", port " + port);
			return true;
		}
		catch (SocketTimeoutException e) {
			e.getMessage();
			return false;
		}
		catch (IOException e) {
			e.getMessage();
			return false;
		}

	}

	public static void print(String msg) {
		System.out.println(msg);
	}

	public static void err(String msg) {
		System.err.println("ERROR: " + msg);
	}

}
