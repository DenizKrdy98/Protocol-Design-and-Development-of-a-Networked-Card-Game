import javax.sound.midi.Soundbank;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SingleGameServer extends Thread{
    public static final int TOTAL_ROUND_COUNT = 26;
    public Socket s1;
    public Socket s2;
    protected PlayerThread player1;
    protected PlayerThread player2;
    public List<Integer> deckOfCards  = new ArrayList<Integer> ();
    public List<Integer> cardsP1 = new ArrayList<Integer>();
    public List<Integer> cardsP2 = new ArrayList<Integer>();
    public static int cnt1 = 0;
    public static int cnt2 = 0;
    public boolean singlePlayerLeft = false;

    protected String in1="";
    protected String in2="";
    protected int stateOfGame;
    public int round;
    public int playedCard1;
    public int playedCard2;
    public int pointsOfP1 = 0;
    public int pointsOfP2 = 0;
    public String nameP1 = "";
    public String nameP2 = "";


    /**
     * Initializes the SingleGameServer Object
     * @param p1:   Socket of client one which will be handled by PlayerThread object player1
     * @param p2:   Socket of client one which will be handled by PlayerThread object player1
     * @throws IOException
     */
    public SingleGameServer(Socket p1, Socket p2) throws IOException {
        System.out.println("A new SingleGameServer has opened.");
        s1 = p1;
        s2 = p2;
        player1 = new PlayerThread(s1);
        player1.index = 1;
        player2 = new PlayerThread(s2);
        player2.index = 2;
        player1.start();
        player2.start();
        stateOfGame=1;
    }

    /**
     * Warn player1 thread when the client handled by player2 thread is left the game.
     * Does the required parameter changes for it as well.
     */
    public void announcementOfLeftToP1(){
        player1.sendMess("Your partner is LEFT. " +nameP1+" has "+pointsOfP1+" points, "+nameP2+" has "+pointsOfP2+" points. You can either quit or start new game?");
        player1.stateOfGame = 4;
        singlePlayerLeft = true;
        stateOfGame = 3;
    }

    /**
     * Warn player2 thread when the client handled by player1 thread is left the game.
     * Does the required parameter changes for it as well.
     */
    public void announcementOfLeftToP2(){
        player2.sendMess("Your partner is LEFT. " +nameP1+" has "+pointsOfP1+" points, "+nameP2+" has "+pointsOfP2+" points. You can either quit or start new game?");
        player2.stateOfGame = 4;
        singlePlayerLeft = true;
        stateOfGame = 3;

    }

    @Override
    /**
     * Manages the game by following the both PlayerThread objects for all states of the game.
     * State of Game 1,   Players entered their names, begin comment is being waited.
     * State of Game 2,   Players entered begin, starts and handle the game.
     * State of Game 3,   Players played their last cards, wait until both of them exist.
     * Also handles the quit and start new game requests done before the game ends.
     */
    public void run() { //throws java.lang.NullPointerException

        while (true) {
            if(stateOfGame == 1){
                if(player1.isAlive()){
                    in1 = player1.inLine;
                    try{
                        nameP1 = player1.getName();
                        if(in1.equals("begin")&& cnt1==0) {
                            System.out.println("Player2 wants to play.");
                            cnt1=1;
                        }
                    }catch(java.lang.NullPointerException e) {
                        System.out.println("NO PROBLEM");
                        announcementOfLeftToP2();
                    }
                }else{
                    announcementOfLeftToP2();
                }
                if(player2.isAlive() ){
                    in2 = player2.inLine;
                    try{
                        nameP2 = player2.getName();
                        if(in2.equals("begin")&& cnt2==0) {
                            System.out.println("Player2 wants to play.");
                            cnt2=1;
                        }
                    }catch(java.lang.NullPointerException e){
                        System.out.println("NO PROBLEM");
                        announcementOfLeftToP1();
                    }

                }else{
                    announcementOfLeftToP1();
                }
                if(cnt1==1 && cnt2==1) {
                    stateOfGame = 2;
                    establishTheGame();
                    cnt1=0;
                    cnt2=0;
                }
            }else if(stateOfGame==2) {
                //System.out.println("Cards are supposed to be distributed.");
                if(player1.isAlive()) player1.sendMess(listToString(cardsP1));
                if(player2.isAlive()) player2.sendMess(listToString(cardsP2));
                while(stateOfGame!=3){
                    if(!player1.isAlive()){
                        announcementOfLeftToP2();
                    }
                    if(!player2.isAlive()){
                        announcementOfLeftToP1();
                    }
                    in1=player1.inLine;
                    in2=player2.inLine;
                    if(isInteger(in1) && isInteger(in2)){
                        if(playedCard1!=Integer.parseInt(in1) && playedCard2!=Integer.parseInt(in2)){
                            System.out.println("Integers are taken.");
                            playedCard1 = Integer.parseInt(in1);
                            playedCard2 = Integer.parseInt(in2);
                            System.out.println("Played cards after assign: "+playedCard1+" "+playedCard2);
                            if(playedCard1<= TOTAL_ROUND_COUNT) cnt1=1;
                            if(playedCard2<= TOTAL_ROUND_COUNT) cnt2=1;
                            if(cnt1==1  && cnt2==1){
                                System.out.println("MAIN SAYS round is "+round+". Now, playTheRound() is not called yet.");
                                playTheRound(playedCard1,playedCard2);
                                cnt1=0;
                                cnt2=0;

                                //  Write current state as json file
                                try {
                                    JSONHelper.writeCurrentState(this);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    System.out.println("Error with state writing");
                                }
                            }
                        }
                    }
                }
            }else if(stateOfGame==3) {
                if(!singlePlayerLeft){
                    System.out.println("MAIN THINKS game is OVER.");
                    cnt1=0;
                    cnt2=0;
                    String lastCard1 = Integer.toString(playedCard1);
                    String lastCard2 = Integer.toString(playedCard2);
                    try {
                        while (player1.inLine.equals(lastCard1) || player2.inLine.equals(lastCard2)) {
                            //System.out.println("Player1 inLine: "+player1.inLine+", Player2 inLine: "+player2.inLine);
                            in1 = player1.inLine;
                            in2 = player2.inLine;
                            if (!in1.equals(lastCard1) && cnt1 == 0) {
                                player1.isGameOver = true;
                                player1.sendMess("Bye");
                                cnt1++;
                            }
                            if (!player2.inLine.equals(lastCard2) && cnt2 == 0) {
                                player2.isGameOver = true;
                                player2.sendMess("Bye");
                                cnt2++;
                            }
                        }
                    }catch(java.lang.NullPointerException e){
                        System.out.println("NO PROBLEM");
                    }
                    System.out.println("MAIN makes isGameOver TRUE.");
                }else{
                    System.out.println("MAIN realized that a single player left.");
                }
                break;
            }
        }
    }

    /**
     * Declare the initial parameters for the game.
     * Shuffle and distribute cards, reset the round etc.
     */
    public void establishTheGame() {
            round = 0;
            playedCard1 = -1;
            playedCard2 = -1;
            for (int i=0; i<TOTAL_ROUND_COUNT*2; i++) {
                deckOfCards.add(i);
            }
            Collections.shuffle(deckOfCards);
            cardsP1 = deckOfCards.subList(0, TOTAL_ROUND_COUNT);
            cardsP2 = deckOfCards.subList(TOTAL_ROUND_COUNT, TOTAL_ROUND_COUNT*2);
        }

    /**
     * Check if the entered answer of the client is integer, when a card indicator is being waited.
     * @param s: string entered by client
     * @return true if integer, false otherwise
     */
    public static boolean isInteger(String s) {
            try {
                Integer.parseInt(s);
            } catch(NumberFormatException e) {
                return false;
            } catch(NullPointerException e) {
                return false;
            }
            return true;
        }

    /**
     * Refresh the card list of the clients, removes the played cards, and assign card groups again.
     * Calls compareCards method to compare real values of played cards.
     * @param c1: index of the card sent by client1
     * @param c2: index of the card sent by client2
     */
    public void playTheRound(int c1, int c2) {
            if(round< TOTAL_ROUND_COUNT) {
                int c11;
                int c22;
                c2 = c2 + TOTAL_ROUND_COUNT;
                c11 = deckOfCards.get(c1-1);
                c22= deckOfCards.get(c2-1);
                deckOfCards.set(c1-1,-1);
                deckOfCards.set(c2-1,-1);
                cardsP1 = deckOfCards.subList(0, TOTAL_ROUND_COUNT);
                cardsP2 = deckOfCards.subList(TOTAL_ROUND_COUNT, TOTAL_ROUND_COUNT*2);
                round++;
                compareCards(c11,c22);
            }
            /*
            if(round== TOTAL_ROUND_COUNT){

            }*/
        }

    /**
     * Compares the real values of played cards, increases the points accordingly, and announces the results of rounds.
     * Calls finishGame method if the last round is played.
     * @param card1: index of card played by client1
     * @param card2: index of card played by client1
     */
    public void compareCards(int card1, int card2) {

            int c1 = card1 % 13;
            int c2 = card2 % 13;
            System.out.println(player1.getName()+" plays "+c1);
            System.out.println(player2.getName()+" plays "+c2);
            if(c1>c2) {
                pointsOfP1++;
                System.out.println(player1.getName()+" wins");
                if(round!= TOTAL_ROUND_COUNT){
                    player1.sendMess("You won the round.\t"+listToString(cardsP1));
                    player2.sendMess("You lost the round.\t"+listToString(cardsP2));
                }

            }else if(c1<c2){
                pointsOfP2++;
                System.out.println(player2.getName()+" wins");
                if(round!= TOTAL_ROUND_COUNT){
                    player2.sendMess("You won the round.\t"+listToString(cardsP2));
                    player1.sendMess("You lost the round.\t"+listToString(cardsP1));
                }

            }else{
                if(round!= TOTAL_ROUND_COUNT){
                    player2.sendMess("Even the round.\t"+listToString(cardsP2));
                    player1.sendMess("Even the round.\t"+listToString(cardsP1));
                }

            }
            if(round== TOTAL_ROUND_COUNT){
                finishTheGame();
            }

    }

    /**
     * Announces the result of the game, makes the stateOfGame 3.
     */
    private void finishTheGame() {
        System.out.println("End of the game.");
        if(pointsOfP1==pointsOfP2){
            player1.sendMess(player1.getName()+" has "+pointsOfP1+" points, "+player2.getName()+" has "+pointsOfP2+" points. No winner in the game...\tENTER QUIT TO EXIT.");
            player2.sendMess(player1.getName()+" has "+pointsOfP1+" points, "+player2.getName()+" has "+pointsOfP2+" points. No winner in the game...\tENTER QUIT TO EXIT.");
        }else{
            if(pointsOfP1>pointsOfP2){
                player1.sendMess(player1.getName()+" has "+pointsOfP1+" points, "+player2.getName()+" has "+pointsOfP2+" points. P1 is the winner.\tENTER QUIT TO EXIT.");
                player2.sendMess(player1.getName()+" has "+pointsOfP1+" points, "+player2.getName()+" has "+pointsOfP2+" points. P1 is the winner.\tENTER QUIT TO EXIT.");
            }else{
                player1.sendMess(player1.getName()+" has "+pointsOfP1+" points, "+player2.getName()+" has "+pointsOfP2+" points. P2 is the winner.\tENTER QUIT TO EXIT.");
                player2.sendMess(player1.getName()+" has "+pointsOfP1+" points, "+player2.getName()+" has "+pointsOfP2+" points. P2 is the winner.\tENTER QUIT TO EXIT.");
            }
        }
        stateOfGame=3;
    }

    /**
     * @param list: list of cards
     * @return the string expression of the card list, makes the places of the played cards space.
     */
    public static String listToString(List<Integer> list) {
            Integer [] arr = new Integer [list.size()];
            list.toArray(arr);
            String str = Arrays.toString(arr);
            char [] ch = str.toCharArray();
            for(int i = 0 ; i<ch.length; i++) {
                if(ch[i]=='-') {
                    ch[i]=' ';
                    ch[i+1]=' ';
                }
            }
            return String.valueOf(ch);
        }

}

