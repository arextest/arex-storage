package com.arextest.storage.service;

import java.net.MalformedURLException;
import java.util.List;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.arextest.common.model.classloader.RemoteJarClassLoader;
import com.arextest.common.utils.RemoteJarLoaderUtils;
import com.arextest.extension.desensitization.DataDesensitization;
import com.arextest.extension.desensitization.DefaultDataDesensitization;
import com.arextest.model.response.ResponseStatusType;
import com.arextest.storage.client.HttpWepServiceApiClient;

import lombok.Data;

@Slf4j
@Component
public class DesensitizeService {

    @Resource
    private HttpWepServiceApiClient httpWepServiceApiClient;

    @Value("${arex.url.report.desensitization}")
    private String desensitizationUrl;

    public DataDesensitization loadDesensitization() {
        DataDesensitization dataDesensitization = new DefaultDataDesensitization();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Token",
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpbmZvIjoidGVzdCJ9.YeLmUW--fqrtmag1QTDmL8U7RVZlb34xPAAxorxSCPM");
        HttpEntity<?> request = new HttpEntity<>(headers);
        DesensitizationResponse response =
            httpWepServiceApiClient.jsonPost(desensitizationUrl, request, DesensitizationResponse.class);
        List<DesensitizationJar> desensitizationJars = response.getBody();
        if (CollectionUtils.isEmpty(desensitizationJars)) {
            return dataDesensitization;
        }

        String jarUrl = desensitizationJars.get(0).jarUrl;

        try {
            // RemoteJarClassLoader remoteJarClassLoader = RemoteJarLoaderUtils.loadJar(jarUrl);

            RemoteJarClassLoader remoteJarClassLoader = RemoteJarLoaderUtils
                .loadJar("./lib/arex-desensitization-core-0.0.0-SNAPSHOT-jar-with-dependencies.jar");
            List<DataDesensitization> dataDesensitizations =
                RemoteJarLoaderUtils.loadService(DataDesensitization.class, remoteJarClassLoader);

            // desensitization = dataDesensitizations.get(0);
            // String origin = "nihao";
            // String encrypt = desensitization.encrypt(origin);
            // System.out.println(encrypt);
            dataDesensitization = dataDesensitizations.get(0);
        } catch (Exception e) {
            LOGGER.error("load desensitization error", e);
            throw new RuntimeException(e);
        }
        return dataDesensitization;
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

    public static void main(String[] args) {

    }

}
