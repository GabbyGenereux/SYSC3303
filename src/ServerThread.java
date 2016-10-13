import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
	byte[] data;
	
	// Opcodes
	private static final byte[] readReqOP = {0, 1};
	private static final byte[] writeReqOP = {0, 2};
	private static final byte[] dataOP = {0, 3};
	private static final byte[] ackOP = {0, 4};
	private static final byte[] errorOP = {0, 5};
	
	public ServerThread(String name, DatagramPacket receivedPacket, byte[] receivedData){
		super(name);
		receivePacket = receivedPacket;
		data = receivedData;
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while(data[i+2] != 0x00) {
			sb.append((char)data[i+2]);
			i++;
		}
		file = sb.toString();
		
	}
	
	public void run(){

		// Determination of type of packet received
		byte[] opcode = {data[0], data[1]};
		
		if (Arrays.equals(opcode, readReqOP)) {
			try {
				writeToClient(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if (Arrays.equals(opcode, writeReqOP)){
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
	
	private byte[] convertBlockNumberByteArr(int blockNumber) 
	{
		return new byte[] {(byte)((blockNumber >> 8) & 0xFF), (byte)(blockNumber & 0xFF)};
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
		BufferedInputStream in = new BufferedInputStream(new FileInputStream("ServerFiles/" + filename));
		while(true)
		{
			data = new byte[516]; //2 bytes for opcode, 2 bytes for block number, 512 bytes for data
			byte[] block = convertBlockNumberByteArr(currentBlockNumber); //convert the integer block number to big endian word
			byte[] dataBlock = new byte[512];
			data[0] = dataOP[0];
			data[1] = dataOP[1]; //DATA opcode
			data[2] = block[0];
			data[3] = block[1]; //block number

			int bytesRead = in.read(dataBlock);
			if (bytesRead == -1) bytesRead = 0; 
			System.out.println("Bytes read: " + bytesRead);
			System.arraycopy(dataBlock, 0, data, 4, bytesRead);
			
			
			//send the data to the client
			sendPacket = new DatagramPacket(data, bytesRead + 4, receivePacket.getAddress(), receivePacket.getPort());
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
			
			int blockNum = getBlockNumberInt(receivePacket.getData());
			
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
		int currentBlockNumber = 0;
		
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("ServerFiles/" + filename));
		sendReceiveSocket = new DatagramSocket();
		while (true) {
			
			System.out.println("Sending Ack...");
			// Send ack back
			byte[] ack = createAck(currentBlockNumber);
			
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

			int blockNum = getBlockNumberInt(receivedData);
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
			
			// There might be a more efficient method than this.
			byte[] dataBlock = Arrays.copyOfRange(receivedData, 4, receivedData.length); // 4 is where the data starts, after opcode + blockNumber
			
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
	private int getBlockNumberInt(byte[] data) {
		int blockNum;
		// Check opcodes
		
		// Big Endian 
		blockNum = data[2];
		blockNum <<= 8;
		blockNum |= data[3];
		
		return blockNum;
	}
	
	private byte[] createAck(int blockNum) {
		byte[] ack = new byte[4];
		ack[0] = ackOP[0]; //
		ack[1] = ackOP[1]; // Opcode
		byte[] bn = convertBlockNumberByteArr(blockNum);
		ack[2] = bn[0];
		ack[3] = bn[1];
		
		return ack;
	}
}
