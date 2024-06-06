package com.arextest.storage.service.handler;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.mock.AREXMocker;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.trace.MDCTracer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HandleMockerService extends AbstractAgentWorkingService
    implements AgentWorkingHandler<AREXMocker> {

  public HandleMockerService(AgentWorkingMetricService agentWorkingMetricService,
      CacheProvider redisCacheProvider) {
    super(agentWorkingMetricService, redisCacheProvider);
  }

  @Override
  public boolean batchSave(List<AREXMocker> items) {
    if (CollectionUtils.isEmpty(items)) {
      return false;
    }

    AREXMocker mocker = items.get(0);
    MDCTracer.addRecordId(mocker.getRecordId());
    MDCTracer.addRecordId(mocker.getReplayId());

    boolean batchSaveResult = true;
    try {
      batchSaveResult = doBatchSave(items);
      return batchSaveResult;
    } catch (Exception e) {
      LOGGER.error("batch save record error", e);
      handleBatchSaveException(mocker.getAppId(), mocker.getRecordId(), mocker.getReplayId());
      return false;
    } finally {
      if (!batchSaveResult) {
        handleBatchSaveException(mocker.getAppId(), mocker.getRecordId(), mocker.getReplayId());
      }
      MDCTracer.clear();
    }
  }

  @Override
  public List<AREXMocker> findBy(String recordId, String replayId) {
    return null;
  }

  private boolean doBatchSave(List<AREXMocker> items) {
    if (isInvalidCase(items.get(0).getRecordId())) {
      LOGGER.warn("recordId: {} is invalid", items.get(0).getRecordId());
      return false;
    }

    for (AREXMocker item : items) {
      if (!save(item)) {
        LOGGER.error("batch save record failed, recordId: {}", item.getRecordId());
        return false;
      }
    }

    return true;
  }

  private boolean save(AREXMocker mocker) {
    try {
      MDCTracer.addTrace(mocker);
      if (mocker.getCategoryType() == null || StringUtils.isEmpty(mocker.getCategoryType().getName())) {
        LOGGER.warn("The name of category is empty from agent record save not allowed, recordId: {}",
            mocker.getRecordId());
        return false;
      }

      return agentWorkingMetricService.saveRecord(mocker);
    } catch (Exception e) {
      LOGGER.error("save record error: {} from category: {}, recordId: {}", mocker.getCategoryType(), mocker.getRecordId(), e);
      return false;
    } finally {
      MDCTracer.clear();
    }
  }
}
