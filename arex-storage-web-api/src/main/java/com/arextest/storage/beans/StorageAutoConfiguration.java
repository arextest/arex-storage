package com.arextest.storage.beans;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.converter.ZstdJacksonMessageConverter;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.metric.MetricListener;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.model.dto.config.application.ApplicationOperationConfiguration;
import com.arextest.storage.repository.ConfigRepositoryProvider;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.impl.mongo.AREXMockerMongoRepositoryProvider;
import com.arextest.storage.repository.impl.mongo.MongoDbUtils;
import com.arextest.storage.repository.impl.mongo.config.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.storage.repository.impl.mongo.config.ApplicationServiceConfigurationRepositoryImpl;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.*;
import com.arextest.storage.web.controller.MockSourceEditionController;
import com.arextest.storage.web.controller.ScheduleReplayQueryController;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;

import lombok.extern.slf4j.Slf4j;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({StorageConfigurationProperties.class})
@Slf4j
public class StorageAutoConfiguration {
    private final StorageConfigurationProperties properties;
    private static final String COLLECTION_SUFFIX = "Mocker";
    static final String EXPIRATION_TIME_COLUMN_NAME = "expirationTime";

    public StorageAutoConfiguration(StorageConfigurationProperties configurationProperties) {
        properties = configurationProperties;
    }

    @Bean
    @ConditionalOnMissingBean(MongoDatabase.class)
    public MongoDatabase mongoDatabase() {
        MongoDatabase database = MongoDbUtils.create(properties.getMongodbUri());
        setTtlIndexes(database);
        ensureMockerQueryIndex(database);
        return database;
    }

    private void ensureMockerQueryIndex(MongoDatabase database) {
        for (MockCategoryType category : MockCategoryType.DEFAULTS) {
            MongoCollection<AREXMocker> collection =
                database.getCollection(getCollectionName(category), AREXMocker.class);
            try {
                Document index = new Document();
                index.append(AREXMocker.Fields.recordId, 1);
                index.append(AREXMocker.Fields.creationTime, -1);
                collection.createIndex(index);
            } catch (MongoCommandException e) {
                LOGGER.info("create index failed for {}", category.getName(), e);
            }

            try {
                Document index = new Document();
                index.append(AREXMocker.Fields.appId, 1);
                index.append(AREXMocker.Fields.operationName, 1);
                collection.createIndex(index);
            } catch (MongoCommandException e) {
                LOGGER.info("create index failed for {}", category.getName(), e);
            }
        }
    }

    private void setTtlIndexes(MongoDatabase mongoDatabase) {
        for (MockCategoryType category : MockCategoryType.DEFAULTS) {
            setTTLIndexInMockerCollection(category, mongoDatabase);
        }
    }

    private void setTTLIndexInMockerCollection(MockCategoryType category, MongoDatabase mongoDatabase) {
        String categoryName = getCollectionName(category);
        MongoCollection<AREXMocker> collection = mongoDatabase.getCollection(categoryName, AREXMocker.class);
        Bson index = new Document(EXPIRATION_TIME_COLUMN_NAME, 1);
        IndexOptions indexOptions = new IndexOptions().expireAfter(0L, TimeUnit.SECONDS);
        try {
            collection.createIndex(index, indexOptions);
        } catch (MongoCommandException e) {
            // ignore
            collection.dropIndex(index);
            collection.createIndex(index, indexOptions);
        }
    }

    private static String getCollectionName(MockCategoryType category) {
        return ProviderNames.DEFAULT + category.getName() + COLLECTION_SUFFIX;
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

    /**
     * used for web api provider how to decode the request before processing. we add a zstd-jackson converter.
     *
     * @return HttpMessageConverters
     */
    @Bean
    @ConditionalOnMissingBean(HttpMessageConverters.class)
    public HttpMessageConverters customConverters(ZstdJacksonMessageConverter zstdJacksonMessageConverter) {
        return new HttpMessageConverters(zstdJacksonMessageConverter);
    }

    @Bean
    @ConditionalOnProperty(prefix = "arex.storage", name = "enableDiscoveryEntryPoint", havingValue = "true")
    public AgentWorkingListener autoDiscoveryEntryPointListener(
        ApplicationServiceConfigurationRepositoryImpl serviceRepository,
        ApplicationOperationConfigurationRepositoryImpl serviceOperationRepository, CacheProvider cacheProvider) {
        return new AutoDiscoveryEntryPointListener(serviceRepository, serviceOperationRepository, cacheProvider);
    }

    @Bean
    @ConditionalOnMissingBean(AgentWorkingService.class)
    public AgentWorkingService agentWorkingService(MockResultProvider mockResultProvider,
        RepositoryProviderFactory repositoryProviderFactory, ZstdJacksonSerializer zstdJacksonSerializer,
        PrepareMockResultService prepareMockResultService, List<AgentWorkingListener> agentWorkingListeners) {
        AgentWorkingService workingService =
            new AgentWorkingService(mockResultProvider, repositoryProviderFactory, agentWorkingListeners);
        workingService.setPrepareMockResultService(prepareMockResultService);
        workingService.setZstdJacksonSerializer(zstdJacksonSerializer);
        workingService.setRecordEnvType(properties.getRecordEnv());
        return workingService;
    }

    @Bean
    @ConditionalOnMissingBean(AgentWorkingMetricService.class)
    public AgentWorkingMetricService agentWorkingMetricService(AgentWorkingService agentWorkingService,
        List<MetricListener> metricListeners) {
        return new AgentWorkingMetricService(agentWorkingService, metricListeners);
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleReplayQueryController.class)
    public ScheduleReplayQueryController scheduleReplayQueryController(
        ScheduleReplayingService scheduleReplayingService, PrepareMockResultService prepareMockResultService) {
        return new ScheduleReplayQueryController(scheduleReplayingService, prepareMockResultService);
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleReplayingService.class)
    public ScheduleReplayingService scheduleReplayingService(MockResultProvider mockResultProvider,
        RepositoryProviderFactory repositoryProviderFactory,
        ConfigRepositoryProvider<ApplicationOperationConfiguration> serviceOperationRepository) {
        return new ScheduleReplayingService(mockResultProvider, repositoryProviderFactory, serviceOperationRepository);
    }

    @Bean
    @ConditionalOnMissingBean(MockSourceEditionController.class)
    public MockSourceEditionController mockSourceEditionController(MockSourceEditionService editableService,
        PrepareMockResultService storageCache) {
        return new MockSourceEditionController(editableService, storageCache);
    }

    @Bean
    @ConditionalOnMissingBean(DesensitizeService.class)
    public DesensitizeService desensitizeService() {
        return new DesensitizeService();
    }

    @Bean
    @Order(2)
    public RepositoryProvider<AREXMocker> pinnedMockerProvider(MongoDatabase mongoDatabase) {
        return new AREXMockerMongoRepositoryProvider(ProviderNames.PINNED, mongoDatabase, properties);
    }

    @Bean
    @Order(1)
    public RepositoryProvider<AREXMocker> defaultMockerProvider(MongoDatabase mongoDatabase) {
        return new AREXMockerMongoRepositoryProvider(mongoDatabase, properties);
    }
}
