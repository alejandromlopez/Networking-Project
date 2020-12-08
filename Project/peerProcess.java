
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.nio.file.*;
import java.lang.Math;
import java.net.*;
import java.util.HashMap;

public class peerProcess {
    private final int peerID;
    private int numPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private short bitField;
    private boolean haveFile;
    private int numOfPieces;
    private int bitFieldSize;
    private int portNum;
    private ServerSocket server;
    public static HashMap<Integer, Socket> sockets = new HashMap<Integer, Socket>();
    private static HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();
    private HashMap<Integer, String> neighbors = new HashMap<Integer, String>();

    public peerProcess(int pID) {
        peerID = pID;
        initialize();
        computeNumberOfPiece();
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

        computeNumberOfPiece();
        bitField = new byte[bitFieldSize];
        
        String property = prop2.getProperty("" + peerID);
        String bit = property.split(" ")[2];

        portNum = Integer.parseInt(property.split(" ")[1]);
        
        if (bit.equals("1")) {
            int leftover = numOfPieces % 8;
            int byteNum = 0;
<<<<<<< HEAD
            bitField = (short)0b1111111111111111;
            System.out.println("bitField is: " + bitField);
            

            // for (int i = 0; leftover > i; i++) {
            //     byteNum += (int) Math.pow(2, 8 - i);
            // }

            // for (int i = 0; i < bitField.length; i++) {
            //     if (i == (bitField.length - 1)) {
            //         bitField[i] = (byte) byteNum;
            //         continue;
            //     }

            //     bitField[i] = (byte) 255;
            // }
            moveFile();
            haveFile = true;
        } else {
            bitField = 0;
=======
            for (int i = 1; i<=leftover; i++) {
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
            haveFile = true;
        } else {
            for (int i = 0; i < bitField.length; i++) {
                bitField[i] = (byte) 0;
            }
>>>>>>> 0a088199838e0212255f4ef1ead008f85a63b5fd
        }

        Scanner s = null;
        try {
            s = new Scanner(new File(file));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String line = "";
        do {
            line = s.nextLine();
            String[] fields = line.split(" ");
            int ID = Integer.parseInt(fields[0]);
            String address = fields[1];
            int port = Integer.parseInt(fields[2]);
            String blank = "";

            RemotePeerInfo rpi = new RemotePeerInfo(ID, address, port);
            peers.put(ID, rpi);

            if (peerID != ID) {
                neighbors.put(ID, blank);
            }

        } while (s.hasNext());
    }

    // Computes the number of Pieces of the given file
    private void computeNumberOfPiece() {
        double fSize = fileSize;
        double pSize = pieceSize;
        numOfPieces= (int) Math.ceil(fSize / pSize);
        int a = numOfPieces%8;
        if (a == 0)
            bitFieldSize = numOfPieces/8;
        else 
            bitFieldSize = (numOfPieces/8) + 1;
    }

    private void establishConnections() {
        Socket socket = null;
        Socket socket2 = null;
        String workingDir = System.getProperty("user.dir");

        Scanner s = null;
        try {
            s = new Scanner(new File(workingDir + "/PeerInfo.cfg"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            server = new ServerSocket(portNum);
        } catch (IOException io) {
            io.printStackTrace();
        }

        String line = "";

        do {
            line = s.nextLine();

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
                    // socket2 = new Socket(address, port);
                    System.out.println(peerID + ": Connection established with " + address + " " + ID);

                    Writer w = new Writer(new HandshakeMessage(peerID), socket, peerID, ID);
                    Thread t = new Thread(w);
                    t.start();

                    sockets.put(ID, socket);
                    neighbors.replace(ID, "sent");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("peerProcess " + peerID + " socket error: " + e);
                }
            } else {
                break;
            }
        } while (s.hasNext());

        Listener l = new Listener();
        Thread t = new Thread(l);
        t.start();
    }

    // Starts up the peerProcess and begins message delivery
    public static void main(String[] args) {
        // try {
        // System.setOut(new PrintStream(new FileOutputStream("log.txt")));
        // } catch (FileNotFoundException e) {
        // e.printStackTrace();
        // }

        peerProcess pp = new peerProcess(Integer.parseInt(args[0]));
        pp.establishConnections();
    }

    public class Listener implements Runnable {
        private boolean finish;
        private boolean handshakeDone;
        private boolean bitFieldSent;
        private ObjectInputStream in;

        public Listener() {

        }

        public void run() {
            finish = false;
            handshakeDone=false;
            bitFieldSent=false;
            int count = 0;
            

            while (!finish) {
                try {
                    boolean done = true;
                    for (String neighborState : neighbors.values()) {
                        if (neighborState.equals("received"))
                            continue;
                        else {
                            done = false;
                            //System.out.println(peerID + " " + neighbors.values());
                        }
                    }
    
                    if (done && !handshakeDone) {
                        System.out.println(peerID + " is done.");
<<<<<<< HEAD
                        if (haveFile) {
                            for (int pid : sockets.keySet()) {
                                // Bitfield b = new Bitfield(bitField, peerID);
                                // Writer bitfieldOut = new Writer(b, sockets.get(pid), peerID, pid);
                                // Thread wThread = new Thread(bitfieldOut);
                                // wThread.start();
=======
                        if (haveFile && !bitFieldSent) {
                            for (Socket sock : sockets.values()) {
                                Bitfield b = new Bitfield(bitField, peerID);
                                Writer bitfieldOut = new Writer(b, sock, peerID);
                                Thread wThread = new Thread(bitfieldOut);
                                wThread.start();
                                bitFieldSent=true;
>>>>>>> 0a088199838e0212255f4ef1ead008f85a63b5fd
                            }
                        }
                        handshakeDone=true;
                    }

                    //server.close();
                    //System.out.println(peerID + " is listening for messages");
                    Socket s = server.accept();
                    //System.out.println(peerID + "'s listener received a message");

                    in = new ObjectInputStream(s.getInputStream());
                    Object inMessage = in.readObject();

                    if (inMessage instanceof HandshakeMessage) {
                        HandshakeMessage handshake = (HandshakeMessage)inMessage;
                        int pid = handshake.getPeerID();

                        if (neighbors.get(pid).equals("sent")) {
                            System.out.println(peerID + " confirmed from " + pid);
                            neighbors.replace(pid, "sent", "received");
                            continue;
                        } else {
                            System.out.println(peerID + " received from " + pid + ". " + peerID + " will now send handshake back");
                        }
        
                        neighbors.replace(pid, "received");
        
                        String address = peers.get(pid).getAddress();
                        int port = peers.get(pid).getPort();
                        s.close();
        
                        s = new Socket(address, port);
                        sockets.put(pid, s);
                        Writer w = new Writer(new HandshakeMessage(peerID), s, peerID, pid);
                        Thread t = new Thread(w);
                        t.start();
                    } 
                    //Choke
                    else if (inMessage instanceof Choke) {
                        Choke c = (Choke)inMessage;
                    } 
                    //Unchoke
                    else if (inMessage instanceof Unchoke) {
                        Unchoke uc = (Unchoke)inMessage;
                    } 
                    //Interested
                    else if (inMessage instanceof Interested) {
                        Interested interested = (Interested)inMessage;
                        System.out.println(peerID + " has received an interested message from " + interested.getPID());
                        
                    } 
                    //Uninterested
                    else if (inMessage instanceof Uninterested) {
                        Uninterested uninterested = (Uninterested)inMessage;
                        System.out.println(peerID + " has received an uninterested message from " + uninterested.getPID());
<<<<<<< HEAD
                        String workingDir = System.getProperty("user.dir");
                        File dir = new File(workingDir + "/peer_" + " received uninterested");
                        dir.mkdir();
                        break;
=======
                        
>>>>>>> 0a088199838e0212255f4ef1ead008f85a63b5fd
                    } 
                    //Have
                    else if (inMessage instanceof Have) {
                        Have h = (Have)inMessage;
                    } 
                    //Bitfield
                    else if (inMessage instanceof Bitfield) {
                        Bitfield b = (Bitfield)inMessage;
                        boolean write = false;
<<<<<<< HEAD

                        // for (int i = 0; i < bitField.length;i++){
                        //     b.getBitfield()[i] = (byte) 1;
                        //     if(bitField[i] == b.getBitfield()[i])
                        //         continue;
                        //     else{
                        //         Interested interested = new Interested(peerID);
                        //         int pid = b.getPID();
                        //         Writer w = new Writer(interested, sockets.get(pid), peerID, pid);
                        //         Thread t = new Thread(w);
                        //         t.start();
                        //         write = true;
                        //         System.out.println(peerID + " has sent an interested message to " + b.getPID());
                        //     }
                        // }
=======
                        for (int i = 0; i < bitField.length;i++){
                            if(bitField[i] == b.getBitfield()[i])
                                continue;
                            else{
                                Interested interested = new Interested(peerID);
                                int pid = b.getPID();
                                Writer w = new Writer(interested, sockets.get(pid), peerID);
                                Thread t = new Thread(w);
                                t.start();
                                write = true;
                                System.out.println(peerID + " has sent an interested message to " + b.getPID());
                                break;
                            }
                        }
>>>>>>> 0a088199838e0212255f4ef1ead008f85a63b5fd

                        if (!write){
                            Uninterested uninterested = new Uninterested(peerID);
                            int pid = b.getPID();
                            Writer w = new Writer(uninterested, sockets.get(pid), peerID, pid);
                            Thread t = new Thread(w);
                            t.start();
                            System.out.println(peerID + " has sent an uninterested message to " + b.getPID());
                        }
                        //break;
                    } 
                    //Request
                    else if (inMessage instanceof Request) {
                        Request r = (Request)inMessage;
                    } 
                    //Piece
                    else if (inMessage instanceof Piece) {
                        Piece p = (Piece)inMessage;
                    } else {
                        System.out.println(peerID + "'s LISTENER DID NOT RECEIVE A MESSAGE THAT IS KNOWN!!!");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
                
        }
    }
}