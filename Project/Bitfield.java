import java.lang.Math;

public class Bitfield extends Message
{
    public Bitfield(byte[] bitfield)
    {
        super((byte) 5, bitfield);
    }

    public static byte[] update(byte[] bitfield, int pieceIdx)
    {
        int idx = (pieceIdx / 8);
        int byteToInt = bitfield[idx];
        int pow = pieceIdx % 8;
        byteToInt += (int) Math.pow(2, 8-pow);
        bitfield[idx] = (byte) byteToInt;
        return bitfield;
    }
    
}
