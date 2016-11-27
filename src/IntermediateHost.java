import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class IntermediateHost {
	
	private DatagramSocket receiveSocket, sendAndReceiveSocket, sendAndReceiveSocketAlt;
	private int mode = 0;
	private int corruptSeg = 0;
	private byte[] code = new byte[2];
	private int delay = 0;
	private byte[] newBlock = {0,0};
	
	private final int serverWellKnownPort = 69;
	private final int hostWellKnownPort = 23;
	
	public IntermediateHost()
	{
		try{
			receiveSocket = new DatagramSocket(23);
			sendAndReceiveSocket = new DatagramSocket();
			sendAndReceiveSocket.setSoTimeout(5000);
			sendAndReceiveSocketAlt = new DatagramSocket(50);
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
		
		
		DatagramPacket p = new DatagramPacket(data, data.length);
		receiveSocket.receive(p);
		
		clientPort = p.getPort();
		TFTPInfoPrinter.printReceived(p);
		
		while (true) {
			InetAddress addr = InetAddress.getLocalHost();
			int port = -1;
			if (mode == -1) break;
			// from client
			if (p.getPort() == clientPort) {
				port = serverPort;
			}
			// from server
			else if (p.getPort() == serverPort) {
				port = clientPort;
			}
			if (mode != 0 && isTargetPacket(p.getData())) {
				sendSpecially(p.getData(), p.getLength(), addr, port);
			}

			else {
				DatagramPacket sendPacket = new DatagramPacket(p.getData(), p.getLength(), addr, port);
				sendAndReceiveSocket.send(sendPacket);
				TFTPInfoPrinter.printSent(sendPacket);
			}
			
			p = new DatagramPacket(data, data.length);
			while (mode != -1) {
				try {
					sendAndReceiveSocket.receive(p);
					break;
				} catch (SocketTimeoutException e) {
					
				}
			}
			// Get out of loop as trasnfer has been marked as finished.
			if (mode == -1) {
				break;
			}
			
			
			// On the first transfer, the server port will still be the well known port.
			// If this is the cause, change it to the port for steady state transfer.
			if (serverPort == serverWellKnownPort) serverPort = p.getPort();
			
			
			TFTPInfoPrinter.printReceived(p);
		}
	}
	public void resetForNextTransfer() {
		mode = 0;
	}
	
	private void sendSpecially(byte[] data, int length, InetAddress addr, int port) {
		DatagramPacket sendPacket = new DatagramPacket(data, length, addr, port);

		// Drop packet
		if (mode == 1) {
			System.out.println("Dropping packet...");
			byte[] opcode = {data[0], data[1]};
			// If a request is dropped, need to reset.
			if (Arrays.equals(opcode, RequestPacket.readOpcode) || Arrays.equals(opcode, RequestPacket.writeOpcode)){
				mode = -1;
			}
			// Do nothing (packet dropped)
		}
		// Delay packet
		else if (mode == 2) {
			System.out.println("Delaying packet...");
			try {

				TimeUnit.MILLISECONDS.sleep(delay);
				sendAndReceiveSocket.send(sendPacket);
			} catch (InterruptedException ie) {
				
			} catch (IOException ioe) {
				
			}
			
		}
		// Duplicate packet
		else if (mode == 3) {
			System.out.println("Duplicating packet");
			try {
				sendAndReceiveSocket.send(sendPacket);
				TimeUnit.MILLISECONDS.sleep(delay);
				sendAndReceiveSocket.send(sendPacket);
			} catch (InterruptedException ie) {
				
			} catch (IOException ioe) {

			}
			

		}
		// Corrupt the packet
		else if (mode == 4) {
			System.out.println("Corrupting packet");
			System.out.println("CorruptSeg=" + corruptSeg);
			
			//corrupt packet
			if(corruptSeg == 1){
				//change opcode
				data[0] = newBlock[0];
				data[1] = newBlock[1];
			}
			else if(corruptSeg == 2){
				//change block number
				data[2] = newBlock[0];
				data[3] = newBlock[1];
			}
			else if(corruptSeg == 3){
				//change end 0 byte to a value
				data[length - 1] = 1;
			}
			
			try {
				// May or may not be necessary.
				sendPacket.setData(data);
				sendPacket.setLength(length);
				sendAndReceiveSocket.send(sendPacket);
			} catch (IOException ioe) {
				
			}
		}
		// send from different socket
		else if (mode == 5) {
			System.out.println("Sending from a different port: " + sendAndReceiveSocketAlt.getLocalPort());
			try {
				sendAndReceiveSocketAlt.send(sendPacket);
			} catch (IOException e) {
				
			}
		}
		TFTPInfoPrinter.printSent(sendPacket);
		mode = 0;
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
			while (true) {
				host.receiveAndSend();
				host.resetForNextTransfer();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setMode(int m, byte[] c, byte[] nc, int d){
		
		if(m < 4){
			mode = m;
		}
		else if(m == 7){
			mode = 5;
		}
		else{
			mode = 4;
			corruptSeg = m-3;
		}
		code = c;
		newBlock = nc;
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
