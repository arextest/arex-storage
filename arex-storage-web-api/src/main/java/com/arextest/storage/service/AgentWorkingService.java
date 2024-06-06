package com.arextest.storage.service;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.mock.MockResultMatchStrategy;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.mock.MockerResultConverter;
import com.arextest.storage.model.InvalidIncompleteRecordRequest;
import com.arextest.storage.model.RecordEnvType;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.RepositoryReader;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.listener.AgentWorkingListener;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AgentWorkingService {

  private final MockResultProvider mockResultProvider;
  private final RepositoryProviderFactory repositoryProviderFactory;
  private final List<AgentWorkingListener> agentWorkingListeners;
  private final InvalidRecordService invalidRecordService;
  private final ScheduleReplayingService scheduleReplayingService;
  private final MockerResultConverter mockerResultConverter;

  @Setter
  private ZstdJacksonSerializer zstdJacksonSerializer;
  @Setter
  private PrepareMockResultService prepareMockResultService;
  @Setter
  private RecordEnvType recordEnvType;

  /**
   * requested from AREX's agent hits to recording, we direct save to repository for next replay
   * using
   *
   * @param item the instance of T
   * @param <T>  class type
   * @return true means success,else save failure
   */
  public <T extends Mocker> boolean saveRecord(@NotNull T item) {
    if (!prepareAndDispatchRecord(item)) {
      return false;
    }

    RepositoryProvider<T> repositoryWriter = repositoryProviderFactory.defaultProvider();
    return repositoryWriter != null && repositoryWriter.save(item);
  }

  public <T extends Mocker> boolean batchSaveRecord(@NotNull List<T> list) {
    if (CollectionUtils.isEmpty(list)) {
      return false;
    }

    for (T item : list) {
      if (!prepareAndDispatchRecord(item)) {
        return false;
      }
    }

    RepositoryProvider<T> repositoryWriter = repositoryProviderFactory.defaultProvider();
    return repositoryWriter != null && repositoryWriter.saveList(list);
  }

  private <T extends Mocker> boolean prepareAndDispatchRecord(T item) {
    if (shouldMarkRecordEnv(item.getCategoryType())) {
      item.setRecordEnvironment(recordEnvType.getCodeValue());
    }

    if (!this.dispatchRecordSavingEvent(item)) {
      return false;
    }

    mockResultProvider.calculateEigen(item, true);
    return true;
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
      LOGGER.warn("dispatch record mock event failed, skip query record data");
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

  public byte[] queryMockers(String recordId, String[] fieldNames, String[] categoryList) {
    List<RepositoryProvider<? extends Mocker>> repositoryProviderList = repositoryProviderFactory.getRepositoryProviderList();
    Set<MockCategoryType> categoryTypes = repositoryProviderFactory.getCategoryTypesByName(
        categoryList);

    // find data in the order of rolling -> pinned -> auto pinned
    for (RepositoryProvider<? extends Mocker> repositoryReader : repositoryProviderList) {
      List<AREXMocker> mockers = scheduleReplayingService.queryRecordsByRepositoryReader(recordId,
          categoryTypes, repositoryReader, fieldNames);
      if (CollectionUtils.isNotEmpty(mockers)) {
        convertMockers(mockers);
        return zstdJacksonSerializer.serialize(mockers);
      }
    }

    return ZstdJacksonSerializer.EMPTY_INSTANCE_LIST;
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

  public void invalidIncompleteRecord(InvalidIncompleteRecordRequest request) {
    invalidRecordService.invalidIncompleteRecord(request.getRecordId(), request.getReplayId());
  }

  // convert mockers by mockerResultConverter
  private void convertMockers(List<AREXMocker> mockers) {
    for (int i = 0; i < mockers.size(); i++) {
      AREXMocker mocker = mockers.get(i);
      AREXMocker convertedMocker = mockerResultConverter.convert(mocker.getCategoryType(), mocker);
      if (convertedMocker != null) {
        mockers.set(i, convertedMocker);
      }
    }
  }

}
