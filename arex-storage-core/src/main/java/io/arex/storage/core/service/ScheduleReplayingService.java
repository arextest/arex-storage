package io.arex.storage.core.service;

import io.arex.common.utils.CompressionUtils;
import io.arex.storage.core.mock.MockResultProvider;
import io.arex.storage.core.repository.RepositoryProviderFactory;
import io.arex.storage.core.repository.RepositoryReader;
import io.arex.storage.core.trace.MDCTracer;
import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.replay.ReplayCaseRangeRequestType;
import io.arex.storage.model.replay.holder.ListResultHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * When user create a plan, the schedule fired to replaying,
 * which should be know how many replay cases to sending and
 * after send,then should be know what the result compared with origin record.
 * as for this,the ScheduleReplayingService as provider and impl it.
 *
 * @author jmo
 * @since 2021/11/4
 */
@Slf4j
@Service
public final class ScheduleReplayingService {
    @Resource
    private MockResultProvider mockResultProvider;
    private final MockCategoryType[] mockerCategories = MockCategoryType.values();
    @Resource
    private RepositoryProviderFactory repositoryProviderFactory;

    public List<ListResultHolder<String>> queryReplayResult(final String recordId, final String replayResultId) {
        List<ListResultHolder<String>> resultHolderList = new ArrayList<>(mockerCategories.length);
        ListResultHolder<String> listResultHolder;
        for (int i = 0; i < mockerCategories.length; i++) {
            MockCategoryType category = mockerCategories[i];
            if (category.isConfigVersion()) {
                continue;
            }
            MDCTracer.addCategory(category);
            List<String> recordList = encodeToBase64String(mockResultProvider.getRecordResultList(category, recordId));
            List<String> replayResultList = encodeToBase64String(mockResultProvider.getReplayResultList(category,
                    replayResultId));
            if (CollectionUtils.isEmpty(recordList) && CollectionUtils.isEmpty(replayResultList)) {
                LOGGER.info("skipped empty replay result for category:{}, recordId:{} ,replayResultId:{}",
                        category.getDisplayName(), recordId,
                        replayResultId);
                continue;
            }
            listResultHolder = new ListResultHolder<>();
            listResultHolder.setCategoryName(category.getDisplayName());
            listResultHolder.setRecord(recordList);
            listResultHolder.setReplayResult(replayResultList);
            resultHolderList.add(listResultHolder);
        }
        return resultHolderList;
    }

    public Iterable<?> pagingQueryReplayCaseList(MockCategoryType category, ReplayCaseRangeRequestType requestType) {
        RepositoryReader<?> repositoryReader = repositoryProviderFactory.findProvider(category);
        if (repositoryReader != null) {
            return repositoryReader.queryByRange(requestType);
        }
        return Collections.emptyList();
    }

    public Map<Integer, List<String>> queryRecordResult(String recordId, long categoryTypes) {
        Map<Integer, List<String>> resultHolderMap = new HashMap<>(mockerCategories.length);
        for (int i = 0; i < mockerCategories.length; i++) {
            MockCategoryType category = mockerCategories[i];
            if (category.isConfigVersion()) {
                continue;
            }
            if (shouldSkipCategory(categoryTypes, category.getCodeValue())) {
                continue;
            }
            MDCTracer.addCategory(category);
            List<String> recordList = encodeToBase64String(mockResultProvider.getRecordResultList(category, recordId));
            if (CollectionUtils.isEmpty(recordList)) {
                LOGGER.info("skipped empty record result for category:{}, recordId:{} ", category.getDisplayName(),
                        recordId);
                continue;
            }
            resultHolderMap.put(category.getCodeValue(), recordList);
        }
        return resultHolderMap;
    }

    private boolean shouldSkipCategory(long categoryTypes, int codeValue) {
        return (categoryTypes > 0 && (categoryTypes & 1L << codeValue) == 0);
    }

    public int countByRange(MockCategoryType category, ReplayCaseRangeRequestType replayCaseRangeRequest) {
        RepositoryReader<?> repositoryReader = repositoryProviderFactory.findProvider(category);
        if (repositoryReader != null) {
            return repositoryReader.countByRange(replayCaseRangeRequest);
        }
        return 0;
    }
    private  List<String> encodeToBase64String(List<byte[]> source) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        final List<String> recordResult = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            byte[] values = source.get(i);
            String encodeResult = CompressionUtils.encodeToBase64String(values);
            if (encodeResult != null) {
                recordResult.add(encodeResult);
            }
        }
        return recordResult;
    }

}
