import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class HashMethods {

    /**
     * It creates a string from the byte array. Our hashing algorithm creates a byte array.
     *  This method converts it to a string so that we could use it for comparing with other hashes better and
     *  we also can visualize it better this way.
     * @param hash
     * @return a string representing the hash of the file
     */
    private static String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * It reads a json file and converts the content into a string so that afterwards we could use that to save Mongo DB
     * database.
     * @param filename
     * @return a string consisting of the contents of the json file
     * @throws IOException
     * @throws ParseException
     */
    public static String getJSONString(String filename) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        FileReader fileReader= new FileReader(filename);
        Object obj = parser.parse(fileReader);
        JSONObject jsonObject = (JSONObject) obj;
        String originalString= jsonObject.toString();
        fileReader.close();
        return originalString;
    }

    /**
     * It creates a hash string of the contents of the file whose name is given as input.
     * We use a SHA-256 algorithm to ensure that two hashes are same if and only if the contents of the 2 files are the same.
     * @param fileName
     * @return a unique hash for the contents of the file
     * @throws IOException
     * @throws ParseException
     * @throws NoSuchAlgorithmException
     */
    public static String gethHashString(String fileName) throws IOException, ParseException, NoSuchAlgorithmException {
        JSONParser parser = new JSONParser();
        FileReader fileReader =new FileReader(fileName);
        Object obj = parser.parse(fileReader);
        JSONObject jsonObject = (JSONObject) obj;
        String originalString= jsonObject.toString();
        fileReader.close();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(
                originalString.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(encodedhash);
    }
    /*
    public static boolean isSameFile(String fileName1, String fileName2) throws ParseException, NoSuchAlgorithmException, IOException {
        String hash1 = gethHashString(fileName1);
        String hash2 = gethHashString(fileName2);
        return hash1.equals(hash2);
    }

    public static ArrayList<String> getHashListInDir(String dirPath) throws ParseException, NoSuchAlgorithmException, IOException {
        File folder = new File(dirPath);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> hashList = new ArrayList<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            hashList.add(gethHashString(listOfFiles[i].getName()));
        }
        return hashList;
    }
     */

    /**
     * For each file in the given directory we generate a hash based on its contents. that we add that file name, hash pair
     * into a Hash Map. This will be used to determine to see if the files have changed or not during synchronization  of
     * Mongo DB
     * @param dirPath path to the directory
     * @return a hash map where keys are the file names ard the values are the hash strings of the content of that files.
     * @throws ParseException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static HashMap<String,String> getHashMapInDir(String dirPath) throws ParseException, NoSuchAlgorithmException, IOException {
        File folder = new File(dirPath);
        File[] listOfFiles = folder.listFiles();
        HashMap<String,String> map = new HashMap<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            String fileName= listOfFiles[i].getName();
            map.put(fileName, gethHashString(dirPath+"/"+fileName));
        }
        return map;
    }


}
