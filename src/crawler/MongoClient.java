package crawler;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MongoClient {
    private String host;
    private static final String dbName = "crawler";
    private static final String collectionName = "pages";

    public MongoClient(String host) {
        this.host = host;
    }

    public void savePages(List<HtmlPage> pages) {
        try (var mongoClient = MongoClients.create(host)) {
            MongoDatabase database = mongoClient.getDatabase(dbName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            List<Document> documents = new ArrayList<>();
            for (HtmlPage page : pages) {
                documents.add(new Document("url", page.getUrl()).append("title", page.getTitle()));
            }
            collection.insertMany(documents);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<HtmlPage> getPages() {
        List<HtmlPage> result = new ArrayList<>();
        try (var mongoClient = MongoClients.create(host)) {
            MongoDatabase database = mongoClient.getDatabase(dbName);
            MongoCollection<Document> collection = database.getCollection(collectionName);
            try (MongoCursor<Document> cursor = collection.find().iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    HtmlPage page = new HtmlPage(doc.get("url").toString(), 0);
                    page.setTitle(doc.getString("title"));
                    result.add(page);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
