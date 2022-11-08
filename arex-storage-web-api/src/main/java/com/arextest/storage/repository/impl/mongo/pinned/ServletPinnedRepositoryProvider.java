package com.arextest.storage.repository.impl.mongo.pinned;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.ServletMocker;
import com.arextest.storage.model.replay.ReplayCaseRangeRequestType;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author yongwuhe
 */
@Repository
public class ServletPinnedRepositoryProvider extends AbstractPinnedRepositoryProvider<ServletMocker> {
    private static final String PATTERN_COLUMN_NAME = "pattern";

    public ServletPinnedRepositoryProvider(MongoDatabase mongoDatabase) {
        super(mongoDatabase);
    }

    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.SERVLET_ENTRANCE;
    }

    @Override
    protected List<Bson> buildReadRangeFilters(@NotNull ReplayCaseRangeRequestType rangeRequestType) {
        List<Bson> queryItems = super.buildReadRangeFilters(rangeRequestType);
        String value = rangeRequestType.getOperation();
        if (value != null) {
            queryItems.add(Filters.eq(PATTERN_COLUMN_NAME, value));
        }
        return queryItems;
    }
}