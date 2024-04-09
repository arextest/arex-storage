package com.arextest.config.repository.impl;

import com.arextest.config.mapper.SystemConfigurationMapper;
import com.arextest.config.model.dao.config.RecordServiceConfigCollection;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dto.system.SystemConfiguration;
import com.arextest.config.repository.SystemConfigurationRepository;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.conversions.Bson;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author wildeslam.
 * @create 2024/2/21 19:58
 */
@RequiredArgsConstructor
public class SystemConfigurationRepositoryImpl implements SystemConfigurationRepository {
  private final MongoTemplate mongoTemplate;

  @Override
  public boolean saveConfig(SystemConfiguration systemConfig) {
    Query filter = new Query(Criteria.where(SystemConfigurationCollection.Fields.key)
        .is(systemConfig.getKey()));
    Update update = MongoHelper.getFullTemplateUpdates(systemConfig);
    MongoHelper.withMongoTemplateBaseUpdate(update);
    return mongoTemplate.upsert(filter, update, SystemConfigurationCollection.class)
        .getModifiedCount() > 0;
  }

  @Override
  public List<SystemConfiguration> getAllSystemConfigList() {
    return mongoTemplate.findAll(SystemConfigurationCollection.class).stream()
        .map(SystemConfigurationMapper.INSTANCE::dtoFromDao)
        .collect(Collectors.toList());
  }

  @Override
  public SystemConfiguration getSystemConfigByKey(String key) {
    Query filter = new Query(Criteria.where(SystemConfigurationCollection.Fields.key).is(key));
    SystemConfigurationCollection collection =  mongoTemplate.findOne(filter, SystemConfigurationCollection.class);
    return collection == null ? null : SystemConfigurationMapper.INSTANCE.dtoFromDao(collection);
  }

  @Override
  public boolean deleteConfig(String key) {
    Query filter = new Query(Criteria.where(SystemConfigurationCollection.Fields.key).is(key));
    return mongoTemplate.remove(filter, SystemConfigurationCollection.class).getDeletedCount() > 0;
  }
}
