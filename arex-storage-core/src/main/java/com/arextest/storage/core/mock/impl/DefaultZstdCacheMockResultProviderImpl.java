package com.arextest.storage.core.mock.impl;

import com.arextest.storage.core.mock.MockKeyBuilder;
import com.arextest.storage.core.mock.MockResultProvider;
import com.arextest.storage.core.serialization.ZstdJacksonSerializer;
import com.arextest.common.cache.CacheProvider;
import com.arextest.common.utils.CompressionUtils;
import com.arextest.storage.core.cache.CacheKeyUtils;
import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.MockItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author jmo
 * @since 2021/11/8
 */
@Component
@Slf4j
final class DefaultZstdCacheMockResultProviderImpl implements MockResultProvider {
    /**
     * default 2h expired
     */
    @Value("${arex.storage.cache.expired.seconds:7200}")
    private long cacheExpiredSeconds;
    private static final int EMPTY_SIZE = 0;
    @Resource
    private CacheProvider redisCacheProvider;
    @Resource
    private ZstdJacksonSerializer serializer;
    @Resource
    private MockKeyBuilder mockKeyBuilder;

    @Override
    public <T extends MockItem> boolean putRecordResult(MockCategoryType category, String recordId,
                                                        Iterable<T> values) {
        final byte[] recordIdBytes = CacheKeyUtils.toUtf8Bytes(recordId);
        Iterator<T> valueIterator = values.iterator();
        int size = 0;
        byte[] recordKey = CacheKeyUtils.buildRecordKey(category, recordIdBytes);
        while (valueIterator.hasNext()) {
            final T value = valueIterator.next();
            List<byte[]> mockKeyList = mockKeyBuilder.build(value);
            final byte[] zstdValue = serializer.serialize(value);
            byte[] valueRefKey = sequencePut(recordKey, zstdValue);
            if (valueRefKey == null) {
                continue;
            }
            for (int i = 0; i < mockKeyList.size(); i++) {
                byte[] mockKeyBytes = mockKeyList.get(i);
                byte[] key = CacheKeyUtils.buildRecordKey(category, recordIdBytes, mockKeyBytes);
                byte[] sequenceKey = sequencePut(key, valueRefKey);
                if (sequenceKey != null) {
                    size++;
                }
            }
        }
        LOGGER.info("put record result to cache size:{} for category:{},record id:{}", size, category, recordId);
        return size > EMPTY_SIZE;
    }

    @Override
    public boolean removeRecordResult(MockCategoryType category, String recordId) {
        final int removed;
        byte[] recordCountKey = CacheKeyUtils.buildRecordKey(category, recordId);
        removed = removeResult(recordCountKey);
        LOGGER.info("remove record result size:{} for category:{},record id:{}", removed, category, recordId);
        return removed > EMPTY_SIZE;
    }

    @Override
    public boolean removeReplayResult(MockCategoryType category, String replayResultId) {
        if (StringUtils.isEmpty(replayResultId)) {
            return false;
        }
        final byte[] replayCountKey = CacheKeyUtils.buildReplayKey(category, replayResultId);
        int removed = removeResult(replayCountKey);
        return removed > EMPTY_SIZE;
    }

    private int removeResult(final byte[] resultCountKey) {
        int removed = EMPTY_SIZE;
        int replayCount = resultCount(resultCountKey);
        if (replayCount > EMPTY_SIZE) {
            redisCacheProvider.remove(resultCountKey);
        }
        for (int sequence = 1; sequence <= replayCount; sequence++) {
            final byte[] resultSequenceKey = createSequenceKey(resultCountKey, sequence);
            if (redisCacheProvider.remove(resultSequenceKey)) {
                removed++;
            }
        }
        return removed;
    }

    @Override
    public <T extends MockItem> boolean putReplayResult(MockCategoryType category, String replayResultId, T value) {
        byte[] zstdValue = serializer.serialize(value);
        final byte[] key = CacheKeyUtils.buildReplayKey(category, replayResultId);
        boolean success = sequencePut(key, zstdValue) != null;
        LOGGER.info("put replay result:{} for category:{},result id:{}", success, category, replayResultId);
        return success;
    }

    private int nextSequence(byte[] key) {
        long count = redisCacheProvider.incrValue(key);
        redisCacheProvider.expire(key, cacheExpiredSeconds);
        return (int) count;
    }

    private byte[] sequencePut(final byte[] key, final byte[] zstdValue) {
        int next = 0;
        try {
            next = nextSequence(key);
            final byte[] sequenceKey = createSequenceKey(key, next);
            boolean retResult = redisCacheProvider.put(sequenceKey, cacheExpiredSeconds, zstdValue);
            if (retResult) {
                return sequenceKey;
            }
        } catch (Throwable throwable) {
            LOGGER.error("put error:{} sequence:{} for base64 key:{}",
                    throwable.getMessage(), next, CompressionUtils.encodeToBase64String(key), throwable);
        }
        return null;
    }

