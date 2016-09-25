import java.net.DatagramPacket;

public class TFTPInfoPrinter {
	private static boolean verboseMode = true;
	
	
	public static boolean isVerboseMode() {
		return verboseMode;
	}

	public static void setVerboseMode(boolean verboseMode) {
		TFTPInfoPrinter.verboseMode = verboseMode;
	}

	public static void printReceived(DatagramPacket receivePacket) {
		if (verboseMode) {
			byte[] data = receivePacket.getData();
			
			System.out.println("Intermediate Host: Packet received.");
			System.out.println("From host: " + receivePacket.getAddress());
			System.out.println("Host port: " + receivePacket.getPort());
			int len = receivePacket.getLength();
			System.out.println("Length: "+ len);
			System.out.print("Containing: ");
			String received = new String(data, 0, len);
			System.out.println(received);
			for(int k = 0; k < len; k++)
			{
				System.out.print(data[k] + " ");
			}
			System.out.println("\n");
		}	
		
	}
	
	public static void printSent(DatagramPacket sendPacket) {
		if (verboseMode) {
			byte[] data = sendPacket.getData();
			
			System.out.println("Packet sent.");
			System.out.println("To host: " + sendPacket.getAddress());
			System.out.println("Host port: " + sendPacket.getPort());
			int len = sendPacket.getLength();
			System.out.println("Length: "+ len);
			System.out.print("Containing: ");
			String sent = new String(data, 0, len);
			System.out.println(sent);
			for(int k = 0; k < len; k++)
			{
				System.out.print(data[k] + " ");
			}
			System.out.println("\n");
		}
	}
}
