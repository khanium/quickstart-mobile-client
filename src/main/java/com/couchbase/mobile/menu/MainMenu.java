package com.couchbase.mobile.menu;

import com.couchbase.lite.*;
import com.couchbase.mobile.client.ClientLite;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.couchbase.lite.ReplicatorActivityLevel.IDLE;
import static com.couchbase.lite.ReplicatorActivityLevel.STOPPED;

@Slf4j
public class MainMenu extends AbstractMenu implements Runnable, AutoCloseable {
    public static final String DEFAULT_TITLE = "Main Menu";
    private static final String DEFAULT_EXIT_MESSAGE = "Exit";
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final ClientLite client;
    private final CollectionMenu collectionMenu;
    private final CRUDOperationsMenu crudOperationsMenu;
    private final CustomUseCaseMenu customUseCaseSubMenu;

    public MainMenu(ClientLite client) {
        super(new Scanner(System.in), DEFAULT_TITLE,DEFAULT_EXIT_MESSAGE);
        this.client = client;
        this.collectionMenu = new CollectionMenu(scanner, client);
        this.crudOperationsMenu = new CRUDOperationsMenu(scanner, client, collectionMenu::getSelectedCollection);
        this.customUseCaseSubMenu = new CustomUseCaseMenu(scanner, client);
    }

    @Override
    protected void buildActions() {
        actions.clear();
        actions.put("Choose the default working collection (`%s`)".formatted(collectionMenu.getSelectedCollection().getFullName()),  collectionMenu::show);
        actions.put("CRUD Operations on documents", crudOperationsMenu::show);
        actions.put("List All documents", client::printAll);
        actions.put("Count documents in the local database", this::actionCount);
        actions.put("Custom Use Cases", customUseCaseSubMenu::show);
        actions.put("%s Replication".formatted(client.isStarted() ? "Stop" : "Start"), this::actionStartStopReplication);
    }

    private void awaitUntil(ReplicatorActivityLevel status) {
        //TODO via StatusChangeListener
        while (!client.getReplicator().getStatus().getActivityLevel().equals(status)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Error waiting for replication to stop: " + e.getMessage());
            }
        }
    }

    private void actionStartStopReplication() {
        if (client.isStarted()) {
            log.info("Stopping replication...");
            client.stop();
            awaitUntil(STOPPED);
        } else {
            log.info("Starting replication...");
            client.start();
            awaitUntil(IDLE);
        }
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

    @Override
    public void run() {
        try {
            show();
        } catch (Exception e) {
            log.error("Error in the application: " + e.getMessage(), e);
        } finally {
            close();
            System.exit(0);
        }
    }

    @Override
    public void close() {
        log.info("closing in 5 seconds...");
        if (scanner != null) {
            scanner.close();
        }
        if (client != null) {
            client.close();
        }
        enabled.set(false);
        try {
            Thread.sleep(5000); // Give time to close the database
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
