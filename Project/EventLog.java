
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

//TODO: Find a way to referene this class within remote machine. Worked on local.

public class EventLog {
    private String _header;
    private Date date;
    private File log;
    private FileWriter logger;
    private int peerID;

    public EventLog(int pid) {
        _header = "";
        peerID = pid;
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
    public void TCPConnectionTo(int peerID){
        final String msg = getHeader() + " makes a connection to Peer " + peerID + "\n";
        try 
        {
            logger.write(msg);
            logger.flush();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void TCPConnectionFrom(int peerID){
        final String msg = getHeader() + " makes a connection from Peer " + peerID + "\n";
        try
        {
            logger.write(msg);
            logger.flush();
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void receivingInterested(int peerID)
    {
        final String msg = getHeader() + " received the 'interested' message from " + peerID + ".\n";
        try 
        {
            logger.write(msg);
            logger.flush();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void receivingNotInterested(int peerID)
    {
        final String msg = getHeader() + " received the 'not interested' message from " + peerID + ".\n";
        try 
        {
            logger.write(msg);
            logger.flush();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public void downloadingAPiece(int peerID, int pieceIdx, int totalPieces)
    {
        final String msg = getHeader() + " has downloaded the piece " + pieceIdx
                                       + " from " + peerID 
                                       + ". Now the number of pieces it has is " + (totalPieces)
                                       + ".\n";
        try 
        {
            logger.write(msg);
            logger.flush();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
        //TODO: Have to update the Peer1 Total Pieces
    }
    
    public void CompletionOfDownload()
    {
        final String msg = getHeader() + " has downloaded the complete file.\n";
        //System.out.println("1");

        try 
        {
            logger.write(msg);
            logger.flush();
            //System.out.println(msg);
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void changeOfPrefNeighbor(ArrayList<Integer> neighbors){
        String neighborlist = "";
        for (int i = 0; i < neighbors.size(); i++){
            if (i != neighbors.size()-1)
                neighborlist += " " + neighbors.get(i) + ",";
            else
                neighborlist += " " + neighbors.get(i);
        }
        final String msg = getHeader() + " has the preferred neighbors" + neighborlist + ".\n";
        try 
        {
            logger.write(msg);
            logger.flush();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public void changeOfOptUnchkNeighbor(int peerID){
        final String msg = getHeader() + " has the optimistically unchoked neighbor " + peerID + ".\n";
        try 
        {
            logger.write(msg);
            logger.flush();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void unchoking(int peerID){
        final String msg = getHeader() + " is unchoked by " + peerID + ".\n";
        try 
        {
            logger.write(msg);
            logger.flush();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void choking(int peerID){
        final String msg = getHeader() + " is choked by " + peerID + ".\n";
        try 
        {
            logger.write(msg);
            logger.flush();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    public void receivingHave(int peerID, int pieceIdx){
        final String msg = getHeader() + " received the 'have' message from " + peerID 
                                       + " for the piece " + pieceIdx + ".\n";
        try 
        {
            logger.write(msg);
            logger.flush();
        } catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
    
    public void closeLogger(){
        try
        {
            logger.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getHeader(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        date = new Date();
        _header = "[" + formatter.format(date) + "]" + ": Peer " + peerID;
        return _header;
    }
}
