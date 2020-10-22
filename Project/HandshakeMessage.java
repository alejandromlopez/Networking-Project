public class HandshakeMessage 
{
    private static final String handshakeHeader = "P2PFILESHARINGPROJ";
    private static final String zeroBits = "0000000000";
    private static int peerID;

    public HandshakeMessage(int pID) 
    {
        peerID = pID;
    }
}
