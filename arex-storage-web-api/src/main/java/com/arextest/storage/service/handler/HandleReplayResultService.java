package com.arextest.storage.service.handler;

import static com.arextest.storage.model.Constants.TEN_MINUTES;
import com.arextest.common.cache.CacheProvider;
import com.arextest.model.replay.CompareRelationResult;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.serialization.ZstdJacksonSerializer;
import com.arextest.storage.service.InvalidRecordService;
import com.arextest.storage.trace.MDCTracer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * created by xinyuan_wang on 2024/06/03
 */
@Service
@Slf4j
public class HandleReplayResultService extends AbstractAgentWorkingService
    implements AgentWorkingHandler<CompareRelationResult> {

  @Resource
  private ZstdJacksonSerializer serializer;
  private static final String TITLE = "[[title=handleReplayResult]]";

  public HandleReplayResultService(AgentWorkingMetricService agentWorkingMetricService,
      CacheProvider redisCacheProvider, InvalidRecordService invalidRecordService) {
    super(agentWorkingMetricService, redisCacheProvider, invalidRecordService);
  }

  @Override
  public boolean batchSave(List<CompareRelationResult> items) {
    if (CollectionUtils.isEmpty(items)) {
      return false;
    }

    CompareRelationResult replayCompareResult = items.get(0);
    boolean batchSaveResult = true;
    try {
      MDCTracer.addRecordId(replayCompareResult.getRecordId());
      MDCTracer.addReplayId(replayCompareResult.getReplayId());

      batchSaveResult = doBatchSave(items);
      return batchSaveResult;
    } catch (Exception e) {
      LOGGER.error("{}batch save replay result error: {}", TITLE, e.getMessage(), e);
      batchSaveResult = false;
      return false;
    } finally {
      if (!batchSaveResult) {
        handleBatchSaveException(replayCompareResult.getAppId(),
            replayCompareResult.getRecordId(), replayCompareResult.getReplayId());
      }
      MDCTracer.clear();
    }
  }

  @Override
  public List<CompareRelationResult> findBy(String recordId, String replayId) {
    if (StringUtils.isEmpty(recordId) && StringUtils.isEmpty(replayId)) {
      return Collections.emptyList();
    }

    List<byte[]> results =
        redisCacheProvider.lrange(buildReplayResultKey(recordId, replayId), 0, -1);
    if (CollectionUtils.isEmpty(results)) {
      return Collections.emptyList();
    }

    List<CompareRelationResult> replayResults = Lists.newArrayListWithCapacity(results.size());

    for (byte[] result: results) {
      replayResults.addAll(serializer.deserialize(result,
          new TypeReference<List<CompareRelationResult>>() {}));
    }
    return replayResults;
  }

  private boolean doBatchSave(List<CompareRelationResult> items) {
    CompareRelationResult replayCompareResult = items.get(0);
    if (isInvalidCase(replayCompareResult.getReplayId())) {
      LOGGER.warn("{}replayId: {} is invalid", TITLE, replayCompareResult.getReplayId());
      return false;
    }

    byte[] replayResultKey = buildReplayResultKey(replayCompareResult.getRecordId(),
        replayCompareResult.getReplayId());
    byte[] results = serializer.serialize(items);
    long pushCount = saveResultsToRedis(replayResultKey, results);
    LOGGER.info("{}batch save replay result success. save count: {}, total count: {}",
        TITLE, pushCount, items.size());
    return true;
  }

  private static byte[] buildReplayResultKey(String recordId, String replayId) {
    return CacheKeyUtils.buildReplayResultKey(recordId, replayId);
  }

  /**
   * Record the matching relationship in redis and store it for 10 minutes
   * @param key
   * @param results
   */
  private long saveResultsToRedis(byte[] key, byte[] results) {
    Long pushCount = redisCacheProvider.rpush(key, results);
    redisCacheProvider.expire(key, TEN_MINUTES);
    return pushCount;
  }
}