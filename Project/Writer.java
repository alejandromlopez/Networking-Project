import java.io.ObjectOutputStream;
import java.net.Socket;

public class Writer implements Runnable {
    private int peerID;
    private Message message = null;
    private Socket socket;
    private HandshakeMessage handshakeMessage = null;
    private ObjectOutputStream out;
    
    public Writer(Message m, Socket s, int pid) {
        message = m;
        socket = s;
        peerID = pid;
    }

    public Writer(HandshakeMessage hm, Socket s, int pid) {
        handshakeMessage = hm;
        socket = s;
        peerID = pid;
    }

    public void sendMessage(HandshakeMessage hm) {
        try {
            System.out.println(peerID + " starting output stream");
            out = new ObjectOutputStream(socket.getOutputStream());
            System.out.println(peerID + " created output stream");
            
            if (message == null) {
                out.writeObject(handshakeMessage);
                System.out.println(peerID + " writing handshake");
            } else {
                out.writeObject(message);
                System.out.println(peerID + " writine message");
            }

            out.flush();
        } catch(Exception e) {
            System.out.println(peerID + " writer output error: " + e); 
            e.printStackTrace();
        }
    }

    public void run() {
        sendMessage(handshakeMessage);
    }
}
