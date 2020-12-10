
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.lang.Math;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;

public class peerProcess {
    private static int peerID;
    private static int numPreferredNeighbors;
    private static int unchokingInterval;
    private static int optimisticUnchokingInterval;
    private static int currentOptUnchoked;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private static byte[] bitField;
    private byte[] fullBitfield;
    private boolean haveFile;
    private int numOfPieces;
    private int bitfieldSize;
    private static int numLeftover;
    private static boolean areLeftovers;
    private boolean tester;
    private int portNum;
    private ServerSocket server;
    private static HashMap<Integer, Socket> sockets = new HashMap<Integer, Socket>();
    private static HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();
    private static HashMap<Integer, Integer> pieceData = new HashMap<Integer, Integer>();
    private HashMap<Integer, String> neighbors = new HashMap<Integer, String>();
    private HashMap<Integer, byte[]> peersBitfields = new HashMap<Integer, byte[]>();
    private static HashMap<Integer, Boolean> peersInterestedInMe = new HashMap<Integer, Boolean>();
    private static HashMap<Integer, Boolean> isChoke = new HashMap<Integer, Boolean>();
    private Set<Integer> requests = new HashSet<Integer>();
    private EventLog peerlog;
    private byte[][] pieces;
    private Timer timer = new Timer();
    private Timer timer2 = new Timer();

    public peerProcess(int pID) {
        peerID = pID;
        peerlog = new EventLog(pID);
        initialize();
        pieces = new byte[numOfPieces][pieceSize];
        encodeFile();
    }

