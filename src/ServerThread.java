import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;


public class ServerThread extends Thread{
	private static final int bufferSize = 516;
	private static final int blockSize = 512;
	
	private DatagramPacket receivePacket, sendPacket;
	private DatagramSocket sendReceiveSocket;
	private String file;
	private byte[] receivedData;
	
	public ServerThread(String name, DatagramPacket receivedPacket, byte[] receivedData){
		super(name);
		receivePacket = receivedPacket;
		this.receivedData = receivedData;
		RequestPacket rp = new RequestPacket(receivedData);
		file = rp.getFilename();
	}
	
	//receives a packet on the socket given with a timeout of 5 seconds, eventually gives up after a few timeouts
	//returns false if unsuccessful, true if successful
	private boolean packetReceiveWithTimeout(DatagramSocket socket, DatagramPacket packet, DatagramPacket resendPacket) throws IOException
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
				System.out.print("Timed out, retrying transfer.");
				socket.send(resendPacket);
			}
		}
		if(numTimeouts >= 5)
		{
			System.out.print("Transfer failed, timed out too many times.");
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
				System.out.print("Timed out, retrying transfer.");					
			}
		}
		if(numTimeouts >= 5)
		{
			System.out.print("Transfer failed, timed out too many times.");
			return false;
		}
		return true;
	}
	public void run(){
		/*
		if (!RequestPacket.isValid(receivedData)){
			// Send error code 04 and stop transfer
			ErrorPacket ep = new ErrorPacket((byte)4, "DATA block number not in sequence or duplicate.");
			try {
				sendReceiveSocket.send(new DatagramPacket(ep.encode(), ep.encode().length));
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}*/
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
	}
	
	//send the file from the server to the client, given the filename (for a client's read request)
	//should be called after the initial request has been read
	void writeToClient(String filename) throws IOException
	{
		System.out.println("Writing to client: " + filename);
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
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream("ServerFiles/" + filename));
		} 
		catch(IOException e)
		{
			Throwable cause = e;
			while(cause.getCause() != null) {
			    cause = cause.getCause();
			}
			System.err.println(cause.getMessage());
			if(cause instanceof FileNotFoundException)
			{
				// Hacky solution to get determine if invalid file permissions.
				if (e.getMessage().contains("(Access is denied)")) {
					String errorString = "Server could not access " + '"' + filename + '"' + ".";
					ErrorPacket ep = new ErrorPacket((byte) 2, errorString);
					
					System.err.println(errorString);
					
					// Send errorPacket
					sendPacket = new DatagramPacket(ep.encode(), ep.encode().length, receivePacket.getAddress(), receivePacket.getPort());
					try
					{
						sendReceiveSocket.send(sendPacket);
					}
					catch (IOException e2)
					{
						e2.printStackTrace();
						System.exit(1);
					}
					TFTPInfoPrinter.printSent(sendPacket);
					return;
				}
				else {
					String errorString = '"' + filename + '"' + " was not found on the server.";
					ErrorPacket ep = new ErrorPacket((byte) 1, errorString);
					
					System.err.println(errorString);
					
					// Send errorPacket
					sendPacket = new DatagramPacket(ep.encode(), ep.encode().length, receivePacket.getAddress(), receivePacket.getPort());
					if(!packetSendWithTimeout(sendReceiveSocket, sendPacket))
					{
						return;
					}
					TFTPInfoPrinter.printSent(sendPacket);
					return;
				}
				
			}
			else
			{
				e.printStackTrace();
			}
		} 
		
			
		
		while(true)
		{
			byte[] dataBlock = new byte[blockSize];

			int bytesRead = 0;
			try {
				bytesRead = in.read(dataBlock);
			} catch (IOException e) {
				
			}
			
			if (bytesRead == -1) bytesRead = 0; 
			dataBlock = Arrays.copyOf(dataBlock, bytesRead);
			
			DataPacket dp = new DataPacket(currentBlockNumber, dataBlock);
			data = dp.encode();
			
			//send the data to the client
			sendPacket = new DatagramPacket(data, data.length, receivePacket.getAddress(), receivePacket.getPort());
			if(!packetSendWithTimeout(sendReceiveSocket, sendPacket))
			{
				in.close();
				return;
			}
			TFTPInfoPrinter.printSent(sendPacket);
			
			//receive the ACK from the client
			byte[] ack = new byte[bufferSize];
			receivePacket = new DatagramPacket(ack, ack.length);
			if(!packetReceiveWithTimeout(sendReceiveSocket, receivePacket, sendPacket))
			{
				in.close();
				return;
			}
			TFTPInfoPrinter.printReceived(receivePacket);
			
			//TODO: Need to validate ACK still
			
			opcode = Arrays.copyOf(receivePacket.getData(), 2);

			if (Arrays.equals(opcode, ErrorPacket.opcode)){
				// Determine error code
				
				ErrorPacket ep = new ErrorPacket(Arrays.copyOf(receivePacket.getData(), receivePacket.getLength()));
				System.err.println(ep.getErrorMessage());
				// As the server, 
				in.close();
				return;
			}
			// The received packet should be an ACK packet at this point, and this have the Opcode defined in ackOP.
			// If it is not an error packet or an ACK packet, something happened (these cases are in later iterations).
			else if (!Arrays.equals(opcode, AckPacket.opcode)) {
				// Send ErrorPacket with error code 04 and stop transfer.
				ErrorPacket ep = new ErrorPacket((byte)4, "Was expecting a ACK packet.");
				sendReceiveSocket.send(new DatagramPacket(ep.encode(), ep.encode().length, InetAddress.getLocalHost(), receivePacket.getPort()));
				return;
			}
			byte[] dataReceived = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
			
			if (!AckPacket.isValid(dataReceived)) {
				// Send ErrorPacket with error code 04 and stop transfer.
				ErrorPacket ep = new ErrorPacket((byte)4, "ACK packet was malformed.");
				sendReceiveSocket.send(new DatagramPacket(ep.encode(), ep.encode().length, InetAddress.getLocalHost(), receivePacket.getPort()));
				return;
			}
			AckPacket ap = new AckPacket(dataReceived);
			
			int blockNum = ap.getBlockNum();
			
			
			boolean duplicateDataPacket = false;
			if (blockNum != currentBlockNumber) {
				
				if (blockNum < 0) {
					blockNum += 65536; // If the block rolls over (it's a 16 bit number represented as unsigned)
					currentBlockNumber -= 65536;
				}
				 // If they're still not equal, another problem occurred.
				if (blockNum != currentBlockNumber)
				{
					if(currentBlockNumber > blockNum)
					{
						//received duplicate data packet
						duplicateDataPacket = true;
					}
					else {
						// Send ErrorPacket with error code 04 and stop transfer.
						ErrorPacket ep = new ErrorPacket((byte)4, "ACK packet block number not in sequence or duplicate.");
						sendReceiveSocket.send(new DatagramPacket(ep.encode(), ep.encode().length, InetAddress.getLocalHost(), receivePacket.getPort()));
						return;
					}
				}
			}
			
			if (bytesRead < 512) break;
			//get ready to send the next block of bytes
			currentBlockNumber++;
		}
		in.close();
		System.out.println("Transfer complete");
	}
	
	public void readFromClient(String filename) throws IOException{
		System.out.println("Reading from client: " + filename);
	
		// Already received request
		// Send ACK with blockNumber 0 ... N;
		// Receive dataBlock (blockNumber++)
		byte[] receivedData;
		byte[] opcode;
		int currentBlockNumber = 0;
		sendReceiveSocket = new DatagramSocket();
		if(new File("ServerFiles/" + filename).exists()){
			String errorString = filename + " already exists on Server.";
			ErrorPacket ep = new ErrorPacket((byte) 6, errorString);
			System.err.println(errorString);
			
			// Send errorPacket
			sendPacket = new DatagramPacket(ep.encode(), ep.encode().length, receivePacket.getAddress(), receivePacket.getPort());
			if(!packetSendWithTimeout(sendReceiveSocket, sendPacket))
			{
				return;
			}
				sendReceiveSocket.send(sendPacket);
			TFTPInfoPrinter.printSent(sendPacket);
			return;
		}
		
		BufferedOutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream("ServerFiles/" + filename));
		} catch (IOException e) {
			if (e.getMessage().contains("(Access is denied)")){
				System.err.println("Access to ServerFiles folder was denied");
				return;
			}
			else {
				System.err.println("Unknown file error");
			}
		}
		
		while (true) {
			// Send ack back
			AckPacket ap = new AckPacket(currentBlockNumber);
			byte[] ack = ap.encode();
			
			// Initial request was sent to wellKnownPort, but steady state file transfer should happen on another port.
			sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), receivePacket.getPort());
			if(!packetSendWithTimeout(sendReceiveSocket, sendPacket))
			{
				out.close();
				return;
			}
			TFTPInfoPrinter.printSent(sendPacket);
			currentBlockNumber++;
			
			receivedData = new byte[bufferSize];
			receivePacket = new DatagramPacket(receivedData, receivedData.length);
			// receive block
			if(!packetReceiveWithTimeout(sendReceiveSocket, receivePacket, sendPacket))
			{
				out.close();
				return;
			}
			TFTPInfoPrinter.printReceived(receivePacket);
			
			// validate packet
			receivedData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());

			
			opcode = Arrays.copyOf(receivedData, 2);

			if (Arrays.equals(opcode, ErrorPacket.opcode)){
				ErrorPacket ep = new ErrorPacket(receivedData);
				System.err.println(ep.getErrorMessage());
				
				out.close();
				return;
			}
			// The received packet should be an DATA packet at this point, and this have the Opcode defined in ackOP.
			// If it is not an error packet or an DATA packet, something happened (these cases are in later iterations).
			else if (!Arrays.equals(opcode, DataPacket.opcode)) {
				// Send ErrorPacket with error code 04 and stop transfer.
				ErrorPacket ep = new ErrorPacket((byte)4, "Was expecting DATA packet.");
				sendReceiveSocket.send(new DatagramPacket(ep.encode(), ep.encode().length, InetAddress.getLocalHost(), receivePacket.getPort()));
				return;
			}
			
			if (!DataPacket.isValid(receivedData)) {
				// Send ErrorPacket with error code 04 and stop transfer.
				ErrorPacket ep = new ErrorPacket((byte)4, "DATA packet was malformed.");
				sendReceiveSocket.send(new DatagramPacket(ep.encode(), ep.encode().length, InetAddress.getLocalHost(), receivePacket.getPort()));
				return;
			}
			DataPacket dp = new DataPacket(receivedData);
			int blockNum = dp.getBlockNum();

			boolean duplicateDataReceived = false;
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
						duplicateDataReceived = true;
						currentBlockNumber += 65536; // Restore block number since packet was a duplicate
					}
					else {
						// Send ErrorPacket with error code 04 and stop transfer.
						ErrorPacket ep = new ErrorPacket((byte)4, "DATA packet block number not in sequence or duplicate.");
						sendReceiveSocket.send(new DatagramPacket(ep.encode(), ep.encode().length, InetAddress.getLocalHost(), receivePacket.getPort()));
						return;
					}
				}
			}
			
			byte[] dataBlock = dp.getDataBlock();
			
			// Write dataBlock to file
			if(!duplicateDataReceived)
				{
				try {
					out.write(dataBlock);
				}
				catch(IOException e)
				{
					String errorString;
					ErrorPacket ep;
					if(e.getMessage().contains("(Access is denied)"))
					{ // Hacky solution to get determine if invalid file permissions.
						errorString = "Server could not write " + '"' + filename + '"' + ".";
						ep = new ErrorPacket((byte) 2, errorString);
					}
					else{
						errorString = "Server disk full, unable to write.";
						ep = new ErrorPacket((byte) 3, errorString);
					}
						
					System.err.println(errorString);
					
					// Send errorPacket
					sendPacket = new DatagramPacket(ep.encode(), ep.encode().length, receivePacket.getAddress(), receivePacket.getPort());
					if(!packetSendWithTimeout(sendReceiveSocket, sendPacket))
					{
						out.close();
						return;
					}
					TFTPInfoPrinter.printSent(sendPacket);
					try {
						out.close();
					} catch (IOException e2) {
						
					}
					return;
				}
			}
			
			
			
			// check if block is < 512 bytes which signifies end of file
			if (dataBlock.length < 512) {
				break; 
			}
		}
		
		//send the last ACK packet
		AckPacket ap = new AckPacket(currentBlockNumber);
		byte[] ack = ap.encode();
		
		// Initial request was sent to wellKnownPort, but steady state file transfer should happen on another port.
		sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), receivePacket.getPort());
		if(!packetSendWithTimeout(sendReceiveSocket, sendPacket))
		{
			out.close();
			return;
		}
		TFTPInfoPrinter.printSent(sendPacket);
		
		out.close();
		sendReceiveSocket.close();
		System.out.println("Transfer complete");
	}
	
}
