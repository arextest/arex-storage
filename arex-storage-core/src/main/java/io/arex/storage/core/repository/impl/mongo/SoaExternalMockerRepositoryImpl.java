package io.arex.storage.core.repository.impl.mongo;

import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.impl.SoaExternalMocker;
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
