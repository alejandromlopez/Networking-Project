import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class Reader implements Runnable {
    private int peerID;
    private ObjectInputStream in;
    private Message message;
    private HandshakeMessage handshakeMessage;
    private Socket socket;

    public Reader(Socket s, int pid) {
        socket = s;
        peerID = pid;
    }

    public Message getMessage() {
        return message;
    }

    public HandshakeMessage getHandshakeMessage() {
        return handshakeMessage;
    }

    public void run() {
        try {
            System.out.println(peerID + " reading");
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println(peerID + " RECEIVED MESSAGE!");
            Object obj = in.readObject();
            System.out.println(peerID + " READ MESSAGE!");

            if (obj instanceof HandshakeMessage) {
                handshakeMessage = (HandshakeMessage)obj;
                System.out.println(peerID + " received handshake from " + handshakeMessage.getPeerID());
            } else if (obj instanceof Message) {
                message = (Message)obj;
                System.out.println(peerID + " received message from " + handshakeMessage.getPeerID());
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(peerID + " " + e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println(peerID + " reader output error: " + e);
        }

    }
}
