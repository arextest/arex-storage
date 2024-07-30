package com.arextest.storage.service.handler;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.mock.AREXMocker;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.service.InvalidRecordService;
import com.arextest.storage.trace.MDCTracer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
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
  public boolean batchSave(List<AREXMocker> items) {
    if (CollectionUtils.isEmpty(items)) {
      return false;
    }

    AREXMocker mocker = items.get(0);
    boolean batchSaveResult = true;
    try {
      MDCTracer.addTrace(mocker);

      batchSaveResult = doBatchSave(items);
      return batchSaveResult;
    } catch (Exception e) {
      LOGGER.error("{}batch save record error", TITLE, e);
      handleBatchSaveException(mocker.getAppId(), mocker.getRecordId(), mocker.getReplayId());
      return false;
    } finally {
      if (!batchSaveResult) {
        handleBatchSaveException(mocker.getAppId(), mocker.getRecordId(), mocker.getReplayId());
      }
      MDCTracer.clear();
    }
  }

  private boolean doBatchSave(List<AREXMocker> items) {
    if (isInvalidCase(items.get(0).getRecordId())) {
      LOGGER.warn("{}recordId: {} is invalid", TITLE, items.get(0).getRecordId());
      return false;
    }

    Map<String, List<AREXMocker>> groupedItems = items.stream()
        .collect(Collectors.groupingBy(item -> item.getCategoryType().getName()));

    boolean batchSaveResult = true;
    for (Entry<String, List<AREXMocker>> entry : groupedItems.entrySet()) {
      String categoryName = entry.getKey();
      if (!agentWorkingMetricService.batchSaveRecord(entry.getValue())) {
        batchSaveResult = false;
      }
      LOGGER.info("{}batch save record success, category: {}, recordId: {}, count: {}",
          TITLE, categoryName, items.get(0).getRecordId(), entry.getValue().size());
    }

    return batchSaveResult;
  }

  @Override
  public boolean save(AREXMocker mocker) {
    try {
      MDCTracer.addTrace(mocker);
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
    } finally {
      MDCTracer.clear();
    }
  }
}
