
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.io.*;
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
    private byte[] fullBitField;
    private boolean haveFile;
    private int numOfPieces;
    private int bitFieldSize;
    private static int numLeftover;
    private static boolean areLeftovers;
    private boolean tester;
    private int portNum;
    private ServerSocket server;
    public static HashMap<Integer, Socket> sockets = new HashMap<Integer, Socket>();
    private static HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();
    private static HashMap<Integer, Integer> pieceData = new HashMap<Integer, Integer>();
    private HashMap<Integer, String> neighbors = new HashMap<Integer, String>();
    private HashMap<Integer, byte[]> peersBitfields = new HashMap<Integer, byte[]>();
    private static HashMap<Integer, Boolean> peersInterestedInMe = new HashMap<Integer, Boolean>();
    private static HashMap<Integer, Boolean> isChoke = new HashMap<Integer, Boolean>();
    private Set<Integer> requests = new HashSet<Integer>();
    private EventLog peerlog;
    private byte[][] pieces;

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
        
        // splits up allFileBytes into pieceSize portions, and places them inside of pieces
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
        bitField = new byte[bitFieldSize];
        fullBitField = new byte[bitFieldSize];

        String property = prop2.getProperty("" + peerID);
        String bit = property.split(" ")[2];

        portNum = Integer.parseInt(property.split(" ")[1]);

        if (bit.equals("1")) {
            int leftover = numOfPieces % 8;
            int byteNum = 0;
            for (int i = 1; i <= leftover; i++) {
                byteNum += (int) Math.pow(2, 8 - i);
            }

            for (int i = 0; i < bitField.length; i++) {
                if (areLeftovers && i == (bitField.length - 1)) {
                    bitField[i] = (byte) byteNum;
                    fullBitField[i] = (byte) byteNum;
                    continue;
                }

                bitField[i] = (byte) 255;
                fullBitField[i] = (byte) 255;
            }
            moveFile();
            haveFile = true;
        } else {
            for (int i = 0; i < bitField.length; i++) {
                bitField[i] = (byte) 0;
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
            String blank = "";

            RemotePeerInfo rpi = new RemotePeerInfo(ID, address, port);
            peers.put(ID, rpi);
            if (peerID != ID)
                peersInterestedInMe.put(ID, false);
            pieceData.put(ID, 0);

            if (peerID != ID) {
                neighbors.put(ID, blank);
            }

        } while (s.hasNext());
    }

    // Computes the number of Pieces of the given file
    private void computeNumberOfPiece() {
        double fSize = fileSize;
        double pSize = pieceSize;
        numOfPieces = (int) Math.ceil(fSize / pSize);
        int a = numOfPieces % 8;
        if (a == 0)
            bitFieldSize = numOfPieces / 8;
        else{
            bitFieldSize = (numOfPieces / 8) + 1;
            areLeftovers = true;
            numLeftover = a;
        }
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
                    peerlog.TCPConnectionTo(ID);

                    Writer w = new Writer(new HandshakeMessage(peerID), socket, peerID);//, ID);
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

        // Listener l = new Listener(this);
        // Thread t = new Thread(l);
        // t.start();
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
        Listener l = pp.new Listener(pp);
        Thread t = new Thread(l);
        t.start();
        // Timer timer = new Timer();
        // timer.schedule(new newNeighbors(numPreferredNeighbors, unchokingInterval, peers, bitField, peerID, areLeftovers, numLeftover, pp), 0, unchokingInterval * 1000);
        // Timer timer2 = new Timer();
        // timer2.schedule(new Optimistically(isChoke, pp), 0, optimisticUnchokingInterval*1000);
    }

    public class Listener implements Runnable {
        private boolean finish;
        private boolean handshakeDone;
        private boolean bitFieldSent;
        private ObjectInputStream in;
        private peerProcess pp;
        private HashMap<Integer, Boolean> isChockedBy = new HashMap<Integer, Boolean>();
        private HashMap<Integer, Boolean> containsInterestingPieces = new HashMap<Integer, Boolean>();


        public Listener(peerProcess p) {
            pp = p;
        }

        public void run() {
            finish = false;
            handshakeDone = false;
            bitFieldSent = false;
            int count = 0;

            while (!finish) {
                try {
                    boolean done = true;
                    for (String neighborState : neighbors.values()) {
                        if (neighborState.equals("received"))
                            continue;
                        else {
                            done = false;
                        }
                    }
    
                    if (done && !handshakeDone) {
                        System.out.println(peerID + " is done.");
                        if (haveFile && !bitFieldSent) {
                            for (Socket sock : sockets.values()) {
                                Bitfield b = new Bitfield(bitField, peerID);
                                Writer bitfieldOut = new Writer(b, sock, peerID);
                                Thread wThread = new Thread(bitfieldOut);
                                wThread.start();
                                bitFieldSent = true;
                            }
                        } 
                        handshakeDone=true;
                        Timer timer = new Timer();
                        timer.schedule(new newNeighbors(numPreferredNeighbors, unchokingInterval, peers, bitField, peerID, areLeftovers, numLeftover, pp), 0, unchokingInterval * 1000);
                        Timer timer2 = new Timer();
                        timer2.schedule(new Optimistically(isChoke, pp), 0, optimisticUnchokingInterval*1000);
                    }

                    //System.out.println(peerID + " is listening for messages");
                    Socket s = server.accept();
                    //System.out.println(peerID + "'s listener received a message");

                    in = new ObjectInputStream(s.getInputStream());
                    Object inMessage = in.readObject();
                    String inAddress = s.getInetAddress().getHostName();
                    int inPeerID = 0;
                    byte[] inBitfield = peersBitfields.get(inPeerID);

                    if (peersBitfields.containsKey(inPeerID)) {
                        for (int i = 0; i < inBitfield.length; i++) {
                            if (inBitfield[i] == 1 &&  bitField[i] == 0) {
                                containsInterestingPieces.put(inPeerID, true);
                            }
                        }
                    }
                    

                    for (Integer i : peers.keySet()) {
                        if (peers.get(i).getAddress().equals(inAddress)) {
                            inPeerID = i;
                        }
                    }

                    if (inMessage instanceof HandshakeMessage) {
                        HandshakeMessage handshake = (HandshakeMessage)inMessage;
                        int pid = handshake.getPeerID();

                        if (neighbors.get(pid).equals("sent")) {
                            System.out.println(peerID + " confirmed from " + pid);
                            neighbors.replace(pid, "sent", "received");
                            continue;
                        } else {
                            System.out.println(peerID + " received from " + pid + ". " + peerID + " will now send handshake back");
                            peerlog.TCPConnectionFrom(pid);
                        }
        
                        neighbors.replace(pid, "received");
        
                        String address = peers.get(pid).getAddress();
                        int port = peers.get(pid).getPort();
                        s.close();
        
                        s = new Socket(address, port);
                        sockets.put(pid, s);

                        Writer w = new Writer(new HandshakeMessage(peerID), s, peerID);//, pid);
                        Thread t = new Thread(w);
                        t.start();
                    } 
                    //Choke
                    else if (inMessage instanceof Choke) {
                        Choke c = (Choke)inMessage;
                        isChockedBy.put(inPeerID, true);
                    } 
                    //Unchoke
                    else if (inMessage instanceof Unchoke) {
                        Unchoke uc = (Unchoke)inMessage;
                        isChockedBy.put(inPeerID, false);

                        for (int i = 0; i < bitField.length; i++) {
                            byte mask = 1;

                            for (int j = 0; j < 8; j++) {
                                byte myBit = (byte)((bitField[i] >> (7 - j)) & mask);
                                byte inBit = (byte)((inBitfield[i] >> (7 - j)) & mask);

                                if (myBit == inBit) {
                                    continue;
                                } else if (myBit == 0 && inBit == 1) {
                                    int pieceIdx = (8 * i) + j;

                                    if (!requests.contains(pieceIdx)) {
                                        requests.add(pieceIdx);

                                        Writer w = new Writer(new Request(pieceIdx), sockets.get(inPeerID), peerID);
                                        Thread t = new Thread(w);
                                        t.start();
                                    }
                                }
                            }
                                
                        }
                    } 
                    //Interested
                    else if (inMessage instanceof Interested) {
                        Interested interested = (Interested)inMessage;
                        System.out.println(peerID + " has received an interested message from " + interested.getPID());
                        peersInterestedInMe.replace(interested.getPID(), true);
                        peerlog.receivingInterested(interested.getPID());
                    } 
                    //Uninterested
                    else if (inMessage instanceof Uninterested) {
                        Uninterested uninterested = (Uninterested)inMessage;
                        System.out.println(peerID + " has received an uninterested message from " + uninterested.getPID());
                        peerlog.receivingNotInterested(uninterested.getPID());
                    } 
                    //Have
                    else if (inMessage instanceof Have) {
                        Have h = (Have)inMessage;
                        boolean write = false;
                        for (int i = 0; i < bitField.length;i++){
                            if (bitField[i] == h.getBitfield()[i])
                                continue;
                            else {
                                Interested interested = new Interested(peerID);
                                int pid = h.getPID();

                                Writer w = new Writer(interested, sockets.get(pid), peerID);
                                Thread t = new Thread(w);
                                t.start();

                                write = true;
                                System.out.println(peerID + " has sent an interested message to " + h.getPID());
                                break;
                            }
                        }

                        if (!write){
                            Uninterested uninterested = new Uninterested(peerID);
                            int pid = h.getPID();

                            Writer w = new Writer(uninterested, sockets.get(pid), peerID);
                            Thread t = new Thread(w);
                            t.start();

                            System.out.println(peerID + " has sent an uninterested message to " + h.getPID());
                        }

                        //Updating the peersBitfield hashmap everytime a have message is received.
                        if(peersBitfields.containsKey(h.getPID())){
                            peersBitfields.replace(h.getPID(), h.getBitfield());
                        }
                        else {
                            peersBitfields.put(h.getPID(), h.getBitfield());
                        }
                    } 
                    //Bitfield
                    else if (inMessage instanceof Bitfield) {
                        Bitfield b = (Bitfield)inMessage;
                        boolean write = false;
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

                        if (!write){
                            Uninterested uninterested = new Uninterested(peerID);
                            int pid = b.getPID();
                            Writer w = new Writer(uninterested, sockets.get(pid), peerID);//, pid);
                            Thread t = new Thread(w);
                            t.start();
                            System.out.println(peerID + " has sent an uninterested message to " + b.getPID());
                        }

                        if(peersBitfields.containsKey(b.getPID())){
                            peersBitfields.replace(b.getPID(), b.getBitfield());
                        }
                        else
                            peersBitfields.put(b.getPID(), b.getBitfield());
                    } 
                    //Request
                    else if (inMessage instanceof Request) {
                        Request r = (Request)inMessage;
                        int p = r.getPieceIdx();

                        Writer w = new Writer(new Piece(pieces[p], peerID), sockets.get(inPeerID), peerID);
                        Thread t = new Thread(w);
                        t.start();
                    } 
                    //Piece
                    else if (inMessage instanceof Piece) {
                        Piece p = (Piece)inMessage;
                        int temp = pieceData.get(p.getPID());
                        pieceData.replace(p.getPID(), (temp+1));

                        for (int i = 0; i < bitField.length; i++) {
                            byte mask = 1;

                            for (int j = 0; j < 8; j++) {
                                byte myBit = (byte)((bitField[i] >> (7 - j)) & mask);
                                byte inBit = (byte)((inBitfield[i] >> (7 - j)) & mask);

                                if (myBit == inBit) {
                                    continue;
                                } else if (myBit == 0 && inBit == 1) {
                                    int pieceIdx = (8 * i) + j;

                                    if (!requests.contains(pieceIdx)) {
                                        requests.add(pieceIdx);

                                        if (isChockedBy.get(inPeerID) || !containsInterestingPieces.get(inPeerID)){
                                            continue;
                                        }

                                        Writer w = new Writer(new Request(pieceIdx), sockets.get(inPeerID), peerID);
                                        Thread t = new Thread(w);
                                        t.start();
                                    }
                                }
                            }       
                        }
                    } else {
                        System.out.println(peerID + "'s LISTENER DID NOT RECEIVE A MESSAGE THAT IS KNOWN!!!");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                

                //Checks to see if all the peers have all of the pieces
                boolean allDone = false;
                for (byte[] b : peersBitfields.values()){
                    if (b!=fullBitField){
                        allDone=false;
                        break;
                    }
                    allDone=true;
                }
                if (allDone)
                    finish = true;

            }
            peerlog.closeLogger();  
        }
    }

    public boolean getTester(){
        return tester;
    }

    public HashMap<Integer, Integer> getPieceData(){
        return pieceData;
    }

    public HashMap<Integer, Boolean> getPeersInterestedInMe(){
        return peersInterestedInMe;
    }

    public HashMap<Integer, Socket> getSockets(){
        return sockets;
    }

    public int getCurrentOptUnchoked(){
        return currentOptUnchoked;
    }

    public void setPieceData(HashMap<Integer, Integer> p){
        pieceData = p;
    }

    public void setInterestedInMe(HashMap<Integer, Boolean> i){
        peersInterestedInMe = i;
    }

    public void setCurrentOptUnchoked(int pid){
        currentOptUnchoked = pid;
    }

    public void setIsChoke(HashMap<Integer, Boolean> choking){
        isChoke = choking;
    }

}