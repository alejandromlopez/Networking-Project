import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Request extends Message
{

    public Request(byte[] pieceIDIdx) 
    {
        super((byte) 6, pieceIDIdx);
    }

    public Request(int pieceID) 
    {
        this(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceID).array());
    }
    
}
