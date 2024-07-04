package com.arextest.storage.web.controller.config;

import com.arextest.common.model.response.Response;
import com.arextest.common.model.response.ResponseCode;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.config.mapper.InstancesMapper;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.model.dto.record.DynamicClassConfiguration;
import com.arextest.config.model.dto.record.ServiceCollectConfiguration;
import com.arextest.config.model.vo.AgentRemoteConfigurationRequest;
import com.arextest.config.model.vo.AgentRemoteConfigurationResponse;
import com.arextest.config.model.vo.AgentStatusRequest;
import com.arextest.config.model.vo.AgentStatusType;
import com.arextest.config.model.vo.CompareConfiguration;
import com.arextest.storage.service.QueryConfigService;
import com.arextest.storage.service.config.ConfigurableHandler;
import com.arextest.storage.service.config.impl.ApplicationConfigurableHandler;
import com.arextest.storage.service.config.impl.ApplicationInstancesConfigurableHandler;
import com.arextest.storage.service.config.impl.ApplicationServiceConfigurableHandler;
import com.arextest.storage.service.config.impl.ServiceCollectConfigurableHandler;
import com.arextest.storage.trace.MDCTracer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author jmo
 * @since 2022/1/22
 */
@Slf4j
@Controller
@RequestMapping(path = "/api/config/agent", produces = MediaType.APPLICATION_JSON_VALUE)
public final class AgentRemoteConfigurationController {

  private static final String NOT_RECORDING = "(not recording)";
  private static final String EMPTY_TIME = "0";
  private static final String LAST_MODIFY_TIME = "If-Modified-Since";
  private static final String INCLUDE_SERVICE_OPERATIONS = "includeServiceOperations";

  @Resource
  private ConfigurableHandler<DynamicClassConfiguration> dynamicClassHandler;
  @Resource
  private ConfigurableHandler<ApplicationConfiguration> applicationHandler;
  @Resource
  private ApplicationInstancesConfigurableHandler instanceHandler;
  @Resource
  private ApplicationServiceConfigurableHandler applicationServiceHandler;
  @Resource
  private ServiceCollectConfigurableHandler serviceCollectHandler;
  @Resource
  private ApplicationConfigurableHandler applicationConfigurableHandler;
  @Resource
  private ExecutorService envUpdateHandlerExecutor;
  @Resource
  private QueryConfigService queryConfigService;
  @Resource
  private ObjectMapper objectMapper;

  @PostMapping("/load")
  @ResponseBody
  public Response load(@RequestBody AgentRemoteConfigurationRequest request) {
    try {
      final String appId = request.getAppId();
      if (StringUtils.isEmpty(appId)) {
        return ResponseUtils.parameterInvalidResponse("The requested " + "appId is empty");
      }
      MDCTracer.addAppId(appId);
      LOGGER.info("from appId: {} , load config", appId);
      ApplicationConfiguration applicationConfiguration = this.loadApplicationResult(request);
      if (applicationConfiguration == null) {
        LOGGER.error("from appId: {} , load config resource not found", appId);
        return ResponseUtils.resourceNotFoundResponse();
      }
      InstancesConfiguration requestInstance = InstancesMapper.INSTANCE.dtoFromContract(
          request);

      // ensure new instance is created
      instanceHandler.createOrUpdate(requestInstance);
      applicationServiceHandler.createOrUpdate(request.getAppId());

      // all active instances of app
      List<InstancesConfiguration> fullInstance = instanceHandler.listByAppOrdered(appId);

      Pair<ServiceCollectConfiguration, List<InstancesConfiguration>> collectConfigAndInstance =
          serviceCollectHandler.allocateServiceCollectConfig(appId, fullInstance, requestInstance);
      ServiceCollectConfiguration collectConfig = collectConfigAndInstance.getLeft();
      List<InstancesConfiguration> envInstance = collectConfigAndInstance.getRight();

      AgentRemoteConfigurationResponse body = new AgentRemoteConfigurationResponse();
      body.setDynamicClassConfigurationList(dynamicClassHandler.useResultAsList(appId));
      body.setServiceCollectConfiguration(collectConfig);
      body.setExtendField(getExtendField(collectConfig));
      body.setStatus(applicationConfiguration.getStatus());

      InstancesConfiguration sourceInstance = fullInstance.stream()
          .filter(instance -> Objects.equals(instance.getHost(), (request.getHost())))
          .findFirst().orElse(null);
      if (sourceInstance != null && sourceInstance.getExtendField() != null) {
        body.getExtendField().putAll(sourceInstance.getExtendField());
      }

      // only a limited number of machines IN THIS ENVIRONMENT are allowed to record
      Set<String> allowRecordingHosts =
          envInstance.stream()
              .limit(collectConfig.getRecordMachineCountLimit())
              .map(InstancesConfiguration::getHost)
              .collect(Collectors.toSet());
      if (allowRecordingHosts.contains(request.getHost())) {
        body.setTargetAddress(request.getHost());
        body.setMessage(request.getHost());
        body.setAgentEnabled(Boolean.TRUE);
      } else {
        body.setTargetAddress(request.getHost() + NOT_RECORDING);
        body.setMessage(request.getHost() + NOT_RECORDING);
        body.setAgentEnabled(Boolean.FALSE);
      }

      // asynchronously update application env
      asyncUpdateAppEnv(requestInstance);
      try {
        CompareConfiguration compareConfiguration = queryConfigService.queryCompareConfiguration(appId);
        body.setCompareConfiguration(compareConfiguration);
      } catch (Exception e) {
        LOGGER.error("query compare configuration error", e);
      }


      return ResponseUtils.successResponse(body);
    } catch (Exception e) {
      LOGGER.error("load config error", e);
      return ResponseUtils.errorResponse(e.getMessage(), ResponseCode.REQUESTED_HANDLE_EXCEPTION);
    } finally {
      MDCTracer.clear();
    }
  }

