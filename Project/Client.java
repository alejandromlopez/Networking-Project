
import java.net.*;
import java.io.*;

public class Client extends Thread {
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Socket socket;
    private final String hostname;
    private final int port;
    private String message;

    public Client(String h, int p) {
        hostname = h;
        port = p;
    }

    public void sendMessage() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
 
            output.writeObject(message);
            output.flush();

            System.out.println("Send message: " + message + " to Server ");
        } catch(IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void setMessage(String m) {
        message = m;
    }

    public void run() {
        try {
            socket = new Socket(hostname, port);
            input = new ObjectInputStream(socket.getInputStream());
            sendMessage();
            System.out.println("Client sent: " + message);

            String in = (String)input.readObject();
            System.out.println("Client received: " + in);

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } 
        // finally {
        //     try {
		// 		input.close();
		// 		output.close();
		// 		socket.close();
		// 	} catch(IOException ioException) {
		// 		ioException.printStackTrace();
		// 	}
        // }
    }
}