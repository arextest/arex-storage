package com.arextest.storage.core.service;

import com.arextest.common.cache.CacheProvider;
import com.arextest.storage.core.cache.CacheKeyUtils;
import com.arextest.storage.core.mock.MockResultProvider;
import com.arextest.storage.core.repository.RepositoryProvider;
import com.arextest.storage.core.repository.RepositoryProviderFactory;
import com.arextest.storage.core.repository.RepositoryReader;
import com.arextest.storage.core.repository.ServiceOperationRepository;
import com.arextest.storage.core.repository.ServiceRepository;
import com.arextest.storage.core.serialization.ZstdJacksonSerializer;
import com.arextest.storage.model.dao.ServiceEntity;
import com.arextest.storage.model.dao.ServiceOperationEntity;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.ConfigVersion;
import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.model.mocker.impl.ConfigVersionMocker;
import com.arextest.storage.model.mocker.impl.ServletMocker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * The agent working should be complete two things:
 * save the origin record and fetch the record result for mocked source when replaying
 *
 * @author jmo
 * @since 2021/11/11
 */
@Slf4j
@Service
public final class AgentWorkingService {
    @Resource
    private MockResultProvider mockResultProvider;
    @Resource
    private RepositoryProviderFactory repositoryProviderFactory;
    @Resource
    private ZstdJacksonSerializer zstdJacksonSerializer;
    @Resource
    private PrepareMockResultService prepareMockResultService;
    @Resource
    private RecordReplayMappingBuilder recordReplayMappingBuilder;
    @Resource
    private ServiceRepository serviceRepository;
    @Resource
    private ServiceOperationRepository serviceOperationRepository;
    @Resource
    private CacheProvider cacheProvider;
    @Value("${arex.storage.enable-auto-discovery-main-entry:true}")
    private boolean enableAutoDiscoveryMainEntry;
    private static final String DASH = "_";
    private static final int SERVICE_TYPE_NORMAL = 4;
    private static final String SERVICE_MAPPINGS_PREFIX = "service_mappings_";
    private static final byte[] EMPTY_BYTE_ARRAY = CacheKeyUtils.toUtf8Bytes(StringUtils.EMPTY);

    /**
     * requested from AREX's agent hits to recording, we direct save to repository for next replay using
     *
     * @param category the resource type of requested
     * @param item     the instance of T
     * @param <T>      class type
     * @return true means success,else save failure
     */
    public <T extends MockItem> boolean saveRecord(@NotNull MockCategoryType category, @NotNull T item) {
        RepositoryProvider<T> repositoryWriter = repositoryProviderFactory.findProvider(category);
        if (enableAutoDiscoveryMainEntry) {
            updateMapping(category, item);
        }
        return repositoryWriter != null && repositoryWriter.save(item);
    }

    /**
     * requested from AREX's agent replaying which should be fetch the mocked resource of dependency.
     * NOTE:
     * This is sequence query from the cached result.
     * if requested times overhead the size of the resource, return the last sequence item.
     * if the requested should be compared by scheduler,we put it to cache for performance goal.
     *
     * @param category   the resource type of requested
     * @param recordItem from AREX's recording
     * @return compress bytes with zstd from the cached which filled by scheduler's preload
     */
    public <T extends MockItem> byte[] queryMockResult(@NotNull MockCategoryType category, @NotNull T recordItem) {
        mockResultProvider.putReplayResult(category, recordItem.getReplayId(), recordItem);
        String recordId = recordItem.getRecordId();
        String replayId = recordItem.getReplayId();
        if (category.isMainEntry()) {
            LOGGER.info("skip main entry mock response,record id:{},replay id:{}", recordId, replayId);
            if (category == MockCategoryType.QMQ_CONSUMER) {
                recordReplayMappingBuilder.putLastReplayResultId(category, recordId, replayId);
            }
            return zstdJacksonSerializer.serialize(recordItem);
        }
        byte[] result = mockResultProvider.getRecordResult(category, recordItem);
        if (result == null) {
            LOGGER.info("fetch replay mock record empty from cache,record id:{},replay id:{}", recordId, replayId);
            boolean reloadResult = prepareMockResultService.preload(category, recordId);
            if (reloadResult) {
                result = mockResultProvider.getRecordResult(category, recordItem);
            }
            if (result == null) {
                LOGGER.info("reload fetch replay mock record empty from cache,record id:{},replay id:{}, " +
                                "reloadResult:{}",
                        recordId,
                        replayId, reloadResult);
                return ZstdJacksonSerializer.EMPTY_INSTANCE;
            }
        }
        LOGGER.info("agent query found result for category:{},record id: {},replay id: {}", category.getDisplayName(),
                recordId, replayId);
        return result;
    }

    public byte[] queryConfigVersion(MockCategoryType category, ConfigVersion version) {
        RepositoryReader<?> repositoryReader = repositoryProviderFactory.findProvider(category);
        Object value = null;
        if (repositoryReader != null) {
            value = repositoryReader.queryByVersion(version);
        }
        return zstdJacksonSerializer.serialize(value);
    }

    /**
     * build a key for all config files before agent recording,then save any config resources with the key, after
     * that, it used to replay restore as part of mock dependency.
     *
     * @param application the request app
     * @return version object of ConfigVersionMocker
     * @see ConfigVersionMocker
     */
    public byte[] queryConfigVersionKey(ConfigVersion application) {
        if (StringUtils.isEmpty(application.getAppId())) {
            LOGGER.warn("The appId is empty from request application");
            return ZstdJacksonSerializer.EMPTY_INSTANCE;
        }
        return queryConfigVersion(MockCategoryType.CONFIG_VERSION, application);
    }

    private <T extends MockItem> void updateMapping(@NotNull MockCategoryType category, @NotNull T item) {
        if (item.getClass() == ServletMocker.class) {
            ServletMocker servlet = (ServletMocker) item;

            byte[] appServiceKey = CacheKeyUtils.toUtf8Bytes(SERVICE_MAPPINGS_PREFIX + servlet.getAppId());
            if (cacheProvider.get(appServiceKey) == null) {
                ServiceEntity serviceEntity = serviceRepository.queryByAppId(servlet.getAppId());
                if (serviceEntity == null) {
                    LOGGER.info("AppId:{} does not have a valid service", servlet.getAppId());
                    return;
                } else {
                    cacheProvider.put(appServiceKey, CacheKeyUtils.toUtf8Bytes(serviceEntity.getId().toString()));
                }
            }

            String serviceId = CacheKeyUtils.fromUtf8Bytes(cacheProvider.get(appServiceKey));
            byte[] operationKey =
                    CacheKeyUtils.toUtf8Bytes(SERVICE_MAPPINGS_PREFIX + serviceId + DASH + servlet.getPattern());
            if (cacheProvider.get(operationKey) != null) {
                return;
            }
            ServiceOperationEntity operationEntity = new ServiceOperationEntity();
            operationEntity.setAppId(servlet.getAppId());
            operationEntity.setOperationName(servlet.getPattern());
            operationEntity.setOperationType(category.getCodeValue());
            operationEntity.setServiceId(serviceId);
            operationEntity.setStatus(SERVICE_TYPE_NORMAL);
            if (serviceOperationRepository.findAndUpdate(operationEntity)) {
                cacheProvider.put(operationKey, EMPTY_BYTE_ARRAY);
            }
        }
    }
}