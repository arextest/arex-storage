package com.arextest.storage.repository.impl.mongo.config;

import java.util.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import com.arextest.model.dao.BaseEntity;
import com.arextest.model.dao.config.DynamicClassCollection;
import com.arextest.model.dao.config.InstancesCollection;
import com.arextest.storage.model.dto.config.application.InstancesConfiguration;
import com.arextest.storage.model.mapper.InstancesMapper;
import com.arextest.storage.repository.ConfigRepositoryProvider;
import com.arextest.storage.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class InstancesConfigurationRepositoryImpl implements ConfigRepositoryProvider<InstancesConfiguration> {

    @Resource
    private MongoDatabase mongoDatabase;

    MongoCollection<InstancesCollection> mongoCollection;

    @PostConstruct
    public void init() {
        mongoCollection = this.getCollection();
    }

    @Override
    public String getCollectionName() {
        return InstancesCollection.DOCUMENT_NAME;
    }

    @Override
    public MongoCollection<InstancesCollection> getCollection() {
        return mongoDatabase.getCollection(this.getCollectionName(), InstancesCollection.class);
    }

    @Override
    public List<InstancesConfiguration> list() {
        // Query query = new Query();
        // List<InstancesCollection> instancesCollections = mongoTemplate.find(query, InstancesCollection.class);
        // return instancesCollections.stream().map(InstancesMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
        throw new UnsupportedOperationException("this method is not implemented");
    }

    @Override
    public List<InstancesConfiguration> listBy(String appId) {
        // Query query = Query.query(Criteria.where(appId).is(appId));
        // List<InstancesCollection> instancesCollections = mongoTemplate.find(query, InstancesCollection.class);
        // return instancesCollections.stream().map(InstancesMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
        Bson filter = Filters.eq(DynamicClassCollection.Fields.appId, appId);
        List<InstancesConfiguration> dtos = new ArrayList<>();
        try (MongoCursor<InstancesCollection> cursor = mongoCollection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                InstancesCollection document = cursor.next();
                InstancesConfiguration dto = InstancesMapper.INSTANCE.dtoFromDao(document);
                dtos.add(dto);
            }
        }
        return dtos;
    }

    public List<InstancesConfiguration> listBy(String appId, int top) {
        // if (top == 0) {
        // return Collections.emptyList();
        // }
        // Query query = Query.query(Criteria.where(appId).is(appId)).limit(top).with(Sort.by(DASH_ID).ascending());
        // List<InstancesCollection> instancesCollections = mongoTemplate.find(query, InstancesCollection.class);
        // return instancesCollections.stream().map(InstancesMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
        if (top == 0) {
            return Collections.emptyList();
        }
        Bson filter = Filters.eq(InstancesCollection.Fields.appId, appId);
        Bson sort = Sorts.ascending(BaseEntity.Fields.id);
        List<InstancesConfiguration> instancesDTOList = new ArrayList<>();
        try (MongoCursor<InstancesCollection> cursor = mongoCollection.find(filter).sort(sort).limit(top).iterator()) {
            while (cursor.hasNext()) {
                InstancesCollection document = cursor.next();
                InstancesConfiguration dto = InstancesMapper.INSTANCE.dtoFromDao(document);
                instancesDTOList.add(dto);
            }
        }
        return instancesDTOList;
    }

    @Override
    public boolean update(InstancesConfiguration configuration) {
        Bson filter = Filters.and(Filters.eq(InstancesCollection.Fields.appId, configuration.getAppId()),
            Filters.eq(InstancesCollection.Fields.host, configuration.getHost()));

        List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(), MongoHelper.getFullProperties(configuration),
            Updates.set(InstancesCollection.Fields.dataUpdateTime, new Date()));
        Bson updateCombine = Updates.combine(updateList);

        FindOneAndUpdateOptions options =
            new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(true);
        InstancesCollection result = mongoCollection.findOneAndUpdate(filter, updateCombine, options);
        return result != null;
    }

    @Override
    public boolean remove(InstancesConfiguration configuration) {
        // Query query = Query.query(Criteria.where(DASH_ID).is(configuration.getId()));
        // DeleteResult remove = mongoTemplate.remove(query, InstancesCollection.class);
        // return remove.getDeletedCount() > 0;
        Bson filter = Filters.eq(BaseEntity.Fields.id, configuration.getId());
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public boolean insert(InstancesConfiguration configuration) {
        configuration.setDataUpdateTime(new Date());
        InstancesCollection instancesCollection = InstancesMapper.INSTANCE.daoFromDto(configuration);
        InsertOneResult insertOneResult = mongoCollection.insertOne(instancesCollection);
        if (insertOneResult.getInsertedId() != null) {
            configuration.setId(insertOneResult.getInsertedId().toString());
        }
        return insertOneResult.getInsertedId() != null;

    }

    @Override
    public boolean removeByAppId(String appId) {
        // Query query = Query.query(Criteria.where(appId).is(appId));
        // DeleteResult remove = mongoTemplate.remove(query, InstancesCollection.class);
        // return remove.getDeletedCount() > 0;

        Bson filter = Filters.eq(InstancesCollection.Fields.appId, appId);
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }

}
