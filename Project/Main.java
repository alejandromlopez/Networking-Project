import java.util.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        // File path to PeerInfo.cfg to read from
        String workingDir = System.getProperty("user.dir");
        String fileName = workingDir + "/Project/PeerInfo.cfg";
        ArrayList<Integer> keys = new ArrayList<Integer>();

        // Reading the file line by line
        try {
            Scanner scanner = new Scanner(new File(fileName));

            //Grabbing the peerID from each line
            while (scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split(" ");
                keys.add(Integer.parseInt(line[0]));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Creates threads for each peerProcess in the order of the peerIDs from PeerInfo.cfg
        ArrayList<Thread> peerProcesses = new ArrayList<Thread>();
        for (int key: keys) { 
            Thread t = new Thread(new peerProcess(key));
            peerProcesses.add(t); 
            t.start(); 
        } 
    }
}