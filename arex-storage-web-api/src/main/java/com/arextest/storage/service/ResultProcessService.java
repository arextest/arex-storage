package com.arextest.storage.service;

import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.replay.result.PostProcessResultRequestType;
import com.arextest.model.replay.result.ResultCodeGroup;
import com.arextest.storage.repository.impl.mongo.AutoPinedMockerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * Mocker post-processing service
 * Update or delete auto-pin mockers according to the result of replay
 */
@Service
@Slf4j
public class ResultProcessService {
    @Resource
    AutoPinedMockerRepository autoPinedMockerRepository;
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

        for (ResultCodeGroup.CategoryGroup categoryGroup : categoryGroups) {
            MockCategoryType category = MockCategoryType.create(categoryGroup.getCategoryName());
            if (!category.isEntryPoint()) continue;
            for (String recordId : categoryGroup.getRecordIds()) {
                autoPinedMockerRepository.incrFailCount(category, recordId);
            }
        }
    }

    private void resetFailCount(List<ResultCodeGroup.CategoryGroup> categoryGroups) {
        if (CollectionUtils.isEmpty(categoryGroups)) {
            return;
        }

        for (ResultCodeGroup.CategoryGroup categoryGroup : categoryGroups) {
            MockCategoryType category = MockCategoryType.create(categoryGroup.getCategoryName());
            if (!category.isEntryPoint()) continue;
            for (String recordId : categoryGroup.getRecordIds()) {
                autoPinedMockerRepository.resetFailCount(category, recordId);
            }
        }
    }
}
