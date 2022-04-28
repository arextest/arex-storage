package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.SoaExternalMocker;
import org.springframework.stereotype.Repository;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class SoaExternalMockerRepositoryImpl extends AbstractMongoDbRepository<SoaExternalMocker> {

    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.SOA_EXTERNAL;
    }
}
