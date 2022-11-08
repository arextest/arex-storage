package com.arextest.storage.repository.impl.mongo.rolling;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.ConfigVersion;
import com.arextest.storage.model.mocker.impl.ConfigFileMocker;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
final class ConfigFileRollingRepositoryProvider extends AbstractRollingRepositoryProvider<ConfigFileMocker> {
    private static final String CONFIG_KEY_COLUMN_NAME = "key";

    public ConfigFileRollingRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.CONFIG_FILE;
    }

    @Override
    protected List<Bson> buildConfigVersionFilters(ConfigVersion version) {
        final List<Bson> bsonList = super.buildConfigVersionFilters(version);
        bsonList.add(Filters.eq(CONFIG_KEY_COLUMN_NAME, version.getKey()));
        return bsonList;
    }
}