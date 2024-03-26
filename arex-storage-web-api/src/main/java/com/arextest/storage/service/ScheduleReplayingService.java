package com.arextest.storage.service;

import com.arextest.common.utils.CompressionUtils;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MergeRecordDTO;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.model.replay.ViewRecordRequestType;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.RepositoryReader;
import com.arextest.storage.trace.MDCTracer;
import com.arextest.storage.utils.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * When user create a plan, the schedule fired to replaying, which should be know how many replay
 * cases to sending and after send,then should be know what the result compared with origin record.
 * as for this,the ScheduleReplayingService as provider and impl it.
 *
 * @author jmo
 * @since 2021/11/4
 */
@Slf4j
@AllArgsConstructor
public class ScheduleReplayingService {

  private static final String MERGE_RECORD_OPERATION_NAME = "arex.mergeRecord";
  private static final List<String> MOCKER_PROVIDER_NAMES = Lists.newArrayList(
      ProviderNames.DEFAULT, ProviderNames.PINNED, ProviderNames.AUTO_PINNED);

  private final MockResultProvider mockResultProvider;
  private final RepositoryProviderFactory repositoryProviderFactory;
  private final ConfigRepositoryProvider<ApplicationOperationConfiguration> serviceOperationRepository;

  public List<ListResultHolder> queryReplayResult(String recordId, String replayResultId) {
    Set<MockCategoryType> categoryTypes = repositoryProviderFactory.getCategoryTypes();
    List<ListResultHolder> resultHolderList = new ArrayList<>(categoryTypes.size());
    ListResultHolder listResultHolder;
    for (MockCategoryType categoryType : categoryTypes) {
      if (categoryType.isSkipComparison()) {
        continue;
      }
      MDCTracer.addCategory(categoryType);
      List<String> recordList = encodeToBase64String(
          mockResultProvider.getRecordResultList(categoryType, recordId));
      List<String> replayResultList = encodeToBase64String(
          mockResultProvider.getReplayResultList(categoryType, replayResultId));
      if (CollectionUtils.isEmpty(recordList) && CollectionUtils.isEmpty(replayResultList)) {
        LOGGER.info("skipped empty replay result for category:{}, recordId:{} ,replayResultId:{}",
            categoryType, recordId, replayResultId);
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

  public List<AREXMocker> queryEntryPointByRange(PagedRequestType requestType) {
    RepositoryProvider<AREXMocker> repositoryProvider = repositoryProviderFactory.findProvider(
        requestType.getSourceProvider());
    if (repositoryProvider != null) {
      return new IterableListWrapper<>(repositoryProvider.queryEntryPointByRange(requestType));
    }
    return Collections.emptyList();
  }

  public List<AREXMocker> queryRecordList(ViewRecordRequestType request) {
    // request category list
    Set<MockCategoryType> mockCategoryTypes = calculateNormalCategories(request);
    String recordId = request.getRecordId();

    // try query for requested provider
    List<AREXMocker> result = queryRecordsByProvider(request.getSourceProvider(), recordId,
        mockCategoryTypes);

    // if no result found, try query for all providers
    if (CollectionUtils.isEmpty(result)) {
      for (String providerName : MOCKER_PROVIDER_NAMES) {
        if (StringUtils.equals(providerName, request.getSourceProvider())) {
          continue;
        }
        result = queryRecordsByProvider(providerName, recordId, mockCategoryTypes);
        if (CollectionUtils.isNotEmpty(result)) {
          break;
        }
      }
    }

    if (Boolean.TRUE.equals(request.getSplitMergeRecord())) {
      return splitMergedMockers(result);
    }
    return result;
  }

  /**
   * @return note that the returned list MUST be modifiable, may need to append afterward
   */
  private List<AREXMocker> queryRecordsByProvider(String providerName, String recordId,
      Set<MockCategoryType> types) {
    RepositoryProvider<Mocker> repositoryReader = repositoryProviderFactory.findProvider(
        providerName);
    if (repositoryReader == null) {
      return Collections.emptyList();
    }

    // true -> entry point, false -> dependency
    Map<Boolean, List<MockCategoryType>> partition = types.stream()
        .collect(Collectors.partitioningBy(MockCategoryType::isEntryPoint));

    List<MockCategoryType> entryPointTypes = partition.get(true);

    if (CollectionUtils.isNotEmpty(entryPointTypes)) {
      // try get entrypoint first
      List<AREXMocker> result = entryPointTypes.stream()
          .flatMap(category -> queryRecordList(repositoryReader, category, recordId).stream())
          .collect(Collectors.toList());
      // if entry point mockers not found, early return
      if (CollectionUtils.isEmpty(result)) {
        return Collections.emptyList();
      } else {
        // if entry point mockers found, try getting all mockers back
        result.addAll(
            Optional.ofNullable(partition.get(false)).orElse(Collections.emptyList()).stream()
                .flatMap(category -> queryRecordList(repositoryReader, category, recordId).stream())
                .collect(Collectors.toList()));
        return result;
      }
    } else {
      return types.stream()
          .flatMap(category -> queryRecordList(repositoryReader, category, recordId).stream())
          .collect(Collectors.toList());
    }
  }

  /**
   * Calculate the requested categories.
   *
   * @param request incoming request defining the requesting categories
   * @return the set of categories to be queried
   */
  private Set<MockCategoryType> calculateNormalCategories(ViewRecordRequestType request) {
    Set<MockCategoryType> mockCategoryTypes;
    if (!StringUtils.isEmpty(request.getCategoryType())) {
      MockCategoryType category = repositoryProviderFactory.findCategory(request.getCategoryType());
      if (category == null) {
        return Collections.emptySet();
      } else {
        return Collections.singleton(category);
      }
    }

    if (request.getCategoryTypes() == null) {
      mockCategoryTypes = new HashSet<>(repositoryProviderFactory.getCategoryTypes());
    } else {
      mockCategoryTypes = new HashSet<>(request.getCategoryTypes().size());

      for (String categoryName : request.getCategoryTypes()) {
        // special logic shim for coverage mocker, currently used internally
        // repositoryProviderFactory.findCategory() will return null for type COVERAGE because it's needed for most case
        MockCategoryType category =
            MockCategoryType.COVERAGE.getName().equals(categoryName) ? MockCategoryType.COVERAGE
                : repositoryProviderFactory.findCategory(categoryName);

        if (category == null) {
          continue;
        }
        mockCategoryTypes.add(category);
      }
    }
    return mockCategoryTypes;
  }

  private List<AREXMocker> splitMergedMockers(List<AREXMocker> mockerList) {
    List<AREXMocker> result = new ArrayList<>();
    for (AREXMocker mocker : mockerList) {
      if (MERGE_RECORD_OPERATION_NAME.equals(mocker.getOperationName())) {
        List<AREXMocker> splitMockers = splitMergedMocker(mocker);
        if (CollectionUtils.isNotEmpty(splitMockers)) {
          result.addAll(splitMockers);
        }
      } else {
        result.add(mocker);
      }
    }
    return result;
  }

  private List<AREXMocker> splitMergedMocker(AREXMocker mocker) {
    List<AREXMocker> result = new ArrayList<>();
    String json = mocker.getTargetResponse().getBody();
    try {
      List<MergeRecordDTO> mergeRecords = JsonUtil.OBJECT_MAPPER.readValue(json,
          new TypeReference<List<MergeRecordDTO>>() {
          });
      for (int i = 0; i < mergeRecords.size(); i++) {
        MergeRecordDTO mergeRecord = mergeRecords.get(i);

        AREXMocker splitMocker = new AREXMocker();
        splitMocker.setAppId(mocker.getAppId());
        splitMocker.setCategoryType(mocker.getCategoryType());
        splitMocker.setRecordId(mocker.getRecordId());
        splitMocker.setReplayId(mocker.getReplayId());
        splitMocker.setCreationTime(mocker.getCreationTime());
        splitMocker.setRecordVersion(mocker.getRecordVersion());
        splitMocker.setRecordEnvironment(mocker.getRecordEnvironment());
        splitMocker.setId(mocker.getId());
        splitMocker.setIndex(i);

        splitMocker.setOperationName(mergeRecord.getOperationName());
        Target request = new Target();
        request.setBody(mergeRecord.getRequest());
        request.setAttributes(mergeRecord.getRequestAttributes());
        splitMocker.setTargetRequest(request);

        Target response = new Target();
        response.setBody(mergeRecord.getResponse());
        response.setAttributes(mergeRecord.getResponseAttributes());
        response.setType(mergeRecord.getResponseType());
        splitMocker.setTargetResponse(response);

        result.add(splitMocker);
      }
    } catch (Exception e) {
      LOGGER.error("failed to split mocker", e);
      return null;
    }
    return result;
  }

  public List<AREXMocker> queryRecordList(RepositoryProvider<Mocker> repositoryReader,
      MockCategoryType categoryType, String recordId) {
    Iterable<? extends Mocker> iterable = repositoryReader.queryRecordList(categoryType, recordId);
    if (iterable == null) {
      return null;
    }
    List<AREXMocker> resultList = new LinkedList<>();
    for (Mocker mocker : iterable) {
      resultList.add((AREXMocker) mocker);
    }
    resultList.sort(Comparator.comparing(Mocker::getCreationTime));
    return resultList;
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
    RepositoryReader<?> repositoryReader = repositoryProviderFactory.findProvider(
        pagedRequestType.getSourceProvider());
    if (repositoryReader != null) {
      return repositoryReader.countByOperationName(pagedRequestType);
    }
    return new HashMap<>();
  }

  private Map<String, Long> countAllEntrypointCategoryByOperationName(
      PagedRequestType pagedRequestType) {
    Set<String> operationTypes = getALlOperationTypes(pagedRequestType.getAppId());
    Map<String, Long> countMap = new HashMap<>();
    for (String operationType : operationTypes) {
      pagedRequestType.setCategory(MockCategoryType.createEntryPoint(operationType));
      mergeMap(countMap, countSingleCategoryByOperationName(pagedRequestType));
    }
    return countMap;
  }

  private void mergeMap(Map<String, Long> source, Map<String, Long> addition) {
    if (MapUtils.isEmpty(addition)) {
      return;
    }
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
    for (ApplicationOperationConfiguration serviceOperationEntity : serviceOperationRepository.listBy(
        appId)) {
      if (serviceOperationEntity.getOperationTypes() != null) {
        operationTypes.addAll(serviceOperationEntity.getOperationTypes());
      }
    }
    return operationTypes;
  }

  private long countSingleCategory(PagedRequestType replayCaseRangeRequest) {
    RepositoryReader<?> repositoryReader = repositoryProviderFactory.findProvider(
        replayCaseRangeRequest.getSourceProvider());
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
