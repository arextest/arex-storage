package com.arextest.storage.web.api.service.bean;


import com.arextest.storage.core.repository.impl.mongo.MongoDbUtils;
import com.mongodb.client.MongoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jmo
 * @since 2021/10/18
 */
@Configuration
class StorageRepositoryConfiguration {
    @Value("${arex.storage.mongo.host:}")
    private String mongoDbHost;
    @Value("${arex.storage.mongo.dbName:arex_storage_db}")
    private String mongoDbName;

    @Bean
    MongoDatabase mongoRepository() {
        return MongoDbUtils.create(mongoDbHost, mongoDbName);
    }
}
