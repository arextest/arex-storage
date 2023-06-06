package com.arextest.storage.repository.impl.mongo;

import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.SortingOption;
import com.arextest.model.replay.SortingTypeEnum;
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
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
        Integer pageIndex = pagedRequestType.getPageIndex();
        MongoCollection<AREXMocker> collectionSource = createOrGetCollection(categoryType);

        List<Bson> sorts = new ArrayList<>();
        List<SortingOption> sortingOptions = pagedRequestType.getSortingOptions();
        if (CollectionUtils.isEmpty(sortingOptions)) {
            sorts.add(CREATE_TIME_ASCENDING_SORT);
        } else {
            sortingOptions.forEach(sortingOption -> sorts.add(
                    Objects.equals(SortingTypeEnum.ASCENDING.getCode(), sortingOption.getSortingType())
                            ? Sorts.ascending(sortingOption.getLabel())
                            : Sorts.descending(sortingOption.getLabel())));
        }

        AREXMocker item = getLastRecordVersionMocker(pagedRequestType, collectionSource);
        String recordVersion = item == null ? null : item.getRecordVersion();

        Iterable<AREXMocker> iterable = collectionSource
                .find(Filters.and(withRecordVersionFilters(pagedRequestType, recordVersion)))
                .sort(Sorts.orderBy(sorts))
                .skip(pageIndex == null ? 0 : pagedRequestType.getPageSize() * (pageIndex - 1))
                .limit(Math.min(pagedRequestType.getPageSize(), DEFAULT_MAX_LIMIT_SIZE));
        return new AttachmentCategoryIterable(categoryType, iterable);
    }

    private AREXMocker getLastRecordVersionMocker(PagedRequestType pagedRequestType,
                                                  MongoCollection<AREXMocker> collectionSource) {
        return collectionSource
                .find(Filters.and(buildReadRangeFilters(pagedRequestType)))
                .sort(CREATE_TIME_DESCENDING_SORT)
                .limit(DEFAULT_MIN_LIMIT_SIZE)
                .first();
    }

    @Override
    public long countByRange(PagedRequestType rangeRequestType) {
        MongoCollection<AREXMocker> collectionSource = createOrGetCollection(rangeRequestType.getCategory());
        AREXMocker item = getLastRecordVersionMocker(rangeRequestType, collectionSource);
        String recordVersion = item == null ? null : item.getRecordVersion();
        return collectionSource.countDocuments(Filters.and(withRecordVersionFilters(rangeRequestType, recordVersion)));
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

    private List<Bson> withRecordVersionFilters(@NotNull PagedRequestType rangeRequestType, String recordVersion) {
        List<Bson> bsons = buildReadRangeFilters(rangeRequestType);
        if (StringUtils.isNotEmpty(recordVersion)) {
            bsons.add(Filters.eq(AGENT_RECORD_VERSION_COLUMN_NAME, recordVersion));
        }
        return bsons;
    }

    private Bson buildTimeRangeFilter(long beginTime, long endTime) {
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