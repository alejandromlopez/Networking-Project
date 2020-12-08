import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;

public class Writer implements Runnable {
    private int peerID;
    //private int ID;
    private Message message = null;
    private Socket socket;
    private HandshakeMessage handshakeMessage = null;
    private ObjectOutputStream out;
    
    public Writer(Message m, Socket s, int pid) {
        message = m;
        Socket nSocket = null;
        try {
            nSocket = new Socket(s.getInetAddress(), s.getPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.out.println(e);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e);
        }

        socket = nSocket;
        peerID = pid;
        //ID = id;
    }

    public Writer(HandshakeMessage hm, Socket s, int pid) {
        handshakeMessage = hm;
        socket = s;
        peerID = pid;
        //ID = id;
    }

    public void run() {
        try {
            //System.out.println(peerID + " starting output stream");
            //System.out.println(peerID + " socket is closed is equal to " + socket.isClosed());
            out = new ObjectOutputStream(socket.getOutputStream());
            //System.out.println(peerID + " created output stream");
            
            if (handshakeMessage != null) {
                out.writeObject(handshakeMessage);
                //System.out.println(peerID + " wrote handshake");
            } else {
                out.writeObject(message);
                //System.out.println(peerID + " wrote message");
            }

            out.flush();
            //peerProcess.sockets.put(ID, socket);
        } catch(Exception e) {
            System.out.println(peerID + " writer output error: " + e); 
            e.printStackTrace();
        }
    }
}
