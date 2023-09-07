package com.arextest.config.repository.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.bson.conversions.Bson;

import com.arextest.config.mapper.ServiceMapper;
import com.arextest.config.model.dao.BaseEntity;
import com.arextest.config.model.dao.config.ServiceCollection;
import com.arextest.config.model.dto.application.ApplicationServiceConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;

public class ApplicationServiceConfigurationRepositoryImpl
    implements ConfigRepositoryProvider<ApplicationServiceConfiguration> {

    private MongoDatabase mongoDatabase;
    @Resource
    private ApplicationOperationConfigurationRepositoryImpl applicationOperationConfigurationRepository;

    public ApplicationServiceConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    private MongoCollection<ServiceCollection> mongoCollection;

    @PostConstruct
    public void init() {
        this.mongoCollection = mongoDatabase.getCollection(ServiceCollection.DOCUMENT_NAME, ServiceCollection.class);
    }

    @Override
    public List<ApplicationServiceConfiguration> list() {
        throw new UnsupportedOperationException("this method is not implemented");
    }

    @Override
    public List<ApplicationServiceConfiguration> listBy(String appId) {
        // Query query = Query.query(Criteria.where(APP_ID).is(appId));
        // List<ServiceCollection> serviceCollections = mongoTemplate.find(query, ServiceCollection.class);
        // List<ApplicationServiceConfiguration> applicationServiceConfigurations =
        // serviceCollections.stream().map(ServiceMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
        // applicationServiceConfigurations.forEach(item ->
        // item.setOperationList(applicationOperationConfigurationRepository.operationBaseInfoList(item.getId())));
        // return applicationServiceConfigurations;

        Bson filter = Filters.eq(ServiceCollection.Fields.appId, appId);
        List<ApplicationServiceConfiguration> dtos = new ArrayList<>();
        try (MongoCursor<ServiceCollection> cursor = mongoCollection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                ServiceCollection document = cursor.next();
                ApplicationServiceConfiguration dto = ServiceMapper.INSTANCE.dtoFromDao(document);
                dto.setOperationList(applicationOperationConfigurationRepository.operationBaseInfoList(dto.getId()));
                dtos.add(dto);
            }
        }
        return dtos;
    }

    @Override
    public boolean update(ApplicationServiceConfiguration configuration) {
        // Query query = Query.query(Criteria.where(DASH_ID).is(configuration.getId()));
        // Update update = MongoHelper.getConfigUpdate();
        // MongoHelper.assertNull("update parameter is null", configuration.getStatus());
        // update.set(STATUS, configuration.getStatus());
        // UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ServiceCollection.class);
        // return updateResult.getModifiedCount() > 0;
        Bson filter = Filters.eq(BaseEntity.Fields.id, configuration.getId());

        List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
            Updates.set(ServiceCollection.Fields.status, configuration.getStatus()));
        Bson updateCombine = Updates.combine(updateList);

        return mongoCollection.updateMany(filter, updateCombine).getModifiedCount() > 0;
    }

    @Override
    public boolean remove(ApplicationServiceConfiguration configuration) {
        // Query query = Query.query(Criteria.where(DASH_ID).is(configuration.getId()));
        // DeleteResult remove = mongoTemplate.remove(query, ServiceCollection.class);
        //
        // return remove.getDeletedCount() > 0;
        Bson filter = Filters.eq(BaseEntity.Fields.id, configuration.getId());
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public boolean insert(ApplicationServiceConfiguration configuration) {
        // ServiceCollection serviceCollection = ServiceMapper.INSTANCE.daoFromDto(configuration);
        // ServiceCollection insert = mongoTemplate.insert(serviceCollection);
        // if (insert.getId() != null) {
        // configuration.setId(insert.getId());
        // }
        // return insert.getId() != null;
        ServiceCollection serviceCollection = ServiceMapper.INSTANCE.daoFromDto(configuration);
        InsertOneResult insertOneResult = mongoCollection.insertOne(serviceCollection);
        if (insertOneResult.getInsertedId() != null) {
            configuration.setId(serviceCollection.getId());
        }
        return insertOneResult.getInsertedId() != null;
    }

    @Override
    public long count(String appId) {
        // Query query = Query.query(Criteria.where(APP_ID).is(appId));
        // return mongoTemplate.count(query, ServiceCollection.class);
        Bson filter = Filters.eq(ServiceCollection.Fields.appId, appId);
        return mongoCollection.countDocuments(filter);
    }

    @Override
    public boolean removeByAppId(String appId) {
        // Query query = Query.query(Criteria.where(APP_ID).is(appId));
        // DeleteResult remove = mongoTemplate.remove(query, ServiceCollection.class);
        // return remove.getDeletedCount() > 0;

        Bson filter = Filters.eq(ServiceCollection.Fields.appId, appId);
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }
}
