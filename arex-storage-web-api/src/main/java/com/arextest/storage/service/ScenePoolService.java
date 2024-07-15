package com.arextest.storage.service;

import static com.arextest.storage.repository.scenepool.ScenePoolFactory.REPLAY_SCENE_POOL;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.scenepool.Scene;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
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

  public AREXMocker findByRecordId(String recordId, MockCategoryType categoryType) {
    if (StringUtils.isEmpty(recordId)) {
      return null;
    }

    ScenePoolProvider provider = scenePoolFactory.getProviderByCategory(categoryType);
    if (provider == null) {
      return null;
    }

    return convert(provider.findByRecordId(recordId));
  }

  private AREXMocker convert(Scene scene) {
    if (scene == null) {
      return null;
    }

    AREXMocker mocker = new AREXMocker();
    mocker.setOperationName(scene.getSceneKey());
    mocker.setAppId(scene.getAppId());
    mocker.setRecordId(scene.getRecordId());
    Target targetResponse = new Target();
    targetResponse.setBody(scene.getExecutionPath());
    mocker.setTargetResponse(targetResponse);
    mocker.setCategoryType(MockCategoryType.COVERAGE);
    return mocker;
  }
}
