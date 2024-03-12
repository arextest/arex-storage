package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.scenepool.Scene;
import com.arextest.storage.metric.MetricListener;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.trace.MDCTracer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class CoverageMockerHandler implements MockerSaveHandler {
  private MockSourceEditionService mockSourceEditionService;
  private ScheduledExecutorService coverageHandleDelayedPool;
  private ScenePoolFactory scenePoolFactory;
  public final List<MetricListener> metricListeners;
  // coverage metric constants
  private static final String COVERAGE_METRIC_NAME = "coverage.recording";
  private static final String COVERAGE_OP_TAG_KEY = "operation";
  private static final String COVERAGE_APP_TAG_KEY = "clientAppId";


  @Override
  public MockCategoryType getMockCategoryType() {
    return MockCategoryType.COVERAGE;
  }

  private static final String INVALID_SCENE_KEY = "0";

  /**
   * if is auto pined Case: update path
   * <p>
   * if is new case: if it has same path key: replace old case else: store
   */
  @Override
  public void handle(Mocker coverageMocker) {
    ScenePoolProvider scenePoolProvider;
    Runnable task;

    if (StringUtils.isEmpty(coverageMocker.getOperationName())
        || coverageMocker.getOperationName().equals(INVALID_SCENE_KEY)) {
      LOGGER.warn("CoverageMockerHandler got invalid case, operationName is empty, recordId:{}",
          coverageMocker.getRecordId());
      return;
    }

    // if replayId is empty, meaning this coverage mocker is received during record phase
    if (StringUtils.isEmpty(coverageMocker.getReplayId())) {
      scenePoolProvider = scenePoolFactory.getProvider(ScenePoolFactory.RECORDING_SCENE_POOL);
      task = new RecordTask(scenePoolProvider, coverageMocker);
      coverageHandleDelayedPool.schedule(task, 5, TimeUnit.SECONDS);
    } else {
      scenePoolProvider = scenePoolFactory.getProvider(ScenePoolFactory.REPLAY_SCENE_POOL);
      task = new ReplayTask(scenePoolProvider, coverageMocker);
      coverageHandleDelayedPool.schedule(task, 1, TimeUnit.SECONDS);
    }
  }

  /**
   * async task for coverage mocker received during replay phase
   * todo: use this task
   */
  @AllArgsConstructor
  private class ReplayTask implements Runnable {
    private final ScenePoolProvider scenePoolProvider;
    private final Mocker coverageMocker;

    @Override
    public void run() {
      try {
        String recordId = coverageMocker.getRecordId();
        Scene newScene = convert(coverageMocker);
        MDCTracer.addRecordId(recordId);

        // todo: bug here, may insert multiple scene with same key, should be ensure by unique index
        Scene scene = scenePoolProvider.findAndUpdate(newScene);
        // remove old related
        if (scene != null) {
          mockSourceEditionService.removeByRecordId(ProviderNames.AUTO_PINNED, scene.getRecordId());
        }

        // try moving from rolling to AP,
        // if the related case is already AUTO-PINNED, nothing needs to be done
        mockSourceEditionService.moveTo(ProviderNames.DEFAULT, recordId, ProviderNames.AUTO_PINNED);
      } catch (Exception e) {
        LOGGER.error("Error handling replay task for record: {}", coverageMocker.getRecordId());
      } finally {
        MDCTracer.clear();
      }
    }
  }

  /**
   * async task for coverage mocker received during record phase
   */
  @AllArgsConstructor
  private class RecordTask implements Runnable {
    private final ScenePoolProvider scenePoolProvider;
    private final Mocker coverageMocker;
    private static final long EXPIRATION_EXTENSION_DAYS = 14L;
    private static final String NEW_SCENE_OP = "NEW_SCENE";
    private static final String EXISTING_SCENE_OP = "EXISTING_SCENE";

    @Override
    public void run() {
      try {
        String appId = coverageMocker.getAppId();
        String sceneKey = coverageMocker.getOperationName();
        String recordId = coverageMocker.getRecordId();
        String op = NEW_SCENE_OP;
        MDCTracer.addAppId(appId);
        MDCTracer.addRecordId(recordId);

        // scene exist remove Rolling mocker
        if (scenePoolProvider.checkSceneExist(appId, sceneKey)) {
          mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT, coverageMocker.getRecordId());
          LOGGER.info("CoverageMockerHandler received existing case, recordId: {}, pathKey: {}",
              coverageMocker.getRecordId(), coverageMocker.getOperationName());
        } else {
          op = EXISTING_SCENE_OP;
          // new scene: extend mocker expiration and insert scene
          Scene scene = convert(coverageMocker);

          scenePoolProvider.upsertOne(scene);
          mockSourceEditionService.extendMockerExpirationByRecordId(ProviderNames.DEFAULT,
              coverageMocker.getRecordId(), EXPIRATION_EXTENSION_DAYS);
          LOGGER.info("CoverageMockerHandler received new case, recordId: {}, pathKey: {}",
              coverageMocker.getRecordId(), coverageMocker.getOperationName());
        }

        recordCoverageHandle(appId, op);
      } catch (Exception e) {
        LOGGER.error("CoverageMockerHandler handle error", e);
      } finally {
        MDCTracer.clear();
      }
    }
  }

  private Scene convert(Mocker coverageMocker) {
    Scene result = new Scene();
    result.setSceneKey(coverageMocker.getOperationName());
    result.setAppId(coverageMocker.getAppId());
    result.setRecordId(coverageMocker.getRecordId());
    result.setExecutionPath(Optional.ofNullable(coverageMocker.getTargetResponse()).map(
        Target::getBody).orElse(null));
    return result;
  }

  private void recordCoverageHandle(String appId, String op) {
    if (CollectionUtils.isEmpty(metricListeners)) {
      return;
    }
    Map<String, String> tags = new HashMap<>(2);
    tags.put(COVERAGE_APP_TAG_KEY, appId);
    tags.put(COVERAGE_OP_TAG_KEY, op);
    for (MetricListener metricListener : metricListeners) {
      metricListener.recordMatchingCount(COVERAGE_METRIC_NAME, tags);
    }
  }
}
