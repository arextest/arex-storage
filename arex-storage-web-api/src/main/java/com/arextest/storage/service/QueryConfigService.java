package com.arextest.storage.service;

import static com.arextest.diff.utils.JacksonHelperUtil.objectMapper;
import com.arextest.common.cache.CacheProvider;
import com.arextest.config.model.dto.ComparisonExclusionsConfiguration;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.model.vo.ConfigComparisonExclusionsVO;
import com.arextest.config.model.vo.QueryConfigOfCategoryRequest;
import com.arextest.config.model.vo.QueryConfigOfCategoryResponse;
import com.arextest.config.model.vo.QueryConfigOfCategoryResponse.QueryConfigOfCategory;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ComparisonExclusionsConfigurationRepositoryImpl;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.client.HttpWebServiceApiClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * query config service created by xinyuan_wang on 2023/11/5
 */
@Service
@Slf4j
public class QueryConfigService {

  private static final String CONFIG_PREFIX = "config_";
  private static final int COMPARE_CONFIG_TYPE = 0;

  @Value("${arex.query.config.url}")
  private String queryConfigOfCategoryUrl;

  @Value("${arex.query.schedule.url}")
  private String queryScheduleReplayConfigurationUrl;

  @Value("${arex.query.systemConfig.url}")
  private String querySystemConfigUrl;

  @Value("${arex.query.contracts.url}")
  private String queryContractsUrl;

  @Value("${arex.config.cache.expired.seconds:600}")
  private long cacheExpiredSeconds;

  @Resource
  private HttpWebServiceApiClient httpWebServiceApiClient;

  @Resource
  private CacheProvider redisCacheProvider;

  @Resource
  private ComparisonExclusionsConfigurationRepositoryImpl comparisonExclusionsConfigurationRepository;

  @Resource
  private ApplicationOperationConfigurationRepositoryImpl applicationOperationConfigurationRepository;

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

  public ScheduleReplayConfigurationResponse queryScheduleReplayConfiguration(String appId) {
    String url = String.format(queryScheduleReplayConfigurationUrl, appId);
    return httpWebServiceApiClient.get(url, Collections.emptyMap(),
        ScheduleReplayConfigurationResponse.class);
  }

  public List<ConfigComparisonExclusionsVO> queryComparisonExclusions(String appId) {
    List<ComparisonExclusionsConfiguration> configs = comparisonExclusionsConfigurationRepository.listBy(
        appId, COMPARE_CONFIG_TYPE);
    if (CollectionUtils.isNotEmpty(configs)) {
      return getComparisonExclusions(configs, appId);
    }
    return Collections.emptyList();
  }

  public Set<String> getIgnoreNodeSet() {
    SystemConfigResponse systemConfigResponse = httpWebServiceApiClient.get(
        querySystemConfigUrl, Collections.emptyMap(), SystemConfigResponse.class);
    if (systemConfigResponse == null || systemConfigResponse.getBody() == null) {
      return Collections.emptySet();
    }
    return systemConfigResponse.getBody().getIgnoreNodeSet();
  }

  private List<ConfigComparisonExclusionsVO> getComparisonExclusions(
      List<ComparisonExclusionsConfiguration> configs, String appId) {

    List<ApplicationOperationConfiguration> operationConfigurationList =
        applicationOperationConfigurationRepository.listBy(appId);
    Map<String, ApplicationOperationConfiguration> operationConfigurationMap = operationConfigurationList.stream()
        .collect(Collectors.toMap(ApplicationOperationConfiguration::getId, operation -> operation));

    QueryContractRequestType requestType = new QueryContractRequestType();
    requestType.setAppId(appId);
    QueryContractResponse response = httpWebServiceApiClient.jsonPost(queryContractsUrl, requestType, QueryContractResponse.class);
    Map<String, AppContractDto> contractMap = new HashMap<>();
    if (response != null && response.getBody() != null) {
      contractMap.putAll(response.getBody().stream()
          .collect(Collectors.toMap(AppContractDto::getId, contract -> contract)));
    }


    List<ComparisonExclusionsConfiguration> newConfigs = new ArrayList<>();
    for (ComparisonExclusionsConfiguration config : configs) {
      //dependency
      if (config.getDependencyId() != null) {
        AppContractDto contract = contractMap.get(config.getDependencyId());
        if (contract != null) {
          config.setOperationType(contract.getOperationType());
          config.setOperationName(contract.getOperationName());
          newConfigs.add(config);
        }
        continue;
      }

      ApplicationOperationConfiguration operationConfiguration = operationConfigurationMap.get(config.getOperationId());
      if (operationConfiguration != null) {
        config.setOperationName(operationConfiguration.getOperationName());
        if (operationConfiguration.getOperationTypes().size() > 1) {
          for (String operationType : operationConfiguration.getOperationTypes()) {
            ComparisonExclusionsConfiguration newConfig = new ComparisonExclusionsConfiguration();
            newConfig.setOperationName(config.getOperationName());
            newConfig.setOperationType(operationType);
            newConfig.setExclusions(config.getExclusions());
            newConfigs.add(newConfig);
          }
        } else if (operationConfiguration.getOperationTypes().size() == 1) {
          String operationType = operationConfiguration.getOperationTypes().iterator().next();
          config.setOperationType(operationType);
          newConfigs.add(config);
        }
      } else {
        newConfigs.add(config);
      }
    }

    Map<String, List<ComparisonExclusionsConfiguration>> groupByOperationNameAndType = newConfigs
        .stream()
        .collect(Collectors.groupingBy(
            config -> config.getOperationName() + config.getOperationType()));
    List<ConfigComparisonExclusionsVO> result = new ArrayList<>();
    for (Entry<String, List<ComparisonExclusionsConfiguration>> entry : groupByOperationNameAndType
        .entrySet()) {
      List<ComparisonExclusionsConfiguration> configList = entry.getValue();
      if (CollectionUtils.isEmpty(configList)) {
        continue;
      }
      ConfigComparisonExclusionsVO vo = new ConfigComparisonExclusionsVO();
      vo.setOperationName(configList.get(0).getOperationName());
      vo.setCategoryType(configList.get(0).getOperationType());
      vo.setExclusionList(configList.stream().map(ComparisonExclusionsConfiguration::getExclusions)
          .collect(Collectors.toSet()));
      result.add(vo);
    }
    return result;
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
  public static class ScheduleReplayConfigurationResponse {
    ScheduleReplayConfiguration body;
  }

  @Data
  public static class ScheduleReplayConfiguration {

    private String mockHandlerJarUrl;

  }

  @Data
  public static class SystemConfigResponse {

    private SystemConfigWithProperties body;

  }

  @Data
  public static class SystemConfigWithProperties {

    /**
     * according to the names of node to ignore the node.
     */
    private Set<String> ignoreNodeSet;
  }

  @Data
  public static class QueryContractRequestType {

    // query global contract
    private String appId;
    // query entryPointContract
    private String operationId;
    // query dependency
    private String operationType;
    private String operationName;

  }

  @Data
  public static class QueryContractResponse {

    private List<AppContractDto> body;

  }

  @Data
  public static class AppContractDto  {

    private String id;
    private String appId;
    private Integer contractType;
    private String operationId;

    /**
     * the operationName and operationType exist, when contractType is 2
     */
    private String operationName;
    private String operationType;
    private String contract;
  }

}