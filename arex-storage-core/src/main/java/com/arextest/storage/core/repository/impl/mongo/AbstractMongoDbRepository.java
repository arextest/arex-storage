package com.arextest.storage.core.repository.impl.mongo;

import com.arextest.storage.core.compression.GenericCompressionBuilder;
import com.arextest.storage.core.repository.RepositoryProvider;
import com.arextest.storage.model.enums.MockResultType;
import com.arextest.storage.model.mocker.ConfigVersion;
import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.model.replay.ReplayCaseRangeRequestType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author jmo
 * @since 2021/11/7
 */

@Slf4j
abstract class AbstractMongoDbRepository<T extends MockItem> implements RepositoryProvider<T> {
    static final String FILE_VERSION_COLUMN_NAME = "fileVersion";
    static final String CREATE_TIME_COLUMN_NAME = "createTime";
    private static final String PRIMARY_KEY_COLUMN_NAME = "_id";
    private static final String RECORD_ID_COLUMN_NAME = "recordId";
    private static final String APP_ID_COLUMN_NAME = "appId";
    private static final String CASE_FROM_ENV_COLUMN_NAME = "isTest";
    private static final String ENV_COLUMN_NAME = "env";
    private static final String REPLAY_ID_COLUMN_NAME = "replayId";
    private static final String MOCKED_RESULT_TYPE_COLUMN_NAME = "type";
    private static final String CONFIG_RECORD_VERSION_COLUMN_NAME = "recordVersion";
    private static final String AGENT_RECORD_VERSION_COLUMN_NAME = "agentVersion";
    private static final String PINNED_COLLECTION_SUFFIX_NAME = "Fixed";
    private final static Bson CREATE_TIME_ASCENDING_SORT = Sorts.ascending(CREATE_TIME_COLUMN_NAME);
    private final static Bson CREATE_TIME_DESCENDING_SORT = Sorts.descending(CREATE_TIME_COLUMN_NAME);
    private final static Bson FILE_VERSION_DESCENDING_SORT = Sorts.descending(FILE_VERSION_COLUMN_NAME);
    private final Class<T> targetClassType;
    private static final int DEFAULT_MIN_LIMIT_SIZE = 1;
    private static final int DEFAULT_MAX_LIMIT_SIZE = 1000;
    private final GenericCompressionBuilder<T> genericCompressionBuilder;
    private List<Field> compressionFieldList;
    private MongoCollection<T> mongoRollingCollectionSource;
    private MongoCollection<T> mongoPinnedCollectionSource;
    private static final int DEFAULT_BSON_WHERE_SIZE = 8;
    @Resource
    private MongoDatabase mongoDatabase;

    @SuppressWarnings("unchecked")
    protected AbstractMongoDbRepository() {
        this((GenericCompressionBuilder<T>) GenericCompressionBuilder.DEFAULT);
    }

    @SuppressWarnings("unchecked")
    protected AbstractMongoDbRepository(GenericCompressionBuilder<T> genericCompressionBuilder) {
        this.targetClassType = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
        this.genericCompressionBuilder = genericCompressionBuilder;
    }


    @PostConstruct
    private void initMongoCollection() {
        final String name = collectionName();
        this.mongoRollingCollectionSource = createOrGetCollection(name);
        this.mongoPinnedCollectionSource = createOrGetCollection(name + PINNED_COLLECTION_SUFFIX_NAME);
        if (enableCompression()) {
            this.compressionFieldList = genericCompressionBuilder.discoverCompression(this.targetClassType);
        }
    }

    /**
     * If a collection does not exist, MongoDB creates the collection when you first store data for that collection.
     *
     * @param collectionName Specify the name of the collection to the getCollection() method.
     * @return MongoCollection
     */
    final MongoCollection<T> createOrGetCollection(String collectionName) {
        return mongoDatabase.getCollection(collectionName, this.targetClassType);
    }

    @Override
    public Iterable<T> queryRecordList(String recordId) {
        DecodeIterable decodeIterable = this.queryRecordList(pinnedRecordIdWhere(recordId),
                this.mongoPinnedCollectionSource);
        if (decodeIterable.isEmpty()) {
            return this.queryRecordList(fromRecordIdWhere(recordId), this.mongoRollingCollectionSource);
        }
        return decodeIterable;
    }

