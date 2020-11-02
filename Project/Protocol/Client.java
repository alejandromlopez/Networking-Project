package Protocol;

import java.net.*;
import java.io.*;

public class Client {
	Socket requestSocket;           // socket connect to the server
	ObjectOutputStream out;         // stream write to the socket
 	ObjectInputStream in;           // stream read from the socket
	String message;                 // message send to the server
	String inMessage;			    // received message fromt the server

	public void run() {
		try {
			// create a socket to connect to the server
			requestSocket = new Socket("localhost", 1313);
			System.out.println("Connected to localhost in port 1313");
			// initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			while(true) {
				// send the message to the server
				sendMessage(message);
				// receive the message from the server
				inMessage = (String)in.readObject();
				// show the message to the user
				System.out.println("Receive message: " + inMessage);
			}

		} catch (ConnectException e) {
    		System.err.println("Connection refused. You need to initiate a server first.");
		} catch ( ClassNotFoundException e ) {
    		System.err.println("Class not found");
        } catch(UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch(IOException ioException) {
			ioException.printStackTrace();
		} finally {
			// close connections
			try {
				in.close();
				out.close();
				requestSocket.close();
			} catch(IOException ioException) {
				ioException.printStackTrace();
			}
		}
	}

	// send a message to the output stream
	void sendMessage(String msg) {
		try {
			// stream write the message
			out.writeObject(msg);
			out.flush();
		} catch(IOException ioException) {
			ioException.printStackTrace();
		}
	}

	// main method
	public static void main(String args[]) {
		Client client = new Client();
		client.run();
	}

}
