package com.arextest.storage.repository.impl.mongo.pinned;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.DynamicResultMocker;
import com.mongodb.client.MongoDatabase;
import org.springframework.stereotype.Repository;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class DynamicPinnedRepositoryProvider extends AbstractPinnedRepositoryProvider<DynamicResultMocker> {
    public DynamicPinnedRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.DYNAMIC;
    }
}