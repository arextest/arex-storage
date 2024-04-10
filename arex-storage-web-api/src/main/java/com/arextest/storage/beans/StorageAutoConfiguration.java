package com.arextest.storage.beans;

import com.arextest.common.cache.CacheProvider;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationServiceConfigurationRepositoryImpl;
import com.arextest.config.utils.MongoHelper;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.converter.ZstdJacksonMessageConverter;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.metric.MatchStrategyMetricService;
import com.arextest.storage.metric.MetricListener;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.impl.mongo.AREXMockerMongoRepositoryProvider;
import com.arextest.storage.repository.impl.mongo.converters.ArexMockerCompressionConverter;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.AgentWorkingListener;
import com.arextest.storage.service.AgentWorkingService;
import com.arextest.storage.service.AutoDiscoveryEntryPointListener;
import com.arextest.storage.service.InvalidIncompleteRecordService;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.service.PrepareMockResultService;
import com.arextest.storage.service.ScheduleReplayingService;
import com.arextest.storage.service.mockerhandlers.MockerHandlerFactory;
import com.arextest.storage.web.controller.MockSourceEditionController;
import com.arextest.storage.web.controller.ScheduleReplayQueryController;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({StorageConfigurationProperties.class})
@Slf4j
public class StorageAutoConfiguration {

  private final StorageConfigurationProperties properties;

  @Resource
  IndexesSettingConfiguration indexesSettingConfiguration;

  @Value("${arex.app.auth.switch}")
  private boolean authSwitch;

  public StorageAutoConfiguration(StorageConfigurationProperties configurationProperties) {
    properties = configurationProperties;
  }

  @Bean
  @ConditionalOnMissingBean
  public MongoDatabaseFactory mongoDbFactory() {
    try {
      SimpleMongoClientDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(
          properties.getMongodbUri());
      MongoDatabase database = factory.getMongoDatabase();
      indexesSettingConfiguration.setIndexes(database);
      syncAuthSwitch(database);
      return factory;
    } catch (Exception e) {
      LOGGER.error("cannot connect mongodb {}", e.getMessage(), e);
      throw e;
    }
  }

  @Bean
  @ConditionalOnMissingBean(MongoOperations.class)
  MongoTemplate mongoTemplate(MongoDatabaseFactory factory, MongoConverter converter) {
    return new MongoTemplate(factory, converter);
  }

  @Bean
  public MongoCustomConversions customConversions() {
    return MongoCustomConversions.create((adapter) -> {
      adapter.registerConverter(new ArexMockerCompressionConverter.Read());
      adapter.registerConverter(new ArexMockerCompressionConverter.Write());
    });
  }


