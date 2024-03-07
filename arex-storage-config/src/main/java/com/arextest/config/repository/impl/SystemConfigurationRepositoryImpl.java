package com.arextest.config.repository.impl;

import com.arextest.config.mapper.SystemConfigurationMapper;
import com.arextest.config.model.dao.config.SystemConfigurationCollection;
import com.arextest.config.model.dto.system.SystemConfiguration;
import com.arextest.config.repository.SystemConfigurationRepository;
import com.arextest.config.utils.MongoHelper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import org.bson.conversions.Bson;

/**
 * @author wildeslam.
 * @create 2024/2/21 19:58
 */
public class SystemConfigurationRepositoryImpl implements SystemConfigurationRepository {

  MongoCollection<SystemConfigurationCollection> mongoCollection;
  private MongoDatabase mongoDatabase;

  public SystemConfigurationRepositoryImpl(MongoDatabase mongoDatabase) {
    this.mongoDatabase = mongoDatabase;
  }

  @PostConstruct
  public void init() {
    this.mongoCollection = mongoDatabase.getCollection(SystemConfigurationCollection.DOCUMENT_NAME,
        SystemConfigurationCollection.class);
  }

  @Override
  public boolean saveConfig(SystemConfiguration systemConfig) {
    Bson filter = Filters.eq(SystemConfigurationCollection.Fields.key, systemConfig.getKey());
    List<Bson> updateList = Arrays.asList(MongoHelper.getUpdate(),
        MongoHelper.getFullProperties(systemConfig));
    Bson updateCombine = Updates.combine(updateList);
    return mongoCollection.updateOne(filter, updateCombine, new UpdateOptions().upsert(true))
        .getModifiedCount() > 0;
  }

  @Override
  public List<SystemConfiguration> getAllSystemConfigList() {
    List<SystemConfiguration> systemConfigurations = new ArrayList<>();
    try (MongoCursor<SystemConfigurationCollection> cursor = mongoCollection.find().iterator()) {
      while (cursor.hasNext()) {
        SystemConfigurationCollection document = cursor.next();
        SystemConfiguration dto = SystemConfigurationMapper.INSTANCE.dtoFromDao(document);
        systemConfigurations.add(dto);
      }
    }
    return systemConfigurations;
  }

  @Override
  public SystemConfiguration getSystemConfigByKey(String key) {
    Bson filter = Filters.eq(SystemConfigurationCollection.Fields.key, key);
    SystemConfigurationCollection collection =  mongoCollection.find(filter).first();
    return collection == null ? null : SystemConfigurationMapper.INSTANCE.dtoFromDao(collection);
  }

  @Override
  public boolean deleteConfig(String key) {
    Bson filter = Filters.eq(SystemConfigurationCollection.Fields.key, key);
    return mongoCollection.deleteOne(filter).getDeletedCount() > 0;
  }
}
