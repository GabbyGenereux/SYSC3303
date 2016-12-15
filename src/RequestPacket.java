import java.util.Arrays;

public class RequestPacket {
	public static final byte[] readOpcode = {0, 1};
	public static final byte[] writeOpcode = {0, 2};
	private byte[] data;
	byte[] reqType;
	private String filename;
	private String mode;
	
	public String getFilename() {
		return filename;
	}
	public String getMode() {
		return mode;
	}
	
	public static boolean isValid(byte[] data) {
		byte[] opcode = {data[0], data[1]};
		if (!(Arrays.equals(opcode, readOpcode) || Arrays.equals(opcode, writeOpcode))) return false;
		
		int i;
		try {
			for (i = 2; data[i] != 0; i++) {
				; // Empty loop
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return false; // 0 byte was not found.
		}
		
		if (i == 2) return false; // Filename must be have characters.
		
		try {
			for (i++; data[i] != 0; i++) {
				
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return false; // 0 byte was not found.
		}
		
		return true;
	}
	
	public RequestPacket(byte[] data) {
		//if (!isValid(data)) throw new IllegalArgumentException("");
		this.data = data;
		int i, j;
		byte[] buffer = new byte[data.length];
		
		for (i = 2; data[i] != 0; i++) {
			buffer[i-2] = data[i];
		}
		filename = new String(Arrays.copyOf(buffer, i-2));
		
		// Start after 0 byte that denoted end of file name.
		for (j = ++i; data[i] != 0; i++) {
			buffer[i - j] = data[i];
		}
		mode = new String(Arrays.copyOf(buffer,  i - j));

	}
	public RequestPacket(byte[] reqType, String filename, String mode) {
		this.filename = filename;
		this.mode = mode;
		byte[] filenameBytes = filename.getBytes();
		byte[] modeBytes = mode.getBytes();
		
		// opcode + 2 zero bytes + filename bytes + mode bytes
		data = new byte[4 + filenameBytes.length + modeBytes.length];
		
		if (Arrays.equals(reqType, readOpcode)){
			data[0] = readOpcode[0];
			data[1] = readOpcode[1];
		}
		else if (Arrays.equals(reqType,  writeOpcode)){
			data[0] = writeOpcode[0];
			data[1] = writeOpcode[1];
		}
		else System.out.println("ERROR: INVALID REQ TYPE");
	
		// offset of 2 to dest because of opcode
		System.arraycopy(filenameBytes, 0, data, 2, filenameBytes.length);
		data[2 + filenameBytes.length] = 0;
		// Now extra 3 offset because of opcode + 0 byte
		System.arraycopy(modeBytes, 0, data, filenameBytes.length + 3, modeBytes.length);
		data[3 + filenameBytes.length + modeBytes.length] = 0;
		
	}
	public byte[] encode() {
		return data;
	}
}
