package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.ConfigVersion;
import com.arextest.storage.model.mocker.impl.ConfigFileMocker;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class ConfigFileMockerRepositoryImpl extends AbstractMongoDbRepository<ConfigFileMocker> {
    private static final String CONFIG_KEY_COLUMN_NAME = "key";

    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.CONFIG_FILE;
    }

    @Override
    protected List<Bson> buildConfigVersionFetchWhere(ConfigVersion version) {
        final List<Bson> bsonList = super.buildConfigVersionFetchWhere(version);
        bsonList.add(Filters.eq(CONFIG_KEY_COLUMN_NAME, version.getKey()));
        return bsonList;
    }
}
