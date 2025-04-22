package com.couchbase.mobile.config;

import com.couchbase.lite.Endpoint;
import com.couchbase.lite.LogLevel;
import com.couchbase.lite.ReplicatorType;
import com.couchbase.lite.URLEndpoint;
import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.*;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.joining;

@Data
@ConfigurationProperties(prefix = "couchbase")
@ConfigurationPropertiesScan
public class CouchbaseLiteProperties {

    RemoteProperties remote = new RemoteProperties();
    LocalDBProperties local = new LocalDBProperties();
    LogProperties log = new LogProperties();

    public String toString() {
        return """
                
                couchbase:
                \t local: %s
                \t remote: %s
                \t log: %s
                """.formatted(local, remote, log);
    }


    @Data
    public static class CollectionProperties {
        List<String> channelsFilter = new ArrayList<>();
        List<String> documentIDsFilter = new ArrayList<>();


        public String toString() {
            return """
                    
                    \t\t\t\tdocumentIDs-filter: %s
                    \t\t\t\tchannels-filter: %s""".formatted(documentIDsFilter.isEmpty()? "--none--": join(",", documentIDsFilter),channelsFilter.isEmpty()? "--none--": join(",", channelsFilter));
        }
    }

    @Data
    public static class AuthenticatorProperties {
        String username="test";
        String password="password";

        public String toString() {
            return """
                    
                    \t\t\tusername: %s
                    \t\t\tpassword: %s""".formatted(username, "*".repeat(password.length()));
        }
    }

    @Data
    public static class RemoteProperties {
        public static final String DEFAULT_SERVER = "127.0.0.1";
        public static final int DEFAULT_PORT = 4984;
        public static final String DEFAULT_DATABASE = "db";
        public static final String DEFAULT_ENDPOINT_URL = "ws://%s:%d/%s".formatted(DEFAULT_SERVER, DEFAULT_PORT, DEFAULT_DATABASE);

        private String endpointUrl = DEFAULT_ENDPOINT_URL;
        private String certificatePath;
        private boolean continuous = false;
        private ReplicatorType replicatorType = ReplicatorType.PUSH_AND_PULL;
        private boolean resetCheckpoint = false;
        private Map<String, CollectionProperties> collections = new HashMap<>();
        private AuthenticatorProperties authenticator;

        public Endpoint getEndpoint() {
            Endpoint endpoint;
            try {
                endpoint = new URLEndpoint(new URI(endpointUrl));
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
            return endpoint;
        }

        private String collectionsToString() {
            return collections.entrySet().stream().map(e -> "\n\t\t\t%s: %s".formatted(e.getKey(), e.getValue())).collect(joining());
        }

        public String toString() {
            return """
                    
                    \t\tendpoint-url: %s                 
                    \t\tcontinuous: %b
                    \t\treplicator-type: %s
                    \t\treset-checkpoint: %b 
                    \t\tcollections: %s 
                    \t\tauthenticator: %s
                    """.formatted(endpointUrl, continuous, replicatorType, resetCheckpoint,collectionsToString(), authenticator);
        }
    }

    @Data
    public static class LocalDBProperties {
        String database = "demo";
        String dbPath = "data";
        ScopeProperties scope = new ScopeProperties();
        boolean flushPreviousDb = true;
        boolean autoPurge = true;
        String encryptionKey = null;


        public boolean isEncryptedDb() {
            return !Objects.isNull(this.encryptionKey) && !encryptionKey.isEmpty();
        }

        public String getDbFolderName() {
            return this.getDatabase()+".cblite2";
        }

        public File getDbFolderFile() {
            return new File(getDbPath(),getDbFolderName());
        }

        public File getSQLLiteDBFile() {
            return new File(new File(this.getDbPath(),getDbFolderName()),"db.sqlite3");
        }

        public String getSqliteURL() {
            return format("jdbc:sqlite:%s",this.getSQLLiteDBFile().getAbsolutePath());
        }

        public String toString() {
            return """
                    
                    \t\tdatabase: %s                   
                    \t\tdb-path: %s
                    \t\tscope: %s 
                    \t\tflush-previous-db: %b
                    \t\tauto-purge: %b
                    \t\tencryption: %b 
                    """.formatted(database,dbPath, scope,flushPreviousDb,autoPurge, isEncryptedDb());
        }

    }

    @Data
    public static class LogProperties {
      String path = "logs";
      LogLevel level = LogLevel.INFO;
      long maxSize = 10;
      int rotationCount = 10;
      boolean plainText = false;

      public String toString() {
          return """
                  
                  \t\tpath: %s
                  \t\tlevel: %s
                  \t\tmax-size: %d
                  \t\trotation-count: %d
                  \t\tplaintext: %b
                  """.formatted(path, level, maxSize, rotationCount, plainText);
      }
    }

    @Data
    public static class ScopeProperties {
        public static final String DEFAULT_SCOPE = "_default";
        public static final String DEFAULT_COLLECTION = "_default";

        String name = DEFAULT_SCOPE;
        List<String> collections = List.of(DEFAULT_COLLECTION);

        public String toString() {
            return """

                    \t\t\tname: %s
                    \t\t\tcollections: %s""".formatted(name, join(",", collections));
        }
    }
}
