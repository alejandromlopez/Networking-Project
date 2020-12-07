import java.net.*;
import java.io.*;
import java.util.HashMap;

public class Listener implements Runnable{
    private ServerSocket server;
    private int peerID;
    private HashMap<Integer, RemotePeerInfo> peers;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean exit;

    public Listener(ServerSocket s, int pid, HashMap<Integer, RemotePeerInfo> p) {
        server = s;
        peerID = pid;
        peers = p;
        exit = false;
        String workingDir = System.getProperty("user.dir");
        File dir = new File(workingDir + "/hiddenPeers_size_for_" + peerID+"_is_"+peers.size());
        dir.mkdir();
    }

    public void run() {
        String workingDir = System.getProperty("user.dir");
        // File dir = new File(workingDir + "/listener_is_listening_" + peerID);
        // dir.mkdir();

        while (true) {
            int pid=-1;
            try {
                if(peers.isEmpty())
                    break;

                socket = server.accept();
                // If we're B listening on 9998
                // this socket that gets created could be (A, 5567)
                // even though A is listening on 9998 themself

                // dir = new File(workingDir + "/listener_after_accept_" + peerID);
                // dir.mkdir();

                in = new ObjectInputStream(socket.getInputStream());
                HandshakeMessage input = (HandshakeMessage)in.readObject();

                // dir = new File(workingDir + "/listener_after_input_" + peerID);
                // dir.mkdir();

                pid = input.getPeerID();
                String address = peers.get(pid).getAddress();
                int port = peers.get(pid).getPort();
                socket.close();

                socket = new Socket(address, port);
                // dir = new File(workingDir + "/listener_after_socket_" + peerID);
                // dir.mkdir();

                out = new ObjectOutputStream(socket.getOutputStream());
                // dir = new File(workingDir + "/listener_after_output_" + peerID);
                // dir.mkdir();

                out.writeObject(new HandshakeMessage(peerID));
                // dir = new File(workingDir + "/listener_after_write_" + peerID);
                // dir.mkdir();
                peers.remove(pid);
            } catch (Exception e) {
                e.printStackTrace();
                // dir = new File(workingDir + "/listener_" + peerID + "_"+ e);
                // dir.mkdir();
            }
        }
    }

    public void exit(){
        exit = true;
    }
}