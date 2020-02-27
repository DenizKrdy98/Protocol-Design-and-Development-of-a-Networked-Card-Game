import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Iterator;

public class MongoDelete extends Thread {

    MongoClientURI uri = new MongoClientURI(
            "mongodb+srv://admin:comp416@cluster0-kobje.mongodb.net/test?retryWrites=true&w=majority");
    MongoClient mongo;
    MongoDatabase database;
    MongoCollection<Document> collection;

    /**
     * It iterates through all the documents in the "GameStates" collection in "COMP416" database and deletes them.
     */
    public MongoDelete(){
        this.mongo= new MongoClient(uri);
        this.database = mongo.getDatabase("COMP416");
        this.collection = database.getCollection("GameStates");
        FindIterable<Document> iterDoc = collection.find();
        Iterator it = iterDoc.iterator();

        while (it.hasNext()) {
            Document doc = (Document) it.next();

            collection.deleteOne(Filters.eq("_id", doc.get("_id")));
        }
        System.out.println("All Documents are deleted.");
    }

    public static void main(String[] args) {
        MongoDelete m= new MongoDelete();

        m.run();
    }
}
