package Messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Have extends Message 
{

    public Have(byte[] pieceIDIdx) 
    {
        super((byte) 4, pieceIDIdx);
    }

    public Have(int pieceID) 
    {
        this(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceID).array());
    }
    
}