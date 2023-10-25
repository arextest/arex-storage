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
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
final class DefaultMockResultProviderImpl implements MockResultProvider {

  private static final int EMPTY_SIZE = 0;
  private static final String CALL_REPLAY_MAX = "callReplayMax";
  private static final String DUBBO_PREFIX = "Dubbo";
  /**
   * default 2h expired
   */
  @Value("${arex.storage.cache.expired.seconds:7200}")
  private long cacheExpiredSeconds;
  @Resource
  private CacheProvider redisCacheProvider;
  @Resource
  private ZstdJacksonSerializer serializer;
  @Resource
  private MatchKeyFactory matchKeyFactory;

  /**
   * 1. Store recorded data and matching keys in redis 2. The mock type associated with dubbo, which
   * needs to record the maximum number of replays
   */
  @Override
  public <T extends Mocker> boolean putRecordResult(MockCategoryType category, String recordId,
      Iterable<T> values) {
    final byte[] recordIdBytes = CacheKeyUtils.toUtf8Bytes(recordId);
    Iterator<T> valueIterator = values.iterator();
    int size = 0;
    byte[] recordKey = CacheKeyUtils.buildRecordKey(category, recordIdBytes);

    boolean shouldRecordCallReplayMax = shouldRecordCallReplayMax(category);
    // Records the maximum number of operations corresponding to recorded data
    if (shouldRecordCallReplayMax) {
      List<T> mockList = new ArrayList<>();
      // Obtain the number of the same interfaces in recorded data
      while (valueIterator.hasNext()) {
        T value = valueIterator.next();
        byte[] recordOperationKey = CacheKeyUtils.buildRecordOperationKey(category, recordId,
            value.getOperationName());
        nextSequence(recordOperationKey);
        mockList.add(value);
      }

      for (T value : mockList) {
        // Place the maximum number of playback times corresponding to the operations into the recorded data
        byte[] recordOperationKey = CacheKeyUtils.buildRecordOperationKey(category, recordId,
            value.getOperationName());
        int count = resultCount(recordOperationKey);
        Mocker.Target targetResponse = value.getTargetResponse();
        if (targetResponse != null) {
          targetResponse.setAttribute(CALL_REPLAY_MAX, count);
        }
        size = sequencePutRecordData(category, recordIdBytes, size, recordKey, value);
      }
    } else {
      while (valueIterator.hasNext()) {
        T value = valueIterator.next();
        size = sequencePutRecordData(category, recordIdBytes, size, recordKey, value);
      }
    }
    LOGGER.info("put record result to cache size:{} for category:{},record id:{}", size, category,
        recordId);
    return size > EMPTY_SIZE;
  }

  private <T extends Mocker> int sequencePutRecordData(MockCategoryType category,
      byte[] recordIdBytes, int size, byte[] recordKey, T value) {
    List<byte[]> mockKeyList = matchKeyFactory.build(value);
    final byte[] zstdValue = serializer.serialize(value);
    byte[] valueRefKey = sequencePut(recordKey, zstdValue);
    if (valueRefKey == null) {
      return size;
    }
    for (int i = 0; i < mockKeyList.size(); i++) {
      byte[] mockKeyBytes = mockKeyList.get(i);
      byte[] key = CacheKeyUtils.buildRecordKey(category, recordIdBytes, mockKeyBytes);
      byte[] sequenceKey = sequencePut(key, valueRefKey);
      if (sequenceKey != null) {
        size++;
      }
    }
    // if category type is the type to be compared.associate the mock instance id with the related mock key.
    if (shouldUseIdOfInstanceToMockResult(category)) {
      putRecordInstanceId(valueRefKey, value.getId());
      putMockKeyListWithInstanceId(value.getId(), mockKeyList);
    }
    return size;
  }

  private boolean shouldUseIdOfInstanceToMockResult(MockCategoryType category) {
    return !category.isEntryPoint() && !category.isSkipComparison();
  }

  private boolean shouldRecordCallReplayMax(MockCategoryType category) {
    return shouldUseIdOfInstanceToMockResult(category) && category.getName()
        .startsWith(DUBBO_PREFIX);
  }

  @Override
  public boolean removeRecordResult(MockCategoryType category, String recordId) {
    final int removed;
    byte[] recordCountKey = CacheKeyUtils.buildRecordKey(category, recordId);
    removed = removeResult(recordCountKey);
    LOGGER.info("remove record result size:{} for category:{},record id:{}", removed, category,
        recordId);
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
    LOGGER.info("put replay result:{} for category:{},result id:{}", success, category,
        replayResultId);
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
   * sequence query for record result,if consume overhead the total,we use last one instead as
   * return.
   * <p>
   * if the accurate matches the data, let the fuzzy key related to the mock data increase. vice
   * versa.
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
        LOGGER.warn("build empty mock keys,skip mock result query,recordId:{},replayId:{}",
            recordId, replayId);
        return null;
      }
      final byte[] recordIdBytes = CacheKeyUtils.toUtf8Bytes(recordId);
      final byte[] replayIdBytes = CacheKeyUtils.toUtf8Bytes(replayId);
      byte[] result = null;
      byte[] mockResultId = null;
      byte[] mockKeyBytes;
      int mockResultIndex = 0;
      int mockKeySize = mockKeyList.size();
      boolean useInstanceIdToMockResult = shouldUseIdOfInstanceToMockResult(category);
      boolean strictMatch = context.getMockStrategy() == MockResultMatchStrategy.STRICT_MATCH;

