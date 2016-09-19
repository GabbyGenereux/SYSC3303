import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class IntermediateHost {
	
	DatagramSocket recieveSocket, sendAndRecieveSocket;
	DatagramPacket recievePacket, sendPacket;
	
	public IntermediateHost()
	{
		try{
			recieveSocket = new DatagramSocket(23);
			sendAndRecieveSocket = new DatagramSocket();
		}catch(SocketException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void recieveAndSend()
	{
		while(true)
		{
			byte data[] = new byte[100];
			recievePacket = new DatagramPacket(data, data.length);
			System.out.println("Intermediate Host: waiting for packet..");
			
			try{
				System.out.println("Waiting..");
				recieveSocket.receive(recievePacket);
			}catch(IOException e)
			{
				System.out.print("IO Exception, likely recieve socket timeout");
				e.printStackTrace();
				System.exit(1);
			}
			
			byte[] data2 = new byte[recievePacket.getData().length];
			System.arraycopy(data, recievePacket.getOffset(), data2, 0, recievePacket.getData().length);
			
			System.out.println("Intermediate Host: Packet recieved.");
			System.out.println("From host: " + recievePacket.getAddress());
			System.out.println("Host port: " + recievePacket.getPort());
			int len = recievePacket.getLength();
			System.out.println("Length: "+ len);
			System.out.print("Containing: ");
			String recieved = new String(data2, 0, len);
			System.out.println(recieved);
			for(int k = 0; k < len; k++)
			{
				System.out.print(recievePacket.getData()[k] + " ");
			}
			System.out.println("\n");
			
			InetAddress clientAddress = recievePacket.getAddress();
			int clientPort = recievePacket.getPort();
			
			try {
				sendPacket = new DatagramPacket(recievePacket.getData(), recievePacket.getLength(), InetAddress.getLocalHost(), 69);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Intermediate Host: Sending packet");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			len = sendPacket.getLength();
			System.out.println("Length: " + len);
			System.out.print("Containing: ");
			System.out.println("String: " + new String(sendPacket.getData(), 0, len));
			for(int k = 0; k < len; k++)
			{
				System.out.print(sendPacket.getData()[k] + " ");
			}
			System.out.println("\n");			
			try{
				sendAndRecieveSocket.send(sendPacket);
			}catch(IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Intermediate Host: packet sent");
			
			System.out.println("Intermediate Host: Waiting for packet");
			try{
				sendAndRecieveSocket.receive(recievePacket);
			}catch(IOException e)
			{
				System.out.print("IO Exception, likely recieve socket timeout");
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Intermediate Host: Packet recieved.");
			System.out.println("From host: " + recievePacket.getAddress());
			System.out.println("Host port: " + recievePacket.getPort());
			len = recievePacket.getLength();
			System.out.println("Length: "+ len);
			System.out.print("Containing: ");
			
			recieved = new String(data2, 0, len);
			System.out.println(recieved);
			for(int k = 0; k < len; k++)
			{
				System.out.print(recievePacket.getData()[k] + " ");
			}
			System.out.println("\n");
			
			sendPacket.setData(recievePacket.getData());
			sendPacket.setLength(recievePacket.getLength());
			sendPacket.setAddress(clientAddress);
			sendPacket.setPort(clientPort);
			try{
				recieveSocket.send(sendPacket);
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
		host.recieveAndSend();
	}

}
