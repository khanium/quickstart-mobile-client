package com.couchbase.mobile.config;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.mobile.client.ClientBuilder;
import com.couchbase.mobile.client.ClientLite;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;


@EnableConfigurationProperties(CouchbaseLiteProperties.class)
@ConfigurationPropertiesScan
@Configuration
public class AppConfig {
    static {
        System.out.println("Starting CouchbaseLite at " + LocalTime.now());
        com.couchbase.lite.CouchbaseLite.init();
    }

    @Bean
    public ClientLite clientLite(CouchbaseLiteProperties properties) throws CouchbaseLiteException {
        return new ClientBuilder(properties).build();
    }

}
