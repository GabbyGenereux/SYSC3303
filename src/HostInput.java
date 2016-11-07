import java.util.Scanner;

public class HostInput extends Thread 
{

	IntermediateHost host;
	
	
	public HostInput (String name, IntermediateHost host)
	{
		super(name);
		this.host = host;
	}
	
	public void run()
	{
		String input = new String();
		Scanner s = new Scanner(System.in);
		while(true)
		{
			int mode = 0;
			byte[] code = new byte[2];
			int delay = -1;
			System.out.println("Enter Error Simulator mode:");
			System.out.println("\t0 : normal operation\n\t1 : lose a packet\n\t2 : delay a packet\n\t3 : duplicate a packet");
			input = s.nextLine();
			try{
				mode = Integer.parseInt(input);
				if(mode == 0){
					//set to mode 0
					host.setMode(mode,code,delay);
				}
				else if(mode > 0 && mode <= 3){
					String type;
					int num = -1;
					//Set packet type
					do{
						System.out.println("Enter Packet type to trigger error:");
						System.out.println("Packets: RRQ, WRQ, ACK, DATA");
						type = s.nextLine();	
					}while(!type.equals("RRQ") && !type.equals("WRQ") && !type.equals("ACK") && !type.equals("DATA"));
					//Set packet number if needed
					if(type.equals("ACK") || type.equals("DATA")){
						do{
							System.out.println("Enter Packet number to trigger error:");
							input = s.nextLine();
							try{
								num = Integer.parseInt(input);
							}catch(NumberFormatException e){
								num = -1;
							}
						}while(num < 0);
					}
					//set delay if needed
					if(mode != 1){
						do{
							System.out.println("Enter delay length for packet (milliseconds):");
							input = s.nextLine();
							try{
								delay = Integer.parseInt(input);
							}catch(NumberFormatException e){
								delay = -1;
							}
						}while(delay < 0);
					}
					switch(type){
					case "RRQ":
						code = new byte[2];
						code[1] = 1;
						break;
					case "WRQ":
						code = new byte[2];
						code[1] = 2;
						break;
					case "ACK":
						code = new byte[4];
						code[1] = 3;
						code[2] = (byte)((num >> 8) & 0xFF);
						code[3] = (byte)(num & 0xFF);
						break;	
					case "DATA":
						code = new byte[4];
						code[1] = 4;
						code[2] = (byte)((num >> 8) & 0xFF);
						code[3] = (byte)(num & 0xFF);
						break;	
					}
					host.setMode(mode,code,delay);
				}
				else{
					System.err.println("Not a mode number");
				}
			}catch(NumberFormatException e){
				System.err.println("Not a mode number");
			}
		}
		//s.close();
	}
}
