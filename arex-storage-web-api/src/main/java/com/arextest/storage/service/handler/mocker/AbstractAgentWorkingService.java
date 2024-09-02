package com.arextest.storage.service.handler.mocker;

import com.arextest.common.cache.CacheProvider;
import com.arextest.storage.enums.InvalidReasonEnum;
import com.arextest.storage.metric.AgentWorkingMetricService;
import com.arextest.storage.model.InvalidIncompleteRecordRequest;
import com.arextest.storage.service.InvalidRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public abstract class AbstractAgentWorkingService {

  protected final AgentWorkingMetricService agentWorkingMetricService;
  protected final CacheProvider redisCacheProvider;
  protected final InvalidRecordService invalidRecordService;

  protected AbstractAgentWorkingService(AgentWorkingMetricService agentWorkingMetricService,
      CacheProvider redisCacheProvider,
      InvalidRecordService invalidRecordService) {
    this.agentWorkingMetricService = agentWorkingMetricService;
    this.redisCacheProvider = redisCacheProvider;
    this.invalidRecordService = invalidRecordService;
  }

  /**
   * Data that fails to be saved is marked as invalid case
   * @param appId
   * @param recordId
   * @param replayId
   */
  public void handleBatchSaveException(String appId, String recordId, String replayId) {
    InvalidIncompleteRecordRequest request = new InvalidIncompleteRecordRequest();
    request.setRecordId(recordId);
    request.setAppId(appId);
    request.setReplayId(replayId);
    request.setReason(InvalidReasonEnum.STORAGE_SAVE_ERROR.getValue());
    agentWorkingMetricService.invalidIncompleteRecord(request);
  }

  /**
   * Determine whether it is an invalid case
   * @param caseId
   * @return
   */
  public boolean isInvalidCase(String caseId) {
    return invalidRecordService.isInvalidCase(caseId);
  }

}