    /**
     * sequence query for record result,if consume overhead the total,we use last one instead as return.
     *
     * @param category The category of mocker's resource
     * @param mockItem The record id from agent produced
     * @return compressed bytes with zstd
     */
    @Override
    public byte[] getRecordResult(MockCategoryType category, @NotNull MockItem mockItem) {
        String recordId = mockItem.getRecordId();
        String replayId = mockItem.getReplayId();
        try {
            long start = System.currentTimeMillis();
            List<byte[]> mockKeyList = mockKeyBuilder.build(mockItem);
            long end = System.currentTimeMillis();
            LOGGER.info("build mock keys cost:{} ms", end - start);
            if (CollectionUtils.isEmpty(mockKeyList)) {
                LOGGER.warn("build empty mock keys,skip mock result query,recordId:{},replayId:{}", recordId, replayId);
                return null;
            }
            final byte[] recordIdBytes = CacheKeyUtils.toUtf8Bytes(recordId);
            final byte[] replayIdBytes = CacheKeyUtils.toUtf8Bytes(replayId);
            byte[] result;
            byte[] mockKeyBytes;
            int mockKeySize = mockKeyList.size();
            for (int i = 0; i < mockKeySize; i++) {
                mockKeyBytes = mockKeyList.get(i);
                result = getRecordResult(category, recordIdBytes, replayIdBytes, mockKeyBytes);
                if (result != null) {
                    return result;
                }
            }
        } catch (Throwable throwable) {
            LOGGER.error("from agent's sequence consumeResult error:{} for category:{},recordId:{},replayId:{}",
                    throwable.getMessage(),
                    category,
                    recordId, replayId, throwable);
        }
        return null;
    }

    private byte[] getRecordResult(MockCategoryType category, final byte[] recordIdBytes, byte[] replayIdBytes,
                                   final byte[] mockKeyBytes) {
        try {
            byte[] sourceKey = CacheKeyUtils.buildRecordKey(category, recordIdBytes, mockKeyBytes);
            int count = resultCount(sourceKey);
            if (count == EMPTY_SIZE) {
                return null;
            }
            byte[] consumeSource = CacheKeyUtils.buildConsumeKey(category, recordIdBytes, replayIdBytes,
                    mockKeyBytes);
            int sequence = nextSequence(consumeSource);
            if (sequence > count) {
                LOGGER.info("overhead consume record result,try use last one instead it,current sequence:{},count:{}"
                        , sequence, count);
                sequence = count;
            }
            byte[] consumeSequenceKey = createSequenceKey(sourceKey, sequence);
            byte[] valueRefKey = redisCacheProvider.get(consumeSequenceKey);
            if (valueRefKey != null) {
                return redisCacheProvider.get(valueRefKey);
            }
        } catch (Throwable throwable) {
            LOGGER.error("from agent's sequence consumeResult error:{} for category:{}",
                    throwable.getMessage(),
                    category, throwable);
        }
        return null;
    }

    @Override
    public List<byte[]> getRecordResultList(MockCategoryType category, String recordId) {
        byte[] recordCountKey = CacheKeyUtils.buildRecordKey(category, recordId);
        return getResultList(recordCountKey);
    }

    private List<byte[]> getResultList(byte[] resultCountKey) {
        int size = resultCount(resultCountKey);
        if (size == EMPTY_SIZE) {
            return Collections.emptyList();
        }
        final List<byte[]> recordResult = new ArrayList<>(size);
        for (int sequence = 1; sequence <= size; sequence++) {
            byte[] sequenceKey = createSequenceKey(resultCountKey, sequence);
            byte[] values = redisCacheProvider.get(sequenceKey);
            if (values != null) {
                recordResult.add(values);
            }
        }
        return recordResult;
    }

    @Override
    public List<byte[]> getReplayResultList(MockCategoryType category, String replayResultId) {
        byte[] replayKey = CacheKeyUtils.buildReplayKey(category, replayResultId);
        return getResultList(replayKey);
    }

    @Override
    public int replayResultCount(MockCategoryType category, String replayResultId) {
        return resultCount(CacheKeyUtils.buildReplayKey(category, replayResultId));
    }

    @Override
    public int recordResultCount(MockCategoryType category, String recordId) {
        return resultCount(CacheKeyUtils.buildRecordKey(category, recordId));
    }

    private int resultCount(byte[] countKey) {
        byte[] totalBytes = redisCacheProvider.get(countKey);
        if (totalBytes == null) {
            return EMPTY_SIZE;
        }
        String totalText = new String(totalBytes);
        return StringUtils.isEmpty(totalText) ? EMPTY_SIZE : Integer.parseInt(totalText);
    }

    private byte[] createSequenceKey(byte[] src, int sequence) {
        return CacheKeyUtils.merge(src, sequence);
    }
}
