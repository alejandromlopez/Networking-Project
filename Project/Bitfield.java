
import java.lang.Math;
import java.io.Serializable;

public class Bitfield extends Message implements Serializable  {
    private byte[] bitfield;
    private int peerID;

    public Bitfield(byte[] bfield, int pid) {
        super((byte)5, bfield);
        peerID = pid;
    }

    public static byte[] update(byte[] bfield, int pieceIdx) {
        int idx = (pieceIdx / 8);
        int byteToInt = bfield[idx];
        int pow = pieceIdx % 8;
        byteToInt += (int) Math.pow(2, 8-pow);
        bfield[idx] = (byte) byteToInt;
        return bfield;
    }

    public byte[] getBitfield(){
        return bitfield;
    }

    public int getPID(){
        return peerID;
    }
    
}
