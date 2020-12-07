import java.net.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class Handler implements Runnable{
    private Socket socket;
    private int peerID;
    private int ID;
    private boolean exit;

    public Handler(Socket s, int pid, int id) {
        socket = s;
        peerID = pid;
        ID = id;
        exit = false;
    }

    public void run() {
        HandshakeMessage handshake = new HandshakeMessage(peerID); 
        Writer w = new Writer(handshake, socket, peerID);
        Thread wThread = new Thread(w);
        wThread.start();

        Reader r = new Reader(socket, peerID);
        Thread rThread = new Thread(r);
        rThread.start();

        HandshakeMessage inMessage = r.getHandshakeMessage(); 
        try {
            peerProcess.printThis(peerID + " received from " + inMessage.getPeerID() + " in Handler");

            if (inMessage.getHandshakeHeader().equals("P2PFILESHARINGPROJ") && inMessage.getPeerID() == ID) {
                System.out.println(peerID + " received the right handshake message!");
            }
        } catch (Exception e2) {
            //e2.printStackTrace();
            System.out.println(peerID + " handler input error: " + e2);
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

    public void exit(){
        exit = true;
    }
}
