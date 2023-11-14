package com.arextest.storage.mock.impl;

import com.arextest.common.cache.CacheProvider;
import com.arextest.common.utils.CompressionUtils;
import com.arextest.model.constants.MockAttributeNames;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.metric.MatchStrategyMetricService;
import com.arextest.storage.mock.MatchKeyFactory;
import com.arextest.storage.mock.MockResultContext;
import com.arextest.storage.mock.MockResultMatchStrategy;
import com.arextest.storage.mock.MockResultProvider;
import com.arextest.storage.mock.internal.matchkey.impl.DatabaseMatchKeyBuilderImpl;
import com.arextest.storage.model.MockResultType;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
final class DefaultMockResultProviderImpl implements MockResultProvider {

  private static final int EMPTY_SIZE = 0;
  private static final String CALL_REPLAY_MAX = "callReplayMax";
  private static final String DUBBO_PREFIX = "Dubbo";
  private static final String STRICT_MATCH = "strictMatch";
  private static final String MULTI_OPERATION_WITH_STRICT_MATCH = "multiOperationStrictMatch";
  private static final String FUZZY_MATCH = "fuzzyMatch";
  private static final String SIMILARITY_MATCH = "similarityMatch";
  private static final String COMMA_STRING = ",";
  /**
   * default 2h expired
   */
  @Value("${arex.storage.cache.expired.seconds:7200}")
  private long cacheExpiredSeconds;
  @Value("${arex.storage.not.use.similarity.strategy.appIds}")
  private String notUseSimilarityStrategyAppIds;
  @Resource
  private CacheProvider redisCacheProvider;
  @Resource
  private ZstdJacksonSerializer serializer;
  @Resource
  private MatchKeyFactory matchKeyFactory;
  @Resource
  private MatchStrategyMetricService matchStrategyMetricService;
  @Resource
  private DatabaseMatchKeyBuilderImpl databaseMatchKeyBuilder;

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
    boolean shouldUseIdOfInstanceToMockResult = shouldUseIdOfInstanceToMockResult(category);
    boolean shouldRecordCallReplayMax = shouldRecordCallReplayMax(category);