class PlayerThread extends Thread{
    public static final int TOTAL_ROUND_COUNT = 26;
    public static boolean isGameOver = false;
    public Socket p;
    public String inLine= new String();
    public int stateOfGame = 1;
    public int round = 0;
    protected Integer [] cardChecker = new Integer[TOTAL_ROUND_COUNT];
    public BufferedReader is;
    public PrintWriter os;
    public int index = 0;

    /**
     * Initializes the PlayerThread Object
     * @param s: socket of interested client
     * @throws IOException
     */
    public PlayerThread (Socket s) throws IOException {
        System.out.println("A new PlayerThread has opened.");
        p=s;

    };

    /**
     * Sends message to the client's socket
     * @param mess: message wanted to be sent.
     */
    public void sendMess(String mess){
        os.println(mess);
        os.flush();
    }


    @Override
    /**
     * Provides the communication between SingleGameServer and Client
     * Manages the game client-wisely, follows the messages coming from the clients and makes sure that it fits the flow of the game.
     * Interferes when it is needed to be handled before the issue is being transmitted to the SingleGameServer
     */
    public void run() {
        try {
            is = new BufferedReader(new InputStreamReader(p.getInputStream()));
            os = new PrintWriter(p.getOutputStream());
            stateOfGame=1;

        }catch (IOException e) {
            System.err.println("Server Thread. Run. IO error in server thread");
        }
        try { // 	MAIN FUNCTION	//
            inLine = is.readLine();
            this.setName(inLine);


            while (inLine.compareToIgnoreCase("QUIT") != 0 && !isGameOver) {
                if(stateOfGame==1) {

                        os.println("Enter begin to play. After you start, you can either play the card by its index, \"start new game\" or \"quit\" anytime.");
                        os.flush();
                        System.out.println(this.getName()+ " sent: " + inLine);
                        inLine = is.readLine();
                        while(!inLine.equals("begin")) {
                            if(!wantsToExit(inLine)){
                                System.out.println(this.getName()+ " sent: " + inLine);
                                os.println("Enter a valid command. Enter begin to start.");
                                os.flush();
                                inLine = is.readLine();
                            }else break;
                        }
                        if(inLine.equalsIgnoreCase("begin")) stateOfGame=2;

                }else if(stateOfGame==2) {
                    System.out.println(this.getName()+" passed th state 2.");

                        while (stateOfGame!=3){
                            System.out.println(this.getName()+" is being waited to play.");
                            String line = is.readLine();
                            System.out.println(this.getName()+" line is entered.");
                            if(check(line)){
                                inLine=line;
                                System.out.println(this.getName()+ "'s line"+inLine+" is accepted.");
                            }
                            //System.out.println("PLAYER"+index+" SAYS stateOfGame is "+stateOfGame);
                            //System.out.println("PLAYER"+index+" SAYS round is "+round+". Now, the card is already sent.");
                            if(stateOfGame == 4) break;
                            if(round== TOTAL_ROUND_COUNT){
                                stateOfGame=3;
                                System.out.println("PLAYER"+index+" THINKS StateOfGame is 3, and game is over.");
                                inLine = is.readLine();
                            }

                        }
                        //System.out.println("PLAYER"+index+" is out of game thinking stateOfGame is "+stateOfGame);

                }else if(stateOfGame == 4){
                    inLine = is.readLine();
                    while((!inLine.equalsIgnoreCase("quit")) && (!inLine.equalsIgnoreCase("start new game"))){
                        os.println("Enter a valid command. \"quit\" or \"start a new game\"");
                        os.flush();
                        inLine = is.readLine();
                    }
                    wantsToExit(inLine);
                    //if ((!inLine.equalsIgnoreCase("quit"))) quit();
                    //else startNewGame();
                }
                System.out.println(this.getName()+ ": Statelerin dışında oyundan çıkmam gereken yerdeyim.");


            }
            //inLine = is.readLine();

                //inLine = is.readLine();
                //p.close();
            p.close();
            System.out.println(this.getName()+" is closed.");




        } catch (IOException e) {
            inLine = this.getName(); //reused String line for getting thread name
            System.err.println("Server Thread. Run. IO Error/ Client " + inLine + " terminated abruptly");
        } catch (NullPointerException e) {
            inLine = this.getName(); //reused String line for getting thread name
            System.err.println("Server Thread. Run.Client " + inLine + " Closed");
        } finally {
            try
            {
                System.out.println("Closing the connection");
                if (is != null)
                {
                    is.close();
                    System.err.println("Socket Input Stream Closed");
                }

                if (os != null)
                {
                    os.close();
                    System.err.println("Socket Out Closed");
                }
                if (p != null)
                {
                    p.close();
                    System.err.println("Socket Closed");
                }

            }
            catch (IOException ie)
            {
                System.err.println("Socket Close Error");
            }
        }//end finally
}

