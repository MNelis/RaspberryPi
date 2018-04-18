package com.nedap.university.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import com.nedap.university.utils.ApplicationThread;
import com.nedap.university.utils.Utils;

public class ClientApplication extends Thread {
	private BufferedReader reader;
	private static InetAddress serverAddress;
	private static int port = Utils.DEFAULTPORT;

	private boolean paused = false;
	private List<ApplicationThread> currentThreads = new ArrayList<>();
	private ApplicationThread transferThread = null;

	public static void main(String args[]) {
		new ClientApplication().start();
	}

	/**
	 * Runs the client application. It reads the input from the console and processes it.	
	 */
	public void run() {
		if (locateServer()) {
			print(Utils.RASPBERRYMENU);

			boolean running = true;
			try {
				reader = new BufferedReader(new InputStreamReader(System.in));
				print("Enter the path to the folder we can read/write files:");
				String input = reader.readLine();
				boolean validPath = Utils.setClientPath(input);
				while (!validPath) {
					print("Invalid path, please enter a valid path:");
					input = reader.readLine();
					validPath = Utils.setClientPath(input);
				}
				while (running) {
					byte[] data;
					DatagramPacket pkt;

					input = reader.readLine();
					String[] splitInput = input.split(" ");
					switch (splitInput[0].toUpperCase()) {

						case "0" : // upload file to server.
							getThreads();
							if (currentThreads.size() > 0) {
								print("Since transfering several files at the same time doesn't work as desired, \n"
										+ "this function is disabled in this version.");
								break;
							}
							else {
								print("(upload) Enter the filename:");
								input = reader.readLine();
								if (input.length() > 0) {
									(transferThread = new ApplicationThread(
											true, input, serverAddress, port))
													.start();
									transferThread.setName(input + " (upload)");
									currentThreads.add(transferThread);
									break;
								}
								else {
									err(" Please enter the filename:");
									break;
								}
							}

						case "1" : // download file to server
							getThreads();
							if (currentThreads.size() > 0) {
								print("Since transfering several files at the same time doesn't work as desired, \n"
										+ "this function is disabled in this version.");
								break;
							}
							else {
								print("(download) Enter the filename:");
								input = reader.readLine();
								if (input.length() > 0) {
									data = Utils.createFlagData(Utils.RQ_FILE,
											input.getBytes());
									pkt = new DatagramPacket(data, data.length,
											serverAddress, port);
									(transferThread = new ApplicationThread(
											true, pkt)).start();
									transferThread
											.setName(input + " (download)");
									currentThreads.add(transferThread);
									break;
								}
								else {
									err("Please enter the file name:");
									break;
								}
							}

						case "2" : // request file list
							data = Utils.createFlagData(Utils.RQ_LIST, null);
							pkt = new DatagramPacket(data, data.length,
									serverAddress, port);
							new ApplicationThread(true, pkt).start();
							break;

						case "3" : // list all active up/download threads
							print(getThreads());
							break;

						case "4" : // pause all active up/download threads
							getThreads();
							if (currentThreads.size() > 0) {

								paused = true;

								// Update list of current threads.
								getThreads();

								// Send ACK_PAUSE to active server thread
								data = Utils.createFlagData(Utils.ACK_PAUSE,
										null);
								pkt = new DatagramPacket(data, data.length,
										serverAddress, port);
								for (ApplicationThread t : currentThreads) {
									t.sendPacket(data);
								}

								// Set all client threads to paused
								setPause(paused);

								// Any input will resume the threads.
								print("Press enter to resume the up/downloads.");
								input = reader.readLine();

								paused = false;

								// Send ACK_UNPAUSE to all active server
								// threads.
								data = Utils.createFlagData(Utils.ACK_UNPAUSE,
										null);
								pkt = new DatagramPacket(data, data.length,
										serverAddress, port);
								for (ApplicationThread t : currentThreads) {
									t.sendPacket(data);
								}

								// Set all client threads to not-paused
								setPause(paused);
							}
							else {
								print("Nothing to pause.");
							}
							break;

						case "5" : // display and change the clietn path
							print("== Current path ==");
							print(Utils.getClientPath());

							print("\nEnter new path:");
							input = reader.readLine();
							validPath = Utils.setClientPath(input);
							while (!validPath) {
								print("Invalid path, please enter a valid path:");
								input = reader.readLine();
								validPath = Utils.setClientPath(input);
							}
							break;

						case "6" : // exit
							running = false;
							break;

						case "HELP" :
							print(Utils.RASPBERRYMENU);

						default :
							// err("Unknown command.");
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

	/**
	 * Sends a broadcast and waits for the server to reply in order to obtain its ip-address.
	 * @return true if the server is located; false otherwise.
	 */
	private boolean locateServer() {
		// Find the server using UDP broadcast
		try (DatagramSocket socket = new DatagramSocket()) {
			
			socket.setBroadcast(true);
			byte[] sendData = new byte[]{(byte) Utils.MSG_DSC};

			// Try 255.255.255.255
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, InetAddress.getByName("192.168.1.255"),
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

	/**
	 * Creates a String with all the active up/download threads
	 * @return String with all the name of the active threads.
	 */
	private String getThreads() {
		List<ApplicationThread> deadThread = new ArrayList<>();

		String list = "== Current up/downloads ==\n";
		for (ApplicationThread t : currentThreads) {
			if (t.isAlive()) {
				list += t.getName() + "\n";
			}
			else {
				deadThread.add(t);
			}
		}

		for (ApplicationThread t : deadThread) {
			currentThreads.remove(t);
		}
		list += "Total: " + currentThreads.size() + " threads.";
		return list;
	}

	/**
	 * Sets the paused-boolean in the active threads.
	 * @param paused
	 */
	private void setPause(boolean paused) {
		for (ApplicationThread t : currentThreads) {
			t.setPause(paused);
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
		System.err.println("ERROR: " + msg);
	}

}
