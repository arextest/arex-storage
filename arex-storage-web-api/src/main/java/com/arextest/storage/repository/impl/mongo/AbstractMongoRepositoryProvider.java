package com.arextest.storage.repository.impl.mongo;

import com.arextest.storage.compression.GenericCompressionBuilder;
import com.arextest.storage.model.mocker.ConfigVersion;
import com.arextest.storage.model.mocker.MockItem;
import com.arextest.storage.model.replay.ReplayCaseRangeRequestType;
import com.arextest.storage.repository.RepositoryProvider;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Slf4j
@SuppressWarnings("unchecked")
public abstract class AbstractMongoRepositoryProvider<T extends MockItem> implements RepositoryProvider<T> {
    private static final String FILE_VERSION_COLUMN_NAME = "fileVersion";
    static final String CREATE_TIME_COLUMN_NAME = "createTime";
    private static final String PRIMARY_KEY_COLUMN_NAME = "_id";
    static final String RECORD_ID_COLUMN_NAME = "recordId";
    private static final String APP_ID_COLUMN_NAME = "appId";
    private static final String ENV_COLUMN_NAME = "env";
    private static final String CONFIG_RECORD_VERSION_COLUMN_NAME = "recordVersion";
    private static final String AGENT_RECORD_VERSION_COLUMN_NAME = "agentVersion";
    private final static Bson CREATE_TIME_ASCENDING_SORT = Sorts.ascending(CREATE_TIME_COLUMN_NAME);
    private final static Bson CREATE_TIME_DESCENDING_SORT = Sorts.descending(CREATE_TIME_COLUMN_NAME);
    private final static Bson FILE_VERSION_DESCENDING_SORT = Sorts.descending(FILE_VERSION_COLUMN_NAME);
    private final Class<T> targetClassType;
    private static final int DEFAULT_MIN_LIMIT_SIZE = 1;
    private static final int DEFAULT_MAX_LIMIT_SIZE = 1000;
    private final GenericCompressionBuilder<T> genericCompressionBuilder;
    private List<Field> compressionFieldList;
    protected final MongoCollection<T> collectionSource;
    private static final int DEFAULT_BSON_WHERE_SIZE = 8;

    public AbstractMongoRepositoryProvider(MongoDatabase mongoDatabase) {
        this((GenericCompressionBuilder<T>) GenericCompressionBuilder.DEFAULT, mongoDatabase);
    }

    public AbstractMongoRepositoryProvider(GenericCompressionBuilder<T> genericCompressionBuilder, MongoDatabase mongoDatabase) {
        this.targetClassType = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
        this.genericCompressionBuilder = genericCompressionBuilder;
        this.collectionSource = createOrGetCollection(mongoDatabase);
        if (enableCompression()) {
            this.compressionFieldList = genericCompressionBuilder.discoverCompression(this.targetClassType);
        }
    }

    private MongoCollection<T> createOrGetCollection(MongoDatabase mongoDatabase) {
        return mongoDatabase.getCollection(collectionName(), this.targetClassType);
    }

    protected String collectionName() {
        return this.getCategory().getMocker();
    }

    @Override
    public Iterable<T> queryRecordList(String recordId) {
        return this.queryRecordList(buildRecordIdFilter(recordId), this.collectionSource);
    }

    private DecodeIterable queryRecordList(Bson recordId, MongoCollection<T> mongoCollection) {
        Iterable<T> iterableResult = mongoCollection
                .find(recordId)
                .sort(CREATE_TIME_ASCENDING_SORT);
        return new DecodeIterable(iterableResult);
    }

    @Override
    public T queryByVersion(ConfigVersion versionRequestType) {
        T item = this.collectionSource
                .find(Filters.and(buildConfigVersionFilters(versionRequestType)))
                .sort(FILE_VERSION_DESCENDING_SORT)
                .limit(DEFAULT_MIN_LIMIT_SIZE)
                .first();
        return decode(item);
    }

    @Override
    public T queryRecord(String recordId) {
        T item = this.collectionSource
                .find(buildRecordIdFilter(recordId))
                .sort(CREATE_TIME_DESCENDING_SORT)
                .limit(DEFAULT_MIN_LIMIT_SIZE)
                .first();
        return decode(item);
    }

