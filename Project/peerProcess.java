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
    private String bitField;

    public peerProcess(int pID) {
        peerID = pID;
        bitField = "0000000000";
        start();
    }

    // Starting message delivery
    private void start() {
        // File path to Common.cfg to read from
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

        // Initializing variables from Common.cfg
        numPreferredNeighbors = Integer.parseInt(prop.getProperty("NumberOfPreferredNeighbors"));
        unchokingInterval = Integer.parseInt(prop.getProperty("UnchokingInterval"));
        optimisticUnchokingInterval = Integer.parseInt(prop.getProperty("OptimisticUnchokingInterval"));
        fileName = prop.getProperty("FileName");
        fileSize = Integer.parseInt(prop.getProperty("FileSize"));
        pieceSize = Integer.parseInt(prop.getProperty("PieceSize"));

        System.out.println("peerID is: " + peerID);

        // Reading PeerInfo.cfg to adjust this peerProcess's bitfield
        Properties prop2 = new Properties();
        fileName = workingDir + "/Project/PeerInfo.cfg";
        inStream = null;

        try {
            inStream = new FileInputStream(fileName);
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            prop2.load(inStream);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }

        // Initializes this peerProcess' bitField accoriding to the PeerInfo.cfg
        String property = prop2.getProperty("" + peerID);
        String bit = property.split(" ")[2];

        if (bit.equals("1"))
            bitField = "1111111111";
        else
            bitField = "0000000000";
    }
}