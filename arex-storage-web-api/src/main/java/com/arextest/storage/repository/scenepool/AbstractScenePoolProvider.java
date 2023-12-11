package com.arextest.storage.repository.scenepool;

import com.arextest.model.scenepool.Scene;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import javax.annotation.Resource;

public abstract class AbstractScenePoolProvider implements ScenePoolProvider {
  @Resource
  private MongoDatabase mongoDatabase;
  MongoDatabase getDataBase() {
    return mongoDatabase;
  }

  void setMongoDataBase(MongoDatabase mongoDatabase) {
    this.mongoDatabase = mongoDatabase;
  }

  MongoCollection<Scene> getCollection() {
    String categoryName = this.getProviderName() + "ScenePool";
    return getDataBase().getCollection(categoryName, Scene.class);
  }
}
