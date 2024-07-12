package com.arextest.storage.repository.impl.mongo;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.model.mock.AREXMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.model.replay.SortingOption;
import com.arextest.model.replay.SortingTypeEnum;
import com.arextest.model.util.MongoCounter;
import com.arextest.storage.beans.StorageConfigurationProperties;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import com.arextest.storage.utils.TimeUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.pojo.IdGenerators;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * The rolling provider used by default, which means auto deleted the records after TTL index
 * created on creationTime of collection
 */
@Slf4j
@EnableConfigurationProperties({StorageConfigurationProperties.class})
public class AREXMockerMongoRepositoryProvider implements RepositoryProvider<AREXMocker> {

  public static final String PRIMARY_KEY_COLUMN_NAME = "_id";
  static final String CREATE_TIME_COLUMN_NAME = "creationTime";
  static final String UPDATE_TIME_COLUMN_NAME = "updateTime";
  static final String EXPIRATION_TIME_COLUMN_NAME = "expirationTime";
  static final String RECORD_ID_COLUMN_NAME = "recordId";
  private static final String APP_ID_COLUMN_NAME = "appId";
  private static final String ENV_COLUMN_NAME = "recordEnvironment";
  private static final String OPERATION_COLUMN_NAME = "operationName";
  private static final String COLLECTION_PREFIX = "Mocker";

  private static final String AGENT_RECORD_VERSION_COLUMN_NAME = "recordVersion";
  private static final String TARGET_RESPONSE_COLUMN_NAME = "targetResponse";
  private static final String TAGS_COLUMN_NAME = "tags";

  // region: the options of mongodb
  private static final String DOT_OP = ".";
  // endregion

  private static final String EIGEN_MAP_COLUMN_NAME = "eigenMap";
  private final static Sort CREATE_TIME_ASCENDING_SORT = Sort.by(Direction.ASC, CREATE_TIME_COLUMN_NAME);
  private final static Sort CREATE_TIME_DESCENDING_SORT = Sort.by(Direction.DESC, CREATE_TIME_COLUMN_NAME);
  private static final int DEFAULT_MIN_LIMIT_SIZE = 1;
  private static final int DEFAULT_MAX_LIMIT_SIZE = 1000;
  private static final String AUTO_PINNED_MOCKER_EXPIRATION_MILLIS = "AutoPinned.mocker.expiration.millis";
  private static final long FOURTEEN_DAYS_MILLIS = TimeUnit.DAYS.toMillis(14L);
  protected final MongoTemplate mongoTemplate;
  private final String providerName;
  private final StorageConfigurationProperties properties;
  private final Set<MockCategoryType> entryPointTypes;
  private final DefaultApplicationConfig defaultApplicationConfig;

  private static final String[] DEFAULT_INCLUDE_FIELDS =
      new String[]{AREXMocker.Fields.id, AREXMocker.Fields.categoryType, AREXMocker.Fields.recordId,
          AREXMocker.Fields.appId, AREXMocker.Fields.recordEnvironment, AREXMocker.Fields.creationTime,
          AREXMocker.Fields.expirationTime, AREXMocker.Fields.targetRequest, AREXMocker.Fields.operationName,
          AREXMocker.Fields.tags, AREXMocker.Fields.recordVersion};

  public AREXMockerMongoRepositoryProvider(MongoTemplate mongoTemplate,
      StorageConfigurationProperties properties,
      Set<MockCategoryType> entryPointTypes,
      DefaultApplicationConfig defaultApplicationConfig) {
    this(ProviderNames.DEFAULT, mongoTemplate, properties, entryPointTypes, defaultApplicationConfig);
  }

  public AREXMockerMongoRepositoryProvider(String providerName,
      MongoTemplate mongoTemplate,
      StorageConfigurationProperties properties,
      Set<MockCategoryType> entryPointTypes,
      DefaultApplicationConfig defaultApplicationConfig) {
    this.properties = properties;
    this.mongoTemplate = mongoTemplate;
    this.providerName = providerName;
    this.entryPointTypes = entryPointTypes;
    this.defaultApplicationConfig = defaultApplicationConfig;
  }

  private String getCollectionName(MockCategoryType category) {
    return this.getProviderName() + category.getName() + COLLECTION_PREFIX;
  }

  public Iterable<AREXMocker> queryRecordList(MockCategoryType category, String recordId) {
    return queryRecordList(category, recordId, null);
  }

