package com.arextest.storage.service.mockerhandlers;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.impl.mongo.CoverageRepository;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import com.arextest.storage.repository.scenepool.ScenePoolProviderImpl;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.trace.MDCTracer;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CoverageMockerHandler implements MockerSaveHandler<AREXMocker> {
  @Resource
  private RepositoryProviderFactory repositoryProviderFactory;
  @Resource
  private MockSourceEditionService mockSourceEditionService;
  @Resource
  private CoverageRepository coverageRepository;
  @Resource
  private CacheProvider cacheProvider;
  @Resource
  private ThreadPoolExecutor coverageHandlerExecutor;
  @Resource
  private ScenePoolFactory scenePoolFactory;

  @Override
  public MockCategoryType getMockCategoryType() {
    return MockCategoryType.COVERAGE;
  }

  /**
   * if is auto pined Case: update path
   * <p>
   * if is new case: if it has same path key: replace old case else: store
   */
  @Override
  public void handle(AREXMocker coverageMocker) {
    ScenePoolProvider scenePoolProvider;
    if (StringUtils.isEmpty(coverageMocker.getReplayId())) {
      scenePoolProvider = scenePoolFactory.getProvider(ProviderNames.RECORDING_SCENE_POOL);
      RecordTask task = new RecordTask(scenePoolProvider, coverageMocker);
      coverageHandlerExecutor.submit(task);
    } else {
      scenePoolProvider = scenePoolFactory.getProvider(ProviderNames.REPLAY_SCENE_POOL);
      // todo: implement task for replay phase
    }

    LOGGER.info(
        "CoverageMockerHandler submitted async task, recordId: {}, pathKey: {}, pool queue size: {}",
        coverageMocker.getRecordId(), coverageMocker.getOperationName(),
        coverageHandlerExecutor.getQueue().size());
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
    @Override
    public void run() {
      try {
        String appId = coverageMocker.getAppId();
        String sceneKey = coverageMocker.getOperationName();
        MDCTracer.addAppId(appId);

        // scene exist remove Rolling mocker
        if (scenePoolProvider.checkSceneExist(appId, sceneKey)) {
          mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT, coverageMocker.getRecordId());
          LOGGER.info("CoverageMockerHandler receive exist case, recordId: {}, pathKey: {}",
              coverageMocker.getRecordId(), coverageMocker.getOperationName());
        } else {
          // scene not exist, do nothing
          LOGGER.info("CoverageMockerHandler receive new case, recordId: {}, pathKey: {}",
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
