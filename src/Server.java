import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {

	DatagramSocket sendSocket, recieveSocket;
	DatagramPacket recievePacket, sendPacket;
	
	/*
	 * Constructor
	 * Creates new receive socket
	 */
	public Server()
	{
		try{
			recieveSocket = new DatagramSocket(69);
		}catch(SocketException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	/*
	 * void recieveAndSend
	 */
	public void recieveAndSend()
	{
		byte data[] = new byte[100];
		byte data2[];
		byte sendData[] = new byte[4];
		byte zeroByte = 0;
		byte oneByte = 1;
		byte twoByte = 2;
		byte threeByte = 3;
		byte fourByte = 4;
		
		while(true)
		{
			//create packet to receive
			recievePacket = new DatagramPacket(data, data.length);
			System.out.println("Server: Waiting for packet..");
			
			//receive packet
			try{
				System.out.println("Waiting...");
				recieveSocket.receive(recievePacket);
			}catch(IOException e)
			{
				System.out.println("IO Exception: Likely recieve socket timeout");
				e.printStackTrace();
				System.exit(1);
			}
			
			data2 = new byte[recievePacket.getLength()];
			System.arraycopy(recievePacket.getData(), recievePacket.getOffset(), data2, 0, recievePacket.getLength());
			//Print information from received packet
			System.out.println("Server: Packet recieved.");
			System.out.println("From host: " + recievePacket.getAddress());
			System.out.println("Host port: " + recievePacket.getPort());
			int len = recievePacket.getLength();
			System.out.println("Length: "+ len);
			System.out.print("Containing: ");
			System.out.println(new String(data2, 0, len));
			for(int k = 0; k < len; k++)
			{
				System.out.print(recievePacket.getData()[k] + " ");
			}
			System.out.println("\n");
			
			//Check if packet is valid
			int i;
			for(i= 0; i < data2.length; i++)
			{
				if(i == 0)
				{
					if(data2[i] != zeroByte)
					{
						System.out.println("invalid packet, quitting..");
						System.exit(1);
					}
					
				}
				else if(i == 1)
				{
					if(data2[i] != oneByte && data2[i] != twoByte)
					{
						System.out.println("invalid packet, quitting..");
						System.exit(1);
					}
				}
				else if(i == data2.length-1)
				{
					if(data2[i] != zeroByte)
					{
						System.out.println("invalid packet, quitting..");
						System.exit(1);
					}
				}
				else
				{
					if(data2[i] == zeroByte)
					{
						if(data2[i-1] == zeroByte && data2[i+1] == zeroByte)
						{
							System.out.println("invalid packet, quitting..");
							System.exit(1);
						}
					}
				}
			}
			//Create new message depending on whether the packet was a read or write request
			if(data2[1] == oneByte)
			{
				sendData[0] = zeroByte;
				sendData[1] = threeByte;
				sendData[2] = zeroByte;
				sendData[3] = oneByte;
			}
			else if(data2[1] == twoByte)
			{
				sendData[0] = zeroByte;
				sendData[1] = fourByte;
				sendData[2] = zeroByte;
				sendData[3] = zeroByte;
			}
			
			//Create new send packet
			sendPacket = new DatagramPacket(sendData, sendData.length, recievePacket.getAddress(), recievePacket.getPort());
			//Print information being sent out
			System.out.println("Server: Sending packet");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			len = sendPacket.getLength();
			System.out.println("Length: " + len);
			System.out.print("Containing: ");
			System.out.println("String: " + new String(sendPacket.getData(), 0, len));
			for(int k = 0; k < sendPacket.getData().length; k++)
			{
				System.out.print(sendPacket.getData()[k] + " ");
			}
			//System.out.println("\n");			
			//Create socket to send out on
			try {
				sendSocket = new DatagramSocket();
			} catch (SocketException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
			
			//Send packet on newly created socket
			try{
				sendSocket.send(sendPacket);
			}catch(IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Server: packet sent");
			
			sendSocket.close();
		}
		
	}
	public static void main(String args[])
	{
		Server s = new Server();
		s.recieveAndSend();
	}
	
}
