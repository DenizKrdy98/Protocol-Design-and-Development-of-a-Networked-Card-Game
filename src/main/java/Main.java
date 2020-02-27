import java.io.DataInputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        DataInputStream in = new DataInputStream(System.in);

        System.out.println("Enter mode (follower, master):");
        String mode = in.readLine();

        if (mode.equalsIgnoreCase("follower")){
            follower.main(null);
        }
        else if (mode.equalsIgnoreCase("master")) {
            TheGameServer.main(null);
        }
        else {
            System.out.println("incorrect mode.");
        }
    }
}
