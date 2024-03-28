package com.arextest.storage.service;

import static com.arextest.storage.repository.scenepool.ScenePoolFactory.RECORDING_SCENE_POOL;
import static com.arextest.storage.repository.scenepool.ScenePoolFactory.REPLAY_SCENE_POOL;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.scenepool.Scene;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * created by xinyuan_wang on 2024/2/2
 */
@Service
public class ScenePoolService {
  @Resource
  private ScenePoolFactory scenePoolFactory;

  public long clearReplayPoolByApp(String appId) {
    ScenePoolProvider provider = scenePoolFactory.getProvider(REPLAY_SCENE_POOL);
    return provider.clearSceneByAppid(appId);
  }
}
