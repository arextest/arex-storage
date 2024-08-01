package com.arextest.storage.service.handler.mocker.coverage;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.cache.LockWrapper;
import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.model.replay.CaseSendScene;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.CaseStatusEnum;
import com.arextest.model.scenepool.Scene;
import com.arextest.storage.metric.MetricListener;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import com.arextest.storage.service.InvalidRecordService;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.service.UpdateCaseStatusService;
import com.arextest.storage.service.handler.mocker.MockerHandler;
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
public class CoverageMockerHandler implements MockerHandler {
  private MockSourceEditionService mockSourceEditionService;
  private ScheduledExecutorService coverageHandleDelayedPool;
  private ScenePoolFactory scenePoolFactory;
  private UpdateCaseStatusService updateCaseStatusService;
  private CoverageHandlerSwitch handlerSwitch;
  private InvalidRecordService invalidRecordService;
  private DefaultApplicationConfig defaultApplicationConfig;
  private CacheProvider cacheProvider;
  public final List<MetricListener> metricListeners;
  // coverage metric constants
  private static final String METRIC_NAME_RECORD_COVERAGE = "coverage.recording";
  private static final String METRIC_NAME_REPLAY_COVERAGE = "coverage.replay";
  private static final String TAG_KEY_COVERAGE_OP = "operation";
  private static final String TAG_KEY_COVERAGE_APP = "clientAppId";
  private static final String INVALID_SCENE_KEY = "0";
  private static final String TITLE_REPLAY_TASK = "[[title=replayTask]]";
  private static final String TITLE_RECORD_TASK = "[[title=recordTask]]";
  private static final String UNDERLINE_SLASH = "_";
  private static final String REPLAY_LOCK_WAIT_TIME = "coverage.replay.lock.wait.millis";
  private static final String REPLAY_LOCK_LEASE_TIME = "coverage.replay.lock.lease.millis";
  private static final String LOCK_FAILURE_DELETE_AUTO_PINNED_EXISTING_CASE =
      "lock.failure.delete.auto.pinned.existing.case";
  private static final String EXISTING_SCENE_OP = "EXISTING_SCENE";
  private static final String[] DEFAULT_PROVIDER_NAMES = new String[]{ProviderNames.AUTO_PINNED, ProviderNames.DEFAULT};
  @Override
  public MockCategoryType getMockCategoryType() {
    return MockCategoryType.COVERAGE;
  }

  /**
   * if is auto pined Case: update path
   * if is new case: if it has same path key: replace old case else: store
   */
  @Override
  public void handleOnRecordSaving(Mocker coverageMocker) {
    ScenePoolProvider scenePoolProvider;
    Runnable task;
    String appId = coverageMocker.getAppId();
    Optional<Target> targetRequest = Optional.ofNullable(coverageMocker.getTargetRequest());

    if (!validate(coverageMocker) || skipTask(coverageMocker)) {
      return;
    }

    // passed by schedule
    String scheduleSendScene = targetRequest
        .map(i -> i.attributeAsString(MockAttributeNames.SCHEDULE_PARAM))
        .orElse(null);

    // if replayId is empty, meaning this coverage mocker is received during record phase
    if (StringUtils.isEmpty(coverageMocker.getReplayId()) && handlerSwitch.allowRecordTask(appId)) {
      scenePoolProvider = scenePoolFactory.getProvider(ScenePoolFactory.RECORDING_SCENE_POOL);
      task = new RecordTask(scenePoolProvider, coverageMocker);
      coverageHandleDelayedPool.schedule(task, 5, TimeUnit.SECONDS);

    } else if (CaseSendScene.MIXED_NORMAL.name().equals(scheduleSendScene) &&
        handlerSwitch.allowReplayTask(appId)) {
      scenePoolProvider = scenePoolFactory.getProvider(ScenePoolFactory.REPLAY_SCENE_POOL);
      task = new ReplayTask(scenePoolProvider, coverageMocker);
      coverageHandleDelayedPool.schedule(task, 1, TimeUnit.SECONDS);
    }
  }

  private boolean skipTask(Mocker coverageMocker) {
    boolean forceRecord = Optional.ofNullable(coverageMocker.getTargetRequest())
        .map(i -> Boolean.parseBoolean(i.attributeAsString(MockAttributeNames.FORCE_RECORD)))
        .orElse(false);

    // force record data insert to RollingCoverageMocker, skip the scene pool
    if (forceRecord) {
      mockSourceEditionService.add(ProviderNames.DEFAULT, coverageMocker);
      LOGGER.info("CoverageMockerHandler received force record case, recordId: {}, pathKey: {}",
          coverageMocker.getRecordId(), coverageMocker.getOperationName());
      return true;
    }
    return false;
  }

