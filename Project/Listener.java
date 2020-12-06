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
        // for (Socket s : sockets.values()) {
        //     try {
        //         // while(true) {
        //         //     message = (String)in.readObject();
        //         //     sendMessage(message);
        //         // }
        //         message = (String)in.readObject();
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
