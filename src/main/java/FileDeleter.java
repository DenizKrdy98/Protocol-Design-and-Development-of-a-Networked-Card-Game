import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

public class FileDeleter {

    String dirPath = "./src/main/Master Files";
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);


    /**
     * schedules an operation to be done periodically. In this case it calls the
     * deleteFinishedGames method every second.
     */
    public synchronized void   scheduleOperation() {
        final Runnable updater = new Runnable() {
            public void run() {
                try {
                    deleteFinishedGames();
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
            }
        };
        final ScheduledFuture<?> updaterHandle =
                scheduler.scheduleAtFixedRate(updater, 1, 1, SECONDS);

    }

    /**
     * Deletes the finished games in the "Master Files" directory. For every file in the directory it checks if the
     * round number is equal to number of total rounds which means the game is over.
     * It uses JSONHelper.getJSONObject to get the number of rounds and JSONHelper.deleteJSONFile to delete the file afterwards.
     * @throws IOException
     * @throws ParseException
     */
    private synchronized void deleteFinishedGames() throws IOException, ParseException {
        File folder = new File(dirPath);
        File[] listOfFiles = folder.listFiles();

        for(int i=0; i<listOfFiles.length; i++){
            JSONObject object= JSONHelper.getJSONObject(this.dirPath+"/"+listOfFiles[i].getName());
            int round= Integer.parseInt(String.valueOf((Long) object.get("round")));
            if (round==26){
                JSONHelper.deleteJSONFile(listOfFiles[i].getName());
            }
        }
    }


}
