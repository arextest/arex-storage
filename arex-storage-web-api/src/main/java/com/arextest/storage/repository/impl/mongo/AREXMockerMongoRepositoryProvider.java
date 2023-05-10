package com.arextest.storage.repository.impl.mongo;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * The rolling provider used by default,
 * which means auto deleted the records after TTL index created on creationTime of collection
 */
@Slf4j
public class AREXMockerMongoRepositoryProvider implements RepositoryProvider<AREXMocker> {

    static final String CREATE_TIME_COLUMN_NAME = "creationTime";
    public static final String PRIMARY_KEY_COLUMN_NAME = "_id";
    static final String RECORD_ID_COLUMN_NAME = "recordId";
    private static final String APP_ID_COLUMN_NAME = "appId";
    private static final String ENV_COLUMN_NAME = "recordEnvironment";
    private static final String OPERATION_COLUMN_NAME = "operationName";
    private static final String COLLECTION_PREFIX = "Mocker";

    private static final String AGENT_RECORD_VERSION_COLUMN_NAME = "recordVersion";
    private final static Bson CREATE_TIME_ASCENDING_SORT = Sorts.ascending(CREATE_TIME_COLUMN_NAME);
    private final static Bson CREATE_TIME_DESCENDING_SORT = Sorts.descending(CREATE_TIME_COLUMN_NAME);
    private final static Bson PRIMARY_KEY_DESCENDING_SORT = Sorts.descending(PRIMARY_KEY_COLUMN_NAME);


    private final Class<AREXMocker> targetClassType;
    private static final int DEFAULT_MIN_LIMIT_SIZE = 1;
    private static final int DEFAULT_MAX_LIMIT_SIZE = 1000;
    private static final int DEFAULT_BSON_WHERE_SIZE = 8;
    protected final MongoDatabase mongoDatabase;
    private final String providerName;

    public AREXMockerMongoRepositoryProvider(MongoDatabase mongoDatabase) {
        this(ProviderNames.DEFAULT, mongoDatabase);
    }

    public AREXMockerMongoRepositoryProvider(String providerName, MongoDatabase mongoDatabase) {
        this.targetClassType = AREXMocker.class;
        this.mongoDatabase = mongoDatabase;
        this.providerName = providerName;
    }

    private MongoCollection<AREXMocker> createOrGetCollection(MockCategoryType category) {
        String categoryName = this.getProviderName() + category.getName() + COLLECTION_PREFIX;
        return mongoDatabase.getCollection(categoryName, this.targetClassType);
    }

    @Override
    public Iterable<AREXMocker> queryRecordList(MockCategoryType category, String recordId) {
        MongoCollection<AREXMocker> collectionSource = createOrGetCollection(category);
        Bson recordIdFilter = buildRecordIdFilter(category, recordId);
        Iterable<AREXMocker> iterable = collectionSource
                .find(recordIdFilter)
                .sort(CREATE_TIME_ASCENDING_SORT);
        return new AttachmentCategoryIterable(category, iterable);
    }

    @Override
    public AREXMocker queryRecord(Mocker requestType) {
        MockCategoryType categoryType = requestType.getCategoryType();
        MongoCollection<AREXMocker> collectionSource = createOrGetCollection(categoryType);
        AREXMocker item = collectionSource
                .find(Filters.and(buildRecordFilters(categoryType, requestType)))
                .sort(CREATE_TIME_DESCENDING_SORT)
                .limit(DEFAULT_MIN_LIMIT_SIZE)
                .first();
        return AttachmentCategoryIterable.attach(categoryType, item);
    }

