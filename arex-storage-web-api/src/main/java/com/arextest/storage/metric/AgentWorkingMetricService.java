package com.arextest.storage.metric;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.model.InvalidIncompleteRecordRequest;
import com.arextest.storage.service.AgentWorkingService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

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
  private static final String SCENE = "scene";
  private static final String SCENE_RECORD = "record";
  private static final String SCENE_REPLAY = "replay";
  public final List<MetricListener> metricListeners;
  private final AgentWorkingService agentWorkingService;

  public AgentWorkingMetricService(AgentWorkingService agentWorkingService,
                                   List<MetricListener> metricListeners) {
    this.agentWorkingService = agentWorkingService;
    this.metricListeners = metricListeners;
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
  public void invalidIncompleteRecord(InvalidIncompleteRecordRequest requestType) {
    // do invalid
    agentWorkingService.invalidIncompleteRecord(requestType);
    // record count
    recordInvalidIncompleteCount(requestType);
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

  public void recordInvalidIncompleteCount(InvalidIncompleteRecordRequest request) {
    // save into redis
    if (CollectionUtils.isEmpty(metricListeners)) {
      return;
    }
    String scene = StringUtils.isNotEmpty(request.getReplayId()) ? SCENE_REPLAY : SCENE_RECORD;
    Map<String, String> tags = new HashMap<>(3);
    tags.put(CLIENT_APP_ID, request.getAppId());
    tags.put(SCENE, scene);
    tags.put(REASON, request.getReason());

    for (MetricListener metricListener : metricListeners) {
      metricListener.recordMatchingCount(METRIC_NAME, tags);
    }
  }
}
