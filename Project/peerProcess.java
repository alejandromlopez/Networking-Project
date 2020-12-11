
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.Math;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class peerProcess {
    private ServerSocket server;
    private static int peerID;
    private int portNum;
    private static int numPreferredNeighbors;
    private static int unchokingInterval;
    private static int optimisticUnchokingInterval;
    private static int currentOptUnchoked;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private int numOfPieces;
    private static byte[] bitfield;
    private boolean protocolCompleted = false;
    private static HashMap<Integer, Socket> sockets = new HashMap<Integer, Socket>();
    private static HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();
    private static HashMap<Integer, Thread> threads = new HashMap<Integer, Thread>();
    private static HashMap<Integer, String> neighbors = new HashMap<Integer, String>();
    private static HashMap<Integer, Message> messagesToSend = new HashMap<Integer, Message>();
    private static EventLog peerlog;
    private static int numLeftover;
    private static boolean areLeftovers;
    private int bitfieldSize;
    private byte[] fullBitfield;
    private static HashMap<Integer, Integer> pieceData = new HashMap<Integer, Integer>();
    private static HashMap<Integer, byte[]> peersBitfields = new HashMap<Integer, byte[]>();
    private static HashMap<Integer, Boolean> peersInterestedInMe = new HashMap<Integer, Boolean>();
    private static HashMap<Integer, Boolean> isChoke = new HashMap<Integer, Boolean>();
    private Set<Integer> requests = new HashSet<Integer>();
    private byte[][] pieces;
    private static Timer timer = new Timer();
    private static Timer timer2 = new Timer();

    private boolean haveFile;

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

    // reads in the file and converts it into bytes
    // these bytes are then separated into pieces and stored in the array "pieces"
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

    // Read PeerInfo.cfg and Common.cfg and set all necessary variables and read all necessary data
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
        bitfield = new byte[bitfieldSize];
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

            for (int i = 0; i < bitfield.length; i++) {
                if (areLeftovers && i == (bitfield.length - 1)) {
                    bitfield[i] = (byte) byteNum;
                    fullBitfield[i] = (byte) byteNum;
                    continue;
                }

                bitfield[i] = (byte) 255;
                fullBitfield[i] = (byte) 255;
            }
            moveFile();
            haveFile = true;
        } else {
            for (int i = 0; i < bitfield.length; i++) {
                bitfield[i] = (byte) 0;
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

    // Establishes sockets with all other remote machines and spawns Handler threads for each
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

        // reads the PeerInfo.cfg file to establish connections with the remote machines that preceded this one
        do {
            line = scanner.nextLine();

            String[] fields = line.split(" ");
            int ID = Integer.parseInt(fields[0]);
            String address = fields[1];
            int port = Integer.parseInt(fields[2]);

            // Establishes connections with all peerProcesses that were created before this one
            if (ID != peerID) {
                try {
                    // creates the socket with this remote peer and writes it to the log
                    socket = new Socket(address, port);
                    System.out.println(peerID + ": Connection established with " + address + " " + ID);
                    peerlog.TCPConnectionTo(ID);

                    // stores this socket in the HashMap "sockets"
                    sockets.put(ID, socket);

                    // spawns a Handler thread for this socket
                    Handler beforeHandler = new Handler(peers.get(ID), false);
                    Thread beforeThread = new Thread(beforeHandler);
                    threads.put(ID, beforeThread);
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

        // TODO: look into simplifying this / fixing it
        while (!done) {
            for (String state : neighbors.values()) {
                if (state.equals("sent") || state.equals("")) {
                    done = false;
                    break;
                }
                done = true;
            }

            if (done) {
                break;
            }

            Socket s = null;
            try {
                s = server.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // obtains the hostname of the incoming socket and resolves the peerID associated with it
            String inAddress = s.getInetAddress().getHostName();
            // System.out.println(s.getRemoteSocketAddress());
            int inPeerID = peerID + IDCounter;
            IDCounter++;
            System.out.println("Created socket with " + inPeerID);
            // for (RemotePeerInfo rpi : peers.values()) {
            // if (inAddress.equals(rpi.getAddress())) {
            // inPeerID = rpi.getPeerID();
            // }
            // }

            /*
             * Checks if this peerProcess has already created a socket to this incoming
             * message If it hasn't, then creates a socket with this connection, places it
             * into the HashMap "sockets", and spawns a Handler thread for it
             */
            if (sockets.containsKey(inPeerID)) {
                System.out.println("Already created socket with " + inPeerID);
                continue;
            } else {
                sockets.put(inPeerID, s);
                neighbors.put(inPeerID, "received");
                Handler afterHandler = new Handler(peers.get(inPeerID), true);
                Thread afterThread = new Thread(afterHandler);
                threads.put(inPeerID, afterThread);
                afterThread.start();
            }
        }
    }

    public static byte[] update(byte[] bfield, int pieceIdx) {
        int idx = (pieceIdx / 8);
        int byteToInt = bfield[idx];
        int pow = pieceIdx % 8;
        byteToInt += (int) Math.pow(2, 7 - pow);
        bfield[idx] = (byte) byteToInt;
        return bfield;
    }

    public static synchronized HashMap<Integer, Socket> getSockets() {
        return sockets;
    }

    public static synchronized byte[] getBitField() {
        return bitfield;
    }

    public static synchronized HashMap<Integer, Integer> getPieceData() {
        return pieceData;
    }

    public static synchronized void setPieceData(HashMap<Integer, Integer> p) {
        pieceData = p;
    }

    public static synchronized byte[] getPeersBitfields(int remotePID) {
        return peersBitfields.get(remotePID);
    }

    public static synchronized void setPeersBitfields(int remotePID, byte[] newBitfield) {
        peersBitfields.put(remotePID, newBitfield);
    }

    private synchronized boolean getProtocolCompleted() {
        return protocolCompleted;
    }

    private synchronized void setProtocolCompleted(boolean state) {
        protocolCompleted = state;
    }

    private static synchronized HashMap<Integer, Message> getMessagesToSend(){
        return messagesToSend;
    }

    private static synchronized void setMessagesToSend(int rPID, Message message){
        messagesToSend.put(rPID, message);
    }

    public static HashMap<Integer, Boolean> getPeersInterestedInMe() {
        return peersInterestedInMe;
    }

    public static int getCurrentOptUnchoked() {
        return currentOptUnchoked;
    }
    public static void setInterestedInMe(HashMap<Integer, Boolean> i) {
        peersInterestedInMe = i;
    }

    public static void setCurrentOptUnchoked(int pid) {
        currentOptUnchoked = pid;
    }

    public static void setIsChoke(HashMap<Integer, Boolean> choking) {
        isChoke = choking;
    }

    // Starts up the peerProcess and begins message delivery
    public static void main(String[] args) {
        peerProcess pp = new peerProcess(Integer.parseInt(args[0]));

        // create sockets with all peers that came before us
        pp.establishConnections();

        timer.schedule(new newNeighbors(), 0, unchokingInterval * 1000);
        timer2.schedule(new Optimistically(), 0, optimisticUnchokingInterval*1000);

        System.out.println("About to end peerProcess");
        for (Thread thread: threads.values()) {
            try {
                thread.join();
                System.out.println("Successfully closed a Handler");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Threads have all been closed");
        // System.exit(0);

        // byte[] temp = {0, 0, 1, 1};
        // System.out.print("temp is: ");
        // for (byte b: temp) {
        //     System.out.print(b + " ");
        // }
        // int i = ByteBuffer.wrap(temp).getInt();
        // System.out.println("\nthe integer is: " + i);

        // ByteBuffer unpack = ByteBuffer.allocate(4);
        // unpack.putInt(i);
        // byte[] bytes = unpack.array();
        // System.out.print("bytes is: ");
        // for (byte b: bytes) {
        //     System.out.print(b + " ");
        // }
    }

    public class Handler implements Runnable {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private int remotePeerID;
        private RemotePeerInfo remotePeer;
        private Message outMessage;
        private Message inMessage;
        private byte[] inMessageBytes;
        private boolean receivedHandshake;
        private boolean sendHandshake = true;
        private byte[] remoteBitfield = new byte[bitfield.length];

        private boolean finish;
        private boolean handshakeDone;
        private boolean bitFieldSent;
        private peerProcess pp;
        private HashMap<Integer, Boolean> isChockedBy = new HashMap<Integer, Boolean>();
        private HashMap<Integer, Boolean> containsInterestingPieces = new HashMap<Integer, Boolean>();

        public Handler(RemotePeerInfo remoteP, boolean rcvdHandshake) {
            remotePeer = remoteP;
            remotePeerID = remotePeer.getPeerID();
            socket = getSockets().get(remotePeerID);
            receivedHandshake = rcvdHandshake;
            System.out.println("Handler has received handshake from " + remotePeerID + ": " + receivedHandshake);
        }

        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Output stream created");
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Input stream created");
            } catch (IOException e) {
                System.out.println(e);
                e.printStackTrace();
            }
            finish = false;
            // handshakeDone = false;
            // bitFieldSent = false;

            // int count = 0;
            Writer_temp writer = new Writer_temp();
            Thread wThread = new Thread(writer);
            wThread.start();

            Reader reader = new Reader();
            Thread rThread = new Thread(reader);
            rThread.start();



            try {
                rThread.join();
                wThread.join();
                timer.cancel();
                timer2.cancel();
                System.out.println("Reader and Writer for " + remotePeerID + " have ended");
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            try {
                out.close();
                in.close();
                //socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            //while (false) {
              //  try {
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
                /*} catch (Exception e) {
                    e.printStackTrace();
                }*/
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

                // if (allDone && areEqual) { //////////////////////////////////////////////////////////////////////////////////////
                //     System.out.println("should quit");///////////////////////////////////////////////////////////////////////////
                //     timer.cancel();/////////////////////NEED THIS TO STOP THE CHOKING AND UNCHOKING INTERVALS////////////////////
                //     timer2.cancel();/////////////////////////////////////////////////////////////////////////////////////////////
                //     break;
                // }
                // for (byte b : bitField) {
                //     System.out.println("bitField: " + b);
                // }*/
            //}

            //peerlog.closeLogger();
        
        }

        private synchronized boolean getReceivedHandshake() {
            return receivedHandshake;
        }

        private synchronized void setReceivedHandshake(boolean state) {
            receivedHandshake = state;
        }

        private synchronized Message getOutMessage() {
            return outMessage;
        }

        private synchronized void setOutMessage(Message m) {
            outMessage = m;
        }
        
        public class Reader implements Runnable {
            public Reader() {
            }

            public void run() {
                // takes care of the handshake portion of the protocol when it comes from below
                while(!getProtocolCompleted()){
                    try {
                        // extract the incoming Handshake, in the case that this handler is interacting
                        // with a socket that came before
                        if (in.available() > 0) {
                            if (!getReceivedHandshake()) {
                                extractHandshake();
                            } else {
                                // once the above if statement is completed, we can assert that the handshake
                                // has been received
                                
                                // extracts the length of the incoming message
                                byte[] lenBuf = new byte[4];
                                in.read(lenBuf, 0, 4);
                                int length = ByteBuffer.wrap(lenBuf).getInt();

                                // extracts the message type of the incoming message
                                byte[] messageTypeBuf = new byte[1];
                                in.read(messageTypeBuf, 0, 1);
                                byte messageType = messageTypeBuf[0];

                                // extracts the payload of the incoming message
                                byte[] payload = new byte[length - 1];
                                in.read(payload, 0, length - 1);
                                inMessageBytes = new byte[4 + length];

                                for (int i = 0; i < 4 + length; i++) {
                                    if (i < 4) {
                                        inMessageBytes[i] = lenBuf[i];
                                    } else if (i == 4 ){
                                        inMessageBytes[i] = messageType;
                                    } else {
                                        inMessageBytes[i] = payload[i - 5];
                                    }
                                }

                                switch (messageType) {
                                    // Received choke message
                                    case 0:
                                        isChockedBy.put(remotePeerID, true);
                                        peerlog.choking(remotePeerID);
                                        break;
                                    // Received unchoke message
                                    case 1:
                                        //inBitfield = peersBitfields.get(remotePeerID);
                                        isChockedBy.put(remotePeerID, false);
                                        peerlog.unchoking(remotePeerID);
                
                                        for (int i = 0; i < bitfield.length; i++) {
                                            byte mask = 1;
                                            for (int j = 0; j < 8; j++) {
                                                byte myBit = (byte) ((bitfield[i] >> (7 - j)) & mask);
                                                byte inBit = (byte) ((remoteBitfield[i] >> (7 - j)) & mask);
                
                                                if (myBit == inBit) {
                                                    continue;
                                                } else if (myBit == 0 && inBit == 1) {
                                                    int pieceIdx = (8 * i) + j;
                
                                                    if (!requests.contains(pieceIdx)) {
                                                        //TODO: Make Selection Random
                                                        requests.add(pieceIdx);
                                                        setOutMessage(new Request(bitfield, pieceIdx, peerID));
                                                        System.out.println("Sent request from an unchoke method");
                                                    }
                                                }
                                            }
                
                                        }
                                        break;
                                    // Received interested message
                                    case 2:
                                        peersInterestedInMe.put(remotePeerID, true);
                                        peerlog.receivingInterested(remotePeerID);
                                        break;
                                    // Received not interested message
                                    case 3:
                                        peersInterestedInMe.remove(remotePeerID);
                                        peerlog.receivingNotInterested(remotePeerID);
                                        break;
                                    // Received have message
                                    case 4:
                                        int haveIndex = ByteBuffer.wrap(payload).getInt();
                                        int haveIndexByte = haveIndex / 8;
                                        int haveIndexBit = haveIndex % 8;

                                        // checks if the remote peer has an interesting piece or not
                                        // 11100000 >> 5 = 00000111
                                        // 00000111 & 00000001
                                        if (((bitfield[haveIndexByte] >> (7 - haveIndexBit)) & 1) == 0) {
                                            setOutMessage(new Interested(peerID));
                                            System.out.println("Have: Sending interested message to " + remotePeerID);
                                        } else {
                                            setOutMessage(new Uninterested(peerID));
                                            System.out.println("Have: sending not interested message to " + remotePeerID);
                                        }

                                        // update the bitfield for this remote peer
                                        byte[] newRemoteBitfield = getPeersBitfields(remotePeerID);
                                        byte newBit = (byte)(1 << (7 - haveIndexBit));
                                        byte newByte = (byte)(newRemoteBitfield[haveIndexByte] | newBit);
                                        newRemoteBitfield[haveIndexByte] = newByte;
                                        setPeersBitfields(remotePeerID, newRemoteBitfield);

                                        break;
                                    // Received bitfield message
                                    case 5:
                                        // retrieves the bitfield of the remote peer
                                        for (int i = 0; i < bitfield.length; i++) {
                                            remoteBitfield[i] = inMessageBytes[i + 5];
                                        }

                                        // checks if the remote peer has any pieces that are needed
                                        boolean interested = false;
                                        for (int i = 0; i < remoteBitfield.length; i++) {
                                            // compares each bit in this peer's bitfield with each bit in the remote peer's bitfield
                                            byte mask = 1;
                
                                            for (int j = 0; j < 8; j++) {
                                                byte myBit = (byte)((bitfield[i] >> (7 - j)) & mask);
                                                byte inBit = (byte)((remoteBitfield[i] >> (7 - j)) & mask);
                
                                                if (myBit == inBit) {
                                                    continue;
                                                } else if (myBit == 0 && inBit == 1) {
                                                    setOutMessage(new Interested(peerID));
                                                    System.out.println("Bitfield: sending interested message to " + remotePeerID);
                                                    interested = true;
                                                    break;
                                                }
                                            }
                                        }

                                        // adds this remote peer's bitfield
                                        setPeersBitfields(remotePeerID, remoteBitfield);

                                        // sends not interested if the remote peer contained no interesting pieces
                                        if (!interested) {
                                            setOutMessage(new Uninterested(peerID));
                                            System.out.println("Bitfield: sending not interested message to " + remotePeerID);
                                        }
                                        break;
                                    // Received request message
                                    case 6:
                                        break;
                                    // Received piece message
                                    case 7:
                                        // TODO: IMPLEMENT THE FOLLOWING
                                        /*
                                         * Whenever a peer receives a piece completely, it checks the bitfields of its neighbors 
                                         * and decides whether it should send ‘not interested’ messages to some neighbors.
                                         */
                                        break;
                                    default:
                                        System.out.println("Received an unknown message type");
                                        break;
                                }

                            }
                        } 
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void extractHandshake() {
                try {
                    boolean correctHeader = false;
                    byte[] headerBytes = new byte[18];
                    in.read(headerBytes, 0, 18);
                    String header = new String(headerBytes, StandardCharsets.UTF_8);
                    System.out.println("The received handshake header is: " + header);

                    if (header.equals("P2PFILESHARINGPROJ")) {
                        correctHeader = true;
                    }

                    byte[] zerosBytes = new byte[10];
                    in.read(zerosBytes, 0, 10);

                    boolean correctID = false;
                    byte[] handshakePeerIDBytes = new byte[4];
                    in.read(handshakePeerIDBytes, 0, 4);
                    int handshakePeerID = Integer.parseInt(new String(handshakePeerIDBytes, StandardCharsets.UTF_8));//ByteBuffer.wrap(handshakePeerIDBytes).getInt();
                    System.out.println("The received handshake ID is: " + handshakePeerID);

                    if (remotePeerID == handshakePeerID) {
                        correctID = true;
                    }

                    System.out.println("correctHeader is : " + correctHeader + " and correctID is : " + correctID);
                    if (correctHeader && correctID){
                        setReceivedHandshake(true);
                        neighbors.put(remotePeerID, "received");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public class Writer_temp implements Runnable {
            private HandshakeMessage handshakeMessage = new HandshakeMessage(peerID);
            private byte[] outMessageBytes;

            public Writer_temp() {}

            public void run() {
                // sends out the Handshake message
                sendMessage();

                // waits for the corresponding handshake to be received to move on to sending bitfield
                while (!getReceivedHandshake()) {
                }
                System.out.println("Handshake has been sent and received: " + remotePeerID);

                // proceed to send out bitfield message
                setOutMessage(new Bitfield(bitfield, peerID));
                sendMessage();
                System.out.println("Bitfield message sent to " + remotePeerID);

                while (!getProtocolCompleted()) {
                    if (messagesToSend.containsKey(remotePeerID)){
                        setOutMessage(getMessagesToSend().get(remotePeerID));
                    }

                    // spins until there is a message to send out
                    while (getOutMessage() == null) {
                    }

                    sendMessage();
                }

                // TODO: remmember this line when needing to terminate the program
                //setProtocolCompleted(true);
            }

            // converts the outgoing message to byte array
            void convertMessageToBytes() {
                int ourPeerID = peerID;
                int mask = 0xff000000;

                // checks if the mesage to be converted is the handshake message
                if (sendHandshake) {
                    outMessageBytes = new byte[32];
                    String outID = peerID + "";

                    // converts the handshake message to byte array to be sent out
                    for (int i = 0; i < outMessageBytes.length; i++) {
                        if (i < 18) {
                            outMessageBytes[i] = (byte) handshakeMessage.getHandshakeHeader().charAt(i);
                        } else if (i >= 18 && i < 28) {
                            outMessageBytes[i] = 0;
                        } else {
                            outMessageBytes[i] = (byte)outID.charAt(i - 28);

                            //outMessageBytes[i] = (byte) (ourPeerID & mask);
                            //System.out.println("Writing our handshake peerID to send to" + remotePeerID);
                            //ourPeerID <<= 8;
                        }
                    }

                    // this is to prevent future calls to this function from writing out another handshake message
                    sendHandshake = false;
                    System.out.println("Wrote handshake out to " + remotePeerID);
                }
                // checks if the message to be converted is a non-handshake message
                else {
                    int len = outMessage.getMLength();
                    byte[] payload = outMessage.getMPayload();
                    outMessageBytes = new byte[5 + len];

                    // converts the outgoing message to byte array to be sent out
                    for (int i = 0; i < 5 + len; i++) {
                        if (i < 4) {
                            // outMessageBytes[i] = (byte) (len & mask);
                            // len <<= 8;
                            ByteBuffer lengthByteBuffer = ByteBuffer.allocate(4);
                            lengthByteBuffer.putInt(len);


                            System.out.println("Writing our message length to send to " + remotePeerID);
                        } else if (i == 4) {
                            outMessageBytes[i] = outMessage.getMType();
                            System.out.println("Writing our message type to send to " + remotePeerID);
                        } else {
                            outMessageBytes[i] = payload[i - 5];
                            System.out.println("Writing our message payload to send to " + remotePeerID);
                        }
                    }
                    System.out.println("Wrote message out to " + remotePeerID);
                }
            }

            private void sendMessage() {
                // converts the outgoing message to byte array
                convertMessageToBytes();

                // sets outMessage to null so that the writer spins until the reader has another message to send out
                setOutMessage(null);

                try {
                    out.write(outMessageBytes);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } 
        }
    }

    public static class newNeighbors extends TimerTask {
        private HashMap<Integer, Double> rates = new HashMap<Integer, Double>();
        private HashMap<Integer, Double> hrates = new HashMap<Integer, Double>();
        private HashMap<Integer, Boolean> isChoked = new HashMap<Integer, Boolean>();
        private boolean complete;
        private int[] highPeers;
        private double[] highRates;
        private ArrayList<Integer> newPrefNeighbor;
    
        public newNeighbors() {
            highPeers = new int[numPreferredNeighbors];
            highRates = new double[numPreferredNeighbors];
            newPrefNeighbor  = new ArrayList<Integer>();
        }
    
        
    
        public void run(){
            //Calc rates
            for (RemotePeerInfo p : peers.values()){
                int numPieces = getPieceData().get(p.getPeerID());
                double rate = (double)numPieces / unchokingInterval;
                if (p.getPeerID()!=peerID)
                    rates.put(p.getPeerID(), rate);
            }
    
            //Used to assign complete.
            for (int i = 0; i < getBitField().length; i++){
                complete = true;
                if (areLeftovers){
                    int byteNum = 0;
                    for (int j = 1; j <= numLeftover; j++) {
                        byteNum += (int) Math.pow(2, 8 - j);
                    }
                    if (i < getBitField().length-1){
                        if (getBitField()[i]==((byte)-1)){
                            continue;
                        } else {
                            complete = false;
                            break;
                        }
                    } else {
                        if (getBitField()[i]==((byte)byteNum) || getBitField()[i] == ((byte)-1)){
                            continue;
                        } else {
                            complete = false;
                            break;
                        }
                    }
                } else {
                    if (getBitField()[i]==((byte)-1)){
                        continue;
                    } else {
                        complete = false;
                        break;
                    }
                }
            }
            System.out.println("");
    
            if (!complete){
                //Compute neighbors by download rate.
                System.out.println("Computing new neighbors when I don't have the complete file.");
                for(int p : rates.keySet()){
                    boolean notinputed = true;
                    for (int i = 0; i < highRates.length; i++){
                        if(rates.get(p)>highRates[i] && getPeersInterestedInMe().get(p)){
                            for (int j = i; j < highRates.length-1; i++){
                                highRates[j+1] = highRates[j];
                                highPeers[j+1] = highPeers[j];
                            }
                            highRates[i] = rates.get(p);
                            highPeers[i] = p;
                            break;
                        } else if(rates.get(p)==highRates[i] && rates.get(p) == 0  && highPeers[i] == 0 && notinputed){
                            //highRates[i] = rates.get(p);
                            highPeers[i] = p;
                            notinputed = false;
                        } else if (rates.get(p)==highRates[i] && i == (highRates.length-1) && getPeersInterestedInMe().get(p)){
                            double a = Math.random();
                            if (a > 0.5){
                                highRates[i] = rates.get(p);
                                highPeers[i] = p;
                            }
                        }
                        
                    }
                }
                System.out.println("Assign hrates to the peers with the highest rates");
                for (int i = 0; i < highRates.length; i++){
                    hrates.put(highPeers[i], highRates[i]);
                }
    
                // System.out.print("HighPeers has ");
                // for (int i = 0; i < highPeers.length; i ++){
                //     System.out.print(highPeers[i] + " ");
                // }
                
                for (int i = 0; i < highRates.length; i++){
                    newPrefNeighbor.add(highPeers[i]);
                    Unchoke unchoke = new Unchoke(peerID);
                    setMessagesToSend(highPeers[i], unchoke);
                    System.out.println("Sent unchoke to " + highPeers[i] + "when file is incomplete");
                }
                // System.out.print("newPrefNeighbor has ");
                // for (int i = 0; i < newPrefNeighbor.size(); i ++){
                //     System.out.print(newPrefNeighbor.get(i) + " ");
                // }
                peerlog.changeOfPrefNeighbor(newPrefNeighbor);
                newPrefNeighbor = new ArrayList<Integer>();
                highPeers = new int[numPreferredNeighbors];
                highRates = new double[numPreferredNeighbors];
    
    
                for (int p : peers.keySet()){
                    if ((hrates.containsKey(p) || getCurrentOptUnchoked() == p) || p == peerID){
                        continue;
                    }
                    Choke choke = new Choke(peerID);
                    setMessagesToSend(p, choke);
                    isChoked.put(p, true);
                    System.out.println("Sent choke to " + p + "when file is incomplete");
                }
    
            } else {
                //Compute neighbors randomly.
    
                System.out.println("I MADE IT TO THE RANDOM PEER SELECTION");
                HashMap<Integer, Integer> sentAlready = new HashMap<Integer, Integer>();
                int[] peerIDInInterested = new int[getPeersInterestedInMe().size()];
                int idx = 0;
                for(int p : getPeersInterestedInMe().keySet()){
                    peerIDInInterested[idx] = p;
                    idx++;
                }
                System.out.println(peerIDInInterested.length);
    
                for (int i = 0; i < numPreferredNeighbors; i++){
                    while(true){
                        int ran = (int) (Math.random()*peerIDInInterested.length);
                        
                        if(!sentAlready.containsKey(peerIDInInterested[ran])){
                            System.out.println("MADE IT INSIDE unchoking in random assignment");
                            newPrefNeighbor.add(peerIDInInterested[ran]);
                            Unchoke unchoke = new Unchoke(peerID);
                            setMessagesToSend(peerIDInInterested[ran], unchoke);
                            sentAlready.put(peerIDInInterested[ran], 0);
                            System.out.println("Sent unchoke in random peer selection to " + peerIDInInterested[ran]);
                            break;
                        }
    
                    }
                }
                peerlog.changeOfPrefNeighbor(newPrefNeighbor);
                newPrefNeighbor = new ArrayList<Integer>();
    
                int[] notSent = new int[getPeersInterestedInMe().size() - sentAlready.size()];
                int count=0;
                System.out.println("notSent size is: " + notSent.length);
                System.out.println("interestedInMe size is: " + getPeersInterestedInMe().size());
    
                for (int p : getPeersInterestedInMe().keySet()){
                    System.out.println("count is: " + count);
                    if (!sentAlready.containsKey(p) && getCurrentOptUnchoked() != p){
                        notSent[count]=p;
                        count++;
                    }
                }
    
                for (int i = 0; i < notSent.length; i++){
                    if (notSent[i]==0){
                        continue;
                    }
                    Choke choke = new Choke(peerID);
                    setMessagesToSend(notSent[i], choke);
                    isChoked.put(notSent[i], true);
                    System.out.println("Sent choke message at the end of random selection of peers to " + notSent[i]);
                }
            }
    
            
    
            //Reset Interests
            HashMap<Integer, Boolean> reset = new HashMap<Integer, Boolean>();
            for (int p : getPeersInterestedInMe().keySet()){
                reset.put(p, false);
            }
            //pp.setInterestedInMe(reset);
            setIsChoke(isChoked);
        }
        
    }

    public static class Optimistically extends TimerTask{
    
        private ArrayList<Integer> currentlyChoked = new ArrayList<Integer>();
        
        
        public Optimistically(){

        }
    
        public void run(){
            for (int pid : isChoke.keySet()){
                currentlyChoked.add(pid);
            }
    
            if (currentlyChoked.size() != 0){
                int ran = (int) (Math.random() * isChoke.size());
                Unchoke unchoke = new Unchoke(currentlyChoked.get(ran));
                setMessagesToSend(currentlyChoked.get(ran), unchoke);
                setCurrentOptUnchoked(currentlyChoked.get(ran));
                peerlog.changeOfOptUnchkNeighbor(currentlyChoked.get(ran));

                System.out.println("Sent unchoke from Optimistically to unchoke " + currentlyChoked.get(ran));
                currentlyChoked = new ArrayList<Integer>();
            }
        }
    }
}