package com.arextest.storage.mock.impl;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.utils.CompressionUtils;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MatchKeyFactory;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.mock.MockResultMatchStrategy;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.model.MockResultType;
import com.arextest.storage.model.RecordStatusType;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;


@Component
@Slf4j
final class DefaultMockResultProviderImpl implements MockResultProvider {
    /**
     * default 2h expired
     */
    @Value("${arex.storage.cache.expired.seconds:7200}")
    private long cacheExpiredSeconds;
    private static final int EMPTY_SIZE = 0;
    private static final int SINGLE_RECORD_SIZE = 1;
    @Resource
    private CacheProvider redisCacheProvider;
    @Resource
    private ZstdJacksonSerializer serializer;
    @Resource
    private MatchKeyFactory matchKeyFactory;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public <T extends Mocker> boolean putRecordResult(MockCategoryType category, String recordId, Iterable<T> values) {
        final byte[] recordIdBytes = CacheKeyUtils.toUtf8Bytes(recordId);
        Iterator<T> valueIterator = values.iterator();
        int size = 0;
        byte[] recordKey = CacheKeyUtils.buildRecordKey(category, recordIdBytes);
        List<RecordInstanceData> unusedRecordInstanceList = new ArrayList<>();
        while (valueIterator.hasNext()) {
            final T value = valueIterator.next();
            List<byte[]> mockKeyList = matchKeyFactory.build(value);
            final byte[] zstdValue = serializer.serialize(value);
            byte[] valueRefKey = sequencePut(recordKey, zstdValue);
            if (valueRefKey == null) {
                continue;
            }
            if (shouldUseIdOfInstanceToMockResult(category)) {
                putRecordInstanceId(valueRefKey, value.getId());
                unusedRecordInstanceList.add(
                        new RecordInstanceData(CacheKeyUtils.toUtf8Bytes(value.getId()), RecordStatusType.UNUSED.getCodeValue(),
                                value.getOperationName(), zstdValue));
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
        if (shouldUseIdOfInstanceToMockResult(category) && CollectionUtils.isNotEmpty(unusedRecordInstanceList)) {
            putRecordInstanceData(category, recordIdBytes, unusedRecordInstanceList);
        }
        LOGGER.info("put record result to cache size:{} for category:{},record id:{}", size, category, recordId);
        return size > EMPTY_SIZE;
    }

    private void putRecordInstanceData(MockCategoryType category, byte[] recordIdBytes, List<RecordInstanceData> unusedRecordInstanceList) {
        byte[] unusedRecordInstanceKey = getRecordInstanceDataKey(category, recordIdBytes);
        byte[] value = null;
        try {
            value = serializer.serialize(objectMapper.writeValueAsString(unusedRecordInstanceList));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        redisCacheProvider.put(unusedRecordInstanceKey, cacheExpiredSeconds, value);
    }

    private byte[] getRecordInstanceDataKey(MockCategoryType category, byte[] recordIdBytes) {
        return CacheKeyUtils.buildSourceKey(MockResultType.RECORD_INSTANCE_ID, category, recordIdBytes);
    }

    private byte[] getRecordInstanceData(MockCategoryType category, String recordId) {
        byte[] recordInstanceDataKey = getRecordInstanceDataKey(category, CacheKeyUtils.toUtf8Bytes(recordId));
        return redisCacheProvider.get(recordInstanceDataKey);
    }

    @Override
    public void setRecordStatus(MockCategoryType categoryType, String recordId, int status) {
        byte[] recordInstanceData = getRecordInstanceData(categoryType, recordId);
        List<RecordInstanceData> allRecordInstanceData = serializer.deserializeToList(recordInstanceData, RecordInstanceData.class);
        if (CollectionUtils.isNotEmpty(allRecordInstanceData)) {
            allRecordInstanceData.forEach(data -> {
                data.setStatus(status);
            });
        }
        putRecordInstanceData(categoryType, CacheKeyUtils.toUtf8Bytes(recordId), allRecordInstanceData);
    }

    private byte[] getRecordInstanceData(MockCategoryType category, byte[] recordIdBytes) {
        byte[] recordInstanceDataKey = getRecordInstanceDataKey(category, recordIdBytes);
        return redisCacheProvider.get(recordInstanceDataKey);
    }

    private boolean shouldUseIdOfInstanceToMockResult(MockCategoryType category) {
        return !category.isEntryPoint() && !category.isSkipComparison();
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
    public <T extends Mocker> boolean putReplayResult(T value) {
        MockCategoryType category = value.getCategoryType();
        String replayResultId = value.getReplayId();
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
     * @param mockItem The record id from agent produced
     * @return compressed bytes with zstd
     */
    @Override
    public byte[] getRecordResult(@NotNull Mocker mockItem, MockResultContext context) {
        MockCategoryType category = mockItem.getCategoryType();
        String recordId = mockItem.getRecordId();
        String replayId = mockItem.getReplayId();
        try {
            long start = System.currentTimeMillis();
            List<byte[]> mockKeyList = matchKeyFactory.build(mockItem);
            long end = System.currentTimeMillis();
            LOGGER.info("build mock keys cost:{} ms", end - start);
            if (CollectionUtils.isEmpty(mockKeyList)) {
                LOGGER.warn("build empty mock keys,skip mock result query,recordId:{},replayId:{}", recordId, replayId);
                return null;
            }
            final byte[] recordIdBytes = CacheKeyUtils.toUtf8Bytes(recordId);
            final byte[] replayIdBytes = CacheKeyUtils.toUtf8Bytes(replayId);
            byte[] result;
            byte[] firstResult = null;
            byte[] mockKeyBytes;
            List<RecordInstanceData> recordInstanceList = getRecordDataFromZstd(mockItem, category, recordIdBytes);
            int mockKeySize = mockKeyList.size();
            boolean strictMatch = context.getMockStrategy() == MockResultMatchStrategy.STRICT_MATCH;
            for (int i = 0; i < mockKeySize; i++) {
                mockKeyBytes = mockKeyList.get(i);
                result = sequenceMockResult(category, recordIdBytes, replayIdBytes, mockKeyBytes, context);
                if (firstResult != null) {
                    break;
                }
                if (strictMatch || result != null) {
                    if (shouldUseIdOfInstanceToMockResult(category)) {
                        byte[] id = getIdOfRecordInstance(context.getValueRefKey());
                        mockItem.setId(CacheKeyUtils.fromUtf8Bytes(id));
                        if (CollectionUtils.isEmpty(recordInstanceList)) {
                            break;
                        }
                        Optional<RecordInstanceData> instanceOptional =
                                recordInstanceList.stream().filter(data -> Arrays.equals(id, data.getInstanceId())).findFirst();
                        if (instanceOptional.isPresent()) {
                            RecordInstanceData instanceData = instanceOptional.get();
                            if (instanceData.used()) {
                                Optional<RecordInstanceData> unusedInstanceOptional =
                                        recordInstanceList.stream().filter(data -> !data.used()).findFirst();
                                if (unusedInstanceOptional.isPresent()) {
                                    RecordInstanceData unusedInstanceData = unusedInstanceOptional.get();
                                    firstResult = unusedInstanceData.getRecordData();
                                    unusedInstanceData.setStatus(RecordStatusType.USED.getCodeValue());
                                }
                            } else {
                                instanceData.setStatus(RecordStatusType.USED.getCodeValue());
                            }
                            putRecordInstanceData(category, recordIdBytes, recordInstanceList);
                        }
                        LOGGER.info("get record result from record instance id :{}", CacheKeyUtils.fromUtf8Bytes(id));
                    }
                    if (firstResult == null) {
                        firstResult = result;
                    }
                }
            }
            return firstResult;
        } catch (Throwable throwable) {
            LOGGER.error("from agent's sequence consumeResult error:{} for category:{},recordId:{},replayId:{}",
                    throwable.getMessage(),
                    category,
                    recordId, replayId, throwable);
        }
        return null;
    }

    private List<RecordInstanceData> getRecordDataFromZstd(Mocker mockItem, MockCategoryType category, byte[] recordIdBytes) {
        List<RecordInstanceData> recordInstanceList = null;
        if (shouldUseIdOfInstanceToMockResult(category)) {
            byte[] recordInstanceData = getRecordInstanceData(category, recordIdBytes);
            List<RecordInstanceData> allRecordInstanceData = serializer.deserializeToList(recordInstanceData, RecordInstanceData.class);
            recordInstanceList = allRecordInstanceData.stream().filter(data -> StringUtils.equals(data.getOperationName(),
                    mockItem.getOperationName())).collect(Collectors.toList());
        }
        return recordInstanceList;
    }

    private byte[] sequenceMockResult(MockCategoryType category, final byte[] recordIdBytes, byte[] replayIdBytes,
                                      final byte[] mockKeyBytes, MockResultContext context) {
        try {
            byte[] sourceKey = CacheKeyUtils.buildRecordKey(category, recordIdBytes, mockKeyBytes);
            int count = resultCount(sourceKey);
            if (count == EMPTY_SIZE) {
                return null;
            }
            byte[] consumeSource = CacheKeyUtils.buildConsumeKey(category, recordIdBytes, replayIdBytes,
                    mockKeyBytes);
            int sequence = nextSequence(consumeSource);
            boolean tryFindLastValue = MockResultMatchStrategy.TRY_FIND_LAST_VALUE == context.getMockStrategy();
            context.setLastOfResult(sequence > count);
            if (context.isLastOfResult() && tryFindLastValue) {
                LOGGER.info("overhead consume record result,try use last one instead it,current sequence:{},count:{}"
                        , sequence, count);
                sequence = count;
            }
            byte[] consumeSequenceKey = createSequenceKey(sourceKey, sequence);
            byte[] valueRefKey = redisCacheProvider.get(consumeSequenceKey);
            if (valueRefKey != null) {
                context.setValueRefKey(valueRefKey);
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
            byte[] value = redisCacheProvider.get(sequenceKey);
            if (value != null) {
                recordResult.add(value);
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

    private void putRecordInstanceId(byte[] valueRefKey, String id) {
        final byte[] recordInstanceIdKey = createRecordInstanceIdKey(valueRefKey);
        redisCacheProvider.put(recordInstanceIdKey, cacheExpiredSeconds, CacheKeyUtils.toUtf8Bytes(id));
    }

    private byte[] getIdOfRecordInstance(byte[] valueRefKey) {
        final byte[] recordInstanceIdKey = createRecordInstanceIdKey(valueRefKey);
        return redisCacheProvider.get(recordInstanceIdKey);
    }

    private byte[] createRecordInstanceIdKey(byte[] src) {
        return CacheKeyUtils.merge(src, MockResultType.RECORD_INSTANCE_ID.getCodeValue());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static final class RecordInstanceData {
        private byte[] instanceId;
        /**
         * @see com.arextest.storage.model.RecordStatusType
         */
        private int status;
        private String operationName;
        private byte[] recordData;

        public boolean used() {
            return status == RecordStatusType.USED.getCodeValue();
        }
    }
}