package com.arextest.storage.repository.scenepool;

import com.arextest.model.scenepool.Scene;
import java.util.List;

public interface ScenePoolProvider {
  String getProviderName();

  boolean checkSceneExist(String appId, String sceneKey);

  Scene findAndUpdate(Scene scene);

  void upsertOne(Scene scene);

  long clearSceneByAppid(String appid);

  Scene findByRecordId(String recordId);

  List<String> findRecordsByAppId(String appId, int pageIndex, int pageSize);

  long countByAppId(String appId);
}
