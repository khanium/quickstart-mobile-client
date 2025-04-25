package com.couchbase.mobile.menu;

import com.couchbase.lite.Collection;
import com.couchbase.mobile.client.ClientLite;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Slf4j
public class CollectionMenu extends AbstractMenu {
    public static final String title = "Collection CRUD Operations";
    private final ClientLite clientLite;
    private Collection collection;

    public CollectionMenu(Scanner scanner, ClientLite clientLite) {
        super(scanner, title);
        this.collection = clientLite.getCollections().stream().findAny().get();
        this.clientLite = clientLite;
    }

    public Collection getSelectedCollection() {
        return collection;
    }

    @Override
    protected void buildActions() {
        actions.clear();
        actions.putAll(extractActions());
    }

    private Map<String, Runnable> extractActions() {
        Map<String, Runnable> actions = new HashMap<>();
        clientLite.getCollections().forEach(col -> {
            if(!col.getFullName().equals(collection.getFullName())) {
                actions.put("Switch to collection: " + col.getFullName(), () -> {
                    setDefaultCollection(col);
                    log.info(" - Working collection set to: " + collection.getFullName());
                });
            }
        });
        return actions;
    }

    private void setDefaultCollection(Collection collection) {
        this.collection = collection;
    }

}
