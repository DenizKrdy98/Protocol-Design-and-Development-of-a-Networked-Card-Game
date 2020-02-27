import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Each time a follower connects to the master, a FollowerHandler thread is created for
 * communication with the follower. Sends .json gamestate files to the follower, per follower's request
 * The follower checks the integrity of those files and may request the same files if there was a transmission
 * issue
 */
class FollowerHandler extends Thread {

    public static final String CONSISTENCY_CHECK_PASS = "CONSISTENCY_CHECK_PASS";
    public static final String RETRANSMIT = "RETRANSMIT";
    public static final String FILE_PATH = "./src/main/Master Files/";
    private HashMap<File, Long> fileHashMap;  // file, lastmodified

    private static final Logger LOGGER = Logger.getLogger(FollowerHandler.class.getName());
    SimpleFormatter formatter;
    FileHandler fileHandler;

    DataInputStream dataIn;
    DataOutputStream dataOut;
    DataInputStream commandIn;
    DataOutputStream commandOut;

    Socket data;
    Socket command;

    int followerNo;

    /**
     * Initializes the fields and logger.
     * @param dataIn DataInputStream
     * @param dataOut DataOutputStream
     * @param commandIn DataInputStream
     * @param commandOut DataOutputStream
     * @param data Socket
     * @param command Socket
     * @param followerNo int
     */
    public FollowerHandler(DataInputStream dataIn, DataOutputStream dataOut, DataInputStream commandIn, DataOutputStream commandOut, Socket data, Socket command, int followerNo) {
        this.dataIn = dataIn;
        this.dataOut = dataOut;
        this.commandIn = commandIn;
        this.commandOut = commandOut;
        this.data = data;
        this.command = command;
        fileHashMap = new HashMap<File, Long>();
        this.followerNo = followerNo;
        formatter = new SimpleFormatter();
        try {
            fileHandler = new FileHandler("followerhandler" + followerNo  + ".log", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileHandler.setFormatter(formatter);
        LOGGER.addHandler(fileHandler);
    }

    /**
     * Writes the followerNo to commandOut stream
     * Keeps checking for messages from the follower.
     * If send command is received , calls send file
     */
    @Override
    public void run() {
        try {
            commandOut.writeInt(followerNo);

            while (true) {
                String option = commandIn.readUTF();
                if (option.equals("SEND")) {
                    System.out.println("SEND Command Received..");
                    this.sendFile(data, command);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calls the overloaded sendFile method with the changedFiles() method
     * @param data socket
     * @param command socket
     * @throws Exception
     */
    public void sendFile(Socket data, Socket command) throws Exception {
        sendFile(data, command, changedFiles());
    }

    /**
     * Date streams are created using the given ports. changedFiles array size is sent, and
     * then the files are sent one by one to the follower. After sending a file, its hash string
     * is sent using the HashMethods class' gethHashString method.
     * After all files are sent, receives the final message. If there are no errors in transmission,
     * we receive the CONSISTENCY_CHECK_PASSED message and we're done. Otherwise, we receive the RETRANSMIT
     * message and get the number of corrupted files and their names. We call the method again using those files
     * as the changedFiles arraylist
     *
     * @param data socket for communication with the follower. Files are sent through this socket
     * @param command socket for communication with the follower. Messages are sent through this socket
     * @param changedFiles ArrayList<File> of changed files in the directory
     * @throws Exception
     */
    public void sendFile(Socket data, Socket command, ArrayList<File> changedFiles) throws Exception {
        DataInputStream cin = new DataInputStream(data.getInputStream());
        DataOutputStream cout = new DataOutputStream(data.getOutputStream());
        DataInputStream commandIn = new DataInputStream(command.getInputStream());
        DataOutputStream commandOut = new DataOutputStream(command.getOutputStream());

        cout.writeUTF(Integer.toString(changedFiles.size()));

        for (File f : changedFiles) {
            //String filename = "./src/main/Master Files/test.txt"; // just trying it out
            //File f = new File(fileName);
            String fileName = f.getName();
            cout.writeUTF(fileName); // write fileName
            FileInputStream fin = new FileInputStream(f);
            int ch;
            do {
                ch = fin.read();
                cout.writeUTF(Integer.toString(ch));
            } while (ch != -1);
            fin.close();
            System.out.println("File Sent");

            commandOut.writeUTF(HashMethods.gethHashString(FILE_PATH + fileName));
        }

        String finalMsg = commandIn.readUTF();
        System.out.println(finalMsg);

        LOGGER.log(Level.INFO, finalMsg);
        if (finalMsg.equals(CONSISTENCY_CHECK_PASS))
            return;
        else if (finalMsg.equals(RETRANSMIT)) {
            int noCorrupted = commandIn.readInt();
            changedFiles = new ArrayList<>();
            for (int i = 0; i < noCorrupted; i++) {
                String fileName = commandIn.readUTF();
                changedFiles.add(new File("./src/main/Master Files/" + fileName));
            }
            sendFile(data, command, changedFiles);
        }
    }

    /**
     * Checks the Master Files folder for changed files. Returns changed files in an ArrayList
     * @return ArrayList<File>. List of changed files
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public ArrayList<File> changedFiles() throws NoSuchAlgorithmException, IOException {
        File folder = new File("./src/main/Master Files/");
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> changedFiles = new ArrayList<File>();

        /*for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }*/

        for (File f : listOfFiles) {
            if (!fileHashMap.containsKey(f) || (fileHashMap.containsKey(f) && fileHashMap.get(f) != f.lastModified())) {
                fileHashMap.put(f, f.lastModified());
                changedFiles.add(f);
            }
        }

        return changedFiles;
    }

}