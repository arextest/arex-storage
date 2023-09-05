package com.arextest.storage.repository.impl.mongo.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.arextest.model.dao.BaseEntity;
import com.arextest.model.dao.config.ServiceCollection;
import com.arextest.storage.model.dto.config.application.ApplicationServiceConfiguration;
import com.arextest.storage.model.mapper.ServiceMapper;
import com.arextest.storage.repository.ConfigRepositoryProvider;
import com.arextest.storage.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;

@Repository
public class ApplicationServiceConfigurationRepositoryImpl
    implements ConfigRepositoryProvider<ApplicationServiceConfiguration> {

    @Autowired
    ApplicationOperationConfigurationRepositoryImpl applicationOperationConfigurationRepository;

    @Resource
    private MongoDatabase mongoDatabase;

    private MongoCollection<ServiceCollection> mongoCollection;

    @PostConstruct
    public void init() {
        mongoCollection = this.getCollection();
    }

    @Override
    public String getCollectionName() {
        return ServiceCollection.DOCUMENT_NAME;
    }

    @Override
    public MongoCollection<ServiceCollection> getCollection() {
        return mongoDatabase.getCollection(this.getCollectionName(), ServiceCollection.class);
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
            // TODO：verify the id type
            configuration.setId(insertOneResult.getInsertedId().toString());
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