  @Override
  public Iterable<AREXMocker> queryRecordList(MockCategoryType category, String recordId, String[] fieldNames) {
    Criteria criteria = buildRecordIdFilter(category, recordId);

    if (Objects.equals(this.providerName, ProviderNames.DEFAULT)) {
      updateExpirationTime(criteria, getCollectionName(category));
    }

    Query query = new Query(criteria);
    if (ArrayUtils.isNotEmpty(fieldNames)) {
      query.fields().include(fieldNames);
    }

    Iterable<AREXMocker> iterable = mongoTemplate.find(query,
        AREXMocker.class, getCollectionName(category));
    iterable.forEach(this::addUseMocker);
    return new AttachmentCategoryIterable(category, iterable);
  }

  @Override
  public AREXMocker queryRecord(Mocker requestType) {
    MockCategoryType categoryType = requestType.getCategoryType();
    Query query = new Query(buildRecordFilters(categoryType, requestType))
        .with(CREATE_TIME_DESCENDING_SORT)
        .limit(DEFAULT_MIN_LIMIT_SIZE);

    AREXMocker item = mongoTemplate.findOne(query, AREXMocker.class, getCollectionName(categoryType));
    addUseMocker(item);
    return AttachmentCategoryIterable.attach(categoryType, item);
  }

  @Override
  public AREXMocker queryById(MockCategoryType categoryType, String id) {
    String collection = getCollectionName(categoryType);
    Query query = new Query(Criteria.where(PRIMARY_KEY_COLUMN_NAME).is(id));
    return mongoTemplate.findOne(query, AREXMocker.class, collection);
  }

  @Override
  public Iterable<AREXMocker> queryEntryPointByRange(PagedRequestType pagedRequestType) {
    MockCategoryType categoryType = pagedRequestType.getCategory();
    String collection = getCollectionName(categoryType);

    Integer pageIndex = pagedRequestType.getPageIndex();

    AREXMocker item = getLastRecordVersionMocker(pagedRequestType, collection);
    String recordVersion = item == null ? null : item.getRecordVersion();

    Criteria criteria = withRecordVersionFilters(pagedRequestType, recordVersion);
    if (Objects.equals(this.providerName, ProviderNames.DEFAULT)) {
      updateExpirationTime(criteria, collection);
    }

    Query query = new Query(criteria)
        .with(toSupportSortingOptions(pagedRequestType.getSortingOptions()))
        .skip(pageIndex == null ? 0 : pagedRequestType.getPageSize() * (pageIndex - 1))
        .limit(Math.min(pagedRequestType.getPageSize(), DEFAULT_MAX_LIMIT_SIZE));

    // By default, targetResponse is not output. When includeExtendFields is included, it is output.
    query.fields().include(DEFAULT_INCLUDE_FIELDS);
    if (ArrayUtils.isNotEmpty(pagedRequestType.getIncludeExtendFields())) {
        query.fields().include(pagedRequestType.getIncludeExtendFields());
    }

    Iterable<AREXMocker> iterable = mongoTemplate.find(query, AREXMocker.class, collection);
    return new AttachmentCategoryIterable(categoryType, iterable);
  }

  private Sort toSupportSortingOptions(List<SortingOption> sortingOptions) {
    if (CollectionUtils.isEmpty(sortingOptions)) {
      return CREATE_TIME_ASCENDING_SORT;
    }
    List<Order> orders = new ArrayList<>(sortingOptions.size());
    for (SortingOption sortingOption : sortingOptions) {
      if (SortingTypeEnum.ASCENDING.getCode() == sortingOption.getSortingType()) {
        orders.add(Order.asc(sortingOption.getLabel()));
      } else {
        orders.add(Order.desc(sortingOption.getLabel()));
      }
    }
    return Sort.by(orders);
  }

  private void updateExpirationTime(Criteria criteria, String collectionName) {
    long currentTimeMillis = System.currentTimeMillis();
    long allowedLastMills = TimeUtils.getTodayFirstMills() +
        properties.getAllowReRunDays() * TimeUtils.ONE_DAY;

    Criteria finalCriteria = new Criteria().andOperator(
        criteria,
        new Criteria().orOperator(
            Criteria.where(EXPIRATION_TIME_COLUMN_NAME).lt(new Date(allowedLastMills)),
            Criteria.where(EXPIRATION_TIME_COLUMN_NAME).exists(false)
        )
    );

    // Add different minutes to avoid the same expiration time
    Update update = new Update();
    update.set(EXPIRATION_TIME_COLUMN_NAME,
        new Date(allowedLastMills + currentTimeMillis % TimeUtils.ONE_HOUR));
    update.set(UPDATE_TIME_COLUMN_NAME, new Date(currentTimeMillis));
    mongoTemplate.updateMulti(new Query(finalCriteria), update, collectionName);
  }


