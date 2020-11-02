package Protocol;

import java.net.*;
import java.io.*;

public class Server {
	private static final int sPort = 1313; 		// the server will be listening on this port number

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running."); 
        ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;

		// listening loop for incoming messages
		try {
			while(true) {
				new Handler(listener.accept(), clientNum).start();
				System.out.println("Client "  + clientNum + " is connected!");
				clientNum++;
			}
		} finally {
				listener.close();
		} 
    }

	/**
     	* A handler thread class.  Handlers are spawned from the listening
     	* loop and are responsible for dealing with a single client's requests.
	*/
	private static class Handler extends Thread {
		private String message;    				// message received from the client
		private String outMessage;    			// message send to the client
		private Socket connection;				// connedtion socket
		private ObjectInputStream in;			// stream read from the socket
		private ObjectOutputStream out;   		// stream write to the socket
		private int no;							// the index number of the client

		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
		}

		public void run() {
			try {
				// initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());

				try {
					while(true) {
						// receive the message sent from the client
						message = (String)in.readObject();
						// show the message to the user
						System.out.println("Receive message: " + message + " from client " + no);
						// TODO: change the following line so that it is more productive
						outMessage = message;
						// send outMessage back to the client
						sendMessage(outMessage);
					}
				} catch(ClassNotFoundException classnot){
						System.err.println("Data received in unknown format");
				}
			} catch(IOException ioException){
					System.out.println("Disconnect with Client " + no);
			} finally {
				// close connections
				try {
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client " + no);
				}
			}
		}

		// send a message to the output stream
		public void sendMessage(String msg) {
			try {
				out.writeObject(msg);
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + no);
			} catch(IOException ioException) {
				ioException.printStackTrace();
			}
		}
    }
}
