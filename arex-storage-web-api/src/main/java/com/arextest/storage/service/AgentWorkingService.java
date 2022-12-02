package com.arextest.storage.service;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.mock.MockResultMatchStrategy;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.model.RecordEnvType;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The agent working should be complete two things:
 * save the origin record and fetch the record result for mocked source when replaying
 *
 * @author jmo
 * @since 2021/11/11
 */
@Slf4j
public final class AgentWorkingService {
    private final MockResultProvider mockResultProvider;
    private final RepositoryProviderFactory repositoryProviderFactory;
    @Setter
    private ZstdJacksonSerializer zstdJacksonSerializer;
    @Setter
    private PrepareMockResultService prepareMockResultService;

    private final List<AgentWorkingListener> agentWorkingListeners;

    @Setter
    private RecordEnvType recordEnvType;

    public AgentWorkingService(MockResultProvider mockResultProvider,
                               RepositoryProviderFactory repositoryProviderFactory, List<AgentWorkingListener> agentWorkingListeners) {
        this.mockResultProvider = mockResultProvider;
        this.repositoryProviderFactory = repositoryProviderFactory;
        this.agentWorkingListeners = agentWorkingListeners;
    }

    public AgentWorkingService(MockResultProvider mockResultProvider,
                               RepositoryProviderFactory repositoryProviderFactory) {
        this(mockResultProvider, repositoryProviderFactory, null);
    }

    /**
     * requested from AREX's agent hits to recording, we direct save to repository for next replay using
     *
     * @param item the instance of T
     * @param <T>  class type
     * @return true means success,else save failure
     */
    public <T extends Mocker> boolean saveRecord(@NotNull T item) {
        if (shouldMarkRecordEnv(item.getCategoryType())) {
            item.setRecordEnvironment(recordEnvType.getCodeValue());
        }
        this.dispatchRecordSavingEvent(item);
        RepositoryProvider<T> repositoryWriter = repositoryProviderFactory.defaultProvider();

        return repositoryWriter != null && repositoryWriter.save(item);
    }

    private void dispatchRecordSavingEvent(Mocker instance) {
        if (CollectionUtils.isEmpty(this.agentWorkingListeners)) {
            return;
        }
        for (AgentWorkingListener agentWorkingListener : this.agentWorkingListeners) {
            agentWorkingListener.onRecordSaving(instance);
        }
    }

    private void dispatchMockResultEnterEvent(Mocker instance, MockResultContext context) {
        if (CollectionUtils.isEmpty(this.agentWorkingListeners)) {
            return;
        }
        for (AgentWorkingListener agentWorkingListener : this.agentWorkingListeners) {
            agentWorkingListener.onRecordMocking(instance, context);
        }
    }

    private boolean shouldMarkRecordEnv(MockCategoryType category) {
        return category.isEntryPoint() ||
                StringUtils.equals(category.getName(), MockCategoryType.CONFIG_FILE.getName());
    }

    /**
     * requested from AREX's agent replaying which should be fetch the mocked resource of dependency.
     * NOTE:
     * This is sequence query from the cached result.
     * if requested times overhead the size of the resource, return the last sequence item.
     * if the requested should be compared by scheduler,we put it to cache for performance goal.
     *
     * @param recordItem from AREX's recording
     * @return compress bytes with zstd from the cached which filled by scheduler's preload
     */
    public <T extends Mocker> byte[] queryMockResult(@NotNull T recordItem, MockResultContext context) {
        this.dispatchMockResultEnterEvent(recordItem, context);
        String recordId = recordItem.getRecordId();
        String replayId = recordItem.getReplayId();
        MockCategoryType category = recordItem.getCategoryType();
        if (category.isEntryPoint()) {
            mockResultProvider.putReplayResult(recordItem);
            LOGGER.info("skip main entry mock response,record id:{},replay id:{}", recordId, replayId);
            return zstdJacksonSerializer.serialize(recordItem);
        }
        byte[] result = mockResultProvider.getRecordResult(recordItem, context);
        if (result == null) {
            LOGGER.info("fetch replay mock record empty from cache,record id:{},replay id:{}", recordId, replayId);
            boolean reloadResult = prepareMockResultService.preload(category, recordId);
            if (reloadResult) {
                result = mockResultProvider.getRecordResult(recordItem, context);
            }
            if (result == null) {
                if (MockResultMatchStrategy.BREAK_RECORDED_COUNT == context.getMockStrategy() && context.isLastOfResult()) {
                    return ZstdJacksonSerializer.EMPTY_INSTANCE;
                }
                mockResultProvider.putReplayResult(recordItem);
                LOGGER.info("reload fetch replay mock record empty from cache,record id:{},replay id:{}, " +
                                "reloadResult:{}",
                        recordId,
                        replayId, reloadResult);
                return ZstdJacksonSerializer.EMPTY_INSTANCE;
            }
        }
        mockResultProvider.putReplayResult(recordItem);
        LOGGER.info("agent query found result for category:{},record id: {},replay id: {}", category,
                recordId, replayId);
        return result;
    }
}