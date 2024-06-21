package com.arextest.config.repository.impl;

import com.arextest.config.mapper.DynamicClassMapper;
import com.arextest.config.model.dao.config.DynamicClassCollection;
import com.arextest.config.model.dto.record.DynamicClassConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@RequiredArgsConstructor
public class DynamicClassConfigurationRepositoryImpl implements
    ConfigRepositoryProvider<DynamicClassConfiguration> {

  private final MongoTemplate mongoTemplate;

  @Override
  public List<DynamicClassConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<DynamicClassConfiguration> listBy(String appId) {
    Query filter = new Query(Criteria.where(DynamicClassCollection.Fields.appId).is(appId));
    return mongoTemplate.find(filter, DynamicClassCollection.class).stream()
        .map(DynamicClassMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
  }

  @Override
  public boolean update(DynamicClassConfiguration configuration) {
    Query filter = new Query(Criteria.where(DASH_ID).is(new ObjectId(configuration.getId())));

    Update update = MongoHelper.getMongoTemplateUpdates(configuration,
        DynamicClassCollection.Fields.fullClassName, DynamicClassCollection.Fields.methodName,
        DynamicClassCollection.Fields.parameterTypes, DynamicClassCollection.Fields.keyFormula);
    MongoHelper.withMongoTemplateBaseUpdate(update);
    return mongoTemplate.findAndModify(filter, update, new FindAndModifyOptions().upsert(true),
        DynamicClassCollection.class) != null;
  }

  @Override
  public boolean remove(DynamicClassConfiguration configuration) {
    Query filter = new Query(Criteria.where(DASH_ID).is(new ObjectId(configuration.getId())));
    return mongoTemplate.remove(filter, DynamicClassCollection.class).getDeletedCount() > 0;
  }

  @Override
  public boolean insert(DynamicClassConfiguration configuration) {
    DynamicClassCollection dynamicClassCollection = DynamicClassMapper.INSTANCE.daoFromDto(
        configuration);
    mongoTemplate.insert(dynamicClassCollection);
    if (dynamicClassCollection.getId() != null) {
      configuration.setId(dynamicClassCollection.getId());
    }
    return dynamicClassCollection.getId() != null;
  }

  @Override
  public boolean removeByAppId(String appId) {
    Query filter = new Query(Criteria.where(DynamicClassCollection.Fields.appId).is(appId));
    return mongoTemplate.remove(filter, DynamicClassCollection.class).getDeletedCount() > 0;
  }

  public boolean cover(String appId, List<DynamicClassConfiguration> configuration) {
    Query filter = new Query(Criteria.where(DynamicClassCollection.Fields.appId).is(appId));
    List<DynamicClassCollection> allAndRemove = mongoTemplate.findAllAndRemove(filter,
        DynamicClassCollection.class);
    try {
      List<DynamicClassCollection> daos = configuration.stream()
          .map(DynamicClassMapper.INSTANCE::daoFromDto).collect(Collectors.toList());
      mongoTemplate.insertAll(daos);
    } catch (RuntimeException e) {
      LOGGER.error("cover error, before cover: {}, after cover: {}", allAndRemove, configuration);
      return false;
    }
    return true;
  }

}
