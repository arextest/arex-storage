package com.arextest.storage.repository.impl.mongo.rolling;

import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.impl.mongo.AbstractMongoRepositoryProvider;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRollingRepositoryProvider<T extends MockItem> extends AbstractMongoRepositoryProvider<T> {
    public AbstractRollingRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public String getProviderName() {
        return ProviderNames.DEFAULT;
    }
}