package com.arextest.config.repository.impl;

import com.arextest.config.mapper.InstancesMapper;
import com.arextest.config.model.dao.config.DynamicClassCollection;
import com.arextest.config.model.dao.config.InstancesCollection;
import com.arextest.config.model.dao.config.ServiceCollection;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@RequiredArgsConstructor
public class InstancesConfigurationRepositoryImpl implements
    ConfigRepositoryProvider<InstancesConfiguration> {

  private final MongoTemplate mongoTemplate;

  @Override
  public List<InstancesConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<InstancesConfiguration> listBy(String appId) {
    Query filter = new Query(Criteria.where(InstancesCollection.Fields.appId).is(appId));
    return mongoTemplate.find(filter, InstancesCollection.class)
        .stream().map(InstancesMapper.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  public List<InstancesConfiguration> listByAppOrdered(String appId) {
    Query filter = new Query(Criteria.where(InstancesCollection.Fields.appId).is(appId));
    filter.with(Sort.by(Sort.Direction.DESC, DASH_ID));
    return mongoTemplate.find(filter, InstancesCollection.class)
        .stream().map(InstancesMapper.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(InstancesConfiguration configuration) {
    Query filter = new Query(Criteria
        .where(InstancesCollection.Fields.appId).is(configuration.getAppId())
        .and(InstancesCollection.Fields.host).is(configuration.getHost()));
    Update update = MongoHelper.getMongoTemplateUpdates(configuration);
    MongoHelper.withMongoTemplateBaseUpdate(update);
    update.set(InstancesCollection.Fields.dataUpdateTime, new Date());
    return mongoTemplate.updateFirst(filter, update, InstancesCollection.class)
        .getModifiedCount() > 0;
  }

  @Override
  public boolean remove(InstancesConfiguration configuration) {
    return mongoTemplate.remove(configuration).getDeletedCount() > 0;
  }

  @Override
  public boolean insert(InstancesConfiguration configuration) {
    configuration.setDataUpdateTime(new Date());
    InstancesCollection instancesCollection = InstancesMapper.INSTANCE.daoFromDto(configuration);
    mongoTemplate.insert(instancesCollection);
    if (configuration.getId() != null) {
      configuration.setId(instancesCollection.getId());
    }
    return configuration.getId() != null;
  }

  @Override
  public boolean removeByAppId(String appId) {
    Query filter = new Query(Criteria.where(InstancesCollection.Fields.appId).is(appId));
    return mongoTemplate.remove(filter, InstancesCollection.class).getDeletedCount() > 0;
  }

  public boolean removeByAppIdAndHost(String appId, String host) {
    Query filter = new Query(Criteria.where(InstancesCollection.Fields.appId).is(appId)
        .and(InstancesCollection.Fields.host).is(host));
    return mongoTemplate.remove(filter, InstancesCollection.class).getDeletedCount() > 0;
  }
}
