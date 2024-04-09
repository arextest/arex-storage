package com.arextest.config.repository.impl;

import com.arextest.config.mapper.RecordServiceConfigMapper;
import com.arextest.config.model.dao.BaseEntity;
import com.arextest.config.model.dao.MultiEnvBaseEntity;
import com.arextest.config.model.dao.config.RecordServiceConfigCollection;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.repository.MultiEnvConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@RequiredArgsConstructor
public class ServiceCollectConfigurationRepositoryImpl
    implements MultiEnvConfigRepositoryProvider<ServiceCollectConfiguration> {

  private final MongoTemplate mongoTemplate;

  @Override
  public List<ServiceCollectConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ServiceCollectConfiguration> listBy(String appId) {
    Query filter = new Query(Criteria.where(RecordServiceConfigCollection.Fields.appId).is(appId));

    return mongoTemplate.find(filter, RecordServiceConfigCollection.class)
        .stream()
        .map(RecordServiceConfigMapper.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ServiceCollectConfiguration configuration) {
    Query filter = new Query(Criteria.where(RecordServiceConfigCollection.Fields.appId)
        .is(configuration.getAppId()));

    Update update = MongoHelper.getMongoTemplateUpdates(configuration,
        RecordServiceConfigCollection.Fields.sampleRate,
        RecordServiceConfigCollection.Fields.allowDayOfWeeks,
        RecordServiceConfigCollection.Fields.allowTimeOfDayFrom,
        RecordServiceConfigCollection.Fields.allowTimeOfDayTo,
        RecordServiceConfigCollection.Fields.excludeServiceOperationSet,
        RecordServiceConfigCollection.Fields.timeMock,
        RecordServiceConfigCollection.Fields.extendField,
        RecordServiceConfigCollection.Fields.serializeSkipInfoList);
    MongoHelper.withMongoTemplateBaseUpdate(update);
    update.set(RecordServiceConfigCollection.Fields.recordMachineCountLimit,
            configuration.getRecordMachineCountLimit() == null ? 1
                : configuration.getRecordMachineCountLimit());
    return mongoTemplate.updateMulti(filter, update, RecordServiceConfigCollection.class)
        .getModifiedCount() > 0;
  }

  @Override
  public boolean remove(ServiceCollectConfiguration configuration) {
    Query filter = new Query(Criteria.where(RecordServiceConfigCollection.Fields.appId)
        .is(configuration.getAppId()));
    return mongoTemplate.remove(filter, RecordServiceConfigCollection.class).getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ServiceCollectConfiguration configuration) {
    RecordServiceConfigCollection recordServiceConfigCollection =
        RecordServiceConfigMapper.INSTANCE.daoFromDto(configuration);
    return mongoTemplate.insert(recordServiceConfigCollection).getId() != null;
  }

  @Override
  public boolean removeByAppId(String appId) {
    Query filter = new Query(Criteria.where(RecordServiceConfigCollection.Fields.appId).is(appId));
    return mongoTemplate.remove(filter, RecordServiceConfigCollection.class).getDeletedCount() > 0;
  }

  @Override
  public boolean updateMultiEnvConfig(ServiceCollectConfiguration configuration) {
    Query filter = new Query(Criteria.where(RecordServiceConfigCollection.Fields.appId)
        .is(configuration.getAppId()));

    List<RecordServiceConfigCollection> configs = Optional.ofNullable(configuration.getMultiEnvConfigs())
        .orElse(Collections.emptyList())
        .stream().map(RecordServiceConfigMapper.INSTANCE::daoFromDto)
        .collect(Collectors.toList());

    Update update = new Update();
    update.set(MultiEnvBaseEntity.Fields.multiEnvConfigs, configs);
    update.set(BaseEntity.Fields.dataChangeUpdateTime, System.currentTimeMillis());
    return mongoTemplate.updateMulti(filter, update, RecordServiceConfigCollection.class)
        .getModifiedCount() > 0;
  }
}
