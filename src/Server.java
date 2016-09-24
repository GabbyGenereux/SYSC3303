import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {

	DatagramSocket sendSocket, receiveSocket;
	DatagramPacket receivePacket, sendPacket;
	boolean run = true;
	/*
	 * Constructor
	 * Creates new receive socket
	 */
	public Server()
	{
		try{
			receiveSocket = new DatagramSocket(69);
		}catch(SocketException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	//stop the server process
	private void stop()
	{
		receiveSocket.close();
	}
	/*
	 * void receiveAndSend
	 */
	public void receiveAndSend()
	{
		byte data[] = new byte[100];
		byte data2[];
		int threadCounter = 1;
		
		//once user input is added, the server operator can choose to shutdown
		while(run)
		{
			//create packet to receive
			receivePacket = new DatagramPacket(data, data.length);
			System.out.println("Server: Waiting for packet..");
			
			//receive packet
			try{
				System.out.println("Waiting...");
				receiveSocket.receive(receivePacket);
			}catch(IOException e)
			{
				System.out.println("IO Exception: Likely receive socket timeout");
				e.printStackTrace();
				System.exit(1);
			}
			
			data2 = new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data2, 0, receivePacket.getLength());
			//Print information from received packet
			System.out.println("Server: Packet received.");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			int len = receivePacket.getLength();
			System.out.println("Length: "+ len);
			System.out.print("Containing: ");
			System.out.println(new String(data2, 0, len));
			for(int k = 0; k < len; k++)
			{
				System.out.print(receivePacket.getData()[k] + " ");
			}
			System.out.println("\n");
			
			//create and run new Server thread
			Thread serverThread = new ServerThread("Server Thread #" + threadCounter,receivePacket,data2);
			threadCounter++;
			System.out.println("Server: Created " + serverThread);
			serverThread.start();
		}
		stop();
	}
	public static void main(String args[])
	{
		Server s = new Server();
		s.receiveAndSend();
	}
	
}
