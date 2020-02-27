import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;

public class JSONHelper {
    /**
     * It is used to write the current state of the game into a json file.
     * Name of the json file determined as player1name-player2name.json
     * All the necessary information required to write the game is extracted from the singleGameServer instance
     * @param singleGameServer
     * @throws IOException
     */
    public synchronized static void writeCurrentState(SingleGameServer singleGameServer) throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("player1name", singleGameServer.player1.getName());
        jsonObject.put("player2name", singleGameServer.player2.getName());
        jsonObject.put("pointsOfP1", singleGameServer.pointsOfP1);
        jsonObject.put("pointsOfP2", singleGameServer.pointsOfP2);

        jsonObject.put("round", singleGameServer.round);


        JSONArray arr = new JSONArray();
        arr.addAll(singleGameServer.cardsP1);
        jsonObject.put("cardsP1", arr);

        JSONArray arr1 = new JSONArray();
        arr1.addAll(singleGameServer.cardsP2);
        jsonObject.put("cardsP2", arr1);


        FileWriter file = new FileWriter("./src/main/Master Files/"+getGameName(singleGameServer));
        file.write(jsonObject.toJSONString());
        file.close();
    }

    /**
     * Gets the player names from the singleGameServer to return a string for the file name in the following format:
     * player1name-player2name.json
     * @param singleGameServer
     * @return name of the file that is used to store game state
     */
    public static String getGameName(SingleGameServer singleGameServer){
        String ans = singleGameServer.player1.getName()+"-"+singleGameServer.player2.getName()+".json";
        System.out.println(ans);
        return ans;
    }

    /**
     * Deletes the json file in the "Master Files" directory
     * prints a message afterwards to indicate if the operation was successful or not
     * @param filename name of the file that is going to be deleted
     */
    public synchronized static void deleteJSONFile(String filename){
        File file = new File("./src/main/Master Files/"+filename);

        if(file.delete())
        {
            System.out.println("File deleted successfully");
        }
        else
        {
            System.out.println("Failed to delete the file");
        }
    }

    /**
     * Reads the file and parses in to a json object so that we could benefit from key-value pairs of json format.
     * @param filename
     * @return a json object
     * @throws IOException
     * @throws ParseException
     */
    public static JSONObject getJSONObject(String filename) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        FileReader fileReader= new FileReader(filename);
        Object obj = parser.parse(fileReader);
        JSONObject jsonObject = (JSONObject) obj;
        fileReader.close();

        return jsonObject;
    }

}
