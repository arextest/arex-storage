package com.arextest.config.repository.impl;

import com.arextest.config.mapper.AppMapper;
import com.arextest.config.model.dao.config.AppCollection;
import com.arextest.config.model.dao.config.AppCollection.Fields;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class ApplicationConfigurationRepositoryImpl implements
    ConfigRepositoryProvider<ApplicationConfiguration> {

  private static final String UNKNOWN_APP_NAME = "unknown app name";
  private static final String DOT_OP = ".";
  private final MongoTemplate mongoTemplate;

  @Resource
  private List<ConfigRepositoryProvider<?>> configRepositoryProviders;

  public ApplicationConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public MongoCollection<AppCollection> getCollection() {
    return mongoTemplate.getMongoDatabaseFactory().getMongoDatabase()
        .getCollection(AppCollection.DOCUMENT_NAME, AppCollection.class);
  }

  @PostConstruct
  private void init() {
    // flush appName
    flushAppName();
  }

  private void flushAppName() {
    Bson filter = Filters.in(AppCollection.Fields.appName, UNKNOWN_APP_NAME, "", null);
    List<WriteModel<AppCollection>> bulkUpdateOps = new ArrayList<>();
    try (MongoCursor<AppCollection> cursor = getCollection().find(filter).iterator()) {
      while (cursor.hasNext()) {
        AppCollection document = cursor.next();
        document.setAppName(document.getAppId());

        Bson filter2 = Filters.eq(DASH_ID, new ObjectId(document.getId()));
        Bson update = Updates.combine(Arrays.asList(MongoHelper.getUpdate(),
            MongoHelper.getSpecifiedProperties(document, AppCollection.Fields.appName)));
        bulkUpdateOps.add(new UpdateManyModel<>(filter2, update));
      }
    }
    if (!bulkUpdateOps.isEmpty()) {
      getCollection().bulkWrite(bulkUpdateOps);
    }
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
        Fields.agentVersion,
        Fields.agentExtVersion,
        Fields.status,
        Fields.features,
        Fields.appName,
        Fields.owners,
        Fields.visibilityLevel,
        Fields.tags);
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
