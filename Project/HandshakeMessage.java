

public class HandshakeMessage {
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
}