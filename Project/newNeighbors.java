import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.TimerTask;

public class newNeighbors extends TimerTask {
    private int prefNeighbors;
    private int unchockingInterval;
    private HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();
    private HashMap<Integer, Double> rates = new HashMap<Integer, Double>();
    private HashMap<Integer, Boolean> isChoked = new HashMap<Integer, Boolean>();
    private byte[] bitfield;
    private boolean complete;
    private int peerID;
    private int numLeftover;
    private boolean areLeftovers;
    private peerProcess pp;
    private int[] highPeers;
    private double[] highRates;

    public newNeighbors(int pn, int interval, HashMap<Integer, RemotePeerInfo> prs, byte[] b, int pid, boolean leftover,
            int numLeft, peerProcess p) {
        prefNeighbors = pn;
        unchockingInterval = interval;
        peers = prs;
        bitfield = b;
        peerID = pid;
        areLeftovers = leftover;
        numLeftover = numLeft;
        pp = p;
        highPeers = new int[pn];
        highRates = new double[pn];
    }

    

    public void run(){

        //populates rates.
        for (RemotePeerInfo p : peers.values()){
            int numPieces = pp.getPieceData().get(p.getPeerID());
            double rate = (double)numPieces / unchockingInterval;
            if (p.getPeerID()!=peerID)
                rates.put(p.getPeerID(), rate);
        }

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
            for(int p : rates.keySet()){
                for (int i = 0; i < highRates.length; i++){
                    if(rates.get(p)>highRates[i] && pp.getPeersInterestedInMe().get(p)){
                        for (int j = i; j < highRates.length-1; i++){
                            highRates[j+1] = highRates[j];
                            highPeers[j+1] = highPeers[j];
                        }
                        highRates[i] = rates.get(p);
                        highPeers[i] = p;
                        break;
                    } else if (rates.get(p)==highRates[i] && i == (highRates.length-1) && pp.getPeersInterestedInMe().get(p)){
                        double a = Math.random();
                        if (a > 0.5){
                            highRates[i] = rates.get(p);
                            highPeers[i] = p;
                        }
                    }
                }
            }
            for (int i = 0; i < highRates.length; i++){
                rates.replace(highPeers[i], highRates[i]);
            }

            for (int p : rates.keySet()){
                Unchoke unchoke = new Unchoke(peerID);
                Writer w = new Writer(unchoke, pp.getSockets().get(p), peerID);
                Thread t = new Thread(w);
                t.start();
            }

            for (int p : peers.keySet()){
                if ((rates.containsKey(p) || pp.getCurrentOptUnchoked() == p) || p == peerID){
                    continue;
                }
                Choke choke = new Choke(peerID);
                Writer w = new Writer(choke, pp.getSockets().get(p), peerID);
                Thread t = new Thread(w);
                t.start();
                isChoked.put(p, true);
            }

        } else {
            //Compute neighbors randomly.
            HashMap<Integer, Integer> sentAlready = new HashMap<Integer, Integer>();
            // pp.getPeersInterestedInMe();
            int[] peerIDInInterested = new int[pp.getPeersInterestedInMe().size()];
            int idx = 0;
            for(int p : pp.getPeersInterestedInMe().keySet()){
                peerIDInInterested[idx] = p;
                idx++;
            }
            for (int i = 0; i < prefNeighbors; i++){
                while(true){
                    int ran = (int) Math.random()*prefNeighbors;
                    
                    if(!sentAlready.containsKey(peerIDInInterested[ran])){
                        Unchoke unchoke = new Unchoke(peerID);
                        Writer w = new Writer(unchoke, pp.getSockets().get(peerIDInInterested[ran]), peerID);
                        Thread t = new Thread(w);
                        t.start();
                        sentAlready.put(peerIDInInterested[ran], 0);
                        break;
                    }
                }
            }
            int[] notSent = new int[pp.getPeersInterestedInMe().size() - sentAlready.size()];
            int count=0;
            for (int p : pp.getPeersInterestedInMe().keySet()){
                if (!sentAlready.containsKey(p) && pp.getCurrentOptUnchoked() != p){
                    notSent[count]=p;
                }
            }

            for (int i = 0; i < notSent.length; i++){
                if (notSent[i]==0){
                    continue;
                }
                Choke choke = new Choke(peerID);
                Writer w = new Writer(choke, pp.getSockets().get(notSent[i]), peerID);
                Thread t = new Thread(w);
                t.start();
                isChoked.put(notSent[i], true);
            }
        }

        

        //Reset Interests
        HashMap<Integer, Boolean> reset = new HashMap<Integer, Boolean>();
        for (int p : pp.getPeersInterestedInMe().keySet()){
            reset.put(p, false);
        }
        pp.setInterestedInMe(reset);
        pp.setIsChoke(isChoked);
    }
    
}