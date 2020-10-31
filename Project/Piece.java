import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Piece extends Message
{

    public Piece(byte[] piecePayload) 
    {
        super((byte) 7, piecePayload);
    }

    public Piece(int pieceID, byte[] pieceIDIdx) 
    {
        super((byte) 7, AllForOne(pieceID, pieceIDIdx));
    }

    //Used to concatenate the pieceID with toBeAdded to create the piece payload.
    private static byte[] AllForOne(int pID, byte[] toBeAdded)
    {
        int len;
        if (toBeAdded != null)
        {
            len = toBeAdded.length;
        } 
        else
        {
            len = 0;
        }
        byte[] temp = new byte[4 + len];
        System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pID).array(), 0, temp, 0, 4);
        System.arraycopy(toBeAdded, 0, temp, 4, toBeAdded.length);
        return temp;
    }
    
}
