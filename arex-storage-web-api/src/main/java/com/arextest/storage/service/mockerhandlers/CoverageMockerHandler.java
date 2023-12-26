package com.arextest.storage.service.mockerhandlers;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.scenepool.Scene;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.impl.mongo.CoverageRepository;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import com.arextest.storage.repository.scenepool.ScenePoolProviderImpl;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.trace.MDCTracer;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class CoverageMockerHandler implements MockerSaveHandler<AREXMocker> {
  private RepositoryProviderFactory repositoryProviderFactory;
  private MockSourceEditionService mockSourceEditionService;
  private CoverageRepository coverageRepository;
  private CacheProvider cacheProvider;
  private ScheduledExecutorService coverageHandleDelayedPool;
  private ScenePoolFactory scenePoolFactory;

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
  public void handle(AREXMocker coverageMocker) {
    ScenePoolProvider scenePoolProvider;
    Runnable task = null;

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
    } else {
      scenePoolProvider = scenePoolFactory.getProvider(ScenePoolFactory.REPLAY_SCENE_POOL);
      // todo: implement task for replay phase
    }

    Optional.ofNullable(task).ifPresent((t) -> {
      coverageHandleDelayedPool.schedule(t, 5, TimeUnit.SECONDS);
      LOGGER.info(
          "CoverageMockerHandler submitted async task, recordId: {}, pathKey: {}",
          coverageMocker.getRecordId(), coverageMocker.getOperationName());
    });
  }

  /**
   * async task for coverage mocker received during replay phase
   * todo: use this task
   */
  @AllArgsConstructor
  private class ReplayTask implements Runnable {
    private final ScenePoolProviderImpl scenePoolProvider;
    private final AREXMocker coverageMocker;

    @Override
    public void run() {
      MDCTracer.addRecordId(coverageMocker.getRecordId());
      MDCTracer.addReplayId(coverageMocker.getReplayId());
      try {
        if (StringUtils.isEmpty(coverageMocker.getOperationName())
            || coverageMocker.getOperationName().equals("0")) {
          LOGGER.warn("CoverageMockerHandler handle error, operationName is empty, recordId:{}",
              coverageMocker.getRecordId());

          // getting operationName(Coverage key) as 0 but having recordId, meaning this is an extremely simple and meaningless case, remove it
          mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT,
              coverageMocker.getRecordId());
          return;
        }
        final RepositoryProvider<Mocker> pinedProvider = repositoryProviderFactory.findProvider(
            ProviderNames.AUTO_PINNED);
        assert pinedProvider != null;

        String incomingCaseId = coverageMocker.getRecordId();
        Mocker pinned = pinedProvider.findEntryFromAllType(incomingCaseId);
        // Mocker rolling = rollingProvider.findEntryFromAllType(newCaseId);

        if (pinned != null) {
          coverageRepository.updatePathByRecordId(incomingCaseId, coverageMocker);
          LOGGER.info("CoverageMockerHandler handle update, recordId:{}, pathKey: {}",
              incomingCaseId, coverageMocker.getOperationName());
        } else {
          boolean locked = cacheProvider.putIfAbsent(
              (coverageMocker.getAppId() + coverageMocker.getOperationName()).getBytes(),
              60 * 24 * 12L,
              coverageMocker.getRecordId().getBytes());

          if (locked) {
            transferEntry(coverageMocker, incomingCaseId);
            LOGGER.info("CoverageMockerHandler handle transfer, recordId:{}, pathKey: {}",
                incomingCaseId, coverageMocker.getOperationName());
          } else {
            mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT, incomingCaseId);
            LOGGER.info("CoverageMockerHandler handle remove, recordId:{}, pathKey: {}",
                incomingCaseId, coverageMocker.getOperationName());
          }
        }
      } catch (Exception e) {
        LOGGER.error("CoverageMockerHandler handle error", e);
      } finally {
        MDCTracer.clear();
      }
    }

    private void transferEntry(AREXMocker coverageMocker, String incomingCaseId) {
      Mocker oldCoverageMocker = coverageRepository.upsertOne(coverageMocker);
      // there is an existing AutoPinnedMocker with the same key, delete the related AutoPinnedMocker
      if (oldCoverageMocker != null) {
        String oldCaseId = oldCoverageMocker.getRecordId();
        boolean removed = mockSourceEditionService.removeByRecordId(ProviderNames.AUTO_PINNED,
            oldCaseId);
        if (!removed) {
          LOGGER.error("remove old auto pinned failed, caseId:{}", oldCaseId);
        }
      }

      // move entry to auto pinned
      boolean moved = mockSourceEditionService.moveTo(ProviderNames.DEFAULT, incomingCaseId,
          ProviderNames.AUTO_PINNED);
      if (!moved) {
        LOGGER.error("move entry to auto pinned failed, caseId:{}", incomingCaseId);
      }
    }
  }

  /**
   * async task for coverage mocker received during record phase
   */
  @AllArgsConstructor
  private class RecordTask implements Runnable {
    private final ScenePoolProvider scenePoolProvider;
    private final AREXMocker coverageMocker;
    private static final long EXPIRATION_EXTENSION_DAYS = 14L;
    @Override
    public void run() {
      try {
        String appId = coverageMocker.getAppId();
        String sceneKey = coverageMocker.getOperationName();
        String recordId = coverageMocker.getRecordId();
        String executionPath = Optional.ofNullable(coverageMocker.getTargetResponse()).map(
            Target::getBody).orElse(null);

        MDCTracer.addAppId(appId);
        MDCTracer.addRecordId(recordId);

        // scene exist remove Rolling mocker
        if (scenePoolProvider.checkSceneExist(appId, sceneKey)) {
          mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT, coverageMocker.getRecordId());
          LOGGER.info("CoverageMockerHandler received existing case, recordId: {}, pathKey: {}",
              coverageMocker.getRecordId(), coverageMocker.getOperationName());
        } else {
          // new scene: extend mocker expiration and insert scene
          Scene scene = new Scene();
          scene.setSceneKey(sceneKey);
          scene.setAppId(appId);
          scene.setRecordId(recordId);
          scene.setExecutionPath(executionPath);

          scenePoolProvider.upsertOne(scene);
          mockSourceEditionService.extendMockerExpirationByRecordId(ProviderNames.DEFAULT,
              coverageMocker.getRecordId(), EXPIRATION_EXTENSION_DAYS);
          LOGGER.info("CoverageMockerHandler received new case, recordId: {}, pathKey: {}",
              coverageMocker.getRecordId(), coverageMocker.getOperationName());
        }
      } catch (Exception e) {
        LOGGER.error("CoverageMockerHandler handle error", e);
      } finally {
        MDCTracer.clear();
      }
    }
  }
}
