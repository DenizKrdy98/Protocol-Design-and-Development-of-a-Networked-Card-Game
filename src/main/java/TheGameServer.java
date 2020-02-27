import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class TheGameServer {
    public static props props1 = props.getInstance();
    public static final int DATA_PORT = props1.getDataPort();
    public static final int COMMAND_PORT = props1.getCommandPort();
    public static final String FOLLOWER = "FOLLOWER";
    public static final String CLIENT = "CLIENT";
    public static Socket p1;
    public static Socket p2;


    public static HashMap<String, Integer> scores = new HashMap<>();
    private static int followerCounter = 0;
    private static int playerCounter = 0;
    private static ArrayList<Socket> listOfPlayerSockets = new ArrayList<>();
    private static int lenSocket=0;

    /**
     * Manages the socket traffic for the server socket, differs followers from clients and initializes different threads:
     * - If client is connected, SingleGameServer thread starts
     * - If follower is connected, FollowerThread thread starts
     * Initializes MongoDB and  File deleter
     * @param args
     * @throws IOException
     * @throws ParseException
     * @throws NoSuchAlgorithmException
     */
    public static void main(String[] args) throws IOException, ParseException, NoSuchAlgorithmException {
        String ip;
        try{
            InetAddress IP=InetAddress.getLocalHost();
            ip = IP.getHostAddress();
            System.out.println("ip is " + ip);
        }
        catch (Exception e){
            System.out.println(e);
        }

        // server is listening on port 5056
        ServerSocket dataSocket = new ServerSocket(DATA_PORT);
        ServerSocket commandSocket = new ServerSocket(COMMAND_PORT);

        MongoWriter mongo = new MongoWriter();
        mongo.scheduleOperation();

        FileDeleter fileDeleter= new FileDeleter();
        fileDeleter.scheduleOperation();
        System.out.println("The main game server is initialized. Available for multiple players.");

        // running infinite loop for getting
        // client request
        while (true) {
            Socket data = null;
            Socket command = null;
            //System.out.println("The main game server is initialized. Available for multiple players.");
            try {
                // socket object to receive incoming client requests
                command = commandSocket.accept();
                DataInputStream commandIn = new DataInputStream(command.getInputStream());
                DataOutputStream commandOut = new DataOutputStream(command.getOutputStream());
                String type = commandIn.readUTF();
                if (type.equals(FOLLOWER)) {

                    data = dataSocket.accept();
                    System.out.println("A new follower is connected : " + data + ", " + command);
                    // obtaining input and out streams
                    DataInputStream dataIn = new DataInputStream(data.getInputStream());
                    DataOutputStream dataOut = new DataOutputStream(data.getOutputStream());

                    System.out.println("Assigning new thread for this follower");
                    Thread t = new FollowerHandler(dataIn, dataOut, commandIn, commandOut, data, command, ++followerCounter);
                    t.start();

                } else if (type.equals(CLIENT)) {

                    System.out.println("A new client is connected : " + command);
                    playerCounter++;

                    if (playerCounter % 2 != 0) { // create new game
                        listOfPlayerSockets.add(command);
                    }else{
                        listOfPlayerSockets.add(command);
                        System.out.println("We found a pair of players. ");
                        lenSocket = listOfPlayerSockets.size();
                        Thread t = new SingleGameServer(listOfPlayerSockets.get(lenSocket-2),listOfPlayerSockets.get(lenSocket-1));
                        t.start();
                    }

                }
            } catch (Exception e) {
                data.close();
                command.close();
                e.printStackTrace();
            }
        }
    }
}


