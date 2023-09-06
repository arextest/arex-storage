package com.arextest.storage.repository.impl.mongo.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import com.arextest.model.dao.BaseEntity;
import com.arextest.model.dao.config.DynamicClassCollection;
import com.arextest.storage.model.dto.config.record.DynamicClassConfiguration;
import com.arextest.storage.model.mapper.DynamicClassMapper;
import com.arextest.storage.repository.ConfigRepositoryProvider;
import com.arextest.storage.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

@Repository
public class DynamicClassConfigurationRepositoryImpl implements ConfigRepositoryProvider<DynamicClassConfiguration> {

    @Resource
    private MongoDatabase mongoDatabase;

    MongoCollection<DynamicClassCollection> mongoCollection;

    @PostConstruct
    public void init() {
        mongoCollection = this.getCollection();
    }

    @Override
    public String getCollectionName() {
        return DynamicClassCollection.DOCUMENT_NAME;
    }

    @Override
    public MongoCollection<DynamicClassCollection> getCollection() {
        return mongoDatabase.getCollection(this.getCollectionName(), DynamicClassCollection.class);
    }

    @Override
    public List<DynamicClassConfiguration> list() {
        // Query query = new Query();
        // List<DynamicClassCollection> dynamicClassCollections = mongoTemplate.find(query,
        // DynamicClassCollection.class);
        // return
        // dynamicClassCollections.stream().map(DynamicClassMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
        // return null;
        throw new UnsupportedOperationException("this method is not implemented");
    }

    @Override
    public List<DynamicClassConfiguration> listBy(String appId) {
        // Query query = Query.query(Criteria.where(appId).is(appId));
        // List<DynamicClassCollection> dynamicClassCollections = mongoTemplate.find(query,
        // DynamicClassCollection.class);
        // return
        // dynamicClassCollections.stream().map(DynamicClassMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
        // return null;

        // MongoCollection<Document> collection = mongoTemplate.getCollection("dynamicClassCollection");
        Bson filter = Filters.eq(DynamicClassCollection.Fields.appId, appId);
        List<DynamicClassConfiguration> dtos = new ArrayList<>();
        try (MongoCursor<DynamicClassCollection> cursor = mongoCollection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                DynamicClassCollection document = cursor.next();
                DynamicClassConfiguration dto = DynamicClassMapper.INSTANCE.dtoFromDao(document);
                dtos.add(dto);
            }
        }
        return dtos;
    }

    @Override
    public boolean update(DynamicClassConfiguration configuration) {
        // Query query = Query.query(Criteria.where(DASH_ID).is(configuration.getId()));
        // Update update = MongoHelper.getConfigUpdate();
        // update.set(FULL_CLASS_NAME, configuration.getFullClassName());
        // update.set(METHOD_NAME, configuration.getMethodName());
        // update.set(PARAMETER_TYPES, configuration.getParameterTypes());
        // UpdateResult updateResult = mongoTemplate.updateMulti(query, update, DynamicClassCollection.class);
        // return updateResult.getModifiedCount() > 0;
        // return false;
        Bson filter = Filters.eq(BaseEntity.Fields.id, configuration.getId());

        List<Bson> updateList = Arrays.asList(
                MongoHelper.getUpdate(),
                MongoHelper.getSpecifiedProperties(configuration, DynamicClassCollection.Fields.fullClassName,
                        DynamicClassCollection.Fields.methodName, DynamicClassCollection.Fields.parameterTypes)
        );
        Bson updateCombine = Updates.combine(updateList);

        UpdateResult updateResult = mongoCollection.updateMany(filter, updateCombine);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean remove(DynamicClassConfiguration configuration) {
        // Query query = Query.query(Criteria.where(DASH_ID).is(configuration.getId()));
        // DeleteResult remove = mongoTemplate.remove(query, DynamicClassCollection.class);
        // return remove.getDeletedCount() > 0;
        // return false;
        Bson filter = Filters.eq(BaseEntity.Fields.id, configuration.getId());
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public boolean insert(DynamicClassConfiguration configuration) {
        // DynamicClassCollection dynamicClassConfiguration = DynamicClassMapper.INSTANCE.daoFromDto(configuration);
        // DynamicClassCollection insert = mongoTemplate.insert(dynamicClassConfiguration);
        // if (insert.getId() != null) {
        // configuration.setId(insert.getId());
        // }
        // return insert.getId() != null;
        DynamicClassCollection dynamicClassCollection = DynamicClassMapper.INSTANCE.daoFromDto(configuration);
        InsertOneResult insertOneResult = mongoCollection.insertOne(dynamicClassCollection);
        if (insertOneResult.getInsertedId() != null) {
            configuration.setId(dynamicClassCollection.getId());
        }
        return insertOneResult.getInsertedId() != null;
    }

    @Override
    public boolean removeByAppId(String appId) {
        // Query query = Query.query(Criteria.where(appId).is(appId));
        // DeleteResult remove = mongoTemplate.remove(query, DynamicClassCollection.class);
        // return remove.getDeletedCount() > 0;
        Bson filter = Filters.eq(DynamicClassCollection.Fields.appId, appId);
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }
}