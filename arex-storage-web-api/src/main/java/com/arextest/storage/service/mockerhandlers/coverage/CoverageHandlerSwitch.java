package com.arextest.storage.service.mockerhandlers.coverage;

/**
 * @author: QizhengMo
 * @date: 2024/3/13 10:57
 */
public interface CoverageHandlerSwitch {
  boolean allowReplayTask(String appId);
  default boolean allowRecordTask(String appId) {
    return true;
  }
}
