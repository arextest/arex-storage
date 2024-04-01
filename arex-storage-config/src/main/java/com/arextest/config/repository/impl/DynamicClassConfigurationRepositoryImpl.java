package com.arextest.config.repository.impl;

import com.arextest.config.mapper.DynamicClassMapper;
import com.arextest.config.model.dao.config.DynamicClassCollection;
import com.arextest.config.model.dto.record.DynamicClassConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

public class DynamicClassConfigurationRepositoryImpl implements
    ConfigRepositoryProvider<DynamicClassConfiguration> {
  private final MongoTemplate mongoTemplate;

  public DynamicClassConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public MongoCollection<DynamicClassCollection> getCollection() {
    return mongoTemplate.getMongoDatabaseFactory().getMongoDatabase()
        .getCollection(DynamicClassCollection.DOCUMENT_NAME, DynamicClassCollection.class);
  }

  @Override
  public List<DynamicClassConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<DynamicClassConfiguration> listBy(String appId) {
    Bson filter = Filters.eq(DynamicClassCollection.Fields.appId, appId);
    List<DynamicClassConfiguration> dtos = new ArrayList<>();
    try (MongoCursor<DynamicClassCollection> cursor = getCollection().find(filter).iterator()) {
      while (cursor.hasNext()) {
        DynamicClassCollection document = cursor.next();
        DynamicClassConfiguration dto = DynamicClassMapper.INSTANCE.dtoFromDao(document);
        dtos.add(dto);
      }
    }
    return dtos;
  }

  @Override
  public boolean update(DynamicClassConfiguration configuration) {
    Bson filter = Filters.eq(DASH_ID, new ObjectId(configuration.getId()));

    List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
        MongoHelper.getSpecifiedProperties(configuration,
            DynamicClassCollection.Fields.fullClassName,
            DynamicClassCollection.Fields.methodName,
            DynamicClassCollection.Fields.parameterTypes,
            DynamicClassCollection.Fields.keyFormula));
    Bson updateCombine = Updates.combine(updateList);

    UpdateResult updateResult = getCollection().updateMany(filter, updateCombine);
    return updateResult.getModifiedCount() > 0;
  }

  @Override
  public boolean remove(DynamicClassConfiguration configuration) {
    Bson filter = Filters.eq(DASH_ID, new ObjectId(configuration.getId()));
    DeleteResult deleteResult = getCollection().deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }

  @Override
  public boolean insert(DynamicClassConfiguration configuration) {
    DynamicClassCollection dynamicClassCollection = DynamicClassMapper.INSTANCE.daoFromDto(
        configuration);
    InsertOneResult insertOneResult = getCollection().insertOne(dynamicClassCollection);
    if (insertOneResult.getInsertedId() != null) {
      configuration.setId(dynamicClassCollection.getId());
    }
    return insertOneResult.getInsertedId() != null;
  }

  @Override
  public boolean removeByAppId(String appId) {
    Bson filter = Filters.eq(DynamicClassCollection.Fields.appId, appId);
    DeleteResult deleteResult = getCollection().deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }
}
