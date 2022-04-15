package io.arex.storage.core.repository.impl.mongo;

import io.arex.storage.model.enums.MockCategoryType;
import io.arex.storage.model.mocker.impl.ServletMocker;
import io.arex.storage.model.replay.ReplayCaseRangeRequestType;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author yongwuhe
 */
@Repository
public class ServletMockerRepositoryImpl extends AbstractMongoDbRepository<ServletMocker> {
    private static final String PATTERN_COLUMN_NAME = "pattern";

    @Override
    public MockCategoryType getCategory() {
        return MockCategoryType.SERVLET_ENTRANCE;
    }

    @Override
    protected List<Bson> buildRangeQueryWhere(@NotNull ReplayCaseRangeRequestType rangeRequestType) {
        List<Bson> queryItems = super.buildRangeQueryWhere(rangeRequestType);
        String value = rangeRequestType.getOperation();
        if (value != null) {
            queryItems.add(Filters.eq(PATTERN_COLUMN_NAME, value));
        }
        return queryItems;
    }
}
