import java.nio.ByteBuffer;

public class Have extends Message {
    private int pieceIdx;
    private byte[] bitfield;

    public Have(int pIdx) 
    {
        super((byte) 4, ByteBuffer.allocate(4).putInt(pIdx).array());
    }
    
    public int getPieceIdx()
    {
        return pieceIdx;
    }

    public byte[] getBitfield(){
        return bitfield;
    }
}