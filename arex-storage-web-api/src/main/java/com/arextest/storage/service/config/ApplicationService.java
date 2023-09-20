package com.arextest.storage.service.config;

import com.arextest.config.model.dto.StatusType;
import com.arextest.config.model.dto.application.ApplicationConfiguration;
import com.arextest.config.model.vo.AddApplicationRequest;
import com.arextest.config.model.vo.AddApplicationResponse;
import com.arextest.config.model.vo.UpdateApplicationRequest;
import com.arextest.config.repository.impl.ApplicationConfigurationRepositoryImpl;
import com.arextest.storage.utils.RandomUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author wildeslam.
 * @create 2023/9/15 14:37
 */
@Component
public class ApplicationService {

    @Autowired
    private ApplicationConfigurationRepositoryImpl applicationConfigurationRepository;

    public AddApplicationResponse addApplication(AddApplicationRequest request) {
        AddApplicationResponse response = new AddApplicationResponse();

        ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        applicationConfiguration.setAppName(request.getAppName());
        applicationConfiguration.setAgentVersion(StringUtils.EMPTY);
        applicationConfiguration.setAgentExtVersion(StringUtils.EMPTY);
        applicationConfiguration.setRecordedCaseCount(0);
        applicationConfiguration.setStatus(StatusType.RECORD.getMask() | StatusType.REPLAY.getMask());
        applicationConfiguration.setOwners(request.getOwners());

        applicationConfiguration.setOrganizationName("unknown organization name");
        applicationConfiguration.setGroupName("unknown group name");
        applicationConfiguration.setGroupId("unknown group id");
        applicationConfiguration.setOrganizationId("unknown organization id");
        applicationConfiguration.setDescription("unknown description");
        applicationConfiguration.setCategory("unknown category");

        String appId = RandomUtils.generateRandomId(request.getAppName());
        applicationConfiguration.setAppId(appId);

        boolean success = applicationConfigurationRepository.insert(applicationConfiguration);
        response.setAppId(appId);
        response.setSuccess(success);
        return response;
    }

    public boolean modifyApplication(UpdateApplicationRequest request) {
        List<ApplicationConfiguration> applicationConfigurationList =
            applicationConfigurationRepository.listBy(request.getAppId());
        if (CollectionUtils.isEmpty(applicationConfigurationList)) {
            return false;
        }
        ApplicationConfiguration applicationConfiguration = applicationConfigurationList.get(0);
        if (request.getAppName() != null) {
            applicationConfiguration.setAppName(request.getAppName());
        }
        if (request.getOwners() != null) {
            applicationConfiguration.setOwners(request.getOwners());
        }
        return applicationConfigurationRepository.update(applicationConfiguration);
    }
}
