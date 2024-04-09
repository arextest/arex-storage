package com.arextest.config.repository.impl;

import com.arextest.config.mapper.ServiceOperationMapper;
import com.arextest.config.model.dao.config.ServiceOperationCollection;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@RequiredArgsConstructor
public class ApplicationOperationConfigurationRepositoryImpl
    implements ConfigRepositoryProvider<ApplicationOperationConfiguration> {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<ApplicationOperationConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ApplicationOperationConfiguration> listBy(String appId) {
    Query query = new Query();
    query.addCriteria(Criteria.where(ServiceOperationCollection.Fields.appId).is(appId));
    return mongoTemplate.find(query, ServiceOperationCollection.class)
        .stream().map(ServiceOperationMapper.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ApplicationOperationConfiguration configuration) {
    Query query = new Query();
    query.addCriteria(Criteria.where(DASH_ID).is(new ObjectId(configuration.getId())));
    Update update = MongoHelper.getMongoTemplateUpdates(configuration,
        ServiceOperationCollection.Fields.status);
    MongoHelper.withMongoTemplateBaseUpdate(update);
    return mongoTemplate.updateMulti(query, update, ServiceOperationCollection.class).getModifiedCount() > 0;
  }

  @Override
  public boolean remove(ApplicationOperationConfiguration configuration) {
    Query filter = new Query(Criteria.where(DASH_ID).is(new ObjectId(configuration.getId())));
    return mongoTemplate.remove(filter, ServiceOperationCollection.class).getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ApplicationOperationConfiguration configuration) {
    ServiceOperationCollection inserted =
        mongoTemplate.insert(ServiceOperationMapper.INSTANCE.daoFromDto(configuration));
    if (inserted.getId() != null) {
      configuration.setId(inserted.getId());
    }
    return inserted.getId() != null;
  }

  public ApplicationOperationConfiguration listByOperationId(String operationId) {
    Query query = new Query(Criteria.where(DASH_ID).is(new ObjectId(operationId)));
    return Optional.ofNullable(mongoTemplate.findOne(query, ServiceOperationCollection.class))
        .map(ServiceOperationMapper.INSTANCE::dtoFromDao)
        .orElse(null);
  }

  // the search of operation's based—info by serviceId
  public List<ApplicationOperationConfiguration> operationBaseInfoList(String serviceId) {
    Query filter = new Query(Criteria.where(ServiceOperationCollection.Fields.serviceId).is(serviceId));
    return mongoTemplate.find(filter, ServiceOperationCollection.class)
        .stream().map(ServiceOperationMapper.INSTANCE::baseInfoFromDao)
        .collect(Collectors.toList());
  }

  @Override
  public boolean removeByAppId(String appId) {
    Query filter = new Query(Criteria.where(ServiceOperationCollection.Fields.appId).is(appId));
    return mongoTemplate.remove(filter, ServiceOperationCollection.class).getDeletedCount() > 0;
  }

  public boolean findAndUpdate(ApplicationOperationConfiguration configuration) {
    Query query = new Query(
        Criteria.where(ServiceOperationCollection.Fields.serviceId).is(configuration.getServiceId())
            .and(ServiceOperationCollection.Fields.operationName).is(configuration.getOperationName())
            .and(ServiceOperationCollection.Fields.appId).is(configuration.getAppId())
    );
    Update update = MongoHelper.getMongoTemplateUpdates(configuration,
        ServiceOperationCollection.Fields.operationType,
        ServiceOperationCollection.Fields.status);
    update.addToSet(ServiceOperationCollection.Fields.operationTypes,
        new ArrayList<>(configuration.getOperationTypes()));

    MongoHelper.withMongoTemplateBaseUpdate(update);

    mongoTemplate.findAndModify(query, update,
        new FindAndModifyOptions().upsert(true).returnNew(true),
        ServiceOperationCollection.class);

    return true;
  }

  public List<ApplicationOperationConfiguration> queryByMultiCondition(
      Map<String, Object> conditions) {
    if (conditions == null || conditions.isEmpty()) {
      return Collections.emptyList();
    }

    Query filters = new Query();
    int filterCount = 0;
    for (Map.Entry<String, Object> condition : conditions.entrySet()) {
      if (condition != null && condition.getKey() != null) {
        filters.addCriteria(Criteria.where(condition.getKey()).is(condition.getValue()));
        filterCount++;
      }
    }

    if (filterCount == 0) {
      return Collections.emptyList();
    }
    return mongoTemplate.find(filters, ServiceOperationCollection.class).stream()
        .map(ServiceOperationMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
  }
}
