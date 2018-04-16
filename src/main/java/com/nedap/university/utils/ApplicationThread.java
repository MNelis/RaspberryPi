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
					receivePacket();
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
		}
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

	private void receivePacket() throws SocketException, IOException {

		DatagramPacket pkt = new DatagramPacket(Utils.BUF, Utils.BUF.length);
		socket.setSoTimeout(10000);
		socket.receive(pkt);
		this.pkt.setAddress(pkt.getAddress());
		this.pkt.setPort(pkt.getPort());
		processPacket(pkt);
	}

	private void processPacket(DatagramPacket pkt) throws IOException {
		int flag = pkt.getData()[0];
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

			// Client only.
			case Utils.ACK_LIST :
				String serverData = new String(data);
				print("\n== File List ==\n" + serverData);
				break;

			// Server only.
			case Utils.RQ_FILE :
				String fileName = new String(data);
				print("Received request for the file " + fileName);

				if (Utils.containsFile(false, fileName)) {
					print("File found on the server.");
					fileID = (byte) (Math.random() * 256);
					sendFile(fileID,
							Utils.getFileContents(isClient, fileName),
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
				int expNoPkts = Integer.parseInt(dataSplit[0]);
				String name = dataSplit[1];

				print("Received announcement for file '" + name + "', "
						+ (expNoPkts - 1) + " packets expected.");

				pkts = new byte[expNoPkts][0];
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
						dataACK[0] = Utils.ACK;
						sendPacket(pkt, dataACK);
						// print("Send ACK.");

						if (currentPktNo == pkts.length - 1) {
							byte[] fileContents = Utils.defragmentData(pkts);
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

	private void sendFile(byte fileID, byte[] fileContents,
			String fileName) {
		int filePointer = 0;
		int pktNo = 0;
		int numberOfPackets = fileContents.length / Utils.DATASIZE;
		if (Utils.DATASIZE*numberOfPackets != fileContents.length) {
			numberOfPackets++;
		}
		
		print("" + numberOfPackets);
		byte[] nxtPkt;
//		byte[][] pkts = new byte[numberOfPackets + 1][Utils.HEADER
//				+ Utils.DATASIZE];

		byte[] data = (numberOfPackets + " " + fileName).getBytes();

		nxtPkt = new byte[Utils.HEADER + data.length];

		nxtPkt[0] = Utils.DATA_ANN;
		nxtPkt[1] = fileID;
		// pkts[0][2] = (byte) numberOfPackets;

		System.arraycopy(data, 0, nxtPkt, Utils.HEADER, data.length);

		print("Sending...");
		// creates new packets as long as necessary
		while (filePointer < fileContents.length) {
			sendPacket(pkt, nxtPkt);
			print("Send pkt " + pktNo + ".");
			packetID = nxtPkt[2];
			receivedACK = false;
			try {
				receivePacket();
			}
			catch (SocketException e) {
				print(e.getMessage());
			}
			catch (IOException e) {
				e.getMessage();
			}
			if (receivedACK) {
				pktNo++;
				// create a new packet of appropriate size
				int datalen = Math.min(Utils.DATASIZE,
						fileContents.length - filePointer);
				nxtPkt = new byte[Utils.HEADER + datalen];

				nxtPkt[0] = Utils.DATA;
				nxtPkt[1] = fileID;
				nxtPkt[2] = (byte) (pktNo % Utils.PKTNORANGE);
				// copy data bytes from the input file into data part of the
				// packet, i.e., after
				// the header
				System.arraycopy(fileContents, filePointer, nxtPkt,
						Utils.HEADER, datalen);
				filePointer += Utils.DATASIZE;
			}
		}
		print("File successfully transfered.");
	}

	// private void sendFile(byte[][] pkts) {
	// int i = 0;
	// print("Sending...");
	// while (i < pkts.length) {
	// sendPacket(pkt, pkts[i]);
	// // print("Send pkt " + i + ".");
	// packetID = pkts[i][2];
	// receivedACK = false;
	// try {
	// receivePacket();
	// }
	// catch (SocketException e) {
	// print(e.getMessage());
	// }
	// catch (IOException e) {
	// e.getMessage();
	// }
	// if (receivedACK) {
	// i++;
	// }
	// }
	// print("File successfully transfered.");
	// }

	private void receiveFile()
			throws SocketException, IOException {
		print("Receiving...");
		while (receiving) {
			receivePacket();
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

	private void err(String msg) {
		if (isClient) {
			ClientApplication.err(msg);
		}
		else {
			ServerApplication.err(msg);
		}
	}

}