    @Override
    public Iterable<T> queryByRange(ReplayCaseRangeRequestType rangeRequestType) {
        Iterable<T> iterableResult = this.collectionSource
                .find(Filters.and(buildReadRangeFilters(rangeRequestType)))
                .sort(CREATE_TIME_ASCENDING_SORT)
                .limit(Math.min(rangeRequestType.getMaxCaseCount(), DEFAULT_MAX_LIMIT_SIZE));
        return new DecodeIterable(iterableResult);
    }

    @Override
    public int countByRange(ReplayCaseRangeRequestType rangeRequestType) {
        return (int) this.collectionSource.countDocuments(Filters.and(buildReadRangeFilters(rangeRequestType)));
    }


    @Override
    public boolean save(T value) {
        if (value == null) {
            return false;
        }
        return this.saveList(Collections.singletonList(value));
    }

    @Override
    public boolean saveList(List<T> valueList) {
        if (CollectionUtils.isEmpty(valueList)) {
            return false;
        }
        try {
            if (enableCompression()) {
                compress(valueList);
            }
            this.collectionSource.insertMany(valueList);
        } catch (Throwable ex) {
            LOGGER.error("save List error:{} , size:{}", ex.getMessage(), valueList.size(), ex);
            return false;
        }
        return true;
    }

    @Override
    public void removeBy(String recordId) {
        this.collectionSource.deleteMany(buildRecordIdFilter(recordId));
    }

    @Override
    public boolean update(T objectValue) {
        Bson primaryKeyFilter = buildPrimaryKeyFilter(objectValue);
        if (enableCompression()) {
            compress(Collections.singletonList(objectValue));
        }
        try {
            return this.collectionSource.replaceOne(primaryKeyFilter, objectValue).getModifiedCount() > 0;
        } catch (Exception c) {
            LOGGER.error("update record error:{} ", c.getMessage(), c);
            return false;
        }

    }

    private Bson buildPrimaryKeyFilter(T objectValue) {
        if (this.getCategory().isMainEntry()) {
            return Filters.eq(PRIMARY_KEY_COLUMN_NAME, objectValue.getId());
        }
        return Filters.eq(PRIMARY_KEY_COLUMN_NAME, new ObjectId(objectValue.getId()));
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

    protected List<Bson> buildConfigVersionFilters(ConfigVersion version) {
        Bson app = Filters.eq(APP_ID_COLUMN_NAME, version.getAppId());
        final List<Bson> bsonList = new ArrayList<>(DEFAULT_BSON_WHERE_SIZE);
        bsonList.add(app);
        if (version.getRecordVersion() != null) {
            bsonList.add(Filters.eq(CONFIG_RECORD_VERSION_COLUMN_NAME, version.getRecordVersion()));
        }
        return bsonList;
    }


    private Bson buildRecordIdFilter(String value) {
        if (this.getCategory().isMainEntry()) {
            return Filters.eq(PRIMARY_KEY_COLUMN_NAME, value);
        }
        return Filters.eq(RECORD_ID_COLUMN_NAME, value);
    }


    protected List<Bson> buildReadRangeFilters(@NotNull ReplayCaseRangeRequestType rangeRequestType) {
        List<Bson> queryItems = new ArrayList<>(DEFAULT_BSON_WHERE_SIZE);
        Bson item = Filters.eq(APP_ID_COLUMN_NAME, rangeRequestType.getAppId());
        queryItems.add(item);
        if (rangeRequestType.getEnv() != null) {
            item = Filters.eq(ENV_COLUMN_NAME, rangeRequestType.getEnv());
            queryItems.add(item);
        }
        item = buildTimeRangeFilter(rangeRequestType.getBeginTime(), rangeRequestType.getEndTime());
        queryItems.add(item);
        String agentRecordVersion = rangeRequestType.getAgentRecordVersion();
        if (StringUtils.isNotEmpty(agentRecordVersion)) {
            item = Filters.eq(AGENT_RECORD_VERSION_COLUMN_NAME, agentRecordVersion);
            queryItems.add(item);
        }
        return queryItems;
    }

    private Bson buildTimeRangeFilter(long beginTime, long endTime) {
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
    }
}