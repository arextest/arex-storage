package com.arextest.storage.core.repository.impl.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jmo
 * @since 2022/2/17
 */
public final class MongoDbUtils {
    private MongoDbUtils() {
    }

    private static final String DEFAULT_CONNECTION_STRING = "mongodb://localhost";
    public static MongoDatabase create(String host, String dbName) {
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();
        if (StringUtils.isEmpty(host)) {
            host = DEFAULT_CONNECTION_STRING;
        }
        settingsBuilder.applyConnectionString(new ConnectionString(host));
        settingsBuilder.codecRegistry(customCodecRegistry());
        MongoClient mongoClient = MongoClients.create(settingsBuilder.build());
        return mongoClient.getDatabase(dbName);
    }

    /**
     * any custom item should be first except pojo
     *
     * @return the combinatorial CodecRegistry
     */
    private static CodecRegistry customCodecRegistry() {
        final List<Convention> conventions = customConventions();
        final CodecRegistry customPojo = CodecRegistries.fromProviders(PojoCodecProvider
                .builder().conventions(conventions).automatic(true).build());
        return CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                customPojo);
    }

    public static final Convention MILLISECONDS_DATE_TIME_CONVENTION = new MillisecondsDateTimeConventionImpl();

    private static List<Convention> customConventions() {
        final List<Convention> conventions = new ArrayList<>(Conventions.DEFAULT_CONVENTIONS);
        conventions.add(MILLISECONDS_DATE_TIME_CONVENTION);
        return Collections.unmodifiableList(conventions);
    }
}