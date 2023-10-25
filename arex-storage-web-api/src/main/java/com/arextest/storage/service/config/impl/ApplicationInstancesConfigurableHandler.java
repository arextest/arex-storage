package com.arextest.storage.service.config.impl;

import com.arextest.config.model.dto.application.InstancesConfiguration;
import com.arextest.config.repository.ConfigRepositoryProvider;
import com.arextest.config.repository.impl.InstancesConfigurationRepositoryImpl;
import com.arextest.storage.service.config.AbstractConfigurableHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author jmo
 * @since 2022/1/23
 */
@Slf4j
@Component
public final class ApplicationInstancesConfigurableHandler extends AbstractConfigurableHandler<InstancesConfiguration> {

    @Resource
    private InstancesConfigurationRepositoryImpl instancesConfigurationRepository;

    protected ApplicationInstancesConfigurableHandler(
            @Autowired ConfigRepositoryProvider<InstancesConfiguration> repositoryProvider) {
        super(repositoryProvider);
    }

    public void createOrUpdate(InstancesConfiguration instancesConfiguration) {
        super.update(instancesConfiguration);
    }

    public List<InstancesConfiguration> useResultAsList(String appId, int top) {
        return instancesConfigurationRepository.listBy(appId, top);
    }

    public boolean deleteByAppIdAndHost(String appId, String host) {
        return instancesConfigurationRepository.removeByAppIdAndHost(appId, host);
    }
}