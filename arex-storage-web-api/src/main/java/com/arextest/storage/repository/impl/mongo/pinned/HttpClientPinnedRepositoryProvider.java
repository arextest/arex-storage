package com.arextest.storage.repository.impl.mongo.pinned;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.HttpClientMocker;
import com.mongodb.client.MongoDatabase;
import org.springframework.stereotype.Repository;

@Repository
public class HttpClientPinnedRepositoryProvider extends AbstractPinnedRepositoryProvider<HttpClientMocker> {
    public HttpClientPinnedRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.SERVICE_CALL;
    }
}