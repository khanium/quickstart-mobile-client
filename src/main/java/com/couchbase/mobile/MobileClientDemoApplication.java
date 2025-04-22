package com.couchbase.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
public class MobileClientDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MobileClientDemoApplication.class, args);
    }

}
