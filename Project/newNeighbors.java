import java.util.HashMap;
import java.util.TimerTask;

public class newNeighbors extends TimerTask
{
    private int prefNeighbors;
    private HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();
    public newNeighbors(int pn, HashMap<Integer, RemotePeerInfo> prs ){
        prefNeighbors = pn;
        peers = prs;
    }

    public void run(){
        for(RemotePeerInfo a : peers.values()){
            
        }
    }
    
}