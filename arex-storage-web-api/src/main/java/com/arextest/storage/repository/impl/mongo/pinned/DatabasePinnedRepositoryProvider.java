package com.arextest.storage.repository.impl.mongo.pinned;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.DatabaseMocker;
import com.arextest.storage.repository.ProviderNames;
import com.mongodb.client.MongoDatabase;
import org.springframework.stereotype.Repository;

@Repository
public class DatabasePinnedRepositoryProvider extends AbstractPinnedRepositoryProvider<DatabaseMocker> {
    public DatabasePinnedRepositoryProvider(MongoDatabase mongoDatabase) {
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