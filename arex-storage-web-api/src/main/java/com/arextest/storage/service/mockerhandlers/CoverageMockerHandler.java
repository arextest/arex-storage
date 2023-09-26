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

import javax.annotation.Resource;

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
        // todo
        if (StringUtils.isEmpty(coverageMocker.getOperationName()) || coverageMocker.getOperationName().equals("0")) {
            LOGGER.warn("CoverageMockerHandler handle error, operationName is empty, recordId:{}", coverageMocker.getRecordId());
            return;
        }
        try {
            final RepositoryProvider<Mocker> pinedProvider = repositoryProviderFactory.findProvider(ProviderNames.AUTO_PINNED);
            assert pinedProvider != null;

            String incomingCaseId = coverageMocker.getRecordId();
            Mocker pinned = pinedProvider.findEntryFromAllType(incomingCaseId);
            // Mocker rolling = rollingProvider.findEntryFromAllType(newCaseId);

            if (pinned != null) {
                coverageRepository.updatePathKeyByRecordId(incomingCaseId, coverageMocker.getOperationName());
            } else {
                boolean locked = cacheProvider.putIfAbsent((coverageMocker.getAppId() + coverageMocker.getOperationName()).getBytes(),
                        60 * 24 * 12L,
                        coverageMocker.getRecordId().getBytes());

                if (locked) {
                    transferEntry(coverageMocker, incomingCaseId);
                } else {
                    mockSourceEditionService.removeByRecordId(ProviderNames.DEFAULT, incomingCaseId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("CoverageMockerHandler handle error", e);
        }
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
}
