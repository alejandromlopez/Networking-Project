
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Have extends Message {
    private int pieceIdx;
    private byte[] bitfield;
    private int peerID;

    public Have(byte[] pieceIDIdx, int pieceID, int pid) 
    {
        super((byte) 4, pieceIDIdx);
        pieceIdx=pieceID;
        bitfield=pieceIDIdx;
        peerID=pid;
    }

    public Have(int pieceID, int pid) 
    {
        this(ByteBuffer.allocate(4)
                       .order(ByteOrder.BIG_ENDIAN)
                       .putInt(pieceID)
                       .array(), pieceID, pid);
    }
    
    public int getPieceIdx()
    {
        return pieceIdx;
    }

    public byte[] getBitfield(){
        return bitfield;
    }

    public int getPID(){
        return peerID;
    }
}