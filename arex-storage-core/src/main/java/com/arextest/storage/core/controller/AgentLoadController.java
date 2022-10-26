package com.arextest.storage.core.controller;

import com.arextest.storage.core.client.HttpWepServiceApiClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * Created by rchen9 on 2022/10/8.
 */
@Controller
@RequestMapping("/api/config/agent")
@Slf4j
public class AgentLoadController {

    @Value("${arex.report.config.agent.url}")
    private String agentLoadUrl;
    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;

    @PostMapping(value = "/load", produces = {MediaType.APPLICATION_JSON_UTF8_VALUE})
    @ResponseBody
    public String load(@RequestBody AgentRemoteConfigurationRequest request) {
        return httpWepServiceApiClient.jsonPost(agentLoadUrl, request, String.class);
    }


    @Data
    private static final class AgentRemoteConfigurationRequest {
        private String appId;
    }
}