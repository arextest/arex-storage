package com.arextest.storage.service.config.impl;

import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.storage.service.config.AbstractConfigurableHandler;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2022/1/22
 */
@Slf4j
@Component
public final class ServiceCollectConfigurableHandler extends
    AbstractConfigurableHandler<ServiceCollectConfiguration> {

  @Resource
  private ServiceCollectConfiguration globalDefaultConfiguration;

  private ServiceCollectConfigurableHandler(
      @Autowired ConfigRepositoryProvider<ServiceCollectConfiguration> repositoryProvider) {
    super(repositoryProvider);
  }

  @Override
  public List<ServiceCollectConfiguration> createFromGlobalDefault(String appId) {
    ServiceCollectConfiguration serviceCollectConfiguration = new ServiceCollectConfiguration();
    serviceCollectConfiguration.setAppId(appId);
    serviceCollectConfiguration.setSampleRate(globalDefaultConfiguration.getSampleRate());
    serviceCollectConfiguration.setAllowDayOfWeeks(globalDefaultConfiguration.getAllowDayOfWeeks());
    serviceCollectConfiguration.setTimeMock(globalDefaultConfiguration.isTimeMock());
    serviceCollectConfiguration.setAllowTimeOfDayFrom(
        globalDefaultConfiguration.getAllowTimeOfDayFrom());
    serviceCollectConfiguration.setAllowTimeOfDayTo(
        globalDefaultConfiguration.getAllowTimeOfDayTo());
    serviceCollectConfiguration.setRecordMachineCountLimit(
        globalDefaultConfiguration.getRecordMachineCountLimit() == null ? 1
            : globalDefaultConfiguration.getRecordMachineCountLimit());
    update(serviceCollectConfiguration);
    return Collections.singletonList(serviceCollectConfiguration);
  }

  @Override
  public boolean update(ServiceCollectConfiguration configuration) {
    return super.update(configuration) || super.insert(configuration);
  }

  @Override
  protected void mergeGlobalDefaultSettings(ServiceCollectConfiguration source) {
  }

  @Override
  protected boolean shouldMergeGlobalDefault() {
    return true;
  }

  /**
   * Allocate service collect config for instances.
   * @param appId appid
   * @param instances all instances of the app
   * @return map of instances and their service collect config
   */
  public Pair<ServiceCollectConfiguration, List<InstancesConfiguration>> allocateServiceCollectConfig(
      String appId, List<InstancesConfiguration> instances, InstancesConfiguration requestInstance) {

    ServiceCollectConfiguration rootConfig = useResult(appId);

    List<ServiceCollectConfiguration> multiEnvConfigs = Optional.ofNullable(
        rootConfig.getMultiEnvConfigs()).orElse(Collections.emptyList());
    // no multi env config, all instance use root config
    if (CollectionUtils.isEmpty(multiEnvConfigs)) {
      return Pair.of(rootConfig, instances);
    }

    // index of config -> instances that will use the config
    Map<Integer, List<InstancesConfiguration>> configAllocation = instances.stream()
        .collect(Collectors.groupingBy(instance -> findConfigIndex(instance, multiEnvConfigs)));

    // selected config index of the incoming request instance
    Integer requestInstanceConfigIndex = findConfigIndex(requestInstance, multiEnvConfigs);
    // meaning using multi env config
    if (requestInstanceConfigIndex != -1) {
      ServiceCollectConfiguration envConfig = multiEnvConfigs.get(requestInstanceConfigIndex);
      rootConfig.setSampleRate(envConfig.getSampleRate());
      rootConfig.setAllowDayOfWeeks(envConfig.getAllowDayOfWeeks());
      rootConfig.setAllowTimeOfDayFrom(envConfig.getAllowTimeOfDayFrom());
      rootConfig.setAllowTimeOfDayTo(envConfig.getAllowTimeOfDayTo());
      rootConfig.setRecordMachineCountLimit(envConfig.getRecordMachineCountLimit());
    }

    List<InstancesConfiguration> instancesOfEnv = configAllocation.get(requestInstanceConfigIndex);

    // clear multi env config to avoid confusion
    rootConfig.setMultiEnvConfigs(Collections.emptyList());
    return Pair.of(rootConfig, instancesOfEnv);
  }

  /**
   * Find the index of the config that matches the tags of the instance.
   * @return index of the config, -1 if not found
   */
  private Integer findConfigIndex(InstancesConfiguration instance,
      List<ServiceCollectConfiguration> multiEnvConfigs) {
    for (int i = 0; i < multiEnvConfigs.size(); i++) {
      ServiceCollectConfiguration envConfig = multiEnvConfigs.get(i);
      Map<String, List<String>> configEnv = envConfig.getEnvTags();
      if (configEnv == null || configEnv.isEmpty()) {
        // this should not happen, data is validated before saving
        LOGGER.error("Invalid multi env config, appid: {}", instance.getAppId());
        continue;
      }

      Map<String, String> serverTags = Optional.ofNullable(instance.getTags()).orElse(Collections.emptyMap());
      if (configEnv.keySet().stream().allMatch(tagKey -> {
        List<String> tagVals = configEnv.get(tagKey);
        return tagVals.contains(serverTags.get(tagKey));
      })) {
        return i;
      }
    }
    return -1;
  }


  private <T> Set<T> mergeValues(Set<T> source, Set<T> globalValues) {
    if (CollectionUtils.isEmpty(globalValues)) {
      return source;
    }
    if (CollectionUtils.isEmpty(source)) {
      return globalValues;
    }
    source.addAll(globalValues);
    return source;
  }

  public void updateServiceCollectTime(String appId) {
    ServiceCollectConfiguration serviceCollectConfiguration = this.useResult(appId);
    this.update(serviceCollectConfiguration);
  }

  @Configuration
  @ConfigurationProperties(prefix = "arex.config.default.service.collect")
  static class GlobalServiceCollectConfiguration extends ServiceCollectConfiguration {

  }
}
