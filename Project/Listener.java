import java.net.*;
import java.io.*;

public class Listener extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String message;

    public Listener(Socket s) {
        socket = s;
    }

    public void sendMessage(String message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            try {
                System.out.println("here");
                // while(true) {
                //     message = (String)in.readObject();
                //     sendMessage(message);
                // }
                message = (String)in.readObject();
            } catch (ClassNotFoundException e2) {
                e2.printStackTrace();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e3) {
                e3.printStackTrace();
            }
        }
    }
}
