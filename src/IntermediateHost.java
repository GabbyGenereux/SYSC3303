import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class IntermediateHost {
	
	DatagramSocket receiveSocket, sendAndReceiveSocket;
	DatagramPacket receivePacket, sendPacket;
	private int mode = 0;
	private byte[] code = new byte[2];
	private int delay = 0;
	private boolean isTarget = false;
	
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
	
	public void receiveAndSend() throws InterruptedException
	{
		int clientPort, serverPort = 69;
		HostInput errorModeCommand = new HostInput("Host Input Handler", this);
		errorModeCommand.start();
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
			isTarget = checkData(data2);
			
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
				if(isTarget && mode == 2){
					TimeUnit.MILLISECONDS.sleep(delay);
					sendAndReceiveSocket.send(sendPacket);
				}
				else if(isTarget && mode == 3){
					sendAndReceiveSocket.send(sendPacket);
					TimeUnit.MILLISECONDS.sleep(delay);
					sendAndReceiveSocket.send(sendPacket);
				}
				else if(!(isTarget && mode == 1)){
					sendAndReceiveSocket.send(sendPacket);
				}
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
			isTarget = checkData(receivePacket.getData());
			
			TFTPInfoPrinter.printReceived(receivePacket);
			// End receive from Server
			
			// Send to Client
			sendPacket.setData(receivePacket.getData());
			sendPacket.setLength(receivePacket.getLength());
			sendPacket.setAddress(clientAddress);
			sendPacket.setPort(clientPort);
			try{
				if(isTarget && mode == 2){
					TimeUnit.MILLISECONDS.sleep(delay);
					receiveSocket.send(sendPacket);
				}
				else if(isTarget && mode == 3){
					receiveSocket.send(sendPacket);
					TimeUnit.MILLISECONDS.sleep(delay);
					receiveSocket.send(sendPacket);
				}
				else if(!(isTarget && mode == 1)){
					receiveSocket.send(sendPacket);
				}
			}catch(IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Intermediate Host: Packet sent");
		}
		// End send to Client
		
	}
	
	public static void main(String args[]) throws InterruptedException
	{
		System.out.println("Choose whether you would like to run in quiet or verbose mode (q/v):");
		Scanner s = new Scanner(System.in);
		String response = s.nextLine();
		//s.close(); //can't close without interfering with the HostInput
		if (response.equals("q")) {
			TFTPInfoPrinter.setVerboseMode(false);
		}
		else if (response.equals("n")) {
			TFTPInfoPrinter.setVerboseMode(true);
		}
		IntermediateHost host = new IntermediateHost();
		//try{
		//	Thread.sleep(100);
		//}catch(InterruptedException e){}
		host.receiveAndSend();
	}
	
	public void setMode(int m, byte[] c, int d){
		mode = m;
		code = c;
		delay = d;
		System.out.print("Mode set to " + m + " for packet [ ");
		for(int i = 0; i < c.length; i++){
			System.out.print(c[i] + " ");
		}
		System.out.println("]" + "with delay of " + d);
	}
	
	public boolean checkData(byte[] data){
		boolean state = true;
		for(int i = 0; i < code.length; i++){
			if(state == true && data[i] != code[i]){
				state = false;
			}
		}
		return state;
	}
}
