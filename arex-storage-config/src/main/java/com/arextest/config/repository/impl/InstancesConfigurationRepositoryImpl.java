package com.arextest.config.repository.impl;

import com.arextest.config.mapper.InstancesMapper;
import com.arextest.config.model.dao.config.DynamicClassCollection;
import com.arextest.config.model.dao.config.InstancesCollection;
import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import javax.annotation.PostConstruct;
import java.util.*;

public class InstancesConfigurationRepositoryImpl implements ConfigRepositoryProvider<InstancesConfiguration> {

    MongoCollection<InstancesCollection> mongoCollection;
    private MongoDatabase mongoDatabase;

    public InstancesConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    @PostConstruct
    public void init() {
        this.mongoCollection =
                mongoDatabase.getCollection(InstancesCollection.DOCUMENT_NAME, InstancesCollection.class);
    }

    @Override
    public List<InstancesConfiguration> list() {
        throw new UnsupportedOperationException("this method is not implemented");
    }

    @Override
    public List<InstancesConfiguration> listBy(String appId) {
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
        if (top == 0) {
            return Collections.emptyList();
        }
        Bson filter = Filters.eq(InstancesCollection.Fields.appId, appId);
        Bson sort = Sorts.ascending(DASH_ID);
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
        Bson filter = Filters.eq(DASH_ID, new ObjectId(configuration.getId()));
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public boolean insert(InstancesConfiguration configuration) {
        configuration.setDataUpdateTime(new Date());
        InstancesCollection instancesCollection = InstancesMapper.INSTANCE.daoFromDto(configuration);
        InsertOneResult insertOneResult = mongoCollection.insertOne(instancesCollection);
        if (insertOneResult.getInsertedId() != null) {
            configuration.setId(instancesCollection.getId());
        }
        return insertOneResult.getInsertedId() != null;

    }

    @Override
    public boolean removeByAppId(String appId) {

        Bson filter = Filters.eq(InstancesCollection.Fields.appId, appId);
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;
    }

    public boolean removeByAppIdAndHost(String appId, String host) {
        Bson filterCombine = Filters.and(Filters.eq(InstancesCollection.Fields.appId, appId),
                Filters.eq(InstancesCollection.Fields.host, host));
        DeleteResult remove = mongoCollection.deleteMany(filterCombine);
        return remove.getDeletedCount() > 0;
    }

}
