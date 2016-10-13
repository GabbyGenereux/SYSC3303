public class AckPacket {
	public static final byte[] opcode = {0, 4};
	int blockNum;
	byte[] data;
	
	public int getBlockNum() {
		return blockNum;
	}
	
	public AckPacket(byte[] data) {
		this.data = data;
		blockNum = data[2];
		blockNum <<= 8;
		blockNum |= data[3];
	}
	public AckPacket(int blockNum) {
		data = new byte[4];
		data[0] = opcode[0];
		data[1] = opcode[1];
		data[2] = (byte)((blockNum >> 8) & 0xFF);
		data[3] = (byte)(blockNum & 0xFF);
	}
	
	public byte[] encode() {
		return data;
	}
}
