public class Have {
    
    private final int pieceID;
    private final byte[] pIdxField;

    public Have(int pieceID)
    {
        this.pieceID = pieceID;
        this.pIdxField = new byte[4];
        
        byte temp = (byte)pieceID;
        this.pIdxField[0] = (temp);
    }
}
