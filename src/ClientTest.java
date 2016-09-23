import java.io.IOException;


public class ClientTest extends Thread{
	Client client;
	String name, mode;
	
	public ClientTest(String name, String mode){
		client = new Client();
		this.name = name;
		this.mode = mode;
	}
	
	public void run(){
		try {
			client.readFromServer(name, mode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		Thread c1 = new ClientTest("test.txt", "octet");
		Thread c2 = new ClientTest("test.txt", "octet");
		Thread c3 = new ClientTest("test.txt", "octet");
		try{
			c1.start();
			Thread.sleep(10);
			c2.start();
			Thread.sleep(10);
			c3.start();
		}catch(InterruptedException e){
			
		}
	}
}
