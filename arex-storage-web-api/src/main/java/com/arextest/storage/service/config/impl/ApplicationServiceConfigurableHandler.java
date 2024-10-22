package com.arextest.storage.service.config.impl;

import com.arextest.config.model.dto.StatusType;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.model.dto.application.ApplicationServiceConfiguration;
import com.arextest.config.model.dto.application.OperationDescription;
import com.arextest.config.model.dto.application.ServiceDescription;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.storage.service.config.AbstractConfigurableHandler;
import com.arextest.storage.service.config.provider.ApplicationServiceDescriptionProvider;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jmo
 * @since 2022/1/23
 */
@Slf4j
@Component
public final class ApplicationServiceConfigurableHandler
    extends AbstractConfigurableHandler<ApplicationServiceConfiguration> {

  @Resource
  private ApplicationServiceDescriptionProvider applicationServiceProvider;
  @Resource
  private AbstractConfigurableHandler<ApplicationOperationConfiguration> operationConfigurableHandler;

  protected ApplicationServiceConfigurableHandler(
      @Autowired ConfigRepositoryProvider<ApplicationServiceConfiguration> repositoryProvider) {
    super(repositoryProvider);
  }

  public void createOrUpdate(String appId) {
    if (this.repositoryProvider.count(appId) != 0) {
      LOGGER.info("skip create serviceList when exists by appId:{}", appId);
      return;
    }
    List<? extends ServiceDescription> originServiceList = applicationServiceProvider.get(appId);
    if (CollectionUtils.isEmpty(originServiceList)) {
      LOGGER.info("skip empty originServiceList from appId:{}", appId);
      return;
    }
    this.create(originServiceList);
  }

  private void create(List<? extends ServiceDescription> originServiceList) {
    ApplicationServiceConfiguration serviceConfiguration;
    List<? extends OperationDescription> sourceOperationList;
    for (ServiceDescription originService : originServiceList) {
      serviceConfiguration = new ApplicationServiceConfiguration();
      serviceConfiguration.setAppId(originService.getAppId());
      serviceConfiguration.setServiceKey(originService.getServiceKey());
      serviceConfiguration.setServiceName(originService.getServiceName());
      serviceConfiguration.setStatus(StatusType.NORMAL.getMask());
      sourceOperationList = originService.getOperationList();
      if (super.insert(serviceConfiguration) && CollectionUtils.isNotEmpty(sourceOperationList)) {
        this.buildOperationList(serviceConfiguration, sourceOperationList);
        operationConfigurableHandler.insertList(serviceConfiguration.getOperationList());
        LOGGER.info("add {} service's operations size:{}", originService.getServiceName(),
            sourceOperationList.size());
      }
    }
  }

  @Override
  public boolean insert(ApplicationServiceConfiguration configuration) {
    if (StringUtils.isEmpty(configuration.getServiceName())) {
      return false;
    }
    if (StringUtils.isEmpty(configuration.getServiceKey())) {
      return false;
    }
    return super.insert(configuration);
  }

  private void buildOperationList(ApplicationServiceConfiguration service,
      List<? extends OperationDescription> source) {
    List<ApplicationOperationConfiguration> operationList = new ArrayList<>(source.size());
    ApplicationOperationConfiguration operationConfiguration;
    String operationName;
    for (OperationDescription operationDescription : source) {
      operationName = operationDescription.getOperationName();
      operationConfiguration = new ApplicationOperationConfiguration();
      operationConfiguration.setOperationName(operationName);
      operationConfiguration.setOperationType(operationDescription.getOperationType());
      operationConfiguration.setOperationTypes(operationDescription.getOperationTypes());
      operationConfiguration.setServiceId(service.getId());
      operationConfiguration.setAppId(service.getAppId());
      operationConfiguration.setStatus(StatusType.NORMAL.getMask());
      operationList.add(operationConfiguration);
    }
    service.setOperationList(operationList);
  }
}
