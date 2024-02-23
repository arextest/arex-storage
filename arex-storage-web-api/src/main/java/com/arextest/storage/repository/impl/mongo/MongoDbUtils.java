package com.arextest.storage.repository.impl.mongo;

import com.arextest.model.mock.Mocker;
import com.arextest.storage.beans.MongoConfigProperties;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

/**
 * @author jmo
 * @since 2022/2/17
 */
public final class MongoDbUtils {

  private static final String AREX_STORAGE_DB = "arex_storage_db";
  private static final String DEFAULT_CONNECTION_STRING = "mongodb://localhost";

  public static MongoDatabase create(String host, MongoConfigProperties mongoConfigProperties) {
    MongoDatabase mongoDatabase = getMongoClient(host, mongoConfigProperties);
    AREXMockerCodecProvider arexMockerCodecProvider =
        AREXMockerCodecProvider.builder()
            .targetCodec(new CompressionCodecImpl<>(Mocker.Target.class)).build();
    return mongoDatabase.withCodecRegistry(
        customCodecRegistry(Collections.singletonList(arexMockerCodecProvider)));
  }

  public static MongoDatabase create(String host, MongoConfigProperties mongoConfigProperties,
      AdditionalCodecProviderFactory additionalCodecProviderFactory) {
    MongoDatabase mongoDatabase = getMongoClient(host, mongoConfigProperties);
    additionalCodecProviderFactory.setMongoDatabase(mongoDatabase);
    List<CodecProvider> additionalCodecProviders = additionalCodecProviderFactory.get();
    return mongoDatabase.withCodecRegistry(customCodecRegistry(additionalCodecProviders));
  }

  /**
   * any custom item should be first except pojo
   *
   * @return the combinatorial CodecRegistry
   */
  public static CodecRegistry customCodecRegistry(List<CodecProvider> additionalCodecProviders) {
    List<CodecProvider> codecProviderList =
        new ArrayList<>(
            Optional.ofNullable(additionalCodecProviders).orElse(Collections.emptyList()));
    codecProviderList.add(PojoCodecProvider.builder().automatic(true).build());
    CodecRegistry customPojo = CodecRegistries.fromProviders(codecProviderList);
    return CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
        customPojo);
  }

  private static MongoDatabase getMongoClient(String host,
      MongoConfigProperties mongoConfig) {
    MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();

    applyMongoConfig(mongoConfig, settingsBuilder);

    if (StringUtils.isEmpty(host)) {
      host = DEFAULT_CONNECTION_STRING;
    }
    ConnectionString connectionString = new ConnectionString(host);
    String dbName = connectionString.getDatabase();
    if (dbName == null) {
      dbName = AREX_STORAGE_DB;
    }
    settingsBuilder.applyConnectionString(connectionString);
    MongoClient mongoClient = MongoClients.create(settingsBuilder.build());
    return mongoClient.getDatabase(dbName);
  }

  private static void applyMongoConfig(MongoConfigProperties mongoConfig,
      MongoClientSettings.Builder settingsBuilder) {
    if (mongoConfig.getMaxIdleTime() != null) {
      settingsBuilder.applyToConnectionPoolSettings(
          builder -> builder.maxConnectionIdleTime(mongoConfig.getMaxIdleTime(),
              TimeUnit.MILLISECONDS));
    }
    if (mongoConfig.getConnectTimeout() != null || mongoConfig.getSocketTimeout() != null) {
      settingsBuilder.applyToSocketSettings(builder -> {
        if (mongoConfig.getConnectTimeout() != null) {
          builder.connectTimeout(Math.toIntExact(mongoConfig.getConnectTimeout()),
              TimeUnit.MILLISECONDS);
        }
        if (mongoConfig.getSocketTimeout() != null) {
          builder.readTimeout(Math.toIntExact(mongoConfig.getSocketTimeout()),
              TimeUnit.MILLISECONDS);
        }
      });
    }
    if (mongoConfig.getServerSelectionTimeout() != null) {
      settingsBuilder.applyToClusterSettings(
          builder -> builder.serverSelectionTimeout(mongoConfig.getServerSelectionTimeout(),
              TimeUnit.MILLISECONDS));
    }
  }
}