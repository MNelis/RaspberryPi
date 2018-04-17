package com.nedap.university.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

import com.nedap.university.client.ClientApplication;
import com.nedap.university.server.ServerApplication;

public class ApplicationThread extends Thread {
	private InetAddress address;
	private int port;
	private long startTime;
	private long endTime;
	private int bytesFile = 0;
	private int noRetransmissions = -1;
	private int expNoPkts;

	private boolean paused;
	private boolean isClient;
	private boolean receivedACK;
	private boolean receiving;
	private byte fileID;
	private byte packetID;
	private DatagramPacket pkt;
	private DatagramSocket socket;
	private byte[][] pkts = null;
	private String msg;
	private int currentPktNo;

	public ApplicationThread(boolean isClient, DatagramPacket pkt) {
		this.isClient = isClient;
		this.pkt = pkt;
	}

	public ApplicationThread(boolean isClient, String msg, InetAddress address,
			int port) {
		this.isClient = isClient;
		this.msg = msg;
		pkt = new DatagramPacket(new byte[0], 0);
		pkt.setAddress(address);
		pkt.setPort(port);
	}

	public void run() {
//		print("New thread: " + this.getName());
		try {
			socket = new DatagramSocket();
		}
		catch (SocketException e) {
			e.getMessage();
			e.printStackTrace();
		}
		init();
	}

