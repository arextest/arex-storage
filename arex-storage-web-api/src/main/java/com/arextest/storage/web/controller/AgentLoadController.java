package com.arextest.storage.web.controller;

import com.arextest.storage.client.HttpWepServiceApiClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by rchen9 on 2022/10/8.
 */
@Controller
@RequestMapping("/api/config/agent")
@Slf4j
public class AgentLoadController {

    @Value("${arex.url.report.agentload}")
    private String agentLoadUrl;
    @Value("${arex.url.report.agentStatus}")
    private String agentStatusUrl;
    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;

    @PostMapping(value = "/load", produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public String load(@RequestBody AgentRemoteConfigurationRequest request) {
        return httpWepServiceApiClient.jsonPost(agentLoadUrl, request, String.class);
    }

    @PostMapping(value = "/agentStatus", produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public ResponseEntity<String> agentStatus(HttpServletRequest httpServletRequest, @RequestBody AgentStatusRequest request) {
        HttpHeaders httpHeaders = Collections.list(httpServletRequest.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        h -> Collections.list(httpServletRequest.getHeaders(h)),
                        (oldValue, newValue) -> newValue,
                        HttpHeaders::new
                ));
        return httpWepServiceApiClient.responsePost(agentStatusUrl, request, String.class, httpHeaders);
    }
    @Data
    private static final class AgentRemoteConfigurationRequest {
        private String appId;
        private String host;
        private String recordVersion;
        private String agentStatus;
        private Map<String, String> systemEnv;
        private Map<String, String> systemProperties;
    }

    @Data
    private static final class AgentStatusRequest {
        private String appId;
        private String host;
        private String agentStatus;
    }
}