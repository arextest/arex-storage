package io.arex.storage.core.repository.impl.mongo;

import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.impl.DatabaseMocker;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseMockerRepositoryImpl extends AbstractMongoDbRepository<DatabaseMocker> {
    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.DATABASE;
    }
}
