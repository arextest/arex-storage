package com.arextest.storage.core.service;

import com.arextest.common.utils.CompressionUtils;
import com.arextest.storage.core.constants.MockCategoryMaskConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * When the use case is solidified,
 * the front-end service needs the query and update operations of the record case
 * Created by rchen9 on 2022/10/11.
 */
@Slf4j
@Service
public class FrontEndRecordService {

    @Resource
    private PrepareMockResultService prepareMockResultService;

    @Resource
    private ScheduleReplayingService scheduleReplayingService;

    public Map<Integer, List<String>> queryRecord(String recordId, Long categoryTypes) {
        prepareMockResultService.preloadAll(recordId);
        if (categoryTypes == null) {
            categoryTypes = MockCategoryMaskConstants.MAIN_CATEGORY_TYPES;
        }
        Map<Integer, List<String>> viewResult = scheduleReplayingService.queryRecordResult(recordId, categoryTypes);
        if (MapUtils.isEmpty(viewResult)) {
            LOGGER.info("viewRecord not found any resource recordId: {} ,categoryTypes: {}", recordId, categoryTypes);
            return viewResult;
        }
        Map<Integer, List<String>> decompressResult = new HashMap<>();
        viewResult.forEach((key, value) -> {
            List<String> decompressValue = Optional.ofNullable(value).orElse(Collections.emptyList()).stream()
                    .map(CompressionUtils::useZstdDecompress)
                    .collect(Collectors.toList());
            decompressResult.put(key, decompressValue);
        });
        return decompressResult;
    }
}
