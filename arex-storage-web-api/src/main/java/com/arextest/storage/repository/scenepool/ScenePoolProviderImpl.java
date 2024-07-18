package com.arextest.storage.repository.scenepool;

import com.arextest.model.scenepool.Scene;
import com.arextest.model.scenepool.Scene.Fields;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

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
    Query filter = new Query();
    filter.addCriteria(Criteria.where(Scene.Fields.appId).is(appId)
        .and(Scene.Fields.sceneKey).is(sceneKey));
    return getTemplate().count(filter, Scene.class, getCollectionName()) > 0;
  }

  @Override
  public Scene findAndUpdate(Scene newScene) {
    Query filter = new Query();
    filter.addCriteria(Criteria.where(Scene.Fields.appId).is(newScene.getAppId())
        .and(Scene.Fields.sceneKey).is(newScene.getSceneKey()));

    Update update = getUpdate(newScene);
    FindAndModifyOptions opt = new FindAndModifyOptions().upsert(true);
    opt.returnNew(false);
    return getTemplate().findAndModify(filter, update, opt, Scene.class, getCollectionName());
  }

  public void upsertOne(Scene scene) {
    Query filter = new Query();
    filter.addCriteria(Criteria.where(Scene.Fields.appId).is(scene.getAppId())
        .and(Scene.Fields.sceneKey).is(scene.getSceneKey()));

    Update update = getUpdate(scene);
    getTemplate().findAndModify(filter, update, new FindAndModifyOptions().upsert(true), Scene.class, getCollectionName());
  }

  private Update getUpdate(Scene scene) {
    Date expire = Date.from(LocalDateTime.now().plusDays(EXPIRATION_DAYS).atZone(ZoneId.systemDefault()).toInstant());
    Date now = new Date();
    return Update.update(Scene.Fields.appId, scene.getAppId())
        .set(Scene.Fields.sceneKey, scene.getSceneKey())
        .set(Scene.Fields.recordId, scene.getRecordId())
        .set(Scene.Fields.executionPath, scene.getExecutionPath())
        .setOnInsert(Scene.Fields.creationTime, now)
        .set(Scene.Fields.updateTime, now)
        .set(Scene.Fields.expirationTime, expire);
  }

  @Override
  public long clearSceneByAppid(String appid) {
    Query filter = Query.query(Criteria.where(Fields.appId).is(appid));
    return getTemplate().remove(filter, Scene.class, getCollectionName()).getDeletedCount();
  }

  @Override
  public Scene findByRecordId(String recordId) {
    Query filter = Query.query(Criteria.where(Fields.recordId).is(recordId));
    return getTemplate().findOne(filter, Scene.class, getCollectionName());
  }
}
