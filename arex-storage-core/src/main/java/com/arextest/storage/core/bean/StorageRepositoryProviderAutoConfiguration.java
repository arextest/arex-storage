package com.arextest.storage.core.bean;


import com.arextest.storage.core.repository.impl.mongo.MongoDbUtils;
import com.mongodb.client.MongoDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({StorageMongoProperties.class})
public class StorageRepositoryProviderAutoConfiguration {
    private final StorageMongoProperties properties;

    public StorageRepositoryProviderAutoConfiguration(StorageMongoProperties storageMongoProperties) {
        properties = storageMongoProperties;
    }

    @Bean
    @ConditionalOnMissingBean(MongoDatabase.class)
    public MongoDatabase mongoRepository() {
        return MongoDbUtils.create(properties.getHost(), properties.getDbName());
    }
}