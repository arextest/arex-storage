package io.arex.storage.core.service;


import io.arex.common.cache.CacheProvider;
import io.arex.storage.core.cache.CacheKeyUtils;
import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.enums.MockResultType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * a temp solution for qmq consumer replay could not get the real replayId
 *
 * @author jmo
 * @since 2022/1/14
 */
@Component
@Slf4j
public final class RecordReplayMappingBuilder {
    @Resource
    private CacheProvider cacheProvider;
    @Value("${arex.storage.cache.expired.seconds:7200}")
    private int cacheExpiredSeconds;

    void putLastReplayResultId(MockCategoryType category, String recordId, String replayId) {
        try {
            cacheProvider.put(buildRecordMappingKey(category, recordId), cacheExpiredSeconds,
                    CacheKeyUtils.toUtf8Bytes(replayId));
        } catch (Throwable throwable) {
            LOGGER.error("putLastReplayResultId error:{},replayId:{}", throwable.getMessage(), replayId, throwable);
        }
    }

    public String lastReplayResultId(MockCategoryType category, String recordId) {
        try {
            return CacheKeyUtils.fromUtf8Bytes(cacheProvider.get(buildRecordMappingKey(category, recordId)));
        } catch (Throwable throwable) {
            LOGGER.error("lastReplayResultId error:{},recordId:{}", throwable.getMessage(), recordId, throwable);
        }
        return null;
    }

    private byte[] buildRecordMappingKey(MockCategoryType category, String recordId) {
        return CacheKeyUtils.buildSourceKey(MockResultType.RECORD_REPLAY_MAPPING,
                category, CacheKeyUtils.toUtf8Bytes(recordId));
    }
}