    // Moves the file from the current working directory to the specified
    // peerProcess subdirectory
    private void moveFile() {
        String workingDir = System.getProperty("user.dir");
        Path source = new File(fileName).toPath();
        Path dest = new File(workingDir + "/peer_" + peerID + "/" + fileName).toPath();

        try {
            Files.copy(source, dest);
        } catch (FileAlreadyExistsException e1) {
            System.out.println("File is already in this subdirectory");
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    private void moveLog() {
        String workingDir = System.getProperty("user.dir");
        Path source = new File("log_peer_" + peerID + ".log").toPath();
        Path dest = new File(workingDir + "/peer_" + peerID + "/log_peer_" + peerID + ".log").toPath();

        try {
            Files.move(source, dest);
        } catch (FileAlreadyExistsException e1) {
            System.out.println("Log is already in this subdirectory");
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    private void encodeFile() {
        String workingDir = System.getProperty("user.dir");
        String path = workingDir + "/" + fileName;
        byte[] allFileBytes = null;

        // reads in the file and stores the contents in byte array allFileBytes
        try {
            File file = new File(path);
            InputStream is = new FileInputStream(file);
            allFileBytes = is.readAllBytes();

            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // splits up allFileBytes into pieceSize portions, and places them inside of
        // pieces
        for (int i = 0; i < pieces.length; i++) {
            byte[] piece = new byte[pieceSize];

            for (int j = 0; j < pieceSize; j++) {
                if (((i * pieceSize) + j) >= allFileBytes.length) {
                    break;
                }

                piece[j] = allFileBytes[(i * pieceSize) + j];
            }
            pieces[i] = piece;
        }
    }

    // Read PeerInfo.cfg and Common.cfg and set all necessary variables and read all
    // necessary data
    private void initialize() {
        String workingDir = System.getProperty("user.dir");

        // Creates the subdirectory for the peerProcess
        // Created logfile moves to subdirectory
        File dir = new File(workingDir + "/peer_" + peerID);
        dir.mkdir();
        moveLog();

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
        bitField = new byte[bitfieldSize];
        fullBitfield = new byte[bitfieldSize];

        String property = prop2.getProperty("" + peerID);
        String bitfieldBit = property.split(" ")[2];

        portNum = Integer.parseInt(property.split(" ")[1]);

        if (bitfieldBit.equals("1")) {
            int leftover = numOfPieces % 8;
            int byteNum = 0;
            for (int i = 1; i <= leftover; i++) {
                byteNum += (int) Math.pow(2, 8 - i);
            }

            for (int i = 0; i < bitField.length; i++) {
                if (areLeftovers && i == (bitField.length - 1)) {
                    bitField[i] = (byte) byteNum;
                    fullBitfield[i] = (byte) byteNum;
                    continue;
                }

                bitField[i] = (byte) 255;
                fullBitfield[i] = (byte) 255;
            }
            moveFile();
            haveFile = true;
        } else {
            for (int i = 0; i < bitField.length; i++) {
                bitField[i] = (byte) 0;
            }
            // Calculate FullBitField
            int leftover = numOfPieces % 8;
            int byteNum = 0;
            for (int i = 1; i <= leftover; i++) {
                byteNum += (int) Math.pow(2, 8 - i);
            }

            for (int i = 0; i < fullBitfield.length; i++) {
                if (areLeftovers && i == (fullBitfield.length - 1)) {
                    fullBitfield[i] = (byte) byteNum;
                    continue;
                }

                fullBitfield[i] = (byte) 255;
            }
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

            RemotePeerInfo rpi = new RemotePeerInfo(ID, address, port);
            peers.put(ID, rpi);
            byte[] zero = new byte[bitfieldSize];

            if (peerID != ID) {
                peersInterestedInMe.put(ID, false);
                peersBitfields.put(ID, zero);
            }
            pieceData.put(ID, 0);

            if (peerID != ID) {
                neighbors.put(ID, "");
            }

        } while (s.hasNext());
    }

    // Computes the number of Pieces of the given file
    private void computeNumberOfPiece() {
        double fSize = fileSize;
        double pSize = pieceSize;
        numOfPieces = (int) Math.ceil(fSize / pSize);
        int a = numOfPieces % 8;
        if (a == 0) {
            bitfieldSize = numOfPieces / 8;
        } else {
            bitfieldSize = (numOfPieces / 8) + 1;
            areLeftovers = true;
            numLeftover = a;
        }
    }

    // Establishes sockets with all other remote machines and spawns Handler threads
    // for each
    private void establishConnections() {
        Socket socket = null;
        String workingDir = System.getProperty("user.dir");

        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(workingDir + "/PeerInfo.cfg"));
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
            line = scanner.nextLine();

            String[] fields = line.split(" ");
            int ID = Integer.parseInt(fields[0]);
            String address = fields[1];
            int port = Integer.parseInt(fields[2]);

            /*
             * Establishes connections with all peerProcesses that were created before this
             * one
             */
            if (ID != peerID) {
                try {
                    socket = new Socket(address, port);
                    System.out.println(peerID + ": Connection established with " + address + " " + ID);
                    peerlog.TCPConnectionTo(ID);

                    // Writer w = new Writer(new HandshakeMessage(peerID), socket, peerID);// , ID);
                    // Thread t = new Thread(w);
                    // t.start();

                    sockets.put(ID, socket);
                    // neighbors.replace(ID, "sent");

                    Handler beforeHandler = new Handler(peers.get(ID), false);
                    Thread beforeThread = new Thread(beforeHandler);
                    beforeThread.start();

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("peerProcess " + peerID + " socket error: " + e);
                }
            } else {
                break;
            }
        } while (scanner.hasNext());

        /*
         * Establishes connections with all peerProcesses that come after this one
         * Listens for incoming Handshake messages, then creates a socket associated
         * with that peerProcess, and spawns a Handler thread for this socket
         */
        boolean done = false;
        int IDCounter = 1;
        while (!done) {
            Socket s = null;
            try {
                s = server.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // obtains the hostname of the incoming socket and resolves the peerID
            // associated with it
            String inAddress = s.getInetAddress().getHostName();
            // System.out.println(s.getRemoteSocketAddress());
            int inPeerID = peerID + IDCounter;
            IDCounter++;
            // for (RemotePeerInfo rpi : peers.values()) {
            // if (inAddress.equals(rpi.getAddress())) {
            // inPeerID = rpi.getPeerID();
            // }
            // }

            /*
             * Checks if this peerProcess has already created a socket to this incoming
             * message If it hasn't, then creates a socket with this connection, places it
             * into the HashMap "sockets", and spawns a Handler for it
             */
            if (sockets.containsKey(inPeerID)) {
                continue;
            } else {
                sockets.put(inPeerID, s);
                neighbors.put(inPeerID, "received");
                Handler afterHandler = new Handler(peers.get(inPeerID), true);
                Thread afterThread = new Thread(afterHandler);
                afterThread.start();
            }

            for (String state : neighbors.values()) {
                if (state.equals("sent") || state.equals("")) {
                    done = false;
                    break;
                }
                done = true;
                continue;
            }
        }
    }

    public boolean getTester() {
        return tester;
    }

    public HashMap<Integer, Integer> getPieceData() {
        return pieceData;
    }

    public HashMap<Integer, Boolean> getPeersInterestedInMe() {
        return peersInterestedInMe;
    }

    public HashMap<Integer, Socket> getSockets() {
        return sockets;
    }

    public int getCurrentOptUnchoked() {
        return currentOptUnchoked;
    }

    public byte[] getBitField() {
        return bitField;
    }

    public void setPieceData(HashMap<Integer, Integer> p) {
        pieceData = p;
    }

    public void setInterestedInMe(HashMap<Integer, Boolean> i) {
        peersInterestedInMe = i;
    }

    public void setCurrentOptUnchoked(int pid) {
        currentOptUnchoked = pid;
    }

    public void setIsChoke(HashMap<Integer, Boolean> choking) {
        isChoke = choking;
    }

    public static byte[] update(byte[] bfield, int pieceIdx) {
        int idx = (pieceIdx / 8);
        int byteToInt = bfield[idx];
        int pow = pieceIdx % 8;
        byteToInt += (int) Math.pow(2, 7 - pow);
        bfield[idx] = (byte) byteToInt;
        return bfield;
    }

    // Starts up the peerProcess and begins message delivery
    public static void main(String[] args) {

        peerProcess pp = new peerProcess(Integer.parseInt(args[0]));

        // create sockets with all peers that came before us
        pp.establishConnections();
        // Handler l = pp.new Handler(pp);
        // Thread t = new Thread(l);
        // t.start();
    }

    public class Handler implements Runnable {
        private Socket socket;
        private int remotePeerID;
        private RemotePeerInfo remotePeer;
        private Message outMessage;
        private byte[] inMessage;
        private boolean receivedHandshake;
        private ObjectInputStream in = null;
        private ObjectOutputStream out = null;
        private boolean sendHandshake = true;

        // private Message message;
        private boolean finish;
        private boolean handshakeDone;
        private boolean bitFieldSent;
        // private ObjectInputStream in;
        private peerProcess pp;
        private HashMap<Integer, Boolean> isChockedBy = new HashMap<Integer, Boolean>();
        private HashMap<Integer, Boolean> containsInterestingPieces = new HashMap<Integer, Boolean>();

        public Handler(RemotePeerInfo remoteP, boolean rcvdHandshake) {
            remotePeer = remoteP;
            remotePeerID = remotePeer.getPeerID();
            socket = sockets.get(remotePeerID);
            receivedHandshake = rcvdHandshake;
            try {
                System.out.println("Before Streams");
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("After Streams");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.out.println(e);
                e.printStackTrace();
            }
        }

        public void run() {
            // finish = false;
            // handshakeDone = false;
            // bitFieldSent = false;

            // int count = 0;
            Reader reader = new Reader();
            Thread rThread = new Thread(reader);
            rThread.start();
            
            Writer_temp writer = new Writer_temp();
            Thread wThread = new Thread(writer);
            wThread.start();
            while (finish) {
                try {
                    // sends out Handshake message to remote peer associated with this socket
                    

                    

                    ///////////////////////////////////////////////////////////////////////
                    // to test the new implementation, comment out the rest of this code //
                    ///////////////////////////////////////////////////////////////////////
                    
                    // in = new ObjectInputStream(s.getInputStream()); //
                    // Object inMessage = in.readObject(); //
                    // String inAddress = s.getInetAddress().getHostName();
                    // int inPeerID = 0;
                    // byte[] inBitfield = null;

                    // if (peersBitfields.containsKey(remotePeerID)) {
                    //     for (int i = 0; i < inBitfield.length; i++) {
                    //         if (inBitfield[i] == 1 && bitField[i] == 0) {
                    //             containsInterestingPieces.put(remotePeerID, true);
                    //         }
                    //     }
                    // }

                    // // Choke
                    // else if (inMessage[5] == (byte) 0) {
                    //     isChockedBy.put(remotePeerID, true);
                    //     peerlog.choking(remotePeerID);
                    // }
                    /*
                    // Unchoke
                    else if (inMessage[5] == (byte) 1) {
                        inBitfield = peersBitfields.get(remotePeerID);
                        isChockedBy.put(remotePeerID, false);
                        peerlog.unchoking(remotePeerID);

                        for (int i = 0; i < bitField.length; i++) {
                            byte mask = 1;
                            for (int j = 0; j < 8; j++) {
                                byte myBit = (byte) ((bitField[i] >> (7 - j)) & mask);
                                byte inBit = (byte) ((inBitfield[i] >> (7 - j)) & mask);

                                if (myBit == inBit) {
                                    continue;
                                } else if (myBit == 0 && inBit == 1) {
                                    int pieceIdx = (8 * i) + j;

                                    if (!requests.contains(pieceIdx)) {
                                        requests.add(pieceIdx);

                                        Wrter w = new Writer(new Request(bitField, pieceIdx, peerID),
                                                sockets.get(uc.getPID()), peerID);
                                        Thread t = new Thread(w);
                                        t.start();
                                        System.out.println("Sent request from an unchoke method and my code is shit");
                                    }
                                }
                            }

                        }
                    }
                    // Interested
                    else if (inMessage[5] == (byte) 2) {
                        System.out.println(peerID + " has received an interested message from " + remotePeerID);
                        peersInterestedInMe.replace(remotePeerID, true);
                        peerlog.receivingInterested(remotePeerID);
                    }
                    // Uninterested
                    else if (inMessage[5] == (byte) 3) {
                        System.out.println(
                                peerID + " has received an uninterested message from " + remotePeerID);
                        peerlog.receivingNotInterested(remotePeerID);
                    }
                    // Have
                    else if (inMessage[5] == (byte) 4) {
                        peerlog.receivingHave(remotePeerID, h.getPieceIdx());
                        boolean write = false;
                        for (int i = 0; i < bitField.length; i++) {
                            byte mask = 1;

                            for (int j = 0; j < 8; j++) {
                                byte myBit = (byte) ((bitField[i] >> (7 - j)) & mask);
                                byte inBit = (byte) ((h.getBitfield()[i] >> (7 - j)) & mask);

                                if (myBit == inBit) {
                                    continue;
                                } else if (myBit == 0 && inBit == 1) {
                                    Interested interested = new Interested(peerID);
                                    int pid = remotePeerID;

                                    Wrier w = new Writer(interested, sockets.get(pid), peerID);
                                    Thread t = new Thread(w);
                                    t.start();

                                    write = true;
                                    System.out.println(peerID + " has sent an interested message to " + h.getPID());
                                    break;
                                }
                            }
                        }

                        if (!write) {
                            Uninterested uninterested = new Uninterested(peerID);
                            int pid = h.getPID();

                            Wrier w = new Writer(uninterested, sockets.get(pid), peerID);
                            Thread t = new Thread(w);
                            t.start();

                            System.out.println(peerID + " has sent an uninterested message to " + h.getPID());
                        }

                        // Updating the peersBitfield hashmap everytime a have message is received.
                        if (peersBitfields.containsKey(remotePeerID)) {
                            peersBitfields.replace(remotePeerID, h.getBitfield());
                        } else {
                            peersBitfields.put(remotePeerID, h.getBitfield());
                        }
                    }
                    // Bitfield
                    else if (inMessage[5] == (byte) 5) {
                        boolean write = false;

                        for (int i = 0; i < bitField.length; i++) {
                            byte mask = 1;

                            for (int j = 0; j < 8; j++) {
                                byte myBit = (byte) ((bitField[i] >> (7 - j)) & mask);
                                byte inBit = (byte) ((b.getBitfield()[i] >> (7 - j)) & mask);

                                if (myBit == inBit) {
                                    continue;
                                } else if (myBit == 0 && inBit == 1) {
                                    Interested interested = new Interested(peerID);
                                    int pid = remotePeerID;

                                    Wrter w = new Writer(interested, sockets.get(pid), peerID);
                                    Thread t = new Thread(w);
                                    t.start();

                                    write = true;
                                    System.out.println(peerID + " has sent an interested message to " + b.getPID());
                                    break;
                                }
                            }
                        }

                        if (!write) {
                            Uninterested uninterested = new Uninterested(peerID);
                            int pid = b.getPID();
                            Wrier w = new Writer(uninterested, sockets.get(pid), peerID);// , pid);
                            Thread t = new Thread(w);
                            t.start();
                            System.out.println(peerID + " has sent an uninterested message to " + b.getPID());
                        }

                        if (peersBitfields.containsKey(remotePeerID)) {
                            peersBitfields.replace(remotePeerID, b.getBitfield());
                        } else
                            peersBitfields.put(remotePeerID, b.getBitfield());
                    }
                    // Request
                    else if (inMessage[5] == (byte) 6) {
                        int p = r.getPieceIdx();

                        Wrter w = new Writer(new Piece(bitField, pieces[p], peerID, r.getPieceIdx()),
                                sockets.get(remotePeerID), peerID);
                        Thread t = new Thread(w);
                        t.start();
                    }
                    // Piece
                    else if (inMessage[5] == (byte) 7) {
                        inBitfield = peersBitfields.get(remotePeerID);
                        pieces[p.getPieceID()] = p.getPiece();

                        // Used later in newNeighbors to calc rates
                        int temp = pieceData.get(remotePeerID);
                        pieceData.replace(remotePeerID, (temp + 1));
                        int totalPieces = 0;
                        for (int i = 0; i < bitField.length; i++) {
                            byte mask = 1;

                            for (int j = 0; j < 8; j++) {
                                byte myBit = (byte) ((bitField[i] >> (7 - j)) & mask);
                                byte fBit = (byte) ((fullBitfield[i] >> (7 - j)) & mask);
                                if (myBit == 1 && fBit == 1)
                                    totalPieces++;
                            }
                        }

                        peerlog.downloadingAPiece(remotePeerID, p.getPieceID(), totalPieces);

                        bitField = update(bitField, p.getPieceID());

                        boolean toBreak = false;
                        for (int i = 0; i < bitField.length; i++) {
                            byte mask = 1;

                            for (int j = 0; j < 8; j++) {
                                byte myBit = (byte) ((bitField[i] >> (7 - j)) & mask);
                                byte inBit = (byte) ((inBitfield[i] >> (7 - j)) & mask);

                                if (myBit == inBit) {
                                    continue;
                                } else if (myBit == 0 && inBit == 1) {
                                    int pieceIdx = (8 * i) + j;

                                    if (!requests.contains(pieceIdx)) {
                                        requests.add(pieceIdx);

                                        if (isChockedBy.get(remotePeerID) || !containsInterestingPieces.get(remotePeerID)) {
                                            continue;
                                        }

                                        Wrter w = new Writer(new Request(bitField, pieceIdx, peerID),
                                                sockets.get(p.getPID()), peerID);
                                        Thread t = new Thread(w);
                                        t.start();
                                    }
                                }
                            }
                        }

                        for (Socket sock : sockets.values()) {
                            Wrier w = new Writer(new Have(bitField, p.getPieceID(), peerID), sock, peerID);
                            Thread t = new Thread(w);
                            t.start();
                        }
                    } else {
                        System.out.println(peerID + "'s LISTENER DID NOT RECEIVE A MESSAGE THAT IS KNOWN!!!");
                    }
                    */
                } catch (Exception e) {
                    e.printStackTrace();
                }
                /*
                // Checks if this peer is done
                // boolean areEqual = true;
                // for (int i = 0; i < bitField.length; i++) {
                //     if (bitField[i] == fullBitfield[i]) {
                //         System.out.println("equal byte in bitfield");
                //         continue;
                //     }
                //     System.out.println("not equal");
                //     areEqual = false;
                // }
                // if (areEqual && count == 0) {
                //     peerlog.CompletionOfDownload();
                //     count++;
                //     System.out.println("completed");
                // }

                // // Checks to see if all the peers have all of the pieces
                // boolean allDone = true;
                // for (byte[] b : peersBitfields.values()) {
                //     System.out.println("this code is shit?");
                //     for (byte by : b) {
                //         System.out.print(by + " ");
                //     }
                //     for (int i = 0; i < bitField.length; i++) {
                //         // byte mask = 1;

                //         if (b[i] == fullBitfield[i]) {
                //             continue;
                //         } else {
                //             allDone = false;
                //             break;
                //         }
                //     }
                //     if (!allDone) {
                //         System.out.println("not done");
                //         break;
                //     }
                // }

                // if (allDone && areEqual) {
                //     System.out.println("should quit");
                //     timer.cancel();
                //     timer2.cancel();
                //     break;
                // }
                // for (byte b : bitField) {
                //     System.out.println("bitField: " + b);
                // }*/
            }

            peerlog.closeLogger();
        
        }

        public TimerTask prefNeighbors() {
            TimerTask a = new TimerTask() {
                public void run() {
                    new newNeighbors(numPreferredNeighbors, unchokingInterval, peers, bitField, peerID, areLeftovers,
                            numLeftover, pp, peerlog);
                }
            };
            return a;
        }
        
        public class Reader implements Runnable {
            private ObjectInputStream in = null;

            public Reader() {
            }

            /*
             * Questions to ask Brandon:
             * 
             * 1. The peerProcess should manually create sockets for peers that came before,
             * then pass those sockets off to a Handler thread 2. The peerProcess should run
             * server.accept() to listen for Handshakes from peerProcesses that came after,
             * create a socket for this new connection, and pass it off to a Handler thread
             * 3. Each Handler is going to maintain a Reader and Writer thread to manage
             * communication over the socket 4. The Writer knows that it must write to the
             * socket's output stream since that goes to the address and port of the other
             * process, as specified in the PeerInfo.cfg file 5. The Reader, however, cannot
             * simply read from this same socket. This socket is connected to the address
             * and port that the other peerProcess is listening on (as specified in the
             * PeerInfo.cfg file), but we do not have any information about what port the
             * information actually LEAVES from the remote peerProcess.
             * 
             * How do we do we know where to listen in on to read incoming messages? Should
             * the handler maintain 2 sockets? (1 for sending data to the remote machine,
             * and 1 for knowing where to read information from)?
             */

            public void run() {
                // takes care of the handshake portion of the protocol when it comes from below
                while(true){
                    try {
                        // opens the input stream for the socket
                        //in = new ObjectInputStream(socket.getInputStream());

                        // extract the incoming Handshake, in the case that this handler is interacting
                        // with a socket
                        // that came before
                        if(in.available()>0){
                            if (!receivedHandshake) {
                                extractMessage();
                            } else
                            {

                            // once the above if statement is completed, we can assert that the handshake
                            // has been received
                            
                            // extracts the length of the incoming message
                            byte[] lenBuf = new byte[4];
                            in.read(lenBuf, 0, 4);
                            int length = ByteBuffer.wrap(lenBuf).getInt();

                            // extracts the message type of the incoming message
                            byte[] messageType = new byte[1];
                            in.read(messageType, 0, 1);

                            // extracts the payload of the incoming message
                            byte[] payload = new byte[length];
                            in.read(payload, 0, length);
                            // inMessage = new byte[5 + length];
                            // for (int i = 0; i < 5 + length; i++) {
                            //     if (i < 4) {
                            //         inMessage[i] = lenBuf[i];
                            //     } else if (i == 4 ){
                            //         inMessage[i] = messageType[i];
                            //     } else {
                            //         inMessage[i] = payload[i - 5];
                            //     }
                            // }

                            //Choke
                            if (messageType[0] == (byte) 0) {
                                isChockedBy.put(remotePeerID, true);
                                peerlog.choking(remotePeerID);
                            }

                        }
                    }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void extractMessage() {
                // this should take in the handshake, verify the header is correct and the
                // peerID is the expected one
                // then run neighbors.put(remotePeerID, "received")
                // else break?
                try {
                    System.out.println("Here in handshakereader");

                    boolean correctHeader = false;
                    byte[] header = new byte[18];
                    in.read(header, 0, 18);
                    String head = ByteBuffer.wrap(header).toString();

                    if (head.equals("P2PFILESHARINGPROJ"))
                        correctHeader = true;

                    byte[] zeros = new byte[10];
                    in.read(zeros, 0, 10);

                    boolean correctID = false;
                    byte[] pID = new byte[4];
                    in.read(pID, 0, 4);
                    int mPID = ByteBuffer.wrap(pID).getInt();

                    if (remotePeerID == mPID)
                        correctID = true;

                    System.out.println("correctHeader is : " + correctHeader + " and correctID is : " + correctID);
                    if (correctHeader & correctID){
                        receivedHandshake = true;
                        neighbors.replace(remotePeerID, "received");
                        System.out.println("Wrote handshake out to " + remotePeerID);
                    }
                } catch (IOException e) {
                    
                    e.printStackTrace();
                }
            }
        }

        public class Writer_temp implements Runnable {
            private HandshakeMessage handshakeMessage = new HandshakeMessage(peerID);
            //private ObjectOutputStream out;
            private byte[] outMessageBytes;

            public Writer_temp() {}

            public void run() {
                try {
                    //out = new ObjectOutputStream(socket.getOutputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("Output stream created");

                //sendHandshake = true;
                sendMessage();

                while (!receivedHandshake) {
                    //System.out.println("Waiting for handshake from " + remotePeerID);
                }
                
                // proceed to send out bitfield message
            }

            // converts the outgoing message to byte array
            void convertMessageToBytes() {
                int ourPeerID = peerID;
                int mask = 0xff000000;

                // checks if the mesage to be converted is the handshake message
                if (sendHandshake) {
                    outMessageBytes = new byte[32];

                    // converts the handshake message to byte array to be sent out
                    for (int i = 0; i < outMessageBytes.length; i++) {
                        if (i < 18) {
                            outMessageBytes[i] = (byte) handshakeMessage.getHandshakeHeader().charAt(i);
                            //System.out.println("Writing our handshake header to send to " + remotePeerID);
                        } else if (i >= 18 && i < 28) {
                            outMessageBytes[i] = 0;
                            //System.out.println("Writing our handshake zero bits to send to " + remotePeerID);
                        } else {
                            outMessageBytes[i] = (byte) (ourPeerID & mask);
                            //System.out.println("Writing our handshake peerID to send to" + remotePeerID);
                            ourPeerID <<= 8;
                        }
                    }

                    // this is to prevent future calls to this function from writing out another handshake message
                    sendHandshake = false;
                    System.out.println("Wrote handshake out to " + remotePeerID);
                }
                // checks if the message to be converted is a non-handshake message
                else {
                    // int len = outMessage.getMLength();
                    // byte[] payload = outMessage.getMPayload();
                    // outMessageBytes = new byte[5 + len];

                    // // converts the outgoing message to byte array to be sent out
                    // for (int i = 0; i < 5 + len; i++) {
                    //     if (i < 4) {
                    //         outMessageBytes[i] = (byte) (len & mask);
                    //         len <<= 8;
                    //         System.out.println("Writing our message length to send to " + remotePeerID);
                    //     } else if (i == 4) {
                    //         outMessageBytes[i] = outMessage.getMType();
                    //         System.out.println("Writing our message type to send to " + remotePeerID);
                    //     } else {
                    //         outMessageBytes[i] = payload[i - 5];
                    //         System.out.println("Writing our message payload to send to " + remotePeerID);
                    //     }
                    // }
                    // System.out.println("Wrote message out to " + remotePeerID);
                }
            }

            private void sendMessage() {
                convertMessageToBytes();

                try {
                    out.write(outMessageBytes);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } 
        }
    }
}