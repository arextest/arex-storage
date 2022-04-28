package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.model.enums.MockCategoryType;
import com.arextest.storage.model.mocker.impl.QmqConsumerMocker;
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
final class QmqConsumerMockerRepositoryImpl extends AbstractMongoDbRepository<QmqConsumerMocker> {
    private static final String SUBJECT_COLUMN_NAME = "subject";

    @Override
    public final MockCategoryType getCategory() {
        return MockCategoryType.QMQ_CONSUMER;
    }

    @Override
    protected List<Bson> buildRangeQueryWhere(@NotNull ReplayCaseRangeRequestType rangeRequestType) {
        List<Bson> queryItems = super.buildRangeQueryWhere(rangeRequestType);
        String value = rangeRequestType.getSubject();
        if (StringUtils.isNotEmpty(value)) {
            queryItems.add(Filters.eq(SUBJECT_COLUMN_NAME, value));
        }
        return queryItems;
    }
}