  @PostMapping("/agentStatus")
  @ResponseBody
  public ResponseEntity<String> agentStatus(HttpServletRequest httpServletRequest,
      HttpServletResponse response,
      @RequestBody AgentStatusRequest request) {

    try {
      // get requested appId
      final String appId = request.getAppId();
      if (StringUtils.isEmpty(appId)) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      }

      MDCTracer.addAppId(appId);
      LOGGER.info("from appId: {} , load agentStatus", request.getAppId());

      String modifiedTime = EMPTY_TIME;
      // update the instance
      InstancesConfiguration instancesConfiguration = InstancesMapper.INSTANCE.dtoFromContract(
          request);
      if (AgentStatusType.SHUTDOWN.equalsIgnoreCase(instancesConfiguration.getAgentStatus())) {
        instanceHandler.deleteByAppIdAndHost(
            instancesConfiguration.getAppId(),
            instancesConfiguration.getHost());
      } else {
        instanceHandler.createOrUpdate(instancesConfiguration);
        // get the latest time
        ServiceCollectConfiguration serviceConfig =
            serviceCollectHandler.useResult(instancesConfiguration.getAppId());
        if (serviceConfig.getModifiedTime() != null) {
          SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
          dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
          modifiedTime = dateFormat.format(serviceConfig.getModifiedTime());
        }
      }

      HttpStatus httpStatus = HttpStatus.OK;
      String ifModifiedSinceValue = httpServletRequest.getHeader(LAST_MODIFY_TIME);
      if (StringUtils.equals(ifModifiedSinceValue, modifiedTime)) {
        // 304 response
        httpStatus = HttpStatus.NOT_MODIFIED;
      }

      // 200 OK response
      response.setHeader("Last-Modified", modifiedTime);
      return new ResponseEntity<>(httpStatus);

    } catch (Exception e) {
      LOGGER.error("load agentStatus error", e);
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    } finally {
      MDCTracer.clear();
    }
  }



  private Map<String, String> getExtendField(
      ServiceCollectConfiguration serviceCollectConfiguration) {
    Map<String, String> extendField = new HashMap<>();
    // set the skipped field of serialization
    if (serviceCollectConfiguration != null && CollectionUtils.isNotEmpty(
        serviceCollectConfiguration.getSerializeSkipInfoList())) {
      try {
        String infoListStr = objectMapper.writeValueAsString(
            serviceCollectConfiguration.getSerializeSkipInfoList());
        extendField.put("serializeSkipInfoList", infoListStr);
      } catch (JsonProcessingException e) {
        LOGGER.error("getExtendField error", e);
      }
    }

    if (serviceCollectConfiguration != null && MapUtils.isNotEmpty(
        serviceCollectConfiguration.getExtendField())) {
      // set includeServiceOperations to allow which operations to record
      String includeServiceOperations = serviceCollectConfiguration.getExtendField()
          .getOrDefault(INCLUDE_SERVICE_OPERATIONS, null);
      if (StringUtils.isNotEmpty(includeServiceOperations)) {
        extendField.put(INCLUDE_SERVICE_OPERATIONS, includeServiceOperations);
      }
    }

    return extendField;
  }

  private void asyncUpdateAppEnv(InstancesConfiguration instancesConfiguration) {
    try {
      CompletableFuture.runAsync(
          new AddEnvRunnable(instancesConfiguration, applicationConfigurableHandler),
          envUpdateHandlerExecutor);
    } catch (RejectedExecutionException e) {
      LOGGER.error("envUpdateHandlerExecutor is full, appId:{}",
          instancesConfiguration.getAppId());
    }
  }

  private ApplicationConfiguration loadApplicationResult(AgentRemoteConfigurationRequest request) {
    return applicationHandler.useResult(request.getAppId());
  }

  @AllArgsConstructor
  private static class AddEnvRunnable implements Runnable {

    private InstancesConfiguration instance;

    private ApplicationConfigurableHandler applicationConfigurableHandler;

    @Override
    public void run() {
      try {
        Map<String, String> tags = instance.getTags();
        if (MapUtils.isEmpty(tags)) {
          return;
        }
        applicationConfigurableHandler.addEnvToApp(instance.getAppId(), tags);
      } catch (RuntimeException e) {
        LOGGER.error("add env error", e);
      }
    }
  }
}
