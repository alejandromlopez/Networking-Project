import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimerTask;

public class newNeighbors implements Runnable {
    private int prefNeighbors;
    private int unchockingInterval;
    private HashMap<Integer, RemotePeerInfo> peers = new HashMap<Integer, RemotePeerInfo>();
    private HashMap<Integer, Double> rates = new HashMap<Integer, Double>();
    private HashMap<Integer, Double> hrates = new HashMap<Integer, Double>();
    private HashMap<Integer, Boolean> isChoked = new HashMap<Integer, Boolean>();
    private byte[] bitfield;
    private boolean complete;
    private int peerID;
    private int numLeftover;
    private boolean areLeftovers;
    private peerProcess pp;
    private int[] highPeers;
    private double[] highRates;
    private ArrayList<Integer> newPrefNeighbor;
    private EventLog peerlog;

    public newNeighbors(int pn, int interval, HashMap<Integer, RemotePeerInfo> prs, byte[] b, int pid, boolean leftover,
            int numLeft, peerProcess p, EventLog pl) {
        prefNeighbors = pn;
        unchockingInterval = interval;
        peers = prs;
        bitfield = b;/////////////////////////////
        peerID = pid;
        areLeftovers = leftover;
        numLeftover = numLeft;
        pp = p;
        highPeers = new int[pn];
        highRates = new double[pn];
        peerlog = pl;
        newPrefNeighbor  = new ArrayList<Integer>();
    }

    

    public void run(){
        // System.out.println("");
        // System.out.print("Bitfield at top of newNeigh is ");
        // for (int i = 0; i < pp.getBitField().length; i++)
        //     System.out.print( pp.getBitField()[i]+ " ");
        // System.out.println("");
        //populates rates.
        for (RemotePeerInfo p : peers.values()){
            int numPieces = pp.getPieceData().get(p.getPeerID());
            double rate = (double)numPieces / unchockingInterval;
            if (p.getPeerID()!=peerID)
                rates.put(p.getPeerID(), rate);
        }

        //Used to assign complete.
        for (int i = 0; i < pp.getBitField().length; i++){
            complete = true;
            if (areLeftovers){
                int byteNum = 0;
                for (int j = 1; j <= numLeftover; j++) {
                    byteNum += (int) Math.pow(2, 8 - j);
                }
                if (i < pp.getBitField().length-1){
                    if (pp.getBitField()[i]==((byte)-1)){
                        continue;
                    } else {
                        complete = false;
                        break;
                    }
                } else {
                    if (pp.getBitField()[i]==((byte)byteNum) || pp.getBitField()[i] == ((byte)-1)){
                        continue;
                    } else {
                        complete = false;
                        break;
                    }
                }
            } else {
                if (pp.getBitField()[i]==((byte)-1)){
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
                boolean notinputed = true;
                for (int i = 0; i < highRates.length; i++){
                    if(rates.get(p)>highRates[i] && pp.getPeersInterestedInMe().get(p)){
                        for (int j = i; j < highRates.length-1; i++){
                            highRates[j+1] = highRates[j];
                            highPeers[j+1] = highPeers[j];
                        }
                        highRates[i] = rates.get(p);
                        highPeers[i] = p;
                        break;
                    } else if(rates.get(p)==highRates[i] && rates.get(p) == 0  && highPeers[i] == 0 && notinputed){
                        //highRates[i] = rates.get(p);
                        highPeers[i] = p;
                        notinputed = false;
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
                hrates.put(highPeers[i], highRates[i]);
            }

            // System.out.print("HighPeers has ");
            // for (int i = 0; i < highPeers.length; i ++){
            //     System.out.print(highPeers[i] + " ");
            // }
            
            for (int i = 0; i < highRates.length; i++){
                newPrefNeighbor.add(highPeers[i]);
                Unchoke unchoke = new Unchoke(peerID);
                Writer w = new Writer(unchoke, pp.getSockets().get(highPeers[i]), peerID);
                Thread t = new Thread(w);
                t.start();
            }
            // System.out.print("newPrefNeighbor has ");
            // for (int i = 0; i < newPrefNeighbor.size(); i ++){
            //     System.out.print(newPrefNeighbor.get(i) + " ");
            // }
            peerlog.changeOfPrefNeighbor(newPrefNeighbor);
            newPrefNeighbor = new ArrayList<Integer>();
            highPeers = new int[prefNeighbors];
            highRates = new double[prefNeighbors];


            for (int p : peers.keySet()){
                if ((hrates.containsKey(p) || pp.getCurrentOptUnchoked() == p) || p == peerID){
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

            System.out.println("I MADE IT TO THE RANDOM PEER SELECTION");
            HashMap<Integer, Integer> sentAlready = new HashMap<Integer, Integer>();
            // pp.getPeersInterestedInMe();
            int[] peerIDInInterested = new int[pp.getPeersInterestedInMe().size()];
            int idx = 0;
            for(int p : pp.getPeersInterestedInMe().keySet()){
                peerIDInInterested[idx] = p;
                idx++;
            }
            System.out.println(peerIDInInterested.length);

            for (int i = 0; i < prefNeighbors; i++){
                while(true){
                    int ran = (int) (Math.random()*peerIDInInterested.length);
                    System.out.println(ran);
                    
                    if(!sentAlready.containsKey(peerIDInInterested[ran])){
                        System.out.println("MADE IT INSIDE");
                        newPrefNeighbor.add(peerIDInInterested[ran]);
                        Unchoke unchoke = new Unchoke(peerID);
                        Writer w = new Writer(unchoke, pp.getSockets().get(peerIDInInterested[ran]), peerID);
                        Thread t = new Thread(w);
                        t.start();
                        sentAlready.put(peerIDInInterested[ran], 0);
                        System.out.println("Before Break");
                        break;
                    }

                }
                System.out.println(i);
            }
            peerlog.changeOfPrefNeighbor(newPrefNeighbor);
            newPrefNeighbor = new ArrayList<Integer>();

            int[] notSent = new int[pp.getPeersInterestedInMe().size() - sentAlready.size()];
            int count=0;
            for (int p : pp.getPeersInterestedInMe().keySet()){
                if (!sentAlready.containsKey(p) && pp.getCurrentOptUnchoked() != p){
                    notSent[count]=p;
                }
                count++;
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
        //pp.setInterestedInMe(reset);
        pp.setIsChoke(isChoked);
    }
    
}