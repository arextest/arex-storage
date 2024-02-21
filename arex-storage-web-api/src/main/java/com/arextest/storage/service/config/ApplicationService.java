package com.arextest.storage.service.config;

import static com.arextest.storage.cache.CacheKeyUtils.DASH;
import static com.arextest.storage.cache.CacheKeyUtils.SERVICE_MAPPINGS_PREFIX;

import com.arextest.common.cache.CacheProvider;
import com.arextest.config.model.dto.StatusType;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration;
import com.arextest.config.model.dto.application.ApplicationOperationConfiguration.Fields;
import com.arextest.config.model.vo.AddApplicationRequest;
import com.arextest.config.model.vo.AddApplicationResponse;
import com.arextest.config.model.vo.DeleteApplicationRequest;
import com.arextest.config.model.vo.UpdateApplicationRequest;
import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ApplicationOperationConfigurationRepositoryImpl;
import com.arextest.config.repository.impl.ServiceCollectConfigurationRepositoryImpl;
import com.arextest.storage.cache.CacheKeyUtils;
import com.arextest.storage.model.Constants;
import com.arextest.storage.model.event.ApplicationCreationEvent;
import com.arextest.storage.repository.ProviderNames;
import com.arextest.storage.service.MockSourceEditionService;
import com.arextest.storage.service.config.impl.ServiceCollectConfigurableHandler;
import com.arextest.storage.utils.RandomUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.context.ApplicationEventPublisher;
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
  private ApplicationOperationConfigurationRepositoryImpl serviceOperationRepository;

  @Resource
  private ServiceCollectConfigurableHandler serviceCollectConfigurableHandler;
  @Resource
  private ApplicationEventPublisher applicationEventPublisher;
  @Resource
  private ServiceCollectConfigurationRepositoryImpl serviceCollectConfigurationRepository;

  @Resource
  private MongoDatabase mongoDatabase;

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
    serviceCollectConfigurableHandler.createFromGlobalDefault(appId);
    applicationEventPublisher.publishEvent(new ApplicationCreationEvent(appId));
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
    removeAllCacheByAppId(appId);

    // remove ServiceOperation
    applicationOperationRepository.removeByAppId(appId);

    // remove App
    applicationConfigurationRepository.removeByAppId(appId);

    // remove RecordServiceConfig
    serviceCollectConfigurationRepository.removeByAppId(appId);

    // remove ReplayScheduleConfig
    mongoDatabase.getCollection(Constants.REPLAY_SCHEDULE_CONFIG_COLLECTION_NAME)
        .deleteMany(new Document(Constants.APP_ID, appId));

    // remove the config about comparison
    removeComparisonConfig(appId);
    return true;
  }

  private void removeAllCacheByAppId(String appId) {
    byte[] appServiceKey = CacheKeyUtils.toUtf8Bytes(SERVICE_MAPPINGS_PREFIX + appId);
    byte[] appServiceValue = redisCacheProvider.get(appServiceKey);
    if (appServiceValue == null) {
      return;
    }
    String serviceId = CacheKeyUtils.fromUtf8Bytes(appServiceValue);
    redisCacheProvider.remove(appServiceKey);

    Map<String, Object> conditions = new HashMap<>();
    conditions.put(Fields.appId, appId);
    List<ApplicationOperationConfiguration> applicationOperationConfigurations =
        serviceOperationRepository.queryByMultiCondition(conditions);

    applicationOperationConfigurations.forEach(operation -> {
      String operationName = operation.getOperationName();
      String operationType = operation.getOperationType();
      byte[] operationKey = CacheKeyUtils.toUtf8Bytes(SERVICE_MAPPINGS_PREFIX + serviceId + DASH
          + operationName + DASH + operationType);
      redisCacheProvider.remove(operationKey);
    });
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

  private void removeComparisonConfig(String appId) {
    // remove the config about comparison
    List<String> COMPARISON_CONFIG_COLLECTIONS = Arrays.asList(
        Constants.CONFIG_COMPARISON_ENCRYPTION_COLLECTION_NAME,
        Constants.CONFIG_COMPARISON_EXCLUSIONS_COLLECTION_NAME,
        Constants.CONFIG_COMPARISON_IGNORE_CATEGORY_COLLECTION_NAME,
        Constants.CONFIG_COMPARISON_LIST_SORT_COLLECTION_NAME,
        Constants.CONFIG_COMPARISON_REFERENCE_COLLECTION_NAME
    );
    COMPARISON_CONFIG_COLLECTIONS.forEach(collection -> {
      Document document = new Document();
      document.put(Constants.APP_ID, appId);
      document.put("compareConfigType", 0);
      mongoDatabase.getCollection(collection).deleteMany(document);
    });
  }
}
