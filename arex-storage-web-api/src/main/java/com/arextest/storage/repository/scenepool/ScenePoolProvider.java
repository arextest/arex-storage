package com.arextest.storage.repository.scenepool;

import com.arextest.model.scenepool.Scene;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public interface ScenePoolProvider {
  String getProviderName();
  MongoDatabase getDataBase();

  default MongoCollection<Scene> getCollection() {
    String categoryName = this.getProviderName() + "ScenePool";
    return getDataBase().getCollection(categoryName, Scene.class);
  }
}
