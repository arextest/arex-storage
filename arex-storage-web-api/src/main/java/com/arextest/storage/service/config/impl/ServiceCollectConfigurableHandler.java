package com.arextest.storage.service.config.impl;

import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.storage.service.config.AbstractConfigurableHandler;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  protected ServiceCollectConfigurableHandler(
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
    serviceCollectConfiguration
        .setRecordMachineCountLimit(
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

  public ServiceCollectConfiguration queryConfigByEnv(String appId,
      Map<String, String> serverTags) {
    ServiceCollectConfiguration config = useResult(appId);
    if (serverTags == null || serverTags.isEmpty()) {
      return config;
    }

    List<ServiceCollectConfiguration> multiEnvConfigs = Optional
        .ofNullable(config.getMultiEnvConfigs())
        .orElse(Collections.emptyList());

   multiEnvConfigs.stream()
        .filter(envConfig -> {
          Map<String, List<String>> configEnv = envConfig.getEnvTags();
          if (configEnv == null || configEnv.isEmpty()) {
            return false;
          }
          for (Entry<String, String> tagPair : serverTags.entrySet()) {
            if (configEnv.get(tagPair.getKey()).contains(tagPair.getValue())) {
              return true;
            }
          }
          return false;
        })
        .findFirst()
        .ifPresent(matched -> {
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
