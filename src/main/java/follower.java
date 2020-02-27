import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The class of the follower. Used to back up the main server
 * In every TIME_INTERVAL, recieves the .json files from the master which keep the state of the game
 * Checks the integrity of those files and requests the corrupted files until all files are received
 * properly
 *
 */
public class follower {
    public static final String CONSISTENCY_CHECK_PASS = "CONSISTENCY_CHECK_PASS";
    public static final String RETRANSMIT = "RETRANSMIT";
    public static final int TIME_INTERVAL = 30;
    public static props props1 = props.getInstance();
    public static final int DATA_PORT = props1.getDataPort();
    public static final int COMMAND_PORT = props1.getCommandPort();
    static String ip= props1.getMasterIP();

    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private static String followerPath = "./src/main/Follower Files ";

    private static final Logger LOGGER = Logger.getLogger( follower.class.getName() );
    static SimpleFormatter formatter;
    static FileHandler fileHandler;

    follower(){
        formatter = new SimpleFormatter();

    }

    /**
     * Initializes the input and output streams and sockets for communication with the master (and then the followerhandler)
     * Notifies the master that this is a follower, creates the necessary directories and schedules the operation.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String option;
        DataInputStream in=new DataInputStream(System.in);
        Socket commandSocket=new Socket(ip, COMMAND_PORT);

        final DataOutputStream commandOut=new DataOutputStream(commandSocket.getOutputStream());
        final DataInputStream commandIn = new DataInputStream(commandSocket.getInputStream());

        commandOut.writeUTF("FOLLOWER"); // Write type

        Socket dataSocket=new Socket(ip, DATA_PORT);
        final DataInputStream fileIn=new DataInputStream(dataSocket.getInputStream());

        System.out.println("MENU");
        System.out.println("1.ASK FOR CHANGED FILES");
        follower follower= new follower();



        int followerNo = commandIn.readInt();

        try {
            fileHandler = new FileHandler("follower" + followerNo + ".log", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileHandler.setFormatter(formatter);
        LOGGER.addHandler(fileHandler);

        followerPath += followerNo + "/";
        File directory = new File(followerPath);
        if (! directory.exists())
            directory.mkdir();
/*
while(true){
            option=in.readLine();
            if(option.equals("1")){
                System.out.println("SEND Command sent..");
                follower.receivefile(fileIn, commandOut, commandIn, false);
            }
        }
 */
            scheduleOperation(fileIn,commandOut,commandIn);



    }

    /**
     * This method schedules the receivefile method to execute every TIME_INTERVAL seconds
     * @param fileIn the DataInputStream used to receive the file
     * @param commandOut the DataOutputStream used to send commands
     * @param commandIn the DataInputStream used to receive commands
     */
    public static synchronized void   scheduleOperation(final DataInputStream fileIn, final DataOutputStream commandOut, final DataInputStream commandIn) {
        final Runnable updater = new Runnable() {
            public void run() {
                try {
                    System.out.println("SEND Command sent..");
                    follower.receivefile(fileIn, commandOut, commandIn, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        final ScheduledFuture<?> updaterHandle =
                scheduler.scheduleAtFixedRate(updater, 1, TIME_INTERVAL, SECONDS);

    }

    /**
     * Receives the changed files from the associated followerHandler.
     * First sends the SEND request message if this is not a reRecieve
     * Then, gets the number of files to be transmitted
     * For each file, gets the file name, creates the file and reads the file data
     * from the fileIn stream. After the file is transmitted, receives the file's hash from
     * the followerHandler and compares it to the hash of the file received, using the getHashString
     * method of the HashMethods class. If files were corrupted (hashes aren't the same), those files
     * are stored in an arrayList and their names are transmitted to the followerHandler (for resending)
     * after sending the RETRANSMIT signal. And then recieveFile method calls itself, reRecieve value is passed as true.
     * Otherwise, everything is good and CONSISTENCY_CHECK_PASSED
     * signal is sent
     *
     * The method also uses the logger to log the aforementioned signals.
     *
     * @param fileIn the DataInputStream used to receive the file
     * @param commandOut the DataOutputStream used to send commands
     * @param commandIn the DataInputStream used to receive commands
     * @param reRecieve boolean. Signifies whether the method is called again (there was need for a RETRANSMIT)
     * @throws Exception
     */
    public static void receivefile(DataInputStream fileIn, DataOutputStream commandOut, DataInputStream commandIn, boolean reRecieve) throws Exception
    {
        ArrayList<String> retransmitFiles = new ArrayList<>();
        if (!reRecieve)
            commandOut.writeUTF("SEND");

        int fileNo = Integer.parseInt(fileIn.readUTF());
        for (int i=0; i<fileNo; i++) {
            String filename = fileIn.readUTF();
            System.out.println("Receiving File " + filename);
            String pathToFile = followerPath + filename;
            File f = new File(pathToFile);
            FileOutputStream fout = new FileOutputStream(f);
            int ch;
            do {
                ch = Integer.parseInt(fileIn.readUTF());
                if (ch != -1) fout.write(ch);
            } while (ch != -1);
            System.out.println("Received File...");

            // now get hash value of the file
            String masterHash = commandIn.readUTF();
            /*// break randomly on purpose
            if (Math.random() > 0.35) {
                fout.write(2);
                //TimeUnit.SECONDS.sleep(1);
                retransmitFiles.add(filename);
            }*/
            if (!HashMethods.gethHashString(pathToFile).equalsIgnoreCase(masterHash))
                retransmitFiles.add(filename);

            fout.close();
        }

        if (retransmitFiles.size() == 0) {
            commandOut.writeUTF(CONSISTENCY_CHECK_PASS);
            LOGGER.log(Level.INFO, CONSISTENCY_CHECK_PASS);
        }
        else {
            commandOut.writeUTF(RETRANSMIT);
            LOGGER.log(Level.INFO, RETRANSMIT);
            commandOut.writeInt(retransmitFiles.size());
            for (String fileName: retransmitFiles){
                commandOut.writeUTF(fileName);
            }
            receivefile(fileIn, commandOut, commandIn, true);
        }

    }


}
