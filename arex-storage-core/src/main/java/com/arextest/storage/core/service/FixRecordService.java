package com.arextest.storage.core.service;

import com.arextest.storage.core.constants.MockCategoryMaskConstants;
import com.arextest.storage.core.repository.RepositoryProvider;
import com.arextest.storage.core.repository.RepositoryProviderFactory;
import com.arextest.storage.core.service.utils.MockCategoryUtils;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MockItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * When the use case is solidified,
 * import the link of record into the "*Fixed" collection
 * <p>
 * Created by rchen9 on 2022/10/19.
 */
@Slf4j
@Service
public class FixRecordService {

    private final MockCategoryType[] mockerCategories = MockCategoryType.values();

    @Resource
    private RepositoryProviderFactory providerFactory;

    @Resource
    private ObjectMapper objectMapper;

    public <T extends MockItem> Map<Integer, List<String>> fixRecord(String recordId) throws Exception {
        String fixedUUID = recordId + "-" + System.currentTimeMillis();
        // search the record of mainEntry
        long categoryTypes = MockCategoryMaskConstants.MAIN_CATEGORY_TYPES;
        Map<Integer, List<String>> resultMap = new HashMap<>(mockerCategories.length);
        for (int i = 0; i < mockerCategories.length; i++) {
            MockCategoryType category = mockerCategories[i];
            if (category.isConfigVersion()) {
                continue;
            }
            if (MockCategoryUtils.shouldSkipCategory(categoryTypes, category.getCodeValue())) {
                continue;
            }

            RepositoryProvider<T> provider = providerFactory.findProvider(category);
            Iterable<T> mockItemIterators = provider.queryRecordList(recordId);
            if (mockItemIterators == null) {
                continue;
            }
            Iterator<T> valueIterator = mockItemIterators.iterator();
            while (valueIterator.hasNext()) {
                List<String> tempList = resultMap.getOrDefault(category.getCodeValue(), new ArrayList<>());
                T value = valueIterator.next();
                value.setRecordId(fixedUUID);
                try {
                    tempList.add(objectMapper.writeValueAsString(value));
                } catch (Throwable throwable) {
                    LOGGER.error("query error:{} from category:{}", throwable.getMessage(), category.getDisplayName());
                }
                resultMap.put(category.getCodeValue(), tempList);
            }
        }
        if (MapUtils.isEmpty(resultMap)) {
            throw new Exception("The record of mainEntry doesn't exist");
        }

        // copy the original record to the collection of "*Fixed"
        copyOriginalRecord(recordId, fixedUUID);
        return resultMap;
    }


    private <T extends MockItem> void copyOriginalRecord(String recordId, String fixedUUID) throws Exception {
        for (int i = 0; i < mockerCategories.length; i++) {
            MockCategoryType category = mockerCategories[i];
            if (category.isConfigVersion()) {
                continue;
            }

            RepositoryProvider<T> provider = providerFactory.findProvider(category);
            Iterable<T> mockItemIterators = provider.queryRecordList(recordId);
            if (mockItemIterators == null) {
                continue;
            }
            Iterator<T> valueIterator = mockItemIterators.iterator();
            while (valueIterator.hasNext()) {
                T value = valueIterator.next();
                replaceObjectId(value, fixedUUID);
                if (!provider.saveFixedRecord(value)) {
                    throw new Exception("The record failed to save");
                }
            }
        }
    }

    private <T extends MockItem> void replaceObjectId(T value, String objectId) {
        value.setCreateTime(System.currentTimeMillis());
        value.setRecordId(objectId);
        value.setId(null);
    }
}
