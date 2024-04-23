package com.arextest.storage.beans;

import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationServiceConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.DynamicClassConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.InstancesConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ServiceCollectConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.SystemConfigurationRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration(proxyBeanMethods = false)
public class ConfigServiceAutoConfiguration {

  // the bean about config to register
  @Bean
  public ApplicationConfigurationRepositoryImpl applicationConfigurationRepositoryImpl(
      MongoTemplate mongoTemplate) {
    return new ApplicationConfigurationRepositoryImpl(mongoTemplate);
  }

  @Bean
  public ApplicationServiceConfigurationRepositoryImpl
  applicationServiceConfigurationRepositoryImpl(MongoTemplate mongoTemplate,
      ApplicationOperationConfigurationRepositoryImpl operationConfigurationRepository) {
    return new ApplicationServiceConfigurationRepositoryImpl(mongoTemplate,
        operationConfigurationRepository);
  }

  @Bean
  public ApplicationOperationConfigurationRepositoryImpl
  applicationOperationConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    return new ApplicationOperationConfigurationRepositoryImpl(mongoTemplate);
  }

  @Bean
  public InstancesConfigurationRepositoryImpl instancesConfigurationRepositoryImpl(
      MongoTemplate mongoTemplate) {
    return new InstancesConfigurationRepositoryImpl(mongoTemplate);
  }

  @Bean
  public ServiceCollectConfigurationRepositoryImpl
  serviceCollectConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    return new ServiceCollectConfigurationRepositoryImpl(mongoTemplate);
  }

  @Bean
  public DynamicClassConfigurationRepositoryImpl
  dynamicClassConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    return new DynamicClassConfigurationRepositoryImpl(mongoTemplate);
  }

  @Bean
  public SystemConfigurationRepositoryImpl systemConfigurationRepositoryImpl(
      MongoTemplate mongoTemplate) {
    return new SystemConfigurationRepositoryImpl(mongoTemplate);
  }
}
