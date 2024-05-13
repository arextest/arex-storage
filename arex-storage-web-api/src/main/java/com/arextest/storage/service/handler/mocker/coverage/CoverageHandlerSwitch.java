package com.arextest.storage.service.handler.mocker.coverage;

/**
 * @author: QizhengMo
 * @date: 2024/3/13 10:57
 */
public interface CoverageHandlerSwitch {
  default boolean allowReplayTask(String appId) {
    return true;
  }
  default boolean allowRecordTask(String appId) {
    return true;
  }
}
