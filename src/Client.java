import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
	private byte[] readReq = {0, 1};
	private byte[] writeReq = {0, 2};
	
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendAndReceiveSocket;

	/*
	 * Constructor
	 * Initializes the Datagram Socket
	 */
	public Client() {
		try {
			sendAndReceiveSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * 
	 * @param fileName
	 * @param mode
	 */
	public void sendAndReceive(String fileName, String mode) {
		
		//side of array determined by the length of the two strings, plus the 4 bytes added to the array
		byte message[];
		//Repeat 10 times, alternating between read and write request
		for (int j = 0; j < 10; j++) {
			//read request
			if (j % 2 == 0) {
				message = formatRequest(readReq, fileName, mode);
			} else {
				//write request
				message = formatRequest(writeReq, fileName, mode);
			}
			System.out.println("Client: Sending packet ");

			//create Datagram packet to send
			try {
				sendPacket = new DatagramPacket(message, message.length, InetAddress.getLocalHost(), 23);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			//Print out packet information
			System.out.println("to host: " + sendPacket.getAddress());
			System.out.println("Destination host port: " + sendPacket.getPort());
			int len = sendPacket.getLength();
			System.out.println("Length: " + len);
			System.out.print("Containing: ");
			System.out.println(new String(sendPacket.getData(), 0, len));			
			for(int k = 0; k < sendPacket.getData().length; k++)
			{
				System.out.print(sendPacket.getData()[k] + " ");
			}
			System.out.println("\n");
			//send packet
			try {
				sendAndReceiveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Client: Packet sent.");
			
			System.out.println("Client: Waiting for packet..");

			byte data[] = new byte[4];
			receivePacket = new DatagramPacket(data, data.length, sendPacket.getAddress(), sendPacket.getPort());
			//Receive packet
			try {
				sendAndReceiveSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			//print received packet information
			System.out.println("Client: Packet received");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			len = receivePacket.getLength();
			System.out.println("Length: " + len);
			System.out.print("Containing: ");
			String received = new String(data, 0, len);
			System.out.println(received);
			for(int k = 0; k < receivePacket.getData().length; k++)
			{
				System.out.print(receivePacket.getData()[k] + " ");
			}
			System.out.println("\n");
		}
		message = new byte[] {1, 2, 3, 4, 5}; // incorrect formatting 
		System.out.println("Client: Sending final packet.");

		sendPacket.setData(message);
		sendPacket.setLength(message.length);
		try {
			sendPacket.setAddress(InetAddress.getLocalHost());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		sendPacket.setPort(23);
		//Print out packet information
		System.out.println("to host: " + sendPacket.getAddress());
		System.out.println("Destination host port: " + sendPacket.getPort());
		int len = sendPacket.getLength();
		System.out.println("Length: " + len);
		System.out.print("Containing: ");
		System.out.println(new String(sendPacket.getData(), 0, len));			
		for(int l = 0; l < sendPacket.getData().length; l++)
		{
			System.out.print(sendPacket.getData()[l] + " ");
		}
		System.out.println("\n");
		//send packet
		try {
			sendAndReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Client: Packet sent.");
		sendAndReceiveSocket.close();

	}
	/**
	 * 
	 * @param reqType
	 * @param filename
	 * @param mode
	 * @return
	 */
	private byte[] formatRequest(byte[] reqType, String filename, String mode) {
		byte[] request = new byte[reqType.length + filename.length() + mode.length() + 2]; // +2 for the zero byte after filename and after mode.
		byte[] filenameData = filename.getBytes();
		byte[] modeData = mode.toLowerCase().getBytes();
		int i, j, k;
		
		for (i = 0; i < reqType.length; i++) {
			request[i] = reqType[i];
		}

		for (j = 0; j < filenameData.length; j++) {
			request[i + j] = filenameData[j];
		}
		request[i + j] = 0; // zero byte after filename.
		j++; 
		for (k = 0; k < modeData.length; k++) {
			request[i + j + k] = modeData[k];
		}
		
		request[i + j + k] = 0; // final zero byte.

		return request;
	}
	
	public static void main(String args[]) {
		Client c = new Client();
		c.sendAndReceive("file.txt", "octet");
	}

}
