package com.arextest.storage.repository.impl.mongo.rolling;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.HttpClientMocker;
import com.mongodb.client.MongoDatabase;
import org.springframework.stereotype.Repository;

@Repository
class HttpClientRollingRepositoryProvider extends AbstractRollingRepositoryProvider<HttpClientMocker> {
    public HttpClientRollingRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.SERVICE_CALL;
    }
}