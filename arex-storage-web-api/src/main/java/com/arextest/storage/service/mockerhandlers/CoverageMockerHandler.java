package com.arextest.storage.service.mockerhandlers;

import com.arextest.common.cache.CacheProvider;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.impl.mongo.CoverageRepository;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.trace.MDCTracer;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Resource;
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
    CoverageTask task = new CoverageTask(coverageMocker);
    coverageHandlerExecutor.submit(task);
    LOGGER.info(
        "CoverageMockerHandler handle submit, recordId:{}, pathKey: {}, pool queue size: {}",
        coverageMocker.getRecordId(), coverageMocker.getOperationName(),
        coverageHandlerExecutor.getQueue().size());
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

  private class CoverageTask implements Runnable {

    private final AREXMocker coverageMocker;

    CoverageTask(AREXMocker coverageMocker) {
      this.coverageMocker = coverageMocker;
    }

    @Override
    public void run() {
      if (StringUtils.isEmpty(coverageMocker.getOperationName())
          || coverageMocker.getOperationName().equals("0")) {
        LOGGER.warn("CoverageMockerHandler handle error, operationName is empty, recordId:{}",
            coverageMocker.getRecordId());
        if (!StringUtils.isEmpty(coverageMocker.getRecordId())) {
          // getting operationName(Coverage key) as 0 but having recordId, meaning this is an extremely simple and meaningless case, remove it
          mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT,
              coverageMocker.getRecordId());
        }
        return;
      }
      MDCTracer.addRecordId(coverageMocker.getRecordId());
      try {
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
      }
    }
  }
}
