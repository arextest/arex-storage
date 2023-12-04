package com.arextest.storage.metric;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.model.InvalidCaseRequest;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.service.AgentWorkingService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

import com.arextest.storage.service.InvalidReplayCaseService;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.trace.MDCTracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * created by xinyuan_wang on 2023/6/7
 */
@Slf4j
public class AgentWorkingMetricService {

  private static final String QUERY_MOCK_METHOD_NAME = "query";
  private static final String SAVE_MOCK_METHOD_NAME = "save";
  private static final String METRIC_NAME = "service.entry.request";
  private static final String CLIENT_APP_ID = "clientAppId";
  private static final String PATH = "path";
  private static final String CATEGORY = "category";

  private static final String REASON = "reason";
  private static final String INVALID_CASE_METHOD_NAME = "invalidCase";
  private static final String INVALID_REPLAY_CASE_METHOD_NAME = "invalidReplayCase";
  public final List<MetricListener> metricListeners;
  private final AgentWorkingService agentWorkingService;
  private final MockSourceEditionService editableService;
  private final InvalidReplayCaseService invalidReplayCaseService;

  public AgentWorkingMetricService(AgentWorkingService agentWorkingService,
                                   MockSourceEditionService editableService,
      List<MetricListener> metricListeners, InvalidReplayCaseService invalidReplayCaseService) {
    this.agentWorkingService = agentWorkingService;
    this.editableService = editableService;
    this.metricListeners = metricListeners;
    this.invalidReplayCaseService = invalidReplayCaseService;
  }

  private static long nanosToMillis(long duration) {
    return TimeUnit.NANOSECONDS.toMillis(duration);
  }

  public <T extends Mocker> boolean saveRecord(@NotNull T item) {
    if (CollectionUtils.isEmpty(metricListeners)) {
      return agentWorkingService.saveRecord(item);
    }

    long startTimeNanos = System.nanoTime();
    boolean saveResult = agentWorkingService.saveRecord(item);
    long totalTimeNanos = System.nanoTime() - startTimeNanos;

    recordEntryTime(SAVE_MOCK_METHOD_NAME, (AREXMocker) item, nanosToMillis(totalTimeNanos));
    return saveResult;
  }

  public <T extends Mocker> byte[] queryMockResult(@NotNull T recordItem,
      MockResultContext context) {
    if (CollectionUtils.isEmpty(metricListeners)) {
      return agentWorkingService.queryMockResult(recordItem, context);
    }

    long startTimeNanos = System.nanoTime();
    byte[] queryMockResult = agentWorkingService.queryMockResult(recordItem, context);
    long totalTimeNanos = System.nanoTime() - startTimeNanos;

    recordEntryTime(QUERY_MOCK_METHOD_NAME, (AREXMocker) recordItem, nanosToMillis(totalTimeNanos));
    return queryMockResult;
  }

  public void invalidCase(InvalidCaseRequest requestType) {
    // replayId is not empty, means this is a replay case
    if (StringUtils.isNotEmpty(requestType.getReplayId())) {
        invalidReplayCase(requestType);
        return;
    }
    // recordId is not empty, means this is a record case
    editableService.invalidCase(ProviderNames.DEFAULT, requestType.getRecordId());
    if (CollectionUtils.isEmpty(metricListeners)) {
      return;
    }
    Map<String, String> tags = new HashMap<>(4);
    tags.put(CLIENT_APP_ID, requestType.getAppId());
    tags.put(PATH, INVALID_CASE_METHOD_NAME);
    tags.put(REASON, requestType.getReason());

    for (MetricListener metricListener : metricListeners) {
      metricListener.recordMatchingCount(METRIC_NAME, tags);
    }
  }

  private void recordEntryTime(String path, AREXMocker item, long timeMillis) {
    Map<String, String> tags = new HashMap<>(5);
    tags.put(CLIENT_APP_ID, item.getAppId());
    tags.put(PATH, path);
    tags.put(CATEGORY, item.getCategoryType().getName());

    for (MetricListener metricListener : metricListeners) {
      metricListener.recordTime(METRIC_NAME, tags, timeMillis);
    }
  }

  private void invalidReplayCase(InvalidCaseRequest request) {
    // save into redis
    String replayId = request.getReplayId();
    MDCTracer.addReplayId(replayId);
    invalidReplayCaseService.saveInvalidCase(replayId);
    LOGGER.info("[[title=invalidReplayCase]]invalid replayId:{}", replayId);
    MDCTracer.clear();
    // metric
    if (CollectionUtils.isEmpty(metricListeners)) {
      return;
    }
    Map<String, String> tags = new HashMap<>(3);
    tags.put(CLIENT_APP_ID, request.getAppId());
    tags.put(PATH, INVALID_REPLAY_CASE_METHOD_NAME);
    tags.put(REASON, request.getReason());

    for (MetricListener metricListener : metricListeners) {
      metricListener.recordMatchingCount(METRIC_NAME, tags);
    }
  }

}
