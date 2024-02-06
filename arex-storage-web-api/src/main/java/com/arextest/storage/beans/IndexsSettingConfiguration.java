package com.arextest.storage.beans;

import com.arextest.model.enums.MongoIndexConfigEnum;
import com.arextest.model.enums.MongoIndexConfigEnum.FieldConfig;
import com.arextest.model.enums.MongoIndexConfigEnum.TtlIndexConfig;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.repository.ProviderNames;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class IndexsSettingConfiguration {

  static final String EXPIRATION_TIME_COLUMN_NAME = "expirationTime";
  private static final String COLLECTION_SUFFIX = "Mocker";

  private void ensureMockerQueryIndex(MongoDatabase database) {
    for (MockCategoryType category : MockCategoryType.DEFAULTS) {
      for (Field field : ProviderNames.class.getDeclaredFields()) {
        String providerName = null;
        try {
          providerName = (String) field.get(ProviderNames.class);
        } catch (IllegalAccessException e) {
          LOGGER.error("get provider name failed", e);
          continue;
        }

        MongoCollection<AREXMocker> collection =
            database.getCollection(getCollectionName(category, providerName),
                AREXMocker.class);

        try {
          collection.dropIndexes();
        } catch (MongoCommandException e) {
          LOGGER.info("drop index failed for {}", category.getName(), e);
        }

        try {
          Document index = new Document();
          index.append(AREXMocker.Fields.recordId, 1);
          collection.createIndex(index);
        } catch (MongoCommandException e) {
          LOGGER.info("create index failed for {}", category.getName(), e);
        }

        try {
          Document index = new Document();
          index.append(AREXMocker.Fields.appId, 1);
          index.append(AREXMocker.Fields.operationName, 1);
          collection.createIndex(index);
        } catch (MongoCommandException e) {
          LOGGER.info("create index failed for {}", category.getName(), e);
        }
      }
    }
  }

  private void setTtlIndexes(MongoDatabase mongoDatabase) {
    for (MockCategoryType category : MockCategoryType.DEFAULTS) {
      setTTLIndexInMockerCollection(category, mongoDatabase);
    }
  }

  private void setTTLIndexInMockerCollection(MockCategoryType category,
      MongoDatabase mongoDatabase) {
    String categoryName = getCollectionName(category, ProviderNames.DEFAULT);
    MongoCollection<AREXMocker> collection = mongoDatabase.getCollection(categoryName,
        AREXMocker.class);
    Bson index = new Document(EXPIRATION_TIME_COLUMN_NAME, 1);
    IndexOptions indexOptions = new IndexOptions().expireAfter(0L, TimeUnit.SECONDS);
    try {
      collection.createIndex(index, indexOptions);
    } catch (MongoCommandException e) {
      // ignore
      collection.dropIndex(index);
      collection.createIndex(index, indexOptions);
    }
  }

  private String getCollectionName(MockCategoryType category, String providerName) {
    return providerName + category.getName() + COLLECTION_SUFFIX;
  }

  public void setIndexes(MongoDatabase mongoDatabase) {
    Runnable runnable = () -> {
      try {
        long timestamp = System.currentTimeMillis();
        LOGGER.info("start to set indexes");
        MongoIndexConfigEnum.INDEX_CONFIGS.forEach(indexConfigEnum -> {
          setIndexByEnum(indexConfigEnum, mongoDatabase);
        });
        setTtlIndexes(mongoDatabase);
        ensureMockerQueryIndex(mongoDatabase);
        LOGGER.info("set indexes success. cost: {}ms", System.currentTimeMillis() - timestamp);
      } catch (Exception e) {
        LOGGER.error("set indexes failed", e);
      }
    };
    Thread thread = new Thread(runnable);
    thread.start();
  }

  private void setIndexByEnum(MongoIndexConfigEnum indexConfigEnum, MongoDatabase mongoDatabase) {
    MongoCollection<Document> collection = mongoDatabase.getCollection(
        indexConfigEnum.getCollectionName());

    List<FieldConfig> fieldConfigs = indexConfigEnum.getFieldConfigs();
    Document index = new Document();
    for (FieldConfig fieldConfig : fieldConfigs) {
      index.append(fieldConfig.getFieldName(), fieldConfig.getAscending() != Boolean.FALSE ? 1 : -1);
    }
    IndexOptions indexOptions = new IndexOptions();
    if (indexConfigEnum.getUnique() != null) {
      indexOptions.unique(indexConfigEnum.getUnique());
    }
    if (indexConfigEnum.getTtlIndexConfig() != null) {
      TtlIndexConfig ttlIndexConfig = indexConfigEnum.getTtlIndexConfig();
      indexOptions.expireAfter(ttlIndexConfig.getExpireAfter(), ttlIndexConfig.getTimeUnit());
    }
    try {
      collection.createIndex(index, indexOptions);
    } catch (MongoCommandException e) {
      LOGGER.info("create index failed for {}", indexConfigEnum.getCollectionName(), e);
      collection.dropIndex(index);
      collection.createIndex(index, indexOptions);
    }
  }
}
