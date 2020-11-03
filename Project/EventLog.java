

import java.text.SimpleDateFormat;  
import java.util.Date;  
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

//TODO: Find a way to referene this class within remote machine. Worked on local.

public class EventLog {
    private final String _header;
    private File log;
    private FileWriter logger;

    public EventLog(int peerID) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        _header = "[" + formatter.format(date) + "]" + ": Peer " + peerID;
        try{
           log = new File("log_peer_" + peerID + ".log");
           if (log.createNewFile()) {
                System.out.println("File created: " + log.getName());
            } else {
                System.out.println("File already exists.");
            }
            try {
                logger = new FileWriter(log, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    public void TCPConnection(int peerID){
        final String msg = getHeader() + " makes a connection to Peer " + peerID;
        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void recievingInterested(int peerID)
    {
        final String msg = getHeader() + " recieved the not 'interested' message from " + peerID + ".";
        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void recievingNotInterested(int peerID)
    {
        final String msg = getHeader() + " recieved the 'not interested' message from " + peerID + ".";
        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public void downloadingAPiece(int peerID, int pieceIdx, int totalPieces)
    {
        final String msg = getHeader() + " has dowloaded the piece " + pieceIdx
                                       + " from " + peerID 
                                       + ". Now the number of pieces it has is " + (totalPieces + 1)
                                       + ".";
        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
        //TODO: Have to update the Peer1 Total Pieces
    }
    
    public void CompletionOfDownload()
    {
        final String msg = getHeader() + " has downloaded the complete file.";
        System.out.println("1");

        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
            System.out.println(msg);
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void changeOfPrefNeighbor(int peerID, String neighbors){
        final String msg = getHeader() + " has the preferred neighbors " + neighbors;
        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public void changeOfOptUnchkNeighbor(int peerID){
        final String msg = getHeader() + " has the optimistically unchoked neighbor " + peerID;
        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void unchoking(int peerID){
        final String msg = getHeader() + " is unchoked by " + peerID;
        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void choking(int peerID){
        final String msg = getHeader() + " is choked by " + peerID;
        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void receivingHave(int peerID, int pieceIdx){
        final String msg = getHeader() + " received the 'have' message from " + peerID 
                                       + " for the piece " + pieceIdx;
        try 
        {
            logger.write(msg);
            logger.flush();
            logger.close();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public String getHeader(){
        return _header;
    }
}
