package com.arextest.config.repository.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.arextest.config.mapper.AppMapper;
import com.arextest.config.model.dao.BaseEntity;
import com.arextest.config.model.dao.config.AppCollection;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
public class ApplicationConfigurationRepositoryImpl implements ConfigRepositoryProvider<ApplicationConfiguration> {
    private MongoDatabase mongoDatabase;

    private MongoCollection<AppCollection> mongoCollection;

    @Resource
    private List<ConfigRepositoryProvider> configRepositoryProviders;

    public ApplicationConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    @PostConstruct
    public void init() {
        mongoCollection = this.getCollection();
    }

    @Override
    public String getCollectionName() {
        return AppCollection.DOCUMENT_NAME;
    }

    @Override
    public MongoCollection<AppCollection> getCollection() {
        return mongoDatabase.getCollection(this.getCollectionName(), AppCollection.class);
    }

    @Override
    public List<ApplicationConfiguration> list() {
        // Query query = new Query();
        // query.with(Sort.by(Sort.Order.desc(DASH_ID)));
        // List<AppCollection> appCollections = mongoTemplate.find(query, AppCollection.class);
        // return appCollections.stream().map(AppMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());

        Bson sort = Sorts.descending(BaseEntity.Fields.id);
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
        // Query query = Query.query(Criteria.where(appId).is(appId));
        // List<AppCollection> appCollections = mongoTemplate.find(query, AppCollection.class);
        // return appCollections.stream().map(AppMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());

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
        // Query query = Query.query(Criteria.where(appId).is(configuration.getAppId()));
        // Update update = MongoHelper.getConfigUpdate();
        // MongoHelper.assertNull("update parameter is null", configuration.getAgentVersion(),
        // configuration.getAgentExtVersion(), configuration.getStatus());

        // update.set(AGENT_VERSION, configuration.getAgentVersion());
        // update.set(AGENT_EXT_VERSION, configuration.getAgentExtVersion());
        // update.set(STATUS, configuration.getStatus());
        // update.set(FEATURES, configuration.getFeatures());

        // UpdateResult updateResult = mongoTemplate.updateMulti(query, update, AppCollection.class);
        // return updateResult.getModifiedCount() > 0;

        Bson filter = Filters.eq(AppCollection.Fields.appId, configuration.getAppId());

        List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
            MongoHelper.getSpecifiedProperties(configuration, AppCollection.Fields.agentVersion,
                AppCollection.Fields.agentExtVersion, AppCollection.Fields.status, AppCollection.Fields.features));
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

        // Query query = Query.query(Criteria.where(appId).is(configuration.getAppId()));
        // DeleteResult remove = mongoTemplate.remove(query, AppCollection.class);
        // return remove.getDeletedCount() > 0;
        Bson filter = Filters.eq(AppCollection.Fields.appId, configuration.getAppId());
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public boolean insert(ApplicationConfiguration configuration) {
        // AppCollection appCollection = AppMapper.INSTANCE.daoFromDto(configuration);
        // AppCollection insert = mongoTemplate.insert(appCollection);
        // return insert.getId() != null;
        AppCollection appCollection = AppMapper.INSTANCE.daoFromDto(configuration);
        InsertOneResult insertOneResult = mongoCollection.insertOne(appCollection);
        return insertOneResult.getInsertedId() != null;
    }

}
