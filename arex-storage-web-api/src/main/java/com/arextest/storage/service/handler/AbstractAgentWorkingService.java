package com.arextest.storage.service.handler;

import static com.arextest.storage.model.Constants.TEN_MINUTES;
import com.arextest.common.cache.CacheProvider;
import com.arextest.storage.enums.InvalidReasonEnum;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.model.InvalidIncompleteRecordRequest;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public abstract class AbstractAgentWorkingService {

  protected final AgentWorkingMetricService agentWorkingMetricService;
  protected final CacheProvider redisCacheProvider;

  protected AbstractAgentWorkingService(AgentWorkingMetricService agentWorkingMetricService,
      CacheProvider redisCacheProvider) {
    this.agentWorkingMetricService = agentWorkingMetricService;
    this.redisCacheProvider = redisCacheProvider;
  }
  private static final String INVALID_CASE_REDIS_KEY = "invalidCase_";
  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  public void handleBatchSaveException(String appId, String recordId, String replayId) {
    InvalidIncompleteRecordRequest request = new InvalidIncompleteRecordRequest();
    request.setRecordId(recordId);
    request.setAppId(appId);
    request.setReplayId(replayId);
    request.setReason(InvalidReasonEnum.STORAGE_SAVE_ERROR.getValue());
    agentWorkingMetricService.invalidIncompleteRecord(request);
    putInvalidCaseInRedis(StringUtils.isEmpty(replayId) ? recordId : replayId);
  }

  public boolean isInvalidCase(String caseId) {
    return redisCacheProvider.get(buildInvalidCaseKey(caseId)) != null;
  }

  private void putInvalidCaseInRedis(String caseId) {
    redisCacheProvider.put(buildInvalidCaseKey(caseId), TEN_MINUTES, EMPTY_BYTE_ARRAY);
  }

  private byte[] buildInvalidCaseKey(String recordId) {
    return (INVALID_CASE_REDIS_KEY + recordId).getBytes(StandardCharsets.UTF_8);
  }

}
