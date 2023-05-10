package com.arextest.storage.service;

import com.arextest.common.utils.CompressionUtils;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.replay.CountRecordCaseResponseType;
import com.arextest.model.replay.ListRecordCaseResponseType;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.model.replay.ViewRecordRequestType;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.model.dao.ServiceOperationEntity;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.RepositoryReader;
import com.arextest.storage.repository.ServiceOperationRepository;
import com.arextest.storage.trace.MDCTracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

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
public class ScheduleReplayingService {
    private final MockResultProvider mockResultProvider;
    private final RepositoryProviderFactory repositoryProviderFactory;
    private final ServiceOperationRepository serviceOperationRepository;

    public ScheduleReplayingService(MockResultProvider mockResultProvider,
                                    RepositoryProviderFactory repositoryProviderFactory,
                                    ServiceOperationRepository serviceOperationRepository) {
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
            List<String> recordList = encodeToBase64String(mockResultProvider.getRecordResultList(categoryType, recordId));
            List<String> replayResultList = encodeToBase64String(mockResultProvider.getReplayResultList(categoryType,
                    replayResultId));
            if (CollectionUtils.isEmpty(recordList) && CollectionUtils.isEmpty(replayResultList)) {
                LOGGER.info("skipped empty replay result for category:{}, recordId:{} ,replayResultId:{}",
                        categoryType, recordId,
                        replayResultId);
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
        RepositoryReader<?> repositoryReader =
                repositoryProviderFactory.findProvider(replayCaseRangeRequest.getSourceProvider());
        if (repositoryReader != null) {
            return repositoryReader.countByRange(replayCaseRangeRequest);
        }
        return 0;
    }

    public CountRecordCaseResponseType countRecordByAppId(String appId) {
        Set<String> operationNames = new HashSet<>();
        Set<String> operationTypes = new HashSet<>();
        serviceOperationRepository.queryServiceOperations(appId, null).forEach(serviceOperationEntity -> {
            operationNames.add(serviceOperationEntity.getOperationName());
            operationTypes.add(serviceOperationEntity.getOperationType());
        });
        CountRecordCaseResponseType response = new CountRecordCaseResponseType();
        //ascending dictionary order
        response.setOperationNameList(new ArrayList<>(operationNames).stream().sorted().collect(Collectors.toList()));

        long count = 0;
        PagedRequestType mockerQueryRequest = new PagedRequestType();
        mockerQueryRequest.setAppId(appId);
        mockerQueryRequest.setFilterPastRecordVersion(false);
        for(String operationType : operationTypes) {
            mockerQueryRequest.setCategory(MockCategoryType.create(operationType));
            count += countByRange(mockerQueryRequest);
        }
        response.setRecordedCaseCount(count);
        return response;
    }

    public ListRecordCaseResponseType listRecordCase(PagedRequestType listRecordCaseRequest) {
        String appId = listRecordCaseRequest.getAppId();
        String operationName = listRecordCaseRequest.getOperation();
        ServiceOperationEntity serviceOperationEntity = null;
        Iterable<ServiceOperationEntity> serviceOperationEntities =
                serviceOperationRepository.queryServiceOperations(appId, operationName);
        for (ServiceOperationEntity entity : serviceOperationEntities) {
            serviceOperationEntity = entity;
            break;
        }
        String operationType = Optional.ofNullable(serviceOperationEntity).map(ServiceOperationEntity::getOperationType)
                .orElse(null);
        listRecordCaseRequest.setCategory(MockCategoryType.create(operationType));

        ListRecordCaseResponseType responseType = new ListRecordCaseResponseType();
        RepositoryReader<AREXMocker> repositoryReader = repositoryProviderFactory.findProvider(
                listRecordCaseRequest.getSourceProvider());
        if (repositoryReader == null) {
            return responseType;
        }
        List<AREXMocker> arexMockers = new IterableListWrapper<>(repositoryReader.queryByRange(listRecordCaseRequest));
        responseType.setRecordList(arexMockers);

        //remove start&end time to count all the records, only First Page will return
        if (listRecordCaseRequest.getEndTime() == null) {
            listRecordCaseRequest.setBeginTime(null);
            listRecordCaseRequest.setEndTime(null);
            responseType.setTotalCount(repositoryReader.countByRange(listRecordCaseRequest));
        }
        return responseType;
    }

    private List<String> encodeToBase64String(List<byte[]> source) {
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