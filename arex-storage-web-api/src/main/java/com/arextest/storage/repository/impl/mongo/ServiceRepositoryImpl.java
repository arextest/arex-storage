package com.arextest.storage.repository.impl.mongo;

import com.arextest.storage.model.dao.ServiceEntity;
import com.arextest.storage.repository.ServiceRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

/**
 * @author b_yu
 * @since 2022/8/25
 */
@Repository
public class ServiceRepositoryImpl implements ServiceRepository {

    private static final String APP_ID = "appId";

    @Resource
    private MongoDatabase mongoDatabase;

    @Override
    public String getCollectionName() {
        return "Service";
    }
    @Override
    public MongoCollection<ServiceEntity> getCollection() {
        return mongoDatabase.getCollection(this.getCollectionName(), ServiceEntity.class);
    }
    @Override
    public ServiceEntity queryByAppId(String appId) {
        Iterable<ServiceEntity> iter = getCollection().find(Filters.eq(APP_ID, appId));
        if (iter.iterator().hasNext()) {
            return iter.iterator().next();
        }
        return null;
    }
}