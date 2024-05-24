package dev.magicmq.pyspigot.manager.database.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import dev.magicmq.pyspigot.manager.database.Database;
import dev.magicmq.pyspigot.manager.script.Script;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

/**
 * Represents an open connection to a Mongo Database.
 */
public class MongoDatabase extends Database {

    private final String uri;

    private MongoClient mongoClient;

    /**
     *
     * @param script The script associated with this MongoDatabase
     * @param uri The connection URI for this MongoDatabase
     */
    public MongoDatabase(Script script, String uri) {
        super(script);
        this.uri = uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean open() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .build();
        mongoClient = MongoClients.create(settings);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean close() {
        mongoClient.close();
        return true;
    }


    //Client methods

    /**
     * Get the {@link com.mongodb.client.MongoClient} associated with this Mongo Database connection.
     * @return The MongoClient
     */
    public MongoClient getMongoClient() {
        return mongoClient;
    }

    //Update Options methods

    /**
     * Fetch a new {@link com.mongodb.client.model.UpdateOptions} object for updates
     */
    public UpdateOptions fetchNewUpdateOptions() {
        return new UpdateOptions();
    }

    /**
     * Fetch a new {@link com.mongodb.client.model.FindOneAndUpdateOptions} object for updates
     */
    public FindOneAndUpdateOptions fetchNewFindOneAndUpdateOptions() {
        return new FindOneAndUpdateOptions();
    }

    //Init Document methods

    /**
     * Create a {@link org.bson.Document}.
     * @param json A JSON representation of the document
     * @return The document
     */
    public Document createDocument(String json) {
        return Document.parse(json);
    }

    /**
     * Create a {@link org.bson.Document}.
     * @param key The key
     * @param value The value
     * @return The document
     */
    public Document createDocument(String key, Object value) {
        return new Document(key, value);
    }

    //Database methods

    /**
     * Get a {@link com.mongodb.client.MongoDatabase} with the given name.
     * @param database The name of the database
     * @return The database
     */
    public com.mongodb.client.MongoDatabase getDatabase(String database) {
        return mongoClient.getDatabase(database);
    }

    /**
     * Check if a database exists with the given name.
     * @param database The name of the database to check
     * @return True if the database exists, false if otherwise
     */
    public boolean doesDatabaseExist(String database) {
        for (String db : mongoClient.listDatabaseNames()) {
            if (db.equals(database))
                return true;
        }
        return false;
    }

    //Collection methods

    /**
     * Create a collection in the given database
     * @param database The name of the database where the collection should be created
     * @param collection The name for the collection
     * @return True if the collection was created, false if it already exists in the database
     */
    public boolean createCollection(String database, String collection) {
        if (!doesCollectionExist(database, collection)) {
            getDatabase(database).createCollection(collection);
            return true;
        } else
            return false;
    }

    /**
     * Delete a collection in the given database
     * @param database The name of the database where the collection should be deleted
     * @param collection The name of the collection
     * @return True if the collection was deleted, false if it did not exist in the database
     */
    public boolean deleteCollection(String database, String collection) {
        if (doesCollectionExist(database, collection)) {
            getDatabase(database).getCollection(collection).drop();
            return true;
        } else
            return false;
    }

    /**
     * Get a collection from a database.
     * @param database The name of the database to fetch from
     * @param collection The name of the collection to get
     * @return A {@link com.mongodb.client.MongoCollection} containing {@link org.bson.Document} representing the collection
     */
    public MongoCollection<Document> getCollection(String database, String collection) {
        return getDatabase(database)
                .getCollection(collection);
    }

    /**
     * Check if a collection exists within a database.
     * @param database The name of the database to check
     * @param collection The name of the collection to check
     * @return True if the collection exists, false if otherwise
     */
    public boolean doesCollectionExist(String database, String collection) {
        com.mongodb.client.MongoDatabase mongoDatabase = getDatabase(database);
        for (String col : mongoDatabase.listCollectionNames()) {
            if (col.equals(collection))
                return true;
        }
        return false;
    }

    /**
     * Create a collection with an index of the given keys.
     * @param database The name of the database where the collection should be created
     * @param collection The name for the collection
     * @param keys A {@link org.bson.conversions.Bson} object representing the index keys
     * @return
     */
    public String createCollectionIndex(String database, String collection, Bson keys) {
        return getDatabase(database)
                .getCollection(collection)
                .createIndex(keys);
    }

    //Document get

    /**
     * Get a document within a collection that match the given filter.
     * @param database The name of the database to fetch from
     * @param collection The name of the collection to fetch from
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @return The first document within the collection that matched the provided filter
     */
    public Document getDocument(String database, String collection, Bson filter) {
        return getDatabase(database)
                .getCollection(collection)
                .find(filter)
                .first();
    }

    /**
     * Get a document within a collection that match the given filter, projections, and sort criteria.
     * @param database The name of the database to fetch from
     * @param collection The name of the collection to fetch from
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param projections The project document
     * @param sorts Sort criteria
     * @return The first document within the collection that matched the provided filter, projections, and sort criteria
     */
    public Document getDocument(String database, String collection, Bson filter, Bson projections, Bson sorts) {
        return getDatabase(database)
                .getCollection(collection)
                .find(filter)
                .projection(projections)
                .sort(sorts)
                .first();
    }

    /**
     * Get all documents within a collection.
     * @param database The name of the database to fetch from
     * @param collection The name of the collection to fetch from
     * @return An iterable list of type {@link com.mongodb.client.FindIterable} containing all documents within the collection
     */
    public FindIterable<Document> getDocuments(String database, String collection) {
        return getDatabase(database)
                .getCollection(collection).
                find();
    }

    /**
     * Get all documents within a collection that match the given filter.
     * @param database The name of the database to fetch from
     * @param collection The name of the collection to fetch from
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @return An iterable list of type {@link com.mongodb.client.FindIterable} containing all documents within the collection that matched the provided filter
     */
    public FindIterable<Document> getDocuments(String database, String collection, Bson filter) {
        return getDatabase(database)
                .getCollection(collection)
                .find(filter);
    }

    //Document insert

    /**
     * Insert a document into a collection.
     * @param database The database that contains the collection
     * @param collection The collection to insert into
     * @param document The document to insert
     * @return An {@link com.mongodb.client.result.InsertOneResult} representing the outcome of the operation
     */
    public InsertOneResult insertDocument(String database, String collection, Document document) {
        MongoCollection<Document> col = getDatabase(database).getCollection(collection);
        return col.insertOne(document);
    }

    /**
     * Insert multiple documents into a collection.
     * @param database The database that contains the collection
     * @param collection The collection to insert into
     * @param documents The documents to insert
     * @return An {@link com.mongodb.client.result.InsertManyResult} representing the outcome of the operation
     */
    public InsertManyResult insertDocuments(String database, String collection, List<Document> documents) {
        MongoCollection<Document> col = getDatabase(database).getCollection(collection);
        return col.insertMany(documents);
    }

    //Document update

    /**
     * Update one or more documents within a collection that match the given filter, with the default update options
     * @param database The database that contains the collection
     * @param collection The collection containing the document
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The update that should be applied to the first matching document
     * @return An {@link com.mongodb.client.result.UpdateResult} representing the outcome of the operation
     */
    public UpdateResult updateDocument(String database, String collection, Bson filter, Bson update) {
        return updateDocument(database, collection, filter, Collections.singletonList(update));
    }

    /**
     * Update one or more documents within a collection that match the given filter, with the provided update options
     * @param database The database that contains the collection
     * @param collection The collection containing the document
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The update that that should be applied to the first matching document
     * @param updateOptions A {@link com.mongodb.client.model.UpdateOptions} object representing the options to apply to the update operation
     * @return An {@link com.mongodb.client.result.UpdateResult} representing the outcome of the operation
     */
    public UpdateResult updateDocument(String database, String collection, Bson filter, Bson update, UpdateOptions updateOptions) {
        return updateDocument(database, collection, filter, Collections.singletonList(update), updateOptions);
    }

    /**
     * Update one or more documents within a collection that match the given filter, with the default update options
     * @param database The database that contains the collection
     * @param collection The collection containing the document
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param updates The updates that should be applied to the first matching document
     * @return An {@link com.mongodb.client.result.UpdateResult} representing the outcome of the operation
     */
    public UpdateResult updateDocument(String database, String collection, Bson filter, List<Bson> updates) {
        return updateDocument(database, collection, filter, updates, new UpdateOptions());
    }

    /**
     * Update one or more documents within a collection that match the given filter, with the provided update options
     * @param database The database that contains the collection
     * @param collection The collection containing the document
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param updates The updates that should be applied to the first matching document
     * @param updateOptions A {@link com.mongodb.client.model.UpdateOptions} object representing the options to apply to the update operation
     * @return An {@link com.mongodb.client.result.UpdateResult} representing the outcome of the operation
     */
    public UpdateResult updateDocument(String database, String collection, Bson filter, List<Bson> updates, UpdateOptions updateOptions) {
        MongoCollection<Document> col = getDatabase(database).getCollection(collection);
        return col.updateOne(filter, updates, updateOptions);
    }

    //Document find and update

    /**
     * Update and return a document within a collection that match the given filter, with the default update options
     * @param database The database that contains the collection
     * @param collection The collection containing the document
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The update that should be applied to the first matching document
     * @return An {@link com.mongodb.client.result.UpdateResult} representing the outcome of the operation
     */
    public Document findAndUpdateDocument(String database, String collection, Bson filter, Bson update) {
        return findAndUpdateDocument(database, collection, filter, Collections.singletonList(update));
    }

    /**
     * Update and return a document within a collection that match the given filter, with the default update options
     * @param database The database that contains the collection
     * @param collection The collection containing the document
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The update that should be applied to the first matching document
     * @param updateOptions A {@link com.mongodb.client.model.FindOneAndUpdateOptions} object representing the options to apply to the update operation
     * @return The document that was updated
     */
    public Document findAndUpdateDocument(String database, String collection, Bson filter, Bson update, FindOneAndUpdateOptions updateOptions) {
        return findAndUpdateDocument(database, collection, filter, Collections.singletonList(update), updateOptions);
    }

    /**
     * Update and return a document within a collection that match the given filter, with the default update options
     * @param database The database that contains the collection
     * @param collection The collection containing the document
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The updates that should be applied to the first matching document
     * @return The document that was updated
     */
    public Document findAndUpdateDocument(String database, String collection, Bson filter, List<Bson> update) {
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.returnDocument(ReturnDocument.AFTER);
        return findAndUpdateDocument(database, collection, filter, update, options);
    }

    /**
     * Update and return a document within a collection that match the given filter, with the default update options
     * @param database The database that contains the collection
     * @param collection The collection containing the document
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The updates that should be applied to the first matching document
     * @param updateOptions A {@link com.mongodb.client.model.FindOneAndUpdateOptions} object representing the options to apply to the update operation
     * @return The document that was updated
     */
    public Document findAndUpdateDocument(String database, String collection, Bson filter, List<Bson> update, FindOneAndUpdateOptions updateOptions) {
        MongoCollection<Document> col = getDatabase(database).getCollection(collection);
        return col.findOneAndUpdate(filter, update, updateOptions);
    }

    //Many document update

    /**
     * Update multiple documents within a collection.
     * @param database The database that contains the collection
     * @param collection The collection whose documents should be updated
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The update that should be applied to all matching documents
     * @return An {@link com.mongodb.client.result.UpdateResult} representing the outcome of the operation
     */
    public UpdateResult updateDocuments(String database, String collection, Bson filter, Bson update) {
        return updateDocuments(database, collection, filter, Collections.singletonList(update));
    }

    /**
     * Update multiple documents within a collection.
     * @param database The database that contains the collection
     * @param collection The collection whose documents should be updated
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The update that should be applied to all matching documents
     * @param updateOptions A {@link com.mongodb.client.model.UpdateOptions} object representing the options to apply to the update operation
     * @return An {@link com.mongodb.client.result.UpdateResult} representing the outcome of the operation
     */
    public UpdateResult updateDocuments(String database, String collection, Bson filter, Bson update, UpdateOptions updateOptions) {
        return updateDocuments(database, collection, filter, Collections.singletonList(update), updateOptions);
    }

    /**
     * Update multiple documents within a collection.
     * @param database The database that contains the collection
     * @param collection The collection whose documents should be updated
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The updates that should be applied to all matching documents
     * @return An {@link com.mongodb.client.result.UpdateResult} representing the outcome of the operation
     */
    public UpdateResult updateDocuments(String database, String collection, Bson filter, List<Bson> update) {
        return updateDocuments(database, collection, filter, update, new UpdateOptions());
    }

    /**
     * Update multiple documents within a collection.
     * @param database The database that contains the collection
     * @param collection The collection whose documents should be updated
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @param update The updates that should be applied to all matching documents
     * @param updateOptions A {@link com.mongodb.client.model.UpdateOptions} object representing the options to apply to the update operation
     * @return An {@link com.mongodb.client.result.UpdateResult} representing the outcome of the operation
     */
    public UpdateResult updateDocuments(String database, String collection, Bson filter, List<Bson> update, UpdateOptions updateOptions) {
        MongoCollection<Document> col = getDatabase(database).getCollection(collection);
        return col.updateMany(filter, update, updateOptions);
    }

    //Document delete

    /**
     * Delete a document from a collection matching the provided filter.
     * @param database The database that contains the collection
     * @param collection The collection that contains the document
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @return An {@link com.mongodb.client.result.DeleteResult} representing the outcome of the operation
     */
    public DeleteResult deleteDocument(String database, String collection, Bson filter) {
        return getDatabase(database)
                .getCollection(collection)
                .deleteOne(filter);
    }

    /**
     * Delete multiple documents from a collection matching the provided filter.
     * @param database The database that contains the collection
     * @param collection The collection that contains the documents
     * @param filter A {@link org.bson.conversions.Bson} object representing a filter to filter documents within the collection
     * @return An {@link com.mongodb.client.result.DeleteResult} representing the outcome of the operation
     */
    public DeleteResult deleteDocuments(String database, String collection, Bson filter) {
        return getDatabase(database)
                .getCollection(collection)
                .deleteMany(filter);
    }

    /**
     * Prints a representation of this MongoDatabase in string format, including the URI and {@link com.mongodb.client.MongoClient}
     * @return A string representation of the MongoDatabase
     */
    @Override
    public String toString() {
        return String.format("MongoDatabase[URI: %s, MongoClient: %s]", uri, mongoClient.toString());
    }
}