    final DecodeIterable queryRecordList(Bson recordId, MongoCollection<T> mongoCollection) {
        Iterable<T> iterableResult = mongoCollection
                .find(recordId)
                .sort(CREATE_TIME_ASCENDING_SORT);
        return new DecodeIterable(iterableResult);
    }

    @Override
    public T queryByVersion(ConfigVersion versionRequestType) {
        T item = this.mongoRollingCollectionSource
                .find(Filters.and(buildConfigVersionFetchWhere(versionRequestType)))
                .sort(FILE_VERSION_DESCENDING_SORT)
                .limit(DEFAULT_MIN_LIMIT_SIZE)
                .first();
        return decode(item);
    }

    @Override
    public T queryRecord(String recordId) {
        T result = this.queryRecord(recordId, this.mongoPinnedCollectionSource);
        if (result == null) {
            return this.queryRecord(recordId, this.mongoRollingCollectionSource);
        }
        return result;
    }

    final T queryRecord(String recordId, MongoCollection<T> mongoCollection) {
        T item = mongoCollection
                .find(fromRecordIdWhere(recordId))
                .sort(CREATE_TIME_DESCENDING_SORT)
                .limit(DEFAULT_MIN_LIMIT_SIZE)
                .first();
        return decode(item);
    }

    @Override
    public Iterable<T> queryByRange(ReplayCaseRangeRequestType rangeRequestType) {
        return this.queryByRange(rangeRequestType, this.mongoRollingCollectionSource);
    }

    final DecodeIterable queryByRange(ReplayCaseRangeRequestType rangeRequestType, MongoCollection<T> mongoCollection) {
        Iterable<T> iterableResult = mongoCollection
                .find(Filters.and(buildRangeQueryWhere(rangeRequestType)))
                .sort(CREATE_TIME_ASCENDING_SORT)
                .limit(Math.min(rangeRequestType.getMaxCaseCount(), DEFAULT_MAX_LIMIT_SIZE));
        return new DecodeIterable(iterableResult);
    }

    @Override
    public int countByRange(ReplayCaseRangeRequestType rangeRequestType) {
        return (int) this.countByRange(rangeRequestType, this.mongoRollingCollectionSource);
    }

    final long countByRange(ReplayCaseRangeRequestType rangeRequestType, MongoCollection<T> mongoCollection) {
        return mongoCollection.countDocuments(Filters.and(buildRangeQueryWhere(rangeRequestType)));
    }

    @Override
    public Iterable<T> queryReplayResult(String replayResultId) {
        throw new NotImplementedException("queryReplayResult");
    }

    @Override
    public boolean save(T objectValue) {
        if (objectValue == null) {
            return false;
        }
        return this.saveList(Collections.singletonList(objectValue));
    }

    @Override
    public boolean saveList(List<T> objectValueList) {
        if (CollectionUtils.isEmpty(objectValueList)) {
            return false;
        }
        try {
            if (enableCompression()) {
                compress(objectValueList);
            }
            this.mongoRollingCollectionSource.insertMany(objectValueList);
        } catch (Throwable ex) {
            LOGGER.error("save List error:{} , size:{}", ex.getMessage(), objectValueList.size(), ex);
            return false;
        }
        return true;
    }

    @Override
    public boolean saveFixedRecord(T objectValue) {
        if (objectValue == null) {
            return false;
        }

        List<T> objectValueList = Collections.singletonList(objectValue);
        try {
            if (enableCompression()) {
                compress(objectValueList);
            }
            this.mongoPinnedCollectionSource.insertMany(objectValueList);
        } catch (Throwable ex) {
            LOGGER.error("save fixed List error:{} , size:{}", ex.getMessage(), objectValueList.size(), ex);
            return false;
        }
        return true;
    }

    private void compress(List<T> objectValueList) {
        T item;
        for (int i = 0; i < objectValueList.size(); i++) {
            item = objectValueList.get(i);
            if (item != null) {
                this.genericCompressionBuilder.compress(item, compressionFieldList);
            }
        }
    }

    private void decompress(T item) {
        this.genericCompressionBuilder.decompress(item, compressionFieldList);
    }