  private static boolean validate(Mocker coverageMocker) {
    boolean checkResult = StringUtils.isNotEmpty(coverageMocker.getOperationName())
        && StringUtils.isNotEmpty(coverageMocker.getAppId())
        && !coverageMocker.getOperationName().equals(INVALID_SCENE_KEY);
    if (!checkResult) {
      LOGGER.warn("CoverageMockerHandler got invalid case, sceneKey is empty, recordId:{}",
          coverageMocker.getRecordId());
    }
    return checkResult;
  }

  @Override
  public boolean isContinue() {
    return false;
  }

  /**
   * async task for coverage mocker received during replay phase
   */
  @AllArgsConstructor
  private class ReplayTask implements Runnable {
    private final ScenePoolProvider scenePoolProvider;
    private final Mocker coverageMocker;

    @Override
    public void run() {
      MDCTracer.addRecordId(coverageMocker.getRecordId());
      MDCTracer.addAppId(coverageMocker.getAppId());
      String lockKey = buildCoverageReplayLockKey(coverageMocker.getAppId(),
          coverageMocker.getOperationName());
      LockWrapper lockWrapper = null;
      boolean locked = false;
      try {
        lockWrapper = tryGetLock(lockKey);
        locked = lockWrapper != null;
        if (locked) {
          long startTimeNanos = System.nanoTime();
          deduplicatedReplayCase();
          recordCoverageTime(coverageMocker.getAppId(),
              TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos));
        } else {
          handleLockFailure();
        }
      } catch (Exception e) {
        LOGGER.error("{}get lock interrupted, record: {}, {}", TITLE_REPLAY_TASK,
            coverageMocker.getRecordId(), e.getMessage(), e);
      } finally {
        if (locked) {
          lockWrapper.unlock();
        }
        MDCTracer.clear();
      }
    }

