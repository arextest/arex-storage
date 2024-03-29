package com.arextest.config.repository.impl;

import com.arextest.config.mapper.InstancesMapper;
import com.arextest.config.model.dao.config.DynamicClassCollection;
import com.arextest.config.model.dao.config.InstancesCollection;
import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

public class InstancesConfigurationRepositoryImpl implements
    ConfigRepositoryProvider<InstancesConfiguration> {

  private final MongoTemplate mongoTemplate;

  public InstancesConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public MongoCollection<InstancesCollection> getCollection() {
    return mongoTemplate.getMongoDatabaseFactory().getMongoDatabase()
        .getCollection(InstancesCollection.DOCUMENT_NAME, InstancesCollection.class);
  }

  @Override
  public List<InstancesConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<InstancesConfiguration> listBy(String appId) {
    Bson filter = Filters.eq(DynamicClassCollection.Fields.appId, appId);
    List<InstancesConfiguration> dtos = new ArrayList<>();
    try (MongoCursor<InstancesCollection> cursor = getCollection().find(filter).iterator()) {
      while (cursor.hasNext()) {
        InstancesCollection document = cursor.next();
        InstancesConfiguration dto = InstancesMapper.INSTANCE.dtoFromDao(document);
        dtos.add(dto);
      }
    }
    return dtos;
  }

  public List<InstancesConfiguration> listByAppOrdered(String appId) {
    Bson filter = Filters.eq(InstancesCollection.Fields.appId, appId);
    Bson sort = Sorts.ascending(DASH_ID);
    List<InstancesConfiguration> instancesDTOList = new ArrayList<>();
    try (MongoCursor<InstancesCollection> cursor = getCollection().find(filter).sort(sort).iterator()) {
      while (cursor.hasNext()) {
        InstancesCollection document = cursor.next();
        InstancesConfiguration dto = InstancesMapper.INSTANCE.dtoFromDao(document);
        instancesDTOList.add(dto);
      }
    }
    return instancesDTOList;
  }

  @Override
  public boolean update(InstancesConfiguration configuration) {
    Bson filter = Filters.and(
        Filters.eq(InstancesCollection.Fields.appId, configuration.getAppId()),
        Filters.eq(InstancesCollection.Fields.host, configuration.getHost()));

    List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
        MongoHelper.getFullProperties(configuration),
        Updates.set(InstancesCollection.Fields.dataUpdateTime, new Date()));
    Bson updateCombine = Updates.combine(updateList);

    FindOneAndUpdateOptions options =
        new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
    InstancesCollection result = (InstancesCollection) getCollection().findOneAndUpdate(filter, updateCombine, options);
    return result != null;
  }

  @Override
  public boolean remove(InstancesConfiguration configuration) {
    Bson filter = Filters.eq(DASH_ID, new ObjectId(configuration.getId()));
    DeleteResult deleteResult = getCollection().deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }

  @Override
  public boolean insert(InstancesConfiguration configuration) {
    configuration.setDataUpdateTime(new Date());
    InstancesCollection instancesCollection = InstancesMapper.INSTANCE.daoFromDto(configuration);
    InsertOneResult insertOneResult = getCollection().insertOne(instancesCollection);
    if (insertOneResult.getInsertedId() != null) {
      configuration.setId(instancesCollection.getId());
    }
    return insertOneResult.getInsertedId() != null;

  }

  @Override
  public boolean removeByAppId(String appId) {

    Bson filter = Filters.eq(InstancesCollection.Fields.appId, appId);
    DeleteResult deleteResult = getCollection().deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }

  public boolean removeByAppIdAndHost(String appId, String host) {
    Bson filterCombine = Filters.and(Filters.eq(InstancesCollection.Fields.appId, appId),
        Filters.eq(InstancesCollection.Fields.host, host));
    DeleteResult remove = getCollection().deleteMany(filterCombine);
    return remove.getDeletedCount() > 0;
  }

}
