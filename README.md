# quickstart-mobile-client
Simple Mobile quickstart tutorial using Java Couchbase Lite +3.2.2 / Sync Gateway/App Service +3.2.2 / Couchbase Server/Capella 7.6.5

## QuickStart

### Prerequisites

* [Java 17+](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
* [Gradle 8.0+](https://gradle.org/install/)
* [Docker](https://docs.docker.com/get-docker/)
* [Docker Compose](https://docs.docker.com/compose/install/)
* [Git](https://git-scm.com/downloads)
* [Git Submodules](https://git-scm.com/book/en/v2/Git-Tools-Submodules)


### Build
```console
git clone https://github.com/your-repo/quickstart-mobile-client.git
cd quickstart-mobile-client
git submodule init
git submodule update --remote --recursive
```

This will initialize and update the [quickstart-sync-gateway](quickstart-environment/README.md) submodule.

### Setup & Deploy Docker Compose Environment

```console
cd quickstart-environment
docker-compose up -d
```

This will deploy a local environment with: 
* **Couchbase Server 7.6.5** - [localhost:8091](http://localhost:8091)
  * Users: [ `Administrator`, `sync_gateway`, `metrics_user` ]
  * Buckets: 
    * Bucket: `demo`
      * Scope: `custom`
        * Collections: [`typeA`,`typeB`, `typeC` ]
    * Bucket: `nonmobilebucket`  
* **Sync Gateway 3.2.2 / App Service** - [localhost:4984](http://localhost:4984)
  * Database / Endpoint: `db`
    * Users: [ `userdb1`/`Password1!`]
      * Scope: `custom`
        * Collections: [`typeA`, `typeB`, `typeC`]
          * sync function / access control (per collection): 
          ```javascript
            function(doc, oldDoc, meta) {
              if(doc._deleted) {
                // not allow to delete documents from device unless you have admin role
                requireRole("admin"); // throw an error unless the user has the "admin" role
                return ;
              }

              if(doc.channels) {  
                channel(doc.channels);
              } else {
                //define channels field into the model entity otherwise throw an error ...
                throw({forbidden: "document '"+doc._id+"' doesn't contain channels field to sync"});
              }
            }
            ```
* **Prometheus**  - [localhost:9090](http://localhost:9090)
  * Metrics: `Couchbase Sync Gateway` / `Couchbase Server`
* **Grafana**. - [localhost:3000](http://localhost:3000)
  * Dashboard: `Couchbase Sync Gateway`
  * Dashboard: `Couchbase Server`

### Run the Mobile Client

Wait until the Sync Gateway is up and running. You can check the service is `UP` with: http://localhost:4984/

Then, run the mobile client:

```console
cd quickstart-mobile-client
gradle build
./gradlew run
```

## Overview

## Mobile Client

0. **Prerequisite** - Initialize CouchbaseLite loading the native libraries
1. **Setup Local Device Database**
   1. **Setup Collections**
2. **Setup Replicator**
   1. **Add replicator status change listener**
3. **Start Replicator synchronization**


### Dependencies

```gradle  
repositories {
    mavenCentral()
    maven {url 'https://mobile.maven.couchbase.com/maven2/dev/'}
}

dependencies {
    ...
    implementation 'com.couchbase.lite:couchbase-lite-java-ee:3.2.2'
    ...
}
```

#### CouchbaseLite Java EE

 

#### Basic Main

0. **Prerequisite** - Initialize CouchbaseLite loading the native libraries
```java
    static {
        System.out.println("Starting CouchbaseLite at " + LocalTime.now());
        com.couchbase.lite.CouchbaseLite.init();
    }
```
1. **Setup Local Device Database**
```java
    DatabaseConfiguration config = new DatabaseConfiguration();
    config.setDirectory("data/"+username);
    Database database = new Database(database, config);
```
   1.1. **Setup Collections**
```java
    Collection colA = database.createCollection("typeA", scope); // if the collection exists, it returns the collection otherwise creates a new one
    Collection colB = database.createCollection("typeB", scope); //Collection default = database.getDefaultCollection();
```
2. **Setup Replicator**
```java
    URI syncGatewayUri = new URI("ws://localhost:4984/"+database);
    ReplicatorConfiguration replConfig = new ReplicatorConfiguration(new URLEndpoint(syncGatewayUri));
    replConfig.setType(PUSH_AND_PULL);
    replConfig.setAutoPurgeEnabled(true);
    replConfig.setAuthenticator(new BasicAuthenticator(username, password.toCharArray())); 
    replConfig.setContinuous(true);
    CollectionConfiguration collectionConfiguration = new CollectionConfiguration();
    collectionConfiguration.setConflictResolver(ConflictResolver.DEFAULT);
    replConfig.addCollections(List.of(colA, colB), collectionConfiguration);  
```
2.1. **Add replicator status change listener**
```java
    replicator.addChangeListener(change -> {
        if (change.getStatus().getError() != null) {
            System.err.println("Error in replication ("+change.getStatus().getActivityLevel()+"): " + change.getStatus().getError().getMessage());
            // TODO handling error here if proceed
        } else {
            System.out.println("Replication in progress: " + change.getStatus().getActivityLevel());
            // TODO handling status change & progress notifications here
        }
        // checking for final or idle replication states...
        if(change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.IDLE) || change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.STOPPED)){
            printCount(database);
            // checking for final replication states here
            if (change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.STOPPED)) {
                System.err.println("Replication stopped unexpectedly!");
                exitApp(); // TODO instead exiting to handle replication exception here. I.e: retry to start the replication `change.getReplicator().start();` with a maximum number of retry...
            }
        }
    });
```
3. **Start Replicator synchronization**
```java
    replicator.start();
```

All the above code is in the `SimpleClientApp.java` class.


SimpleClientApp.java
```java
  static {
    // 0. Initialize CouchbaseLite - Prerequisite - it loads the native libraries
        System.out.println("Starting CouchbaseLite at " + LocalTime.now());
        com.couchbase.lite.CouchbaseLite.init();
    }

    public static void main(String[] args) throws CouchbaseLiteException, URISyntaxException {
        String username = System.getProperty("user.name") != null ? System.getProperty("user.name") : "userdb1";
        String password = System.getProperty("user.password") != null ? System.getProperty("user.password") : "Password1!";
        Array channels = new MutableArray().addString("store0001").addString("blue");
        String scope = "custom";
        String database = "db";
        // 1. Setup Database
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDirectory("data/"+username);
        Database database = new Database(database, config);

        // 1.1.  if they don't exist, it creates collections to replicate
        Collection colA = database.createCollection("typeA", scope); // if the collection exists, it returns the collection otherwise creates a new one
        Collection colB = database.createCollection("typeB", scope); //Collection default = database.getDefaultCollection();

        // 2. Setup Replicator
        URI syncGatewayUri = new URI("ws://localhost:4984/"+database);
        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(new URLEndpoint(syncGatewayUri));
        replConfig.setType(PUSH_AND_PULL);
        replConfig.setAutoPurgeEnabled(true);
        replConfig.setAuthenticator(new BasicAuthenticator(username, password.toCharArray())); 
        replConfig.setContinuous(true);
        CollectionConfiguration collectionConfiguration = new CollectionConfiguration();
        collectionConfiguration.setConflictResolver(ConflictResolver.DEFAULT);
        replConfig.addCollections(List.of(colA, colB), collectionConfiguration);  
        Replicator replicator = new Replicator(replConfig);
        // 2.1. Setup replicator status change listener
        replicator.addChangeListener(change -> {
            if (change.getStatus().getError() != null) {
                System.err.println("Error in replication ("+change.getStatus().getActivityLevel()+"): " + change.getStatus().getError().getMessage());
                // TODO handling error here if proceed
            } else {
                System.out.println("Replication in progress: " + change.getStatus().getActivityLevel());
                // TODO handling status change & progress notifications here
            }
            // checking for final or idle replication states...
            if(change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.IDLE) || change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.STOPPED)){
                printCount(database);
                // checking for final replication states here
                if (change.getReplicator().getConfig().isContinuous() && change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.STOPPED)) {
                    System.err.println("Replication stopped unexpectedly!");
                    exitApp(); // TODO instead exiting to handle replication exception here. I.e: retry to start the replication `change.getReplicator().start();` with a maximum number of retry...
                }
            }
        });
        // 3. Start Replicator synchronization
        replicator.start();
    }

    private static void exitApp(){
        System.err.println("Exiting...");
        System.exit(0);
    }

    private static void printCount(Database database) {
        System.out.println("Documents in the local database: ");
        try {
            //System.out.println(" - _default._default: "+ database.getCollection("_default","_default").getCount()+" documents");
            System.out.println(" - custom.typeA: "+ database.getCollection("typeA","custom").getCount()+" documents");
            System.out.println(" - custom.typeB: "+ database.getCollection("typeB","custom").getCount()+" documents");
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException(e);
        }
    }
```

#### Simple
In this main, you can find a simple example of how to create a simple mobile client with a replicator to sync with the Sync Gateway from a property file values.

On top, a command line action menu to create a document, update it, delete it, and list all documents in the local database.

```console
                ** *************************************** **
                ** Menu:
                  0. change working `{}` collection
                  1. Create documents
                  2. Update doc channel
                  3. List `{}` documents
                  4. List All documents
                  5. Count documents in the local database
                  6. Exit
                 - please, choose one option number:
``` 

#### Application.yaml properties

*resources/application.yaml*
```yaml

couchbase:
  remote:
    endpoint-url: ws://127.0.0.1:4984/db
    # endpoint-url: wss://ecfyee1wi6dvwova.apps.cloud.couchbase.com:4984/db
    # certificate-path: assets/cert.pem
    continuous: true
    replicator-type: PUSH_AND_PULL
    reset-checkpoint: false
    websocket:
      timeout: 10000
      heartbeat: 15000
    collections:
      # _default:
      #  documentIDs-filter:
      #  channels-filter:
      typeA:
        documentIDs-filter:
        channels-filter:
      typeB:
        documentIDs-filter:
        channels-filter:
    authenticator:
      username: userdb1
      password: Password1!
  local:
    database: db
    db-path: data
    flush-previous-db: true
    auto-purge: true
    scope:
      name: custom
      collections: typeA,typeB
    #   name: _default
    #   collections: _default
  log:
    path: logs
    level: debug
    max-size: 100000000
    rotation-count: 10
    plaintext: true
```

Properties files are defined in the `src/main/resources` folder. You can define your own properties file and pass it as a command line argument to the application. This file contains the following structure:

```yaml
couchbase:
  remote: 
    # set the remote database properties (endpoint-url, certificate-path, collections, etc) for syncing
  local:
    # set the local database properties (path, name, scope, collections, etc)
    # note: the collections should contain all collections defined in the replicator collections properties
  log:
    # set the logs properties (path, level to debug,etc)
```

In this terminology, **remote database** is the Couchbase Server / Sync Gateway and **local database** is the Couchbase Lite device database.




## Config Server Environment Deployment

TBD

### Local Docker Compose

### Capella App Service


## What next?

TBD