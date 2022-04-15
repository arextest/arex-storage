package io.arex.storage.core.repository.impl.mongo;

import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.impl.DynamicResultMocker;
import org.springframework.stereotype.Repository;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class DynamicResultMockerRepositoryImpl extends AbstractMongoDbRepository<DynamicResultMocker> {
    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.DYNAMIC;
    }
}
