package com.arextest.storage.service;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker.Target;
import com.arextest.model.replay.dto.SceneDTO;
import com.arextest.model.scenepool.Scene;
import com.arextest.storage.repository.scenepool.ScenePoolFactory;
import com.arextest.storage.repository.scenepool.ScenePoolProvider;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * created by xinyuan_wang on 2024/2/2
 */
@Service
public class ScenePoolService {
  @Resource
  private ScenePoolFactory scenePoolFactory;
  @Resource
  private DefaultApplicationConfig defaultApplicationConfig;
  private static final int DEFAULT_LIMIT = 200;
  private static final String CLEAR_LIMIT_KEY = "clear.pool.limit";
  private static final int MAX_PAGE_SIZE = 300;
  private static final int MAX_PAGE_INDEX = 200;
  private static final int MAX_ITERATIONS = 200;
  private static final int MAX_LIMIT = 5000;
  private static final int MIN_LIMIT = 0;

  public long clearPoolByApp(String appId, String providerName) {
    ScenePoolProvider provider = scenePoolFactory.getProvider(providerName);

    int limit = getLimit();
    long totalDeletedCount = 0;
    Date date = new Date();
    long deletedCount;
    int iterations = 0;

    do {
      deletedCount = provider.clearSceneByAppid(appId, date, limit);
      totalDeletedCount += deletedCount;
      iterations++;
    } while (deletedCount > 0 && iterations <= MAX_ITERATIONS);

    return totalDeletedCount;
  }

  private int getLimit() {
    int limit = defaultApplicationConfig.getConfigAsInt(CLEAR_LIMIT_KEY, DEFAULT_LIMIT);
    if (limit <= MIN_LIMIT) {
      limit = DEFAULT_LIMIT;
    } else if (limit > MAX_LIMIT) {
      limit = MAX_LIMIT;
    }
    return limit;
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
    pageIndex = Math.min(pageIndex, MAX_PAGE_INDEX);

    SceneDTO sceneDTO = new SceneDTO();
    long count = provider.countByAppId(appId);
    sceneDTO.setTotal(count);
    if (count == 0L) {
      sceneDTO.setSceneList(Collections.emptyList());
      return sceneDTO;
    }
    if (pageIndex == MAX_PAGE_INDEX) {
      sceneDTO.setTotal(0L);
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
