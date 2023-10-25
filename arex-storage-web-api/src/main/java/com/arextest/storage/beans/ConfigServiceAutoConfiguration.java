package com.arextest.storage.beans;

import com.arextest.config.repository.impl.*;
import com.mongodb.client.MongoDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ConfigServiceAutoConfiguration {

    // the bean about config to register
    @Bean
    public ApplicationConfigurationRepositoryImpl applicationConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
        return new ApplicationConfigurationRepositoryImpl(mongoDatabase);
    }

    @Bean
    public ApplicationServiceConfigurationRepositoryImpl
    applicationServiceConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
        return new ApplicationServiceConfigurationRepositoryImpl(mongoDatabase);
    }

    @Bean
    public ApplicationOperationConfigurationRepositoryImpl
    applicationOperationConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
        return new ApplicationOperationConfigurationRepositoryImpl(mongoDatabase);
    }

    @Bean
    public InstancesConfigurationRepositoryImpl instancesConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
        return new InstancesConfigurationRepositoryImpl(mongoDatabase);
    }

    @Bean
    public ServiceCollectConfigurationRepositoryImpl
    serviceCollectConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
        return new ServiceCollectConfigurationRepositoryImpl(mongoDatabase);
    }

    @Bean
    public DynamicClassConfigurationRepositoryImpl
    dynamicClassConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
        return new DynamicClassConfigurationRepositoryImpl(mongoDatabase);
    }
}