    @Override
    public Iterable<AREXMocker> queryByRange(PagedRequestType pagedRequestType) {
        MockCategoryType categoryType = pagedRequestType.getCategory();
        MongoCollection<AREXMocker> collectionSource = createOrGetCollection(categoryType);
        List<Bson> filters = buildReadRangeFilters(pagedRequestType);

        if (BooleanUtils.isNotFalse(pagedRequestType.getFilterPastRecordVersion())) {
            withRecordVersionFilters(filters, collectionSource);
        }
        Iterable<AREXMocker> iterable = collectionSource
                .find(Filters.and(filters))
                .sort(CREATE_TIME_ASCENDING_SORT)
                .limit(Math.min(pagedRequestType.getPageSize(), DEFAULT_MAX_LIMIT_SIZE));
        return new AttachmentCategoryIterable(categoryType, iterable);
    }

    @Override
    public Iterable<AREXMocker> queryRecordListPaging(PagedRequestType pagedRequestType, String lastId) {
        MockCategoryType categoryType = pagedRequestType.getCategory();
        MongoCollection<AREXMocker> collectionSource = createOrGetCollection(categoryType);
        List<Bson> filters = this.buildAppIdWithOperationFilters(pagedRequestType.getAppId(),
                pagedRequestType.getOperation());
        if (pagedRequestType.getEnv() != null) {
            filters.add(Filters.eq(ENV_COLUMN_NAME, pagedRequestType.getEnv()));
        }
        if (StringUtils.isNotEmpty(lastId)) {
            filters.add(Filters.lt(PRIMARY_KEY_COLUMN_NAME, lastId));
        }
        if (BooleanUtils.isNotFalse(pagedRequestType.getFilterPastRecordVersion())) {
            withRecordVersionFilters(filters, collectionSource);
        }

        Iterable<AREXMocker> iterable = collectionSource
                .find(Filters.and(filters))
                .sort(PRIMARY_KEY_DESCENDING_SORT)
                .limit(Math.min(pagedRequestType.getPageSize(), DEFAULT_MAX_LIMIT_SIZE));
        return new AttachmentCategoryIterable(categoryType, iterable);
    }

    @Override
    public long countByRange(PagedRequestType rangeRequestType) {
        MongoCollection<AREXMocker> collectionSource = createOrGetCollection(rangeRequestType.getCategory());
        List<Bson> filters = buildReadRangeFilters(rangeRequestType);

        if (BooleanUtils.isNotFalse(rangeRequestType.getFilterPastRecordVersion())) {
            withRecordVersionFilters(filters, collectionSource);
        }
        return collectionSource.countDocuments(Filters.and(filters));
    }


    @Override
    public boolean save(AREXMocker value) {
        if (value == null) {
            return false;
        }
        return this.saveList(Collections.singletonList(value));
    }

    @Override
    public boolean saveList(List<AREXMocker> valueList) {
        if (CollectionUtils.isEmpty(valueList)) {
            return false;
        }
        try {
            MockCategoryType category = valueList.get(0).getCategoryType();
            MongoCollection<AREXMocker> collectionSource = createOrGetCollection(category);
            collectionSource.insertMany(valueList);
        } catch (Throwable ex) {
            LOGGER.error("save List error:{} , size:{}", ex.getMessage(), valueList.size(), ex);
            return false;
        }
        return true;
    }

    @Override
    public long removeBy(MockCategoryType categoryType, String recordId) {
        MongoCollection<AREXMocker> collectionSource = createOrGetCollection(categoryType);
        DeleteResult deleteResult = collectionSource.deleteMany(buildRecordIdFilter(categoryType, recordId));
        return deleteResult.getDeletedCount();
    }

