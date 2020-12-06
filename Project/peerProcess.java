
import java.util.Properties;
import java.util.Scanner;
import java.io.*;
import java.nio.file.*;
import java.rmi.Remote;
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
    private static String wDD = System.getProperty("user.dir");
    private static HashMap<Integer, Thread> sockets = new HashMap<Integer, Thread>();
    // private static HashMap<Integer, Socket> sockets = new HashMap<Integer, Socket>();
    private static HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();

    private ObjectInputStream in;
    private ObjectOutputStream out;

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

        Scanner s = null;
        try {
            s = new Scanner(new File(file));

        } catch (FileNotFoundException e) {
            e.printStackTrace();

            dir = new File(workingDir + "/init_scanner_" + e);
            dir.mkdir();
        }

        String line="";
        do {
            line = s.nextLine();
            String[] fields = line.split(" ");
            int ID = Integer.parseInt(fields[0]);
            String address = fields[1];
            int port = Integer.parseInt(fields[2]);

            RemotePeerInfo rpi = new RemotePeerInfo(ID, address, port);
            peers.put(ID, rpi);
            
            
            
        } while (s.hasNext()); 
    }

    // Computes the number of Pieces of the given file
    private void computeNumberOfPiece() {
        double fSize = fileSize;
        double pSize = pieceSize;
        numOfPieces = (int) Math.ceil(fSize / pSize);
    }

    private void createSockets() {
        Socket socket = null;
        String workingDir = System.getProperty("user.dir");
        File dir = new File(workingDir + "/dir:" + workingDir);
        dir.mkdir();
        HashMap<Integer, RemotePeerInfo> hiddenPeers = peers;

        Scanner s = null;
        try {
            s = new Scanner(new File(workingDir + "/PeerInfo.cfg"));
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            dir = new File(workingDir + "/pp_scanner_" + e);
            dir.mkdir();
        }

        try {
            server = new ServerSocket(portNum);
            dir = new File(workingDir + "/server_" + peerID);
            dir.mkdir();
        } catch (Exception e) {
            dir = new File(workingDir + "/server_error_" + e);
            dir.mkdir();
        }

        String line = s.nextLine();

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
                    Handler h = new Handler(socket, peerID, ID);
                    Thread handler = new Thread(h);
                    handler.start();

                    sockets.put(ID, handler);

                    dir = new File(workingDir + "/peer_" + peerID + "_to_peer_" + ID);
                    dir.mkdir();

                    System.out.println("Connection established with " + address);
                    hiddenPeers.remove(ID);
                } 
                catch (Exception e) {
                    dir = new File(workingDir + "/pp_sockets_"+ peerID+ "_to_" + ID +":"+ e);
                    dir.mkdir();
                }
            } else {
                hiddenPeers.remove(peerID);
                if (hiddenPeers.isEmpty())
                    break;
                Listener l = new Listener(server, peerID, hiddenPeers);
                Thread listener = new Thread(l);
                listener.start();

                dir = new File(workingDir + "/Thread_is_alive_:" + peerID+listener.isAlive());
                    dir.mkdir();
                dir = new File(workingDir + "/pp_listen_" + peerID);
                dir.mkdir();
                break;
            }

            line = s.nextLine();
        }
        // TODO: comment this out when the server stays listening
        try {
            TimeUnit.MILLISECONDS.sleep(10000);
        } catch(Exception ioException) {
            ioException.printStackTrace();
        }
    }

    /*
    // Socket sOut = null;
    // ObjectOutputStream oos = null;
    // File dir = new File(workingDir);

    // if (peerID == 1005) {
    //     try {
    //         sOut = new Socket("lin114-00.cise.ufl.edu", 9998);
    //         // dir = new File(workingDir + "/1005");
    //         // dir.mkdir();
    //         oos = new ObjectOutputStream(sOut.getOutputStream());
    //         oos.writeObject(new HandshakeMessage(1005));
    //         oos.flush();
    //         oos.close();
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     } finally {
    //         try {
    //             sOut.close();
    //             // dir = new File(workingDir + "/1005_closed");
    //             // dir.mkdir();
    //         } catch (Exception e) {
    //             e.printStackTrace();
    //             // dir = new File(workingDir + "/in1005_" + e);
    //             // dir.mkdir();
    //         }
    //     }
    // }

    // Socket s1 = null;
    // int count = 0;

    // try {
    //     server = new ServerSocket(portNum);
    //     // dir = new File(workingDir + "/server_" + peerID);
    //     // dir.mkdir();

    //     while (true) {
    //         if (peerID != 1001 && peerID != 1005) {
    //             break;
    //         }
    //         if (count == 5) {
    //             break;
    //         }

    //         s1 = server.accept();
    //         // dir = new File(workingDir + "/accepted_in_" + peerID);
    //         // dir.mkdir();

    //         in = new ObjectInputStream(s1.getInputStream());
    //         // dir = new File(workingDir + "/instream_" + peerID);
    //         // dir.mkdir();

    //         HandshakeMessage inMessage = (HandshakeMessage)in.readObject();
    //         // dir = new File(workingDir + "/inmsg_" + peerID);
    //         // dir.mkdir();

    //         if (inMessage.getHandshakeHeader().equals("P2PFILESHARINGPROJ") && (inMessage.getPeerID() == 1005 || inMessage.getPeerID() == 1001)) {
    //             dir = new File(workingDir + "/" + peerID + "_rcvd_" + inMessage.getPeerID());
    //             dir.mkdir();
    //         } else {
    //             break;
    //         }

    //         // dir = new File(workingDir + "/port_to_send_to" + s1.getPort() + "at_address_");
    //         // dir.mkdir();

    //         s1 = new Socket("lin114-04.cise.ufl.edu", 9998);

    //         // dir = new File(workingDir + "/port_to_send_to" + s1.getPort() + "at_address_");
    //         // dir.mkdir();

    //         out = new ObjectOutputStream(s1.getOutputStream());
    //         // out.writeObject(new HandshakeMessage(1001));
    //         // out.flush();
    //         HandshakeMessage outMessage = new HandshakeMessage(peerID);
    //         sendMessage(outMessage);
    //         // dir = new File(workingDir + "/" + peerID + "_out_to_" + inMessage.getPeerID());
    //         // dir.mkdir();

    //         try {
    //             TimeUnit.MILLISECONDS.sleep(10000);
    //         } catch (InterruptedException e) {
    //             e.printStackTrace();
    //         }
    //         break;
    //     }
    // } 
    // // catch (IOException e1) {
    // //     e1.printStackTrace();
    // // } catch (ClassNotFoundException e2) {
    // //     e2.printStackTrace();
    // // } 
    // catch (Exception e) {
    //     // dir = new File(workingDir + "/pp_here1_" + peerID + "_" + e);
    //     // dir.mkdir();
    // } finally {
    //     try {
    //         in.close();
    //         s1.close();
    //     } catch (IOException e3) {
    //         e3.printStackTrace();
    //         // dir = new File(workingDir + "/pp_here2_" + peerID + "_" + e3);
    //         // dir.mkdir();
    //     }
    // }*/

    public void sendHandshake(HandshakeMessage msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch(Exception ioException) { 
            ioException.printStackTrace();
        }
    }

    public static void printThis(String a){
        String workingDir = System.getProperty("user.dir");
        File dir = new File(wDD + "/" + a);
        dir.mkdir();
    }
    
    // Starts up the peerProcess and begins message delivery
    public static void main(String[] args) {
        peerProcess pp = new peerProcess(Integer.parseInt(args[0]));
        pp.createSockets();
        // pp.startProtocol();

    }
}