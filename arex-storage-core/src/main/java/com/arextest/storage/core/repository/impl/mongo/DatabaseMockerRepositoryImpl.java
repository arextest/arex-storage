package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.DatabaseMocker;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseMockerRepositoryImpl extends AbstractMongoDbRepository<DatabaseMocker> {
    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.DATABASE;
    }
}
