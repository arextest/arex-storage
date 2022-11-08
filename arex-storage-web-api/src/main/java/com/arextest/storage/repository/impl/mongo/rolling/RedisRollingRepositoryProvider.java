package com.arextest.storage.repository.impl.mongo.rolling;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.RedisMocker;
import com.mongodb.client.MongoDatabase;
import org.springframework.stereotype.Repository;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class RedisRollingRepositoryProvider extends AbstractRollingRepositoryProvider<RedisMocker> {
    public RedisRollingRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.REDIS;
    }
}