public class EventLog {
    private final String _header;

    public EventLog (int peerID) {
        _header = ": Peer " + peerID;
    }
    public void TCPConnection(int peerID){
        final String msg = getHeader() + " makes a connection to Peer " + peerID;
    }

    
    public String getHeader(){
        return _header;
    }
}
