import java.util.Properties;
import java.io.*;
import java.nio.file.*;
import java.nio.*;
import java.lang.Math;

public class peerProcess {
    private static int peerID;
    private static int numPreferredNeighbors;
    private static int unchokingInterval;
    private static int optimisticUnchokingInterval;
    private static String fileName;
    private static int fileSize;
    private static int pieceSize;
    private static byte[] bitField;
    private static int numOfPieces;

    public peerProcess(int pID) {
        peerID = pID;
        computeNumberOfPiece();
        bitField = new byte[numOfPieces];
    }

    // Moves the file from the current working directory to the specified peerProcess subdirectory
    private static void moveFile() {
        String workingDir = System.getProperty("user.dir");
        Path source = new File(workingDir + "/file.txt").toPath();
        Path dest = new File(workingDir + "/peer_" + peerID + "/file.txt").toPath();

        try {
            Files.copy(source, dest);
        } catch (FileAlreadyExistsException e1) {
            System.out.println("File is already in this subdirectory");
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    // INSERT COMMENT HERE FOR EXPLANATION
    private void computeNumberOfPiece() {
        double fSize = fileSize;
        double pSize = pieceSize;
        numOfPieces = (int) Math.ceil(fSize/pSize);
    }

    // Starting message delivery
    public static void main(String[] args) {
        peerProcess pp = new peerProcess(Integer.parseInt(args[0]));
        String workingDir = System.getProperty("user.dir");
        
        // Creates the subdirectory for the peerProcess
        File file = new File(workingDir + "/peer_" + peerID);
        file.mkdir();
        
        // File path to Common.cfg to read from
        Properties prop = new Properties();
        String fileName = workingDir + "/Common.cfg";
        InputStream inStream = null;

        try {
            inStream = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            prop.load(inStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initializing variables from Common.cfg
        numPreferredNeighbors = Integer.parseInt(prop.getProperty("NumberOfPreferredNeighbors"));
        unchokingInterval = Integer.parseInt(prop.getProperty("UnchokingInterval"));
        optimisticUnchokingInterval = Integer.parseInt(prop.getProperty("OptimisticUnchokingInterval"));
        fileName = prop.getProperty("FileName");
        fileSize = Integer.parseInt(prop.getProperty("FileSize"));
        pieceSize = Integer.parseInt(prop.getProperty("PieceSize"));

        // Reading PeerInfo.cfg to adjust this peerProcess's bitfield
        Properties prop2 = new Properties();
        fileName = workingDir + "/PeerInfo.cfg";
        inStream = null;

        try {
            inStream = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            prop2.load(inStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
            Initializes this peerProcess' bitField according to the PeerInfo.cfg
            If the peerProcess owns the entire file, then the file is transferred to
            the corresponding peerProcess' subdirectory
        */
        String property = prop2.getProperty("" + peerID);
        String bit = property.split(" ")[2];

        if (bit.equals("1")) {
            int leftover = numOfPieces % 8;
            int byteNum = 0;
            for (int i = 0; leftover > i; i++)
            {
                byteNum += (int) Math.pow(2, 8-i);
            }

            for (int i = 0; i < bitField.length; i++)
            {
                if ( i == (bitField.length - 1))
                {
                    bitField[i] = (byte) byteNum;
                    continue;
                }

                bitField[i] = (byte) 255;
                
            }
            moveFile();
        }
    }
}