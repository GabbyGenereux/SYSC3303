import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Server {

	private static final int KNOWN_PORT = 69;
	private static InetAddress KNOWN_ADDRESS = null;
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
			receiveSocket = new DatagramSocket(KNOWN_PORT);
		}catch(SocketException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		try {
			receiveSocket.setSoTimeout(1000);
		} catch (SocketException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
	}
	//stop the server process
	public void stop()
	{
		run = false;
	}
	
	private void shutdown()
	{
		receiveSocket.close();
		System.out.println("Socket closed, server will no longer accept requests");
	}
	/*
	 * void receiveAndSend
	 */
	public void receiveAndSend()
	{
		byte data[] = new byte[100];
		byte data2[];
		int threadCounter = 1;
		int loopCounter = 0;
		ServerInput waitForExitCommand = new ServerInput("Input Handler", this);
		waitForExitCommand.start();
		//once user input is added, the server operator can choose to shutdown
		System.out.println("Server: Waiting for packet..");
		while(run)
		{
			loopCounter++;
			//create packet to receive
			receivePacket = new DatagramPacket(data, data.length);
			
			//receive packet
			try
			{
				receiveSocket.receive(receivePacket);
			}
			catch(SocketTimeoutException e)
			{
				continue;
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				System.exit(1);
			}
			
			if(loopCounter == 1)
			{
				KNOWN_ADDRESS = receivePacket.getAddress();
			}
			else
			{
				if(receivePacket.getPort() != KNOWN_PORT || receivePacket.getAddress() != KNOWN_ADDRESS)
				{
					System.err.println("Unknown port or address, discarding.");
					continue;
				}
			}
			
			data2 = new byte[receivePacket.getLength()];
			System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data2, 0, receivePacket.getLength());
			//Print information from received packet
			TFTPInfoPrinter.printReceived(receivePacket);
			System.out.println("\n");

			
			//create and run new Server thread
			Thread serverThread = new ServerThread("Server Thread #" + threadCounter,receivePacket,data2);
			threadCounter++;
			System.out.println("Server: Created " + serverThread);
			serverThread.start();
		}
		shutdown();
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
		Server server = new Server();
		server.receiveAndSend();
		s.close();
	}
	
}
