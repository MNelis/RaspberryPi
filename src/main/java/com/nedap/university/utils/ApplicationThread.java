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

	public void init() {
		if (pkt.getLength() == 0) {
			if (Utils.containsFile(isClient, msg)) {
				print("File '" + msg + "' found.");
				fileID = (byte) (Math.random() * 256);
				byte[][] dataPkts = Utils.fragmentData(fileID,
						Utils.getFileContents(isClient, msg), msg);
				sendFile(dataPkts);
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
	public void sendPacket(DatagramPacket pkt, byte[] data) {
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

	public void receivePacket() throws SocketException, IOException {

		DatagramPacket pkt = new DatagramPacket(Utils.BUF, Utils.BUF.length);
		socket.setSoTimeout(10000);
		socket.receive(pkt);
		this.pkt.setAddress(pkt.getAddress());
		this.pkt.setPort(pkt.getPort());
		processPacket(pkt);
	}

	public void processPacket(DatagramPacket pkt) throws IOException {
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
//				print("Received ACK.");
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
					byte[][] dataPkts = Utils.fragmentData(fileID,
							Utils.getFileContents(isClient, fileName),
							fileName);
					sendFile(dataPkts);
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
				String[] dataSplit =  new String(data).split(" ", 2);
				int expNoPkts = Integer.parseInt(dataSplit[0]);
				String name = dataSplit[1];
				
				print("Received announcement for file '" + name
						+ "', " + (expNoPkts-1) + " packets expected.");
				
				pkts = new byte[expNoPkts][Utils.DATASIZE];
				pkts[currentPktNo] = name.getBytes();

				byte[] dataACK1 = Arrays.copyOfRange(pkt.getData(), 0,
						Utils.HEADER);
				dataACK1[0] = Utils.ACK;
				sendPacket(pkt, dataACK1);

				receiveFile(isClient);
				break;

			// Both applications.
			case Utils.DATA :
				if (receiving) {
					if ((currentPktNo + 1)%Utils.PKTNORANGE == pkt.getData()[2]) {
						currentPktNo++;
						pkts[currentPktNo] = data;
						byte[] dataACK = Arrays.copyOfRange(pkt.getData(), 0,
								Utils.HEADER);
						dataACK[0] = Utils.ACK;
						sendPacket(pkt, dataACK);
//						print("Send ACK.");

						if (currentPktNo == pkts.length - 1) {
							byte[] fileContents = Utils.defragmentData(pkts);
							String nameFile = new String(pkts[0]);
							print("Received " + nameFile + ", "
									+ fileContents.length + " bytes, " + (pkts.length-1) + " packets.");
							Utils.setFileContents(isClient, fileContents, nameFile);
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

	private void sendFile(byte[][] pkts) {
		int i = 0;
		print("Sending...");
		while (i < pkts.length) {
			sendPacket(pkt, pkts[i]);
//			print("Send pkt " + i + ".");
			packetID = pkts[i][2];
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
				i++;
			}
		}
		print("File successfully transfered.");
	}

	private void receiveFile(boolean isClient)
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
