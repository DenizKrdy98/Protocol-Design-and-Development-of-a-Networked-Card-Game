import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MongoWriter extends Thread{
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    public props props1 = props.getInstance();
    String mongoPass = props1.getMongoPass();
    String mongoUser = props1.getMongoUser();

    MongoClientURI uri = new MongoClientURI(
            "mongodb+srv://"+mongoUser+":"+mongoPass+"@cluster0-kobje.mongodb.net/test?retryWrites=true&w=majority");
    MongoClient mongo;
    MongoDatabase database;
    MongoCollection<Document> collection;
    HashMap<String,String> map;
    HashMap<String, ObjectId> idMap=new HashMap<>();
    String dirPath = "./src/main/Master Files";


    Logger logger = Logger.getLogger(MongoWriter.class.getName());

    // Create an instance of FileHandler that write log to a file called
    // app.log. Each new message will be appended at the at of the log file.
    SimpleFormatter formatter = new SimpleFormatter();

    FileHandler fileHandler = new FileHandler("mongoDbLog.log", true);


    /**
     * Constructor for MongoWriter
     * It creates the logger. Assigns the database and the collection (if it doesn't exist it creates it)
     * and finally it creates 2 maps. One for file names and id pairs, the other one is file name and hash pairs.
     * @throws ParseException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public MongoWriter() throws ParseException, NoSuchAlgorithmException, IOException {
        fileHandler.setFormatter(formatter);
        logger.addHandler(fileHandler);


        //this.mongo = new MongoClient( "localhost" , 27017 );
    /*
        MongoCredential credential = MongoCredential.createCredential("user", "admin", "comp416".toCharArray());
        MongoClientSettings settings = MongoClientSettings.builder()
                .credential(credential)
                .retryWrites(true)
                .applyToConnectionPoolSettings(builder ->
                        builder.maxConnectionIdleTime(5000, TimeUnit.MILLISECONDS))
                .applyToSslSettings(builder -> builder.enabled(true))
                .applyToClusterSettings(builder -> {
                    builder.hosts(Arrays.asList(
                            new ServerAddress("cluster0-kobje.mongodb.net", 27017)
                    ));

                })
                .build();

         this.mongo = (MongoClient) MongoClients.create(settings);

*/

        this.mongo= new MongoClient(uri);
        this.database = mongo.getDatabase("COMP416");


        boolean collectionExists = database.listCollectionNames().into(new ArrayList<String>()).contains("GameStates");
        if(collectionExists){
            this.collection = database.getCollection("GameStates");
        }else{
            this.database.createCollection("GameStates");
            this.collection = database.getCollection("GameStates");
        }


        this.collection = database.getCollection("GameStates");
        this.map= HashMethods.getHashMapInDir(this.dirPath);



        //write all files in mongoDB
        for (String filename: map.keySet()){
            Document dbObject = Document.parse(HashMethods.getJSONString(this.dirPath+"/"+filename));
            collection.insertOne(dbObject);
            ObjectId id = (ObjectId)dbObject.get( "_id" );

            idMap.put(filename,id);

        }

        //create an id table

    }

    /**
     * This is the main function that does all updating in the database. It holds a boolean variable called isChanged.
     * If any change is detected this will become true.
     *
     * First thing we calculate is an updated map of file name, hash pairs. This will be used to detect all kind of
     * changes.
     *
     * If a file is in the old map but not in the new map, this means that the game is over and the system deleted the
     * game state file. In this case we don't have to store it in the db any longer. We delete it from db with the id
     * thanks to our id table.
     *
     * If a file is in the new map but not in the old map, this means that this is a new game file. We get the contents
     * with HashMethods.getJSONString and then parse it into a document so that we can store it in db. We also add the
     * newly added object into the id table as well
     *
     * If a file is in both maps but their hashes are not equal, this means that the contents of that file is modified.
     * In this case, the first thing we do is delete that file based on its id with the help of the id table.
     * Then we insert the file into db as mentioned above. Afterwards we update the id table with the id of the newly
     * inserted document.
     *
     *
     * In all these cases we also log the necessary info into a log file.
     */
    public void checkAndUpdateGameStates(){
        try{

                HashMap<String, String> updatedMap= HashMethods.getHashMapInDir(this.dirPath);
                boolean isChanged=false;
                for (Map.Entry entry: map.entrySet()){//check for deleted files
                    if (!updatedMap.containsKey(entry.getKey())){//deleted file
                        isChanged=true;
                        collection.deleteOne(Filters.eq("_id", idMap.get(entry.getKey())));
                        logger.info("This file is going to be synchronized: "+entry.getKey()+ " (Deleted File)");
                    }
                }


                for (Map.Entry entry: updatedMap.entrySet()){
                    if (!this.map.containsKey(entry.getKey())){ // new file
                        logger.info("This file is going to be synchronized: "+entry.getKey()+ " (New File)");
                        //System.out.println("This is a newly added file:"+entry.getKey());
                        isChanged=true;
                        Document dbObject = Document.parse(HashMethods.getJSONString(this.dirPath+"/"+entry.getKey()));

                        collection.insertOne(dbObject);
                        ObjectId id = (ObjectId)dbObject.get( "_id" );
                        idMap.put((String) entry.getKey(),id);

                    }else if (!map.get(entry.getKey()).equals(entry.getValue())){ // Hashes are not equal
                        logger.info("This file is going to be synchronized: "+entry.getKey());
                        //System.out.println("This file is going to be synchronized: "+entry.getKey());
                        isChanged=true;
                        collection.deleteOne(Filters.eq("_id", idMap.get(entry.getKey())));


                        Document dbObject = Document.parse(HashMethods.getJSONString(this.dirPath+"/"+entry.getKey()));

                        collection.insertOne(dbObject);
                        ObjectId id = (ObjectId)dbObject.get( "_id" );
                        idMap.put((String) entry.getKey(),id);


                    }else{
                        //System.out.println("This file is not changed: "+entry.getKey());
                    }

                }
                if (isChanged == false){
                    logger.info("No update is needed. Already synced!");
                }
                this.map = updatedMap;
                System.out.println("Synchronization done with MongoDB");


        }catch (Exception e)
        {
            // Throwing an exception
            System.out.println ("Exception is caught"+e);
        }
    }

    /**
     * schedules an operation to be done periodically. In this case it calls the
     * checkAndUpdateGameStates method every 30 seconds.
     */

    public synchronized void   scheduleOperation() {
        final Runnable updater = new Runnable() {
            public void run() { checkAndUpdateGameStates();}
        };
        final ScheduledFuture<?> updaterHandle =
                scheduler.scheduleAtFixedRate(updater, 2, 30, SECONDS);
        /*
        scheduler.schedule(new Runnable() {
            public void run() { updaterHandle.cancel(true); }
        }, 12, SECONDS);
         */
    }



    public static void main(String[] args) throws ParseException, NoSuchAlgorithmException, IOException {
        MongoWriter m= new MongoWriter();
        m.scheduleOperation();
    }
}
