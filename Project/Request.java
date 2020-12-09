
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Request extends Message {
    private int pieceIdx;

    public Request(byte[] pieceIDIdx, int pieceID) {
        super((byte)4, pieceIDIdx);
        pieceIdx = pieceID;
    }

    public Request(int pieceID) {
        this(ByteBuffer.allocate(4)
                       .order(ByteOrder.BIG_ENDIAN)
                       .putInt(pieceID)
                       .array(), pieceID);
    }
    
    public int getPieceIdx() {
        return pieceIdx;
    }
}
