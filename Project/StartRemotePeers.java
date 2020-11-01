
/*
 *                     CEN5501C Project2
 * This is the program starting remote processes.
 * This program was only tested on CISE SunOS environment.
 * If you use another environment, for example, linux environment in CISE 
 * or other environments not in CISE, it is not guaranteed to work properly.
 * It is your responsibility to adapt this program to your running environment.
 */

import java.io.*;
import java.util.*;

/*
 * The StartRemotePeers class begins remote peer processes. 
 * It reads configuration file PeerInfo.cfg and starts remote peer processes.
 * You must modify this program a little bit if your peer processes are written in C or C++.
 * Please look at the lines below the comment saying IMPORTANT.
 */
public class StartRemotePeers {

	public Vector<RemotePeerInfo> peerInfoVector;
	public static String username = "";
	
	public void getConfiguration()
	{
		System.out.println("Hey dummy, put your username and passsord for cise here on line 23 and 24 and comment me out");
		String st;
		peerInfoVector = new Vector<RemotePeerInfo>();
		try {
			String workingDir = System.getProperty("user.dir");
			BufferedReader in = new BufferedReader(new FileReader(workingDir + "/PeerInfo.cfg"));
			while((st = in.readLine()) != null) {
				
				 String[] tokens = st.split("\\s+");
		    	 //System.out.println("tokens begin ----");
			     //for (int x=0; x<tokens.length; x++) {
			     //    System.out.println(tokens[x]);
			     //}
		         //System.out.println("tokens end ----");
			    
			     peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
			
			}
			
			in.close();
		}
		catch (Exception ex) {
			System.out.println(ex.toString());
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			StartRemotePeers myStart = new StartRemotePeers();
			myStart.getConfiguration();

			// start clients at remote hosts
			ProcessBuilder pb = new ProcessBuilder("/bin/bash");

			for (int i = 0; i < myStart.peerInfoVector.size(); i++) {
				RemotePeerInfo pInfo = (RemotePeerInfo) myStart.peerInfoVector.elementAt(i);
				
				System.out.println("Start remote peer " + pInfo.peerId +  " at " + pInfo.peerAddress );
				
				pb.command("ssh", username + "@" + pInfo.peerAddress);
				Process p = pb.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}
				
				int x = p.waitFor();
				System.out.println(x);
			}
			System.out.println("Starting all remote peers has done." );
		}
		catch (Exception ex) {
			System.out.println(ex);
		}
	}
}