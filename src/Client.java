import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {

	DatagramPacket sendPacket, recievePacket;
	DatagramSocket sendAndRecieveSocket;

	/*
	 * Constructor
	 * Initializes the Datagram Socket
	 */
	public Client() {
		try {
			sendAndRecieveSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * void sendAndRecieve
	 * Para - Strings representing the file name and the mode
	 */
	public void sendAndRecieve(String fileName, String mode) {
		
		//side of array determined by the length of the two strings, plus the 4 bytes added to the array
		byte message[] = new byte[fileName.length() + mode.length() + 4];
		byte zeroByte = 0;
		byte oneByte = 1;
		byte twoByte = 2;
		//Repeat 10 times, alternating between read and write request
		for (int j = 0; j < 10; j++) {
			//read request
			if (j % 2 == 0) {
				message[0] = zeroByte;
				message[1] = oneByte;
				int i, k;
				for (i = 2; i < fileName.length() + 2; i++) {
					message[i] = (byte) fileName.charAt(i - 2);
				}
				message[i] = zeroByte;
				i++;
				for (k = 0; k < mode.length(); k++) {
					message[i + k] = (byte) mode.charAt(k);
				}
				message[i + k] = zeroByte;
			} else {
				//write request
				message[0] = zeroByte;
				message[1] = twoByte;
				int i, k;
				for (i = 2; i < fileName.length() + 2; i++) {
					message[i] = (byte) fileName.charAt(i - 2);
				}
				message[i] = zeroByte;
				i++;
				for (k = 0; k < mode.length(); k++) {
					message[i + k] = (byte) mode.charAt(k);
				}
				message[i + k] = zeroByte;
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
				sendAndRecieveSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			System.out.println("Client: Packet sent.");
			
			System.out.println("Client: Waiting for packet..");

			byte data[] = new byte[4];
			recievePacket = new DatagramPacket(data, data.length, sendPacket.getAddress(), sendPacket.getPort());
			//Receive packet
			try {
				sendAndRecieveSocket.receive(recievePacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			//print received packet information
			System.out.println("Client: Packet recieved");
			System.out.println("From host: " + recievePacket.getAddress());
			System.out.println("Host port: " + recievePacket.getPort());
			len = recievePacket.getLength();
			System.out.println("Length: " + len);
			System.out.print("Containing: ");
			String recieved = new String(data, 0, len);
			System.out.println(recieved);
			for(int k = 0; k < recievePacket.getData().length; k++)
			{
				System.out.print(recievePacket.getData()[k] + " ");
			}
			System.out.println("\n");
		}
		message[0] = zeroByte;
		message[1] = zeroByte;
		int i, k;
		for (i = 2; i < fileName.length() + 2; i++) {
			message[i] = (byte) fileName.charAt(i - 2);
		}
		message[i] = zeroByte;
		i++;
		for (k = 0; k < mode.length(); k++) {
			message[i + k] = (byte) mode.charAt(k);
		}
		message[i + k] = zeroByte;
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
			sendAndRecieveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Client: Packet sent.");
		sendAndRecieveSocket.close();

	}

	public static void main(String args[]) {
		Client c = new Client();
		c.sendAndRecieve("file.txt", "octet");
	}

}
