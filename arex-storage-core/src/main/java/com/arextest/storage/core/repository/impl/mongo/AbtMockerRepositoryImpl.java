package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.ABtMocker;
import org.springframework.stereotype.Repository;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class AbtMockerRepositoryImpl extends AbstractMongoDbRepository<ABtMocker> {
    /**
     * @return false ,skip compress
     */
    @Override
    protected boolean enableCompression() {
        return false;
    }

    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.AB_TEST;
    }
}
