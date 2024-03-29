package com.arextest.storage.repository.scenepool;

import com.arextest.model.scenepool.Scene;
import com.arextest.model.scenepool.Scene.Fields;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
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
  private static final long EXPIRATION_DAYS = 14L;

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

  @Override
  public Scene findAndUpdate(Scene newScene) {
    Bson filter = Filters.and(Filters.eq(Fields.appId, newScene.getAppId()),
        Filters.eq(Fields.sceneKey, newScene.getSceneKey()));

    Bson update = getUpdate(newScene);
    FindOneAndUpdateOptions opt = new FindOneAndUpdateOptions().upsert(true);
    opt.returnDocument(ReturnDocument.BEFORE);
    return getCollection().findOneAndUpdate(filter, update, opt);
  }

  public void upsertOne(Scene scene) {
    Bson filter = Filters.and(Filters.eq(Fields.appId, scene.getAppId()),
        Filters.eq(Fields.sceneKey, scene.getSceneKey()));

    Bson update = getUpdate(scene);
    getCollection().updateOne(filter, update, new UpdateOptions().upsert(true));
  }

  private Bson getUpdate(Scene scene) {
    Date expire = Date.from(LocalDateTime.now().plusDays(EXPIRATION_DAYS).atZone(ZoneId.systemDefault()).toInstant());
    Date now = new Date();
    return Updates.combine(
        Updates.set(Fields.appId, scene.getAppId()),
        Updates.set(Fields.sceneKey, scene.getSceneKey()),
        Updates.set(Fields.recordId, scene.getRecordId()),
        Updates.set(Fields.executionPath, scene.getExecutionPath()),

        Updates.setOnInsert(Fields.creationTime, now),
        Updates.set(Fields.updateTime, now),
        Updates.set(Fields.expirationTime, expire)
    );
  }

  @Override
  public Scene findFirst(String recordId) {
    Bson filter = Filters.eq(Fields.recordId, recordId);
    return getCollection().find(filter).first();
  }

  @Override
  public long clearSceneByAppid(String appid) {
    Bson filter = Filters.eq(Fields.appId, appid);
    return getCollection().deleteMany(filter).getDeletedCount();
  }
}
