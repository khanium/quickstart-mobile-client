package com.couchbase.mobile;

import com.couchbase.lite.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.couchbase.lite.ReplicatorType.PUSH_AND_PULL;

public class SimpleClientApp {

    static {
        System.out.println("Starting CouchbaseLite at " + LocalTime.now());
        com.couchbase.lite.CouchbaseLite.init();
    }

    public static void main(String[] args) throws CouchbaseLiteException, URISyntaxException {
        String username = System.getProperty("user.name") != null ? System.getProperty("user.name") : "userdb1";
        String password = System.getProperty("user.password") != null ? System.getProperty("user.password") : "Password1!";
        Array channels = new MutableArray().addString("store0001").addString("blue");

        // Setup Database
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDirectory("data/"+username); 
        Database database = new Database("db", config);

        Collection colA = database.getDefaultCollection();
     //   Collection colA = database.createCollection("typeA", "custom");
     //   Collection colB = database.createCollection("typeB", "custom");

        // Setup Replicator
        URI syncGatewayUri = new URI("ws://localhost:4984/db");
  //      URI syncGatewayUri = new URI("wss://vl3w96kxe2eybjoh.apps.cloud.couchbase.com:4984/demoapp");

        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(new URLEndpoint(syncGatewayUri));
        replConfig.setType(PUSH_AND_PULL);
        replConfig.setAutoPurgeEnabled(true);
        replConfig.setAuthenticator(new BasicAuthenticator(username, password.toCharArray())); 
        replConfig.setContinuous(true);
        CollectionConfiguration collectionConfiguration = new CollectionConfiguration();
        collectionConfiguration.setConflictResolver(ConflictResolver.DEFAULT);
        //replConfig.addCollections(List.of(colA, colB), collectionConfiguration);
        replConfig.addCollections(List.of(colA), collectionConfiguration);
        Replicator replicator = new Replicator(replConfig);
        //Setup replicator status change listener
        replicator.addChangeListener(change -> {
            if (change.getStatus().getError() != null) {
                System.err.println("Error in replication ("+change.getStatus().getActivityLevel()+"): " + change.getStatus().getError().getMessage());
            } else {
                System.out.println("Replication in progress: " + change.getStatus().getActivityLevel());
            }

            if(change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.IDLE) || change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.STOPPED)){
                printCount(database);
                if (replicator.getConfig().isContinuous() && change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.STOPPED)) {
                    System.err.println("Replication stopped unexpectedly!");
                    System.err.println("Exiting...");
                    System.exit(0);
                }
            }
        });

        replicator.start();
    }

    private static void printCount(Database database) {
        System.out.println("Documents in the local database: ");
        try {
            System.out.println(" - _default._default: "+ database.getCollection("_default","_default").getCount()+" documents");
    //        System.out.println(" - custom.typeB: "+ database.getCollection("typeB","custom").getCount()+" documents");
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException(e);
        }
    }

}
