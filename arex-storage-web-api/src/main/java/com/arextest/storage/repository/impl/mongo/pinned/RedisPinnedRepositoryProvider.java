package com.arextest.storage.repository.impl.mongo.pinned;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.RedisMocker;
import com.mongodb.client.MongoDatabase;
import org.springframework.stereotype.Repository;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class RedisPinnedRepositoryProvider extends AbstractPinnedRepositoryProvider<RedisMocker> {
    public RedisPinnedRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.REDIS;
    }
}