
import java.util.Properties;
import java.util.Scanner;
import java.io.*;
import java.nio.file.*;
import java.lang.Math;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class peerProcess {
    private final int peerID;
    private int numPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private byte[] bitField;
    private int numOfPieces;

    private int portNum;

    public peerProcess(int pID) {
        peerID = pID;
        computeNumberOfPiece();
        bitField = new byte[numOfPieces];
        initialize();
    }

    // Moves the file from the current working directory to the specified
    // peerProcess subdirectory
    private void moveFile() {
        String workingDir = System.getProperty("user.dir");
        Path source = new File("file.txt").toPath();
        Path dest = new File(workingDir + "/peer_" + peerID + "/file.txt").toPath();

        try {
            Files.copy(source, dest);
        } catch (FileAlreadyExistsException e1) {
            System.out.println("File is already in this subdirectory");
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    // Read PeerInfo.cfg and Common.cfg and set all necessary variables and read all
    // necessary data
    private void initialize() {
        String workingDir = System.getProperty("user.dir");

        // Creates the subdirectory for the peerProcess
        File dir = new File(workingDir + "/peer_" + peerID);
        dir.mkdir();

        // File path to Common.cfg to read from
        Properties prop = new Properties();
        String file = workingDir + "/Common.cfg";
        InputStream inStream = null;

        try {
            inStream = new FileInputStream(file);
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

        System.out.println(numPreferredNeighbors);
        System.out.println(unchokingInterval);
        System.out.println(optimisticUnchokingInterval);
        System.out.println(fileName);
        System.out.println(fileSize);
        System.out.println(pieceSize);

        // Reading PeerInfo.cfg to adjust this peerProcess's bitfield
        Properties prop2 = new Properties();
        file = workingDir + "/PeerInfo.cfg";
        inStream = null;

        try {
            inStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            prop2.load(inStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * Initializes this peerProcess' bitField according to the PeerInfo.cfg. If the
         * peerProcess owns the entire file, then the file is transferred to the
         * corresponding peerProcess' subdirectory.
         */
        String property = prop2.getProperty("" + peerID);
        String bit = property.split(" ")[2];

        portNum = Integer.parseInt(property.split(" ")[1]);

        if (bit.equals("1")) {
            int leftover = numOfPieces % 8;
            int byteNum = 0;
            for (int i = 0; leftover > i; i++) {
                byteNum += (int) Math.pow(2, 8 - i);
            }

            for (int i = 0; i < bitField.length; i++) {
                if (i == (bitField.length - 1)) {
                    bitField[i] = (byte) byteNum;
                    continue;
                }

                bitField[i] = (byte) 255;
            }
            moveFile();
        }
    }

    // Computes the number of Pieces of the given file
    private void computeNumberOfPiece() {
        double fSize = fileSize;
        double pSize = pieceSize;
        numOfPieces = (int) Math.ceil(fSize / pSize);
    }

    private void establishConnections() {
        Socket socket = null;
        ServerSocket server = null;

        String workingDir = System.getProperty("user.dir");

        Scanner s = null;
        try {
            s = new Scanner(new File(workingDir + "/PeerInfo.cfg"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String line = s.nextLine();

        try {
            server = new ServerSocket(portNum);
        } catch (IOException e) {
            System.out.println(e);
        }

        while (s.hasNext()) {
            String[] fields = line.split(" ");
            int ID = Integer.parseInt(fields[0]);
            String address = fields[1];
            int port = Integer.parseInt(fields[2]);

            /*
             * Checks if this peerProcess is NOT the first one to run. If so, then establish
             * connections to the peerProcesses that came before.
             */
            if (ID != peerID) {
                try {
                    socket = new Socket(address, port);
                    System.out.println("Connection established with " + address);
                    // File dir = new File(workingDir + "/peer_" + peerID + "_to_peer_" + fields[0]);
                    // dir.mkdir();
                } catch (UnknownHostException e1) {
                    System.out.println("Unknown host: " + fields[1]);
                    e1.printStackTrace();
                } catch (IOException e2) {
                    System.out.println("IOException at port " + fields[2]);
                    e2.printStackTrace();
                }
            } else {
                break;
            }

            line = s.nextLine();
        }

        if (Integer.parseInt(line.split(" ")[0]) != peerID) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // TODO: comment these lines out
        try {
            TimeUnit.MILLISECONDS.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    // Starts up the peerProcess and begins message delivery
    public static void main(String[] args) {
        peerProcess pp = new peerProcess(Integer.parseInt(args[0]));
        pp.establishConnections();
    }
}