  private AREXMocker getLastRecordVersionMocker(PagedRequestType pagedRequestType,
      String collectionName) {
    Query query = new Query(buildReadRangeFilters(pagedRequestType))
        .with(CREATE_TIME_DESCENDING_SORT)
        .limit(DEFAULT_MIN_LIMIT_SIZE);
    return mongoTemplate.findOne(query, AREXMocker.class, collectionName);
  }

  @Override
  public long countByRange(PagedRequestType request) {
    String collectionName = getCollectionName(request.getCategory());
    AREXMocker item = getLastRecordVersionMocker(request, collectionName);
    String recordVersion = item == null ? null : item.getRecordVersion();
    return mongoTemplate.count(new Query(withRecordVersionFilters(request, recordVersion)),
        AREXMocker.class, collectionName);
  }

  @Override
  public Map<String, Long> countByOperationName(PagedRequestType rangeRequestType) {
    String collectionName = getCollectionName(rangeRequestType.getCategory());
    AREXMocker item = getLastRecordVersionMocker(rangeRequestType, collectionName);
    String recordVersion = item == null ? null : item.getRecordVersion();

    Criteria filters = withRecordVersionFilters(rangeRequestType, recordVersion);
    Aggregation agg = Aggregation.newAggregation(
        Aggregation.match(filters),
        Aggregation.group(OPERATION_COLUMN_NAME).count().as(MongoCounter.Fields.count)
    );

    Map<String, Long> resultMap = new HashMap<>();
    mongoTemplate.aggregate(agg, collectionName, MongoCounter.class).forEach(doc -> {
      String operationName = doc.getId();
      if (operationName != null) {
        resultMap.put(operationName, doc.getCount());
      }
    });

    return resultMap;
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
      Long expiration;
      if (StringUtils.equalsIgnoreCase(ProviderNames.AUTO_PINNED, this.providerName)) {
        expiration = defaultApplicationConfig.getConfigAsLong(AUTO_PINNED_MOCKER_EXPIRATION_MILLIS,
            FOURTEEN_DAYS_MILLIS);
      } else {
        expiration = properties.getExpirationDurationMap()
            .getOrDefault(category.getName(), properties.getDefaultExpirationDuration());
      }
      String collection = getCollectionName(category);

      long expirationTime = System.currentTimeMillis() + expiration;
      valueList.forEach(item -> {
        item.setExpirationTime(expirationTime);

        if (category.isEntryPoint()) {
          item.setId(item.getRecordId());
          item.setRecordId(null);
        } else {
          item.setId(IdGenerators.STRING_ID_GENERATOR.generate());
        }
      });
      mongoTemplate.insert(valueList, collection);
    } catch (Throwable ex) {
      // rolling mocker save failed remove all entry point data
      if (Objects.equals(this.providerName, ProviderNames.DEFAULT)) {
        String recordId = valueList.get(0).getRecordId();
        for (MockCategoryType categoryType : entryPointTypes) {
          removeBy(categoryType, recordId);
        }
      }
      LOGGER.error("save List error:{} , size:{}", ex.getMessage(), valueList.size(), ex);
      return false;
    }
    return true;
  }

  @Override
  public long removeBy(MockCategoryType categoryType, String recordId) {
    String collectionName = getCollectionName(categoryType);
    return mongoTemplate.remove(new Query(buildRecordIdFilter(categoryType, recordId)),
            AREXMocker.class, collectionName).getDeletedCount();
  }

  @Override
  public long extendExpirationTo(MockCategoryType categoryType, String recordId, Date expireTime) {
    String collectionName = getCollectionName(categoryType);
    Query query = new Query(buildRecordIdFilter(categoryType, recordId));
    Update update = Update.update(EXPIRATION_TIME_COLUMN_NAME, expireTime);
    return mongoTemplate.updateMulti(query, update, AREXMocker.class, collectionName).getModifiedCount();
  }

  @Override
  public long removeByAppId(MockCategoryType categoryType, String appId) {
    String collectionName = getCollectionName(categoryType);
    Query query = new Query(Criteria.where(APP_ID_COLUMN_NAME).is(appId));
    return mongoTemplate.remove(query, AREXMocker.class, collectionName).getDeletedCount();
  }

  @Override
  public long removeByOperationNameAndAppId(MockCategoryType categoryType, String operationName,
      String appId) {
    String collectionName = getCollectionName(categoryType);
    Query query = new Query(Criteria
        .where(OPERATION_COLUMN_NAME).is(operationName == null ? "" : operationName)
        .and(APP_ID_COLUMN_NAME).is(appId));

    return mongoTemplate.remove(query, AREXMocker.class, collectionName).getDeletedCount();
  }

  @Override
  public long removeById(MockCategoryType categoryType, String id) {
    String collectionName = getCollectionName(categoryType);
    Query query = new Query(Criteria.where(PRIMARY_KEY_COLUMN_NAME).is(id));
    return mongoTemplate.remove(query, AREXMocker.class, collectionName).getDeletedCount();
  }

  @Override
  public boolean update(AREXMocker value) {
    try {
      String collection = getCollectionName(value.getCategoryType());
      mongoTemplate.findAndReplace(new Query(Criteria.where(PRIMARY_KEY_COLUMN_NAME).is(value.getId())), value, collection);
      return true;
    } catch (Exception e) {
      LOGGER.error("update record error:{} ", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public String getProviderName() {
    return this.providerName;
  }

  private Criteria buildAppIdWithOperationFilters(String appId, String operationName) {
    Criteria criteria = Criteria.where(APP_ID_COLUMN_NAME).is(appId);
    if (operationName != null) {
      criteria.and(OPERATION_COLUMN_NAME).is(operationName);
    }
    return criteria;
  }

  private Criteria buildRecordIdFilter(MockCategoryType categoryType, String value) {
    if (categoryType.isEntryPoint()) {
      return Criteria.where(PRIMARY_KEY_COLUMN_NAME).is(value);
    }
    return Criteria.where(RECORD_ID_COLUMN_NAME).is(value);
  }

  private Criteria buildRecordFilters(MockCategoryType categoryType, @NotNull Mocker mocker) {
    Criteria criteria = this.buildAppIdWithOperationFilters(mocker.getAppId(), mocker.getOperationName());
    criteria.andOperator(buildRecordIdFilter(categoryType, mocker.getRecordId()));
    criteria.and(ENV_COLUMN_NAME).is(mocker.getRecordEnvironment());
    return criteria;
  }

  private Criteria buildReadRangeFilters(@NotNull PagedRequestType rangeRequestType) {
    Criteria criteria = this.buildAppIdWithOperationFilters(rangeRequestType.getAppId(),
        rangeRequestType.getOperation());
    if (rangeRequestType.getEnv() != null) {
      criteria.and(ENV_COLUMN_NAME).is(rangeRequestType.getEnv());
    }
    if (rangeRequestType.getBeginTime() != null && rangeRequestType.getEndTime() != null) {
      criteria.andOperator(buildTimeRangeFilter(rangeRequestType.getBeginTime(), rangeRequestType.getEndTime()));
    } else if (rangeRequestType.getBeginTime() != null) {
      criteria.and(CREATE_TIME_COLUMN_NAME).gte(new Date(rangeRequestType.getBeginTime()));
    } else if (rangeRequestType.getEndTime() != null) {
      criteria.and(CREATE_TIME_COLUMN_NAME).lt(new Date(rangeRequestType.getEndTime()));
    }

    if (MapUtils.isNotEmpty(rangeRequestType.getTags())) {
      for (Map.Entry<String, String> entry : rangeRequestType.getTags().entrySet()) {
        String tagName = entry.getKey();
        if (StringUtils.isEmpty(tagName)){
          continue;
        }
        criteria.and(TAGS_COLUMN_NAME + DOT_OP + tagName).is(entry.getValue());
      }
    }
    return criteria;
  }

  private Criteria withRecordVersionFilters(@NotNull PagedRequestType rangeRequestType,
      String recordVersion) {
    Criteria criteria = buildReadRangeFilters(rangeRequestType);
    if (StringUtils.isNotEmpty(recordVersion)) {
      criteria.and(AGENT_RECORD_VERSION_COLUMN_NAME).is(recordVersion);
    }
    return criteria;
  }

  private Criteria buildTimeRangeFilter(long beginTime, long endTime) {
    return Criteria.where(CREATE_TIME_COLUMN_NAME).gte(new Date(beginTime)).lt(new Date(endTime));
  }

  private void addUseMocker(AREXMocker item) {
    if (item != null && item.getUseMock() == null && item.getCategoryType() != null
        && !item.getCategoryType().isEntryPoint()) {
      item.setUseMock(true);
    }
  }

  private static final class AttachmentCategoryIterable implements Iterable<AREXMocker>,
      Iterator<AREXMocker> {

    private final MockCategoryType categoryType;
    private final Iterator<AREXMocker> source;

    private AttachmentCategoryIterable(MockCategoryType categoryType, Iterable<AREXMocker> source) {
      this.categoryType = categoryType;
      this.source = source.iterator();
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
  }
}
