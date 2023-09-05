package com.arextest.storage.repository.impl.mongo.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.arextest.storage.utils.MongoHelper;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import com.arextest.model.dao.BaseEntity;
import com.arextest.model.dao.config.ServiceOperationCollection;
import com.arextest.storage.model.dto.config.application.ApplicationOperationConfiguration;
import com.arextest.storage.model.mapper.ServiceOperationMapper;
import com.arextest.storage.repository.ConfigRepositoryProvider;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;

@Repository
public class ApplicationOperationConfigurationRepositoryImpl
    implements ConfigRepositoryProvider<ApplicationOperationConfiguration> {

    @Resource
    private MongoDatabase mongoDatabase;

    private MongoCollection<ServiceOperationCollection> mongoCollection;

    @PostConstruct
    public void init() {
        mongoCollection = this.getCollection();
    }

    @Override
    public String getCollectionName() {
        return ServiceOperationCollection.DOCUMENT_NAME;
    }

    @Override
    public MongoCollection<ServiceOperationCollection> getCollection() {
        return mongoDatabase.getCollection(this.getCollectionName(), ServiceOperationCollection.class);
    }

    @Override
    public List<ApplicationOperationConfiguration> list() {
        throw new UnsupportedOperationException("this method is not implemented");
    }

    @Override
    public List<ApplicationOperationConfiguration> listBy(String appId) {
        // Query query = Query.query(Criteria.where(APP_ID).is(appId));
        // List<ServiceOperationCollection> serviceOperationCollections =
        // mongoTemplate.find(query, ServiceOperationCollection.class);
        // return serviceOperationCollections.stream().map(ServiceOperationMapper.INSTANCE::dtoFromDao)
        // .collect(Collectors.toList());

        Bson filter = Filters.eq(ServiceOperationCollection.Fields.appId, appId);
        List<ApplicationOperationConfiguration> dtos = new ArrayList<>();
        try (MongoCursor<ServiceOperationCollection> cursor = mongoCollection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                ServiceOperationCollection document = cursor.next();
                ApplicationOperationConfiguration dto = ServiceOperationMapper.INSTANCE.dtoFromDao(document);
                dtos.add(dto);
            }
        }
        return dtos;
    }

    @Override
    public boolean update(ApplicationOperationConfiguration configuration) {
        // Query query = Query.query(Criteria.where(DASH_ID).is(configuration.getId()));
        // Update update = MongoHelper.getConfigUpdate();
        // MongoHelper.appendSpecifiedProperties(update, configuration, STATUS, RECORDED_CASE_COUNT,
        // OPERATION_RESPONSE);
        // UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ServiceOperationCollection.class);
        // return updateResult.getModifiedCount() > 0;

        Bson filter = Filters.eq(BaseEntity.Fields.id, configuration.getId());
        List<Bson> updateList = Arrays.asList(
                MongoHelper.getUpdate(),
                MongoHelper.getSpecifiedProperties(configuration, ServiceOperationCollection.Fields.status)
        );
        Bson updateCombine = Updates.combine(updateList);

        return mongoCollection.updateMany(filter, updateCombine).getModifiedCount() > 0;
    }

    @Override
    public boolean remove(ApplicationOperationConfiguration configuration) {
        // Query query = Query.query(Criteria.where(DASH_ID).is(configuration.getId()));
        // DeleteResult remove = mongoTemplate.remove(query, ServiceOperationCollection.class);
        // return remove.getDeletedCount() > 0;
        Bson filter = Filters.eq(BaseEntity.Fields.id, configuration.getId());
        return mongoCollection.deleteMany(filter).getDeletedCount() > 0;
    }

    @Override
    public boolean insert(ApplicationOperationConfiguration configuration) {
        // ServiceOperationCollection serviceOperationCollection =
        // ServiceOperationMapper.INSTANCE.daoFromDto(configuration);
        // ServiceOperationCollection insert = mongoTemplate.insert(serviceOperationCollection);
        // if (insert.getId() != null) {
        // configuration.setId(insert.getId());
        // }
        // return insert.getId() != null;

        ServiceOperationCollection serviceOperationCollection =
            ServiceOperationMapper.INSTANCE.daoFromDto(configuration);
        InsertOneResult insertOneResult = mongoCollection.insertOne(serviceOperationCollection);
        if (insertOneResult.getInsertedId() != null) {
            // TODO：verify the id type
            configuration.setId(insertOneResult.getInsertedId().toString());
        }
        return insertOneResult.getInsertedId() != null;

    }

    public ApplicationOperationConfiguration listByOperationId(String operationId) {
        // Query query = Query.query(Criteria.where(DASH_ID).is(operationId));
        // ServiceOperationCollection serviceOperationCollection =
        // mongoTemplate.findOne(query, ServiceOperationCollection.class);
        // return serviceOperationCollection == null ? null
        // : ServiceOperationMapper.INSTANCE.dtoFromDao(serviceOperationCollection);

        Bson filter = Filters.eq(BaseEntity.Fields.id, operationId);
        // TODO: verify the result
        ServiceOperationCollection document = mongoCollection.find(filter).first();
        return document == null ? null : ServiceOperationMapper.INSTANCE.dtoFromDao(document);
    }

    // the search of operation's based—info by serviceId
    public List<ApplicationOperationConfiguration> operationBaseInfoList(String serviceId) {
        // Query query = Query.query(Criteria.where(SERVICE_ID).is(serviceId));
        // List<ServiceOperationCollection> serviceOperationCollections =
        // mongoTemplate.find(query, ServiceOperationCollection.class);
        // return serviceOperationCollections.stream().map(ServiceOperationMapper.INSTANCE::baseInfoFromDao)
        // .collect(Collectors.toList());

        Bson filter = Filters.eq(ServiceOperationCollection.Fields.serviceId, serviceId);
        List<ApplicationOperationConfiguration> dtos = new ArrayList<>();
        try (MongoCursor<ServiceOperationCollection> cursor = mongoCollection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                ServiceOperationCollection document = cursor.next();
                ApplicationOperationConfiguration dto = ServiceOperationMapper.INSTANCE.baseInfoFromDao(document);
                dtos.add(dto);
            }
        }
        return dtos;
    }

    @Override
    public boolean removeByAppId(String appId) {
        // Query query = Query.query(Criteria.where(APP_ID).is(appId));
        // DeleteResult remove = mongoTemplate.remove(query, ServiceOperationCollection.class);
        // return remove.getDeletedCount() > 0;

        Bson filter = Filters.eq(ServiceOperationCollection.Fields.appId, appId);
        DeleteResult deleteResult = mongoCollection.deleteMany(filter);
        return deleteResult.getDeletedCount() > 0;

    }

    public boolean findAndUpdate(ApplicationOperationConfiguration configuration) {

        Bson query = Filters.and(Filters.eq(ServiceOperationCollection.Fields.serviceId, configuration.getServiceId()),
                Filters.eq(ServiceOperationCollection.Fields.operationName, configuration.getOperationName()),
                Filters.eq(ServiceOperationCollection.Fields.appId, configuration.getAppId()));

        List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
                MongoHelper.getSpecifiedProperties(configuration, ServiceOperationCollection.Fields.operationType, ServiceOperationCollection.Fields.status),
                Updates.addEachToSet(ServiceOperationCollection.Fields.operationTypes, new ArrayList<>(configuration.getOperationTypes()))
        );
        Bson updateCombine = Updates.combine(updateList);

        ServiceOperationCollection dao = mongoCollection.findOneAndUpdate(query,
                updateCombine,
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));

        return dao != null && StringUtils.isNotEmpty(dao.getId());
    }
}
