package com.arextest.storage.service;

import com.arextest.model.replay.UpdateCaseStatusRequestType;
import com.arextest.model.replay.UpdateCaseStatusResponseType;
import com.arextest.storage.client.HttpWebServiceApiClient;
import com.arextest.storage.trace.MDCTracer;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * update case status service
 *
 * created by xinyuan_wang on 2024/7/17
 */
@Service
@Slf4j
public class UpdateCaseStatusService {

  @Value("${arex.schedule.updateCaseStatus.url}")
  private String updateCaseStatusUrl;
  @Resource
  private HttpWebServiceApiClient httpWebServiceApiClient;
  private static final String TITLE = "[[title=updateCaseStatus]]";

  public void updateStatusOfCase(String recordId, int caseStatus) {
    if (StringUtils.isEmpty(recordId)) {
      return;
    }

    try {
      MDCTracer.addRecordId(recordId);
      UpdateCaseStatusResponseType response = httpWebServiceApiClient.jsonPost(
          updateCaseStatusUrl,
          new UpdateCaseStatusRequestType(recordId, caseStatus),
          UpdateCaseStatusResponseType.class
      );

      if (response == null || response.getBody() == 0L) {
        LOGGER.warn(TITLE + "updateCaseStatus failed for recordId: {}", recordId);
        return;
      }

      LOGGER.info(TITLE + "updateCaseStatus success for recordId: {}, update count: {}", recordId, response.getBody());
    } catch (Exception e) {
      LOGGER.error(TITLE + "updateCaseStatus failed for recordId: {}", recordId, e);
    } finally {
      MDCTracer.clear();
    }
  }

}