import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Writer implements Runnable {
    private int peerID;
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
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println(e);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println(e);
        }

        socket = nSocket;
        peerID = pid;
    }

    public Writer(HandshakeMessage hm, Socket s, int pid) {
        handshakeMessage = hm;
        socket = s;
        peerID = pid;
    }

    public void sendMessage() {
        try {
            // System.out.println(peerID + " starting output stream");
            out = new ObjectOutputStream(socket.getOutputStream());
            // System.out.println(peerID + " created output stream");
            
            if (handshakeMessage != null) {
                out.writeObject(handshakeMessage);
                System.out.println(peerID + " writing handshake");
            } else {
                System.out.println(peerID + " writing message to port " + socket.isClosed());
                out.writeObject(message);
                System.out.println(peerID + " wrote message");
            }

            out.flush();
            //out.close();
        } catch(Exception e) {
            System.out.println(peerID + " writer output error: " + e); 
            e.printStackTrace();
        }
    }

    public void run() {
        sendMessage();
    }
}
