package com.arextest.config.repository.impl;

import com.arextest.config.mapper.AppMapper;
import com.arextest.config.model.dao.config.AppCollection;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.result.DeleteResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class ApplicationConfigurationRepositoryImpl implements
    ConfigRepositoryProvider<ApplicationConfiguration> {

  private static final String DOT_OP = ".";
  private final MongoTemplate mongoTemplate;

  @Resource
  private List<ConfigRepositoryProvider<?>> configRepositoryProviders;

  public ApplicationConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public List<ApplicationConfiguration> list() {
    Query query = new Query().with(Sort.by(Direction.DESC, DASH_ID));
    return mongoTemplate.find(query, AppCollection.class)
        .stream()
        .map(AppMapper.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  @Override
  public List<ApplicationConfiguration> listBy(String appId) {
    Query query = new Query();
    query.addCriteria(Criteria.where(AppCollection.Fields.appId).is(appId));
    List<AppCollection> appCollections = mongoTemplate.find(query, AppCollection.class);
    return appCollections.stream()
        .map(AppMapper.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ApplicationConfiguration configuration) {
    Query filter = new Query(Criteria.where(AppCollection.Fields.appId)
        .is(configuration.getAppId()));

    Update update = MongoHelper.getMongoTemplateUpdates(configuration,
        AppCollection.Fields.agentVersion,
        AppCollection.Fields.agentExtVersion,
        AppCollection.Fields.status,
        AppCollection.Fields.features,
        AppCollection.Fields.appName,
        AppCollection.Fields.owners,
        AppCollection.Fields.visibilityLevel,
        AppCollection.Fields.tags);
    MongoHelper.withMongoTemplateBaseUpdate(update);
    return mongoTemplate.updateMulti(filter, update, AppCollection.class).getModifiedCount() > 0;
  }

  @Override
  public boolean remove(ApplicationConfiguration configuration) {
    if (StringUtils.isBlank(configuration.getAppId())) {
      return false;
    }
    return this.removeByAppId(configuration.getAppId());
  }

  @Override
  public boolean removeByAppId(String appId) {
    for (ConfigRepositoryProvider<?> configRepositoryProvider : configRepositoryProviders) {
      configRepositoryProvider.removeByAppId(appId);
    }
    Query filter = new Query(Criteria.where(AppCollection.Fields.appId).is(appId));
    DeleteResult deleteResult = mongoTemplate.remove(filter, AppCollection.class);
    return deleteResult.getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ApplicationConfiguration configuration) {
    mongoTemplate.insert(AppMapper.INSTANCE.daoFromDto(configuration));
    return true;
  }

  public boolean addEnvToApp(String appId, Map<String, String> tags) {
    if (StringUtils.isBlank(appId) || tags == null || tags.isEmpty()) {
      return false;
    }
    Query query = new Query();
    query.addCriteria(Criteria.where(AppCollection.Fields.appId).is(appId));
    Update update = new Update();
    for (Map.Entry<String, String> entry : tags.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (StringUtils.isBlank(value)) {
        continue;
      }
      update.addToSet(AppCollection.Fields.tags + DOT_OP + key, value);
    }
    if (update.getUpdateObject().isEmpty()) {
      return false;
    }
    return mongoTemplate.updateFirst(query, update, AppCollection.class).getModifiedCount() > 0;
  }

}