	private void init() {
		if (pkt.getLength() == 0) {
			if (Utils.containsFile(isClient, msg)) {
				print("File '" + msg + "' found.");
				fileID = (byte) (Math.random() * 256);
				sendFile(fileID, Utils.getFileContents(isClient, msg), msg);
			}
			else {
				err("File '" + msg + "' not found.");
			}
		}
		else {
			try {
				if (isClient) {
					socket.send(pkt);
					receivePacket(10);
				}
				else {
					processPacket(pkt);
				}
			}
			catch (SocketException e) {
				init();
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// print("Closing socket");
		socket.close();
	}

	private void sendPacket(DatagramPacket pkt, byte[] data) {
		address = pkt.getAddress();
		port = pkt.getPort();

		DatagramPacket newpkt = new DatagramPacket(data, data.length, address,
				port);

		try {
			socket.send(newpkt);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendPacket(byte[] data) {
		sendPacket(this.pkt, data);
	}

	private void receivePacket(int timeOutSec) {
		DatagramPacket pkt = new DatagramPacket(Utils.BUF, Utils.BUF.length);
		try {
			socket.setSoTimeout(timeOutSec * 1000);
			socket.receive(pkt);
			this.pkt.setAddress(pkt.getAddress());
			this.pkt.setPort(pkt.getPort());
			processPacket(pkt);
		}
		catch (IOException | InterruptedException e) {
			receivePacket(timeOutSec);
			e.getMessage();
		}

	}

	private void processPacket(DatagramPacket pkt)
			throws IOException, InterruptedException {
		int flag = pkt.getData()[0];
		// print("" + flag);
		byte[] data = null;
		if (pkt.getLength() > 4) {
			data = Arrays.copyOfRange(pkt.getData(), Utils.HEADER,
					pkt.getLength());
		}
		else {
			data = Arrays.copyOfRange(pkt.getData(), 0, Utils.HEADER);
		}

		// print("Received data: " + pkt.getLength() + " bytes");

		switch (flag) {
			// Both applications.
			case Utils.ACK :
				// print("Received ACK.");
				if (pkt.getData()[1] == fileID
						&& pkt.getData()[2] == packetID) {
					receivedACK = true;
				}
				break;

			case Utils.ACK_PAUSE :
				// if (pkt.getData()[1] == fileID
				// && pkt.getData()[2] == packetID) {
				receivedACK = true;
				// }
				paused = true;
				break;

			case Utils.ACK_UNPAUSE :
				paused = false;
				break;

			// Client only.
			case Utils.ACK_LIST :
				String serverData = new String(data);
				print("== File List ==\n" + serverData);
				break;

			// Server only.
			case Utils.RQ_FILE :
				String fileName = new String(data);
				print("Received request for the file " + fileName);

				if (Utils.containsFile(false, fileName)) {
					print("File found on the server.");
					fileID = (byte) (Math.random() * 256);
					sendFile(fileID, Utils.getFileContents(isClient, fileName),
							fileName);
				}
				else {
					print("File not found on the server.");
					byte[] newData = Utils.createFlagData(Utils.MSG_ERR,
							new String("Unknown file name.").getBytes());
					sendPacket(pkt, newData);
				}
				break;

			// Server only.
			case Utils.RQ_LIST :
				print("Received request for the list of files from "
						+ pkt.getAddress().getHostAddress() + ".");
				byte[] dataList = Utils.createFlagData(Utils.ACK_LIST,
						Utils.getList(false));
				sendPacket(pkt, dataList);
				break;

			// Both applications.
			case Utils.DATA_ANN :
				currentPktNo = 0;
				receiving = true;
				String[] dataSplit = new String(data).split(" ", 2);
				expNoPkts = Integer.parseInt(dataSplit[0]);
				String name = dataSplit[1];

				print("Received announcement for file '" + name + "', "
						+ (expNoPkts) + " packets expected.");

				pkts = new byte[expNoPkts + 1][0];
				pkts[currentPktNo] = name.getBytes();

				byte[] dataACK1 = Arrays.copyOfRange(pkt.getData(), 0,
						Utils.HEADER);
				dataACK1[0] = Utils.ACK;
				sendPacket(pkt, dataACK1);

				receiveFile();
				break;

			// Both applications.
			case Utils.DATA :
				if (receiving) {
					if ((currentPktNo + 1)
							% Utils.PKTNORANGE == pkt.getData()[2]) {
						currentPktNo++;
						pkts[currentPktNo] = data;
						byte[] dataACK = Arrays.copyOfRange(pkt.getData(), 0,
								Utils.HEADER);
						if (!paused) {
							dataACK[0] = Utils.ACK;
						}
						else {
							dataACK[0] = Utils.ACK_PAUSE;
						}

						sendPacket(pkt, dataACK);
						// print("Send ACK.");

						if (currentPktNo == pkts.length - 1) {
							byte[] fileContents = Utils.defragmentData(pkts);
							bytesFile = fileContents.length;
							String nameFile = new String(pkts[0]);
							print("Received " + nameFile + ", "
									+ fileContents.length + " bytes, "
									+ (pkts.length - 1) + " packets.");
							Utils.setFileContents(isClient, fileContents,
									nameFile);
							receiving = false;
						}
					}

				}
				else {
					err("Did not receive the announcement of this data.");
				}
				break;

			// Server only.
			case Utils.MSG_DSC :
				print("Received discover message from "
						+ pkt.getAddress().getHostAddress());
				sendPacket(pkt, data);
				break;

			// Both applications.
			case Utils.MSG_ERR :
				err(new String(data));
				break;

			default :

		}
	}

	private void sendFile(byte fileID, byte[] fileContents, String fileName) {
		startTime = System.nanoTime();
		bytesFile = fileContents.length;
		noRetransmissions = 0;
		int attempt = 0;

		int filePointer = 0;
		int pktNo = 0;
		int numberOfPackets = fileContents.length / Utils.DATASIZE;
		if (Utils.DATASIZE * numberOfPackets != fileContents.length) {
			numberOfPackets++;
		}

		expNoPkts = numberOfPackets;

		byte[] nxtPkt;

		byte[] data = (numberOfPackets + " " + fileName).getBytes();

		nxtPkt = new byte[Utils.HEADER + data.length];

		nxtPkt[0] = Utils.DATA_ANN;
		nxtPkt[1] = fileID;

		System.arraycopy(data, 0, nxtPkt, Utils.HEADER, data.length);

		print("Sending...");
		sendPacket(pkt, nxtPkt);
		attempt++;
		packetID = nxtPkt[2];
		receivedACK = false;
		receivePacket(5);
		// creates new packets as long as necessary
		while (filePointer < fileContents.length) {
			if (receivedACK) {
				attempt = 0;
				pktNo++;
				// create a new packet of appropriate size
				int datalen = Math.min(Utils.DATASIZE,
						fileContents.length - filePointer);
				nxtPkt = new byte[Utils.HEADER + datalen];

				nxtPkt[0] = Utils.DATA;
				nxtPkt[1] = fileID;
				nxtPkt[2] = (byte) (pktNo % Utils.PKTNORANGE);
				// copy data bytes from the input file into data part of the
				// packet, i.e., after the header
				System.arraycopy(fileContents, filePointer, nxtPkt,
						Utils.HEADER, datalen);
				filePointer += Utils.DATASIZE;
			}
			sendPacket(pkt, nxtPkt);
			attempt++;
			if (attempt > 1) {
				noRetransmissions++;
			}
			packetID = nxtPkt[2];
			receivedACK = false;
			receivePacket(5);
			if (paused) {
				print("Sending paused.");
				while (paused) {
					if (!isClient) {
						receivePacket(0);
					}
					else {
						System.out.print("");
						// nothing here
					}
				}
				print("Sending resumed.");
			}
		}
		endTime = System.nanoTime();
		print("File successfully transfered.");
		statistics();

	}

	private void receiveFile()
			throws SocketException, IOException, InterruptedException {
		startTime = System.nanoTime();
		print("Receiving...");
		while (receiving) {
			if (paused) {
				// print("Receiving paused.");
				if (!isClient) {
					receivePacket(0);
				}
				else {
					System.out.print("");
					// nothing here
				}
			}
			else {
				receivePacket(10);
			}

		}
		endTime = System.nanoTime();
		statistics();
	}

	
	public void statistics() {
		if (isClient) {
			String result = "\n== Statistics ==\n";
			long elapsedTime = endTime - startTime;
			double seconds = ((double) elapsedTime) / 1E9;
			double avSpeed = bytesFile / seconds;
			if (avSpeed > 1024 && avSpeed < 1024 * 1024) {
				double speedKB = avSpeed / 1024;
				result += String.format(
						"Average speed   %.1f KB/s \nPackets         %d ",
						speedKB, expNoPkts);
			}
			else if (avSpeed > 1024 * 1024) {
				double speedMB = avSpeed / (1024 * 1024);
				result += String.format(
						"Average speed   %.1f MB/s \nPackets         %d ",
						speedMB, expNoPkts);
			}
			else {
				result += String.format(
						"Average speed   %.1f B/s  \nPackets         %d ",
						avSpeed, expNoPkts);
			}
			if (noRetransmissions != -1) {
				result += String.format("\nRetransmissions %d",
						noRetransmissions);
			}

			print(result);
		}

	}

	public void setPause(boolean paused) {
		this.paused = paused;
	}

	private void print(String msg) {
		if (isClient) {
			ClientApplication.print(msg);
		}
		else {
			ServerApplication.print(msg);
		}
	}

	private void err(String msg) {
		if (isClient) {
			ClientApplication.err(msg);
		}
		else {
			ServerApplication.err(msg);
		}
	}

}
