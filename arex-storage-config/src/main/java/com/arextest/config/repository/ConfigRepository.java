package com.arextest.config.repository;


import com.mongodb.client.MongoCollection;

public interface ConfigRepository<T> {
    String DATA_CHANGE_CREATE_TIME = "dataChangeCreateTime";
    String DATA_CHANGE_UPDATE_TIME = "dataChangeUpdateTime";
    String DASH_ID = "_id";

    String getCollectionName();

    MongoCollection<T> getCollection();
}