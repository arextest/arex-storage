package com.arextest.storage.beans;

import com.arextest.config.model.dao.config.AppCollection;
import com.arextest.config.model.dao.config.InstancesCollection;
import com.arextest.config.model.dao.config.RecordServiceConfigCollection;
import com.arextest.config.model.dao.config.ServiceOperationCollection;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.repository.ProviderNames;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
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

  void ensureMockerQueryIndex(MongoDatabase database) {
    for (MockCategoryType category : MockCategoryType.DEFAULTS) {
      MongoCollection<AREXMocker> collection =
          database.getCollection(getCollectionName(category), AREXMocker.class);
      try {
        Document index = new Document();
        index.append(AREXMocker.Fields.recordId, 1);
        index.append(AREXMocker.Fields.creationTime, -1);
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

  void setTtlIndexes(MongoDatabase mongoDatabase) {
    for (MockCategoryType category : MockCategoryType.DEFAULTS) {
      setTTLIndexInMockerCollection(category, mongoDatabase);
    }
  }

  void setIndexAboutConfig(MongoDatabase mongoDatabase) {

    // AppCollection
    MongoCollection<AppCollection> appCollectionMongoCollection =
        mongoDatabase.getCollection(AppCollection.DOCUMENT_NAME, AppCollection.class);
    appCollectionMongoCollection.createIndex(new Document(AppCollection.Fields.appId, 1),
        new IndexOptions().unique(true));

    // RecordServiceConfigCollection
    MongoCollection<RecordServiceConfigCollection> recordServiceConfigCollectionMongoCollection = mongoDatabase
        .getCollection(RecordServiceConfigCollection.DOCUMENT_NAME,
            RecordServiceConfigCollection.class);
    recordServiceConfigCollectionMongoCollection.createIndex(
        Indexes.ascending(RecordServiceConfigCollection.Fields.appId),
        new IndexOptions().unique(true));

    // ServiceOperationCollection
    MongoCollection<ServiceOperationCollection> serviceOperationCollectionMongoCollection =
        mongoDatabase.getCollection(ServiceOperationCollection.DOCUMENT_NAME,
            ServiceOperationCollection.class);
    serviceOperationCollectionMongoCollection.createIndex(
        Indexes.ascending(ServiceOperationCollection.Fields.appId,
            ServiceOperationCollection.Fields.serviceId,
            ServiceOperationCollection.Fields.operationName),
        new IndexOptions().unique(true));

    // InstancesCollection
    Bson instancesTtlIndex = Indexes.ascending(InstancesCollection.Fields.dataUpdateTime);
    IndexOptions instancesTtlIndexOptions = new IndexOptions().expireAfter(65L, TimeUnit.SECONDS);
    MongoCollection<InstancesCollection> instancesCollectionMongoCollection =
        mongoDatabase.getCollection(InstancesCollection.DOCUMENT_NAME, InstancesCollection.class);
    try {
      instancesCollectionMongoCollection.createIndex(instancesTtlIndex, instancesTtlIndexOptions);
    } catch (Throwable throwable) {
      instancesCollectionMongoCollection.dropIndex(instancesTtlIndex);
      instancesCollectionMongoCollection.createIndex(instancesTtlIndex, instancesTtlIndexOptions);
    }
  }

  private void setTTLIndexInMockerCollection(MockCategoryType category,
      MongoDatabase mongoDatabase) {
    String categoryName = getCollectionName(category);
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

  private String getCollectionName(MockCategoryType category) {
    return ProviderNames.DEFAULT + category.getName() + COLLECTION_SUFFIX;
  }
}
