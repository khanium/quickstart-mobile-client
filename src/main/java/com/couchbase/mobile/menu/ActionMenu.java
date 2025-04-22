package com.couchbase.mobile.menu;

import com.couchbase.lite.*;
import com.couchbase.mobile.client.ClientLite;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ActionMenu implements Runnable {
    private final String FIND_ALL = "SELECT meta().id as `_id`, * FROM %s";
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final ClientLite client;
    private Collection selectedCollection;
    private final Array DEFAULT_CHANNELS = new MutableArray()
            .addString("channel1")
            .addString("channel2")
            .addString("blue");

    public ActionMenu(ClientLite client) {
        this.client = client;
        this.selectedCollection = client.getCollections().stream().findAny().get();
    }

    public void show() {
        try (Scanner scanner = new Scanner(System.in);) {
            //Collection col = database.getCollection("typeA", "custom");
            while (enabled.get()) {
                log.info("""
               \s
                ** *************************************** **
                ** Menu:
                  0. change working `{}` collection
                  1. Create documents
                  2. Update doc channel
                  3. List `{}` documents
                  4. List All documents
                  5. Count documents in the local database
                  6. Exit
                 - please, choose one option number:\s""",selectedCollection.getFullName(),selectedCollection.getFullName());
                int option = System.in.read();
                scanner.nextLine(); // Consume newline
                log.info("** *************************************** **");
                log.info("");
                switch (option) {
                    case '0':
                        log.info("Setting up new working collection...");
                        actionSetupCollection();
                        break;
                    case '1':
                        log.info("Creating documents in `{}` collection...", selectedCollection.getFullName());
                        actionCreate();
                        break;
                    case '2':
                        log.info("Updating document in `{}` collection...", selectedCollection.getFullName());
                        log.info(" - Type the document's Id to update: ");
                        String docId = scanner.nextLine();
                        actionUpdate(docId);
                        break;
                    case '3':
                        log.info("{} Documents in the local database: {}" , selectedCollection.getFullName(),  selectedCollection.getCount());
                        actionList(selectedCollection);
                        break;
                    case '4':
                        log.info("Print all database");
                        actionListAll();
                        break;
                    case '5':
                        log.info("Documents in the local database: ");
                        actionCount();
                        break;
                    case '6':
                        log.info("Exiting...");
                        actionExit();
                        break;
                    default:
                        log.info("No valid Option");
                }
            }
        } catch (IOException e) {
            log.error("Error in the application: " + e.getMessage(), e);
            //throw new RuntimeException(e);
        } finally {
            log.info("Exiting...");
            client.close();
        }
    }

    private void actionSetupCollection() {
        log.info("Setting up new working collection...");
        AtomicInteger index = new AtomicInteger(1);
        final Map<Integer, Collection> collections = new HashMap<>();
        log.info(". Available collections:");
        client.getCollections().forEach(col -> {
            String selected = " [ ] ";
            if(col.getFullName().equals(selectedCollection.getFullName())) {
                selected = " [X] ";
            }
            log.info(" {}. {}{}",index.incrementAndGet(), selected,col.getFullName());
            collections.put(index.get(), col);
        });
        showSubMenuSetup(collections);

    }

    private void showSubMenuSetup(Map<Integer, Collection> collections) {
        try (Scanner scanner = new Scanner(System.in);) {
            log.info(" - Type the collection number to set as working collection: ");
            int option = System.in.read();
            scanner.nextLine(); // Consume newline
            log.info("** *************************************** **");
            log.info("");
            if(collections.containsKey(option)) {
                selectedCollection = collections.get(option);
                log.info(" - Working collection set to: " + selectedCollection.getFullName());
            } else {
                log.info("No valid Option. Keeping the current working collection: " + selectedCollection.getFullName());
            }
        } catch (IOException e) {
            log.error("Error in the application: " + e.getMessage());
        }
    }

    private void actionExit() {
        enabled.set(false);
        client.close();
        System.exit(0);
    }

    private void actionList(Collection collection) {
        try (ResultSet rs = client.getDatabase().createQuery(FIND_ALL.formatted(collection.getFullName())).execute()) {
            rs.forEach(doc -> {
                try {
                    log.info(" - " + doc.toJSON());
                } catch (CouchbaseLiteException e) {
                    log.error("Error showing document: " + e.getMessage());
                }
            });
        } catch (CouchbaseLiteException e) {
            log.error("Error listing documents: " + e.getMessage());
        }
    }

    private void actionListAll() {
        client.getCollections().forEach(this::actionList);
    }

    private void actionCount() {
        final AtomicLong total = new AtomicLong(0);
        client.getCollections().forEach(col -> {
            long collectionDocs = col.getCount();
            log.info(" - {}: {}", col.getFullName(), collectionDocs);
            total.addAndGet(collectionDocs);
        });
        log.info("Total documents in the local database: {}", total);
    }

    private void actionCreate() {
        long seq = client.count()+1;
        for(int i = 0; i < 6; i++) {
            String id = "test:"+String.format("%05d", seq++);
            actionCreateOne(id);
        }
    }

    private void actionUpdate(String id) {
        try {
            Document doc = selectedCollection.getDocument(id);
            if(doc != null) {
                MutableDocument mDoc = doc.toMutable();
                String value = "value "+System.currentTimeMillis();
                mDoc.setString("myproperty", value);
                selectedCollection.save(mDoc);
                log.info("+ Updated Document: " + id + " with property: 'value' = " + value);
            } else {
                log.info("Document not found: " + id);
            }
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException(e);
        }
    }

    private void actionCreateOne(String id) {
        try {
            selectedCollection.save(new MutableDocument(id)
                    .setString("rfid", "123456789")
                    .setString("type", selectedCollection.getName())
                    .setArray("channels", DEFAULT_CHANNELS)
                    .setArray("destination", DEFAULT_CHANNELS));
            log.info("+ Created Document: " + id);
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            show();
        } catch (Exception e) {
            log.error("Error in the application: " + e.getMessage(), e);
        } finally {
            log.info("Exiting...");
            client.close();
            System.exit(0);
        }
    }
}
