package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.SoaMainMocker;
import com.arextest.storage.model.replay.ReplayCaseRangeRequestType;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jmo
 * @since 2021/11/7
 */
@Repository
final class SoaMainMockerRepositoryImpl extends AbstractMongoDbRepository<SoaMainMocker> {
    private static final String SERVICE_COLUMN_NAME = "service";
    private static final String OPERATION_COLUMN_NAME = "operation";

    @Override
    protected List<Bson> buildRangeQueryWhere(@NotNull ReplayCaseRangeRequestType rangeRequestType) {
        List<Bson> queryItems = super.buildRangeQueryWhere(rangeRequestType);
        String value = rangeRequestType.getService();
        if (StringUtils.isNotEmpty(value)) {
            queryItems.add(Filters.eq(SERVICE_COLUMN_NAME, value));
        }
        value = rangeRequestType.getOperation();
        if (StringUtils.isNotEmpty(value)) {
            queryItems.add(Filters.eq(OPERATION_COLUMN_NAME, value));
        }
        return queryItems;
    }

    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.SOA_MAIN;
    }
}
