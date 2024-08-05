package com.arextest.storage.service;

import static com.arextest.storage.repository.scenepool.ScenePoolFactory.REPLAY_SCENE_POOL;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.dto.SceneDTO;
import com.arextest.model.scenepool.Scene;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import java.util.Collections;
import java.util.List;
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
  private static final int MAX_PAGE_SIZE = 300;

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

  public SceneDTO findRecordsByAppId(String appId, String category, Integer pageIndex, int pageSize) {
    if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(category) || pageIndex == null
        || pageIndex < 0 || pageSize == 0) {
      return null;
    }

    ScenePoolProvider provider = scenePoolFactory.getProviderByCategoryName(category);
    if (provider == null) {
      return null;
    }

    pageSize = Math.min(pageSize, MAX_PAGE_SIZE);

    SceneDTO sceneDTO = new SceneDTO();
    long count = provider.countByAppId(appId);
    sceneDTO.setTotal(count);
    if (count == 0L) {
      sceneDTO.setSceneList(Collections.emptyList());
      return sceneDTO;
    }

    List<String> scenes = provider.findRecordsByAppId(appId, pageIndex, pageSize);
    sceneDTO.setSceneList(scenes);
    return sceneDTO;
  }

  private AREXMocker convert(Scene scene) {
    if (scene == null) {
      return null;
    }

    AREXMocker mocker = new AREXMocker(MockCategoryType.COVERAGE);
    mocker.setOperationName(scene.getSceneKey());
    mocker.setAppId(scene.getAppId());
    mocker.setRecordId(scene.getRecordId());
    Target targetResponse = new Target();
    targetResponse.setBody(scene.getExecutionPath());
    mocker.setTargetResponse(targetResponse);
    return mocker;
  }
}
