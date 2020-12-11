
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
    private HashMap<Integer, Socket> sockets = new HashMap<Integer, Socket>();
    private static HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();
    private static HashMap<Integer, Thread> threads = new HashMap<Integer, Thread>();
    private static HashMap<Integer, String> neighbors = new HashMap<Integer, String>();
    private static HashMap<Integer, Message> messagesToSend = new HashMap<Integer, Message>();
    private static EventLog peerlog;
    private static int numLeftover;
    private static boolean areLeftovers;
    private int bitfieldSize;
    private byte[] fullBitfield;
    private static HashMap<Integer, Integer> peerPieceData = new HashMap<Integer, Integer>();
    private static HashMap<Integer, byte[]> peersBitfields = new HashMap<Integer, byte[]>();
    private static HashMap<Integer, Boolean> peersInterestedInMe = new HashMap<Integer, Boolean>();
    private static HashMap<Integer, Boolean> isChoke = new HashMap<Integer, Boolean>();
    private HashMap<Integer, Integer> requests = new HashMap<Integer, Integer>();
    private Set<Integer> piecesIHave = new HashSet<Integer>();
    private byte[][] pieces;
    private int piecesDownloaded;
    private static Timer timer = new Timer();
    private static Timer timer2 = new Timer();
    private File byteFile;
    private boolean haveFile = false;

    public peerProcess(int pID) {
        peerID = pID;
        peerlog = new EventLog(pID);
        initialize();
        pieces = new byte[numOfPieces][pieceSize];

        // TODO: should we add a condition where we encode the file only if we already own the file?
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

    // Take the file of pieces and translate it to original text
    public void decodeBytes(){
        // If the file is empty, write to it
        if (byteFile.length() == 0) {
            String workingDir = System.getProperty("user.dir");
            FileWriter writer = null;
            try {
                writer = new FileWriter(workingDir + "/peer_" + peerID + "/" + fileName);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            
            for (int i = 0; i < pieces.length; i++) {
                String text = new String(pieces[i], StandardCharsets.UTF_8);
                // Checks to see if there are null values
                if (text.indexOf("") > 0){
                    text = text.substring(0, text.indexOf(""));
                }
                try {
                    writer.append(text);
                    writer.flush();
                    //writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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

        // set the bitfield to all ones if the peer owns the file; 0 otherwise
        // set up "fullBitfield" to have all ones. this will be used later on to check if all peers have downloaded the file
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

                bitfield[i] = (byte)255;
                fullBitfield[i] = (byte)255;
            }

            moveFile();
            haveFile = true;
        } else {
            // sets this peer's bitfield to all zeros
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
            byteFile = new File(workingDir + "/peer_" + peerID + fileName);
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
                setInterestedInMe(ID, false);
                peersBitfields.put(ID, zero);
            }
            peerPieceData.put(ID, 0);

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
            peerlog.TCPConnectionFrom(inPeerID);
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
                Handler afterHandler = new Handler(peers.get(inPeerID), false);
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

    public synchronized HashMap<Integer, Socket> getSockets() {
        return sockets;
    }

    public static synchronized byte[] getBitField() {
        return bitfield;
    }

    public static synchronized HashMap<Integer, Integer> getPeerPieceData() {
        return peerPieceData;
    }

    public static synchronized void setPeerPieceData(HashMap<Integer, Integer> p) {
        peerPieceData = p;
    }

    public static synchronized byte[] getPeersBitfields(int remotePID) {
        return peersBitfields.get(remotePID);
    }

    public static synchronized void setPeersBitfields(int remotePID, byte[] newBitfield) {
        peersBitfields.put(remotePID, newBitfield);
    }

    private synchronized byte[][] getPieces() {
        return pieces;
    }

    private synchronized void setPieces(int pIdx, byte[] p) {
        pieces[pIdx] = p;
    }

    private synchronized byte[] getBitfield() {
        return bitfield;
    }

    private synchronized void setBitfield(int pieceIdx) {
        int pieceIdxByte = pieceIdx / 8;
        int pieceIdxBit = pieceIdx % 8;

        byte newBit = (byte)(1 << (7 - pieceIdxBit));
        byte newByte = (byte)(bitfield[pieceIdxByte] | newBit);
        bitfield[pieceIdxByte] = newByte;
    }

    private synchronized int getPiecesDownloaded(){
        return piecesDownloaded;
    }

    private synchronized void setPiecesDownloaded(){
        piecesDownloaded++;
    }

    private synchronized Set<Integer> getPiecesIHave(){
        return piecesIHave;
    }

    private synchronized void setPiecesIHave(int piece){
        piecesIHave.add(piece);
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

    public static synchronized HashMap<Integer, Boolean> getPeersInterestedInMe() {
        return peersInterestedInMe;
    }

    public static synchronized int getCurrentOptUnchoked() {
        return currentOptUnchoked;
    }
    public static synchronized void setInterestedInMe(int pid, boolean status) {
        peersInterestedInMe.put(pid, status);
    }

    public static synchronized void setCurrentOptUnchoked(int pid) {
        currentOptUnchoked = pid;
    }

    public static synchronized void setIsChoke(HashMap<Integer, Boolean> choking) {
        isChoke = choking;
    }

    public static synchronized HashMap<Integer, Boolean> getIsChoke(){
        return isChoke;
    }

    public synchronized void setRequests(int remotePeerID, int pieceIdx){
        requests.put(remotePeerID, pieceIdx);
    }

    public synchronized void setRequestsRemoval(int piD){
        requests.remove(piD);
    }

    public synchronized HashMap<Integer, Integer> getRequests(){
        return requests;
    }

    public static synchronized int getUnchokingInterval(){
        return unchokingInterval;
    }

    public synchronized int getNumOfPieces(){
        return numOfPieces;
    }

    public static synchronized int getOpUnInterval(){
        return optimisticUnchokingInterval;
    }



    // Starts up the peerProcess and begins message delivery
    public static void main(String[] args) {
        peerProcess pp = new peerProcess(Integer.parseInt(args[0]));

        // create sockets with all peers that came before us
        pp.establishConnections();
        timer.schedule(new newNeighbors(), 1000, getUnchokingInterval() * 1000);
        timer2.schedule(new Optimistically(), 1000, getOpUnInterval()*1000);

        //System.out.println("About to end peerProcess");
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
        private byte[] inMessageBytes;
        private boolean receivedHandshake;
        private boolean sendHandshake = true;
        private byte[] remoteBitfield = new byte[bitfield.length];

        private boolean finish;
        private boolean handshakeDone;
        private boolean bitFieldSent;
        private peerProcess pp;
        private HashMap<Integer, Boolean> isChokedBy = new HashMap<Integer, Boolean>();
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

        private synchronized HashMap<Integer, Boolean> getIsChokedBy(){
            return isChokedBy;
        }

        public synchronized void setIsChokedBy(int pid, boolean status){
            isChokedBy.put(pid, status);
        }

        public synchronized boolean getContainsInterestingPieces(int remotePeerID){
            return containsInterestingPieces.get(remotePeerID);
        }

        public synchronized void setContainsInterestingPieces(int pid, boolean does){
            containsInterestingPieces.put(pid, does);
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
                                //System.out.println("Finished extracting the length of the message: " + length);

                                // extracts the message type of the incoming message
                                byte[] messageTypeBuf = new byte[1];
                                in.read(messageTypeBuf, 0, 1);
                                byte messageType = messageTypeBuf[0];
                                //System.out.println("Finished extracting the message type: " + messageType);

                                // extracts the payload of the incoming message
                                byte[] payload = null;
                                if (length != 0) {
                                    payload = new byte[length - 1];
                                    in.read(payload, 0, length - 1);
                                } 

                                inMessageBytes = new byte[4 + length];
                                //System.out.println("Finished extracting the payload");

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
                                        System.out.println("Received Choke message from: " + remotePeerID);
                                        setIsChokedBy(remotePeerID, true);
                                        setRequestsRemoval(remotePeerID);
                                        System.out.println("Requests size is: " + getRequests().size());
                                        peerlog.choking(remotePeerID);
                                        break;
                                    // Received unchoke message
                                    case 1:
                                        System.out.println("Received Unchoke message from: " + remotePeerID);
                                        //inBitfield = peersBitfields.get(remotePeerID);
                                        setIsChokedBy(remotePeerID, false);
                                        peerlog.unchoking(remotePeerID);
                
                                        boolean sentRequest = false;
                                        for (int i = 0; i < bitfield.length; i++) {
                                            byte mask = 1;
                                            for (int j = 0; j < 8; j++) {
                                                byte myBit = (byte) ((bitfield[i] >> (7 - j)) & mask);
                                                byte inBit = (byte) ((remoteBitfield[i] >> (7 - j)) & mask);
                
                                                if (myBit == inBit) {
                                                    continue;
                                                } else if (myBit == 0 && inBit == 1) {
                                                    int pieceIdx = (8 * i) + j;
                                                    if (!getRequests().containsValue(pieceIdx)) {
                                                        //TODO: Make Selection Random
                                                        if (getIsChokedBy().get(remotePeerID) || !getContainsInterestingPieces(remotePeerID)) {
                                                            continue;
                                                        }
                                                        setRequests(remotePeerID, pieceIdx);
                                                        setOutMessage(new Request(pieceIdx));
                                                        System.out.println("Unchoke: sending request message");
                                                        sentRequest = true;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (sentRequest)
                                                break;
                                        }
                                        break;
                                    // Received interested message
                                    case 2:
                                        System.out.println("Received Interested message from: " + remotePeerID);
                                        setInterestedInMe(remotePeerID, true);
                                        peerlog.receivingInterested(remotePeerID);
                                        break;
                                    // Received not interested message
                                    case 3:
                                        System.out.println("Received Not Interested message from: " + remotePeerID);
                                        setInterestedInMe(remotePeerID, false);
                                        peerlog.receivingNotInterested(remotePeerID);
                                        break;
                                    // Received have message
                                    case 4:
                                        System.out.println("Received Have message from: " + remotePeerID);
                                        int haveIndex = ByteBuffer.wrap(payload).getInt();
                                        int haveIndexByte = haveIndex / 8;
                                        int haveIndexBit = haveIndex % 8;

                                        // checks if the remote peer has an interesting piece or not
                                        if (((bitfield[haveIndexByte] >> (7 - haveIndexBit)) & 1) == 0) {
                                            setOutMessage(new Interested(peerID));
                                            setContainsInterestingPieces(remotePeerID, true);
                                            System.out.println("Have: Sending interested message to " + remotePeerID);
                                        } else {
                                            setOutMessage(new Uninterested(peerID));
                                            setContainsInterestingPieces(remotePeerID, false);
                                            System.out.println("Have: sending not interested message to " + remotePeerID);
                                        }

                                        // update the bitfield for this remote peer
                                        byte[] newRemoteBitfield = getPeersBitfields(remotePeerID);
                                        byte newBit = (byte)(1 << (7 - haveIndexBit));
                                        byte newByte = (byte)(newRemoteBitfield[haveIndexByte] | newBit);
                                        newRemoteBitfield[haveIndexByte] = newByte;
                                        setPeersBitfields(remotePeerID, newRemoteBitfield);

                                        peerlog.receivingHave(remotePeerID, haveIndex);
                                        break;
                                    // Received bitfield message
                                    case 5:
                                        System.out.println("Received Bitfield message from: " + remotePeerID);
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
                                                    setContainsInterestingPieces(remotePeerID, true);
                                                    System.out.println("Bitfield: sending interested message to " + remotePeerID);
                                                    interested = true;
                                                    break;
                                                }
                                            }

                                            if (interested) {
                                                break;
                                            }
                                        }

                                        // adds this remote peer's bitfield
                                        setPeersBitfields(remotePeerID, remoteBitfield);

                                        // sends not interested if the remote peer contained no interesting pieces
                                        if (!interested) {
                                            setOutMessage(new Uninterested(peerID));
                                            setContainsInterestingPieces(remotePeerID, false);
                                            System.out.println("Bitfield: sending not interested message to " + remotePeerID);
                                        }
                                        break;
                                    // Received request message
                                    case 6:
                                        System.out.println("Received Request message from: " + remotePeerID);
                                        // extracts the piece index that was requested from the remote peer
                                        int remotePieceIdx = ByteBuffer.wrap(payload).getInt();

                                        setOutMessage(new Piece(remotePieceIdx, pieces[remotePieceIdx]));
                                        System.out.println("Sent Piece Message");
                                        break;
                                    // Received piece message
                                    case 7:
                                        System.out.println("Received Piece message from: " + remotePeerID);
                                        // TODO: IMPLEMENT THE FOLLOWING
                                        /*
                                         * Whenever a peer receives a piece completely, it checks the bitfields of its neighbors 
                                         * and decides whether it should send ‘not interested’ messages to some neighbors.
                                         */

                                        if (!getIsChokedBy().get(remotePeerID) && getContainsInterestingPieces(remotePeerID)) {
                                            byte[] remotePieceIdxBytes = new byte[4];
                                            byte[] remotePieceBytes = new byte[pieceSize];
                                            for (int i = 0; i < length - 1; i++) {
                                                if (i < 4) {
                                                    remotePieceIdxBytes[i] = payload[i];
                                                } else {
                                                    remotePieceBytes[i - 4] = payload[i];
                                                }
                                            }

                                            // extract the piece index received
                                            int remotePIdx = ByteBuffer.wrap(remotePieceIdxBytes).getInt();
                                            System.out.println("Piece: received piece index " + remotePIdx + " from " + remotePeerID);
                                            
                                            if(!getPiecesIHave().contains(remotePIdx)){
                                                // update our bitfield with this newly obtained piece
                                                setBitfield(remotePIdx);
                                                setPieces(remotePIdx, remotePieceBytes);
                                                setPiecesIHave(remotePIdx);
                                                setPiecesDownloaded();
                                                peerlog.downloadingAPiece(remotePeerID, remotePIdx, getPiecesDownloaded());
                                                System.out.println("Our total number of pieces is " + getPiecesDownloaded());

                                                // sends out a Have message to all remote peers
                                                for (int key: peers.keySet()) {
                                                    if (key == peerID) {
                                                        continue;
                                                    } else {
                                                        System.out.println("Piece: sending out have message to everyone for piece: " + remotePIdx);
                                                        setMessagesToSend(key, new Have(remotePIdx));
                                                    }
                                                }
                                            }

                                            // checks to see if the remote peer has any pieces that are interesting
                                            // if so, then send out a request message
                                            // boolean requested = false;
                                            // for (int i = 0; i < remoteBitfield.length; i++) {
                                            //     // compares each bit in this peer's bitfield with each bit in the remote peer's bitfield
                                            //     byte mask = 1;
                    
                                            //     for (int j = 0; j < 8; j++) {
                                            //         byte myBit = (byte)((bitfield[i] >> (7 - j)) & mask);
                                            //         byte inBit = (byte)((remoteBitfield[i] >> (7 - j)) & mask);
                    
                                            //         if (myBit == inBit) {
                                            //             continue;
                                            //         } else if (myBit == 0 && inBit == 1) {
                                            //             if (!getRequests().containsValue((i * 8) + j) && !getPiecesIHave().contains(remotePIdx)) {
                                            //                 setOutMessage(new Request((i * 8) + j));
                                            //                 setRequests(remotePeerID, (i * 8) + j);
                                            //                 System.out.println("Piece: sending request message to " + remotePeerID + " for piece " + ((i * 8) + j));
                                            //                 requested = true;
                                            //                 break;
                                            //             }
                                            //         }
                                            //     }

                                            //     // break out if there has already been a piece requested
                                            //     if (requested) {
                                            //         break;
                                            //     }
                                            // }

                                            while(true){
                                                int ran = (int) (Math.random() * getNumOfPieces());
                                                if (!getRequests().containsValue(ran) && !getPiecesIHave().contains(ran)) {
                                                    setOutMessage(new Request(ran));
                                                    setRequests(remotePeerID, ran);
                                                    System.out.println("Piece: sending request message to " + remotePeerID + " for piece " + (ran));
                                                    break;
                                                }
                                            }
                                        }
                                        
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
                    int handshakePeerID = ByteBuffer.wrap(handshakePeerIDBytes).getInt();
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
                System.out.println("Handshake completed with: " + remotePeerID);

                // proceed to send out bitfield message
                setOutMessage(new Bitfield(bitfield));
                sendMessage();
                System.out.println("Bitfield message sent to " + remotePeerID);

                while (!getProtocolCompleted()) {
                    // spins until there is a message to send out
                    while (getOutMessage() == null) {
                        if (getMessagesToSend().containsKey(remotePeerID)){
                            setOutMessage(getMessagesToSend().get(remotePeerID));
                            getMessagesToSend().remove(remotePeerID);
                            //sendMessage();
                        }
                    }

                    sendMessage();

                    // TODO: add a proper condition here to check that all peers have received all pieces
                    // checks to see if all peers have downloaded the complete file
                    // setProtocolCompleted(true);
                    // for (byte[] bf: peersBitfields.values()) {
                    //     //System.out.println("Entered the loop for checking completed bitfields");
                    //     for (int i = 0; i < bf.length; i++) {
                    //         if (bf[i] != fullBitfield[i]) {
                    //             setProtocolCompleted(false);
                    //             //System.out.println("There is a peer who has not completed downloading the file");
                    //         }
                    //     }
                    // }
                    // if (getProtocolCompleted()) {
                    //     System.out.println("All peers have completed downloading the file");
                    //     break;
                    // }
                }
            }

            // converts the outgoing message to byte array
            void convertMessageToBytes() {
                // checks if the mesage to be converted is the handshake message
                if (sendHandshake) {
                    outMessageBytes = new byte[32];
                    byte[] handshakeRemotePeerIDBytes = ByteBuffer.allocate(4).putInt(peerID).array();

                    // converts the handshake message to byte array to be sent out
                    for (int i = 0; i < outMessageBytes.length; i++) {
                        if (i < 18) {
                            // puts the handshake header into the outgoing message byte array
                            outMessageBytes[i] = (byte)handshakeMessage.getHandshakeHeader().charAt(i);
                        } else if (i >= 18 && i < 28) {
                            // puts the zero bits into the outgoing message byte array
                            outMessageBytes[i] = 0;
                        } else {
                            // puts the peerID into the outgoing message byte array
                            outMessageBytes[i] = handshakeRemotePeerIDBytes[i - 28];
                        }
                    }

                    // this is to prevent future calls to this function from writing out another handshake message
                    sendHandshake = false;
                    System.out.println("Wrote handshake out to " + remotePeerID);
                }
                // checks if the message to be converted is a non-handshake message
                else {
                    int len = getOutMessage().getMLength();
                    byte[] payload = getOutMessage().getMPayload();
                    outMessageBytes = new byte[4 + len];
                    //System.out.println("Length of outgoing message payload is: " + len);

                    ByteBuffer lengthByteBuffer = ByteBuffer.allocate(4);
                    lengthByteBuffer.putInt(len);
                    byte[] lengthBytes = lengthByteBuffer.array();

                    // converts the outgoing message to byte array to be sent out
                    for (int i = 0; i < 4 + len; i++) {
                        if (i < 4) {
                            outMessageBytes[i] = lengthBytes[i];
                            //System.out.println("Writing our message length to send to " + remotePeerID);
                        } else if (i == 4) {
                            //System.out.println("Message type: " + outMessage.getMType());
                            outMessageBytes[i] = outMessage.getMType();
                            //System.out.println("Writing our message type to send to " + remotePeerID);
                        } else {
                            //System.out.println("Payload length: " + payload.length);
                            outMessageBytes[i] = payload[i - 5];
                            //System.out.println("Writing our message payload to send to " + remotePeerID);
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
        private HashMap<Integer, Boolean> isChoked2 = new HashMap<Integer, Boolean>();
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
            // calculate rates
            for (RemotePeerInfo p : peers.values()){
                int numPieces = getPeerPieceData().get(p.getPeerID());
                double rate = (double)numPieces / getUnchokingInterval();
                if (p.getPeerID()!=peerID)
                    rates.put(p.getPeerID(), rate);
            }
    
            // used to assign complete.
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
                // compute neighbors by download rate.
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
                    isChoked2.put(p, true);
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
                    isChoked2.put(notSent[i], true);
                    System.out.println("Sent choke message at the end of random selection of peers to " + notSent[i]);
                }
            }
    
            
    
            //Reset Interests
            //HashMap<Integer, Boolean> reset = new HashMap<Integer, Boolean>();
            for (int p : getPeersInterestedInMe().keySet()){
                //reset.put(p, false);
                setInterestedInMe(p, false);
            }
            //setInterestedInMe(reset);
            setIsChoke(isChoked2);
        }
        
    }

    public static class Optimistically extends TimerTask{
        private ArrayList<Integer> currentlyChoked = new ArrayList<Integer>();
        
        public Optimistically(){
        }
    
        public void run(){
            for (int pid : getIsChoke().keySet()){
                currentlyChoked.add(pid);
            }
    
            if (currentlyChoked.size() != 0){
                int ran = (int) (Math.random() * getIsChoke().size());
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