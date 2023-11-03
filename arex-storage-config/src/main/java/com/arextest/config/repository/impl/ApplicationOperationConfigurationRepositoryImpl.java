package com.arextest.config.repository.impl;

import com.arextest.config.mapper.ServiceOperationMapper;
import com.arextest.config.model.dao.config.ServiceOperationCollection;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public class ApplicationOperationConfigurationRepositoryImpl
    implements ConfigRepositoryProvider<ApplicationOperationConfiguration> {

  private MongoDatabase mongoDatabase;

  private MongoCollection<ServiceOperationCollection> mongoCollection;

  public ApplicationOperationConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
    this.mongoDatabase = mongoDatabase;
  }

  @PostConstruct
  private void init() {
    this.mongoCollection =
        mongoDatabase.getCollection(ServiceOperationCollection.DOCUMENT_NAME,
            ServiceOperationCollection.class);
  }

  @Override
  public List<ApplicationOperationConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ApplicationOperationConfiguration> listBy(String appId) {
    Bson filter = Filters.eq(ServiceOperationCollection.Fields.appId, appId);
    List<ApplicationOperationConfiguration> dtos = new ArrayList<>();
    try (MongoCursor<ServiceOperationCollection> cursor = mongoCollection.find(filter).iterator()) {
      while (cursor.hasNext()) {
        ServiceOperationCollection document = cursor.next();
        ApplicationOperationConfiguration dto = ServiceOperationMapper.INSTANCE.dtoFromDao(
            document);
        dtos.add(dto);
      }
    }
    return dtos;
  }

  @Override
  public boolean update(ApplicationOperationConfiguration configuration) {
    Bson filter = Filters.eq(DASH_ID, new ObjectId(configuration.getId()));
    List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
        MongoHelper.getSpecifiedProperties(configuration,
            ServiceOperationCollection.Fields.status));
    Bson updateCombine = Updates.combine(updateList);

    return mongoCollection.updateMany(filter, updateCombine).getModifiedCount() > 0;
  }

  @Override
  public boolean remove(ApplicationOperationConfiguration configuration) {
    Bson filter = Filters.eq(DASH_ID, new ObjectId(configuration.getId()));
    return mongoCollection.deleteMany(filter).getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ApplicationOperationConfiguration configuration) {

    ServiceOperationCollection serviceOperationCollection =
        ServiceOperationMapper.INSTANCE.daoFromDto(configuration);
    InsertOneResult insertOneResult = mongoCollection.insertOne(serviceOperationCollection);
    if (insertOneResult.getInsertedId() != null) {
      configuration.setId(serviceOperationCollection.getId());
    }
    return insertOneResult.getInsertedId() != null;

  }

  public ApplicationOperationConfiguration listByOperationId(String operationId) {

    Bson filter = Filters.eq(DASH_ID, new ObjectId(operationId));
    ServiceOperationCollection serviceOperationCollection = mongoCollection.find(filter).first();
    return serviceOperationCollection == null ? null
        : ServiceOperationMapper.INSTANCE.dtoFromDao(serviceOperationCollection);
  }

  // the search of operation's based—info by serviceId
  public List<ApplicationOperationConfiguration> operationBaseInfoList(String serviceId) {

    Bson filter = Filters.eq(ServiceOperationCollection.Fields.serviceId, serviceId);
    List<ApplicationOperationConfiguration> dtos = new ArrayList<>();
    try (MongoCursor<ServiceOperationCollection> cursor = mongoCollection.find(filter).iterator()) {
      while (cursor.hasNext()) {
        ServiceOperationCollection document = cursor.next();
        ApplicationOperationConfiguration dto = ServiceOperationMapper.INSTANCE.baseInfoFromDao(
            document);
        dtos.add(dto);
      }
    }
    return dtos;
  }

  @Override
  public boolean removeByAppId(String appId) {

    Bson filter = Filters.eq(ServiceOperationCollection.Fields.appId, appId);
    DeleteResult deleteResult = mongoCollection.deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;

  }

  public boolean findAndUpdate(ApplicationOperationConfiguration configuration) {

    Bson query = Filters.and(
        Filters.eq(ServiceOperationCollection.Fields.serviceId, configuration.getServiceId()),
        Filters.eq(ServiceOperationCollection.Fields.operationName,
            configuration.getOperationName()),
        Filters.eq(ServiceOperationCollection.Fields.appId, configuration.getAppId()));

    List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
        MongoHelper.getSpecifiedProperties(configuration,
            ServiceOperationCollection.Fields.operationType,
            ServiceOperationCollection.Fields.status),
        Updates.addEachToSet(ServiceOperationCollection.Fields.operationTypes,
            new ArrayList<>(configuration.getOperationTypes())));
    Bson updateCombine = Updates.combine(updateList);

    ServiceOperationCollection dao = mongoCollection.findOneAndUpdate(query, updateCombine,
        new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));

    return dao != null && StringUtils.isNotEmpty(dao.getId());
  }

  public List<ApplicationOperationConfiguration> queryByMultiCondition(
      Map<String, Object> conditions) {
    if (conditions == null || conditions.isEmpty()) {
      return Collections.emptyList();
    }

    List<ApplicationOperationConfiguration> dtos = new ArrayList<>();
    List<Bson> filters = new ArrayList<>();
    for (Map.Entry<String, Object> condition : conditions.entrySet()) {
      if (condition != null && condition.getKey() != null) {
        filters.add(Filters.eq(condition.getKey(), condition.getValue()));
      }
    }
    try (MongoCursor<ServiceOperationCollection> cursor = mongoCollection.find(Filters.and(filters))
        .iterator()) {
      while (cursor.hasNext()) {
        ServiceOperationCollection document = cursor.next();
        ApplicationOperationConfiguration dto = ServiceOperationMapper.INSTANCE.baseInfoFromDao(
            document);
        dtos.add(dto);
      }
    }
    return dtos;
  }
}
