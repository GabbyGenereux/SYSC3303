import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;


public class ServerThread extends Thread{
	DatagramPacket receivePacket, sendPacket;
	DatagramSocket sendSocket, sendReceiveSocket;
	byte[] data;
	
	public ServerThread(String name, DatagramPacket receivedPacket, byte[] receivedData){
		super(name);
		receivePacket = receivedPacket;
		data = receivedData;
	}
	
	public void run(){
		byte sendData[] = new byte[4];
		byte zeroByte = 0;
		byte oneByte = 1;
		byte twoByte = 2;
		byte threeByte = 3;
		byte fourByte = 4;
		
		//Check if packet is valid
		int i;
		for(i= 0; i < data.length; i++)
		{
			if(i == 0)
			{
				if(data[i] != zeroByte)
				{
					System.out.println("invalid packet, quitting..");
					System.exit(1);
				}
				
			}
			else if(i == 1)
			{
				if(data[i] != oneByte && data[i] != twoByte)
				{
					System.out.println("invalid packet, quitting..");
					System.exit(1);
				}
			}
			else if(i == data.length-1)
			{
				if(data[i] != zeroByte)
				{
					System.out.println("invalid packet, quitting..");
					System.exit(1);
				}
			}
			else
			{
				if(data[i] == zeroByte)
				{
					if(data[i-1] == zeroByte && data[i+1] == zeroByte)
					{
						System.out.println("invalid packet, quitting..");
						System.exit(1);
					}
				}
			}
		}
		//Create new message depending on whether the packet was a read or write request
		if(data[1] == oneByte)
		{
			//read request
			sendData[0] = zeroByte;
			sendData[1] = threeByte;
			sendData[2] = zeroByte;
			sendData[3] = oneByte;
		}
		else if(data[1] == twoByte)
		{
			//write request
			sendData[0] = zeroByte;
			sendData[1] = fourByte;
			sendData[2] = zeroByte;
			sendData[3] = zeroByte;
		}
		
		//Create new send packet
		sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
		//Print information being sent out
		String name = "Server " + this;
		System.out.println(name + ": Sending packet");
		System.out.println("To host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing: ");
		System.out.println("String: " + new String(sendPacket.getData(), 0, len));
		for(int k = 0; k < sendPacket.getData().length; k++)
		{
			System.out.print(sendPacket.getData()[k] + " ");
		}
		System.out.println();
		//System.out.println("\n");			
		//Create socket to send out on
		try {
			sendSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
		//temporary slowdown for testing purposes
		/*try{
			Thread.sleep(1000);
		}catch(InterruptedException e){}*/
		
		//Send packet on newly created socket
		try{
			sendSocket.send(sendPacket);
		}catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println(name + ": packet sent");
		
		sendSocket.close();
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
		
		int blockNumber = 1; //starting with the first block of 512 bytes
		byte[] data;
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
		while(true)
		{
			data = new byte[516]; //2 bytes for opcode, 2 bytes for block number, 512 bytes for data
			byte[] block = convertBlockNumberByteArr(blockNumber); //convert the integer block number to big endian word
			byte[] dataBlock = new byte[512];
			data[0] = 0;
			data[1] = 4; //ACK opcode
			data[2] = block[0];
			data[3] = block[1]; //block number

			int bytesRead = in.read(dataBlock);
			System.arraycopy(dataBlock, 0, data, 4, bytesRead);
			
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
			
			if (bytesRead == -1) break;
			//get ready to send the next block of bytes
			blockNumber++;
		}
		in.close();
	}
	
	//write the bytes from a client to a file on the server, given filename (for write requests)
	//should be called after the initial request has been read
	void readFromClient(String filename) throws IOException
	{
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
		//Create socket to send out ACKs and receive packets
		try 
		{
			sendReceiveSocket = new DatagramSocket(receivePacket.getPort());
		} 
		catch (SocketException e1) 
		{
			e1.printStackTrace();
			System.exit(1);
		}
		
		int blockNumber = 0;
		byte[] receivedData;
		DatagramPacket transferFromClient;
		byte[] block;
		byte[] ack = new byte[4];
		
		ack[0] = 0;
		ack[1] = 4; //ACK opcode
		while(true)
		{
			receivedData = new byte[516]; //2 bytes for opcode, 2 bytes for block number and 512 bytes for data
			transferFromClient = new DatagramPacket(receivedData, receivedData.length);

			block = convertBlockNumberByteArr(blockNumber); //convert the integer block number to big endian word
			
			data[2] = block[0];
			data[3] = block[1]; //block number
			
			sendPacket = new DatagramPacket(data, data.length, receivePacket.getAddress(), receivePacket.getPort());
			
			//send the ACK to the client
			try
			{
				sendReceiveSocket.send(sendPacket);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			
			//wait to receive the packet of data
			try
			{
				sendReceiveSocket.receive(transferFromClient);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			
			//received the packet full of data, copy just the data (index 4-515)
			byte[] data = Arrays.copyOfRange(receivedData, 4, receivedData.length);
			
			out.write(data);
			
			//check if the data received was less than 512 bytes
			//if so, the last packet has been received and the thread should not loop back to receive more packets
			if(data.length < 512)
				break;
			
			blockNumber++; //increment the block number and loop back to send the ack, then receive another packet
		}
		//send the ACK for the last packet
		block = convertBlockNumberByteArr(blockNumber); //convert the integer block number to big endian word
		
		data[2] = block[0];
		data[3] = block[1]; //block number
		sendPacket = new DatagramPacket(data, data.length, receivePacket.getAddress(), receivePacket.getPort());
		
		//send the ACK to the client
		try
		{
			sendReceiveSocket.send(sendPacket);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		
		sendReceiveSocket.close();
	}
}
