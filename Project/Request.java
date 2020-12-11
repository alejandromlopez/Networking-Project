
import java.nio.ByteBuffer;

public class Request extends Message {
    private int pieceIdx;

    public Request(int pIdx) {
        super((byte)4, ByteBuffer.allocate(4).putInt(pIdx).array());
        pieceIdx = pIdx;
    }

    public int getPieceIdx() {
        return pieceIdx;
    }
}
