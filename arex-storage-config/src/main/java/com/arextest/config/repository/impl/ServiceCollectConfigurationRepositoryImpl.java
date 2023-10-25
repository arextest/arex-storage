package com.arextest.config.repository.impl;

import com.arextest.config.mapper.RecordServiceConfigMapper;
import com.arextest.config.model.dao.config.RecordServiceConfigCollection;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import org.bson.conversions.Bson;

public class ServiceCollectConfigurationRepositoryImpl
    implements ConfigRepositoryProvider<ServiceCollectConfiguration> {

  private MongoDatabase mongoDatabase;

  private MongoCollection<RecordServiceConfigCollection> mongoCollection;

  public ServiceCollectConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
    this.mongoDatabase = mongoDatabase;
  }

  @PostConstruct
  public void init() {
    this.mongoCollection = mongoDatabase.getCollection(RecordServiceConfigCollection.DOCUMENT_NAME,
        RecordServiceConfigCollection.class);
  }

  @Override
  public List<ServiceCollectConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ServiceCollectConfiguration> listBy(String appId) {

    Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, appId);
    List<ServiceCollectConfiguration> recordServiceConfigs = new ArrayList<>();
    try (MongoCursor<RecordServiceConfigCollection> cursor = mongoCollection.find(filter)
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
            RecordServiceConfigCollection.Fields.serializeSkipInfoList),
        Updates.set(RecordServiceConfigCollection.Fields.recordMachineCountLimit,
            configuration.getRecordMachineCountLimit() == null ? 1
                : configuration.getRecordMachineCountLimit())

    );
    Bson updateCombine = Updates.combine(updateList);

    UpdateResult updateResult = mongoCollection.updateMany(filter, updateCombine);
    return updateResult.getModifiedCount() > 0;

  }

  @Override
  public boolean remove(ServiceCollectConfiguration configuration) {
    Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, configuration.getAppId());
    DeleteResult deleteResult = mongoCollection.deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ServiceCollectConfiguration configuration) {
    RecordServiceConfigCollection recordServiceConfigCollection =
        RecordServiceConfigMapper.INSTANCE.daoFromDto(configuration);
    InsertOneResult insertOneResult = mongoCollection.insertOne(recordServiceConfigCollection);
    return insertOneResult.getInsertedId() != null;
  }

  @Override
  public boolean removeByAppId(String appId) {
    Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, appId);
    DeleteResult deleteResult = mongoCollection.deleteMany(filter);
    return deleteResult.getDeletedCount() > 0;
  }
}
