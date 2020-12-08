

/*
 *                     CEN5501C Project2
 * This is the program starting remote processes.
 * This program was only tested on CISE SunOS environment.
 * If you use another environment, for example, linux environment in CISE 
 * or other environments not in CISE, it is not guaranteed to work properly.
 * It is your responsibility to adapt this program to your running environment.
 */

public class RemotePeerInfo {
	public int peerID;
	public String peerAddress;
	public int peerPort;
	
	public RemotePeerInfo(int pId, String pAddress, int pPort) {
		peerID = pId;
		peerAddress = pAddress;
		peerPort = pPort;
	}

	public int getPeerID() {
		return peerID;
	}

	public String getAddress() {
		return peerAddress;
	}

	public int getPort() {
		return peerPort;
	}
}
