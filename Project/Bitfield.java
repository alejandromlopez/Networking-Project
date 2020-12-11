public class Bitfield extends Message {
    private byte[] bitfield;

    public Bitfield(byte[] bfield) {
        super((byte)5, bfield);
        bitfield = bfield;
    }

    public byte[] getBitfield(){
        return bitfield;
    }
}
