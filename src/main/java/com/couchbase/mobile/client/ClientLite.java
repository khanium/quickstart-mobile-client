package com.couchbase.mobile.client;

import com.couchbase.lite.*;
import com.couchbase.mobile.config.CouchbaseLiteProperties;
import com.couchbase.mobile.listeners.StatusChangeListener;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@Slf4j
@Data
public class ClientLite {
    private static final String FIND_ALL = "SELECT meta().id as `_id`, * FROM %s";
    private final Database database;
    private final Replicator replicator;
    private final CouchbaseLiteProperties properties;

    public ClientLite(Database database, Replicator replicator, CouchbaseLiteProperties properties) {
        this.database = database;
        this.replicator = replicator;
        this.properties = properties;
    }

    public long count() {
        try {
            return Objects.requireNonNull(database.getScope(properties.getLocal().getScope().getName())).getCollections().stream().mapToLong(Collection::getCount).sum();
        } catch (CouchbaseLiteException e) {
            log.error("Error counting documents", e);
            throw new RuntimeException(e);
        }
    }

    public void printAll(){
        getCollections().forEach(this::printAll);
    }

    public void printAll(Collection collection) {
        List<Result> docs = findAll(collection.getName());
        log.info("Found {} Documents in the `{}` collection: ", docs.size(), collection.getFullName());
        docs.forEach(doc -> {
            try {
                log.info(" - " + doc.toJSON());
            } catch (CouchbaseLiteException e) {
                log.error("Error showing document: " + e.getMessage());
            }
        });
    }


    public List<Result> findAll(String collectionName) {
        List<Result> results = new ArrayList<>();
        Collection c = getCollection(collectionName);
        if(c!=null) {
            try (ResultSet rs = database.createQuery(FIND_ALL.formatted(c.getFullName())).execute()) {
                results.addAll(rs.allResults());
            } catch (CouchbaseLiteException e) {
                log.error("Error listing documents: " + e.getMessage());
            }
        }
        return results;
    }

    public Set<Collection> getCollections() {
        try {
            return Objects.requireNonNull(database.getScope(properties.getLocal().getScope().getName())).getCollections();
        } catch (CouchbaseLiteException e) {
            log.error("Error getting collections", e);
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    private void init() {
        // Adding a change listener to the replicator to print the status of the replication
        //TODO add your own implementation of the StatusChangeListener & handle the replication error status
        this.replicator.addChangeListener(new StatusChangeListener(this));
    }

    public void start() {
        if (replicator != null) {
            replicator.start();
        } else {
            log.error("Replicator is null");
        }
    }

    public void close() {
        if (replicator != null) {
            replicator.stop();
            replicator.close();
        }
        if (database != null) {
            try {
                database.close();
            } catch (CouchbaseLiteException e) {
                log.error("Exception closing database", e);
                throw new RuntimeException(e);
            }
        }
    }

    @SneakyThrows
    public Collection getCollection(String name) {
        return this.database.getCollection(name, properties.getLocal().getScope().getName());
    }

    public boolean isStarted() {
        return replicator != null && !replicator.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.STOPPED);
    }

    public void stop(){
        if (replicator != null) {
            replicator.stop();
        } else {
            log.error("Replicator is null");
        }
    }
}
