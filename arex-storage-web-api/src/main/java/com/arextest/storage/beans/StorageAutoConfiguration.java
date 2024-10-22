package com.arextest.storage.beans;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.config.ConfigProvider;
import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.common.config.DefaultConfigProvider;
import com.arextest.common.desensitization.DesensitizationProvider;
import com.arextest.common.jwt.JWTService;
import com.arextest.common.jwt.JWTServiceImpl;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationServiceConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.SystemConfigurationRepositoryImpl;
import com.arextest.config.utils.MongoHelper;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.AREXQueryMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.aspect.AppAuthAspectExecutor;
import com.arextest.storage.converter.ZstdJacksonMessageConverter;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.metric.MatchStrategyMetricService;
import com.arextest.storage.metric.MetricListener;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.mock.MockerResultConverter;
import com.arextest.storage.mock.impl.DefaultMockerResultConverterImpl;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.impl.mongo.AREXMockerMongoRepositoryProvider;
import com.arextest.storage.repository.impl.mongo.AREXQueryMockerMongoRepositoryProvider;
import com.arextest.storage.repository.impl.mongo.converters.ArexEigenCompressionConverter;
import com.arextest.storage.repository.impl.mongo.converters.ArexMockerCompressionConverter;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.AgentWorkingService;
import com.arextest.storage.service.InvalidRecordService;
import com.arextest.storage.service.PrepareMockResultService;
import com.arextest.storage.service.QueryConfigService;
import com.arextest.storage.service.ScenePoolService;
import com.arextest.storage.service.ScheduleReplayingService;
import com.arextest.storage.service.config.ApplicationService;
import com.arextest.storage.service.handler.mocker.coverage.CoverageEventListener;
import com.arextest.storage.service.handler.mocker.coverage.DefaultCoverageEventListener;
import com.arextest.storage.service.listener.AgentWorkingListener;
import com.arextest.storage.service.listener.AutoDiscoveryEntryPointListener;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({StorageConfigurationProperties.class})
@Slf4j
public class StorageAutoConfiguration {

  private final StorageConfigurationProperties properties;

  @Lazy
  @Resource
  IndexesSettingConfiguration indexesSettingConfiguration;

  @Value("${arex.app.auth.switch}")
  private boolean authSwitch;

  private static final long ACCESS_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;
  private static final long REFRESH_EXPIRE_TIME = 30 * 24 * 60 * 60 * 1000L;

  @Value("${arex.jwt.secret:arex}")
  private String tokenSecret;

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

