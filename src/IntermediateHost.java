import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
		while(true)
		{
			byte data[] = new byte[1000];
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
			
			byte[] data2 = new byte[receivePacket.getData().length];
			System.arraycopy(data, receivePacket.getOffset(), data2, 0, receivePacket.getData().length);
			
			TFTPInfoPrinter.printReceived(receivePacket);
			
			InetAddress clientAddress = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			
			try {
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getLocalHost(), 69);
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
			
			System.out.println("Intermediate Host: packet sent");
			
			System.out.println("Intermediate Host: Waiting for packet");
			try{
				sendAndReceiveSocket.receive(receivePacket);
			}catch(IOException e)
			{
				System.out.print("IO Exception, likely receive socket timeout");
				e.printStackTrace();
				System.exit(1);
			}
			
			TFTPInfoPrinter.printReceived(receivePacket);
			
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
		
	}
	public static void main(String args[])
	{
		IntermediateHost host = new IntermediateHost();
		host.receiveAndSend();
	}

}
