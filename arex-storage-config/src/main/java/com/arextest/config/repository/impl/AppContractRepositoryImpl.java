package com.arextest.config.repository.impl;

import com.arextest.config.mapper.AppContractMapper;
import com.arextest.config.model.dao.config.AppContractCollection;
import com.arextest.config.model.dto.application.AppContract;
import com.arextest.config.repository.AppContractRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * @author wildeslam.
 * @create 2024/7/4 10:56
 */
@RequiredArgsConstructor
@Repository
public class AppContractRepositoryImpl implements AppContractRepository {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<AppContract> queryAppContracts(String appId) {
    Query query = Query.query(Criteria.where(AppContractCollection.Fields.appId).is(appId));
    List<AppContractCollection> daos = mongoTemplate.find(query, AppContractCollection.class);
    return daos.stream().map(AppContractMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
  }
}
