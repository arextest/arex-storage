package com.arextest.storage.repository.impl.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

/**
 * @author jmo
 * @since 2022/2/17
 */
public final class MongoDbUtils {
    private MongoDbUtils() {
    }

    private static final String AREX_STORAGE_DB = "arex_storage_db";
    private static final String DEFAULT_CONNECTION_STRING = "mongodb://localhost";

    public static MongoDatabase create(String host) {
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();
        if (StringUtils.isEmpty(host)) {
            host = DEFAULT_CONNECTION_STRING;
        }
        ConnectionString connectionString = new ConnectionString(host);
        String dbName = connectionString.getDatabase();
        if (dbName == null) {
            dbName = AREX_STORAGE_DB;
        }
        settingsBuilder.applyConnectionString(connectionString);
        settingsBuilder.codecRegistry(customCodecRegistry());
        MongoClient mongoClient = MongoClients.create(settingsBuilder.build());
        return mongoClient.getDatabase(dbName);
    }

    /**
     * any custom item should be first except pojo
     *
     * @return the combinatorial CodecRegistry
     */
    public static CodecRegistry customCodecRegistry() {
        AREXMockerCodecProvider arexMockerCodecProvider = new AREXMockerCodecProvider();
        final CodecRegistry customPojo = CodecRegistries.fromProviders(arexMockerCodecProvider, PojoCodecProvider
                .builder().automatic(true).build());
        return CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                customPojo);
    }
}