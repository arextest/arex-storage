package com.arextest.storage.service;

import static com.arextest.storage.repository.scenepool.ScenePoolFactory.RECORDING_SCENE_POOL;
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

  public AREXMocker getCoverageMocker(String recordId) {
    ScenePoolProvider provider = scenePoolFactory.getProvider(RECORDING_SCENE_POOL);
    if (provider == null) {
      return null;
    }
    Scene scene = provider.findFirst(recordId);
    if (scene == null) {
      return null;
    }
    return buildCoverageMocker(scene);
  }

  private AREXMocker buildCoverageMocker(Scene scene) {
    AREXMocker arexMocker = new AREXMocker();
    arexMocker.setAppId(scene.getAppId());
    arexMocker.setCategoryType(MockCategoryType.COVERAGE);
    arexMocker.setRecordId(scene.getRecordId());
    arexMocker.setCreationTime(scene.getCreationTime().getTime());
    Target target = new Target();
    target.setBody(scene.getExecutionPath());
    arexMocker.setTargetResponse(target);
    return arexMocker;
  }
}
