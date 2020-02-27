import com.mongodb.gridfs.CLI;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public String name;
    public static String message;
    static int no;

    public static Socket s;
    public static props props1 = props.getInstance();
    public static final int COMMAND_PORT = props1.getCommandPort();
    public static String ip = props1.getMasterIP();
    public static BufferedReader is;
    public static PrintWriter os;

    /**
     * Initializes the client object
     * @param address: IP address of the master which the client is going to connect
     */
    public Client(String address){
        ip = address;
    }

    /**
     * Try to connect thh client socket to the master socket
     */
    public void connect(){

        try{
            s = new Socket(ip, COMMAND_PORT);
            is = new BufferedReader(new InputStreamReader(s.getInputStream()));
            os = new PrintWriter(s.getOutputStream());
            DataOutputStream commandOut=new DataOutputStream(s.getOutputStream());
            commandOut.writeUTF("CLIENT");
            System.out.println("Client Socket is successfully opened for " + ip + " on port " + COMMAND_PORT);
        }catch (IOException e){
            System.err.println("Error: " + "No server has been found on " + ip + "/" + COMMAND_PORT);
        }
    }

    /**
     * The method ensures the communication type is TCP.
     * Takes the string entered by the client end sends it to the master.
     * Waits an answer for the send message and returns it.
     * @param message: string wanted to be sent to master
     * @return the answer responding to the sent message
     */
    public String Answer(String message){
        String response = new String();
        try{
            os.println(message);
            os.flush();
            response = is.readLine();
        }catch(IOException e){
            e.printStackTrace();
            System.out.println("Client-side Error: Server Socket cannot read.");
        }
        return response;
    }

    /**
     * Main function called when the Client is being run, takes the IP of the server before creating an object
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        try{
            InetAddress IP = InetAddress.getLocalHost();
            //ip = IP.getHostAddress();
            System.out.println("ip is " + IP.getHostAddress());
        }catch (Exception e){
            System.out.println(e);
        }

        Client player = new Client(Client.ip);
        runTheClient(player);
    }

    /**
     * Runs the Client object after it is initialized. Manages the TCP communication between server and client.
     * Calls the Exit when clients wants to exit.
     * @param player: current client object
     */
    private static void runTheClient(Client player) {
        player.connect();
        Scanner scanner = new Scanner(System.in);
        System.out.println("We are connected. Enter your name:");
        String name = scanner.nextLine();
        message = name;
        int cnt=0;
        while (!message.equalsIgnoreCase("QUIT")){

            System.out.println("Response of server: " + player.Answer(message));
            message = scanner.nextLine();
            if(message.equalsIgnoreCase("start new game")){
                break;
            }

        }
        player.Exit();
    }

    /**
     * When clients write quit or start new game, it closes the current socket and I/O streams.
     * Initializes a new Client object if new game is wanted.
     */
    public static void Exit(){
        try{
            is.close();
            os.close();
            s.close();
            System.out.println("Current Player Socket is closed.");
            if(message.equalsIgnoreCase("start new game")){
                System.out.println("Entering a new game...");
                Client player = new Client(Client.ip);
                runTheClient(player);
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

}