    // Records the maximum number of operations corresponding to recorded data
    if (shouldUseIdOfInstanceToMockResult) {
      List<T> mockList = new ArrayList<>();
      // Obtain the number of the same interfaces in recorded data
      while (valueIterator.hasNext()) {
        T value = valueIterator.next();
        mockList.add(value);
        if (shouldBuildRecordOperationKey(value) || shouldRecordCallReplayMax) {
          byte[] recordOperationKey = CacheKeyUtils.buildRecordOperationKey(category, recordId,
              getOperationNameWithCategory(value, category));
          nextSequence(recordOperationKey);
        }
      }

      for (T value : mockList) {
        // Place the maximum number of playback times corresponding to the operations into the recorded data
        if (shouldRecordCallReplayMax) {
          byte[] recordOperationKey = CacheKeyUtils.buildRecordOperationKey(category, recordId,
              value.getOperationName());
          int count = resultCount(recordOperationKey);
          Mocker.Target targetResponse = value.getTargetResponse();
          if (targetResponse != null) {
            targetResponse.setAttribute(CALL_REPLAY_MAX, count);
          }
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
    return !category.isEntryPoint();
  }

  private boolean shouldBuildRecordOperationKey(Mocker mocker) {
    if (StringUtils.isEmpty(notUseSimilarityStrategyAppIds)) {
      return true;
    }
    String[] appIds = notUseSimilarityStrategyAppIds.split(COMMA_STRING);
    return !Arrays.asList(appIds).contains(mocker.getAppId());
  }

  private boolean shouldRecordCallReplayMax(MockCategoryType category) {
    return shouldUseIdOfInstanceToMockResult(category) && (category.getName()
        .startsWith(DUBBO_PREFIX) || category.equals(MockCategoryType.DYNAMIC_CLASS));
  }

  @Override
  public <T extends Mocker> boolean removeRecordResult(MockCategoryType category, String recordId, Iterable<T> values) {
    int removed = EMPTY_SIZE;
    final byte[] recordIdBytes = CacheKeyUtils.toUtf8Bytes(recordId);
    byte[] recordCountKey = CacheKeyUtils.buildRecordKey(category, recordId);

    Iterator<T> valueIterator = values.iterator();
    while (valueIterator.hasNext()) {
      T value = valueIterator.next();
      byte[] recordOperationKey = CacheKeyUtils.buildRecordOperationKey(category, recordId,
          getOperationNameWithCategory(value, category));
      redisCacheProvider.remove(recordOperationKey);
      removed += removeResult(category, recordIdBytes, value);
    }

    int replayCount = resultCount(recordCountKey);
    if (replayCount > EMPTY_SIZE) {
      redisCacheProvider.remove(recordCountKey);
      for (int sequence = 1; sequence <= replayCount; sequence++) {
        final byte[] resultSequenceKey = createSequenceKey(recordCountKey, sequence);
        if (redisCacheProvider.remove(resultSequenceKey)) {
          removed++;
        }
      }
    }

    LOGGER.info("remove record result size:{} for category:{},record id:{}", removed, category,
        recordId);
    return removed > EMPTY_SIZE;
  }

  @Override
  public boolean removeReplayResult(MockCategoryType category, String replayResultId) {
//    if (StringUtils.isEmpty(replayResultId)) {
//      return false;
//    }
//    final byte[] replayCountKey = CacheKeyUtils.buildReplayKey(category, replayResultId);
//    int removed = removeResult(replayCountKey);
//    return removed > EMPTY_SIZE;
    return true;
  }

  private <T extends Mocker> int removeResult(MockCategoryType category,
      byte[] recordIdBytes, T value) {
    int removed = EMPTY_SIZE;

    List<byte[]> mockKeyList = matchKeyFactory.build(value);
    for (int i = 0; i < mockKeyList.size(); i++) {
      byte[] mockKeyBytes = mockKeyList.get(i);
      byte[] key = CacheKeyUtils.buildRecordKey(category, recordIdBytes, mockKeyBytes);
      int resultCount = resultCount(key);
      if (resultCount <= EMPTY_SIZE) {
        continue;
      }
      for (int sequence = 1; sequence <= resultCount; sequence ++ ) {
        byte[] sequenceKey = createSequenceKey(key, sequence);
        if (redisCacheProvider.remove(sequenceKey)) {
          removed++;
        }
      }
      if (redisCacheProvider.remove(key)) {
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

      String operationName = getOperationNameWithCategory(mockItem, category);
      byte[] recordOperationKey = CacheKeyUtils.buildRecordOperationKey(category, recordId,
          operationName);
      int count = resultCount(recordOperationKey);
      LOGGER.info("get record result with operation:{}, count: {}", operationName, count);
      if (useSequenceMatch(context.getMockStrategy(), category, mockItem, count)) {
        return getMockResultWithSequenceMatch(mockItem, context, category, mockKeyList,
            recordIdBytes, replayIdBytes);
      }

      return getMockResultWithSimilarityMatch(category, recordIdBytes, replayIdBytes, mockKeyList,
          mockItem, operationName, context);
    } catch (Throwable throwable) {
      LOGGER.error(
          "from agent's sequence consumeResult error: {} for category: {}, recordId: {}, replayId: {}",
          throwable.getMessage(), category, recordId, replayId, throwable);
    }
    return null;
  }

  private boolean useSequenceMatch(MockResultMatchStrategy mockStrategy, MockCategoryType category,
      Mocker mockItem, int recordDataCount) {
    return category.isSkipComparison()
        || mockStrategy == MockResultMatchStrategy.STRICT_MATCH
        || recordDataCount <= 1 || !shouldBuildRecordOperationKey(mockItem);
  }

  private String getOperationNameWithCategory(Mocker mockItem, MockCategoryType category) {
    String operationName = mockItem.getOperationName();

    if (MockCategoryType.DATABASE.equals(category)) {
      String tableNames = databaseMatchKeyBuilder.findDBTableNames(mockItem);
      Object dbName = mockItem.getTargetRequest().getAttribute(MockAttributeNames.DB_NAME);
      operationName = String.format("%s_%s_%s", operationName, tableNames, dbName);
    }
    return operationName;
  }

  private byte[] getMockResultWithSequenceMatch(Mocker mockItem, MockResultContext context,
      MockCategoryType category, List<byte[]> mockKeyList, byte[] recordIdBytes,
      byte[] replayIdBytes) {
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
              "get record result from record instance id: {}, operation: {}, strict match: {}, match key index: {}",
              CacheKeyUtils.fromUtf8Bytes(mockResultId), mockItem.getOperationName(), i == 0, i);
        }
        matchStrategyMetricService.recordMatchingCount(i == 0 ? STRICT_MATCH : FUZZY_MATCH,
            (AREXMocker) mockItem);
        mockResultIndex = i;
        break;
      }
    }

