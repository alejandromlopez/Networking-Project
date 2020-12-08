import java.net.*;

public class Handler implements Runnable{
    private Socket socket;
    private int peerID;
    private int ID;

    public Handler(Socket s, int pid, int id) {
        socket = s;
        peerID = pid;
        ID = id;
    }

    public void run() {
        HandshakeMessage handshake = new HandshakeMessage(peerID); 
        Writer w = new Writer(handshake, socket, peerID);
        Thread wThread = new Thread(w);
        wThread.start();
    }
}
