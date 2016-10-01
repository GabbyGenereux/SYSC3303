import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
	private byte[] readReq = {0, 1};
	private byte[] writeReq = {0, 2};
	private boolean testMode = false;
	private int wellKnownPort;
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendAndReceiveSocket;

	public boolean isTestMode() {
		return testMode;
	}

	public void setTestMode(boolean testMode) {
		if (testMode) wellKnownPort = 23;
		else wellKnownPort = 69;
		this.testMode = testMode;
	}

	
	/*
	 * Constructor
	 * Initializes the Datagram Socket
	 */
	public Client() {
		try {
			sendAndReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}	
	}

	private void sendRequest(byte[] reqType, String filename, String mode) throws UnknownHostException {
		byte[] message = formatRequest(reqType, filename, mode);
		
		DatagramPacket requestPacket = new DatagramPacket(message, message.length, InetAddress.getLocalHost(), wellKnownPort);
		
		try {
			sendAndReceiveSocket.send(requestPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void readFromServer(String filename, String mode) throws IOException{
		System.out.println("Initiating read request with file " + filename);
		
		sendRequest(readReq, filename, mode);
		byte[] receivedData;
		int currentBlockNumber = 1;
		
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
		
		while (true) {
			receivedData = new byte[2 + 2 + 512]; // opcode + blockNumber + 512 bytes of data
			receivePacket = new DatagramPacket(receivedData, receivedData.length);
			System.out.println("Waiting for block of data...");
			// receive block
			sendAndReceiveSocket.receive(receivePacket);
			TFTPInfoPrinter.printReceived(receivePacket);
			
			// validate packet
			receivedData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

			int blockNum = getBlockNumberInt(receivedData);
			System.out.println("Received block of data, Block#: " + blockNum);
			
			if (blockNum != currentBlockNumber) {
				continue; 
			}
			byte[] dataBlock = Arrays.copyOfRange(receivedData, 4, receivedData.length); // 4 is where the data starts, after opcode + blockNumber
			
			// Write dataBlock to file
			out.write(dataBlock);
			
			System.out.println("Sending Ack...");
			// Send ack back
			byte[] ack = createAck(blockNum);
			
			// Initial request was sent to wellKnownPort, but steady state file transfer should happen on another port.
			sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), receivePacket.getPort());
			sendAndReceiveSocket.send(sendPacket);
			TFTPInfoPrinter.printSent(sendPacket);
			currentBlockNumber++;
			
			// check if block is < 512 bytes which signifies end of file
			if (dataBlock.length < 512) { 
				System.out.println("Data was received that was less than 512 bytes in length");
				System.out.println("Total transfers that took place: " + blockNum);
				break; 
			}
		}
		out.close();
	}
	
	private byte[] createAck(int blockNum) {
		byte[] ack = new byte[4];
		ack[0] = 0; //
		ack[1] = 4; // Opcode
		byte[] bn = convertBlockNumberByteArr(blockNum);
		ack[2] = bn[0];
		ack[3] = bn[1];
		
		return ack;
	}
	
	public void writeToServer(String filename, String mode) throws IOException {
		sendRequest(writeReq, filename, mode);
		int currentBlockNum = 0;
		
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
		
		while (true) {
			// receive ACK from previous dataBlock
			byte[] data = new byte[4];
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("Client is waiting to receive ACK from server");
			sendAndReceiveSocket.receive(receivePacket);
			TFTPInfoPrinter.printReceived(receivePacket);
			// need block number
			int blockNum = getBlockNumberInt(receivePacket.getData());
			if (blockNum != currentBlockNum) {
				System.out.println("&&&&&&" + blockNum + "   " + currentBlockNum);
				continue;
			}
			
			// increment block number then send that block
			currentBlockNum++;
			
			byte[] dataBlock = new byte[512];
			
			// Resize dataBlock to total bytes read
			int bytesRead = in.read(dataBlock);
			if (bytesRead == -1) bytesRead = 0;
			dataBlock = Arrays.copyOf(dataBlock, bytesRead);
			
			byte[] sendData = formatData(dataBlock, currentBlockNum);
			// Initial request was sent to wellKnownPort, but steady state file transfer should happen on another port.
			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), receivePacket.getPort());
			sendAndReceiveSocket.send(sendPacket);
			
			TFTPInfoPrinter.printSent(sendPacket);
			
			if (bytesRead < 512) break;
		}
		
		in.close();
	}
	
	
	private int getBlockNumberInt(byte[] data) {
		int blockNum;
		// Check opcodes
		
		// Big Endian 
		blockNum = data[2];
		blockNum <<= 8;
		blockNum += data[3];
		// 
		return blockNum;
	}
	private byte[] convertBlockNumberByteArr(int blockNumber) {
		return new byte[] {(byte)((blockNumber >> 8) & 0xFF), (byte)(blockNumber & 0xFF)};
	}
	
	public void shutdown() {
		sendAndReceiveSocket.close();
		System.exit(1);
	}
	
	
	/**
	 * 
	 * @param reqType
	 * @param filename
	 * @param mode
	 * @return
	 */
	private byte[] formatRequest(byte[] reqType, String filename, String mode) {
		byte[] request = new byte[reqType.length + filename.length() + mode.length() + 2]; // +2 for the zero byte after filename and after mode.
		byte[] filenameData = filename.getBytes();
		byte[] modeData = mode.toLowerCase().getBytes();
		int i, j, k;
		
		for (i = 0; i < reqType.length; i++) {
			request[i] = reqType[i];
		}

		for (j = 0; j < filenameData.length; j++) {
			request[i + j] = filenameData[j];
		}
		request[i + j] = 0; // zero byte after filename.
		j++; 
		for (k = 0; k < modeData.length; k++) {
			request[i + j + k] = modeData[k];
		}
		
		request[i + j + k] = 0; // final zero byte.

		return request;
	}
	
	private byte[] formatData(byte[] data, int blockNumber) {
		
		byte[] formatted = new byte[data.length + 4]; // +4 for opcode and datablock number (2 bytes each)
		byte[] blockNumData = convertBlockNumberByteArr(blockNumber);
		int i;
		
		// opcode
		formatted[0] = 0;
		formatted[1] = 3;
		// blockNumber
		formatted[2] = blockNumData[0];
		formatted[3] = blockNumData[1];
		
		for (i = 0; i < data.length; i++) {
			formatted[i + 4] = data[i];
		}

		return formatted;
	}
	
	public static void main(String args[]) {
		Client c = new Client();
		
		System.out.println("Hello! Please type which mode to run in; normal or test: (n/t)");
		Scanner s = new Scanner(System.in);
		String mode = s.nextLine().toLowerCase();
		
		if (mode.equals("n") || mode.equals("normal")) {
			c.setTestMode(false);
		}
		else if (mode.equals("t") || mode.equals("test")) {
			c.setTestMode(true);
		}
		
		System.out.println("Now choose whether you would like to run in quiet or verbose mode:");
		String response = s.nextLine();
		
		while (true) {
			System.out.println("Please enter in the file name (or \"shutdown\" to exit):");
			String fileName = s.nextLine();
			if (fileName.equals("shutdown")) break;
			
			System.out.println("Read or Write? (r/w)");
			String action = s.nextLine().toLowerCase();
			
			try {
				if (action.equals("r") || action.equals("read")) {
					c.readFromServer(fileName, mode);
					System.out.println("Transfer complete");
				}
				else if (action.equals("w") || action.equals("write")) {
					c.writeToServer(fileName, mode);
					System.out.println("Transfer complete");
				}
				else {
					System.out.println("Invalid command");
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		s.close();
		c.shutdown();
	}

}