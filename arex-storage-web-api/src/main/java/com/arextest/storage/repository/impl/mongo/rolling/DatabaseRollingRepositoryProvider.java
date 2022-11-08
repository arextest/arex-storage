package com.arextest.storage.repository.impl.mongo.rolling;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.DatabaseMocker;
import com.arextest.storage.repository.ProviderNames;
import com.mongodb.client.MongoDatabase;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseRollingRepositoryProvider extends AbstractRollingRepositoryProvider<DatabaseMocker> {
    public DatabaseRollingRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.DATABASE;
    }

    @Override
    public String getProviderName() {
        return ProviderNames.DEFAULT;
    }
}