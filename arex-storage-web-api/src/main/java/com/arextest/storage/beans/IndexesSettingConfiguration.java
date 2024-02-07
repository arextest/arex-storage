package com.arextest.storage.beans;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.LockWrapper;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.enums.MongoCollectionIndexConfigEnum;
import com.arextest.storage.enums.MongoCollectionIndexConfigEnum.FieldConfig;
import com.arextest.storage.enums.MongoCollectionIndexConfigEnum.TtlIndexConfig;
import com.arextest.storage.repository.ProviderNames;
import com.mongodb.MongoCommandException;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class IndexesSettingConfiguration {

  private static final String EXPIRATION_TIME_COLUMN_NAME = "expirationTime";
  private static final String COLLECTION_SUFFIX = "Mocker";
  private static final String BACKGROUND = "background";
  private static final String UNIQUE = "unique";
  private static final String EXPIRE_AFTER_SECONDS = "expireAfterSeconds";
  private static final String ID = "_id_";
  private static final String KEY = "key";
  private static final String NAME = "name";
  private static final String REDIS_KEY_VERSION = "storage_version_%s";

  // increment this version when you want to recreate indexes
  private static final int INDEX_VERSION = 1;

  @Resource
  private CacheProvider cacheProvider;

  public void setIndexes(MongoDatabase mongoDatabase) {
    Runnable runnable = () -> {
      LockWrapper lock = cacheProvider.getLock("setIndexes");
      try {
        lock.lock(30, TimeUnit.SECONDS);
        byte[] versionKey = CacheKeyUtils.toUtf8Bytes(String.format(REDIS_KEY_VERSION, INDEX_VERSION));
        if (cacheProvider.get(versionKey) != null) {
          LOGGER.info("indexes already set");
          return;
        } else {
          cacheProvider.put(versionKey, CacheKeyUtils.toUtf8Bytes("1"));
        }
        long timestamp = System.currentTimeMillis();
        LOGGER.info("start to set indexes");
        for (MongoCollectionIndexConfigEnum indexConfigEnum : MongoCollectionIndexConfigEnum.values()) {
          try {
            setIndexByEnum(indexConfigEnum, mongoDatabase);
          } catch (Exception e) {
            LOGGER.error("set index failed for {}", indexConfigEnum.getCollectionName(), e);
          }
        }
        ensureMockerQueryIndex(mongoDatabase);
        LOGGER.info("set indexes success. cost: {}ms", System.currentTimeMillis() - timestamp);
      } catch (Exception e) {
        LOGGER.error("set indexes failed", e);
      } finally {
        lock.unlock();
      }
    };
    Thread thread = new Thread(runnable);
    thread.start();
  }

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
          ListIndexesIterable<Document> indexes = collection.listIndexes();
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

          try {
            Document index = new Document();
            index.append(AREXMocker.Fields.recordId, 1);
            index.append(AREXMocker.Fields.creationTime, -1);
            if (isIndexExist(indexes, index, null)) {
              collection.dropIndex(index);
            }
          } catch (MongoCommandException e) {
            LOGGER.info("drop index failed for {}", category.getName(), e);
          }

          if (providerName.equals(ProviderNames.DEFAULT)) {
            setTTLIndexInMockerCollection(category, database);
          }
        }
    }
  }

  private void setTTLIndexInMockerCollection(MockCategoryType category,
      MongoDatabase mongoDatabase) {
    String categoryName = getCollectionName(category, ProviderNames.DEFAULT);
    MongoCollection<AREXMocker> collection = mongoDatabase.getCollection(categoryName,
        AREXMocker.class);
    Bson index = new Document(EXPIRATION_TIME_COLUMN_NAME, 1);
    IndexOptions indexOptions = new IndexOptions().expireAfter(0L, TimeUnit.SECONDS);
    indexOptions.background(true);
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

  private void setIndexByEnum(MongoCollectionIndexConfigEnum indexConfigEnum, MongoDatabase mongoDatabase) {
      MongoCollection<Document> collection = mongoDatabase.getCollection(
          indexConfigEnum.getCollectionName());

      ListIndexesIterable<Document> existedIndexes = collection.listIndexes();

      List<Pair<Document, IndexOptions>> toAddIndexes = new ArrayList<>();

      indexConfigEnum.getIndexConfigs().forEach(indexConfig -> {
        List<FieldConfig> fieldConfigs = indexConfig.getFieldConfigs();
        Document index = new Document();
        for (FieldConfig fieldConfig : fieldConfigs) {
          index.append(fieldConfig.getFieldName(),
              fieldConfig.getAscending() != Boolean.FALSE ? 1 : -1);
        }
        IndexOptions indexOptions = new IndexOptions();
        if (indexConfig.getUnique() != null) {
          indexOptions.unique(indexConfig.getUnique());
        }
        if (indexConfig.getTtlIndexConfig() != null) {
          TtlIndexConfig ttlIndexConfig = indexConfig.getTtlIndexConfig();
          indexOptions.expireAfter(ttlIndexConfig.getExpireAfter(), ttlIndexConfig.getTimeUnit());
        }
        indexOptions.background(true);
        toAddIndexes.add(Pair.of(index, indexOptions));
      });

      // remove old indexes which not exist in toAddIndexes
      for (Document existedIndex : existedIndexes) {
        if (!Objects.equals(existedIndex.getString(NAME), ID) &&
            !isIndexExist(toAddIndexes, existedIndex)) {
          collection.dropIndex(existedIndex.get(KEY, Document.class));
        }
      }

      // add new indexes which not exist
      for (Pair<Document, IndexOptions> newIndex : toAddIndexes) {
        if (!isIndexExist(existedIndexes, newIndex.getLeft(), newIndex.getRight())) {
          collection.createIndex(newIndex.getLeft(), newIndex.getRight());
        }
      }

  }

  private boolean isIndexExist(Iterable<Document> existedIndexes, Document index,
      IndexOptions indexOptions) {
    for (Document oldIndex : existedIndexes) {
      if (isMatch(oldIndex, index, indexOptions)) {
        return true;
      }
    }
    return false;
  }

  private boolean isIndexExist(Iterable<Pair<Document, IndexOptions>> indexes, Document index) {
    for (Pair<Document, IndexOptions> newIndexPair : indexes) {
      Document document = newIndexPair.getLeft();
      IndexOptions indexOptions = newIndexPair.getRight();
      if (isMatch(index, document, indexOptions)) {
        return true;
      }
    }
    return false;
  }

  private boolean isMatch(Document oldIndex, Document newIndex, IndexOptions newIndexOptions) {
    Document key = (Document) oldIndex.get(KEY);
    if (!newIndex.equals(key)) {
      return false;
    }
    if (newIndexOptions == null) {
      return false;
    }
    if (!Objects.equals(newIndexOptions.isBackground(), oldIndex.getBoolean(BACKGROUND))) {
      return false;
    }
    if (!Objects.equals(newIndexOptions.isUnique(), oldIndex.getBoolean(UNIQUE))) {
      return false;
    }
    if (!Objects.equals(newIndexOptions.getExpireAfter(TimeUnit.SECONDS),
        oldIndex.getLong(EXPIRE_AFTER_SECONDS))) {
      return false;
    }
    return true;
  }
}