    @Override
    public boolean update(AREXMocker value) {
        Bson primaryKeyFilter = buildPrimaryKeyFilter(value);
        try {
            MongoCollection<AREXMocker> collectionSource = createOrGetCollection(value.getCategoryType());
            return collectionSource.replaceOne(primaryKeyFilter, value).getModifiedCount() > 0;
        } catch (Exception e) {
            LOGGER.error("update record error:{} ", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return this.providerName;
    }

    private Bson buildPrimaryKeyFilter(Mocker value) {
        return Filters.eq(PRIMARY_KEY_COLUMN_NAME, value.getId());
    }

    private List<Bson> buildAppIdWithOperationFilters(String appId, String operationName) {
        Bson app = Filters.eq(APP_ID_COLUMN_NAME, appId);
        final List<Bson> bsonList = new ArrayList<>(DEFAULT_BSON_WHERE_SIZE);
        bsonList.add(app);
        if (operationName != null) {
            bsonList.add(Filters.eq(OPERATION_COLUMN_NAME, operationName));
        }
        return bsonList;
    }

    private Bson buildRecordIdFilter(MockCategoryType categoryType, String value) {
        if (categoryType.isEntryPoint()) {
            return Filters.eq(PRIMARY_KEY_COLUMN_NAME, value);
        }
        return Filters.eq(RECORD_ID_COLUMN_NAME, value);
    }

    private List<Bson> buildRecordFilters(MockCategoryType categoryType, @NotNull Mocker mocker) {
        List<Bson> filters = this.buildAppIdWithOperationFilters(mocker.getAppId(),
                mocker.getOperationName());
        Bson recordIdFilter = buildRecordIdFilter(categoryType, mocker.getRecordId());
        filters.add(recordIdFilter);
        Bson env = Filters.eq(ENV_COLUMN_NAME, mocker.getRecordEnvironment());
        filters.add(env);
        return filters;
    }
    private List<Bson> buildReadRangeFilters(@NotNull PagedRequestType rangeRequestType) {
        List<Bson> filters = this.buildAppIdWithOperationFilters(rangeRequestType.getAppId(),
                rangeRequestType.getOperation());
        Bson item;
        if (rangeRequestType.getEnv() != null) {
            item = Filters.eq(ENV_COLUMN_NAME, rangeRequestType.getEnv());
            filters.add(item);
        }
        item = buildTimeRangeFilter(rangeRequestType.getBeginTime(), rangeRequestType.getEndTime());
        filters.add(item);

        return filters;
    }

    private void withRecordVersionFilters(@NotNull List<Bson> filters, @NotNull MongoCollection<AREXMocker> collectionSource) {
        AREXMocker item = collectionSource
                .find(Filters.and(filters))
                .sort(CREATE_TIME_DESCENDING_SORT)
                .limit(DEFAULT_MIN_LIMIT_SIZE)
                .first();
        if (item != null && StringUtils.isNotEmpty(item.getRecordVersion())) {
            filters.add(Filters.eq(AGENT_RECORD_VERSION_COLUMN_NAME, item.getRecordVersion()));
        }
    }

    private Bson buildTimeRangeFilter(Long beginTime, Long endTime) {
        beginTime = beginTime == null ? 0L : beginTime;
        endTime = endTime == null ? System.currentTimeMillis() : endTime;
        Bson newItemFrom = Filters.gt(CREATE_TIME_COLUMN_NAME, new Date(beginTime));
        Bson newItemTo = Filters.lte(CREATE_TIME_COLUMN_NAME, new Date(endTime));
        return Filters.and(newItemFrom, newItemTo);
    }

    private static final class AttachmentCategoryIterable implements Iterable<AREXMocker>, Iterator<AREXMocker> {
        private final MockCategoryType categoryType;
        private final Iterator<AREXMocker> source;

        private AttachmentCategoryIterable(MockCategoryType categoryType, Iterable<AREXMocker> source) {
            this.categoryType = categoryType;
            this.source = source.iterator();
        }

        @Override
        public Iterator<AREXMocker> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return source.hasNext();
        }

        @Override
        public AREXMocker next() {
            return attach(categoryType, source.next());
        }

        private static AREXMocker attach(MockCategoryType categoryType, AREXMocker item) {
            if (item != null) {
                item.setCategoryType(categoryType);
                if (categoryType.isEntryPoint()) {
                    item.setRecordId(item.getId());
                }
            }
            return item;
        }
    }
}