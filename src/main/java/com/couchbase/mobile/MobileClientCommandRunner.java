package com.couchbase.mobile;

import com.couchbase.mobile.client.ClientLite;
import com.couchbase.mobile.menu.MainMenu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static com.couchbase.lite.ReplicatorActivityLevel.IDLE;
import static com.couchbase.lite.ReplicatorActivityLevel.STOPPED;

@Slf4j
@Component
public class MobileClientCommandRunner implements CommandLineRunner {

    private final ClientLite client;
    private final Thread backgroundMenu;

    public MobileClientCommandRunner(ClientLite client) {
        // Constructor
        this.client = client;
        this.backgroundMenu = new Thread(new MainMenu(client));
    }

    private void welcome() {
        log.info("Welcome to Couchbase Lite Mobile Client");
        log.info("Starting the client...");
    }

    private void showMenu(){
        client.getReplicator().addChangeListener(change -> {
            if (!change.getReplicator().isClosed() && backgroundMenu.isAlive() && (change.getStatus().getActivityLevel().equals(IDLE) || change.getStatus().getActivityLevel().equals(STOPPED))) {
                log.info("Replication completed successfully.");
                //TODO add triggered event here to awake the menu in case it is waiting for the replication to finish
            }
        });
        backgroundMenu.start();
    }

    @Override
    public void run(String... args) throws Exception {
        welcome();
        showMenu();
    }
}