    if (result != null && useInstanceIdToMockResult) {
      updateConsumeSequence(category, recordIdBytes, replayIdBytes, mockResultId, mockResultIndex,
          mockKeySize);
    }

    return result;
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

  /**
   * Calculates the sum of the lengths of the common prefix and the common suffix of two strings.
   *
   * @param val1
   * @param val2
   * @return the sum of the lengths of the common prefix and the common suffix of two strings
   */
  private int calc(String val1, String val2) {
    int len1 = 0, len2 = 0;
    int len = Math.min(val1.length(), val2.length());
    for (int i = 0; i < len; i++) {
      if (val1.charAt(i) != val2.charAt(i)) {
        len1 = i;
        break;
      }
    }

    for (int i = 1; i <= len; i++) {
      if (val1.charAt(val1.length() - i) != val2.charAt(val2.length() - i)) {
        len2 = i;
        break;
      }
    }
    return len1 + len2;
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

  /**
   * the matching result is obtained by similarity.
   *
   * @param category
   * @param recordIdBytes
   * @param replayIdBytes
   * @param mockKeyList
   * @param mockItem
   * @param operationName
   * @return the matching result.
   */
  private byte[] getMockResultWithSimilarityMatch(MockCategoryType category,
      final byte[] recordIdBytes, byte[] replayIdBytes,
      List<byte[]> mockKeyList, @NotNull Mocker mockItem, String operationName,
      MockResultContext context) {
    try {
      // 1. determine whether it can be accurately matched in multiple call scenarios
      byte[] result = sequenceMockResult(category, recordIdBytes, replayIdBytes, mockKeyList.get(0),
          context);

      // 2. the data on the exact match is returned directly
      if (result != null) {
        byte[] mockResultId = getIdOfRecordInstance(context.getValueRefKey());
        String id = CacheKeyUtils.fromUtf8Bytes(mockResultId);
        mockItem.setId(id);
        long increasesCount = increasesReplayConsumer(category, recordIdBytes, replayIdBytes, mockResultId);
        matchStrategyMetricService.recordMatchingCount(MULTI_OPERATION_WITH_STRICT_MATCH, (AREXMocker) mockItem);
        LOGGER.info(
            "[[title=similarityMatch]]get mock result with strictly match, instanceId: {}, increasesCount: {}",
            id, increasesCount);
        return result;
      }

      // 3. use similarity match
      byte[] fuzzMockKeyBytes = mockKeyList.get(mockKeyList.size() - 1);
      byte[] sourceKey = CacheKeyUtils.buildRecordKey(category, recordIdBytes, fuzzMockKeyBytes);
      int count = resultCount(sourceKey);
      if (count == EMPTY_SIZE) {
        return null;
      }
      boolean tryFindLastValue =
          context.getMockStrategy() == MockResultMatchStrategy.TRY_FIND_LAST_VALUE;

      // 4. gets replay request content.
      String replayRequestBody = getRequestBody(mockItem.getTargetRequest(), category);

      // 5. iterate over all records, calculating the similarity between replay requests and record requests.
      Map<Integer, List<Pair<String, byte[]>>> invocationMap = Maps.newHashMap();
      for (int sequence = 1; sequence <= count; sequence++) {
        byte[] mockDataBytes = getMockerDataBytesFromMockKey(sourceKey, sequence);
        if (mockDataBytes == null) {
          continue;
        }

        AREXMocker mocker = serializer.deserialize(mockDataBytes, AREXMocker.class);
        String recordInstanceId = mocker.getId();

        int consumerCount = getReplayConsumerCount(category, recordIdBytes, replayIdBytes, recordInstanceId);
        if (consumerCount > EMPTY_SIZE) {
          if (tryFindLastValue && sequence == count) {
            LOGGER.info(
                "[[title=similarityMatch]]try find last value, recordInstanceId: {}, consumerCount: {}",
                recordInstanceId, consumerCount);
            return mockDataBytes;
          }
          LOGGER.info("[[title=similarityMatch]]operation: {}, recordInstanceId: {} is matched.", operationName, recordInstanceId);
          continue;
        }

        String recordRequestBody = getRequestBody(mocker.getTargetRequest(), category);
        int sumLength = calc(replayRequestBody, recordRequestBody);

        LOGGER.info("[[title=similarityMatch]]recordInstanceId: {}, sumLength: {}",
            recordInstanceId, sumLength);
        Pair<String, byte[]> pair = ImmutablePair.of(recordInstanceId, mockDataBytes);
        if (MapUtils.isEmpty(invocationMap) || invocationMap.get(sumLength) == null) {
          List<Pair<String, byte[]>> pairs = Lists.newArrayListWithExpectedSize(1);
          pairs.add(pair);
          invocationMap.put(sumLength, pairs);
        } else {
          List<Pair<String, byte[]>> pairs = invocationMap.get(sumLength);
          pairs.add(pair);
          invocationMap.put(sumLength, pairs);
        }
      }

      if (MapUtils.isEmpty(invocationMap)) {
        return null;
      }

      // 6. sort the matching results by similarity.
      List<Integer> scores = new ArrayList<>(invocationMap.keySet());
      scores.sort((o1, o2) -> {
        if (o1.equals(o2)) {
          return 0;
        }
        return o2 - o1 > 0 ? 1 : -1;
      });

      // 7. get the matched recording result.
      for (Integer score: scores) {
        List<Pair<String, byte[]>> pairList = invocationMap.get(score);
        for (Pair<String, byte[]> pair : pairList) {
          String instanceId = pair.getLeft();
          if (StringUtils.isEmpty(instanceId)) {
            continue;
          }

          long increaseCount = increasesReplayConsumer(category, recordIdBytes, replayIdBytes,
              CacheKeyUtils.toUtf8Bytes(instanceId));
          if (increaseCount > 1) {
            LOGGER.info(
                "[[title=similarityMatch]]operation: {}, recordInstanceId: {} is matched.", operationName, instanceId);
            continue;
          }
          mockItem.setId(instanceId);
          LOGGER.info(
              "[[title=similarityMatch]]get mock result with similarity match, operation: {}, length: {}, matchedInstanceId: {}}",
              operationName, score, instanceId);
          // 7.1. buried point record the number of times similarity is used.
          matchStrategyMetricService.recordMatchingCount(SIMILARITY_MATCH, (AREXMocker) mockItem);
          return pair.getRight();
        }
      }
      return null;
    } catch (Throwable throwable) {
      LOGGER.error(
          "[[title=similarityMatch]]getMockResultWithSimilarityMatch error: {}, category:{}",
          throwable.getMessage(), category, throwable);
    }
    return null;
  }

  private int getReplayConsumerCount(MockCategoryType category, byte[] recordIdBytes, byte[] replayIdBytes,
      String recordInstanceId) {
    byte[] usedRecordInstanceIdsKey = buildMatchedRecordInstanceIdsKey(category, recordIdBytes,
        replayIdBytes, CacheKeyUtils.toUtf8Bytes(recordInstanceId));
    return resultCount(usedRecordInstanceIdsKey);
  }

  /**
   * Gets the request body content and returns null if the request body is empty. if it is a
   * database mock type, the request parameters are added to the request body.
   *
   * @param target
   * @param category
   * @return request body
   */
  private String getRequestBody(Mocker.Target target, MockCategoryType category) {
    if (target == null) {
      return null;
    }
    String body = target.getBody();
    if (MockCategoryType.DATABASE.equals(category)) {
      body += target.getAttribute(MockAttributeNames.DB_PARAMETERS);
    }
    return body;
  }

  private byte[] getMockerDataBytesFromMockKey(byte[] sourceKey, int sequence) {
    byte[] consumeSequenceKey = createSequenceKey(sourceKey, sequence);
    byte[] valueRefKey = redisCacheProvider.get(consumeSequenceKey);
    if (valueRefKey == null) {
      return null;
    }

    return redisCacheProvider.get(valueRefKey);
  }

  private long increasesReplayConsumer(MockCategoryType category, byte[] recordIdBytes,
      byte[] replayIdBytes, byte[] mockResultId) {
    byte[] usedRecordInstanceIdsKey = buildMatchedRecordInstanceIdsKey(category, recordIdBytes, replayIdBytes,
        mockResultId);
    return redisCacheProvider.incrValue(usedRecordInstanceIdsKey);
  }

  private byte[] buildMatchedRecordInstanceIdsKey(MockCategoryType category, byte[] recordIdBytes, byte[] replayIdBytes,
      byte[] mockResultId) {
    byte[] usedRecordInstanceIdsKey = CacheKeyUtils.buildMatchedRecordInstanceIdsKey(category,
        recordIdBytes, replayIdBytes, mockResultId);
    return usedRecordInstanceIdsKey;
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