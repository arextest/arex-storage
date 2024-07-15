package com.arextest.storage.repository.scenepool;

import com.arextest.model.scenepool.Scene;

public interface ScenePoolProvider {
  String getProviderName();

  boolean checkSceneExist(String appId, String sceneKey);

  Scene findAndUpdate(Scene scene);

  void upsertOne(Scene scene);

  long clearSceneByAppid(String appid);

  Scene findByRecordId(String recordId);
}
