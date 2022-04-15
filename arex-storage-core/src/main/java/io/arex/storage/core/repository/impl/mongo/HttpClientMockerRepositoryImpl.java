package io.arex.storage.core.repository.impl.mongo;

import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.impl.HttpClientMocker;
import org.springframework.stereotype.Repository;

@Repository
public class HttpClientMockerRepositoryImpl extends AbstractMongoDbRepository<HttpClientMocker> {
    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.SERVICE_CALL;
    }
}
