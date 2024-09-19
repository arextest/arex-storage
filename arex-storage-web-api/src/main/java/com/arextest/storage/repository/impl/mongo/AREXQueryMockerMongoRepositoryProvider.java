package com.arextest.storage.repository.impl.mongo;

import com.arextest.common.config.DefaultApplicationConfig;
import com.arextest.model.mock.AREXQueryMocker;
import com.arextest.model.mock.AREXQueryMocker.Fields;
import com.arextest.model.mock.AbstractMocker;
import com.arextest.model.mock.MockCategoryType;
import com.arextest.model.mock.Mocker;
import com.arextest.model.replay.PagedRequestType;
import com.arextest.storage.beans.StorageConfigurationProperties;
import com.arextest.storage.model.Constants;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.repository.RepositoryProvider;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * The rolling provider used by default, which means auto deleted the records after TTL index
 * created on creationTime of collection
 */
@Slf4j
public class AREXQueryMockerMongoRepositoryProvider implements RepositoryProvider<AREXQueryMocker> {

  private static final String COLLECTION_SUFFIX = "Mocker";
  private static final String TARGET_RESPONSE_COLUMN_NAME = "targetResponse";
  private static final String TARGET_REQUEST_COLUMN_NAME = "targetRequest";
  protected final MongoTemplate mongoTemplate;
  private final String providerName;
  private final StorageConfigurationProperties properties;
  private final Set<MockCategoryType> entryPointTypes;
  private final DefaultApplicationConfig defaultApplicationConfig;
  private final String name;
  // fieldMapping for queryRecordList
  private static final Map<String, String> FIELD_MAPPING = new HashMap<>();
  static {
    FIELD_MAPPING.put(Fields.request, TARGET_REQUEST_COLUMN_NAME);
    FIELD_MAPPING.put(Fields.response, TARGET_RESPONSE_COLUMN_NAME);
  }

  public AREXQueryMockerMongoRepositoryProvider(MongoTemplate mongoTemplate,
      StorageConfigurationProperties properties,
      Set<MockCategoryType> entryPointTypes,
      DefaultApplicationConfig defaultApplicationConfig) {
    this(ProviderNames.DEFAULT, mongoTemplate, properties, entryPointTypes, defaultApplicationConfig);
  }

  public AREXQueryMockerMongoRepositoryProvider(String providerName,
      MongoTemplate mongoTemplate,
      StorageConfigurationProperties properties,
      Set<MockCategoryType> entryPointTypes,
      DefaultApplicationConfig defaultApplicationConfig) {
    this.properties = properties;
    this.mongoTemplate = mongoTemplate;
    this.providerName = providerName;
    this.entryPointTypes = entryPointTypes;
    this.defaultApplicationConfig = defaultApplicationConfig;
    this.name = Constants.CLAZZ_NAME_AREX_QUERY_MOCKER;
  }

  private String getCollectionName(MockCategoryType category) {
    return this.getProviderName() + category.getName() + COLLECTION_SUFFIX;
  }

  @Override
  public Iterable<AREXQueryMocker> queryRecordList(MockCategoryType categoryType, String recordId) {
    return queryRecordList(categoryType, recordId, null);
  }

  @Override
  public Iterable<AREXQueryMocker> queryRecordList(MockCategoryType category, String recordId, String[] fieldNames) {
    Criteria criteria = buildRecordIdFilter(category, recordId);

    Query query = new Query(criteria);
    if (ArrayUtils.isNotEmpty(fieldNames)) {
      String[] mappedFieldNames = Arrays.stream(fieldNames)
          .map(fieldName -> FIELD_MAPPING.getOrDefault(fieldName, fieldName))
          .toArray(String[]::new);
      query.fields().include(mappedFieldNames);
    }

    Iterable<AREXQueryMocker> iterable = mongoTemplate.find(query,
        AREXQueryMocker.class, getCollectionName(category));
    iterable.forEach(this::addUseMocker);
    return new AttachmentCategoryIterable(category, iterable);
  }

  private Criteria buildRecordIdFilter(MockCategoryType categoryType, String value) {
    if (categoryType.isEntryPoint()) {
      return Criteria.where(AbstractMocker.Fields.id).is(value);
    }
    return Criteria.where(AbstractMocker.Fields.recordId).is(value);
  }

  private void addUseMocker(AREXQueryMocker item) {
    if (item != null && item.getUseMock() == null && item.getCategoryType() != null
        && !item.getCategoryType().isEntryPoint()) {
      item.setUseMock(true);
    }
  }

  @Override
  public AREXQueryMocker queryRecord(Mocker requestType) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public AREXQueryMocker queryById(MockCategoryType categoryType, String id) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Iterable<AREXQueryMocker> queryEntryPointByRange(PagedRequestType pagedRequestType) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long countByRange(PagedRequestType request) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Map<String, Long> countByOperationName(PagedRequestType rangeRequestType) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public boolean save(AREXQueryMocker value) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public boolean saveList(List<AREXQueryMocker> valueList) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long removeBy(MockCategoryType categoryType, String recordId) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long extendExpirationTo(MockCategoryType categoryType, String recordId, Date expireTime) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public boolean update(AREXQueryMocker value) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long removeByAppId(MockCategoryType categoryType, String appId) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long removeByOperationNameAndAppId(MockCategoryType categoryType, String operationName, String appId) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public long removeById(MockCategoryType categoryType, String id) {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public String getProviderName() {
    return this.providerName;
  }

  @Override
  public String getMockerType() {
    return this.name;
  }


  private static final class AttachmentCategoryIterable implements Iterable<AREXQueryMocker>,
      Iterator<AREXQueryMocker> {

    private final MockCategoryType categoryType;
    private final Iterator<AREXQueryMocker> source;

    private AttachmentCategoryIterable(MockCategoryType categoryType, Iterable<AREXQueryMocker> source) {
      this.categoryType = categoryType;
      this.source = source.iterator();
    }

    private static AREXQueryMocker attach(MockCategoryType categoryType, AREXQueryMocker item) {
      if (item != null) {
        item.setCategoryType(categoryType);
        if (categoryType.isEntryPoint()) {
          item.setRecordId(item.getId());
        }
      }
      return item;
    }

    @Override
    public Iterator<AREXQueryMocker> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      return source.hasNext();
    }

    @Override
    public AREXQueryMocker next() {
      return attach(categoryType, source.next());
    }
  }
}
