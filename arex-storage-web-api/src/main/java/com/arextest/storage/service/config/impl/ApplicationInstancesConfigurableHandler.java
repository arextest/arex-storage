package com.arextest.storage.service.config.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.arextest.storage.model.dto.config.application.InstancesConfiguration;
import com.arextest.storage.repository.ConfigRepositoryProvider;
import com.arextest.storage.repository.impl.mongo.config.InstancesConfigurationRepositoryImpl;
import com.arextest.storage.service.config.AbstractConfigurableHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jmo
 * @since 2022/1/23
 */
@Slf4j
@Component
public final class ApplicationInstancesConfigurableHandler extends AbstractConfigurableHandler<InstancesConfiguration> {

    protected ApplicationInstancesConfigurableHandler(
        @Autowired ConfigRepositoryProvider<InstancesConfiguration> repositoryProvider) {
        super(repositoryProvider);
    }

    @Resource
    private InstancesConfigurationRepositoryImpl instancesConfigurationRepository;

    public void createOrUpdate(InstancesConfiguration instancesConfiguration) {
        super.update(instancesConfiguration);
    }

    public List<InstancesConfiguration> useResultAsList(String appId, int top) {
        return instancesConfigurationRepository.listBy(appId, top);
    }
}