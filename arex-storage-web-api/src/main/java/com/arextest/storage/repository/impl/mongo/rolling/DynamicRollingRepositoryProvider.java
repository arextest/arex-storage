package com.arextest.storage.repository.impl.mongo.rolling;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.DynamicResultMocker;
import com.mongodb.client.MongoDatabase;
import org.springframework.stereotype.Repository;


@Repository
final class DynamicRollingRepositoryProvider extends AbstractRollingRepositoryProvider<DynamicResultMocker> {
    public DynamicRollingRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.DYNAMIC;
    }
}