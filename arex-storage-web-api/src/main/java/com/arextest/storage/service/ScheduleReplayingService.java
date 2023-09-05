package com.arextest.storage.service;

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import com.arextest.common.utils.CompressionUtils;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.model.replay.ViewRecordRequestType;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.model.dto.config.application.ApplicationOperationConfiguration;
import com.arextest.storage.repository.ConfigRepositoryProvider;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.RepositoryReader;
import com.arextest.storage.trace.MDCTracer;

import lombok.extern.slf4j.Slf4j;

/**
 * When user create a plan, the schedule fired to replaying, which should be know how many replay cases to sending and
 * after send,then should be know what the result compared with origin record. as for this,the ScheduleReplayingService
 * as provider and impl it.
 *
 * @author jmo
 * @since 2021/11/4
 */
@Slf4j
public class ScheduleReplayingService {
    private final MockResultProvider mockResultProvider;
    private final RepositoryProviderFactory repositoryProviderFactory;

    private final ConfigRepositoryProvider<ApplicationOperationConfiguration> serviceOperationRepository;

    public ScheduleReplayingService(MockResultProvider mockResultProvider,
        RepositoryProviderFactory repositoryProviderFactory,
        ConfigRepositoryProvider<ApplicationOperationConfiguration> serviceOperationRepository) {
        this.mockResultProvider = mockResultProvider;
        this.repositoryProviderFactory = repositoryProviderFactory;
        this.serviceOperationRepository = serviceOperationRepository;
    }

    public List<ListResultHolder> queryReplayResult(String recordId, String replayResultId) {
        Set<MockCategoryType> categoryTypes = repositoryProviderFactory.getCategoryTypes();
        List<ListResultHolder> resultHolderList = new ArrayList<>(categoryTypes.size());
        ListResultHolder listResultHolder;
        for (MockCategoryType categoryType : categoryTypes) {
            if (categoryType.isSkipComparison()) {
                continue;
            }
            MDCTracer.addCategory(categoryType);
            List<String> recordList =
                encodeToBase64String(mockResultProvider.getRecordResultList(categoryType, recordId));
            List<String> replayResultList =
                encodeToBase64String(mockResultProvider.getReplayResultList(categoryType, replayResultId));
            if (CollectionUtils.isEmpty(recordList) && CollectionUtils.isEmpty(replayResultList)) {
                LOGGER.info("skipped empty replay result for category:{}, recordId:{} ,replayResultId:{}", categoryType,
                    recordId, replayResultId);
                continue;
            }
            listResultHolder = new ListResultHolder();
            listResultHolder.setCategoryType(categoryType);
            listResultHolder.setRecord(recordList);
            listResultHolder.setReplayResult(replayResultList);
            resultHolderList.add(listResultHolder);
        }
        return resultHolderList;
    }

    public List<AREXMocker> queryByRange(PagedRequestType requestType) {
        RepositoryReader<AREXMocker> repositoryReader =
            repositoryProviderFactory.findProvider(requestType.getSourceProvider());
        if (repositoryReader != null) {
            return new IterableListWrapper<>(repositoryReader.queryByRange(requestType));
        }
        return Collections.emptyList();
    }

    public List<AREXMocker> queryEntryPointByRange(PagedRequestType requestType) {
        RepositoryProvider<AREXMocker> repositoryProvider =
            repositoryProviderFactory.findProvider(requestType.getSourceProvider());
        if (repositoryProvider != null) {
            return new IterableListWrapper<>(repositoryProvider.queryEntryPointByRange(requestType));
        }
        return Collections.emptyList();
    }

