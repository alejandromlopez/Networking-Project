import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class Have extends Message{

    public Have(byte[] pieceIDIdk)
    {
        super((byte) 4, pieceIDIdk);
    }

    public Have(int pieceID){
        this(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceID).array());
    }
    
}
