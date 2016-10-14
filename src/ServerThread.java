import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;


public class ServerThread extends Thread{
	DatagramPacket receivePacket, sendPacket;
	DatagramSocket sendSocket, sendReceiveSocket;
	String file;
	byte[] receivedData;
	
	public ServerThread(String name, DatagramPacket receivedPacket, byte[] receivedData){
		super(name);
		receivePacket = receivedPacket;
		this.receivedData = receivedData;
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while(receivedData[i+2] != 0x00) {
			sb.append((char)receivedData[i+2]);
			i++;
		}
		file = sb.toString();
		
	}
	
	public void run(){

		// Determination of type of packet received
		byte[] opcode = {receivedData[0], receivedData[1]};
		
		if (Arrays.equals(opcode, RequestPacket.readOpcode)) {
			try {
				writeToClient(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if (Arrays.equals(opcode, RequestPacket.writeOpcode)){
			try {
				readFromClient(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//else if (Arrays.equals(opcode, error)) {} For future use, error packets will use this.
		// Some sort of packet or data not expected at this time
		else {
			System.out.println("Invalid packet, exiting.");
			System.exit(1);
		}
	}
	
	//send the file from the server to the client, given the filename (for a client's read request)
	//should be called after the initial request has been read
	void writeToClient(String filename) throws IOException
	{
		//Create socket to send out packets and receive ACKs
		try 
		{
			sendReceiveSocket = new DatagramSocket();
		} 
		catch (SocketException e1) 
		{
			e1.printStackTrace();
			System.exit(1);
		}
		
		int currentBlockNumber = 1; //starting with the first block of 512 bytes
		byte[] data;
		byte[] opcode;
		BufferedInputStream in = new BufferedInputStream(new FileInputStream("ServerFiles/" + filename));
		while(true)
		{
			byte[] dataBlock = new byte[512];

			int bytesRead = in.read(dataBlock);
			if (bytesRead == -1) bytesRead = 0; 
			dataBlock = Arrays.copyOf(dataBlock, bytesRead);
			System.out.println("Bytes read: " + bytesRead);
			
			DataPacket dp = new DataPacket(currentBlockNumber, dataBlock);
			data = dp.encode();
			
			//send the data to the client
			sendPacket = new DatagramPacket(data, data.length, receivePacket.getAddress(), receivePacket.getPort());
			try
			{
				sendReceiveSocket.send(sendPacket);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			TFTPInfoPrinter.printSent(sendPacket);
			
			//receive the ACK from the client
			byte[] ack = new byte[4];
			receivePacket = new DatagramPacket(ack, ack.length);
			try
			{
				sendReceiveSocket.receive(receivePacket);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			TFTPInfoPrinter.printReceived(receivePacket);
			
			//TODO: Need to validate ACK still
			
			opcode = Arrays.copyOf(receivePacket.getData(), 2);

			if (Arrays.equals(opcode, ErrorPacket.opcode)){
				// Determine error code.
				// Handle error.
			}
			// The received packet should be an ACK packet at this point, and this have the Opcode defined in ackOP.
			// If it is not an error packet or an ACK packet, something happened (these cases are in later iterations).
			else if (!Arrays.equals(opcode, AckPacket.opcode)) {
				// Do nothing special for Iteration 2.
			}
			byte[] dataReceived = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
			AckPacket ap = new AckPacket(dataReceived);
			
			int blockNum = ap.getBlockNum();
			
			// Note: 256 is the maximum size of a 16 bit number.
			if (blockNum != currentBlockNumber ) {
				if (blockNum < 0) {
					blockNum += 256; // If the block rolls over (it's a 16 bit number represented as unsigned)
				}
				// If they're still not equal, another problem occurred.
				if (blockNum != currentBlockNumber % 256)
				{
					System.out.println("Block Numbers not the same, exiting " + blockNum + " " + currentBlockNumber + " " + currentBlockNumber % 256);
					System.exit(1);
				}	
			}
			
			if (bytesRead < 512) break;
			//get ready to send the next block of bytes
			currentBlockNumber++;
		}
		in.close();
	}
	
	public void readFromClient(String filename) throws IOException{
		System.out.println("Reading from client: " + filename);
	
		// Already received request
		// Send ACK with blockNumber 0 ... N;
		// Receive dataBlock (blockNumber++)
		byte[] receivedData;
		byte[] opcode;
		int currentBlockNumber = 0;
		
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("ServerFiles/" + filename));
		sendReceiveSocket = new DatagramSocket();
		while (true) {
			
			System.out.println("Sending Ack...");
			// Send ack back
			AckPacket ap = new AckPacket(currentBlockNumber);
			byte[] ack = ap.encode();
			
			// Initial request was sent to wellKnownPort, but steady state file transfer should happen on another port.
			sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), receivePacket.getPort());
			sendReceiveSocket.send(sendPacket);
			TFTPInfoPrinter.printSent(sendPacket);
			currentBlockNumber++;
			
			receivedData = new byte[2 + 2 + 512]; // opcode + blockNumber + 512 bytes of data
			receivePacket = new DatagramPacket(receivedData, receivedData.length);
			System.out.println("Waiting for block of data...");
			// receive block
			sendReceiveSocket.receive(receivePacket);
			TFTPInfoPrinter.printReceived(receivePacket);
			
			// validate packet
			receivedData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

			
			opcode = Arrays.copyOf(receivedData, 2);

			if (Arrays.equals(opcode, ErrorPacket.opcode)){
				// Determine error code.
				// Handle error.
			}
			// The received packet should be an DATA packet at this point, and this have the Opcode defined in ackOP.
			// If it is not an error packet or an DATA packet, something happened (these cases are in later iterations).
			else if (!Arrays.equals(opcode, DataPacket.opcode)) {
				// Do nothing special for Iteration 2.
			}
			
			DataPacket dp = new DataPacket(receivedData);
			int blockNum = dp.getBlockNum();
			System.out.println("Received block of data, Block#: " + blockNum);
			
			// Note: 256 is the maximum size of a 16 bit number.
			if (blockNum != currentBlockNumber ) {
				if (blockNum < 0) {
					blockNum += 256; // If the block rolls over (it's a 16 bit number represented as unsigned)
				}
				// If they're still not equal, another problem occurred.
				if (blockNum != currentBlockNumber % 256)
				{
					System.out.println("Block Numbers not the same, exiting " + blockNum + " " + currentBlockNumber + " " + currentBlockNumber % 256);
					System.exit(1);
				}	
			}
			
			byte[] dataBlock = dp.getDataBlock();
			
			// Write dataBlock to file
			out.write(dataBlock);
			
			
			
			// check if block is < 512 bytes which signifies end of file
			if (dataBlock.length < 512) {
				break; 
			}
		}
		out.close();
		sendReceiveSocket.close();
	}
}
