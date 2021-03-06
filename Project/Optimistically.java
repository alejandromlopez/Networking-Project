import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;

public class Optimistically extends TimerTask{
    
    private static HashMap<Integer, Boolean> isChoked = new HashMap<Integer, Boolean>();
    private ArrayList<Integer> currentlyChoked = new ArrayList<Integer>();
    private peerProcess pp;
    private int peerID;
    private EventLog peerlog;
    
    
    public Optimistically(HashMap<Integer, Boolean> choke, peerProcess p, int pid, EventLog pl){
        isChoked = choke;
        pp = p;
        peerID = pid;
        peerlog = pl;
    }

    public void run(){
        for (int pid : isChoked.keySet()){
            currentlyChoked.add(pid);
        }

        if (currentlyChoked.size() != 0){
            int ran = (int) (Math.random() * isChoked.size());
            Unchoke unchoke = new Unchoke(currentlyChoked.get(ran));
            Writer w = new Writer(unchoke, pp.getSockets().get(currentlyChoked.get(ran)), currentlyChoked.get(ran));
            Thread t = new Thread(w);
            t.start();
            pp.setCurrentOptUnchoked(currentlyChoked.get(ran));
            peerlog.changeOfOptUnchkNeighbor(currentlyChoked.get(ran));
        }
    }
}
