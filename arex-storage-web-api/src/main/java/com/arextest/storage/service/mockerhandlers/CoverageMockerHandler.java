package com.arextest.storage.service.mockerhandlers;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.service.MockSourceEditionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class CoverageMockerHandler implements MockerSaveHandler<AREXMocker> {
    @Resource
    private RepositoryProviderFactory repositoryProviderFactory;
    @Resource
    private MockSourceEditionService mockSourceEditionService;

    private final RepositoryProvider<Mocker> pinedProvider = repositoryProviderFactory.findProvider(ProviderNames.AUTO_PINNED);
    private final RepositoryProvider<Mocker> rollingProvider = repositoryProviderFactory.findProvider(ProviderNames.DEFAULT);
    private final RepositoryProvider<Mocker> coverageProvider = repositoryProviderFactory.findProvider(ProviderNames.DEFAULT);

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
        try {
            assert pinedProvider != null;
            assert coverageProvider != null;

            String newCaseId = coverageMocker.getRecordId();
            Mocker pinned = pinedProvider.findEntryFromAllType(newCaseId);
            // Mocker rolling = rollingProvider.findEntryFromAllType(newCaseId);

            if (pinned != null) {
                // todo
                return;
            } else {
                Mocker oldCoverageMocker = coverageProvider.findOneAndReplace(MockCategoryType.COVERAGE,
                        coverageMocker.getAppId(),
                        coverageMocker.getOperationName(),
                        coverageMocker);

                // there is an existing AutoPinnedMocker with the same key, delete the related AutoPinnedMocker
                if (oldCoverageMocker != null) {
                    String oldCaseId = oldCoverageMocker.getRecordId();
                    boolean removed = mockSourceEditionService.removeEntry(ProviderNames.AUTO_PINNED, oldCaseId);
                    if (!removed) {
                        LOGGER.error("remove old auto pinned failed, caseId:{}", oldCaseId);
                    }
                }

                // move entry to auto pinned
                boolean moved = mockSourceEditionService.moveEntryTo(ProviderNames.DEFAULT, newCaseId, ProviderNames.AUTO_PINNED);
                if (!moved) {
                    LOGGER.error("move entry to auto pinned failed, caseId:{}", newCaseId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("CoverageMockerHandler handle error", e);
        }
    }
}
