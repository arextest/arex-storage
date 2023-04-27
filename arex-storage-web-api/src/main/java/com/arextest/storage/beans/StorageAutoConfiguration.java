package com.arextest.storage.beans;


import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.DefaultRedisCacheProvider;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.converter.ZstdJacksonMessageConverter;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.ServiceOperationRepository;
import com.arextest.storage.repository.ServiceRepository;
import com.arextest.storage.repository.impl.mongo.AREXMockerMongoRepositoryProvider;
import com.arextest.storage.repository.impl.mongo.MongoDbUtils;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.*;
import com.arextest.storage.web.controller.AgentRecordingController;
import com.arextest.storage.web.controller.MockSourceEditionController;
import com.arextest.storage.web.controller.ScheduleReplayQueryController;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({StorageConfigurationProperties.class})
public class StorageAutoConfiguration {
    private final StorageConfigurationProperties properties;

    public StorageAutoConfiguration(StorageConfigurationProperties configurationProperties) {
        properties = configurationProperties;
    }

    @Bean
    @ConditionalOnMissingBean(MongoDatabase.class)
    public MongoDatabase mongoDatabase() {
        return MongoDbUtils.create(properties.getMongodbUri());
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
     * used for web api provider how to decode the request before processing.
     * we add a zstd-jackson converter.
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
    public AgentWorkingListener autoDiscoveryEntryPointListener(ServiceRepository serviceRepository,
                                                                ServiceOperationRepository serviceOperationRepository,
                                                                CacheProvider cacheProvider) {
        return new AutoDiscoveryEntryPointListener(serviceRepository, serviceOperationRepository, cacheProvider);
    }

    @Bean
    @ConditionalOnMissingBean(AgentWorkingService.class)
    public AgentWorkingService agentWorkingService(
            MockResultProvider mockResultProvider,
            RepositoryProviderFactory repositoryProviderFactory,
            ZstdJacksonSerializer zstdJacksonSerializer,
            PrepareMockResultService prepareMockResultService,
            List<AgentWorkingListener> agentWorkingListeners) {
        AgentWorkingService workingService = new AgentWorkingService(mockResultProvider, repositoryProviderFactory, agentWorkingListeners);
        workingService.setPrepareMockResultService(prepareMockResultService);
        workingService.setZstdJacksonSerializer(zstdJacksonSerializer);
        workingService.setRecordEnvType(properties.getRecordEnv());
        return workingService;
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleReplayQueryController.class)
    public ScheduleReplayQueryController scheduleReplayQueryController(ScheduleReplayingService scheduleReplayingService,
                                                                       PrepareMockResultService prepareMockResultService) {
        return new ScheduleReplayQueryController(scheduleReplayingService, prepareMockResultService);
    }
    @Bean
    @ConditionalOnMissingBean(AgentRecordingController.class)
    public AgentRecordingController scheduleReplayQueryController(AgentWorkingService agentWorkingService) {
        return new AgentRecordingController(agentWorkingService);
    }

    @Bean
    @ConditionalOnMissingBean(ScheduleReplayingService.class)
    public ScheduleReplayingService scheduleReplayingService(MockResultProvider mockResultProvider,
                                                             RepositoryProviderFactory repositoryProviderFactory) {
        return new ScheduleReplayingService(mockResultProvider, repositoryProviderFactory);
    }

    @Bean
    @ConditionalOnMissingBean(MockSourceEditionController.class)
    public MockSourceEditionController mockSourceEditionController(MockSourceEditionService editableService,
                                                                   PrepareMockResultService storageCache) {
        return new MockSourceEditionController(editableService, storageCache);
    }

    @Bean
    @Order(2)
    public RepositoryProvider<AREXMocker> pinnedMockerProvider(MongoDatabase mongoDatabase) {
        return new AREXMockerMongoRepositoryProvider(ProviderNames.PINNED, mongoDatabase);
    }

    @Bean
    @Order(1)
    public RepositoryProvider<AREXMocker> defaultMockerProvider(MongoDatabase mongoDatabase) {
        return new AREXMockerMongoRepositoryProvider(mongoDatabase);
    }
}