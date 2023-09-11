package com.arextest.storage.beans;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arextest.storage.service.config.provider.ApplicationDescriptionProvider;
import com.arextest.storage.service.config.provider.ApplicationServiceDescriptionProvider;
import com.arextest.storage.service.config.impl.Providers;

/**
 * @author jmo
 * @since 2022/1/31
 */
@Configuration
public class ApplicationProviderConfiguration {
    @Value("${arex.config.application.provider}")
    private String applicationProviderName;
    @Value("${arex.config.application.service.provider}")
    private String applicationServiceProviderName;

    @Bean
    public ApplicationDescriptionProvider applicationDescriptionProvider() {
        return Providers.createApplication(applicationProviderName);
    }

    @Bean
    public ApplicationServiceDescriptionProvider applicationServiceDescriptionProvider() {
        return Providers.createApplicationService(applicationServiceProviderName);
    }
}
