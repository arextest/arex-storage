package com.arextest.storage.beans;

import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProviderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScenePoolProviderConfiguration {

  @Bean
  public ScenePoolProviderImpl getRecordingPool() {
    return new ScenePoolProviderImpl(ScenePoolFactory.RECORDING_SCENE_POOL);
  }

  @Bean
  public ScenePoolProviderImpl getReplayPool() {
    return new ScenePoolProviderImpl(ScenePoolFactory.REPLAY_SCENE_POOL);
  }
}
