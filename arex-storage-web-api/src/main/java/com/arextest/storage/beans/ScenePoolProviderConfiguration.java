package com.arextest.storage.beans;

import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProviderImpl;
import com.arextest.storage.service.handler.mocker.coverage.CoverageHandlerSwitch;
import com.arextest.storage.service.handler.mocker.coverage.DefaultCoverageSwitch;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScenePoolProviderConfiguration {
  @Bean
  @ConditionalOnMissingBean
  public CoverageHandlerSwitch register() {
    return new DefaultCoverageSwitch();
  }

  @Bean
  public ScenePoolProviderImpl getRecordingPool() {
    return new ScenePoolProviderImpl(ScenePoolFactory.RECORDING_SCENE_POOL);
  }

  @Bean
  public ScenePoolProviderImpl getReplayPool() {
    return new ScenePoolProviderImpl(ScenePoolFactory.REPLAY_SCENE_POOL);
  }
}
