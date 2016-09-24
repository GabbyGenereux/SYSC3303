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
	private boolean exit;
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendAndReceiveSocket;

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
		if (testMode) wellKnownPort = 23;
		else wellKnownPort = 69;	
	}

	void sendRequest(byte[] reqType, String filename, String mode) throws UnknownHostException {
		byte[] message = formatRequest(reqType, filename, mode);
		
		DatagramPacket requestPacket = new DatagramPacket(message, message.length, InetAddress.getLocalHost(), wellKnownPort);
		
		try {
			sendAndReceiveSocket.send(requestPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void readFromServer(String filename, String mode) throws IOException{
		System.out.println("Initiating read request with file " + filename);
		
		sendRequest(readReq, filename, mode);
		byte[] receivedData;
		int currentBlockNumber = 1;
		while (true) {
			receivedData = new byte[2 + 2 + 512]; // opcode + blockNumber + 512 bytes of data
			receivePacket = new DatagramPacket(receivedData, receivedData.length);
			System.out.println("Waiting for block of data...");
			// receive block
			sendAndReceiveSocket.receive(receivePacket);
			
			// validate packet
			receivedData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

			int blockNum = getBlockNumberInt(receivedData);
			System.out.println("Received block of data, Block#: " + blockNum);
			
			if (blockNum != currentBlockNumber) {
				continue; // Ignore? Do something else? Maybe need to reconsider.
			}
			// There might be a more efficient method than this.
			byte[] dataBlock = Arrays.copyOfRange(receivedData, 4, receivedData.length); // 4 is where the data starts, after opcode + blockNumber
			// TODO: write dataBlock to file
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
			
			out.write(dataBlock, 0, dataBlock.length);
			
			System.out.println("Sending Ack...");
			// Send ack back
			byte[] ack = createAck(blockNum);
			// Initial request was sent to wellKnownPort, but steady state file transfer should happen on another port.
			sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), receivePacket.getPort());
		
			sendAndReceiveSocket.send(sendPacket);
			currentBlockNumber++;
			
			// check if block is < 512 bytes which signifies end of file
			if (dataBlock.length < 512) { // Magic number? Consider using a constant for this.
				System.out.println("Data was received that was less than 512 bytes in length");
				System.out.println("Total transfers that took place: " + blockNum);
				break; 
			}
		}
	}
	
	byte[] createAck(int blockNum) {
		byte[] ack = new byte[4];
		ack[0] = 0; //
		ack[1] = 4; // Opcode
		byte[] bn = convertBlockNumberByteArr(blockNum);
		ack[2] = bn[0];
		ack[3] = bn[1];
		
		return ack;
	}
	
	void writeToServer(String filename, String mode) throws IOException {
		sendRequest(writeReq, filename, mode);
		int currentBlockNum = 0;
		while (true) {
			// receive ACK from previous dataBlock
			byte[] data = new byte[4];
			receivePacket = new DatagramPacket(data, data.length);
			sendAndReceiveSocket.receive(receivePacket);
			// need block number
			int blockNum = getBlockNumberInt(receivePacket.getData());
			if (blockNum != currentBlockNum) continue;
			
			// increment block number then send that block
			currentBlockNum++;
			
			byte[] dataBlock = new byte[512];
			
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
			in.read(dataBlock, 0, dataBlock.length);
			
			byte[] sendData = formatData(dataBlock, blockNum);
			// Initial request was sent to wellKnownPort, but steady state file transfer should happen on another port.
			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), receivePacket.getPort());
			sendAndReceiveSocket.send(sendPacket);			
		}
	}
	
	
	private int getBlockNumberInt(byte[] data) {
		int blockNum;
		// Check opcodes
		
		// Big Endian 
		blockNum = data[2];
		blockNum <<= 8;
		blockNum += data[3];
		
		return blockNum;
	}
	private byte[] convertBlockNumberByteArr(int blockNumber) {
		return new byte[] {(byte)((blockNumber >> 8) & 0xFF), (byte)(blockNumber & 0xFF)};
	}
	
	private void shutdown() {
		sendAndReceiveSocket.close();
		System.exit(1);
	}
	
	/**
	 * 
	 * @param fileName
	 * @param mode
	 */
	public void sendAndReceive(String fileName, String mode) {
		
		//side of array determined by the length of the two strings, plus the 4 bytes added to the array
		byte message[];
		//Repeat 10 times, alternating between read and write request
		//for (int j = 0; j < 10; j++) {
		
		//j preserves the old functionality, however to shut down the exit var must be set to true.
		//Once user input is added, the user can choose when to shutdown rather than just using j>=10
		int j = 0;
		while(!exit) {
			//read request
			if (j % 2 == 0) {
				message = formatRequest(readReq, fileName, mode);
			} else {
				//write request
				message = formatRequest(writeReq, fileName, mode);
			}
			System.out.println("Client: Sending packet ");

			//create Datagram packet to send
			try {
				sendPacket = new DatagramPacket(message, message.length, InetAddress.getLocalHost(), 23);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			//Print out packet information
			System.out.println("to host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			int len = sendPacket.getLength();
			System.out.println("Length: " + len);
			System.out.print("Containing: ");
			System.out.println(new String(sendPacket.getData(), 0, len));			
			for(int k = 0; k < sendPacket.getData().length; k++)
			{
				System.out.print(sendPacket.getData()[k] + " ");
			}
			System.out.println("\n");
			//send packet
			try {
				sendAndReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Client: Packet sent.");
			
			System.out.println("Client: Waiting for packet..");

			byte data[] = new byte[4];
			receivePacket = new DatagramPacket(data, data.length, sendPacket.getAddress(), sendPacket.getPort());
			//Receive packet
			try {
				sendAndReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			//print received packet information
			System.out.println("Client: Packet received");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.print("Containing: ");
			String received = new String(data, 0, len);
			System.out.println(received);
			for(int k = 0; k < receivePacket.getData().length; k++)
			{
				System.out.print(receivePacket.getData()[k] + " ");
			}
			System.out.println("\n");
			j++;
			if (j >= 10)
				exit = true;
		}
		message = new byte[] {1, 2, 3, 4, 5}; // incorrect formatting 
		System.out.println("Client: Sending final packet.");

		sendPacket.setData(message);
		sendPacket.setLength(message.length);
		try {
			sendPacket.setAddress(InetAddress.getLocalHost());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		sendPacket.setPort(23);
		//Print out packet information
		System.out.println("to host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing: ");
		System.out.println(new String(sendPacket.getData(), 0, len));			
		for(int l = 0; l < sendPacket.getData().length; l++)
		{
			System.out.print(sendPacket.getData()[l] + " ");
		}
		System.out.println("\n");
		//send packet
		try {
			sendAndReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Client: Packet sent.");
		shutdown();
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
	
byte[] formatData(byte[] data, int blockNumber) {
		
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
		System.out.println("Hello! Please type which mode to run in; normal or test:");
		Scanner s = new Scanner(System.in);
		String mode = s.nextLine();
		System.out.println("Now choose whether you would like to run in quiet or verbose mode:");
		String response = s.nextLine();
		System.out.println("Please enter in the file name:");
		String fileName = s.nextLine();
		Client c = new Client();
		//TODO: Need to figure out what to do with quiet/verbose mode and normal/test mode
		c.sendAndReceive(fileName, "octet");
	}

}