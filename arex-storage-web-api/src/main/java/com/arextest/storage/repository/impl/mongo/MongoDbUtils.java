package com.arextest.storage.repository.impl.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.mongodb4.MongoDb4DocumentObject;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Map;

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
        MongoDb4DocumentObjectCodecProvider mongoDb4DocumentObjectCodecProvider = new MongoDb4DocumentObjectCodecProvider();
        return CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                customPojo,CodecRegistries.fromProviders(mongoDb4DocumentObjectCodecProvider));
    }


    public static class MongoDb4DocumentObjectCodecProvider implements CodecProvider {
        @Override
        public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
            if (MongoDb4DocumentObject.class.equals(clazz))
                return (Codec<T>) new MongoDb4DocumentObjectCodec(registry);
            return null;
        }

        private class MongoDb4DocumentObjectCodec implements Codec<MongoDb4DocumentObject> {

            private final Codec<Document> documentCodec;

            public MongoDb4DocumentObjectCodec(CodecRegistry registry) {
                documentCodec = registry.get(Document.class);
            }

            @Override
            public MongoDb4DocumentObject decode(BsonReader reader, DecoderContext decoderContext) {
                Document document = documentCodec.decode(reader, decoderContext);
                MongoDb4DocumentObject mongoDb4DocumentObject = new MongoDb4DocumentObject();
                for (Map.Entry<String, Object> entry : document.entrySet()) {
                    mongoDb4DocumentObject.set(entry.getKey(), entry.getValue());
                }
                return mongoDb4DocumentObject;
            }

            @Override
            public void encode(BsonWriter writer, MongoDb4DocumentObject value, EncoderContext encoderContext) {
                documentCodec.encode(writer, value.unwrap(), encoderContext);
            }

            @Override
            public Class<MongoDb4DocumentObject> getEncoderClass() {
                return MongoDb4DocumentObject.class;
            }
        }
    }
}