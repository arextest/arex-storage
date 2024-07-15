package com.arextest.storage.repository.scenepool;

import com.arextest.model.mock.MockCategoryType;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ScenePoolFactory {
  public static final String RECORDING_SCENE_POOL = "Recording";
  public static final String REPLAY_SCENE_POOL = "Replay";

  private List<ScenePoolProvider> poolImpls;

  public ScenePoolProvider getProvider(String providerName) {
    return poolImpls.stream()
        .filter(poolImpl -> poolImpl.getProviderName().equals(providerName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No such provider: " + providerName));
  }

  public ScenePoolProvider getProviderByCategory(MockCategoryType categoryType) {
   if (categoryType.equals(MockCategoryType.RECORDING_SCENE)) {
     return getProvider(RECORDING_SCENE_POOL);
   }
   return getProvider(REPLAY_SCENE_POOL);
  }

}
