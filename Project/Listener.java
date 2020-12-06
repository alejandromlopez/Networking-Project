import java.net.*;
import java.io.*;
import java.util.HashMap;

public class Listener extends Thread {
    private ServerSocket server;
    private int peerID;
    private HashMap<Integer, RemotePeerInfo> peers;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Listener(ServerSocket s, int pid, HashMap<Integer, RemotePeerInfo> p) {
        server = s;
        peerID = pid;
        peers = p;
    }

    public void run() {
        String workingDir = System.getProperty("user.dir");
        File dir = new File(workingDir + "/server_" + peerID);

        while (true) {
            try {
                socket = server.accept();
                in = new ObjectInputStream(socket.getInputStream());
                HandshakeMessage input = (HandshakeMessage)in.readObject();

                int pid = input.getPeerID();
                String address = peers.get(pid).getAddress();
                int port = peers.get(pid).getPort();

                socket = new Socket(address, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(new HandshakeMessage(peerID));
            } catch (Exception e) {
                e.printStackTrace();
                dir = new File(workingDir + "/listener_" + e);
                dir.mkdir();
            }
        }
    }
}