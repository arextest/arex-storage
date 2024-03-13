package com.arextest.storage.service.mockerhandlers.coverage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: QizhengMo
 * @date: 2024/3/13 10:57
 */
@Configuration
public class DefaultCoverageSwitch implements CoverageHandlerSwitch {
  @Bean
  @ConditionalOnMissingBean
  public CoverageHandlerSwitch register() {
    return new DefaultCoverageSwitch();
  }
}
