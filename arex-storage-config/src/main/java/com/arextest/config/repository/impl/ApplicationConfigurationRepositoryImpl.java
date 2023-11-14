package com.arextest.config.repository.impl;

import com.arextest.config.mapper.AppMapper;
import com.arextest.config.model.dao.config.AppCollection;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public class ApplicationConfigurationRepositoryImpl implements
    ConfigRepositoryProvider<ApplicationConfiguration> {

  private static final String UNKNOWN_APP_NAME = "unknown app name";
  private MongoDatabase mongoDatabase;
  private MongoCollection<AppCollection> mongoCollection;
  @Resource
  private List<ConfigRepositoryProvider> configRepositoryProviders;

  public ApplicationConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
    this.mongoDatabase = mongoDatabase;
  }

  @PostConstruct
  private void init() {
    this.mongoCollection = mongoDatabase.getCollection(AppCollection.DOCUMENT_NAME,
        AppCollection.class);
    // flush appName
    flushAppName();
  }

  private void flushAppName() {
    Bson filter = Filters.in(AppCollection.Fields.appName, UNKNOWN_APP_NAME, "", null);
    List<WriteModel<AppCollection>> bulkUpdateOps = new ArrayList<>();
    try (MongoCursor<AppCollection> cursor = mongoCollection.find(filter).iterator()) {
      while (cursor.hasNext()) {
        AppCollection document = cursor.next();
        document.setAppName(document.getAppId());

        Bson filter2 = Filters.eq(DASH_ID, new ObjectId(document.getId()));
        Bson update = Updates.combine(Arrays.asList(MongoHelper.getUpdate(),
            MongoHelper.getSpecifiedProperties(document, AppCollection.Fields.appName)));
        bulkUpdateOps.add(new UpdateManyModel<>(filter2, update));
      }
    }
    if (bulkUpdateOps.size() > 0) {
      BulkWriteResult result = mongoCollection.bulkWrite(bulkUpdateOps);
    }
  }

  @Override
  public List<ApplicationConfiguration> list() {

    Bson sort = Sorts.descending(DASH_ID);
    List<ApplicationConfiguration> applicationConfigurations = new ArrayList<>();
    try (MongoCursor<AppCollection> cursor = mongoCollection.find().sort(sort).iterator()) {
      while (cursor.hasNext()) {
        AppCollection document = cursor.next();
        ApplicationConfiguration dto = AppMapper.INSTANCE.dtoFromDao(document);
        applicationConfigurations.add(dto);
      }
    }
    return applicationConfigurations;
  }

  @Override
  public List<ApplicationConfiguration> listBy(String appId) {

    Bson filter = Filters.eq(AppCollection.Fields.appId, appId);
    List<ApplicationConfiguration> applicationConfigurations = new ArrayList<>();
    try (MongoCursor<AppCollection> cursor = mongoCollection.find(filter).iterator()) {
      while (cursor.hasNext()) {
        AppCollection document = cursor.next();
        ApplicationConfiguration dto = AppMapper.INSTANCE.dtoFromDao(document);
        applicationConfigurations.add(dto);
      }
    }
    return applicationConfigurations;
  }

  @Override
  public boolean update(ApplicationConfiguration configuration) {

    Bson filter = Filters.eq(AppCollection.Fields.appId, configuration.getAppId());

    List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
        MongoHelper.getSpecifiedProperties(configuration,
            AppCollection.Fields.agentVersion,
            AppCollection.Fields.agentExtVersion,
            AppCollection.Fields.status,
            AppCollection.Fields.features,
            AppCollection.Fields.appName,
            AppCollection.Fields.owners,
            AppCollection.Fields.visibilityLevel,
            AppCollection.Fields.tagEnvs));
    Bson updateCombine = Updates.combine(updateList);

    return mongoCollection.updateMany(filter, updateCombine).getModifiedCount() > 0;
  }

  @Override
  public boolean remove(ApplicationConfiguration configuration) {
    if (StringUtils.isBlank(configuration.getAppId())) {
      return false;
    }
    for (ConfigRepositoryProvider configRepositoryProvider : configRepositoryProviders) {
      configRepositoryProvider.removeByAppId(configuration.getAppId());
    }
    Bson filter = Filters.eq(AppCollection.Fields.appId, configuration.getAppId());
    DeleteResult deleteResult = mongoCollection.deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ApplicationConfiguration configuration) {
    AppCollection appCollection = AppMapper.INSTANCE.daoFromDto(configuration);
    InsertOneResult insertOneResult = mongoCollection.insertOne(appCollection);
    return insertOneResult.getInsertedId() != null;
  }

  public boolean addEnvToApp(String appId, String env) {
    Bson filter = Filters.eq(AppCollection.Fields.appId, appId);
    Bson update = Updates.addToSet(AppCollection.Fields.tagEnvs, env);
    return mongoCollection.updateMany(filter, update).getModifiedCount() > 0;
  }

}
