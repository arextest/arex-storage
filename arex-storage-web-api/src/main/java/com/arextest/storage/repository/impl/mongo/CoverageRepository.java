package com.arextest.storage.repository.impl.mongo;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.repository.ProviderNames;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;


@Repository
public class CoverageRepository {
    private static final String COLLECTION_SUFFIX = "Mocker";
    private final MongoCollection<AREXMocker> coverageCollection;

    public CoverageRepository(MongoDatabase mongoDatabase) {
        coverageCollection = mongoDatabase.getCollection(
                ProviderNames.DEFAULT + MockCategoryType.COVERAGE.getName() + COLLECTION_SUFFIX,
                AREXMocker.class);
    }

    public AREXMocker upsertOne(String appId, String pathKey, AREXMocker value) {
        Bson filters = Filters.and(Filters.eq(AREXMocker.Fields.appId, appId), Filters.eq(AREXMocker.Fields.operationName, pathKey));
        // update record id, todo expire
        Bson update = Updates.set(AREXMocker.Fields.recordId, value.getRecordId());
        FindOneAndUpdateOptions opt = new FindOneAndUpdateOptions();
        opt.upsert(true);
        return coverageCollection.findOneAndUpdate(filters, update, opt);
    }

    public void updatePathKeyByRecordId(String recordId, String pathKey) {
        Bson filters = Filters.eq(AREXMocker.Fields.recordId, recordId);
        Bson update = Updates.set(AREXMocker.Fields.operationName, pathKey);
        coverageCollection.updateOne(filters, update);
    }
}
