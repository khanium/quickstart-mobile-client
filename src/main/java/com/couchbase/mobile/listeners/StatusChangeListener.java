package com.couchbase.mobile.listeners;

import com.couchbase.lite.ReplicatorActivityLevel;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.mobile.client.ClientLite;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class StatusChangeListener implements ReplicatorChangeListener {

    private final ClientLite client;

    public StatusChangeListener(ClientLite client) {
        this.client = client;
    }

    @Override
    public void changed(ReplicatorChange change) {

            if (change.getStatus().getError() != null) {
                log.error("Error in replication ( {} ): {}",change.getStatus().getActivityLevel(), change.getStatus().getError().getMessage());
            } else {
                log.info("Replication in progress: " + change.getStatus().getActivityLevel());
                // TODO add replication status change callback here. i.e. sync replication icon of started, stopped, etc.
            }

            if(change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.IDLE) || change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.STOPPED)){
                log.info("Documents in the local database: {}", client.count());
                if (change.getReplicator().getConfig().isContinuous() && change.getStatus().getActivityLevel().equals(ReplicatorActivityLevel.STOPPED)) {
                    log.error("Replication stopped unexpectedly!");
                    // TODO replication error handle here
                    // log.info("Starting replication again...");
                    // change.getReplicator().start(); // TODO add a maximum number of retries and/or backoff strategy
                }
            }
    }
}
