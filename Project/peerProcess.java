

import java.util.Properties;
import java.util.Scanner;
import java.io.*;
import java.nio.file.*;
import java.lang.Math;
import java.net.*;

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
    private int firstID;


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

    private void createSocket(){
        System.out.println("Starting createSocket");
        String workingDir = System.getProperty("user.dir");
        boolean bool = false;
        Scanner s = null;
        try {
            s = new Scanner(new File(workingDir + "/PeerInfo.cfg"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while(bool){
            System.out.println("First While");
            String line = s.nextLine();
            firstID = Integer.parseInt(line.split(" ")[0]);
            Socket socket = null;
            Server server = null;
            Thread sThread = null;
            System.out.println("Created Threads and Servers");

            while (s.hasNext()) {
                server = new Server(portNum);
                sThread = new Thread(server);
                byte[] a;
                do{
                    try {
                        s = new Scanner(new File(workingDir + "/PeerInfo.cfg"));
                        System.out.println("Try");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } while (bool);
                System.out.println("Finished");
            }
        }
    }


    private void startProtocol() {
        String workingDir = System.getProperty("user.dir");
        Scanner s = null;
        try {
            s = new Scanner(new File(workingDir + "/PeerInfo.cfg"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = s.nextLine();
        firstID = Integer.parseInt(line.split(" ")[0]);
        Socket socket = null;
        Server server = null;
        Thread sThread = null;

        while (s.hasNext()) {
            server = new Server(portNum);
            sThread = new Thread(server);
            sThread.start();


            /* 
            * Checks if this peerProcess is NOT the first one to run.
            * If so, then establish connections to the peerProcesses
            * that came before.
            */ 
            if (firstID != peerID) {
                String[] fields = line.split(" ");
                System.out.println("This is not the first peerProcess");

                if (Integer.parseInt(fields[0]) == peerID) {
                    break;
                }

                try {
                    socket = new Socket(fields[1], Integer.parseInt(fields[2]));
                    File dir = new File(workingDir + "/peer_" + peerID);
                    dir.mkdir();
                } catch (UnknownHostException e1) {
                    System.out.println("Unknown host: " + fields[1]);
                    e1.printStackTrace();
                } catch (IOException e2) {
                    System.out.println("IOException at port " + fields[2]);
                    e2.printStackTrace();
                }
            } else {
                System.out.println("This is the first peerProcess");
                break;
            }

            line = s.nextLine();
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Client client = new Client("localhost", portNum);
        //client.setMessage("This was sent from peerProcess");

        //Thread cThread = new Thread(client);

        //cThread.start();
    }
    
    // Startes up the peerProcess and begins message delivery
    public static void main(String[] args) {
        peerProcess pp = new peerProcess(Integer.parseInt(args[0]));
        // pp.startProtocol();
    }
}