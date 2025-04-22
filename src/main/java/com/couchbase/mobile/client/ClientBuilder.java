package com.couchbase.mobile.client;

import com.couchbase.lite.*;
import com.couchbase.lite.Collection;
import com.couchbase.lite.logging.ConsoleLogSink;
import com.couchbase.lite.logging.FileLogSink;
import com.couchbase.lite.logging.LogSinks;
import com.couchbase.mobile.config.CouchbaseLiteProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import static java.lang.String.join;
import static java.util.stream.Collectors.toMap;
import static com.couchbase.mobile.config.CouchbaseLiteProperties.RemoteProperties;
import static com.couchbase.mobile.config.CouchbaseLiteProperties.AuthenticatorProperties;


@Slf4j
public class ClientBuilder {
    private final CouchbaseLiteProperties properties;

    public ClientBuilder(CouchbaseLiteProperties properties) {
        this.properties = properties;
    }

    private void flushPreviousDb(boolean flush, File dbpath) {
        if(flush) {
            log.info("deleting '{}' folder... {}", dbpath.getAbsolutePath(), FileSystemUtils.deleteRecursively(dbpath.getAbsoluteFile()) ? "OK" : "FAILED");
        }
    }

    private Database buildDB() throws CouchbaseLiteException {
        File dbPathFile = new File(properties.getLocal().getDbPath()+ File.separator+ properties.getRemote().getAuthenticator().getUsername());
        flushPreviousDb(this.properties.getLocal().isFlushPreviousDb(),dbPathFile);
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDirectory(dbPathFile.getAbsolutePath());
        Database db = new Database(properties.getLocal().getDatabase(), config);
        return setupCollections(db);
    }

    private void flushPreviousLogs(boolean flush, File logPath) {
        if(flush) {
            log.info("deleting '{}' folder... {}", logPath.getAbsolutePath(), FileSystemUtils.deleteRecursively(logPath.getAbsoluteFile()) ? "OK" : "FAILED");
        }
    }

    private void setupDatabaseLogs() {

        File logPath = new File(properties.getLog().getPath()+ File.separator+ properties.getRemote().getAuthenticator().getUsername());
        if (!logPath.exists()) {
            if(!logPath.mkdirs()) {
                log.error("Log path: {} folder cannot be created", logPath);
            }
        }
        flushPreviousLogs (properties.getLocal().isFlushPreviousDb(), logPath);
        log.info("Setting database logs to {}", logPath.getAbsolutePath());
        LogSinks.get().setConsole(new ConsoleLogSink(LogLevel.WARNING)); // TODO set level from properties independently of the file logs
        FileLogSink.Builder logCfg = new FileLogSink.Builder().setDirectory(logPath.getAbsolutePath())
                .setMaxFileSize(properties.getLog().getMaxSize())
                .setMaxKeptFiles(properties.getLog().getRotationCount())
                        .setPlainText(properties.getLog().isPlainText())
                                .setLevel(properties.getLog().getLevel());
        LogSinks.get().setFile(logCfg.build());
    }

    private Database setupCollections(Database database) {
        String scopeName = properties.getLocal().getScope().getName();
        List<String> collectionNames = properties.getLocal().getScope().getCollections();
        for (String name : collectionNames) {
            try {
                database.createCollection(name, scopeName); // this method return the collection in case it exists
            } catch (CouchbaseLiteException e) {
                log.error("Error creating collection {} in scope {}", name, scopeName, e);
                // TODO Handle the exception as needed
            }
        }
        return database;
    }

    private Set<Collection> getCollections(CouchbaseLiteProperties properties, Database db) throws CouchbaseLiteException {
        Set<Collection> collections = new HashSet<>();
        if(properties.getLocal().getScope() == null
                || properties.getLocal().getScope().getName() == null
                || properties.getLocal().getScope().getName().isEmpty()
                || properties.getLocal().getScope().getCollections().isEmpty()
                || properties.getLocal().getScope().getCollections().contains("_default")) {
            collections.addAll(db.getCollections());
        } else {
            Scope scope = db.getScope(properties.getLocal().getScope().getName());
            if(scope == null) {
                throw new CouchbaseLiteException("Scope not found: " + properties.getLocal().getScope().getName());
            }
            collections.addAll(scope.getCollections());
        }
        return collections;
    }

    public ClientLite build() throws CouchbaseLiteException {
        // Build the client using the properties
        // For example, you might want to initialize a Couchbase Lite database here
        log.info("Building client with properties: " + properties);
        setupDatabaseLogs();
        Database db = buildDB();
        Set<Collection> collections = getCollections(properties, db);
        Replicator replicator = new ReplicatorBuilder(properties.getRemote(), collections).build();
        return new ClientLite(db, replicator, properties);

    }


    public static class ReplicatorBuilder {
        final RemoteProperties properties;
        final Map<String, Collection> collections = new HashMap<>();

        public ReplicatorBuilder(RemoteProperties properties, Set<Collection> collections) {
            this.properties = properties;
            this.collections.putAll(collections.stream().collect(toMap(Collection::getName, Function.identity() )));
        }

        private Map<Collection, CollectionConfiguration> collectionsConfiguration() {
            Map<String, CouchbaseLiteProperties.CollectionProperties> collectionsConfigs = properties.getCollections();
            Map<Collection, CollectionConfiguration> collectionsMap = new HashMap<>();
            log.info("Setting listeners for collections: {}",join(",",collections.keySet()));
            collectionsConfigs.forEach( (name, p) -> {
                Collection collection = collections.get(name);
                if(collection != null) {
                    CollectionConfiguration collectionCfg = new CollectionConfiguration();
                    if (p.getChannelsFilter() != null && !p.getChannelsFilter().isEmpty()) {
                        collectionCfg.setChannels(p.getChannelsFilter());
                    }
                    if (p.getDocumentIDsFilter() != null && !p.getDocumentIDsFilter().isEmpty()) {
                        collectionCfg.setDocumentIDs(p.getDocumentIDsFilter());
                    }
                    collectionsMap.put(collection, collectionCfg);
                }else {
                    log.warn(" - CollectionReplication {} not found in local database configuration properties", name);
                }
            });
            return collectionsMap; //modify this line if you want to add specific collections configuration
        }




        private ReplicatorConfiguration replicatorConfiguration() {
            Map<Collection, CollectionConfiguration> collectionsCfg = collectionsConfiguration();

            Endpoint endpoint = properties.getEndpoint();
            ReplicatorConfiguration replConfig = new ReplicatorConfiguration(endpoint);
            log.info("Authenticator: {}", properties.getAuthenticator());
            replConfig.setAuthenticator(new AuthenticatorBuilder(this.properties.getAuthenticator()).build());   //TODO decide if it would be built or provided
            replConfig.setType(properties.getReplicatorType());
            replConfig.setContinuous(properties.isContinuous());

            collectionsCfg.forEach(replConfig::addCollection); // migration from deprecated method replConfig.setChannels(properties.getChannels()); to CollectionConfiguration
            return replConfig;
        }

        public Replicator build() {
            return new Replicator(replicatorConfiguration());
        }
    }

    public static class AuthenticatorBuilder {
        final AuthenticatorProperties properties;
        public AuthenticatorBuilder(AuthenticatorProperties authenticator) {
            this.properties = authenticator;
        }

        public Authenticator build() {
            log.info("Authenticator build: {} , {}", properties.getUsername(), properties.getPassword());
            return new BasicAuthenticator(properties.getUsername(), properties.getPassword().toCharArray());
        }
    }
}
