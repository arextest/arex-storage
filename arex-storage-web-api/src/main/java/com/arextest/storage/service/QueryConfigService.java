package com.arextest.storage.service;

import static com.arextest.diff.utils.JacksonHelperUtil.objectMapper;

import com.arextest.common.cache.CacheProvider;
import com.arextest.config.model.vo.QueryConfigOfCategoryRequest;
import com.arextest.config.model.vo.QueryConfigOfCategoryResponse;
import com.arextest.config.model.vo.QueryConfigOfCategoryResponse.QueryConfigOfCategory;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.client.HttpWebServiceApiClient;
import java.util.Collections;
import javax.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * query config service created by xinyuan_wang on 2023/11/5
 */
@Service
@Slf4j
public class QueryConfigService {

  private static final String CONFIG_PREFIX = "config_";

  @Value("${arex.query.config.url}")
  private String queryConfigOfCategoryUrl;

  @Value("${arex.query.schedule.url}")
  private String queryScheduleReplayConfigurationUrl;

  @Value("${arex.config.cache.expired.seconds:600}")
  private long cacheExpiredSeconds;

  @Resource
  private HttpWebServiceApiClient httpWebServiceApiClient;

  @Resource
  private CacheProvider redisCacheProvider;

  public QueryConfigOfCategory queryConfigOfCategory(Mocker mocker) {
    if (mocker.getCategoryType().isSkipComparison()) {
      return null;
    }
    String categoryName = mocker.getCategoryType().getName();
    String appId = mocker.getAppId();
    String operationName = mocker.getOperationName();

    QueryConfigOfCategory configCache = getConfigCache(appId, categoryName,
        operationName);
    if (configCache != null) {
      return configCache;
    }

    QueryConfigOfCategoryRequest queryConfigOfCategoryRequest = new QueryConfigOfCategoryRequest();
    queryConfigOfCategoryRequest.setCategoryName(categoryName);
    queryConfigOfCategoryRequest.setAppId(appId);
    queryConfigOfCategoryRequest.setEntryPoint(mocker.getCategoryType().isEntryPoint());
    queryConfigOfCategoryRequest.setOperationName(operationName);
    QueryConfigOfCategoryResponse queryConfigOfCategoryResponse =
        httpWebServiceApiClient.jsonPost(queryConfigOfCategoryUrl,
            queryConfigOfCategoryRequest, QueryConfigOfCategoryResponse.class);
    if (queryConfigOfCategoryResponse != null && queryConfigOfCategoryResponse.getBody() != null) {
      putConfigCache(appId, categoryName, operationName, queryConfigOfCategoryResponse.getBody());
      return queryConfigOfCategoryResponse.getBody();
    }
    return null;
  }

  public ScheduleReplayConfiguration queryScheduleReplayConfiguration(String appId) {
    String url = String.format(queryScheduleReplayConfigurationUrl, appId);
    return httpWebServiceApiClient.get(url, Collections.emptyMap(),
        ScheduleReplayConfiguration.class);
  }

  private boolean putConfigCache(String appId, String categoryName, String operationName,
      QueryConfigOfCategory response) {
    try {
      byte[] key = CacheKeyUtils.toUtf8Bytes(CONFIG_PREFIX + appId + categoryName + operationName);
      byte[] values = CacheKeyUtils.toUtf8Bytes(objectMapper.writeValueAsString(response));
      redisCacheProvider.put(key, cacheExpiredSeconds, values);
      return true;
    } catch (Exception e) {
      LOGGER.error("putConfigCache failed!", e);
      return false;
    }
  }

  private QueryConfigOfCategory getConfigCache(String appId, String categoryName,
      String operationName) {
    try {
      byte[] key = CacheKeyUtils.toUtf8Bytes(CONFIG_PREFIX + appId + categoryName + operationName);
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

  @Data
  public static class ScheduleReplayConfiguration {

    private String mockHandlerJarUrl;

  }

}