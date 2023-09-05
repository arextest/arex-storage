package com.arextest.storage.service.config.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.commons.lang3.StringUtils;

import com.arextest.storage.model.dto.config.application.ApplicationConfiguration;
import com.arextest.storage.model.dto.config.application.ApplicationDescription;
import com.arextest.storage.model.dto.config.application.OperationDescription;
import com.arextest.storage.model.dto.config.application.ServiceDescription;
import com.arextest.storage.service.config.provider.ApplicationDescriptionProvider;
import com.arextest.storage.service.config.provider.ApplicationServiceDescriptionProvider;

import lombok.Data;

/**
 * @author jmo
 * @since 2022/2/21
 */
public final class Providers {
    private Providers() {}

    public static ApplicationDescriptionProvider createApplication(String name) {
        if (StringUtils.isEmpty(name)) {
            return new UnknownApplicationDescriptionImpl();
        }
        return lookup(ApplicationDescriptionProvider.class, name);
    }

    public static ApplicationServiceDescriptionProvider createApplicationService(String name) {
        if (StringUtils.isEmpty(name)) {
            return new DefaultServiceDescriptionProviderImpl();
        }
        return lookup(ApplicationServiceDescriptionProvider.class, name);
    }

    private static <T> T lookup(Class<T> tClass, String name) {
        ServiceLoader<T> serviceLoader = ServiceLoader.load(tClass);
        Iterator<T> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            T instance = iterator.next();
            if (instance.getClass().getSimpleName().equals(name)) {
                return instance;
            }
        }
        return null;
    }

    private static final class DefaultServiceDescriptionProviderImpl implements ApplicationServiceDescriptionProvider {

        @Override
        public List<? extends ServiceDescription> get(String appId) {
            DefaultServiceDescriptionImpl serviceConfiguration = new DefaultServiceDescriptionImpl();
            serviceConfiguration.setAppId(appId);
            serviceConfiguration.setServiceName("unknown service name");
            serviceConfiguration.setServiceKey("unknown service key");
            return Collections.singletonList(serviceConfiguration);
        }

        @Data
        private static final class DefaultServiceDescriptionImpl implements ServiceDescription {
            private String serviceCode;
            private String serviceName;
            private String serviceKey;
            private List<? extends OperationDescription> operationList;
            private String appId;
        }
    }

    private static final class UnknownApplicationDescriptionImpl implements ApplicationDescriptionProvider {
        @Override
        public ApplicationDescription get(String appId) {
            ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
            applicationConfiguration.setAppId(appId);
            applicationConfiguration.setOwner("unknown owner");
            applicationConfiguration.setAppName("unknown app name");
            applicationConfiguration.setOrganizationName("unknown organization name");
            applicationConfiguration.setGroupName("unknown group name");
            applicationConfiguration.setGroupId("unknown group id");
            applicationConfiguration.setOrganizationId("unknown organization id");
            applicationConfiguration.setDescription("unknown description");
            applicationConfiguration.setCategory("unknown category");
            return applicationConfiguration;
        }
    }
}
