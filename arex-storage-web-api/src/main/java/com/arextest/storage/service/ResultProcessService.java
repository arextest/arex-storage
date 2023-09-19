package com.arextest.storage.service;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.holder.ListResultHolder;
import com.arextest.model.replay.result.PostProcessResultRequestType;
import com.arextest.model.replay.result.ResultCodeGroup;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.repository.RepositoryProviderFactory;
import com.arextest.storage.repository.impl.mongo.AutoPinedMockerRepository;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.internal.Base64;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mocker post-processing service
 * Update or delete auto-pin mockers according to the result of replay
 */
@Service
@Slf4j
public class ResultProcessService {
    @Resource
    AutoPinedMockerRepository autoPinedMockerRepository;
    @Resource
    ScheduleReplayingService scheduleReplayingService;
    @Resource
    ZstdJacksonSerializer zstdJacksonSerializer;
    @Resource
    RepositoryProviderFactory repositoryProviderFactory;
    public static final int COMPARED_WITHOUT_DIFFERENCE = 0;
    public static final int COMPARED_WITH_DIFFERENCE = 1;
    public static final int COMPARED_INTERNAL_EXCEPTION = 2;

    public void handleResult(PostProcessResultRequestType requestType) {
        List<ResultCodeGroup> diffResults = requestType.getResults();
        if (CollectionUtils.isEmpty(diffResults)) {
            return;
        }

        for (ResultCodeGroup diffResult : diffResults) {
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
    }

    private void increaseFailCount(List<ResultCodeGroup.CategoryGroup> categoryGroups) {
        if (CollectionUtils.isEmpty(categoryGroups)) {
            return;
        }
        List<AREXMocker> needDelete = new ArrayList<>();
        for (ResultCodeGroup.CategoryGroup categoryGroup : categoryGroups) {
            MockCategoryType category = MockCategoryType.create(categoryGroup.getCategoryName());
            if (!category.isEntryPoint()) continue;
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
                    updateMocker(idPair);
                }
            }
            if (!CollectionUtils.isEmpty(needDelete)) {
                autoPinedMockerRepository.deleteMany(category, needDelete.stream().map(AREXMocker::getId).collect(Collectors.toList()));
                needDelete = new ArrayList<>();
            }
        }
    }

    private void updateMocker(ResultCodeGroup.IdPair idPair) {
        try {
            List<ListResultHolder> results = scheduleReplayingService.queryReplayResult(idPair.getRecordId(), idPair.getTargetId());
            for (ListResultHolder result : results) {
                MockCategoryType category = result.getCategoryType();
                RepositoryProvider<Mocker> categoryProvider = repositoryProviderFactory.findProvider(category.getName());

                List<AREXMocker> recordedMockers = zstdDeserialize(result.getRecord());
                List<AREXMocker> replayResults = zstdDeserialize(result.getReplayResult());
                // entry point， update response
                if (category.isEntryPoint()) {
                    if (recordedMockers.size() != 1 || replayResults.size() != 1) {
                        continue;
                    } else {
                        categoryProvider.updateResponse(category, recordedMockers.get(0).getId(), replayResults.get(0).getTargetResponse());
                    }
                }

                // dependencies
                Map<String, AREXMocker> recordIdMap = recordedMockers.stream().collect(Collectors.toMap(AREXMocker::getId, i -> i));
                for (AREXMocker replayResult : replayResults) {
                    // new call todo
                    if (StringUtils.isEmpty(replayResult.getId())) {

                    } else {
                        AREXMocker recordedMocker = recordIdMap.get(replayResult.getId());
                        if (recordedMocker == null) {
                            continue;
                        }
                        categoryProvider.updateRequest(category, recordedMocker.getId(), replayResult.getTargetRequest());
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
            if (!category.isEntryPoint()) continue;
            for (ResultCodeGroup.IdPair idPair : categoryGroup.getResultIds()) {
                String recordId = idPair.getRecordId();
                autoPinedMockerRepository.resetFailCount(category, recordId);
            }
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
            AREXMocker source = zstdJacksonSerializer.deserialize(Base64.decode(base64), AREXMocker.class);
            if (source == null) {
                continue;
            }
            decodedResult.add(source);
        }
        return decodedResult;
    }
}