      for (int i = 0; i < mockKeySize; i++) {
        mockKeyBytes = mockKeyList.get(i);
        result = sequenceMockResult(category, recordIdBytes, replayIdBytes, mockKeyBytes, context);
        // if mock result match strategy is strict match, need get full parameter match data.
        if (strictMatch) {
          return result;
        }

        if (result != null) {
          if (useInstanceIdToMockResult) {
            // associate the matched mock id with the related replay data.
            mockResultId = getIdOfRecordInstance(context.getValueRefKey());
            mockItem.setId(CacheKeyUtils.fromUtf8Bytes(mockResultId));
            LOGGER.info(
                "get record result from record instance id :{}, operation :{}, accurateMatch :{}, match key index :{}",
                CacheKeyUtils.fromUtf8Bytes(mockResultId), mockItem.getOperationName(), i == 0, i);
          }
          mockResultIndex = i;
          break;
        }
      }

      if (result != null && useInstanceIdToMockResult) {
        updateConsumeSequence(category, recordIdBytes, replayIdBytes, mockResultId, mockResultIndex,
            mockKeySize);
      }

      return result;
    } catch (Throwable throwable) {
      LOGGER.error(
          "from agent's sequence consumeResult error:{} for category:{},recordId:{},replayId:{}",
          throwable.getMessage(),
          category,
          recordId, replayId, throwable);
    }
    return null;
  }

  /**
   * Treat both full parameter and fuzzy matching of recorded data as a set of data, with one key
   * being consumed and the other keys also consumed
   * <p>
   * follows: 1. if the fuzzy match gets the data, just let other key match increase. 2. if the full
   * parameter match gets the data, only needs to increase the fuzzy match.
   */
  private void updateConsumeSequence(MockCategoryType category, byte[] recordIdBytes,
      byte[] replayIdBytes,
      byte[] mockResultId, int mockResultIndex, int mockKeySize) {
    for (int i = 0; i < mockKeySize; i++) {
      if (mockResultIndex == i) {
        continue;
      }
      byte[] mockKeyWithInstanceId = getMockKeyListWithInstanceId(mockResultId, i);
      byte[] consumeSource = CacheKeyUtils.buildConsumeKey(category, recordIdBytes, replayIdBytes,
          mockKeyWithInstanceId);
      nextSequence(consumeSource);
    }
  }

  private byte[] sequenceMockResult(MockCategoryType category, final byte[] recordIdBytes,
      byte[] replayIdBytes,
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
      boolean tryFindLastValue =
          MockResultMatchStrategy.TRY_FIND_LAST_VALUE == context.getMockStrategy();
      context.setLastOfResult(sequence > count);
      if (context.isLastOfResult() && tryFindLastValue) {
        LOGGER.info(
            "overhead consume record result,try use last one instead it,current sequence:{},count:{}"
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

  /**
   * associate the mock instance id with the related mock key.
   */
  private void putMockKeyListWithInstanceId(String id, List<byte[]> mockKeyList) {
    for (int i = 0; i < mockKeyList.size(); i++) {
      byte[] mockKeyWithInstanceIdKey = createMockKeyWithInstanceIdKey(
          CacheKeyUtils.toUtf8Bytes(id), i);
      redisCacheProvider.put(mockKeyWithInstanceIdKey, cacheExpiredSeconds, mockKeyList.get(i));
    }
  }

  private byte[] getMockKeyListWithInstanceId(byte[] mockResultId, int index) {
    byte[] mockKeyWithInstanceIdKey = createMockKeyWithInstanceIdKey(mockResultId, index);
    return redisCacheProvider.get(mockKeyWithInstanceIdKey);
  }

  private byte[] getIdOfRecordInstance(byte[] valueRefKey) {
    final byte[] recordInstanceIdKey = createRecordInstanceIdKey(valueRefKey);
    return redisCacheProvider.get(recordInstanceIdKey);
  }

  private byte[] createRecordInstanceIdKey(byte[] src) {
    return CacheKeyUtils.merge(src, MockResultType.RECORD_INSTANCE_ID.getCodeValue());
  }

  private byte[] createMockKeyWithInstanceIdKey(byte[] src, int index) {
    return CacheKeyUtils.merge(src, index);
  }
}