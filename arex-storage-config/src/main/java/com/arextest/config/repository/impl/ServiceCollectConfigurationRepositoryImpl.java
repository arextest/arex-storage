package com.arextest.config.repository.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.bson.conversions.Bson;

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

public class ServiceCollectConfigurationRepositoryImpl
    implements ConfigRepositoryProvider<ServiceCollectConfiguration> {

    // private static final String SAMPLE_RATE = "sampleRate";
    // private static final String ALLOW_DAY_OF_WEEKS = "allowDayOfWeeks";
    // private static final String ALLOW_TIME_OF_DAY_FROM = "allowTimeOfDayFrom";
    // private static final String ALLOW_TIME_OF_DAY_TO = "allowTimeOfDayTo";
    // private static final String EXCLUDE_SERVICE_OPERATION_SET = "excludeServiceOperationSet";
    // private static final String TIME_MOCK = "timeMock";
    //
    // private static final String RECORD_MACHINE_COUNT_LIMIT = "recordMachineCountLimit";
    //
    // private static final String EXTEND_FIELD = "extendField";
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

    // @Override
    // public String getCollectionName() {
    // return RecordServiceConfigCollection.DOCUMENT_NAME;
    // }
    //
    // @Override
    // public MongoCollection<RecordServiceConfigCollection> getCollection() {
    // return mongoDatabase.getCollection(this.getCollectionName(), RecordServiceConfigCollection.class);
    // }

    @Override
    public List<ServiceCollectConfiguration> list() {
        // Query query = new Query();
        // List<RecordServiceConfigCollection> recordServiceConfigCollections =
        // mongoTemplate.find(query, RecordServiceConfigCollection.class);
        // return recordServiceConfigCollections.stream()
        // .map(RecordServiceConfigMapper.INSTANCE::dtoFromDao)
        // .collect(Collectors.toList());
        throw new UnsupportedOperationException("this method is not implemented");
    }

    @Override
    public List<ServiceCollectConfiguration> listBy(String appId) {
        // Query query = Query.query(Criteria.where(appId).is(appId));
        // List<RecordServiceConfigCollection> recordServiceConfigCollections =
        // mongoTemplate.find(query, RecordServiceConfigCollection.class);
        // return recordServiceConfigCollections.stream().map(RecordServiceConfigMapper.INSTANCE::dtoFromDao)
        // .collect(Collectors.toList());

        Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, appId);
        List<ServiceCollectConfiguration> recordServiceConfigs = new ArrayList<>();
        try (MongoCursor<RecordServiceConfigCollection> cursor = mongoCollection.find(filter).iterator()) {
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
        // Query query = Query.query(Criteria.where(appId).is(configuration.getAppId()));
        // Update update = MongoHelper.getConfigUpdate();
        // MongoHelper.assertNull("update parameter is null", configuration.getAllowTimeOfDayFrom(),
        // configuration.getAllowTimeOfDayTo());

        // update.set(SAMPLE_RATE, configuration.getSampleRate());
        // update.set(ALLOW_DAY_OF_WEEKS, configuration.getAllowDayOfWeeks());
        // update.set(ALLOW_TIME_OF_DAY_FROM, configuration.getAllowTimeOfDayFrom());
        // update.set(ALLOW_TIME_OF_DAY_TO, configuration.getAllowTimeOfDayTo());
        // update.set(EXCLUDE_SERVICE_OPERATION_SET, configuration.getExcludeServiceOperationSet());
        // update.set(TIME_MOCK, configuration.isTimeMock());
        // update.set(EXTEND_FIELD, configuration.getExtendField());

        // update.set(RECORD_MACHINE_COUNT_LIMIT,
        // configuration.getRecordMachineCountLimit() == null ? 1 : configuration.getRecordMachineCountLimit());

        // UpdateResult updateResult = mongoTemplate.updateMulti(query, update, RecordServiceConfigCollection.class);
        // return updateResult.getModifiedCount() > 0;

        Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, configuration.getAppId());

        List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
            MongoHelper.getSpecifiedProperties(configuration, RecordServiceConfigCollection.Fields.sampleRate,
                RecordServiceConfigCollection.Fields.allowDayOfWeeks,
                RecordServiceConfigCollection.Fields.allowTimeOfDayFrom,
                RecordServiceConfigCollection.Fields.allowTimeOfDayTo,
                RecordServiceConfigCollection.Fields.excludeServiceOperationSet,
                RecordServiceConfigCollection.Fields.timeMock, RecordServiceConfigCollection.Fields.extendField),
            Updates.set(RecordServiceConfigCollection.Fields.recordMachineCountLimit,
                configuration.getRecordMachineCountLimit() == null ? 1 : configuration.getRecordMachineCountLimit())

        );
        Bson updateCombine = Updates.combine(updateList);

        UpdateResult updateResult = mongoCollection.updateMany(filter, updateCombine);
        return updateResult.getModifiedCount() > 0;

    }

    @Override
    public boolean remove(ServiceCollectConfiguration configuration) {
        // Query query = Query.query(Criteria.where(appId).is(configuration.getAppId()));
        // DeleteResult remove = mongoTemplate.remove(query, RecordServiceConfigCollection.class);
        // return remove.getDeletedCount() > 0;
        Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, configuration.getAppId());
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public boolean insert(ServiceCollectConfiguration configuration) {
        // RecordServiceConfigCollection recordServiceConfigCollection =
        // RecordServiceConfigMapper.INSTANCE.daoFromDto(configuration);
        // RecordServiceConfigCollection insert = mongoTemplate.insert(recordServiceConfigCollection);
        // return insert.getId() != null;
        RecordServiceConfigCollection recordServiceConfigCollection =
            RecordServiceConfigMapper.INSTANCE.daoFromDto(configuration);
        InsertOneResult insertOneResult = mongoCollection.insertOne(recordServiceConfigCollection);
        return insertOneResult.getInsertedId() != null;
    }

    @Override
    public boolean removeByAppId(String appId) {
        // Query query = Query.query(Criteria.where(appId).is(appId));
        // DeleteResult remove = mongoTemplate.remove(query, RecordServiceConfigCollection.class);
        // return remove.getDeletedCount() > 0;
        Bson filter = Filters.eq(RecordServiceConfigCollection.Fields.appId, appId);
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }
}
