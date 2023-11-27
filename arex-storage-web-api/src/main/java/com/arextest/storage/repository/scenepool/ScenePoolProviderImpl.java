package com.arextest.storage.repository.scenepool;

import com.arextest.model.scenepool.Scene;
import com.arextest.model.scenepool.Scene.Fields;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.AllArgsConstructor;
import org.bson.conversions.Bson;

@AllArgsConstructor
public class ScenePoolProviderImpl implements ScenePoolProvider {
  private final String providerName;
  private final MongoDatabase mongoDatabase;
  @Override
  public String getProviderName() {
    return providerName;
  }

  @Override
  public MongoDatabase getDataBase() {
    return mongoDatabase;
  }

  public void insertOne(Scene scene) {
    getCollection().insertOne(scene);
  }

  public boolean checkAppSceneExist(String appId, String sceneKey) {
    Bson filter = Filters.and(Filters.eq(Fields.appId, appId),
        Filters.eq(Fields.sceneKey, sceneKey));
    return getCollection().countDocuments(filter) > 0;
  }
}
