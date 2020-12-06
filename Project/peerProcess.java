
import java.util.Properties;
import java.util.Scanner;
import java.io.*;
import java.nio.file.*;
import java.lang.Math;
import java.net.*;
import java.util.HashMap;
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
    private ServerSocket server;
    // private static HashMap<Integer, Listener> sockets;
    private static HashMap<Integer, Handler> sockets = new HashMap<Integer, Handler>();

    private ObjectInputStream in;

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
        // TODO: uncomment this
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

        // System.out.println(numPreferredNeighbors);
        // System.out.println(unchokingInterval);
        // System.out.println(optimisticUnchokingInterval);
        // System.out.println(fileName);
        // System.out.println(fileSize);
        // System.out.println(pieceSize);

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
        // Socket socket = null;

        // String workingDir = System.getProperty("user.dir");

        // Scanner s = null;
        // try {
        // s = new Scanner(new File(workingDir + "/PeerInfo.cfg"));
        // } catch (FileNotFoundException e) {
        // e.printStackTrace();
        // }

        // String line = s.nextLine();

        // while (s.hasNext()) {
        // String[] fields = line.split(" ");
        // int ID = Integer.parseInt(fields[0]);
        // String address = fields[1];
        // int port = Integer.parseInt(fields[2]);

        // /*
        // * Checks if this peerProcess is NOT the first one to run. If so, then
        // establish
        // * connections to the peerProcesses that came before.
        // */
        // if (ID != peerID) {
        // try {
        // socket = new Socket(address, port);
        // Listener l = new Listener(socket);
        // l.start();
        // sockets.put(ID, l);

        // File dir = new File(workingDir + "/peer_" + peerID + "_to_peer_" +
        // fields[0]);
        // dir.mkdir();

        // System.out.println("Connection established with " + address);
        // } catch (UnknownHostException e1) {
        // System.out.println("Unknown host: " + fields[1]);
        // e1.printStackTrace();
        // } catch (IOException e2) {
        // System.out.println("IOException at port " + fields[2]);
        // e2.printStackTrace();
        // }
        // } else {
        // break;
        // }

        // line = s.nextLine();
        // }

        // if (Integer.parseInt(line.split(" ")[0]) != peerID) {
        // try {
        // socket.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }

        String workingDir = System.getProperty("user.dir");
        Socket sOut = null;
        ObjectOutputStream oos = null;
        File dir = new File(workingDir);

        if (peerID == 1003) {
            try {
                sOut = new Socket("lin114-00.cise.ufl.edu", 9998);
                dir = new File(workingDir + "/1003");
                dir.mkdir();
                oos = new ObjectOutputStream(sOut.getOutputStream());
                oos.writeObject(new HandshakeMessage(1003));
                oos.flush();
            } catch (IOException e1) {
                e1.printStackTrace();
                dir = new File(workingDir + "/pp1_1003_" + e1);
                dir.mkdir();
            } finally {
                try {
                    sOut.close();
                    oos.close();
                    dir = new File(workingDir + "/1003_closed");
                    dir.mkdir();
                } catch (IOException e2) {
                    e2.printStackTrace();
                    dir = new File(workingDir + "/pp2_1003_" + e2);
                    dir.mkdir();
                }
            }
        }
    
        Socket s1 = null;
        int count = 0;

        try {
            server = new ServerSocket(portNum);
            dir = new File(workingDir + "/server_" + peerID);
            dir.mkdir();

            while (true) {
                if (peerID != 1001 && peerID != 1003) {
                    break;
                }
                if (count == 5) {
                    break;
                }

                s1 = server.accept();
                dir = new File(workingDir + "/accepted_in_" + peerID);
                dir.mkdir();

                new Handler(s1, peerID).start();

                count++;
            }
        } 
        // catch (IOException e1) {
        //     e1.printStackTrace();
        // } catch (ClassNotFoundException e2) {
        //     e2.printStackTrace();
        // } 
        catch (Exception e) {
            dir = new File(workingDir + "/pp_here1_" + peerID + "_" + e);
            dir.mkdir();
        } finally {
            try {
                in.close();
                s1.close();
            } catch (IOException e3) {
                e3.printStackTrace();
                dir = new File(workingDir + "/pp_here2_" + peerID + "_" + e3);
                dir.mkdir();
            }
        }

        // TODO: comment this out when the server stays listening
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
        // pp.startProtocol();
    }
}