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
	
	private DatagramSocket receiveSocket, sendAndReceiveSocket;
	private int mode = 0;
	private int corruptSeg = 0;
	private byte[] code = new byte[2];
	byte[] data2;
	private int delay = 0;
	private boolean isTarget = false;
	private boolean isDrop = false;
	byte[] newBlock = {0,0};
	String modeStr = "";
	String filename = "";
	
	private final int serverWellKnownPort = 69;
	private final int hostWellKnownPort = 23;
	
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
	
	public void receiveAndSend() throws Exception
	{
		int clientPort = -1, serverPort = 69;
		HostInput errorModeCommand = new HostInput("Host Input Handler", this);
		errorModeCommand.start();
		
		byte[] data = new byte[516];
		byte[] tmpData;
		
		// IH 2 logic
		// REPEAT:
		//  Receive
		//  Check if from client
		//   Check if target packet
		//    Send specially to server
		//   else
		//    send normally to server
		//  else ( from server )
		//   Check if target packet
		//    send specially to client
		//   else
		//    send normally to client
		
		DatagramPacket p = new DatagramPacket(data, data.length);
		receiveSocket.receive(p);
		
		clientPort = p.getPort();
		TFTPInfoPrinter.printReceived(p);
		
		while (true) {
			InetAddress addr = InetAddress.getLocalHost();
			int port = -1;
			
			// from client
			if (p.getPort() == clientPort) {
				port = serverPort;
			}
			// from server
			else if (p.getPort() == serverPort) {
				port = clientPort;
			}
			if (isTargetPacket(p.getData())) {
				
				sendSpecially(p.getData(), addr, port);
			}
			else {
				p.setPort(port);
				sendAndReceiveSocket.send(p);
			}
			TFTPInfoPrinter.printSent(p);
			p = new DatagramPacket(data, data.length);
			sendAndReceiveSocket.receive(p);
			
			// On the first transfer, the server port will still be the well known port.
			// If this is the cause, change it to the port for steady state transfer.
			if (serverPort == serverWellKnownPort) serverPort = p.getPort();
			
			TFTPInfoPrinter.printReceived(p);
		}
	}
	
	private void sendSpecially(byte[] data, InetAddress addr, int port) {
		DatagramPacket sendPacket = new DatagramPacket(data, data.length, addr, port);
		
		if (mode == 0) {
			
		}
		// Drop packet
		else if (mode == 1) {
			// Do nothing (packet dropped)
		}
		// Delay packet
		else if (mode == 2) {
			try {
				TimeUnit.MILLISECONDS.sleep(delay);
				sendAndReceiveSocket.send(sendPacket);
			} catch (InterruptedException ie) {
				
			} catch (IOException ioe) {
				
			}
			
		}
		// Duplicate packet
		else if (mode == 3) {
			
		}
		// Corrupt the packet
		else if (mode == 4) {
			
		}
		// ???
		else if (mode == 5) {
			
		}
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
		try {
			host.receiveAndSend();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setMode(int m, byte[] c, byte[] nc, int d){
		if(m < 4){
			mode = m;
		}
		else{
			mode = 4;
			corruptSeg = m-4;
		}
		code = c;
		if(mode == 7){
			modeStr = "";
			for(int k = 0; k < nc.length; k++)
			{
				modeStr += nc[k];
			}
		}
		else{
			newBlock = nc;
		}
		delay = d;
		System.out.print("Mode set to " + m + " for packet [ ");
		for(int i = 0; i < c.length; i++){
			System.out.print(c[i] + " ");
		}
		System.out.print("] with replacement [ ");
		for(int i = 0; i < nc.length; i++){
			System.out.print(nc[i] + " ");
		}
		System.out.println("] with delay of " + d);
	}
	
	public boolean isTargetPacket(byte[] data){
		boolean state = true;
		for(int i = 0; i < code.length; i++){
			if(state == true && data[i] != code[i]){
				state = false;
			}
		}
		return state;
	}
}
