package com.arextest.storage.service;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.arextest.common.model.classloader.RemoteJarClassLoader;
import com.arextest.common.utils.RemoteJarLoaderUtils;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.extension.desensitization.DefaultDataDesensitization;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.storage.client.HttpWepServiceApiClient;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DesensitizeService {

    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;

    @Value("${arex.url.report.desensitization}")
    private String desensitizationUrl;

    public DataDesensitization loadDesensitization(String remoteJarUrl) {
        DataDesensitization dataDesensitization = new DefaultDataDesensitization();
        if (StringUtils.isEmpty(remoteJarUrl)) {
            return dataDesensitization;
        }
        try {
            RemoteJarClassLoader remoteJarClassLoader = RemoteJarLoaderUtils.loadJar(remoteJarUrl);
            List<DataDesensitization> dataDesensitizations =
                RemoteJarLoaderUtils.loadService(DataDesensitization.class, remoteJarClassLoader);
            dataDesensitization = dataDesensitizations.get(0);
        } catch (Exception e) {
            LOGGER.error("load desensitization error", e);
            throw new RuntimeException(e);
        }
        return dataDesensitization;
    }

    @Retryable(value = {NullPointerException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String getRemoteJarUrl() {
        String result = null;
        DesensitizationResponse response =
            httpWepServiceApiClient.jsonPost(desensitizationUrl, null, DesensitizationResponse.class);
        List<DesensitizationJar> desensitizationJars = response.getBody();
        if (CollectionUtils.isEmpty(desensitizationJars)) {
            return result;
        }
        return desensitizationJars.get(0).jarUrl;
    }

    @Data
    private static final class DesensitizationResponse {
        private ResponseStatusType responseStatusType;
        private List<DesensitizationJar> body;
    }

    @Data
    private static final class DesensitizationJar {
        private String jarUrl;
    }

}
