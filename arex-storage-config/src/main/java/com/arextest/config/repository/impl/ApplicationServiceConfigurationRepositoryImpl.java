package com.arextest.config.repository.impl;

import com.arextest.config.mapper.ServiceMapper;
import com.arextest.config.model.dao.config.ServiceCollection;
import com.arextest.config.model.dto.application.ApplicationServiceConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.utils.MongoHelper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@RequiredArgsConstructor
public class ApplicationServiceConfigurationRepositoryImpl
    implements ConfigRepositoryProvider<ApplicationServiceConfiguration> {

  private final MongoTemplate mongoTemplate;
  private final ApplicationOperationConfigurationRepositoryImpl applicationOperationConfigurationRepository;

  @Override
  public List<ApplicationServiceConfiguration> list() {
    throw new UnsupportedOperationException("this method is not implemented");
  }

  @Override
  public List<ApplicationServiceConfiguration> listBy(String appId) {
    Query filter = new Query(Criteria.where(ServiceCollection.Fields.appId).is(appId));
    return mongoTemplate.find(filter, ServiceCollection.class)
        .stream()
        .map(source -> {
          ApplicationServiceConfiguration dto = ServiceMapper.INSTANCE.dtoFromDao(source);
          dto.setOperationList(applicationOperationConfigurationRepository.operationBaseInfoList(dto.getId()));
          return dto;
        })
        .collect(Collectors.toList());
  }

  @Override
  public boolean update(ApplicationServiceConfiguration configuration) {
    Query filter = new Query(Criteria.where(DASH_ID).is(new ObjectId(configuration.getId())));

    Update update = new Update();
    update.set(ServiceCollection.Fields.status, configuration.getStatus());
    MongoHelper.withMongoTemplateBaseUpdate(update);
    return mongoTemplate.updateMulti(filter, update, ServiceCollection.class).getModifiedCount() > 0;
  }

  @Override
  public boolean remove(ApplicationServiceConfiguration configuration) {
    Query filter = new Query(Criteria.where(DASH_ID).is(new ObjectId(configuration.getId())));
    return mongoTemplate.remove(filter, ServiceCollection.class).getDeletedCount() > 0;
  }

  @Override
  public boolean insert(ApplicationServiceConfiguration configuration) {
    ServiceCollection serviceCollection = ServiceMapper.INSTANCE.daoFromDto(configuration);
    mongoTemplate.insert(serviceCollection);
    if (serviceCollection.getId() != null) {
      configuration.setId(serviceCollection.getId());
    }
    return serviceCollection.getId() != null;
  }

  @Override
  public long count(String appId) {
    Query filter = new Query(Criteria.where(ServiceCollection.Fields.appId).is(appId));
    return mongoTemplate.count(filter, ServiceCollection.class);
  }

  @Override
  public boolean removeByAppId(String appId) {
    Query filter = new Query(Criteria.where(ServiceCollection.Fields.appId).is(appId));
    return mongoTemplate.remove(filter, ServiceCollection.class).getDeletedCount() > 0;
  }
}
