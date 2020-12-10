import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.HashMap;

public class Writer implements Runnable {
    private int peerID;
    private int ID;
    private Message message = null;
    private Socket socket;
    private HandshakeMessage handshakeMessage = null;
    private ObjectOutputStream out;
    private byte[] outMessage;
    
    public Writer(Message m, Socket s, int pid) {//, int id) {
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
        //socket = s;
        peerID = pid;
        //ID = id;
    }

    public Writer(HandshakeMessage hm, Socket s, int pid) { //), int id) {
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
            
            int mask = 0xff000000;
            
            if (handshakeMessage != null) {
                outMessage = new byte[32];
                int inPeerID = handshakeMessage.getPeerID();

                for (int i = 0; i < outMessage.length; i++) {
                    if (i < 18) {
                        outMessage[i] = (byte)handshakeMessage.getHandshakeHeader().charAt(i);
                    } else if (i >= 18 && i < 28) {
                        outMessage[i] = 0;
                    } else {
                        outMessage[i] = (byte)(inPeerID & mask);
                        inPeerID <<= 8;
                    }
                }
                System.out.println(peerID + " wrote handshake");
            } else {
                int len = message.getMLength();
                byte[] payload = message.getMPayload();
                outMessage = new byte[5 + len];

                for (int i = 0; i < 5 + len; i++) {
                    if (i < 4) {
                        outMessage[i] = (byte)(len & mask);
                        len <<= 8;
                    } else if (i == 4 ){
                        outMessage[i] = message.getMType();
                    } else {
                        outMessage[i] = payload[i - 5];
                    }
                }
            
                System.out.println(peerID + " wrote message");
            }

            out.write(outMessage);
            out.flush();
            //peerProcess.sockets.put(ID, socket);
        } catch(Exception e) {
            System.out.println(peerID + " writer output error: " + e); 
            e.printStackTrace();
        }
    }
}
