package com.arextest.storage.service;

import static com.arextest.diff.utils.JacksonHelperUtil.objectMapper;
import com.arextest.common.cache.CacheProvider;
import com.arextest.config.model.dao.config.SystemConfigurationCollection.KeySummary;
import com.arextest.config.model.dto.ComparisonExclusionsConfiguration;
import com.arextest.config.model.dto.application.AppContract;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.model.dto.system.SystemConfiguration;
import com.arextest.config.model.vo.CompareConfiguration;
import com.arextest.config.model.vo.ConfigComparisonExclusion;
import com.arextest.config.model.vo.QueryConfigOfCategoryRequest;
import com.arextest.config.model.vo.QueryConfigOfCategoryResponse;
import com.arextest.config.model.vo.QueryConfigOfCategoryResponse.QueryConfigOfCategory;
import com.arextest.config.repository.SystemConfigurationRepository;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ComparisonExclusionsConfigurationRepositoryImpl;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.client.HttpWebServiceApiClient;
import com.arextest.storage.repository.AppContractRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  private static final int SOFT_TIME_EXPIRED = 1;

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

  @Resource
  private ComparisonExclusionsConfigurationRepositoryImpl comparisonExclusionsConfigurationRepository;

  @Resource
  private ApplicationOperationConfigurationRepositoryImpl applicationOperationConfigurationRepository;

  @Resource
  private SystemConfigurationRepository systemConfigurationRepository;

  @Resource
  private AppContractRepository appContractRepository;

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

  public CompareConfiguration queryCompareConfiguration(String appId) {
    CompareConfiguration compareConfiguration = new CompareConfiguration();

    List<ConfigComparisonExclusion> comparisonExclusions = queryComparisonExclusions(appId);
    for (Iterator<ConfigComparisonExclusion> iterator = comparisonExclusions.iterator();
        iterator.hasNext(); ) {
      ConfigComparisonExclusion exclusion = iterator.next();
      if (exclusion.getCategoryType() == null || exclusion.getOperationName() == null) {
        compareConfiguration.setGlobalExclusionList(exclusion.getExclusionList());
        iterator.remove();
        break;
      }
    }

    compareConfiguration.setComparisonExclusions(comparisonExclusions);
    compareConfiguration.setIgnoreNodeSet(getIgnoreNodeSet());

    return compareConfiguration;
  }

  private List<ConfigComparisonExclusion> queryComparisonExclusions(String appId) {
    // ConfigComparisonExclusion, Source data
    List<ComparisonExclusionsConfiguration> configs = comparisonExclusionsConfigurationRepository.listBy(
        appId, COMPARE_CONFIG_TYPE).stream().filter(config -> !removeDetailsExpired(config))
        .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(configs)) {
      return getComparisonExclusions(configs, appId);
    }
    return Collections.emptyList();
  }

  private boolean removeDetailsExpired(ComparisonExclusionsConfiguration config) {
    return config.getExpirationType() == 1
        && config.getExpirationDate().getTime() < System.currentTimeMillis();
  }

  private Set<String> getIgnoreNodeSet() {
    SystemConfiguration systemConfiguration = systemConfigurationRepository.getSystemConfigByKey(
        KeySummary.IGNORE_NODE_SET);
    return systemConfiguration == null ? Collections.emptySet()
        : systemConfiguration.getIgnoreNodeSet();
  }

  private List<ConfigComparisonExclusion> getComparisonExclusions(
      List<ComparisonExclusionsConfiguration> configs, String appId) {

    List<ComparisonExclusionsConfiguration> interfaceConfigs = new ArrayList<>();
    List<ComparisonExclusionsConfiguration> dependencyConfigs = new ArrayList<>();
    List<ComparisonExclusionsConfiguration> globalConfigs = new ArrayList<>();

    for (ComparisonExclusionsConfiguration config : configs) {
      if (config.getDependencyId() != null) {
        dependencyConfigs.add(config);
      } else if (config.getOperationId() != null) {
        interfaceConfigs.add(config);
      } else {
        globalConfigs.add(config);
      }
    }

    if (CollectionUtils.isNotEmpty(dependencyConfigs)) {
      fillDependencyInfo(dependencyConfigs, appId);
    }

    if (CollectionUtils.isNotEmpty(interfaceConfigs)) {
      interfaceConfigs = fillInterfaceInfo(interfaceConfigs, appId);
    }

    return mergeExclusionConfig(globalConfigs, interfaceConfigs, dependencyConfigs);
  }

  private List<ConfigComparisonExclusion> mergeExclusionConfig(
      List<ComparisonExclusionsConfiguration> globalConfigs,
      List<ComparisonExclusionsConfiguration> interfaceConfigs,
      List<ComparisonExclusionsConfiguration> dependencyConfigs) {
    List<ComparisonExclusionsConfiguration> allConfigs = Stream.of(
            globalConfigs, interfaceConfigs, dependencyConfigs)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    Map<String, List<ComparisonExclusionsConfiguration>> groupByOperationNameAndType = allConfigs
        .stream()
        .collect(
            Collectors.groupingBy(config -> config.getOperationName() + config.getOperationType()));
    List<ConfigComparisonExclusion> result = new ArrayList<>();
    for (List<ComparisonExclusionsConfiguration> configList : groupByOperationNameAndType
        .values()) {
      if (CollectionUtils.isEmpty(configList)) {
        continue;
      }
      ConfigComparisonExclusion configComparisonExclusions = new ConfigComparisonExclusion();
      configComparisonExclusions.setOperationName(configList.get(0).getOperationName());
      configComparisonExclusions.setCategoryType(configList.get(0).getOperationType());
      configComparisonExclusions.setExclusionList(
          configList.stream().map(ComparisonExclusionsConfiguration::getExclusions)
              .collect(Collectors.toSet()));
      result.add(configComparisonExclusions);
    }
    return result;
  }


  private void fillDependencyInfo(List<ComparisonExclusionsConfiguration> dependencyConfigs,
      String appId) {
    Map<String, AppContract> contractMap = new HashMap<>();
    List<AppContract> contracts = appContractRepository.queryAppContracts(appId);
    if (CollectionUtils.isNotEmpty(contracts)) {
      contractMap.putAll(
          contracts.stream().collect(Collectors.toMap(AppContract::getId, contract -> contract)));
    }
    for (ComparisonExclusionsConfiguration config : dependencyConfigs) {
      AppContract contract = contractMap.get(config.getDependencyId());
      if (contract != null) {
        config.setOperationType(contract.getOperationType());
        config.setOperationName(contract.getOperationName());
      }
    }
  }

  private List<ComparisonExclusionsConfiguration> fillInterfaceInfo(
      List<ComparisonExclusionsConfiguration> interfaceConfigs, String appId) {
    List<ComparisonExclusionsConfiguration> newConfigs = new ArrayList<>();
    List<ApplicationOperationConfiguration> operationConfigurationList =
        applicationOperationConfigurationRepository.listBy(appId);
    Map<String, ApplicationOperationConfiguration> operationConfigurationMap =
        operationConfigurationList.stream().collect(
            Collectors.toMap(ApplicationOperationConfiguration::getId, operation -> operation));
    for (ComparisonExclusionsConfiguration config : interfaceConfigs) {
      ApplicationOperationConfiguration operationConfiguration = operationConfigurationMap.get(
          config.getOperationId());
      if (operationConfiguration != null) {
        config.setOperationName(operationConfiguration.getOperationName());
        // If the operation type is more than one, each operation type corresponds to the same configuration
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
      }
    }
    return newConfigs;
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
}