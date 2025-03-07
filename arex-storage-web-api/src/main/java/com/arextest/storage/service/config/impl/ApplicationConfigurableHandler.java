package com.arextest.storage.service.config.impl;

import com.arextest.config.model.dto.StatusType;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.model.dto.application.ApplicationDescription;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
import com.arextest.storage.model.event.ApplicationCreationEvent;
import com.arextest.storage.service.config.AbstractConfigurableHandler;
import com.arextest.storage.service.config.provider.ApplicationDescriptionProvider;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2022/1/23
 */
@Component
public class ApplicationConfigurableHandler extends AbstractConfigurableHandler<ApplicationConfiguration> {

  @Resource
  private ApplicationDescriptionProvider applicationDescriptionProvider;
  @Resource
  private ApplicationConfigurationRepositoryImpl applicationConfigurationRepository;

  @Resource
  private ApplicationEventPublisher applicationEventPublisher;

  protected ApplicationConfigurableHandler(
      @Autowired ConfigRepositoryProvider<ApplicationConfiguration> repositoryProvider) {
    super(repositoryProvider);
  }

  @Override
  public boolean insert(ApplicationConfiguration configuration) {
    if (configuration == null || StringUtils.isEmpty(configuration.getAppId())) {
      return false;
    }
    ApplicationDescription applicationOrganization = applicationDescriptionProvider.get(
        configuration.getAppId());
    if (applicationOrganization != null) {
      configuration.setAppName(applicationOrganization.getAppName());
      configuration.setDescription(applicationOrganization.getDescription());
      configuration.setGroupId(applicationOrganization.getGroupId());
      configuration.setGroupName(applicationOrganization.getGroupName());
      configuration.setOrganizationId(applicationOrganization.getOrganizationId());
      configuration.setOrganizationName(applicationOrganization.getOrganizationName());
      configuration.setOwner(applicationOrganization.getOwner());
      configuration.setCategory(applicationOrganization.getCategory());
    }
    return super.insert(configuration);
  }

  @Override
  protected List<ApplicationConfiguration> createFromGlobalDefault(String appId) {
    ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
    applicationConfiguration.setAppName(appId);
    applicationConfiguration.setAppId(appId);
    applicationConfiguration.setAgentVersion(StringUtils.EMPTY);
    applicationConfiguration.setAgentExtVersion(StringUtils.EMPTY);
    applicationConfiguration.setRecordedCaseCount(0);
    applicationConfiguration.setStatus(StatusType.RECORD.getMask() | StatusType.REPLAY.getMask());
    this.insert(applicationConfiguration);
    applicationEventPublisher.publishEvent(new ApplicationCreationEvent(appId));
    return Collections.singletonList(applicationConfiguration);
  }

  public boolean addEnvToApp(String appId, Map<String, String> tags) {
    return applicationConfigurationRepository.addEnvToApp(appId, tags);
  }

}
