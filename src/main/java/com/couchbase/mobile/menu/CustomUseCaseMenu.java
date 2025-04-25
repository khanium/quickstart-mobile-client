package com.couchbase.mobile.menu;

import com.couchbase.mobile.client.ClientLite;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

@Slf4j
public class CustomUseCaseMenu extends AbstractMenu {
    public static final String title = "Custom Use Case Menu";
    private final ClientLite client;
    private final Scanner scanner;

    public CustomUseCaseMenu(Scanner scanner, ClientLite client) {
        super(scanner, title);
        this.client = client;
        this.scanner = scanner;
    }

    @Override
    public void show() {
        //TODO add custom use case menu here
        log.info("TBD Custom use case menu here");
    }
}
