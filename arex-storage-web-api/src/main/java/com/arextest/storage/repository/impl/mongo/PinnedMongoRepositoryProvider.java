package com.arextest.storage.repository.impl.mongo;

import com.arextest.storage.repository.ProviderNames;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PinnedMongoRepositoryProvider extends DefaultMongoRepositoryProvider {
    public PinnedMongoRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public String getProviderName() {
        return ProviderNames.PINNED;
    }

}