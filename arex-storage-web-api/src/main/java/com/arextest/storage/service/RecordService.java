package com.arextest.storage.service;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.CountRecordCaseResponseType;
import com.arextest.model.replay.ListRecordCaseRequestType;
import com.arextest.model.replay.ListRecordCaseResponseType;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.RepositoryReader;
import com.arextest.storage.repository.ServiceOperationRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RecordService {
    private final RepositoryProviderFactory repositoryProviderFactory;
    private final ServiceOperationRepository serviceOperationRepository;
    private final ScheduleReplayingService scheduleReplayingService;

    private static final Cache<String, Long> recordCaseCountCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build();

    public RecordService(RepositoryProviderFactory repositoryProviderFactory,
                         ServiceOperationRepository serviceOperationRepository,
                         ScheduleReplayingService scheduleReplayingService) {
        this.repositoryProviderFactory = repositoryProviderFactory;
        this.serviceOperationRepository = serviceOperationRepository;
        this.scheduleReplayingService = scheduleReplayingService;
    }

    public CountRecordCaseResponseType countRecordByAppId(String appId) {
        CountRecordCaseResponseType response = new CountRecordCaseResponseType();
        Long count = recordCaseCountCache.getIfPresent(appId);
        if (count == null) count = countRecord(appId);
        response.setRecordedCaseCount(count);
        return response;
    }

    private Long countRecord(String appId) {
        Set<String> operationTypes = new HashSet<>();
        serviceOperationRepository.queryServiceOperations(appId, null)
                .forEach(serviceOperationEntity -> operationTypes.add(serviceOperationEntity.getOperationType()));
        long count = 0;
        PagedRequestType mockerQueryRequest = new PagedRequestType();
        mockerQueryRequest.setAppId(appId);
        mockerQueryRequest.setFilterPastRecordVersion(false);
        for(String operationType : operationTypes) {
            mockerQueryRequest.setCategory(MockCategoryType.create(operationType));
            count += scheduleReplayingService.countByRange(mockerQueryRequest);
        }
        return count;
    }

    public ListRecordCaseResponseType listRecordCase(PagedRequestType pagedRequestType) {

        ListRecordCaseResponseType responseType = new ListRecordCaseResponseType();
        RepositoryReader<AREXMocker> repositoryReader = repositoryProviderFactory.findProvider(
                pagedRequestType.getSourceProvider());
        if (repositoryReader == null) {
            return responseType;
        }
        List<AREXMocker> arexMockers = new IterableListWrapper<>(repositoryReader.queryRecordListPaging(
                pagedRequestType, pagedRequestType.getPageIndex()));
        responseType.setRecordList(arexMockers);

        pagedRequestType.setBeginTime(null);
        pagedRequestType.setEndTime(null);
        responseType.setTotalCount(repositoryReader.countByRange(pagedRequestType));
        return responseType;
    }

    private PagedRequestType listRecordCaseRequestToPagedRequestType(ListRecordCaseRequestType input) {
        PagedRequestType output = new PagedRequestType();
        output.setOperation(input.getOperationName());
        output.setPageSize(input.getPageSize());
        output.setAppId(input.getAppId());
        return output;
    }

    public void updateCount(Mocker item) {
        String key = item.getAppId();
        Long count = recordCaseCountCache.getIfPresent(item.getAppId());

        if (count == null) {
            count = countRecord(key);
        }
        recordCaseCountCache.put(key, ++count);
    }
}
