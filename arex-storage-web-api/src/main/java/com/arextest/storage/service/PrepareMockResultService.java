package com.arextest.storage.service;

import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.trace.MDCTracer;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * As for improve performance goal,
 * The schedule send replay case before,should be call prepare action,touch all dependency to cached.
 * After compared,remove all cached source or auto expired.
 */
@Slf4j
@Service
public final class PrepareMockResultService {
    @Resource
    private RepositoryProviderFactory providerFactory;
    @Resource
    private MockResultProvider mockResultProvider;

    public boolean preloadAll(String sourceProvider, String recordId) {
        List<RepositoryProvider<? extends MockItem>> repositoryProviderList =
                providerFactory.getRepositoryProviderList();
        boolean result = false;
        MockCategoryType categoryType;
        for (RepositoryProvider<? extends MockItem> repositoryReader : repositoryProviderList) {
            categoryType = repositoryReader.getCategory();
            if (categoryType.isConfigVersion()) {
                continue;
            }
            if (StringUtils.isNotEmpty(sourceProvider) &&
                    !StringUtils.equals(sourceProvider, repositoryReader.getProviderName())) {
                continue;
            }
            result = preload(repositoryReader, recordId);
            LOGGER.info("preload cache result:{},category:{},record id:{}", result, categoryType.getDisplayName(),
                    recordId);

        }
        return result;
    }

    boolean preload(MockCategoryType category, String recordId) {
        return this.preload(providerFactory.findProvider(category), recordId);
    }

    private boolean preload(RepositoryProvider<? extends MockItem> repositoryReader, String recordId) {
        if (repositoryReader == null) {
            return true;
        }
        MockCategoryType category = repositoryReader.getCategory();
        MDCTracer.addCategory(category);
        if (mockResultProvider.recordResultCount(category, recordId) > 0) {
            LOGGER.warn("skip preload cache for category:{},record id:{}", category.getDisplayName(), recordId);
            return true;
        }
        Iterable<? extends MockItem> iterable;
        iterable = repositoryReader.queryRecordList(recordId);
        if (iterable == null) {
            return true;
        }
        return mockResultProvider.putRecordResult(category, recordId, iterable);
    }

    public boolean removeAll(String recordId) {
        List<RepositoryProvider<? extends MockItem>> repositoryProviderList =
                providerFactory.getRepositoryProviderList();
        boolean result = false;
        for (RepositoryProvider<? extends MockItem> repositoryReader : repositoryProviderList) {
            result = remove(repositoryReader.getCategory(), recordId);
        }
        return result;
    }

    public boolean remove(MockCategoryType category, String recordId) {
        return mockResultProvider.removeRecordResult(category, recordId);
    }

}