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
	private byte[] defCode = new byte[2];
	byte[] data2;
	private int delay = 0;
	private boolean isTarget = false;
	private boolean isDrop = false;
	byte[] newBlock = {0,0};
	byte[] newOp = {0,0};
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
			
			do{
				try{
					System.out.println("Waiting for packet from client");
					receiveSocket.receive(receivePacket);
					
				}catch(IOException e)
				{
					System.out.print("IO Exception, likely receive socket timeout");
					e.printStackTrace();
					System.exit(1);
				}
				// End receive from client
				
				// Send to Server
				data2 = Arrays.copyOf(receivePacket.getData(),  receivePacket.getLength());
				isTarget = checkData(data2);
				if(isTarget && mode == 1){
					mode = 0;
					System.out.println("Packet Dropped");
				}
				else{
					isDrop = false;
				}
			}while(isDrop);
			
			TFTPInfoPrinter.printReceived(receivePacket);
			
			InetAddress clientAddress = receivePacket.getAddress();
			clientPort = receivePacket.getPort();
			
			if(isTarget && mode == 4){
				//corrupt packet
				if(corruptSeg == 1){
					//change opcode
					data2[0] = newOp[0];
					data2[1] = newOp[1];
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
			}
			InetAddress addr = null;
			try {
				addr = InetAddress.getLocalHost();
			} catch (UnknownHostException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			if(isTarget && mode == 5){
				try {
					addr = InetAddress.getByName("192.168.1.1");
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			sendPacket = new DatagramPacket(data2, data2.length, addr, serverPort);
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
			
			//return to normal operation
			if(isTarget){
				mode = 0;
			}
			// End send to server
			
			// Receive from Server
			do{
				try{
					System.out.println("Waiting for packet from server");
					sendAndReceiveSocket.receive(receivePacket);
				}catch(IOException e)
				{
					System.out.print("IO Exception, likely receive socket timeout");
					e.printStackTrace();
					System.exit(1);
				}
				serverPort = receivePacket.getPort();
				isTarget = checkData(receivePacket.getData());
				if(isTarget && mode == 1){
					mode = 0;
					System.out.println("Packet Dropped");
				}
				else{
					isDrop = false;
				}
			}while(isDrop);
			
			TFTPInfoPrinter.printReceived(receivePacket);
			// End receive from Server
			
			data2 = receivePacket.getData();
			if(isTarget && mode == 4){
				//corrupt packet
				if(corruptSeg == 1){
					//change opcode
					data2[0] = newOp[0];
					data2[1] = newOp[1];
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
			}
			
			// Send to Client
			sendPacket.setData(data2);
			sendPacket.setLength(receivePacket.getLength());
			sendPacket.setPort(clientPort);
			
			addr = clientAddress;
			if(isTarget && mode == 5){
				try {
					addr = InetAddress.getByName("192.168.1.1");
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			sendPacket.setAddress(clientAddress);
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
			
			//return to normal operation
			if(isTarget){
				mode = 0;
			}
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
