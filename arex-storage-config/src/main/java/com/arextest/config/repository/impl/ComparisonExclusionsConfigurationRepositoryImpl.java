package com.arextest.config.repository.impl;

import com.arextest.config.mapper.ConfigComparisonExclusionsMapper;
import com.arextest.config.model.dao.config.AbstractComparisonDetails;
import com.arextest.config.model.dao.config.ConfigComparisonExclusionsCollection;
import com.arextest.config.model.dto.ComparisonExclusionsConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author wildeslam.
 * @create 2024/5/29 16:29
 */

public class ComparisonExclusionsConfigurationRepositoryImpl implements
    ConfigRepositoryProvider<ComparisonExclusionsConfiguration> {


  private final MongoTemplate mongoTemplate;

  public ComparisonExclusionsConfigurationRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public List<ComparisonExclusionsConfiguration> list() {
    return null;
  }

  @Override
  public List<ComparisonExclusionsConfiguration> listBy(String appId) {
    Query query = new Query();
    query.addCriteria(Criteria.where(AbstractComparisonDetails.Fields.appId).is(appId));
    List<ConfigComparisonExclusionsCollection> configCollections =
        mongoTemplate.find(query, ConfigComparisonExclusionsCollection.class);
    return configCollections.stream()
        .map(ConfigComparisonExclusionsMapper.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ComparisonExclusionsConfiguration configuration) {
    return false;
  }

  @Override
  public boolean remove(ComparisonExclusionsConfiguration configuration) {
    return false;
  }

  @Override
  public boolean insert(ComparisonExclusionsConfiguration configuration) {
    return false;
  }

  public List<ComparisonExclusionsConfiguration> listBy(String appId, int compareConfigType) {
    Query query = new Query();
    query.addCriteria(Criteria.where(AbstractComparisonDetails.Fields.appId).is(appId)
        .and(AbstractComparisonDetails.Fields.compareConfigType).is(compareConfigType));
    List<ConfigComparisonExclusionsCollection> configCollections =
        mongoTemplate.find(query, ConfigComparisonExclusionsCollection.class);
    return configCollections.stream()
        .map(ConfigComparisonExclusionsMapper.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }
}
