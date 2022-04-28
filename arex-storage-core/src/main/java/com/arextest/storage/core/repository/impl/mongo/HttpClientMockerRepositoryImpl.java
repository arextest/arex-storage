package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.HttpClientMocker;
import org.springframework.stereotype.Repository;

@Repository
public class HttpClientMockerRepositoryImpl extends AbstractMongoDbRepository<HttpClientMocker> {
    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.SERVICE_CALL;
    }
}
