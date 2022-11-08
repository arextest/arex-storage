package com.arextest.storage.service;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
public class MockSourceEditableService {

    private final MockCategoryType[] mockerCategories = MockCategoryType.values();

    @Resource
    private RepositoryProviderFactory providerFactory;

    public <T extends MockItem> boolean update(String providerName, MockCategoryType category, T item) {
        RepositoryProvider<T> repositoryWriter = providerFactory.findProvider(providerName, category);
        return repositoryWriter != null && repositoryWriter.update(item);
    }

    public boolean removeAll(String providerName, String recordId) {
        for (int i = 0; i < mockerCategories.length; i++) {
            MockCategoryType category = mockerCategories[i];
            remove(providerName, category, recordId);
        }
        return true;
    }

    public boolean remove(String providerName, MockCategoryType category, String recordId) {
        try {
            removeBy(providerName, category, recordId);
        } catch (Throwable throwable) {
            LOGGER.error("remove record error:{} from {} for category:{} at recordId:{}", throwable.getMessage(),
                    providerName, category,
                    recordId,
                    throwable);
            return false;
        }
        return true;
    }

    public int copyTo(String srcProviderName, String srcRecordId, String targetProviderName, String targetRecordId) {
        int count = 0;
        if (StringUtils.equals(srcProviderName, targetProviderName)) {
            return count;
        }
        RepositoryProvider<MockItem> srcProvider;
        RepositoryProvider<MockItem> targetProvider;
        Iterable<MockItem> srcItemIterable;

        for (int i = 0; i < mockerCategories.length; i++) {
            MockCategoryType category = mockerCategories[i];
            srcProvider = providerFactory.findProvider(srcProviderName, category);
            if (srcProvider == null) {
                continue;
            }
            targetProvider = providerFactory.findProvider(targetProviderName, category);
            if (targetProvider == null) {
                continue;
            }
            srcItemIterable = srcProvider.queryRecordList(srcRecordId);
            if (srcItemIterable == null) {
                continue;
            }
            List<MockItem> targetList = createTargetList(srcItemIterable, targetRecordId);
            if (CollectionUtils.isNotEmpty(targetList)) {
                targetProvider.saveList(targetList);
                count += targetList.size();
            }

        }
        return count;
    }

    private List<MockItem> createTargetList(Iterable<MockItem> srcItemIterable, String targetRecordId) {
        Iterator<MockItem> valueIterator = srcItemIterable.iterator();
        List<MockItem> targetList = null;
        long now = System.currentTimeMillis();
        while (valueIterator.hasNext()) {
            if (targetList == null) {
                targetList = new LinkedList<>();
            }
            MockItem value = valueIterator.next();
            value.setRecordId(targetRecordId);
            value.setId(null);
            value.setCreateTime(now);
            targetList.add(value);
        }
        return targetList;
    }

    private <T extends MockItem> void removeBy(String srcType, MockCategoryType category, String recordId) {
        RepositoryProvider<T> repositoryWriter = providerFactory.findProvider(srcType, category);
        if (repositoryWriter != null) {
            repositoryWriter.removeBy(recordId);
        }
    }
}