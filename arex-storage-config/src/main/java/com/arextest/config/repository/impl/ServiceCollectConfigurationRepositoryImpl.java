package com.arextest.config.repository.impl;

import com.arextest.config.mapper.RecordServiceConfigMapper;
import com.arextest.config.model.dao.BaseEntity;
import com.arextest.config.model.dao.MultiEnvBaseEntity;
import com.arextest.config.model.dao.config.RecordServiceConfigCollection;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.repository.MultiEnvConfigRepositoryProvider;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;

public class ServiceCollectConfigurationRepositoryImpl
    implements MultiEnvConfigRepositoryProvider<ServiceCollectConfiguration> {

  private final MongoTemplate mongoTemplate;

  public ServiceCollectConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public MongoCollection<RecordServiceConfigCollection> getCollection() {
    return mongoTemplate.getMongoDatabaseFactory().getMongoDatabase()
        .getCollection(RecordServiceConfigCollection.DOCUMENT_NAME, RecordServiceConfigCollection.class);
  }

  @Override
  public List<ServiceCollectConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ServiceCollectConfiguration> listBy(String appId) {

    Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, appId);
    List<ServiceCollectConfiguration> recordServiceConfigs = new ArrayList<>();
    try (MongoCursor<RecordServiceConfigCollection> cursor = getCollection().find(filter)
        .iterator()) {
      while (cursor.hasNext()) {
        RecordServiceConfigCollection document = cursor.next();
        ServiceCollectConfiguration dto = RecordServiceConfigMapper.INSTANCE.dtoFromDao(document);
        recordServiceConfigs.add(dto);
      }
    }
    return recordServiceConfigs;
  }

  @Override
  public boolean update(ServiceCollectConfiguration configuration) {

    Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, configuration.getAppId());

    List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
        MongoHelper.getSpecifiedProperties(configuration,
            RecordServiceConfigCollection.Fields.sampleRate,
            RecordServiceConfigCollection.Fields.allowDayOfWeeks,
            RecordServiceConfigCollection.Fields.allowTimeOfDayFrom,
            RecordServiceConfigCollection.Fields.allowTimeOfDayTo,
            RecordServiceConfigCollection.Fields.excludeServiceOperationSet,
            RecordServiceConfigCollection.Fields.timeMock,
            RecordServiceConfigCollection.Fields.extendField,
            RecordServiceConfigCollection.Fields.serializeSkipInfoList
        ),
        Updates.set(RecordServiceConfigCollection.Fields.recordMachineCountLimit,
            configuration.getRecordMachineCountLimit() == null ? 1
                : configuration.getRecordMachineCountLimit())

    );
    Bson updateCombine = Updates.combine(updateList);

    UpdateResult updateResult = getCollection().updateMany(filter, updateCombine);
    return updateResult.getModifiedCount() > 0;

  }

  @Override
  public boolean remove(ServiceCollectConfiguration configuration) {
    Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, configuration.getAppId());
    DeleteResult deleteResult = getCollection().deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ServiceCollectConfiguration configuration) {
    RecordServiceConfigCollection recordServiceConfigCollection =
        RecordServiceConfigMapper.INSTANCE.daoFromDto(configuration);
    InsertOneResult insertOneResult = getCollection().insertOne(recordServiceConfigCollection);
    return insertOneResult.getInsertedId() != null;
  }

  @Override
  public boolean removeByAppId(String appId) {
    Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, appId);
    DeleteResult deleteResult = getCollection().deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }

  @Override
  public boolean updateMultiEnvConfig(ServiceCollectConfiguration configuration) {
    Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, configuration.getAppId());

    List<RecordServiceConfigCollection> configs = Optional.ofNullable(configuration.getMultiEnvConfigs())
        .orElse(Collections.emptyList())
        .stream().map(RecordServiceConfigMapper.INSTANCE::daoFromDto)
        .collect(Collectors.toList());

    Bson update = Updates.combine(
        Updates.set(MultiEnvBaseEntity.Fields.multiEnvConfigs, configs),
        Updates.set(BaseEntity.Fields.dataChangeUpdateTime, System.currentTimeMillis())
    );
    return getCollection().updateOne(filter, update).getModifiedCount() > 0;
  }
}
