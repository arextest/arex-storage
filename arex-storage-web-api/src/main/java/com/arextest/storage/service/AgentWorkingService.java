package com.arextest.storage.service;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.mock.MockResultMatchStrategy;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.model.RecordEnvType;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.RepositoryReader;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.mockerhandlers.MockerHandlerFactory;
import com.arextest.storage.service.mockerhandlers.MockerSaveHandler;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * The agent working should be complete two things: save the origin record and fetch the record
 * result for mocked source when replaying
 *
 * @author jmo
 * @since 2021/11/11
 */
@Slf4j
public class AgentWorkingService {

  private final MockResultProvider mockResultProvider;
  private final RepositoryProviderFactory repositoryProviderFactory;
  private final MockerHandlerFactory mockerHandlerFactory;
  private final List<AgentWorkingListener> agentWorkingListeners;
  @Setter
  private ZstdJacksonSerializer zstdJacksonSerializer;
  @Setter
  private PrepareMockResultService prepareMockResultService;
  @Setter
  private RecordEnvType recordEnvType;

  public AgentWorkingService(MockResultProvider mockResultProvider,
      RepositoryProviderFactory repositoryProviderFactory,
      MockerHandlerFactory mockerHandlerFactory,
      List<AgentWorkingListener> agentWorkingListeners) {
    this.mockResultProvider = mockResultProvider;
    this.repositoryProviderFactory = repositoryProviderFactory;
    this.agentWorkingListeners = agentWorkingListeners;
    this.mockerHandlerFactory = mockerHandlerFactory;
  }

  /**
   * requested from AREX's agent hits to recording, we direct save to repository for next replay
   * using
   *
   * @param item the instance of T
   * @param <T>  class type
   * @return true means success,else save failure
   */
  public <T extends Mocker> boolean saveRecord(@NotNull T item) {
    if (shouldMarkRecordEnv(item.getCategoryType())) {
      item.setRecordEnvironment(recordEnvType.getCodeValue());
    }
    if (!this.dispatchRecordSavingEvent(item)) {
      LOGGER.warn("switch not open, skip save record data");
      return false;
    }

    mockResultProvider.calculateEigen(item);
    MockerSaveHandler<T> handler = mockerHandlerFactory.getHandler(item.getCategoryType());
    if (handler != null) {
      try {
        handler.handle(item);
      } catch (Exception e) {
        LOGGER.error("Mocker handler error", e);
        return false;
      }
      return true;
    }

    RepositoryProvider<T> repositoryWriter = repositoryProviderFactory.defaultProvider();
    return repositoryWriter != null && repositoryWriter.save(item);
  }

  private boolean dispatchRecordSavingEvent(Mocker instance) {
    if (CollectionUtils.isEmpty(this.agentWorkingListeners)) {
      return true;
    }
    for (AgentWorkingListener agentWorkingListener : this.agentWorkingListeners) {
      if (agentWorkingListener.onRecordSaving(instance)) {
        return false;
      }
    }
    return true;
  }

  private boolean dispatchMockResultEnterEvent(Mocker instance, MockResultContext context) {
    if (CollectionUtils.isEmpty(this.agentWorkingListeners)) {
      return true;
    }
    for (AgentWorkingListener agentWorkingListener : this.agentWorkingListeners) {
      if (agentWorkingListener.onRecordMocking(instance, context)) {
        return false;
      }
    }
    return true;
  }


  private boolean shouldMarkRecordEnv(MockCategoryType category) {
    return category.isEntryPoint() ||
        StringUtils.equals(category.getName(), MockCategoryType.CONFIG_FILE.getName());
  }

  /**
   * requested from AREX's agent replaying which should be fetch the mocked resource of dependency.
   * NOTE: This is sequence query from the cached result. if requested times overhead the size of
   * the resource, return the last sequence item. if the requested should be compared by
   * scheduler,we put it to cache for performance goal.
   *
   * @param recordItem from AREX's recording
   * @return compress bytes with zstd from the cached which filled by scheduler's preload
   */
  public <T extends Mocker> byte[] queryMockResult(@NotNull T recordItem,
      MockResultContext context) {
    if (!this.dispatchMockResultEnterEvent(recordItem, context)) {
      LOGGER.warn("switch not open, skip query record data");
      return ZstdJacksonSerializer.EMPTY_INSTANCE;
    }
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
      LOGGER.info("fetch replay mock record empty from cache,record id:{},replay id:{}", recordId,
          replayId);
      boolean reloadResult = prepareMockResultService.preload(category, recordId);
      if (reloadResult) {
        result = mockResultProvider.getRecordResult(recordItem, context);
      }
      if (result == null) {
        if (MockResultMatchStrategy.STRICT_MATCH == context.getMockStrategy()) {
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

  public byte[] queryConfigFile(AREXMocker requestType) {
    RepositoryReader<AREXMocker> repositoryReader = repositoryProviderFactory.defaultProvider();
    if (repositoryReader == null) {
      return ZstdJacksonSerializer.EMPTY_INSTANCE;
    }
    AREXMocker arexMocker = repositoryReader.queryRecord(requestType);
    if (arexMocker != null) {
      return zstdJacksonSerializer.serialize(arexMocker);
    }
    return ZstdJacksonSerializer.EMPTY_INSTANCE;
  }
}