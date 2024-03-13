package com.arextest.storage.service.mockerhandlers.coverage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * @author: QizhengMo
 * @date: 2024/3/13 10:57
 */
@ConditionalOnMissingBean
@Component
public class DefaultCoverageSwitch implements CoverageHandlerSwitch {

  @Override
  public boolean allowReplayTask(String appId) {
    return true;
  }
}
