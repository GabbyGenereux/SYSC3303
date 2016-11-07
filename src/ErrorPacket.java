/***
 * 
 * @author Josh
 *
 */
public class ErrorPacket {
	public static final byte[] opcode = {0, 5};
	private byte errorCode;
	private String errorMessage;
	private byte[] data;

	public byte getErrorCode() {
		return errorCode;
	}
	public String getErrorMessage() {
		return errorMessage;
	}

	/***
	 * 
	 * @param data
	 */
	public ErrorPacket(byte[] data) {
		this.data = data;
		errorCode = data[3];
		
		byte[] strBytes = new byte[data.length - 4];
		
		for (int i = 4; data[i] != 0; i++) {
			strBytes[i - 4] = data[i];
		}
		
		errorMessage = new String(strBytes);
	}
	/***
	 * 
	 * @param code
	 * @param message
	 */
	public ErrorPacket(byte code, String message) {
		errorCode = code;
		errorMessage = message;
		
		byte[] messageBytes = message.getBytes();
		
		data = new byte[4 + messageBytes.length + 1];
		data[0] = opcode[0];
		data[1] = opcode[1];
		data[2] = 0;
		data[3] = code;
		System.arraycopy(messageBytes, 0, data, 4, messageBytes.length);
		data[4 + messageBytes.length] = 0;
	}
	
	public byte[] encode() {
		return data;
	}
}
