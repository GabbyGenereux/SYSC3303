import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
	private static final int bufferSize = 516;
	private static final int blockSize = 512;
	private boolean testMode = false;
	private int wellKnownPort;
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendAndReceiveSocket;

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
	//receives a packet on the socket given with a timeout of 5 seconds, eventually gives up after a few timeouts
	//returns false if unsuccessful, true if successful
	private boolean packetReceiveWithTimeout(DatagramSocket socket, DatagramPacket packet) throws IOException
	{
		socket.setSoTimeout(5000);		//set timeout to 5000 ms (5 seconds)
		int numTimeouts = 0;
		boolean receivedOrSent = false;
		while(numTimeouts < 5 & !receivedOrSent)
		{
			receivedOrSent = true;
			try{
				socket.receive(packet);
			} catch(SocketTimeoutException e)
			{
				receivedOrSent = false;	
				numTimeouts++;
				System.out.println("Timed out, retrying transfer.");					
			}
		}
		if(numTimeouts >= 5)
		{
			System.out.println("Transfer failed, timed out too many times.");
			return false;
		}
		return true;
	}
	
	//sends a packet on the socket given with a timeout of 5 seconds, eventually gives up after a few timeouts
	//returns false if unsuccessful, true if successful
	private boolean packetSendWithTimeout(DatagramSocket socket, DatagramPacket packet) throws IOException
	{
		socket.setSoTimeout(5000);		//set timeout to 5000 ms (5 seconds)
		int numTimeouts = 0;
		boolean receivedOrSent = false;
		while(numTimeouts < 5 & !receivedOrSent)
		{
			receivedOrSent = true;
			try{
				socket.send(packet);
			} catch(SocketTimeoutException e)
			{
				receivedOrSent = false;	
				numTimeouts++;
				System.out.println("Timed out, retrying transfer.");					
			}
		}
		if(numTimeouts >= 5)
		{
			System.out.println("Transfer failed, timed out too many times.");
			return false;
		}
		return true;
	}
	
	private void sendRequest(byte[] reqType, String filename, String mode) throws UnknownHostException {
		RequestPacket p = new RequestPacket(reqType, filename, mode);
		byte[] message = p.encode();
		
		DatagramPacket request = new DatagramPacket(message, message.length, InetAddress.getLocalHost(), wellKnownPort);
		boolean sent = false;
		try {
			sent = packetSendWithTimeout(sendAndReceiveSocket, request);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(sent)
			TFTPInfoPrinter.printSent(request);
	}
	
	public void readFromServer(String filename, String mode) throws IOException{		
		System.out.println("Initiating read request with file " + filename);
		
		
		byte[] receivedData;
		byte[] receivedOpcode;
		int currentBlockNumber = 1;
		
		if(new File("ClientFiles/" + filename).exists()){
			System.err.println('"' + filename + '"' + " already exists on Client.");
			return;
		}
		BufferedOutputStream out = null;
		
		try {
			out = new BufferedOutputStream(new FileOutputStream("ClientFiles/" + filename));
		} catch (IOException e) {
			if (e.getMessage().contains("(Access is denied)")){
				System.err.println("Access to ClientFiles folder was denied");
				return;
			}
			else {
				System.err.println("Unknown file error");
			}
		}
		
		
		sendRequest(RequestPacket.readOpcode, filename, mode);
		while (true) {
			receivedData = new byte[bufferSize];
			receivePacket = new DatagramPacket(receivedData, receivedData.length);
			// receive block
			
			if(!packetReceiveWithTimeout(sendAndReceiveSocket, receivePacket))
			{
				out.close();
				return;
			}
			TFTPInfoPrinter.printReceived(receivePacket);
			
			// validate packet
			receivedData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
			receivedOpcode = Arrays.copyOf(receivedData, 2);

			
			if (Arrays.equals(receivedOpcode, ErrorPacket.opcode)){
				ErrorPacket ep = new ErrorPacket(receivedData);
				System.err.println(ep.getErrorMessage());
				// Handle error.
				
				// File not found on server
				if (ep.getErrorCode() == 1){
					
				}
				// Access denied on server
				else if (ep.getErrorCode() == 2) {
					
				}
				out.close();
				return;
			}
			// The received packet should be an DATA packet at this point, and this have the Opcode defined in dataOP.
			// If it is not an error packet or an DATA packet, something happened (these cases are in later iterations).
			else if (!Arrays.equals(receivedOpcode, DataPacket.opcode)) {
				// Do nothing special for Iteration 2.
			}
			
			DataPacket dp = new DataPacket(receivedData);
			
			int blockNum = dp.getBlockNum();
			//System.out.println("Received block of data, Block#: " + currentBlockNumber);
			boolean duplicateDataPacket = false;
			if (blockNum != currentBlockNumber) {
				
				if (blockNum == 0) {
					currentBlockNumber -= 65536;
				}
				
				 // If they're still not equal, another problem occurred.
				if (blockNum != currentBlockNumber)
				{
					if(currentBlockNumber > blockNum)
					{
						//received duplicate data packet
						duplicateDataPacket = true;
						currentBlockNumber += 65536; // Restore block number since packet was a duplicate
					}
					else {
						System.out.println("Block Numbers not valid or duplicate" + blockNum + " " + currentBlockNumber + " " + currentBlockNumber % 256);
						System.exit(1);
					}
				}
			}
			byte[] dataBlock = dp.getDataBlock();
			
			// Write dataBlock to file
			if(!duplicateDataPacket) 	// do not write duplicate dataBlocks to file
			{
				try{
					out.write(dataBlock);
				}
				catch(IOException e){ //disk full
					String msg = "Unable to write file " +filename+", disk space full";
					System.err.println(msg);
					ErrorPacket errPckt = new ErrorPacket((byte) 3, msg);
					byte[] err = errPckt.encode();
					sendPacket = new DatagramPacket(err, err.length, InetAddress.getLocalHost(), receivePacket.getPort());
					sendAndReceiveSocket.send(sendPacket);
					if(!packetSendWithTimeout(sendAndReceiveSocket, sendPacket))
					{
						out.close();
						return;
					}
					try {
						out.close();
					} catch (IOException e2) {
						
					}
					return;
				}
			}
			// Send ack back
			// Duplicate dataBlocks are still ACKed, but are not written to file
			AckPacket ap = new AckPacket(blockNum);
			
			// Initial request was sent to wellKnownPort, but steady state file transfer should happen on another port.
			sendPacket = new DatagramPacket(ap.encode(), ap.encode().length, InetAddress.getLocalHost(), receivePacket.getPort());
			if(!packetSendWithTimeout(sendAndReceiveSocket, sendPacket))
			{
				out.close();
				return;
			}
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
		System.out.println("Transfer complete");
	}
	
	public void writeToServer(String filename, String mode) throws IOException {
		
		int currentBlockNumber = 0;
		BufferedInputStream in = null;
		byte[] receivedData;
		byte[] receivedOpcode;
		
		
		
		try {
			// It's a full path
			if (filename.contains("\\") || filename.contains("/")) {
				
				if(!(new File(filename).exists())){
					System.err.println(filename + " does not exist on Client.");
					return;
				}
				
				in = new BufferedInputStream(new FileInputStream(filename));
				// for sending to Server
				int idx = filename.lastIndexOf('\\');
				if (idx == -1) {
					idx = filename.lastIndexOf('/');
				} 
				filename = filename.substring(idx+1);
				 
			}
			// It's in the default ClientFiles folder
			else {
				

				if(!(new File("ClientFiles/" + filename).exists())){
					System.err.println(filename + " does not exist on Client.");
					return;
				}
				in = new BufferedInputStream(new FileInputStream("ClientFiles/" + filename));
				
				
			}
		} catch (IOException e) {
			// Don't bother sending the server any error packets as the request hasn't been sent and server doesn't need to know
			// Print out error information/handle error.
			
			if (e.getMessage().contains("(Access is denied)")) {
				System.err.println("Cound not read " + filename + " on Client");
			}
			else {
				e.printStackTrace();
			}
			return;
		}
		
		sendRequest(RequestPacket.writeOpcode, filename, mode);
		
		while (true) {
			// receive ACK from previous dataBlock
			byte[] data = new byte[bufferSize];
			receivePacket = new DatagramPacket(data, data.length);	
			if(!packetReceiveWithTimeout(sendAndReceiveSocket, receivePacket))
			{
				in.close();
				return;
			}
			TFTPInfoPrinter.printReceived(receivePacket);
			
			receivedData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
			receivedOpcode = Arrays.copyOf(receivedData, 2);
			
			if (Arrays.equals(receivedOpcode, ErrorPacket.opcode)){
				ErrorPacket ep = new ErrorPacket(receivedData);
				System.err.println(ep.getErrorMessage());
				// Access denied, can't write to server
				if (ep.getErrorCode() == 2) {
					
				}
				// File already exits (on server)
				else if (ep.getErrorCode() == 6) {
					
				}
				
				in.close();
				return;
			}
			// The received packet should be an ACK packet at this point, and this have the Opcode defined in ackOP.
			// If it is not an error packet or an ACK packet, something happened (these cases are in later iterations).
			else if (!Arrays.equals(receivedOpcode, AckPacket.opcode)) {
				// Do nothing special for Iteration 2.
			}
			
			AckPacket ap = new AckPacket(receivedData);
			// need block number
			int blockNum = ap.getBlockNum();
			
			boolean duplicateDataPacket = false;
			if (blockNum != currentBlockNumber) {
				
				if (blockNum == 0) {
					currentBlockNumber -= 65536;
				}
				
				 // If they're still not equal, another problem occurred.
				if (blockNum != currentBlockNumber)
				{
					if(currentBlockNumber > blockNum)
					{
						//received duplicate data packet
						duplicateDataPacket = true;
						currentBlockNumber += 65536; // Restore block number since packet was a duplicate
					}
					else {
						System.out.println("Block Numbers not valid or duplicate" + blockNum + " " + currentBlockNumber + " " + currentBlockNumber % 256);
						System.exit(1);
					}
				}
			}
			
			// increment block number then send that block
			currentBlockNumber++;
			
			byte[] dataBlock = new byte[blockSize];
			
			// Resize dataBlock to total bytes read
			int bytesRead = in.read(dataBlock);
			if (bytesRead == -1) bytesRead = 0;
			dataBlock = Arrays.copyOf(dataBlock, bytesRead);
			
			DataPacket dp = new DataPacket(currentBlockNumber, dataBlock);
			byte[] sendData = dp.encode();
			// Initial request was sent to wellKnownPort, but steady state file transfer should happen on another port.
			sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), receivePacket.getPort());
			if(!packetSendWithTimeout(sendAndReceiveSocket, sendPacket))
			{
				in.close();
				return;
			}
			TFTPInfoPrinter.printSent(sendPacket);
			
			if (bytesRead < 512) break;
		}
		
		in.close();
		System.out.println("Transfer complete");
	}
	
	
	public void shutdown() {
		sendAndReceiveSocket.close();
		System.exit(1);
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
		
		System.out.println("Now choose whether you would like to run in quiet or verbose mode (q/v):");
		String response = s.nextLine();
		if (response.equals("q")) {
			TFTPInfoPrinter.setVerboseMode(false);
		}
		else if (response.equals("n")) {
			TFTPInfoPrinter.setVerboseMode(true);
		}
		
		while (true) {
			System.out.println("Please enter in the file name (or \"shutdown\" to exit):");
			String fileName = s.nextLine();
			if (fileName.equals("shutdown")) break;
			
			System.out.println("Read or Write? (r/w)");
			String action = s.nextLine().toLowerCase();
			
			try {
				if (action.equals("r") || action.equals("read")) {
					//check if a file with that name exists on the client side
					c.readFromServer(fileName, "octet");

				}
				else if (action.equals("w") || action.equals("write")) {
					c.writeToServer(fileName, "octet");
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