      // todo make this optional
      indexesSettingConfiguration.setIndexes(database);
      syncAuthSwitch(database);
      return factory;
    } catch (Exception e) {
      LOGGER.error("Mongo initialization failed: {}", e.getMessage(), e);
      throw e;
    }
  }

  @Bean
  @ConditionalOnMissingBean(MongoOperations.class)
  MongoTemplate mongoTemplate(MongoDatabaseFactory factory, MongoConverter converter) {
    return new MongoTemplate(factory, converter);
  }

  @Bean
  @ConditionalOnMissingBean(DesensitizationProvider.class)
  DesensitizationProvider desensitizationProvider(MongoDatabaseFactory factory) {
    String desensitizationJarUrl = DataDesensitizationUtils.getDesensitizationJarUrl(
        factory.getMongoDatabase());
    return new DesensitizationProvider(desensitizationJarUrl);
  }

  @Bean
  DataDesensitization dataDesensitization(DesensitizationProvider desensitizationProvider) {
    return desensitizationProvider.get();
  }

  @Bean
  @ConditionalOnMissingBean(MongoCustomConversions.class)
  public MongoCustomConversions customConversions(DataDesensitization dataDesensitization) {
    return MongoCustomConversions.create((adapter) -> {
      // Type based converter
      adapter.registerConverter(new ArexMockerCompressionConverter.Read(dataDesensitization));
      adapter.registerConverter(new ArexMockerCompressionConverter.Write(dataDesensitization));

      // Property based converter
      adapter.configurePropertyConversions((register) -> {
        register.registerConverter(AREXMocker.class, AREXMocker.Fields.eigenMap,
            new ArexEigenCompressionConverter());
      });
    });
  }


  @Bean
  @ConditionalOnMissingBean(MongoConverter.class)
  MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory factory,
      MongoMappingContext context,
      MongoCustomConversions conversions) {
    DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
    MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver, context);
    mappingConverter.setCustomConversions(conversions);
    // Don't save _class to mongo， may cause issues when using polymorphic types
    mappingConverter.setTypeMapper(new DefaultMongoTypeMapper(null));
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
  public AgentWorkingService agentWorkingService(
      MockResultProvider mockResultProvider,
      RepositoryProviderFactory repositoryProviderFactory,
      ZstdJacksonSerializer zstdJacksonSerializer,
      PrepareMockResultService prepareMockResultService,
      List<AgentWorkingListener> agentWorkingListeners,
      InvalidRecordService invalidRecordService,
      ScheduleReplayingService scheduleReplayingService,
      MockerResultConverter mockerResultConverter) {
    AgentWorkingService workingService = new AgentWorkingService(
        mockResultProvider,
        repositoryProviderFactory,
        agentWorkingListeners,
        invalidRecordService,
        scheduleReplayingService,
        mockerResultConverter
    );
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
  @ConditionalOnMissingBean(ScheduleReplayingService.class)
  public ScheduleReplayingService scheduleReplayingService(MockResultProvider mockResultProvider,
      RepositoryProviderFactory repositoryProviderFactory,
      ApplicationOperationConfigurationRepositoryImpl serviceOperationRepository,
      ScenePoolService scenePoolService) {
    return new ScheduleReplayingService(mockResultProvider, repositoryProviderFactory,
        serviceOperationRepository, scenePoolService);
  }

  @Bean
  @ConditionalOnMissingBean(AppAuthAspectExecutor.class)
  public AppAuthAspectExecutor appAuthAspectExecutor(
      ApplicationConfigurationRepositoryImpl applicationConfigurationRepository,
      ApplicationService applicationService,
      SystemConfigurationRepositoryImpl systemConfigurationRepository, JWTService jwtService) {
    return new AppAuthAspectExecutor(applicationConfigurationRepository, applicationService,
        systemConfigurationRepository, jwtService);
  }

  @Bean
  @ConditionalOnMissingBean(JWTService.class)
  public JWTService jwtService() {
    return new JWTServiceImpl(ACCESS_EXPIRE_TIME, REFRESH_EXPIRE_TIME, tokenSecret);
  }

  @Bean
  @ConditionalOnMissingBean(MockerResultConverter.class)
  public MockerResultConverter mockerResultConverter(QueryConfigService queryConfigService,
      DefaultApplicationConfig defaultApplicationConfig) {
    return new DefaultMockerResultConverterImpl(queryConfigService, defaultApplicationConfig);
  }

  @Bean
  @ConditionalOnMissingBean(ConfigProvider.class)
  public ConfigProvider defaultConfigProvider(Environment environment) {
    return new DefaultConfigProvider(environment);
  }

  @Bean
  public DefaultApplicationConfig defaultApplicationConfig(ConfigProvider configProvider) {
    return new DefaultApplicationConfig(configProvider);
  }

  @Bean
  @ConditionalOnMissingBean(CoverageEventListener.class)
  public CoverageEventListener defaultCoverageEventListener() {
    return new DefaultCoverageEventListener();
  }

  @Bean
  @Order(3)
  public RepositoryProvider<AREXMocker> autoPinnedMockerProvider(MongoTemplate mongoTemplate,
      Set<MockCategoryType> entryPointTypes, DefaultApplicationConfig defaultApplicationConfig) {
    return new AREXMockerMongoRepositoryProvider(ProviderNames.AUTO_PINNED, mongoTemplate,
        properties,
        entryPointTypes, defaultApplicationConfig);
  }

  @Bean
  @Order(2)
  public RepositoryProvider<AREXMocker> pinnedMockerProvider(MongoTemplate mongoTemplate,
      Set<MockCategoryType> entryPointTypes, DefaultApplicationConfig defaultApplicationConfig) {
    return new AREXMockerMongoRepositoryProvider(ProviderNames.PINNED, mongoTemplate, properties,
        entryPointTypes, defaultApplicationConfig);
  }

  @Bean
  @Order(1)
  public RepositoryProvider<AREXMocker> defaultMockerProvider(MongoTemplate mongoTemplate,
      Set<MockCategoryType> entryPointTypes, DefaultApplicationConfig defaultApplicationConfig) {
    return new AREXMockerMongoRepositoryProvider(mongoTemplate, properties, entryPointTypes,
        defaultApplicationConfig);
  }

  @Bean
  @Order(4)
  public RepositoryProvider<AREXQueryMocker> defaultQueryMockerProvider(MongoTemplate mongoTemplate,
      Set<MockCategoryType> entryPointTypes, DefaultApplicationConfig defaultApplicationConfig) {
    return new AREXQueryMockerMongoRepositoryProvider(mongoTemplate, properties, entryPointTypes,
        defaultApplicationConfig);
  }

  @Bean
  @Order(5)
  public RepositoryProvider<AREXQueryMocker> pinnedQueryMockerProvider(MongoTemplate mongoTemplate,
      Set<MockCategoryType> entryPointTypes, DefaultApplicationConfig defaultApplicationConfig) {
    return new AREXQueryMockerMongoRepositoryProvider(ProviderNames.PINNED, mongoTemplate,
        properties,
        entryPointTypes, defaultApplicationConfig);
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

  private static class DataDesensitizationUtils {

    private static final String SYSTEM_CONFIGURATION = "SystemConfiguration";
    private static final String DESENSITIZATION_JAR = "desensitizationJar";
    private static final String JAR_URL = "jarUrl";

    private static String getDesensitizationJarUrl(MongoDatabase database) {
      MongoCollection<Document> collection = database.getCollection(SYSTEM_CONFIGURATION);
      Bson filter = Filters.eq(SystemConfigurationCollection.Fields.key,
          KeySummary.DESERIALIZATION_JAR);
      Document document = collection.find(filter).first();
      if (document != null && document.get(DESENSITIZATION_JAR) != null) {
        return document.get(DESENSITIZATION_JAR, Document.class).getString(JAR_URL);
      }
      return null;
    }
  }
}
