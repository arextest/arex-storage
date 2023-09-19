package com.arextest.storage.repository.impl.mongo;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.storage.beans.StorageConfigurationProperties;
import com.arextest.storage.repository.ProviderNames;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class AutoPinedMockerRepository extends AREXMockerMongoRepositoryProvider {
    public AutoPinedMockerRepository(MongoDatabase mongoDatabase,
                                     StorageConfigurationProperties properties,
                                     Set<MockCategoryType> entryPointTypes) {
        super(ProviderNames.AUTO_PINNED, mongoDatabase, properties, entryPointTypes);
    }

    public AREXMocker countFailAndUpdateReq(MockCategoryType categoryType, String caseId) {
        MongoCollection<AREXMocker> collection = createOrGetCollection(categoryType);
        return collection.findOneAndUpdate(
                Filters.eq("_id", caseId),
                Updates.inc("continuousFailCount", 1),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        );
    }

    public void deleteOne(MockCategoryType categoryType, String caseId) {
        MongoCollection<AREXMocker> collection = createOrGetCollection(categoryType);
        collection.deleteOne(Filters.eq("_id", caseId));
    }

    public void deleteMany(MockCategoryType categoryType, List<String> caseIds) {
        MongoCollection<AREXMocker> collection = createOrGetCollection(categoryType);
        collection.deleteMany(Filters.in("_id", caseIds));
    }

    public void resetFailCount(MockCategoryType categoryType, String caseId) {
        MongoCollection<AREXMocker> collection = createOrGetCollection(categoryType);
        collection.findOneAndUpdate(
                Filters.eq("_id", caseId),
                Updates.set("continuousFailCount", 0)
        );
    }
}
