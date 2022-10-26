package com.arextest.storage.core.service;

import com.arextest.storage.core.constants.MockCategoryMaskConstants;
import com.arextest.storage.core.repository.RepositoryProvider;
import com.arextest.storage.core.repository.RepositoryProviderFactory;
import com.arextest.storage.core.service.utils.MockCategoryUtils;
import com.arextest.storage.core.trace.MDCTracer;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MockItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * When the use case is solidified,
 * the front-end service needs the query and update operations of the record case
 * Created by rchen9 on 2022/10/11.
 */
@Slf4j
@Service
public class FrontEndRecordService {

    private final MockCategoryType[] mockerCategories = MockCategoryType.values();

    @Resource
    private RepositoryProviderFactory providerFactory;

    @Resource
    private ObjectMapper objectMapper;

    public <T extends MockItem> Map<Integer, List<String>> queryFixedRecord(String recordId, Long categoryTypes) {
        if (categoryTypes == null) {
            categoryTypes = MockCategoryMaskConstants.MAIN_CATEGORY_TYPES;
        }

        Map<Integer, List<String>> resultHolderMap = new HashMap<>(mockerCategories.length);
        for (int i = 0; i < mockerCategories.length; i++) {
            MockCategoryType category = mockerCategories[i];
            if (category.isConfigVersion()) {
                continue;
            }
            if (MockCategoryUtils.shouldSkipCategory(categoryTypes, category.getCodeValue())) {
                continue;
            }
            MDCTracer.addCategory(category);

            RepositoryProvider<T> provider = providerFactory.findProvider(category);
            Iterable<T> mockItemIterators = provider.queryRecordList(recordId);
            if (mockItemIterators == null) {
                continue;
            }
            Iterator<T> valueIterator = mockItemIterators.iterator();
            while (valueIterator.hasNext()) {
                T value = valueIterator.next();
                List<String> tempList = resultHolderMap.getOrDefault(category.getCodeValue(), new ArrayList<>());
                try {
                    tempList.add(objectMapper.writeValueAsString(value));
                } catch (Throwable throwable) {
                    LOGGER.error("query error:{} from category:{}", throwable.getMessage(), category.getDisplayName());
                }
                resultHolderMap.put(category.getCodeValue(), tempList);
            }
        }
        return resultHolderMap;
    }
}
