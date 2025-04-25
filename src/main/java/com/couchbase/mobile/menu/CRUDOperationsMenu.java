package com.couchbase.mobile.menu;

import com.couchbase.lite.*;
import com.couchbase.mobile.client.ClientLite;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Scanner;
import java.util.function.Supplier;

@Slf4j
public class CRUDOperationsMenu extends AbstractMenu {
    private final static String SUBMENU_TITLE = "CRUD Operations";
    private final Array DEFAULT_CHANNELS = new MutableArray()
            .addString("channel1")
            .addString("channel2")
            .addString("blue");
    private final Map<String, Runnable> CRUD_ACTIONS = Map.of(
            "Create documents", this::actionCreate,
            "Update documents", this::actionUpdate,
            "List documents", this::actionList
    );

    private final ClientLite clientLite;
    private final Supplier<Collection> getDefaultCollection;

    public CRUDOperationsMenu(Scanner scanner, ClientLite clientLite, Supplier<Collection> getDefaultCollection) {
        super(scanner, SUBMENU_TITLE);
        this.clientLite = clientLite;
        this.getDefaultCollection = getDefaultCollection;
        actions.putAll(CRUD_ACTIONS);
    }

    private void actionList() {
        log.info("{} Documents in the local database: {}" , getDefaultCollection.get().getFullName(),  getDefaultCollection.get().getCount());
        clientLite.printAll(getDefaultCollection.get());
    }


    private void actionUpdate() {
        log.info("Updating document in `{}` collection...", getDefaultCollection.get().getFullName());
        log.info(" - Type the document's Id to update: ");
        String docId = scanner.nextLine();
        actionUpdate(docId);
    }


    private void actionUpdate(String id) {
        try {
            Document doc = getDefaultCollection.get().getDocument(id);
            if(doc != null) {
                MutableDocument mDoc = doc.toMutable();
                String value = "value "+System.currentTimeMillis();
                mDoc.setString("myproperty", value);
                getDefaultCollection.get().save(mDoc);
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
            getDefaultCollection.get().save(new MutableDocument(id)
                    .setString("rfid", "123456789")
                    .setString("type", getDefaultCollection.get().getName())
                    .setArray("channels", DEFAULT_CHANNELS)
                    .setArray("destination", DEFAULT_CHANNELS));
            log.info("+ Created Document: " + id);
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException(e);
        }
    }


    private void actionCreate() {
        long seq = this.getDefaultCollection.get().getCount()+1;
        for(int i = 0; i < 6; i++) {
            String id = "test:"+String.format("%05d", seq++);
            actionCreateOne(id);
        }
    }

}
