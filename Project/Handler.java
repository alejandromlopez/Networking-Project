import java.net.*;
import java.io.*;

public class Handler extends Thread {
    private Socket socket;
    private int peerID;
    private int ID;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public Handler(Socket s, int pid, int id) {
        socket = s;
        peerID = pid;
        ID = id;
    }

    public void run() {
        HandshakeMessage handshake = null; 
        String workingDir = System.getProperty("user.dir");
        File dir = new File(workingDir + "/server_" + peerID);
        
        try {
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            handshake = new HandshakeMessage(peerID);
            out.writeObject(handshake);
            out.flush();

            dir = new File(workingDir + "/handler_connections_" + peerID);
            dir.mkdir();
        } catch(Exception e) { 
            e.printStackTrace();
            
            dir = new File(workingDir + "/handler_send_" + e);
            dir.mkdir();
        }

        HandshakeMessage inMessage = null; 
        try {
            inMessage = (HandshakeMessage)in.readObject();
            if (inMessage.getHandshakeHeader().equals("P2PFILESHARINGPROJ") && inMessage.getPeerID() == ID) {
                out.writeObject(handshake);
                out.flush();

                dir = new File(workingDir + "/handler_out_success_" + peerID);
                dir.mkdir();
            }
        } catch (Exception e2) {
            e2.printStackTrace();

            dir = new File(workingDir + "/handler_rcv_" + e2);
            dir.mkdir();
        }


    //     String workingDir = System.getProperty("user.dir");
    //     File dir = new File(workingDir);

    //     try {
    //         in = new ObjectInputStream(socket.getInputStream());
    //         out = new ObjectOutputStream(socket.getOutputStream());
    //     } catch (Exception e) {
    //         dir = new File(workingDir + "/here1_" + peerID + "_" + e);
    //         dir.mkdir();
    //     }

    //     HandshakeMessage inMessage = null;
    //     try {
    //         inMessage = (HandshakeMessage) in.readObject();
    //     } catch (ClassNotFoundException e1) {
    //         e1.printStackTrace();
    //     } catch (IOException e1) {
    //         e1.printStackTrace();
    //     }

    //     dir = new File(workingDir + "/inmsg_" + peerID);
    //     dir.mkdir();

    //     while (true) {
    //         try {
    //             if (inMessage == null || inMessage.getZeroBits().equals("1111111111")) {
    //                 break;
    //             } else {
    //                 if (inMessage.getHandshakeHeader().equals("P2PFILESHARINGPROJ")
    //                         && (inMessage.getPeerID() == 1003 || inMessage.getPeerID() == 1001)) {
    //                     dir = new File(workingDir + "/" + peerID + "_rcvd_" + inMessage.getPeerID());
    //                     dir.mkdir();

    //                     HandshakeMessage outMessage = new HandshakeMessage(peerID);
    //                     try {
    //                         out.writeObject(outMessage);
    //                         out.flush();

    //                         dir = new File(workingDir + "/" + peerID + "_out_to_" + inMessage.getPeerID());
    //                         dir.mkdir();
    //                     } catch (Exception e2) {
    //                         dir = new File(workingDir + "/here2_" + peerID + "_" + e2);
    //                         dir.mkdir();
    //                     }
    //                 } else {
    //                     break;
    //                 }
    //             }
    //         } catch (Exception e) {
    //             dir = new File(workingDir + "/here3_" + peerID + "_" + e);
    //             dir.mkdir();
    //         }
    //     }
    //     try {
    //         socket.close();
    //     } catch (IOException e) {
    //         dir = new File(workingDir + "/" + peerID + "_socket_closed");
    //         dir.mkdir();
    //     }
    // }
    }
}
