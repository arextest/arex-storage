package com.arextest.storage.service;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.replay.CountRecordCaseResponseType;
import com.arextest.model.replay.ListRecordCaseRequestType;
import com.arextest.model.replay.ListRecordCaseResponseType;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.RepositoryReader;
import com.arextest.storage.repository.ServiceOperationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RecordQueryService {
    private final RepositoryProviderFactory repositoryProviderFactory;
    private final ServiceOperationRepository serviceOperationRepository;
    private final ScheduleReplayingService scheduleReplayingService;

    public RecordQueryService(RepositoryProviderFactory repositoryProviderFactory,
                              ServiceOperationRepository serviceOperationRepository,
                              ScheduleReplayingService scheduleReplayingService) {
        this.repositoryProviderFactory = repositoryProviderFactory;
        this.serviceOperationRepository = serviceOperationRepository;
        this.scheduleReplayingService = scheduleReplayingService;
    }

    public CountRecordCaseResponseType countRecordByAppId(String appId) {
        Set<String> operationTypes = new HashSet<>();
        serviceOperationRepository.queryServiceOperations(appId, null)
                .forEach(serviceOperationEntity -> operationTypes.add(serviceOperationEntity.getOperationType()));
        CountRecordCaseResponseType response = new CountRecordCaseResponseType();

        long count = 0;
        PagedRequestType mockerQueryRequest = new PagedRequestType();
        mockerQueryRequest.setAppId(appId);
        mockerQueryRequest.setFilterPastRecordVersion(false);
        for(String operationType : operationTypes) {
            mockerQueryRequest.setCategory(MockCategoryType.create(operationType));
            count += scheduleReplayingService.countByRange(mockerQueryRequest);
        }
        response.setRecordedCaseCount(count);
        return response;
    }

    public ListRecordCaseResponseType listRecordCase(ListRecordCaseRequestType listRecordCaseRequest) {
        PagedRequestType pagedRequestType = listRecordCaseRequestToPagedRequestType(listRecordCaseRequest);
        pagedRequestType.setCategory(MockCategoryType.create(listRecordCaseRequest.getOperationType()));
        pagedRequestType.setFilterPastRecordVersion(false);

        ListRecordCaseResponseType responseType = new ListRecordCaseResponseType();
        RepositoryReader<AREXMocker> repositoryReader = repositoryProviderFactory.findProvider(
                pagedRequestType.getSourceProvider());
        if (repositoryReader == null) {
            return responseType;
        }
        List<AREXMocker> arexMockers = new IterableListWrapper<>(repositoryReader.queryRecordListPaging(
                pagedRequestType, listRecordCaseRequest.getPageIndex()));
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
}