    /**
     * use replay id instead of record id from old version
     */
    private void alternativeRecordId(T item) {
        if (StringUtils.isEmpty(item.getRecordId())) {
            item.setRecordId(item.getReplayId());
        }
    }

    protected List<Bson> buildConfigVersionFetchWhere(ConfigVersion version) {
        Bson app = Filters.eq(APP_ID_COLUMN_NAME, version.getAppId());
        final List<Bson> bsonList = new ArrayList<>(DEFAULT_BSON_WHERE_SIZE);
        bsonList.add(app);
        if (version.getRecordVersion() != null) {
            bsonList.add(Filters.eq(CONFIG_RECORD_VERSION_COLUMN_NAME, version.getRecordVersion()));
        }
        return bsonList;
    }

    /**
     * like sql: where name=value and (name2=value2 or name2 is null)
     */
    private Bson pinnedRecordIdWhere(String value) {
        Bson caseId = Filters.and(Filters.eq(REPLAY_ID_COLUMN_NAME, value),
                Filters.eq(MOCKED_RESULT_TYPE_COLUMN_NAME, MockResultType.RECORD_RESULT.getCodeValue()));
        Bson recordId = Filters.eq(RECORD_ID_COLUMN_NAME, value);
        if (this.getCategory().isMainEntry()) {
            Bson primaryId = Filters.eq(PRIMARY_KEY_COLUMN_NAME, value);
            return Filters.or(primaryId, recordId, caseId);
        }
        return Filters.or(recordId, caseId);
    }

    private Bson fromRecordIdWhere(String value) {
        if (this.getCategory().isMainEntry()) {
            return Filters.eq(PRIMARY_KEY_COLUMN_NAME, value);
        }
        return Filters.eq(RECORD_ID_COLUMN_NAME, value);
    }

    @NotNull
    private String collectionName() {
        return this.getCategory().getMocker();
    }

    protected List<Bson> buildRangeQueryWhere(@NotNull ReplayCaseRangeRequestType rangeRequestType) {
        List<Bson> queryItems = new ArrayList<>(DEFAULT_BSON_WHERE_SIZE);
        Bson item = Filters.eq(APP_ID_COLUMN_NAME, rangeRequestType.getAppId());
        queryItems.add(item);
        if (rangeRequestType.getEnv() != null) {
            item = Filters.or(Filters.eq(CASE_FROM_ENV_COLUMN_NAME, rangeRequestType.getEnv()),
                    Filters.eq(ENV_COLUMN_NAME, rangeRequestType.getEnv()));
            queryItems.add(item);
        }
        item = buildCreateTimeRange(rangeRequestType.getBeginTime(), rangeRequestType.getEndTime());
        queryItems.add(item);
        String agentRecordVersion = rangeRequestType.getAgentRecordVersion();
        if (StringUtils.isNotEmpty(agentRecordVersion)) {
            item = Filters.eq(AGENT_RECORD_VERSION_COLUMN_NAME, agentRecordVersion);
            queryItems.add(item);
        }
        return queryItems;
    }

    private Bson buildCreateTimeRange(long beginTime, long endTime) {
        Bson newItemFrom = Filters.gt(CREATE_TIME_COLUMN_NAME, new Date(beginTime));
        Bson newItemTo = Filters.lte(CREATE_TIME_COLUMN_NAME, new Date(endTime));
        return Filters.or(Filters.and(newItemFrom, newItemTo));
    }

    /**
     * @return default true ,should be compression
     */
    protected boolean enableCompression() {
        return true;
    }

    private T decode(T source) {
        if (source == null) {
            return null;
        }
        alternativeRecordId(source);
        if (enableCompression()) {
            decompress(source);
        }
        return source;
    }

    private final class DecodeIterable implements Iterable<T>, Iterator<T> {
        private Iterator<T> iterator;
        private final Iterable<T> iterable;

        private DecodeIterable(Iterable<T> iterable) {
            this.iterable = iterable;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<T> iterator() {
            if (this.iterable != null) {
                this.iterator = iterable.iterator();
            }
            return this;
        }

        @Override
        public boolean hasNext() {
            return iterator != null && iterator.hasNext();
        }

        @Override
        public T next() {
            T source = iterator.next();
            return decode(source);
        }

        final boolean isEmpty() {
            return !this.iterator().hasNext();
        }
    }
}
