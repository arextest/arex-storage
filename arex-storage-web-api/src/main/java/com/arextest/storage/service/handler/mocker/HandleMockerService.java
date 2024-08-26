package com.arextest.storage.service.handler.mocker;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.mock.AREXMocker;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.service.InvalidRecordService;
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

  private static final String TITLE = "[[title=handleMocker]]";

  public HandleMockerService(AgentWorkingMetricService agentWorkingMetricService,
      CacheProvider redisCacheProvider, InvalidRecordService invalidRecordService) {
    super(agentWorkingMetricService, redisCacheProvider, invalidRecordService);
  }

  @Override
  public boolean save(AREXMocker mocker) {
    try {
      if (isInvalidCase(mocker.getRecordId())) {
        LOGGER.warn("{}recordId: {} is invalid", TITLE, mocker.getRecordId());
        return false;
      }

      if (mocker.getCategoryType() == null || StringUtils.isEmpty(mocker.getCategoryType().getName())) {
        LOGGER.warn("{}The name of category is empty from agent record save not allowed, "
            + "recordId: {}", TITLE, mocker.getRecordId());
        return false;
      }

      return agentWorkingMetricService.saveRecord(mocker);
    } catch (Exception e) {
      LOGGER.error("{}save record error: {} from category: {}, recordId: {}",
          TITLE, mocker.getCategoryType(), mocker.getRecordId(), e);
      return false;
    }
  }

  @Override
  public boolean batchSave(List<AREXMocker> items) {
    if (CollectionUtils.isEmpty(items)) {
      return false;
    }

    AREXMocker mocker = items.get(0);
    try {
      MDCTracer.addTrace(mocker);
      if (isInvalidCase(items.get(0).getRecordId())) {
        LOGGER.warn("{}recordId: {} is invalid", TITLE, items.get(0).getRecordId());
        return false;
      }

      return agentWorkingMetricService.batchSaveRecord(items);
    } catch (Exception e) {
      LOGGER.error("{}batch save record error", TITLE, e);
      handleBatchSaveException(mocker.getAppId(), mocker.getRecordId(), mocker.getReplayId());
      return false;
    } finally {
      MDCTracer.clear();
    }
  }

}
