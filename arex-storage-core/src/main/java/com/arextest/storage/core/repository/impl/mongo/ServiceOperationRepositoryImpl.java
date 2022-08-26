package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.core.repository.ServiceOperationRepository;
import com.arextest.storage.model.dao.ServiceOperationEntity;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.omg.CORBA.PRIVATE_MEMBER;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

/**
 * @author b_yu
 * @since 2022/8/25
 */
@Repository
public class ServiceOperationRepositoryImpl implements ServiceOperationRepository {
    private static final String APP_ID = "appId";
    private static final String SERVICE_ID = "serviceId";
    private static final String OPERATION_NAME = "operationName";
    private static final String OPERATION_TYPE = "operationType";
    private static final String STATUS = "status";

    @Resource
    private MongoDatabase mongoDatabase;

    @Override
    public String getCollectionName() {
        return "ServiceOperation";
    }

    @Override
    public MongoCollection<ServiceOperationEntity> getCollection() {
        return mongoDatabase.getCollection(this.getCollectionName(), ServiceOperationEntity.class);
    }
    @Override
    public boolean findAndUpdate(ServiceOperationEntity entity) {
        Bson query = Filters.and(Filters.eq(SERVICE_ID, entity.getServiceId()),
                Filters.eq(OPERATION_NAME, entity.getOperationName()),
                Filters.eq(APP_ID, entity.getAppId()));
        Bson update = Updates.combine(Updates.set(OPERATION_TYPE, entity.getOperationType()),
                Updates.set(STATUS, 4),
                Updates.set(DATA_CHANGE_UPDATE_TIME, System.currentTimeMillis()),
                Updates.setOnInsert(DATA_CHANGE_CREATE_TIME, System.currentTimeMillis()));

        entity = getCollection().findOneAndUpdate(query,
                update,
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));

        if (entity != null && StringUtils.isNotEmpty(entity.getId().toString())) {
            return true;
        }
        return false;
    }
}
