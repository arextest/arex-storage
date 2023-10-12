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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import javax.annotation.Resource;
import java.util.concurrent.*;

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
     * 自动固化的Case:
     *      更新数据
     *
     * 新记录的Case:
     *      判断是否存在相同Key，相同则删除原自动固化Case，更新为新Case（每次回放只更新一次），不存在则自动固化
     */
    @Override
    public void handle(AREXMocker coverageMocker) {
        CoverageTask task = new CoverageTask(coverageMocker);
        coverageHandlerExecutor.submit(task);
        LOGGER.info("CoverageMockerHandler handle submit, recordId:{}, pathKey: {}, pool queue size: {}",
                coverageMocker.getRecordId(), coverageMocker.getOperationName(), coverageHandlerExecutor.getQueue().size());
    }

    private void transferEntry(AREXMocker coverageMocker, String incomingCaseId) {
        Mocker oldCoverageMocker = coverageRepository.upsertOne(coverageMocker);
        // there is an existing AutoPinnedMocker with the same key, delete the related AutoPinnedMocker
        if (oldCoverageMocker != null) {
            String oldCaseId = oldCoverageMocker.getRecordId();
            boolean removed = mockSourceEditionService.removeByRecordId(ProviderNames.AUTO_PINNED, oldCaseId);
            if (!removed) {
                LOGGER.error("remove old auto pinned failed, caseId:{}", oldCaseId);
            }
        }

        // move entry to auto pinned
        boolean moved = mockSourceEditionService.moveTo(ProviderNames.DEFAULT, incomingCaseId, ProviderNames.AUTO_PINNED);
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
            if (StringUtils.isEmpty(coverageMocker.getOperationName()) || coverageMocker.getOperationName().equals("0")) {
                LOGGER.warn("CoverageMockerHandler handle error, operationName is empty, recordId:{}", coverageMocker.getRecordId());
                if (!StringUtils.isEmpty(coverageMocker.getRecordId())) {
                    // getting operationName(Coverage key) as 0 but having recordId, meaning this is an extremely simple and meaningless case, remove it
                    mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT, coverageMocker.getRecordId());
                }
                return;
            }
            try {
                final RepositoryProvider<Mocker> pinedProvider = repositoryProviderFactory.findProvider(ProviderNames.AUTO_PINNED);
                assert pinedProvider != null;

                String incomingCaseId = coverageMocker.getRecordId();
                Mocker pinned = pinedProvider.findEntryFromAllType(incomingCaseId);
                // Mocker rolling = rollingProvider.findEntryFromAllType(newCaseId);

                if (pinned != null) {
                    coverageRepository.updatePathByRecordId(incomingCaseId, coverageMocker);
                    LOGGER.info("CoverageMockerHandler handle update, recordId:{}, pathKey: {}", incomingCaseId, coverageMocker.getOperationName());
                } else {
                    boolean locked = cacheProvider.putIfAbsent((coverageMocker.getAppId() + coverageMocker.getOperationName()).getBytes(),
                            60 * 24 * 12L,
                            coverageMocker.getRecordId().getBytes());

                    if (locked) {
                        transferEntry(coverageMocker, incomingCaseId);
                        LOGGER.info("CoverageMockerHandler handle transfer, recordId:{}, pathKey: {}", incomingCaseId, coverageMocker.getOperationName());
                    } else {
                        mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT, incomingCaseId);
                        LOGGER.info("CoverageMockerHandler handle remove, recordId:{}, pathKey: {}", incomingCaseId, coverageMocker.getOperationName());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("CoverageMockerHandler handle error", e);
            }
        }
    }

}
