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
	private int corruptSeg = 0;
	private byte[] code = new byte[2];
	byte[] data2;
	private int delay = 0;
	private boolean isTarget = false;
	private boolean isDrop = false;
	byte[] newBlock = {0,0};
	String modeStr = "";
	String filename = "";
	
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
	private void reset() {
		mode = 0;
		corruptSeg = 0;
		code = new byte[2];
		data2 = null;
		delay = 0;
		isTarget = false;
		isDrop = false;
		//newBlock = {0,0};
		modeStr = "";
		filename = "";
	}
	
	public void receiveAndSend() throws InterruptedException, IOException
	{
		int clientPort, serverPort = 69;
		HostInput errorModeCommand = new HostInput("Host Input Handler", this);
		errorModeCommand.start();
		
		byte[] data = new byte[516];
		byte[] tmpData;
		
		
		// IH logic
		// REPEAT:
		// 	Receive from client
		// 	Check if it is the target packet
		//   if so do thing
		//  Send to server
		//  Receive from server
		//  Check if it is target packet
		//   if so do thing
		//  Send to client
		
		
		while (true) {
			// Receive from client
			receivePacket = new DatagramPacket(data, data.length);
			receiveSocket.receive(receivePacket);
			TFTPInfoPrinter.printReceived(receivePacket);
			
			tmpData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
			
			// Check if it is the target packet
			if (isTargetPacket(tmpData)) {
				sendSpecially(tmpData, InetAddress.getLocalHost(), serverPort);
			}
			// Send the packet to the server normally
			else {
				sendPacket = new DatagramPacket(tmpData, tmpData.length, InetAddress.getLocalHost(), serverPort);
				sendAndReceiveSocket.send(sendPacket);
				TFTPInfoPrinter.printSent(sendPacket);
			}
			
			InetAddress clientAddress = receivePacket.getAddress();
			clientPort = receivePacket.getPort();
			
			// Receive from server
			receivePacket = new DatagramPacket(data, data.length);
			sendAndReceiveSocket.receive(receivePacket);
			TFTPInfoPrinter.printReceived(receivePacket);
			
			serverPort = receivePacket.getPort();
			
			tmpData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
			
			// Check if it is the target packet
			if (isTargetPacket(tmpData)) {
				sendSpecially(tmpData, InetAddress.getLocalHost(), clientPort);
			}
			// Send packet to client normally
			else {
				sendPacket = new DatagramPacket(tmpData, tmpData.length, clientAddress, clientPort);
				receiveSocket.send(sendPacket);
				TFTPInfoPrinter.printSent(sendPacket);
			}
		}
		
		
		/*
		
		// End send to Client
		*/
	}
	
	private void sendSpecially(byte[] data, DatagramPacket sendPacket) {
		if(mode == 1){
			mode = 0;
			System.out.println("Packet Dropped");
		}
		else if (mode == 2) {
			try {
				TimeUnit.MILLISECONDS.sleep(delay);
				sendAndReceiveSocket.send(sendPacket);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}			
		}
		
		else if (mode == 3) {
			try {
				sendAndReceiveSocket.send(sendPacket);
				TimeUnit.MILLISECONDS.sleep(delay);
				sendAndReceiveSocket.send(sendPacket);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		else if (mode == 4) {
			if(corruptSeg == 1){
				//change opcode
				data2[0] = newBlock[0];
				data2[1] = newBlock[1];
			}
			else if(corruptSeg == 2){
				//change block number
				data2[2] = newBlock[0];
				data2[3] = newBlock[1];
			}
			else if(corruptSeg == 3){
				//change end 0 byte to a value
				data2[data2.length - 1] = 1;
			}
			else if(corruptSeg == 4){
				//change mode
				byte[] reqType = {data2[0], data2[1]};
				filename = "";
				for(int i = 2; data2[i] != 0; i++){
					filename += data2[i];
				}
				RequestPacket p = new RequestPacket(reqType, filename, modeStr);
				data2 = p.encode();
			}
			sendPacket.setData(data2);
			sendPacket.setLength(receivePacket.getLength());
		}
		else if (mode == 5) {
			try {
				InetAddress addr = InetAddress.getByName("192.168.1.1");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
		} catch (IOException e) {
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
		for(int i = 0; i < code.length; i++){
			if(data[i] != code[i]){
				return false;
			}
		}
		return true;
	}
}
