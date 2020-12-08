import java.util.HashMap;
import java.util.TimerTask;

public class newNeighbors extends TimerTask
{
    private int prefNeighbors;
    private int unchockingInterval;
    private HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();
    private HashMap<Integer, Integer> rates= new HashMap<Integer, Integer>();
    private byte[] bitfield;
    private boolean complete;
    private int peerID;
    private int numLeftover;
    private boolean areLeftovers;
    private peerProcess pp;

    public newNeighbors(int pn, int interval, HashMap<Integer, RemotePeerInfo> prs, byte[] b, int pid, boolean leftover, int numLeft, peerProcess p){
        prefNeighbors = pn;
        unchockingInterval = interval;
        peers = prs;
        bitfield=b;
        peerID = pid;
        areLeftovers = leftover;
        numLeftover = numLeft;
        pp = p;

    }

    public void run(){
        System.out.println(pp.getTester());
        System.out.println(pp.getPieceData().values());

        for (RemotePeerInfo p : peers.values()){
            int numPieces = pp.getPieceData().get(p.getPeerID());
        }
        //pp.getTester() = false;

        //Used to assign complete.
        for (int i = 0; i < bitfield.length; i++){
            complete = true;
            if (areLeftovers){
                int byteNum = 0;
                for (int j = 1; j <= numLeftover; j++) {
                    byteNum += (int) Math.pow(2, 8 - j);
                }
                if (i < bitfield.length-1){
                    if (bitfield[i]==((byte)-1)){
                        continue;
                    } else {
                        complete = false;
                        break;
                    }
                } else {
                    if (bitfield[i]==((byte)byteNum)){
                        continue;
                    } else {
                        complete = false;
                        break;
                    }
                }
            } else {
                if (bitfield[i]==((byte)-1)){
                    continue;
                } else {
                    complete = false;
                    break;
                }
            }
        }
        System.out.println("");

        if (!complete){
            //Compute neighbors by download rate.
            for(RemotePeerInfo a : peers.values()){
                
            }
        } else {
            //Compute neighbors randomly.

        }
    }
    
}