    public List<AREXMocker> queryRecordList(ViewRecordRequestType viewRecordRequestType) {
        String sourceProvider = viewRecordRequestType.getSourceProvider();
        String recordId = viewRecordRequestType.getRecordId();
        RepositoryReader<AREXMocker> repositoryReader = repositoryProviderFactory.findProvider(sourceProvider);
        if (repositoryReader == null) {
            return Collections.emptyList();
        }
        MockCategoryType categoryType = repositoryProviderFactory.findCategory(viewRecordRequestType.getCategoryType());
        if (categoryType != null) {
            return new IterableListWrapper<>(repositoryReader.queryRecordList(categoryType, recordId));
        }
        List<AREXMocker> readableResult = new LinkedList<>();
        for (MockCategoryType category : repositoryProviderFactory.getCategoryTypes()) {
            MDCTracer.addCategory(category);
            Iterable<AREXMocker> recordList = repositoryReader.queryRecordList(category, recordId);
            if (recordList == null) {
                continue;
            }
            for (AREXMocker mocker : recordList) {
                readableResult.add(mocker);
            }
        }
        return readableResult;
    }

    public long countByRange(PagedRequestType replayCaseRangeRequest) {
        if (replayCaseRangeRequest.getCategory() == null) {
            return countAllEntrypointCategory(replayCaseRangeRequest);
        } else {
            return countSingleCategory(replayCaseRangeRequest);
        }
    }

    public Map<String, Long> countByOperationName(PagedRequestType pagedRequestType) {
        if (pagedRequestType.getCategory() == null) {
            return countAllEntrypointCategoryByOperationName(pagedRequestType);
        } else {
            return countSingleCategoryByOperationName(pagedRequestType);
        }
    }

    private Map<String, Long> countSingleCategoryByOperationName(PagedRequestType pagedRequestType) {
        RepositoryReader<?> repositoryReader =
            repositoryProviderFactory.findProvider(pagedRequestType.getSourceProvider());
        if (repositoryReader != null) {
            return repositoryReader.countByOperationName(pagedRequestType);
        }
        return new HashMap<>();
    }

    private Map<String, Long> countAllEntrypointCategoryByOperationName(PagedRequestType pagedRequestType) {
        Set<String> operationTypes = getALlOperationTypes(pagedRequestType.getAppId());
        Map<String, Long> countMap = new HashMap<>();
        for (String operationType : operationTypes) {
            pagedRequestType.setCategory(MockCategoryType.createEntryPoint(operationType));
            mergeMap(countMap, countSingleCategoryByOperationName(pagedRequestType));
        }
        return countMap;
    }

    private void mergeMap(Map<String, Long> source, Map<String, Long> addition) {
        if (MapUtils.isEmpty(addition))
            return;
        for (Map.Entry<String, Long> entry : addition.entrySet()) {
            if (source.containsKey(entry.getKey())) {
                source.put(entry.getKey(), source.get(entry.getKey()) + entry.getValue());
            } else {
                source.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Set<String> getALlOperationTypes(String appId) {
        Set<String> operationTypes = new HashSet<>();
        for (ApplicationOperationConfiguration serviceOperationEntity : serviceOperationRepository.listBy(appId)) {
            if (serviceOperationEntity.getOperationTypes() != null)
                operationTypes.addAll(serviceOperationEntity.getOperationTypes());
        }
        return operationTypes;
    }

    private long countSingleCategory(PagedRequestType replayCaseRangeRequest) {
        RepositoryReader<?> repositoryReader =
            repositoryProviderFactory.findProvider(replayCaseRangeRequest.getSourceProvider());
        if (repositoryReader != null) {
            return repositoryReader.countByRange(replayCaseRangeRequest);
        }
        return 0;
    }

    private long countAllEntrypointCategory(PagedRequestType replayCaseRangeRequest) {
        Set<String> operationTypes = getALlOperationTypes(replayCaseRangeRequest.getAppId());
        long count = 0;
        for (String operationType : operationTypes) {
            replayCaseRangeRequest.setCategory(MockCategoryType.createEntryPoint(operationType));
            count += countSingleCategory(replayCaseRangeRequest);
        }
        return count;
    }

    private List<String> encodeToBase64String(List<byte[]> source) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        final List<String> recordResult = new ArrayList<>(source.size());
        for (byte[] values : source) {
            String encodeResult = CompressionUtils.encodeToBase64String(values);
            if (encodeResult != null) {
                recordResult.add(encodeResult);
            }
        }
        return recordResult;
    }

}