package com.arextest.storage.service;

import com.arextest.common.cache.CacheProvider;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.trace.MDCTracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * listen agent service save/query storage service if exception mark record/replay case invalid
 * when recording or replaying, the data needs to be cleaned up or marked invalid due to incomplete data
 * @author: sldu
 * @date: 2023/12/6 13:46
 **/
@Component
@Slf4j
public class InvalidIncompleteRecordService {
    @Resource
    private CacheProvider redisCacheProvider;
    @Resource
    private MockSourceEditionService editableService;
    @Resource
    private ScheduledExecutorService coverageHandleDelayedPool;
    private static final byte[] INVALID_CASE_KEY = "invalid_case".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INVALID_CASE_VALUE = "1".getBytes(StandardCharsets.UTF_8);
    private static final long THREE_MINUTES_EXPIRE = 3 * 60L;
    private static final String NULL = "null";
    public void invalidIncompleteRecord(String recordId, String replayId) {
        // replaying scene
        if (StringUtils.isNotEmpty(replayId) && !NULL.equalsIgnoreCase(replayId)) {
            invalidReplayIncompleteRecords(replayId);
        } else {
            // recording scene
            invalidIncompleteRecords(recordId);
        }
    }
    public boolean isInvalidReplayIncompleteCase(String replayId) {
        if (StringUtils.isEmpty(replayId)) {
            return false;
        }
        byte[] key = toInvalidCaseKeyBytes(replayId);
        try {
            byte[] bytes = redisCacheProvider.get(key);
            if (bytes != null) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("[[title=invalidIncompleteRecord]]invalid replay case replayId:{} exception message:{}",
                    replayId, e.getMessage());
        }
        return false;
    }
    private void invalidIncompleteRecords(String recordId) {
        if (StringUtils.isEmpty(recordId)) {
            return;
        }
        InvalidTask invalidTask = new InvalidTask(recordId);
        coverageHandleDelayedPool.schedule(invalidTask, 1, TimeUnit.MINUTES);
    }
    private void invalidReplayIncompleteRecords(String replayId) {
        if (StringUtils.isEmpty(replayId)) {
            return;
        }
        byte[] key = toInvalidCaseKeyBytes(replayId);
        redisCacheProvider.put(key, THREE_MINUTES_EXPIRE, INVALID_CASE_VALUE);
        LOGGER.info("[[title=invalidIncompleteRecord]]invalid replay case replayId:{}", replayId);
    }
    private byte[] toInvalidCaseKeyBytes(String replayId) {
        byte[] paramKey = replayId.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(INVALID_CASE_KEY.length + paramKey.length).put(INVALID_CASE_KEY).put(paramKey).array();
    }

    private class InvalidTask implements Runnable {

        private final String recordId;

        public InvalidTask(String requestType) {
            this.recordId = requestType;
        }

        @Override
        public void run() {
            MDCTracer.addRecordId(recordId);
            try {
                // recordId is not empty, means this is a record case
                editableService.removeAll(ProviderNames.DEFAULT, recordId);
            } catch (Exception exception) {
                LOGGER.error("[[title=invalidIncompleteRecord]]invalid remove all case recordId:{} exception message:{}",
                        recordId, exception.getMessage());
            } finally {
                MDCTracer.clear();
            }
        }
    }
}