    private LockWrapper tryGetLock(String lockKey) throws Exception {
      LockWrapper lockWrapper = cacheProvider.getLock(lockKey);
      long waitTime = getLockWaitTime();
      long leaseTime = getLockLeaseTime();
      if (lockWrapper != null && lockWrapper.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS)) {
        return lockWrapper;
      }
      return null;
    }

    private void handleLockFailure() {
      LOGGER.warn("{}Failed to get lock record: {}, sceneKey: {}", TITLE_REPLAY_TASK,
          coverageMocker.getRecordId(), coverageMocker.getOperationName());

      boolean allowDeletingAutoPinnedCase =
          defaultApplicationConfig.getConfigAsBoolean(LOCK_FAILURE_DELETE_AUTO_PINNED_EXISTING_CASE, false);
      // allowDeletingAutoPinnedCase: false, only delete all rolling cases
      for (String providerName : DEFAULT_PROVIDER_NAMES) {
        if ((allowDeletingAutoPinnedCase ||
            StringUtils.equalsIgnoreCase(providerName, ProviderNames.DEFAULT)) &&
            mockSourceEditionService.removeByRecordId(providerName, coverageMocker.getRecordId())) {
          break;
        }
      }

      updateCaseStatusService.updateStatusOfCase(coverageMocker.getRecordId(), CaseStatusEnum.DEDUPLICATED.getCode());
      recordCoverageHandle(coverageMocker.getAppId(), EXISTING_SCENE_OP, METRIC_NAME_REPLAY_COVERAGE);
    }

    private void recordCoverageTime(String appId, long cost) {
      if (CollectionUtils.isEmpty(metricListeners)) {
        return;
      }
      Map<String, String> tags = new HashMap<>(1);
      tags.put(TAG_KEY_COVERAGE_APP, appId);
      for (MetricListener metricListener : metricListeners) {
        metricListener.recordTime(METRIC_NAME_REPLAY_COVERAGE, tags, cost);
      }
    }

    private String buildCoverageReplayLockKey(String appId, String operationName) {
      return appId + UNDERLINE_SLASH + operationName;
    }

    private long getLockWaitTime() {
      return defaultApplicationConfig.getConfigAsLong(REPLAY_LOCK_WAIT_TIME, 400L);
    }

    private long getLockLeaseTime() {
      return defaultApplicationConfig.getConfigAsLong(REPLAY_LOCK_LEASE_TIME, 1000L);
    }

    private void deduplicatedReplayCase() {
      try {
        String recordId = coverageMocker.getRecordId();
        Scene newScene = convert(coverageMocker);
        LOGGER.info("{}removed by case start, recordId: {}, sceneKey: {}",
            TITLE_REPLAY_TASK, recordId, newScene.getSceneKey());
        Scene oldScene = scenePoolProvider.findAndUpdate(newScene);
        // remove old related
        if (oldScene != null) {
          // special case that may happen when manually debugging one case repeatedly
          if (recordId.equals(oldScene.getRecordId())) {
            return;
          }
          LOGGER.info("{}removed by case: {}, old RecordId: {}, sceneKey: {}",
              TITLE_REPLAY_TASK, recordId, oldScene.getRecordId(), newScene.getSceneKey());
          boolean removeResult =
              mockSourceEditionService.removeByRecordId(ProviderNames.AUTO_PINNED, oldScene.getRecordId());
          if (removeResult) {
            updateCaseStatusService.updateStatusOfCase(oldScene.getRecordId(), CaseStatusEnum.DEDUPLICATED.getCode());
          }
        }

        // try moving from rolling to AP,
        // if the related case is already AUTO-PINNED, nothing needs to be done
        int movedCount = mockSourceEditionService.moveTo(ProviderNames.DEFAULT, recordId,
            ProviderNames.AUTO_PINNED);

        // todo should not happen, will handle case by case
        if (movedCount == 0) {
          LOGGER.warn("{}Case {}, failed to transfer case to AUTO-PINNED", TITLE_REPLAY_TASK, recordId);
        }
      } catch (Exception e) {
        LOGGER.error("{}Error handling replay task for record: {}", TITLE_REPLAY_TASK, coverageMocker.getRecordId(), e);
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

    private static final long COVERAGE_EXPIRATION_DAYS = 14L;
    private static final String COVERAGE_EXPIRATION_DAYS_KEY = "coverage.expiration.days";
    private static final String NEW_SCENE_OP = "NEW_SCENE";

    @Override
    public void run() {
      try {
        String appId = coverageMocker.getAppId();
        String sceneKey = coverageMocker.getOperationName();
        String recordId = coverageMocker.getRecordId();
        String op = EXISTING_SCENE_OP;
        MDCTracer.addAppId(appId);
        MDCTracer.addRecordId(recordId);

        // scene exist remove Rolling mocker
        if (scenePoolProvider.checkSceneExist(appId, sceneKey)) {
          invalidRecordService.putInvalidCaseInRedis(recordId);
          mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT, coverageMocker.getRecordId());
          LOGGER.info("{}CoverageMockerHandler received existing case, recordId: {}, pathKey: {}",
              TITLE_RECORD_TASK, coverageMocker.getRecordId(), coverageMocker.getOperationName());
        } else {
          op = NEW_SCENE_OP;
          // new scene: extend mocker expiration and insert scene
          Scene scene = convert(coverageMocker);

          scenePoolProvider.upsertOne(scene);
          mockSourceEditionService.extendMockerExpirationByRecordId(ProviderNames.DEFAULT,
              coverageMocker.getRecordId(),
              defaultApplicationConfig.getConfigAsLong(COVERAGE_EXPIRATION_DAYS_KEY, COVERAGE_EXPIRATION_DAYS));
          LOGGER.info("{}CoverageMockerHandler received new case, recordId: {}, pathKey: {}",
              TITLE_RECORD_TASK, coverageMocker.getRecordId(), coverageMocker.getOperationName());
        }

        recordCoverageHandle(appId, op, METRIC_NAME_RECORD_COVERAGE);
      } catch (Exception e) {
        LOGGER.error("{}CoverageMockerHandler handle error", TITLE_RECORD_TASK, e);
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

  private void recordCoverageHandle(String appId, String op, String metricName) {
    if (CollectionUtils.isEmpty(metricListeners)) {
      return;
    }
    Map<String, String> tags = new HashMap<>(2);
    tags.put(TAG_KEY_COVERAGE_APP, appId);
    tags.put(TAG_KEY_COVERAGE_OP, op);
    for (MetricListener metricListener : metricListeners) {
      metricListener.recordMatchingCount(metricName, tags);
    }
  }
}
