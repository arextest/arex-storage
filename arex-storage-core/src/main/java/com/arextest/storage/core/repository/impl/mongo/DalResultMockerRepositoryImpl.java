package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.DalResultMocker;
import org.springframework.stereotype.Repository;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class DalResultMockerRepositoryImpl extends AbstractMongoDbRepository<DalResultMocker> {

    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.DAL;
    }
}
