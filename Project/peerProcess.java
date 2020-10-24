import java.util.Properties;
import java.io.*;

public class peerProcess {
    private final int peerID;
    private int numPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;

    public peerProcess(int pID) {
        peerID = pID;
        start();
    }

    private void start() {
        Properties prop = new Properties();
        String workingDir = System.getProperty("user.dir");
        String fileName = workingDir + "/Project/Common.cfg";
        InputStream inStream = null;

        try {
            inStream = new FileInputStream(fileName);
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            prop.load(inStream);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }

        numPreferredNeighbors = Integer.parseInt(prop.getProperty("NumberOfPreferredNeighbors"));
        unchokingInterval = Integer.parseInt(prop.getProperty("UnchokingInterval"));
        optimisticUnchokingInterval = Integer.parseInt(prop.getProperty("OptimisticUnchokingInterval"));
        fileName = prop.getProperty("FileName");
        fileSize = Integer.parseInt(prop.getProperty("FileSize"));
        pieceSize = Integer.parseInt(prop.getProperty("PieceSize"));

        System.out.println("peerID is: " + peerID);
    }
}