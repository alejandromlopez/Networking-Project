import java.text.SimpleDateFormat;  
import java.util.Date;  

public class EventLog {
    private final String _header;

    public EventLog (int peerID) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  
        Date date = new Date();
        _header = "[" + formatter.format(date) + "]" + ": Peer " + peerID;
    }
    public void TCPConnection(int peerID){
        final String msg = getHeader() + " makes a connection to Peer " + peerID;
    }

    public void recievingInterested(int peerID)
    {
        final String msg = getHeader() + " recieved the not 'interested' message from " + peerID + ".";
    }

    public void recievingNotInterested(int peerID)
    {
        final String msg = getHeader() + " recieved the 'not interested' message from " + peerID + ".";
    }
    
    public void downloadingAPiece(int peerID, int pieceIdx, int totalPieces)
    {
        final String msg = getHeader() + " has dowloaded the piece " + pieceIdx
                                       + " from " + peerID 
                                       + ". Now the number of pieces it has is " + (totalPieces + 1)
                                       + ".";
        //TODO: Have to update the Peer1 Total Pieces
    }
    
    public void CompletionOfDownload()
    {
        final String msg = getHeader() + " has downloaded the complete file.";
    }

    public String getHeader(){
        return _header;
    }
}
