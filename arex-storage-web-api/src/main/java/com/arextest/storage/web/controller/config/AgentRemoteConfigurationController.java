package com.arextest.storage.web.controller.config;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.arextest.common.model.response.Response;
import com.arextest.common.model.response.ResponseCode;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.storage.model.dto.config.application.ApplicationConfiguration;
import com.arextest.storage.model.dto.config.application.InstancesConfiguration;
import com.arextest.storage.model.dto.config.record.DynamicClassConfiguration;
import com.arextest.storage.model.dto.config.record.ServiceCollectConfiguration;
import com.arextest.storage.model.mapper.InstancesMapper;
import com.arextest.storage.model.vo.config.AgentRemoteConfigurationRequest;
import com.arextest.storage.model.vo.config.AgentRemoteConfigurationResponse;
import com.arextest.storage.model.vo.config.AgentStatusRequest;
import com.arextest.storage.service.config.ConfigurableHandler;
import com.arextest.storage.service.config.impl.ApplicationInstancesConfigurableHandler;
import com.arextest.storage.service.config.impl.ApplicationServiceConfigurableHandler;
import com.arextest.storage.trace.MDCTracer;

import lombok.extern.slf4j.Slf4j;

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
    @Resource
    private ConfigurableHandler<DynamicClassConfiguration> dynamicClassHandler;
    @Resource
    private ConfigurableHandler<ApplicationConfiguration> applicationHandler;
    @Resource
    private ConfigurableHandler<ServiceCollectConfiguration> serviceCollectHandler;
    @Resource
    private ApplicationInstancesConfigurableHandler applicationInstancesConfigurableHandler;
    @Resource
    private ApplicationServiceConfigurableHandler applicationServiceHandler;
    // private ScheduledExecutorService executorService;

    @Value("${arex.config.application.service.update.delaySeconds:30}")
    private long delayUpdateServiceSeconds;

    // @PostConstruct
    // private void init() {
    // executorService = Executors.newSingleThreadScheduledExecutor();
    // }

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
            ServiceCollectConfiguration serviceCollectConfiguration = serviceCollectHandler.useResult(appId);
            applicationServiceHandler.createOrUpdate(request.getAppId());
            AgentRemoteConfigurationResponse body = new AgentRemoteConfigurationResponse();
            body.setDynamicClassConfigurationList(dynamicClassHandler.useResultAsList(appId));
            body.setServiceCollectConfiguration(serviceCollectConfiguration);
            body.setStatus(applicationConfiguration.getStatus());
            InstancesConfiguration instancesConfiguration = InstancesMapper.INSTANCE.dtoFromContract(request);
            applicationInstancesConfigurableHandler.createOrUpdate(instancesConfiguration);
            List<InstancesConfiguration> instances = applicationInstancesConfigurableHandler.useResultAsList(appId,
                serviceCollectConfiguration.getRecordMachineCountLimit());
            Set<String> recordingHosts =
                instances.stream().map(InstancesConfiguration::getHost).collect(Collectors.toSet());
            if (recordingHosts.contains(request.getHost())) {
                body.setTargetAddress(request.getHost());
            } else {
                body.setTargetAddress(request.getHost() + NOT_RECORDING);
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
    public ResponseEntity<String> agentStatus(HttpServletRequest httpServletRequest, HttpServletResponse response,
        @RequestBody AgentStatusRequest request) {

        try {
            // get requested appId
            final String appId = request.getAppId();
            if (StringUtils.isEmpty(appId)) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            MDCTracer.addAppId(appId);
            LOGGER.info("from appId: {} , load agentStatus", request.getAppId());
            // update the instance
            InstancesConfiguration instancesConfiguration = InstancesMapper.INSTANCE.dtoFromContract(request);
            applicationInstancesConfigurableHandler.createOrUpdate(instancesConfiguration);

            // get the latest time
            ServiceCollectConfiguration serviceConfig = serviceCollectHandler.useResult(appId);

            String modifiedTime = EMPTY_TIME;
            if (serviceConfig.getModifiedTime() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                modifiedTime = dateFormat.format(serviceConfig.getModifiedTime());
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

    private ApplicationConfiguration loadApplicationResult(AgentRemoteConfigurationRequest request) {
        return applicationHandler.useResult(request.getAppId());
    }
}
