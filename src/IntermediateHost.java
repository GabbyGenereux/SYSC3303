import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class IntermediateHost {
	
	DatagramSocket receiveSocket, sendAndReceiveSocket;
	DatagramPacket receivePacket, sendPacket;
	
	public IntermediateHost()
	{
		try{
			receiveSocket = new DatagramSocket(23);
			sendAndReceiveSocket = new DatagramSocket();
		}catch(SocketException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void receiveAndSend()
	{
		int clientPort, serverPort = 69;
		while(true)
		{
			byte data[] = new byte[516];
			
			// Receive from client
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("Intermediate Host: waiting for packet..");
			
			try{
				System.out.println("Waiting..");
				receiveSocket.receive(receivePacket);
				
			}catch(IOException e)
			{
				System.out.print("IO Exception, likely receive socket timeout");
				e.printStackTrace();
				System.exit(1);
			}
			// End receive from client
			
			// Send to Server
			byte[] data2 = Arrays.copyOf(receivePacket.getData(),  receivePacket.getLength());
			
			TFTPInfoPrinter.printReceived(receivePacket);
			
			InetAddress clientAddress = receivePacket.getAddress();
			clientPort = receivePacket.getPort();
			
			try {
				sendPacket = new DatagramPacket(data2, data2.length, InetAddress.getLocalHost(), serverPort);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			try{
				sendAndReceiveSocket.send(sendPacket);
			}catch(IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			TFTPInfoPrinter.printSent(sendPacket);	
			
			// End send to server
			
			// Receive from Server
			System.out.println("Intermediate Host: Waiting for packet");
			try{
				sendAndReceiveSocket.receive(receivePacket);
			}catch(IOException e)
			{
				System.out.print("IO Exception, likely receive socket timeout");
				e.printStackTrace();
				System.exit(1);
			}
			serverPort = receivePacket.getPort();
			
			TFTPInfoPrinter.printReceived(receivePacket);
			// End receive from Server
			
			// Send to Client
			sendPacket.setData(receivePacket.getData());
			sendPacket.setLength(receivePacket.getLength());
			sendPacket.setAddress(clientAddress);
			sendPacket.setPort(clientPort);
			try{
				receiveSocket.send(sendPacket);
			}catch(IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Intermediate Host: Packet sent");
		}
		// End send to Client
		
	}
	
	public static void main(String args[])
	{
		System.out.println("Choose whether you would like to run in quiet or verbose mode (q/v):");
		Scanner s = new Scanner(System.in);
		String response = s.nextLine();
		
		if (response.equals("q")) {
			TFTPInfoPrinter.setVerboseMode(false);
		}
		else if (response.equals("n")) {
			TFTPInfoPrinter.setVerboseMode(true);
		}
		IntermediateHost host = new IntermediateHost();
	
		host.receiveAndSend();
	}
}
