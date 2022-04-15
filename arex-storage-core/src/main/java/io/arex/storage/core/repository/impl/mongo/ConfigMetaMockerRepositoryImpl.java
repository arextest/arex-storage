package io.arex.storage.core.repository.impl.mongo;

import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.impl.ConfigMetaMocker;
import org.springframework.stereotype.Repository;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class ConfigMetaMockerRepositoryImpl extends AbstractMongoDbRepository<ConfigMetaMocker> {

    /**
     * @return false ,skip compress
     */
    @Override
    protected boolean enableCompression() {
        return false;
    }

    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.CONFIG_META;
    }

}
