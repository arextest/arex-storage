package com.arextest.storage.service;

import com.arextest.common.cache.CacheProvider;
import com.arextest.config.model.vo.QueryConfigOfCategory;
import com.arextest.config.model.vo.QueryConfigOfCategoryRequest;
import com.arextest.config.model.vo.QueryConfigOfCategoryResponse;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.client.HttpWepServiceApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.arextest.diff.utils.JacksonHelperUtil.objectMapper;

/**
 * query config service
 * created by xinyuan_wang on 2023/11/5
 */
@Service
@Slf4j
public class QueryConfigService {
  private static final String CONFIG_PREFIX = "config_";
  @Value("${arex.query.config.url}")
  private String queryConfigOfCategoryUrl;
  @Value("${arex.query.config.cache.expired.seconds:600}")
  private long cacheExpiredSeconds;
  @Resource
  private HttpWepServiceApiClient httpWepServiceApiClient;
  @Resource
  private CacheProvider redisCacheProvider;

  public QueryConfigOfCategory queryConfigOfCategory(Mocker mocker) {
    if (mocker.getCategoryType().isSkipComparison()) {
      return null;
    }
    QueryConfigOfCategory configCache = getConfigCache(mocker.getAppId(), mocker.getOperationName());
    if (configCache != null) {
      return null;
    }

    QueryConfigOfCategoryRequest queryConfigOfCategoryRequest = new QueryConfigOfCategoryRequest();
    queryConfigOfCategoryRequest.setCategoryName(mocker.getCategoryType().getName());
    queryConfigOfCategoryRequest.setAppId(mocker.getAppId());
    queryConfigOfCategoryRequest.setEntryPoint(mocker.getCategoryType().isEntryPoint());
    queryConfigOfCategoryRequest.setOperationName(mocker.getOperationName());
    QueryConfigOfCategoryResponse queryConfigOfCategoryResponse =
            httpWepServiceApiClient.jsonPost(queryConfigOfCategoryUrl, queryConfigOfCategoryRequest, QueryConfigOfCategoryResponse.class);
    if (queryConfigOfCategoryResponse != null && queryConfigOfCategoryResponse.getBody() != null) {
      putConfigCache(mocker.getAppId(), mocker.getOperationName(), queryConfigOfCategoryResponse.getBody());
      return queryConfigOfCategoryResponse.getBody();
    }
    return null;
  }

  public boolean putConfigCache(String appId, String operationName, QueryConfigOfCategory response) {
    try {
      byte[] key = CacheKeyUtils.toUtf8Bytes(CONFIG_PREFIX + appId + operationName);
      byte[] values = CacheKeyUtils.toUtf8Bytes(objectMapper.writeValueAsString(response));
      redisCacheProvider.put(key, cacheExpiredSeconds, values);
      return true;
    } catch (Exception e) {
      LOGGER.error("putConfigCache failed!", e);
      return false;
    }
  }

  public QueryConfigOfCategory getConfigCache(String appId, String operationName) {
    try {
      byte[] key = CacheKeyUtils.toUtf8Bytes(CONFIG_PREFIX + appId + operationName);
      byte[] values = redisCacheProvider.get(key);
      if (values == null) {
        return null;
      }
      return objectMapper.readValue(new String(values), QueryConfigOfCategory.class);
    } catch (Exception e) {
      LOGGER.error("getConfigCache failed!", e);
      return null;
    }
  }

}