package com.arextest.storage.repository.scenepool;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ScenePoolFactory {
  private List<ScenePoolProvider> poolImpls;

  public ScenePoolProvider getProvider(String providerName) {
    return poolImpls.stream()
        .filter(poolImpl -> poolImpl.getProviderName().equals(providerName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No such provider: " + providerName));
  }
}