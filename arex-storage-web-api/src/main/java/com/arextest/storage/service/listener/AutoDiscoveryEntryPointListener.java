package com.arextest.storage.service.listener;

import static com.arextest.storage.cache.CacheKeyUtils.DASH;
import static com.arextest.storage.cache.CacheKeyUtils.SERVICE_MAPPINGS_PREFIX;
import com.arextest.common.cache.CacheProvider;
import com.arextest.config.model.dto.StatusType;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.model.dto.application.ApplicationServiceConfiguration;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationServiceConfigurationRepositoryImpl;
import com.arextest.model.mock.Mocker;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.mock.MockResultContext;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class AutoDiscoveryEntryPointListener implements AgentWorkingListener {
  private static final byte[] EMPTY_BYTE_ARRAY = CacheKeyUtils.toUtf8Bytes(StringUtils.EMPTY);
  private final ApplicationServiceConfigurationRepositoryImpl serviceRepository;
  private final ApplicationOperationConfigurationRepositoryImpl serviceOperationRepository;
  private final CacheProvider cacheProvider;

  public AutoDiscoveryEntryPointListener(
      ApplicationServiceConfigurationRepositoryImpl serviceRepository,
      ApplicationOperationConfigurationRepositoryImpl serviceOperationRepository,
      CacheProvider cacheProvider) {
    this.serviceRepository = serviceRepository;
    this.serviceOperationRepository = serviceOperationRepository;
    this.cacheProvider = cacheProvider;
  }

  private <T extends Mocker> void register(@NotNull T item) {
    String appId = item.getAppId();
    byte[] appServiceKey = CacheKeyUtils.toUtf8Bytes(SERVICE_MAPPINGS_PREFIX + appId);
    if (cacheProvider.get(appServiceKey) == null) {
      List<ApplicationServiceConfiguration> serviceEntities = serviceRepository.listBy(appId);
      if (CollectionUtils.isEmpty(serviceEntities)) {
        LOGGER.info("AppId:{} does not have a valid service", appId);
        return;
      } else {
        cacheProvider.put(appServiceKey, CacheKeyUtils.toUtf8Bytes(serviceEntities.get(0).getId()));
      }
    }
    String operationName = item.getOperationName();
    String serviceId = CacheKeyUtils.fromUtf8Bytes(cacheProvider.get(appServiceKey));
    byte[] operationKey = CacheKeyUtils.toUtf8Bytes(SERVICE_MAPPINGS_PREFIX + serviceId + DASH
        + item.getOperationName() + DASH + item.getCategoryType().getName());
    if (cacheProvider.get(operationKey) != null) {
      return;
    }

    ApplicationOperationConfiguration operationEntity = new ApplicationOperationConfiguration();
    operationEntity.setAppId(appId);
    operationEntity.setOperationName(operationName);
    operationEntity.setOperationType(item.getCategoryType().getName());
    operationEntity.setOperationTypes(Collections.singleton(item.getCategoryType().getName()));
    operationEntity.setServiceId(serviceId);
    operationEntity.setStatus(StatusType.NORMAL.getMask());
    if (serviceOperationRepository.findAndUpdate(operationEntity)) {
      cacheProvider.put(operationKey, EMPTY_BYTE_ARRAY);
    }
  }

  @Override
  public boolean onRecordSaving(Mocker instance) {
    if (instance.getCategoryType().isEntryPoint()) {
      this.register(instance);
    }
    return false;
  }

  @Override
  public boolean onRecordMocking(Mocker instance, MockResultContext context) {
    return false;
  }
}