    /**
     * Checks if the message sent by client indicates a quit or a new game request
     * @param line: message sent by client
     * @return true if client wants to exit, false otherwise
     */
    private boolean wantsToExit(String line){
        if(line.equalsIgnoreCase("start new game")){
            startNewGame();
            return true;
        }else if(line.equalsIgnoreCase("quit")){
            quit();
            return true;
        }
        return false;
    }

    /**
     * At game playing state, where clients are supposed sent an integer indicating the cards they want to play, checks whether the sent message is acceptable to play cards.
     * @param line: message sent by client
     * @return true if the message is acceptable
     */
    private boolean check(String line) {
        int c;
        System.out.println(this.getName()+ ": linenı check ediyor.");
        if(!isInteger(line)){
            if(line.equalsIgnoreCase("start new game")){
                startNewGame();
            }else if(line.equalsIgnoreCase("quit")){
                quit();
            }else{
                os.println("Enter an integer please to select cards.");
                os.flush();
            }
            return false;
        }else if (Integer.parseInt(line)<1 || Integer.parseInt(line)> TOTAL_ROUND_COUNT) {
            os.println("Enter an valid integer please.");
            os.flush();
            return false;
        }else{
            c = Integer.parseInt(line);
            for(int i=0; i<round;i++){
                if(cardChecker[i]==c ){
                    os.println("You have already played this card, enter another one.");
                    os.flush();
                    return false;
                }
            }
            cardChecker[round] = c;
            round++;
            return true;
        }
    }

    /**
     * Makes the announcement when the client wants to start new game.
     * Adjusts the parameters to exit.
     */
    private void startNewGame() {
        os.println("We are searching for a new game for you.");
        os.flush();
        stateOfGame = 3;
        isGameOver = true;
    }

    /**
     * Makes the announcement when the client wants to quit.
     * Adjusts the parameters to exit.
     */
    private void quit() {
        os.println("Goodbye...");
        os.flush();
        stateOfGame = 3;
        isGameOver = true;
    }


    /**
     * Check if the entered answer of the client is integer, when a card indicator is being waited.
     * @param s: string entered by client
     * @return true if integer, false otherwise
     */
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        return true;
    }
}


