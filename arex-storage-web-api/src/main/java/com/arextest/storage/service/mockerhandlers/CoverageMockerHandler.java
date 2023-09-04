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
public class CoverageMockerHandler implements MockerSaveHandler {
    @Resource
    private RepositoryProviderFactory repositoryProviderFactory;
    @Resource
    private MockSourceEditionService mockSourceEditionService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    private static class CoverageData {
        private String key;
        private String caseId;
        private String type;
    }

    @Override
    public MockCategoryType getMockCategoryType() {
        return MockCategoryType.COVERAGE;
    }

    /**
     * 如果为自动固化的Case:
     *      更新数据
     *
     * 为新记录的Case:
     *      判断是否存在相同Key，相同则删除原自动固化Case，更新为新Case（每次回放只更新一次），不存在则自动固化
     */
    @Override
    public void handle(AREXMocker item) {
        try {
            final RepositoryProvider<Mocker> mockerProvider = repositoryProviderFactory.findProvider(ProviderNames.AUTO_PINNED);
            final RepositoryProvider<Mocker> coverageProvider = repositoryProviderFactory.findProvider(ProviderNames.DEFAULT);
            assert mockerProvider != null;
            assert coverageProvider != null;

            CoverageData data = objectMapper.readValue(item.getTargetResponse().getBody(), CoverageData.class);
            MockCategoryType entryType = MockCategoryType.createEntryPoint(data.getType());

            if (mockerProvider.exist(entryType, data.getCaseId())) {
                // todo
                return;
            } else {
                Mocker oldCoverageData = coverageProvider.findOneAndReplace(MockCategoryType.COVERAGE, item.getAppId(), data.getKey(), item);
                if (oldCoverageData != null) {
                    // clear old entry with the same path (calculated to key)
                    CoverageData oldData = objectMapper.readValue(oldCoverageData.getTargetResponse().getBody(), CoverageData.class);
                    MockCategoryType oldEntryType = MockCategoryType.createEntryPoint(oldData.getType());
                    mockerProvider.removeBy(oldEntryType, oldCoverageData.getId());

                    // save new entry
                    mockSourceEditionService.copyTo(ProviderNames.DEFAULT, data.getCaseId(), ProviderNames.AUTO_PINNED, data.getCaseId());
                }
            }

        } catch (Exception e) {
            LOGGER.error("CoverageMockerHandler handle error", e);
        }
    }
}
