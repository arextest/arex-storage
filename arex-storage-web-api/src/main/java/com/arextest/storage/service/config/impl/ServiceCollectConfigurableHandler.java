package com.arextest.storage.service.config.impl;

import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.storage.service.config.AbstractConfigurableHandler;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
  protected List<ServiceCollectConfiguration> createFromGlobalDefault(String appId) {
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
   * Query service collect config from db, merge multi env config into root config according to the
   * tags sent from agent. Match the first config having the same tag with agent.
   *
   * @param serverTags server tags sent from agent, e.g. env: fat
   */
  public ServiceCollectConfiguration queryConfigByEnv(String appId,
      Map<String, String> serverTags) {
    ServiceCollectConfiguration config = useResult(appId);
    if (serverTags == null || serverTags.isEmpty() || config == null) {
      return config;
    }

    List<ServiceCollectConfiguration> multiEnvConfigs = Optional.ofNullable(
        config.getMultiEnvConfigs()).orElse(Collections.emptyList());

    multiEnvConfigs.stream().filter(envConfig -> {
      Map<String, List<String>> configEnv = envConfig.getEnvTags();
      if (configEnv == null || configEnv.isEmpty()) {
        return false;
      }

      SetView<String> keyToCheck = Sets.intersection(serverTags.keySet(), configEnv.keySet());
      // config and server tags have no common key, skip
      if (keyToCheck.isEmpty()) {
        return false;
      } else {
        // config and server tags have common key, check if all values are matched
        return keyToCheck.stream().allMatch(key -> {
          List<String> configEnvVals = Optional.ofNullable(configEnv.get(key)).orElse(Collections.emptyList());
          return configEnvVals.contains(serverTags.get(key));
        });
      }
    }).findFirst().ifPresent(matched -> {
      config.setSampleRate(matched.getSampleRate());
      config.setAllowDayOfWeeks(matched.getAllowDayOfWeeks());
      config.setAllowTimeOfDayFrom(matched.getAllowTimeOfDayFrom());
      config.setAllowTimeOfDayTo(matched.getAllowTimeOfDayTo());
      config.setRecordMachineCountLimit(matched.getRecordMachineCountLimit());
    });
    return config;
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
