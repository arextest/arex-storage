package com.arextest.storage.repository.impl.mongo;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.repository.ProviderNames;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import java.util.Date;
import java.util.List;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Repository;


@Repository
public class CoverageRepository {

  private static final String COLLECTION_SUFFIX = "Mocker";
  private static final String DASH_ID = "_id";
  private final MongoCollection<AREXMocker> coverageCollection;

  public CoverageRepository(MongoDatabase mongoDatabase) {
    coverageCollection = mongoDatabase.getCollection(
        ProviderNames.DEFAULT + MockCategoryType.COVERAGE.getName() + COLLECTION_SUFFIX,
        AREXMocker.class);
  }

  public AREXMocker upsertOne(AREXMocker value) {
    Bson filters = Filters.and(Filters.eq(AREXMocker.Fields.appId, value.getAppId()),
        Filters.eq(AREXMocker.Fields.operationName, value.getOperationName()));
    // update record id, todo expire
    Bson update = Updates.combine(Updates.set(AREXMocker.Fields.recordId, value.getRecordId()),
        Updates.set(AREXMocker.Fields.targetResponse, value.getTargetResponse()),
        Updates.set(AREXMocker.Fields.updateTime, new Date()),
        Updates.setOnInsert(AREXMocker.Fields.creationTime, new Date(value.getCreationTime())),
        Updates.setOnInsert("_id", new ObjectId().toString()));
    FindOneAndUpdateOptions opt = new FindOneAndUpdateOptions();
    opt.upsert(true);
    opt.projection(Projections.exclude(AREXMocker.Fields.targetResponse));

    try {
      return coverageCollection.findOneAndUpdate(filters, update, opt);
    } catch (DuplicateKeyException e) {
      // todo optimize
      update = Updates.combine(Updates.set(AREXMocker.Fields.recordId, value.getRecordId()),
          Updates.setOnInsert(DASH_ID, new ObjectId().toString()));
      return coverageCollection.findOneAndUpdate(filters, update, opt);
    }
  }

  public void updatePathByRecordId(String recordId, AREXMocker mocker) {
    String pathKey = mocker.getOperationName();
    Mocker.Target response = mocker.getTargetResponse();
    Bson filters = Filters.eq(AREXMocker.Fields.recordId, recordId);
    Bson updates = Updates.combine(Updates.set(AREXMocker.Fields.operationName, pathKey),
        Updates.set(AREXMocker.Fields.targetResponse, response));

    coverageCollection.updateOne(filters, updates);
  }

  public void deleteByRecordIds(List<String> recordIds) {
    Bson filters = Filters.in(AREXMocker.Fields.recordId, recordIds);
    coverageCollection.deleteMany(filters);
  }
}
