package com.arextest.storage.service;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.model.replay.result.PostProcessResultRequestType;
import com.arextest.model.replay.result.ResultCodeGroup;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.impl.mongo.AutoPinedMockerRepository;
import com.arextest.storage.repository.impl.mongo.CoverageRepository;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.internal.Base64;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Mocker post-processing service Update or delete auto-pin mockers according to the result of
 * replay
 */
@Service
@Slf4j
public class ResultProcessService {

  private static final int COMPARED_WITHOUT_DIFFERENCE = 0;
  private static final int COMPARED_WITH_DIFFERENCE = 1;
  private static final int COMPARED_INTERNAL_EXCEPTION = 2;
  private static final int NORMAL_FINISH_CODE = 2;
  @Resource
  AutoPinedMockerRepository autoPinedMockerRepository;
  @Resource
  ScheduleReplayingService scheduleReplayingService;
  @Resource
  ZstdJacksonSerializer zstdJacksonSerializer;
  @Resource
  RepositoryProviderFactory repositoryProviderFactory;
  @Resource
  CoverageRepository coverageRepository;
  @Resource
  PrepareMockResultService prepareMockResultService;
  public void handleResult(PostProcessResultRequestType requestType) {
    List<ResultCodeGroup> diffResults = requestType.getResults();
    if (CollectionUtils.isEmpty(diffResults)) {
      return;
    }
    for (ResultCodeGroup diffResult : diffResults) {
      if (requestType.getReplayStatusCode() == NORMAL_FINISH_CODE) {
        handleAutoPin(diffResult);
        LOGGER.info("Auto pin modification done for plan {}", requestType.getReplayPlanId());
      }
    }
  }

  private void handleAutoPin(ResultCodeGroup diffResult) {
    int diffCode = diffResult.getResultCode();
    switch (diffCode) {
      case COMPARED_WITHOUT_DIFFERENCE:
        resetFailCount(diffResult.getCategoryGroups());
        break;
      case COMPARED_WITH_DIFFERENCE:
        increaseFailCount(diffResult.getCategoryGroups());
        break;
      case COMPARED_INTERNAL_EXCEPTION:
      default:
        // do nothing
        break;
    }
  }

  private void increaseFailCount(List<ResultCodeGroup.CategoryGroup> categoryGroups) {
    if (CollectionUtils.isEmpty(categoryGroups)) {
      return;
    }
    List<AREXMocker> needDelete = new ArrayList<>();
    for (ResultCodeGroup.CategoryGroup categoryGroup : categoryGroups) {
      MockCategoryType category = MockCategoryType.create(categoryGroup.getCategoryName());
      if (!category.isEntryPoint()) {
        continue;
      }
      for (ResultCodeGroup.IdPair idPair : categoryGroup.getResultIds()) {
        String recordId = idPair.getRecordId();
        AREXMocker updated = autoPinedMockerRepository.countFailAndUpdateReq(category, recordId);

        // todo might need to be more careful here
        // updated = null means the record is deleted by other plan, or this is not a auto-pined mocker
        if (updated != null &&
            updated.getContinuousFailCount() > 2 &&
            updated.getCreationTime() < getExpirationMillis()) {
          needDelete.add(updated);
        } else if (updated != null) {
          updateMocker(idPair, updated);
        }
      }
      if (!CollectionUtils.isEmpty(needDelete)) {
        autoPinedMockerRepository.deleteMany(category,
            needDelete.stream().map(AREXMocker::getId).collect(Collectors.toList()));
        coverageRepository.deleteByRecordIds(
            needDelete.stream().map(AREXMocker::getRecordId).collect(Collectors.toList()));
        needDelete = new ArrayList<>();
      }
    }
  }

  private void updateMocker(ResultCodeGroup.IdPair idPair, AREXMocker entry) {
    try {
      List<ListResultHolder> results = scheduleReplayingService.queryReplayResult(
          idPair.getRecordId(), idPair.getTargetId());
      for (ListResultHolder result : results) {
        MockCategoryType category = result.getCategoryType();
        RepositoryProvider<Mocker> categoryProvider = repositoryProviderFactory.findProvider(
            ProviderNames.AUTO_PINNED);

        List<AREXMocker> recordedMockers = zstdDeserialize(result.getRecord());
        List<AREXMocker> replayResults = zstdDeserialize(result.getReplayResult());
        // entry point， update response
        if (category.isEntryPoint()) {
          if (recordedMockers.size() != 1 || replayResults.size() != 1) {
            continue;
          } else {
            AREXMocker recordedMocker = recordedMockers.get(0);
            recordedMocker.setTargetResponse(replayResults.get(0).getTargetResponse());
            recordedMocker.setContinuousFailCount(entry.getContinuousFailCount());
            categoryProvider.update(recordedMocker);
          }
        } else {
          // dependencies
          Map<String, AREXMocker> recordIdMap = recordedMockers.stream()
              .collect(Collectors.toMap(AREXMocker::getId, i -> i));
          for (AREXMocker replayResult : replayResults) {
            // new call todo
            if (StringUtils.isEmpty(replayResult.getId())) {

            } else {
              AREXMocker recordedMocker = recordIdMap.get(replayResult.getId());
              if (recordedMocker == null) {
                continue;
              }
              recordedMocker.setTargetRequest(replayResult.getTargetRequest());
              categoryProvider.update(recordedMocker);
            }
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("update mocker error:{}", e.getMessage(), e);
    }
  }

  private void resetFailCount(List<ResultCodeGroup.CategoryGroup> categoryGroups) {
    if (CollectionUtils.isEmpty(categoryGroups)) {
      return;
    }

    for (ResultCodeGroup.CategoryGroup categoryGroup : categoryGroups) {
      MockCategoryType category = MockCategoryType.create(categoryGroup.getCategoryName());
      if (!category.isEntryPoint()) {
        continue;
      }
      List<String> allIds = categoryGroup.getResultIds().stream()
          .map(ResultCodeGroup.IdPair::getRecordId).collect(Collectors.toList());
      autoPinedMockerRepository.resetFailCount(category, allIds);
    }
  }

  private long getExpirationMillis() {
    return System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7;
  }

  private List<AREXMocker> zstdDeserialize(List<String> base64List) {
    if (CollectionUtils.isEmpty(base64List)) {
      return Collections.emptyList();
    }
    List<AREXMocker> decodedResult = new ArrayList<>(base64List.size());
    for (int i = 0; i < base64List.size(); i++) {
      String base64 = base64List.get(i);
      AREXMocker source = zstdJacksonSerializer.deserialize(Base64.decode(base64),
          AREXMocker.class);
      if (source == null) {
        continue;
      }
      decodedResult.add(source);
    }
    return decodedResult;
  }

  public void clearAllCache(PostProcessResultRequestType requestType) {
    List<ResultCodeGroup> diffResults = requestType.getResults();
    if (CollectionUtils.isEmpty(diffResults)) {
      return;
    }
    for (ResultCodeGroup diffResult : diffResults) {
      for (ResultCodeGroup.CategoryGroup categoryGroup : diffResult.getCategoryGroups()) {
        for (ResultCodeGroup.IdPair idPair : categoryGroup.getResultIds()) {
          prepareMockResultService.removeAllRecordCache(idPair.getRecordId(), null);
        }
      }
      LOGGER.info("Clear cache done for plan {}", requestType.getReplayPlanId());
    }
  }

}
