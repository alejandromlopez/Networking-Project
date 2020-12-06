import java.io.Serializable;

public class HandshakeMessage implements Serializable {
    private final String handshakeHeader = "P2PFILESHARINGPROJ";
    private String zeroBits = "0000000000";
    private final int peerID;

    public HandshakeMessage(int pID) {
        peerID = pID;
    }

    public String getHandshakeHeader() {
        return handshakeHeader;
    }

    public String getZeroBits() {
        return zeroBits;
    }

    public int getPeerID() {
        return peerID;
    }

    public void setZeroBits(String zb) {
        zeroBits = zb;
    }

    // public void read (DataInputStream in) throws IOException 
    // {
    //     if ((mPayload != null) && (mPayload.length) > 0) 
    //     {
    //         in.readFully(mPayload, 0, mPayload.length);
    //     }

    // }

    // public void write(DataOutputStream o) throws IOException
    // {
    //     o.writeChars(handshakeHeader);
    //     o.writeChars(zeroBits);
    //     o.writeInt(peerID);
    // }
}