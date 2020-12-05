import java.net.*;
import java.io.*;
import java.util.HashMap;

public class Listener extends Thread {
    private int peerID;
    private ServerSocket sSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private String message;
    private HandshakeMessage hMessage;
    private HandshakeMessage inHMessage;
    private static HashMap<Integer, Socket> sockets;
    private byte[] b = new byte[32];

    public Listener(ServerSocket sS, int p, HashMap<Integer, Socket> socks) {
        sSocket = sS;
        peerID = p;
        hMessage = new HandshakeMessage(p);
        sockets = socks;
    }

    public void run() {
        String workingDir = System.getProperty("user.dir");
        File dir = new File(workingDir + "/beforeInOUT");
                    dir.mkdir();
        for (Socket s : sockets.values()) {
            try {
                out = new DataOutputStream(s.getOutputStream());
                    File dir2 = new File(workingDir + "28");
                    dir2.mkdir();
                hMessage.write(out);
                    File dir3 = new File(workingDir + "31");
                    dir3.mkdir();
                sSocket.accept();
                    File dir4 = new File(workingDir + "34");
                    dir4.mkdir();
                in = new DataInputStream(s.getInputStream());
                    File dir5 = new File(workingDir + "37");
                    dir5.mkdir();
                in.readFully(b);
                    File dir6 = new File(workingDir + "40");
                    dir6.mkdir();
                message = new String(b);
                    File dir7 = new File(workingDir + "43");
                    dir7.mkdir();
                String rPeerID = message.substring(28);
                    File dir8 = new File(workingDir + "/rPeerID"+rPeerID);
                    dir8.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                    File dir6 = new File(workingDir + peerID +  e);
                    dir6.mkdir();
            }
        }
        

        // try {
        //     out = new DataOutputStream(socket.getOutputStream());
        //     out.flush();
        //     in = new DataInputStream(socket.getInputStream());
        //     try {
        //         System.out.println("here");
        //         // while(true) {
        //         //     message = (String)in.readObject();
        //         //     sendMessage(message);
        //         // }
        //         // message = (String)in.readFully();

        //     } catch (ClassNotFoundException e2) {
        //         e2.printStackTrace();
        //     }
        // } catch (IOException e1) {
        //     e1.printStackTrace();
        // } finally {
        //     try {
        //         in.close();
        //         out.close();
        //         socket.close();
        //     } catch (IOException e3) {
        //         e3.printStackTrace();
        //     }
        // }
    }
}
