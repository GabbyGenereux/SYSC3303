import java.util.Scanner;

public class HostInput extends Thread 
{
	private Scanner s;
	private IntermediateHost host;
	private static final int modes = 7; //max mode number
	
	
	public HostInput (String name, IntermediateHost host)
	{
		super(name);
		this.host = host;
	}
	
	public void run()
	{
		String input = new String();
		s = new Scanner(System.in);
		while(true)
		{
			int mode = getModeNumber();
			byte[] code = {};
			byte[] newCode = {};
			int delay = -1;
			if(mode != 0){
				String type = getPacketType();
				byte[] num = null;
				byte[] newByte = null;
				if(type.equals("ACK") || type.equals("DATA")){
					num = getPacketNumber();
				}
				if(mode == 2 || mode == 3){
					delay = getDelay();
				}
				else if(mode == 4 || mode == 5){
					newByte = getCodeBytes();
				}
				else if(mode == 7){
					newByte = getNewMode().getBytes();
				}
				if(mode != 7){
					code = getCode(getOpcode(type),num);
					if(mode == 4){
						newCode = getCode(newByte,num);
					}
					else if(mode == 5){
						newCode = getCode(getOpcode(type),newByte);
					}
				}
			}
			
			host.setMode(mode,code,newCode,delay);
		}
		//s.close();
	}
	
	private int getModeNumber(){
		int mode = -1;
		System.out.println("Enter Error Simulator mode:");
		System.out.println("\t0 : normal operation\n\t1 : lose a packet\n\t2 : delay a packet\n\t3 : duplicate a packet\n\t4 : change packet opcode\n\t5 : change packet block number\n\t6 : replace a zero byte\n\t7 : corrupt mode type");
		do{
			String m = s.nextLine();
			try{
				mode = Integer.parseInt(m);
				if(mode < 0 || mode > 7){
					System.err.println("Not a mode number");
				}
			}
			catch(NumberFormatException e){
				System.err.println("Not a mode number");
			}
			
		}while(mode < 0 || mode > 7);
		return mode;
	}
	
	private String getPacketType(){
		String type = "";
		System.out.println("Enter Packet type to trigger error:");
		System.out.println("Packets: RRQ, WRQ, ACK, DATA");
		do{
			type = s.nextLine().toUpperCase();
			if(!type.equals("RRQ") && !type.equals("WRQ") && !type.equals("ACK") && !type.equals("DATA")){
				System.err.println("Not a valid packet type");
			}
		}while(!type.equals("RRQ") && !type.equals("WRQ") && !type.equals("ACK") && !type.equals("DATA"));
		return type;
	}
	
	private byte[] getPacketNumber(){
		int num = -1;
		do{
			System.out.println("Enter Packet number to trigger error:");
			String n = s.nextLine();
			try{
				num = Integer.parseInt(n);
				if(num < 0){
					System.err.println("Not a valid number");
				}
			}
			catch(NumberFormatException e){
				System.err.println("Not a valid number");
			}
		}while(num < 0);
		byte[] number = {(byte)((num >> 8) & 0xFF),(byte)(num & 0xFF)};
		return number;
	}
	
	private int getDelay(){
		int num = -1;
		do{
			System.out.println("Enter delay length for packet (milliseconds):");
			String n = s.nextLine();
			try{
				num = Integer.parseInt(n);
				if(num < 0){
					System.err.println("Not a valid number");
				}
			}
			catch(NumberFormatException e){
				System.err.println("Not a valid number");
			}
		}while(num < 0);
		return num;
	}
	
	private String getNewMode(){
		String type = "";
		System.out.println("Enter replacement mode name:");
		do{
			type = s.nextLine().toUpperCase();
		}while(type == null);
		return type;
	}
	
	private byte[] getCodeBytes(){
		int num = -1;
		do{
			System.out.println("Enter replacement bytes (as Integer value):");
			String n = s.nextLine();
			try{
				num = Integer.parseInt(n);
				if(num < 0){
					System.err.println("Not a valid number (0-65535)");
				}
			}
			catch(NumberFormatException e){
				System.err.println("Not a valid number (0-65535)");
			}
		}while(num < 0);
		byte[] number = {(byte)((num >> 8) & 0xFF),(byte)(num & 0xFF)};
		return number;
	}
	
	private byte[] getOpcode(String type){
		byte[] code = new byte[2];
		switch(type){
		case "RRQ":
			code[1] = 1;
			break;
		case "WRQ":
			code[1] = 2;
			break;
		case "ACK":
			code[1] = 3;
			break;	
		case "DATA":
			code[1] = 4;
			break;	
		}
		return code;
	}
	
	private byte[] getCode(byte[] arr1, byte[] arr2){
		if(arr2 == null || arr2.length < 2){
			return arr1;
		}
		byte[] code = new byte[4];
		code[0] = arr1[0];
		code[1] = arr1[1];
		code[2] = arr2[0];
		code[3] = arr2[1];
		return code;
	}
}
