import java.util.Scanner;

public class ServerInput extends Thread 
{

	Server server;
	
	public ServerInput (String name, Server server)
	{
		super(name);
		this.server = server;
	}
	
	public void run()
	{
		String input = new String();
		Scanner s = new Scanner(System.in);
		while(true)
		{
			System.out.println("Enter \"stop\" to stop running the server.");
			input = s.nextLine();
			if(input.equals("stop")) break;
		}
		s.close();
		server.stop();
	}
}
