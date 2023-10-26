package com.arextest.storage.beans;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationServiceConfigurationRepositoryImpl;
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
import com.arextest.storage.repository.impl.mongo.AdditionalCodecProviderFactory;
import com.arextest.storage.repository.impl.mongo.AutoPinedMockerRepository;
import com.arextest.storage.repository.impl.mongo.MongoDbUtils;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.AgentWorkingListener;
import com.arextest.storage.service.AgentWorkingService;
import com.arextest.storage.service.AutoDiscoveryEntryPointListener;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.service.PrepareMockResultService;
import com.arextest.storage.service.ScheduleReplayingService;
import com.arextest.storage.service.mockerhandlers.MockerHandlerFactory;
import com.arextest.storage.web.controller.MockSourceEditionController;
import com.arextest.storage.web.controller.ScheduleReplayQueryController;
import com.mongodb.client.MongoDatabase;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({StorageConfigurationProperties.class})
@Slf4j
public class StorageAutoConfiguration {

  private final StorageConfigurationProperties properties;

  @Resource
  IndexsSettingConfiguration indexsSettingConfiguration;

  public StorageAutoConfiguration(StorageConfigurationProperties configurationProperties) {
    properties = configurationProperties;
  }

  @Bean
  @ConditionalOnMissingBean(MongoDatabase.class)
  public MongoDatabase mongoDatabase(
      AdditionalCodecProviderFactory additionalCodecProviderFactory) {
    MongoDatabase database = MongoDbUtils.create(properties.getMongodbUri(),
        additionalCodecProviderFactory);
    indexsSettingConfiguration.setTtlIndexes(database);
    indexsSettingConfiguration.ensureMockerQueryIndex(database);
    indexsSettingConfiguration.setIndexAboutConfig(database);
    return database;
  }

  @Bean
  @ConditionalOnMissingBean(AdditionalCodecProviderFactory.class)
  public AdditionalCodecProviderFactory additionalCodecProviderFactory() {
    return new AdditionalCodecProviderFactory();
  }

  @Bean
  @ConditionalOnMissingBean(CacheProvider.class)
  public CacheProvider cacheProvider() {
    if (StringUtils.isEmpty(properties.getCache().getUri())) {
      return new DefaultRedisCacheProvider();
    }
    return new DefaultRedisCacheProvider(properties.getCache().getUri());
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
      MockerHandlerFactory mockerHandlerFactory,
      ZstdJacksonSerializer zstdJacksonSerializer,
      PrepareMockResultService prepareMockResultService,
      List<AgentWorkingListener> agentWorkingListeners) {
    AgentWorkingService workingService =
        new AgentWorkingService(mockResultProvider, repositoryProviderFactory, mockerHandlerFactory,
            agentWorkingListeners);
    workingService.setPrepareMockResultService(prepareMockResultService);
    workingService.setZstdJacksonSerializer(zstdJacksonSerializer);
    workingService.setRecordEnvType(properties.getRecordEnv());
    return workingService;
  }

  @Bean
  @ConditionalOnMissingBean(AgentWorkingMetricService.class)
  public AgentWorkingMetricService agentWorkingMetricService(
      AgentWorkingService agentWorkingService,
      List<MetricListener> metricListeners) {
    return new AgentWorkingMetricService(agentWorkingService, metricListeners);
  }

  @Bean
  @ConditionalOnMissingBean(ScheduleReplayQueryController.class)
  public ScheduleReplayQueryController scheduleReplayQueryController(
      ScheduleReplayingService scheduleReplayingService,
      PrepareMockResultService prepareMockResultService) {
    return new ScheduleReplayQueryController(scheduleReplayingService, prepareMockResultService);
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
      PrepareMockResultService prepareMockResultService) {
    return new ScheduleReplayQueryController(scheduleReplayingService, prepareMockResultService);
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
      MockSourceEditionService editableService,
      PrepareMockResultService storageCache) {
    return new MockSourceEditionController(editableService, storageCache);
  }

  @Bean
  @Order(3)
  public RepositoryProvider<AREXMocker> autoPinnedMockerProvider(MongoDatabase mongoDatabase,
      Set<MockCategoryType> entryPointTypes) {
    return new AutoPinedMockerRepository(mongoDatabase, properties, entryPointTypes);
  }

  @Bean
  @Order(2)
  public RepositoryProvider<AREXMocker> pinnedMockerProvider(MongoDatabase mongoDatabase,
      Set<MockCategoryType> entryPointTypes) {
    return new AREXMockerMongoRepositoryProvider(ProviderNames.PINNED, mongoDatabase, properties,
        entryPointTypes);
  }

  @Bean
  @Order(1)
  public RepositoryProvider<AREXMocker> defaultMockerProvider(MongoDatabase mongoDatabase,
      Set<MockCategoryType> entryPointTypes) {
    return new AREXMockerMongoRepositoryProvider(mongoDatabase, properties, entryPointTypes);
  }
}
