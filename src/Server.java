import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

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
		while(true){} //keep the process open to allow existing transfers to finish
	}
	/*
	 * void receiveAndSend
	 */
	public void receiveAndSend()
	{
		byte data[] = new byte[100];
		byte data2[];
		int threadCounter = 1;
		ServerInput waitForExitCommand = new ServerInput("Input Handler", this);
		waitForExitCommand.start();
		//once user input is added, the server operator can choose to shutdown
		System.out.println("Server: Waiting for packet..");
		while(run)
		{
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
	}
	
}
