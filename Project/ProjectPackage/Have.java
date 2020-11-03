package ProjectPackage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Have extends Message 
{
    private int pieceIdx;

    public Have(byte[] pieceIDIdx, int pieceID) 
    {
        super((byte) 4, pieceIDIdx);
        pieceIdx=pieceID;
    }

    public Have(int pieceID) 
    {
        this(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceID).array(), pieceID);
    }
    
    public int getPieceIdx()
    {
        return pieceIdx;
    }
}