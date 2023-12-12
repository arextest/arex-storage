package com.arextest.storage.repository.scenepool;

import com.arextest.model.scenepool.Scene;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public interface ScenePoolProvider {
  String getProviderName();

  boolean checkSceneExist(String appId, String sceneKey);

  void upsertOne(Scene scene);
}
