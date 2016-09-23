import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class ServerThread extends Thread{
	DatagramPacket receivePacket, sendPacket;
	DatagramSocket sendSocket;
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
			sendData[0] = zeroByte;
			sendData[1] = threeByte;
			sendData[2] = zeroByte;
			sendData[3] = oneByte;
		}
		else if(data[1] == twoByte)
		{
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
}