  @Bean
  @ConditionalOnMissingBean(MongoConverter.class)
  MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory factory, MongoMappingContext context,
      MongoCustomConversions conversions) {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
    MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver, context);
    mappingConverter.setCustomConversions(conversions);
    return mappingConverter;
  }

  @Bean
  public Set<MockCategoryType> categoryTypes() {
    Set<MockCategoryType> customCategoryTypes = properties.getCategoryTypes();
    if (CollectionUtils.isEmpty(customCategoryTypes)) {
      return MockCategoryType.DEFAULTS;
    }
    customCategoryTypes.addAll(MockCategoryType.DEFAULTS);
    return customCategoryTypes;
  }

  @Bean
  public Set<MockCategoryType> entryPointTypes() {
    Set<MockCategoryType> entryPoints = new LinkedHashSet<>();
    Set<MockCategoryType> customCategoryTypes = properties.getCategoryTypes();
    // order matters here, we want all custom entry points to be added first
    if (CollectionUtils.isNotEmpty(customCategoryTypes)) {
      customCategoryTypes.stream().filter(MockCategoryType::isEntryPoint).forEach(entryPoints::add);
    }
    MockCategoryType.DEFAULTS.stream().filter(MockCategoryType::isEntryPoint)
        .forEach(entryPoints::add);
    return entryPoints;
  }

  /**
   * used for web api provider how to decode the request before processing. we add a zstd-jackson
   * converter.
   *
   * @return HttpMessageConverters
   */
  @Bean
  @ConditionalOnMissingBean(HttpMessageConverters.class)
  public HttpMessageConverters customConverters(
      ZstdJacksonMessageConverter zstdJacksonMessageConverter) {
    return new HttpMessageConverters(zstdJacksonMessageConverter);
  }

  @Bean
  @ConditionalOnProperty(prefix = "arex.storage", name = "enableDiscoveryEntryPoint", havingValue = "true")
  public AgentWorkingListener autoDiscoveryEntryPointListener(
      ApplicationServiceConfigurationRepositoryImpl serviceRepository,
      ApplicationOperationConfigurationRepositoryImpl serviceOperationRepository,
      CacheProvider cacheProvider) {
    return new AutoDiscoveryEntryPointListener(serviceRepository, serviceOperationRepository,
        cacheProvider);
  }

  @Bean
  @ConditionalOnMissingBean(AgentWorkingService.class)
  public AgentWorkingService agentWorkingService(MockResultProvider mockResultProvider,
      RepositoryProviderFactory repositoryProviderFactory,
      MockerHandlerFactory mockerHandlerFactory, ZstdJacksonSerializer zstdJacksonSerializer,
      PrepareMockResultService prepareMockResultService,
      List<AgentWorkingListener> agentWorkingListeners,
      InvalidIncompleteRecordService invalidIncompleteRecordService) {
    AgentWorkingService workingService = new AgentWorkingService(mockResultProvider,
        repositoryProviderFactory, mockerHandlerFactory, agentWorkingListeners,
        invalidIncompleteRecordService);
    workingService.setPrepareMockResultService(prepareMockResultService);
    workingService.setZstdJacksonSerializer(zstdJacksonSerializer);
    workingService.setRecordEnvType(properties.getRecordEnv());
    return workingService;
  }

  @Bean
  @ConditionalOnMissingBean(AgentWorkingMetricService.class)
  public AgentWorkingMetricService agentWorkingMetricService(
      AgentWorkingService agentWorkingService, List<MetricListener> metricListeners) {
    return new AgentWorkingMetricService(agentWorkingService, metricListeners);
  }

  @Bean
  @ConditionalOnMissingBean(MatchStrategyMetricService.class)
  public MatchStrategyMetricService matchStrategyMetricService(
      List<MetricListener> metricListeners) {
    return new MatchStrategyMetricService(metricListeners);
  }

  @Bean
  @ConditionalOnMissingBean(ScheduleReplayQueryController.class)
  public ScheduleReplayQueryController scheduleReplayQueryController(
      ScheduleReplayingService scheduleReplayingService,
      PrepareMockResultService prepareMockResultService,
      InvalidIncompleteRecordService invalidIncompleteRecordService) {
    return new ScheduleReplayQueryController(scheduleReplayingService, prepareMockResultService,
        invalidIncompleteRecordService);
  }

  @Bean
  @ConditionalOnMissingBean(ScheduleReplayingService.class)
  public ScheduleReplayingService scheduleReplayingService(MockResultProvider mockResultProvider,
      RepositoryProviderFactory repositoryProviderFactory,
      ApplicationOperationConfigurationRepositoryImpl serviceOperationRepository) {
    return new ScheduleReplayingService(mockResultProvider, repositoryProviderFactory,
        serviceOperationRepository);
  }

  @Bean
  @ConditionalOnMissingBean(MockSourceEditionController.class)
  public MockSourceEditionController mockSourceEditionController(
      MockSourceEditionService editableService, PrepareMockResultService storageCache) {
    return new MockSourceEditionController(editableService, storageCache);
  }

  //  @Bean
  //  @Order(3)
  //  public RepositoryProvider<AREXMocker> autoPinnedMockerProvider(MongoTemplate mongoTemplate,
  //      Set<MockCategoryType> entryPointTypes) {
  //    return new AutoPinedMockerRepository(mongoTemplate, properties, entryPointTypes);
  //  }

  @Bean
  @Order(2)
  public RepositoryProvider<AREXMocker> pinnedMockerProvider(MongoTemplate mongoTemplate,
      Set<MockCategoryType> entryPointTypes) {
    return new AREXMockerMongoRepositoryProvider(ProviderNames.PINNED, mongoTemplate, properties,
        entryPointTypes);
  }

  @Bean
  @Order(1)
  public RepositoryProvider<AREXMocker> defaultMockerProvider(MongoTemplate mongoTemplate,
      Set<MockCategoryType> entryPointTypes) {
    return new AREXMockerMongoRepositoryProvider(mongoTemplate, properties, entryPointTypes);
  }

  private void syncAuthSwitch(MongoDatabase database) {
    try {
      MongoCollection<SystemConfigurationCollection> mongoCollection = database.getCollection(
          SystemConfigurationCollection.DOCUMENT_NAME, SystemConfigurationCollection.class);
      Bson filter = Filters.eq(SystemConfigurationCollection.Fields.key, KeySummary.AUTH_SWITCH);

      List<Bson> updateList = Arrays.asList(
          Updates.set(SystemConfigurationCollection.Fields.authSwitch, authSwitch),
          MongoHelper.getUpdate()
      );

      Bson updateCombine = Updates.combine(updateList);
      mongoCollection.updateOne(filter, updateCombine, new UpdateOptions().upsert(true))
          .getModifiedCount();
    } catch (Exception e) {
      LOGGER.error("sync auth switch failed", e);
    }
  }
}
