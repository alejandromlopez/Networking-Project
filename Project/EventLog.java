public class EventLog {
    private final String _header;

    public EventLog (int peerID) {
        _header = ": Peer " + peerID;
    }
    public void TCPConnection(int peerID, boolean isConnecting){
        if(isConnecting){
            final String msg = getHeader() + " makes a connection to Peer " + peerID;
        }
        else{
            final String msg = getHeader() + " is connected from Peer " + peerID;
        }
    }

    public void changeOfPrefNeighbor(int peerID, String neighbors){
        final String msg = getHeader() + " has the preferred neighbors " + neighbors;
    }
    
    public void changeOfOptUnchkNeighbor(int peerID){
        final String msg = getHeader() + " has the optimistically unchoked neighbor " + peerID;
    }

    public void unchoking(int peerID){
        final String msg = getHeader() + " is unchoked by " + peerID;
    }

    public void choking(int peerID){
        final String msg = getHeader() + " is choked by " + peerID;
    }

    public void receivingHave(int peerID, int pieceIdx){
        final String msg = getHeader() + " received the 'have' message from " + peerID + " for the piece " + pieceIdx;
    }
    
    public String getHeader(){
        return _header;
    }
}
