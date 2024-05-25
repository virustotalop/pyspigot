package dev.magicmq.pyspigot.manager.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.zaxxer.hikari.HikariConfig;
import dev.magicmq.pyspigot.manager.database.mongo.MongoDatabase;
import dev.magicmq.pyspigot.manager.database.sql.SqlDatabase;
import dev.magicmq.pyspigot.manager.script.Script;
import dev.magicmq.pyspigot.util.ScriptUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Manager that allows connection to and interact with a variety of database types. Primarily used by scripts to interact with external databases, such as SQL and MongoDB.
 */
public class DatabaseManager {

    private static DatabaseManager instance;

    private final HashMap<Script, List<Database>> activeConnections;

    private DatabaseManager() {
        activeConnections = new HashMap<>();
    }

    /**
     * Get a new {@link com.zaxxer.hikari.HikariConfig} for specifying configuration options.
     * @return A new HikariConfig
     */
    public HikariConfig newHikariConfig() {
        return new HikariConfig();
    }

    /**
     * Open a new connection with an SQL database, using the default configuration options.
     * @param host The host URL or IP of the SQL database
     * @param port The port of the SQL database
     * @param database The name of the SQL database
     * @param username The username of the SQL database
     * @param password The password of the SQL database
     * @return An {@link SqlDatabase} object representing an open connection to the database
     */
    public SqlDatabase connectSql(String host, String port, String database, String username, String password) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.addDataSourceProperty("cachePrepStmts", true);
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", 250);
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);

        return connectSql(host, port, database, username, password, hikariConfig);
    }

    /**
     * Open a new connection with an SQL database, using the specified configuration options.
     * @param host The host URL or IP of the SQL database
     * @param port The port of the SQL database
     * @param database The name of the SQL database
     * @param username The username of the SQL database
     * @param password The password of the SQL database
     * @param hikariConfig A {@link com.zaxxer.hikari.HikariConfig} object representing the configuration options for the connection
     * @return An {@link SqlDatabase} object representing an open connection to the database
     */
    public SqlDatabase connectSql(String host, String port, String database, String username, String password, HikariConfig hikariConfig) {
        Script script = ScriptUtils.getScriptFromCallStack();
        String uri = URLEncoder.encode(String.format(DatabaseType.SQL.getUri(), host, port, database, username, password), StandardCharsets.UTF_8);
        hikariConfig.setJdbcUrl(uri);
        SqlDatabase connection = new SqlDatabase(script, uri, hikariConfig);

        if (connection.open()) {
            addConnection(connection);
            return connection;
        } else {
            script.getLogger().log(Level.SEVERE, "Failed to open a connection to the SQL database. URI: " + uri);
            return null;
        }
    }

    /**
     * Get a new {@link com.mongodb.MongoClientSettings.Builder} for specifying client settings.
     * @return A new MongoClientSettings builder
     */
    public MongoClientSettings.Builder newMongoClientSettings() {
        return MongoClientSettings.builder();
    }

    /**
     * Open a new connection with a Mongo database, using the default client settings.
     * @param host The host URL or IP of the Mongo database
     * @param port The port of the Mongo database
     * @param username The username of the Mongo database
     * @param password The password of the Mongo database
     * @return An {@link MongoDatabase} object representing an open connection to the database
     */
    public MongoDatabase connectMongo(String host, String port, String username, String password) {
        MongoClientSettings clientSettings = MongoClientSettings.builder().build();
        return connectMongo(host, port, username, password, clientSettings);
    }

    /**
     * Open a new connection with a Mongo database, using the specified client settings.
     * @param host The host URL or IP of the Mongo database
     * @param port The port of the Mongo database
     * @param username The username of the Mongo database
     * @param password The password of the Mongo database
     * @param clientSettings A {@link com.mongodb.MongoClientSettings} object representing the client settings for the connection
     * @return An {@link MongoDatabase} object representing an open connection to the database
     */
    public MongoDatabase connectMongo(String host, String port, String username, String password, MongoClientSettings clientSettings) {
        Script script = ScriptUtils.getScriptFromCallStack();
        String uri = URLEncoder.encode(String.format(DatabaseType.MONGO_DB.getUri(), username, password, host, port), StandardCharsets.UTF_8);
        MongoClientSettings newClientSettings = MongoClientSettings.builder(clientSettings)
                .applyConnectionString(new ConnectionString(uri))
                .build();
        MongoDatabase connection = new MongoDatabase(script, uri, newClientSettings);

        if (connection.open()) {
            addConnection(connection);
            return connection;
        } else {
            script.getLogger().log(Level.SEVERE, "Failed to open a connection to the Mongo database. URI: " + uri);
            return null;
        }
    }

    /**
     * Disconnect from the provided database connection. Should be called when no longer using the database connection.
     * @param connection The database connection to disconnect from
     * @return True if the disconnection was successful, false if otherwise
     */
    public boolean disconnect(Database connection) {
        removeConnection(connection);
        return connection.close();
    }

    /**
     * Disconnect from all database connections belonging to a certain script.
     * @param script The script whose database connections should be disconnected
     * @return True if all disconnections were successful, false if one or more connections were not closed successfully or if the script had no database connections to close
     */
    public boolean disconnectAll(Script script) {
        boolean toReturn = false;
        List<Database> scriptConnections = activeConnections.get(script);
        if (scriptConnections != null) {
            for (Database connection : scriptConnections) {
                toReturn = connection.close();
            }
            activeConnections.remove(script);
        }
        return toReturn;
    }

    /**
     * Get all database connnections belonging to a script.
     * @param script The script to get database connections from
     * @return An immutable List of {@link Database} containing all database connections associated with the script. Will return null if there are no open database connections associated with the script
     */
    public List<Database> getConnections(Script script) {
        List<Database> scriptConnections = activeConnections.get(script);
        if (scriptConnections != null)
            return new ArrayList<>(scriptConnections);
        else
            return null;
    }

    /**
     * Get all database connnections belonging to a script of the given type.
     * @param script The script to get database connections from
     * @param type The type of database connection to filter by
     * @return An immutable List of {@link Database} containing all database connections of the given type associated with the script. Will return null if there are no open database connections of the given type associated with the script
     */
    public List<Database> getConnections(Script script, DatabaseType type) {
        List<Database> scriptConnections = getConnections(script);
        if (scriptConnections != null) {
            List<Database> toReturn = new ArrayList<>(scriptConnections);
            toReturn.removeIf(connection -> connection.getClass() != type.getDbClass());
            return toReturn;
        } else
            return null;
    }

    private void addConnection(Database connection) {
        Script script = connection.getScript();
        if (activeConnections.containsKey(script))
            activeConnections.get(script).add(connection);
        else {
            List<Database> scriptConnections = new ArrayList<>();
            scriptConnections.add(connection);
            activeConnections.put(script, scriptConnections);
        }
    }

    private void removeConnection(Database connection) {
        Script script = connection.getScript();
        List<Database> scriptConnections = activeConnections.get(script);
        scriptConnections.remove(connection);
        if (scriptConnections.isEmpty())
            activeConnections.remove(script);
    }

    /**
     * Get the singleton instance of this DatabaseManager.
     * @return The instance
     */
    public static DatabaseManager get() {
        if (instance == null)
            instance = new DatabaseManager();
        return instance;
    }
}
