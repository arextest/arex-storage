package com.arextest.storage.beans;

import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.scenepool.ScenePoolProviderImpl;
import com.mongodb.client.MongoDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScenePoolProviderConfiguration {

  @Bean
  public ScenePoolProviderImpl getRecordingPool(MongoDatabase mongoDatabase) {
    return new ScenePoolProviderImpl(ProviderNames.RECORDING_SCENE_POOL, mongoDatabase);
  }

  @Bean
  public ScenePoolProviderImpl getReplayPool(MongoDatabase mongoDatabase) {
    return new ScenePoolProviderImpl(ProviderNames.REPLAY_SCENE_POOL, mongoDatabase);
  }
}
