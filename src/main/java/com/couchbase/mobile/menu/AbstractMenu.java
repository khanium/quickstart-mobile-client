package com.couchbase.mobile.menu;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class AbstractMenu implements Menu {
    private static final String DEFAULT_EXIT_MESSAGE = "Back to Main Menu";
    @Setter
    private String title;
    private String exitMessage = "Exiting menu...";
    protected final Map<String,Runnable> actions = new HashMap<>();
    protected final Map<Integer, Map.Entry<String, Runnable>> options = new HashMap<>();
    protected final Scanner scanner;
    protected final AtomicBoolean enable = new AtomicBoolean(true);

    public AbstractMenu(Scanner scanner, String title) {
       this(scanner, title, DEFAULT_EXIT_MESSAGE);
    }

    public AbstractMenu(Scanner scanner, String title, String exitMessage) {
        this.title = title;
        this.scanner = scanner;
        this.exitMessage = exitMessage;
    }

    protected void buildActions() {

    }

    @Override
    public void show() {
        // Default implementation (if any)
        enable.set(true);
        do {
            buildActions();
            displayMenu();
            char choice = getUserChoice();
            performAction(choice);
        }while (enable.get());
    }

    protected void exit() {
        enable.set(false);
        log.info("{}", exitMessage);
    }

    protected void performAction(char choice) {
        int option = choice - '0';
        Map.Entry<String,Runnable> action = options.get(option);
        if (action != null && action.getValue() != null) {
            action.getValue().run();
        } else {
            log.warn("Invalid {} choice. Please try again.", choice);
        }
    }

    protected char getUserChoice() {
        char choice = '0';
        try {
            choice = (char) System.in.read();
            scanner.nextLine(); // Consume newline
        } catch (Exception e) {
            log.error("Error reading user choice: {}", e.getMessage());
        }
        return choice;
    }

    protected void buildDisplayBuildOptions() {
        int i = 1;
        for (String action : actions.keySet()) {
            options.put(i++, new AbstractMap.SimpleEntry<>(action, actions.get(action)));
        }
        options.put(0, new AbstractMap.SimpleEntry<>(exitMessage, this::exit));
    }

    private void displayMenu() {
        log.info("** *************************************** **");
        log.info("** {} Menu: ", title);
        buildDisplayBuildOptions();
        options.forEach((k, v) -> {
            log.info("      {}. {}", k, v.getKey());
        });
        log.info("** ");
        log.info("** Please enter your choice: ");
    }
}
