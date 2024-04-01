package com.arextest.config.repository.impl;

import com.arextest.config.mapper.ServiceMapper;
import com.arextest.config.model.dao.config.ServiceCollection;
import com.arextest.config.model.dto.application.ApplicationServiceConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

public class ApplicationServiceConfigurationRepositoryImpl
    implements ConfigRepositoryProvider<ApplicationServiceConfiguration> {
  private final MongoTemplate mongoTemplate;

  @Resource
  private ApplicationOperationConfigurationRepositoryImpl applicationOperationConfigurationRepository;

  public ApplicationServiceConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public MongoCollection<ServiceCollection> getCollection() {
    return mongoTemplate.getMongoDatabaseFactory().getMongoDatabase()
        .getCollection(ServiceCollection.DOCUMENT_NAME, ServiceCollection.class);
  }

  @Override
  public List<ApplicationServiceConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ApplicationServiceConfiguration> listBy(String appId) {

    Bson filter = Filters.eq(ServiceCollection.Fields.appId, appId);
    List<ApplicationServiceConfiguration> dtos = new ArrayList<>();
    try (MongoCursor<ServiceCollection> cursor = getCollection().find(filter).iterator()) {
      while (cursor.hasNext()) {
        ServiceCollection document = cursor.next();
        ApplicationServiceConfiguration dto = ServiceMapper.INSTANCE.dtoFromDao(document);
        dto.setOperationList(
            applicationOperationConfigurationRepository.operationBaseInfoList(dto.getId()));
        dtos.add(dto);
      }
    }
    return dtos;
  }

  @Override
  public boolean update(ApplicationServiceConfiguration configuration) {
    Bson filter = Filters.eq(DASH_ID, new ObjectId(configuration.getId()));

    List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
        Updates.set(ServiceCollection.Fields.status, configuration.getStatus()));
    Bson updateCombine = Updates.combine(updateList);

    return getCollection().updateMany(filter, updateCombine).getModifiedCount() > 0;
  }

  @Override
  public boolean remove(ApplicationServiceConfiguration configuration) {
    Bson filter = Filters.eq(DASH_ID, new ObjectId(configuration.getId()));
    DeleteResult deleteResult = getCollection().deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ApplicationServiceConfiguration configuration) {
    ServiceCollection serviceCollection = ServiceMapper.INSTANCE.daoFromDto(configuration);
    InsertOneResult insertOneResult = getCollection().insertOne(serviceCollection);
    if (insertOneResult.getInsertedId() != null) {
      configuration.setId(serviceCollection.getId());
    }
    return insertOneResult.getInsertedId() != null;
  }

  @Override
  public long count(String appId) {
    Bson filter = Filters.eq(ServiceCollection.Fields.appId, appId);
    return getCollection().countDocuments(filter);
  }

  @Override
  public boolean removeByAppId(String appId) {

    Bson filter = Filters.eq(ServiceCollection.Fields.appId, appId);
    DeleteResult deleteResult = getCollection().deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }
}
