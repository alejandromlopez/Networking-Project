import java.util.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        String workingDir = System.getProperty("user.dir");
        String fileName = workingDir + "/Project/PeerInfo.cfg";
        ArrayList<Integer> keys = new ArrayList<Integer>();
        
        try {
            Scanner scanner = new Scanner(new File(fileName));
            while (scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split(" ");
                keys.add(Integer.parseInt(line[0]));
            }
            scanner.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (int key : keys) {
            peerProcess pp = new peerProcess(key);
        }
    }
}