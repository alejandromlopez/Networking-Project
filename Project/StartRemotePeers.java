import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/*
 * The StartRemotePeers class begins remote peer processes. 
 * It reads configuration file PeerInfo.cfg and starts remote peer processes.
 */
public class StartRemotePeers {

	private Vector<RemotePeerInfo> peerInfoVector;

	private static final String username = "alopez";
	
	public void getConfiguration()
	{
		// System.out.println("Put your username for cise here on line 23 and comment me out");
		String st;
		peerInfoVector = new Vector<RemotePeerInfo>();

		try {
			String workingDir = System.getProperty("user.dir");
			BufferedReader in = new BufferedReader(new FileReader(workingDir + "/PeerInfo.cfg"));
			while((st = in.readLine()) != null) {
				 String[] tokens = st.split("\\s+");
			     peerInfoVector.addElement(new RemotePeerInfo(tokens[0], tokens[1], tokens[2]));
			}
			
			in.close();
		} catch (Exception ex) {
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
			String workingDir = "Desktop/Project";

			// start clients at remote hosts
			for (int i = 0; i < myStart.peerInfoVector.size(); i++) {
				RemotePeerInfo pInfo = (RemotePeerInfo) myStart.peerInfoVector.elementAt(i);
				
				System.out.println("Start remote peer " + pInfo.peerId 
								   + " at " + pInfo.peerAddress);

				Runtime.getRuntime().exec("ssh " + username 
										  + "@" + pInfo.peerAddress 
										  + " && cd " + workingDir 
										  + " && java peerProcess " + pInfo.peerId);
				//TimeUnit.MILLISECONDS.sleep(100);
			}
			System.out.println("Starting all remote peers has done." );
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
