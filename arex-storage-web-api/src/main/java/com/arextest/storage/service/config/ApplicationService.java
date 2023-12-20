package com.arextest.storage.service.config;

import com.arextest.common.cache.CacheProvider;
import com.arextest.config.model.dto.StatusType;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.model.vo.AddApplicationRequest;
import com.arextest.config.model.vo.AddApplicationResponse;
import com.arextest.config.model.vo.DeleteApplicationRequest;
import com.arextest.config.model.vo.UpdateApplicationRequest;
import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.service.AutoDiscoveryEntryPointListener;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.utils.RandomUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author wildeslam.
 * @create 2023/9/15 14:37
 */
@Slf4j
@Component
public class ApplicationService {

  private static final String APP_OWNERS_PREFIX = "app_owners_";

  private static final long cacheExpiredSeconds = 3600;

  private static final List<String> PROVIDERS = Arrays.asList(ProviderNames.DEFAULT,
      ProviderNames.PINNED, ProviderNames.AUTO_PINNED);

  @Resource
  private ObjectMapper objectMapper;
  @Resource
  private ApplicationConfigurationRepositoryImpl applicationConfigurationRepository;
  @Resource
  private ApplicationOperationConfigurationRepositoryImpl applicationOperationRepository;
  @Resource
  private CacheProvider redisCacheProvider;
  @Resource
  private MockSourceEditionService mockSourceEditionService;
  @Resource
  private AutoDiscoveryEntryPointListener autoDiscoveryEntryPointListener;

  public AddApplicationResponse addApplication(AddApplicationRequest request) {
    AddApplicationResponse response = new AddApplicationResponse();

    ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
    applicationConfiguration.setAppName(request.getAppName());
    applicationConfiguration.setAgentVersion(StringUtils.EMPTY);
    applicationConfiguration.setAgentExtVersion(StringUtils.EMPTY);
    applicationConfiguration.setRecordedCaseCount(0);
    applicationConfiguration.setStatus(StatusType.RECORD.getMask() | StatusType.REPLAY.getMask());
    applicationConfiguration.setOwners(request.getOwners());
    applicationConfiguration.setVisibilityLevel(request.getVisibilityLevel());

    applicationConfiguration.setOrganizationName("unknown organization name");
    applicationConfiguration.setGroupName("unknown group name");
    applicationConfiguration.setGroupId("unknown group id");
    applicationConfiguration.setOrganizationId("unknown organization id");
    applicationConfiguration.setDescription("unknown description");
    applicationConfiguration.setCategory("unknown category");

    String appId = RandomUtils.generateRandomId(request.getAppName());
    applicationConfiguration.setAppId(appId);

    boolean success = applicationConfigurationRepository.insert(applicationConfiguration);
    response.setAppId(appId);
    response.setSuccess(success);
    return response;
  }

  public boolean modifyApplication(UpdateApplicationRequest request) {
    List<ApplicationConfiguration> applicationConfigurationList =
        applicationConfigurationRepository.listBy(request.getAppId());
    if (CollectionUtils.isEmpty(applicationConfigurationList)) {
      return false;
    }
    ApplicationConfiguration applicationConfiguration = applicationConfigurationList.get(0);
    if (request.getAppName() != null) {
      applicationConfiguration.setAppName(request.getAppName());
    }
    if (request.getOwners() != null) {
      applicationConfiguration.setOwners(request.getOwners());
      putAppOwnersCache(request.getAppId(), request.getOwners());
    }
    if (request.getVisibilityLevel() != null) {
      applicationConfiguration.setVisibilityLevel(request.getVisibilityLevel());
    }
    return applicationConfigurationRepository.update(applicationConfiguration);
  }

  public boolean deleteApplication(DeleteApplicationRequest request) {
    final String appId = request.getAppId();
    // remove Mockers
    PROVIDERS.forEach(provider -> mockSourceEditionService.removeAllByAppId(provider, appId));

    // remove redis
    autoDiscoveryEntryPointListener.removeAllCacheByAppId(appId);

    // remove ServiceOperation
    applicationOperationRepository.removeByAppId(appId);

    // remove App
    applicationConfigurationRepository.removeByAppId(request.getAppId());
    return true;
  }


  public boolean putAppOwnersCache(String appId, Set<String> owners) {
    try {
      byte[] appServiceKey = CacheKeyUtils.toUtf8Bytes(APP_OWNERS_PREFIX + appId);
      byte[] values = CacheKeyUtils.toUtf8Bytes(objectMapper.writeValueAsString(owners));
      redisCacheProvider.put(appServiceKey, cacheExpiredSeconds, values);
      return true;
    } catch (Exception e) {
      LOGGER.error("PutAppOwners failed!", e);
      return false;
    }
  }

  public Set<String> getAppOwnersCache(String appId) {
    try {
      byte[] appServiceKey = CacheKeyUtils.toUtf8Bytes(APP_OWNERS_PREFIX + appId);
      byte[] values = redisCacheProvider.get(appServiceKey);
      if (values == null) {
        return null;
      }
      return objectMapper.readValue(new String(values), Set.class);
    } catch (Exception e) {
      LOGGER.error("getAppOwners failed!", e);
      return null;
    }
  }
}
