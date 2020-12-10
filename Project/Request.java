
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Request extends Message {
    private int pieceIdx;
    private int peerID;

    public Request(byte[] bitfield, int pieceID, int pid) {
        super((byte)4, bitfield);
        pieceIdx = pieceID;
        peerID=pid;
    }

    public Request(int pieceID, int pid) {
        this(ByteBuffer.allocate(4)
                       .order(ByteOrder.BIG_ENDIAN)
                       .putInt(pieceID)
                       .array(), pieceID, pid);
    }
    
    public int getPieceIdx() {
        return pieceIdx;
    }

    public int getPID(){
        return peerID;
    }
}
