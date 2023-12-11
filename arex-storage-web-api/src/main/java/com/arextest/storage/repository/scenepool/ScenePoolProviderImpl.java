package com.arextest.storage.repository.scenepool;

import com.arextest.model.scenepool.Scene;
import com.arextest.model.scenepool.Scene.Fields;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.AllArgsConstructor;
import org.bson.conversions.Bson;

@AllArgsConstructor
public class ScenePoolProviderImpl extends AbstractScenePoolProvider {
  private final String providerName;
  @Override
  public String getProviderName() {
    return providerName;
  }

  @Override
  public boolean checkSceneExist(String appId, String sceneKey) {
    Bson filter = Filters.and(Filters.eq(Fields.appId, appId),
        Filters.eq(Fields.sceneKey, sceneKey));
    return getCollection().countDocuments(filter) > 0;
  }

  public void upsertOne(Scene scene) {
    Bson filter = Filters.and(Filters.eq(Fields.appId, scene.getAppId()),
        Filters.eq(Fields.sceneKey, scene.getSceneKey()));

    Date expire = Date.from(LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant());
    Bson update = Updates.combine(
        Updates.set(Fields.appId, scene.getAppId()),
        Updates.set(Fields.sceneKey, scene.getSceneKey()),
        Updates.set(Fields.recordId, scene.getRecordId()),

        Updates.setOnInsert(Fields.creationTime, new Date()),
        Updates.set(Fields.updateTime, new Date()),
        Updates.set(Fields.expirationTime, expire)
    );
    getCollection().updateOne(filter, update, new UpdateOptions().upsert(true));
  }
}
